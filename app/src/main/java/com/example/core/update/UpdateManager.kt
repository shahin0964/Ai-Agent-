package com.example.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.core.companion.CompanionNotificationCategory
import com.example.core.companion.CompanionNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubRelease(
  val tagName: String,
  val name: String,
  val body: String,
  val publishedAt: String,
  val apkUrl: String?,
  val apkName: String?
)

sealed interface UpdateState {
  data object Idle : UpdateState
  data object Checking : UpdateState
  data class UpToDate(val currentVersion: String) : UpdateState
  data class UpdateAvailable(val release: GitHubRelease) : UpdateState
  data class Downloading(val progress: Float) : UpdateState
  data class ReadyToInstall(val apkFile: File, val release: GitHubRelease) : UpdateState
  data class Installing(val release: GitHubRelease) : UpdateState
  data class Error(val message: String) : UpdateState
  data object Offline : UpdateState
}

class UpdateManager(private val context: Context) {

  private val prefs = context.getSharedPreferences("aegis_update_prefs", Context.MODE_PRIVATE)
  private val notificationManager = CompanionNotificationManager(context)

  private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

  private val _latestRelease = MutableStateFlow<GitHubRelease?>(null)
  val latestRelease: StateFlow<GitHubRelease?> = _latestRelease.asStateFlow()

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val TAG = "UpdateManager"
    private const val GITHUB_API_URL = "https://api.github.com/repos/shahin0964/Ai-Agent-/releases/latest"
    private const val PREF_LAST_NOTIFIED_TAG = "last_notified_tag"
  }

  suspend fun checkForUpdates(notifyUserIfNew: Boolean = true): UpdateState = withContext(Dispatchers.IO) {
    _updateState.value = UpdateState.Checking
    try {
      val request = Request.Builder()
        .url(GITHUB_API_URL)
        .header("Accept", "application/vnd.github+json")
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val errState = UpdateState.Error("GitHub API returned status ${response.code}")
        _updateState.value = errState
        return@withContext errState
      }

      val responseBody = response.body?.string() ?: return@withContext UpdateState.Error("Empty response")
      val json = JSONObject(responseBody)

      val tagName = json.optString("tag_name", json.optString("tagName", "v1.0"))
      val name = json.optString("name", tagName)
      val body = json.optString("body", "No changelog provided.")
      val publishedAt = json.optString("published_at", json.optString("publishedAt", ""))

      val assets = json.optJSONArray("assets") ?: JSONArray()
      var apkUrl: String? = null
      var apkName: String? = null

      for (i in 0 until assets.length()) {
        val asset = assets.getJSONObject(i)
        val assetName = asset.optString("name", "")
        if (assetName.endsWith(".apk", ignoreCase = true)) {
          apkUrl = asset.optString("browser_download_url")
          apkName = assetName
          break
        }
      }

      val release = GitHubRelease(
        tagName = tagName,
        name = name,
        body = body,
        publishedAt = publishedAt,
        apkUrl = apkUrl,
        apkName = apkName
      )

      _latestRelease.value = release

      val currentVersion = BuildConfig.VERSION_NAME
      val isNewer = isVersionNewer(currentVersion, tagName)

      if (isNewer) {
        val updateState = UpdateState.UpdateAvailable(release)
        _updateState.value = updateState

        if (notifyUserIfNew) {
          val lastNotified = prefs.getString(PREF_LAST_NOTIFIED_TAG, null)
          if (lastNotified != tagName) {
            prefs.edit().putString(PREF_LAST_NOTIFIED_TAG, tagName).apply()
            notificationManager.postNotification(
              category = CompanionNotificationCategory.SYSTEM,
              title = "Nexus AI Update Available",
              message = "Version $tagName is available. Tap to view details and install."
            )
          }
        }
        updateState
      } else {
        val upToDateState = UpdateState.UpToDate(currentVersion)
        _updateState.value = upToDateState
        upToDateState
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to check for updates", e)
      val offlineState = UpdateState.Offline
      _updateState.value = offlineState
      offlineState
    }
  }

  suspend fun downloadUpdate(release: GitHubRelease): Boolean = withContext(Dispatchers.IO) {
    val url = release.apkUrl
    if (url.isNullOrBlank()) {
      _updateState.value = UpdateState.Error("No APK asset found in release ${release.tagName}")
      return@withContext false
    }

    try {
      _updateState.value = UpdateState.Downloading(0f)
      val request = Request.Builder().url(url).build()
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        _updateState.value = UpdateState.Error("Download failed with HTTP ${response.code}")
        return@withContext false
      }

      val body = response.body ?: run {
        _updateState.value = UpdateState.Error("Empty download body")
        return@withContext false
      }

      val contentLength = body.contentLength()
      val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
      val apkFile = File(updatesDir, release.apkName ?: "nexus-update.apk")

      body.byteStream().use { input ->
        FileOutputStream(apkFile).use { output ->
          val buffer = ByteArray(8192)
          var bytesCopied = 0L
          var bytesRead: Int
          while (input.read(buffer).also { bytesRead = it } >= 0) {
            output.write(buffer, 0, bytesRead)
            bytesCopied += bytesRead
            if (contentLength > 0) {
              val progress = bytesCopied.toFloat() / contentLength.toFloat()
              _updateState.value = UpdateState.Downloading(progress)
            }
          }
        }
      }

      if (apkFile.exists() && apkFile.length() > 0) {
        _updateState.value = UpdateState.ReadyToInstall(apkFile, release)
        true
      } else {
        _updateState.value = UpdateState.Error("Downloaded APK file is invalid or empty")
        false
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception downloading update", e)
      _updateState.value = UpdateState.Error("Download exception: ${e.localizedMessage}")
      false
    }
  }

  fun installUpdate(apkFile: File, release: GitHubRelease) {
    try {
      _updateState.value = UpdateState.Installing(release)
      val authority = "${context.packageName}.fileprovider"
      val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer", e)
      _updateState.value = UpdateState.Error("Failed to launch installer: ${e.localizedMessage}")
    }
  }

  private fun isVersionNewer(currentVersion: String, remoteTag: String): Boolean {
    val cleanCurrent = currentVersion.removePrefix("v").trim()
    val cleanRemote = remoteTag.removePrefix("v").trim()

    val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
    val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }

    val maxLen = maxOf(currentParts.size, remoteParts.size)
    for (i in 0 until maxLen) {
      val c = currentParts.getOrNull(i) ?: 0
      val r = remoteParts.getOrNull(i) ?: 0
      if (r > c) return true
      if (r < c) return false
    }
    return false
  }
}
