package com.example.core.companion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class CompanionNotificationCategory(
  val channelId: String,
  val displayName: String,
  val description: String,
  val defaultImportance: Int
) {
  AGENT_STATUS(
    "aegis_channel_agent_status",
    "Agent Operational Status",
    "Updates on active modes, background state, and provider connectivity",
    NotificationManager.IMPORTANCE_LOW
  ),
  COMPANION(
    "aegis_channel_companion",
    "Companion Dialogue & Check-ins",
    "Proactive conversational check-ins and greetings if authorized",
    NotificationManager.IMPORTANCE_DEFAULT
  ),
  REMINDER(
    "aegis_channel_reminder",
    "Reminders & Scheduled Alerts",
    "Explicit reminders requested by the operator",
    NotificationManager.IMPORTANCE_HIGH
  ),
  RESEARCH(
    "aegis_channel_research",
    "Research & Intelligence Briefings",
    "Autonomous research completion and synthesis reports",
    NotificationManager.IMPORTANCE_DEFAULT
  ),
  SYSTEM(
    "aegis_channel_system",
    "System Notices & Permissions",
    "Important capability and permission audit advisories",
    NotificationManager.IMPORTANCE_DEFAULT
  ),
  ERROR(
    "aegis_channel_error",
    "Diagnostic & Recovery Alerts",
    "Provider outages, synthesis failures, or device tool exceptions",
    NotificationManager.IMPORTANCE_HIGH
  )
}

data class NotificationPreferences(
  val allowedCategories: Set<CompanionNotificationCategory> = CompanionNotificationCategory.values().toSet(),
  val silentMode: Boolean = false
)

class CompanionNotificationManager(private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_notification_prefs", Context.MODE_PRIVATE)

  private val _preferences = MutableStateFlow(loadPreferences())
  val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

  // Rate limiting & Duplicate suppression
  private val lastSentTimestamps = ConcurrentHashMap<String, Long>()
  private val recentNotificationHashes = ConcurrentHashMap<String, Long>()

  companion object {
    private const val MIN_CATEGORY_COOLDOWN_MS = 60_000L // 1 minute between same category
    private const val DUPLICATE_SUPPRESSION_WINDOW_MS = 300_000L // 5 minutes duplicate window
  }

  init {
    createChannels()
  }

  fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  private fun loadPreferences(): NotificationPreferences {
    val enabledNames = prefs.getStringSet("enabled_categories", null)
    val categories = if (enabledNames != null) {
      enabledNames.mapNotNull {
        try { CompanionNotificationCategory.valueOf(it) } catch (_: Exception) { null }
      }.toSet()
    } else {
      CompanionNotificationCategory.values().toSet()
    }
    val silent = prefs.getBoolean("silent_notifications", false)
    return NotificationPreferences(categories, silent)
  }

  fun toggleCategory(category: CompanionNotificationCategory, enabled: Boolean) {
    val current = _preferences.value.allowedCategories.toMutableSet()
    if (enabled) current.add(category) else current.remove(category)
    val updated = _preferences.value.copy(allowedCategories = current)
    savePreferences(updated)
  }

  fun setSilentMode(silent: Boolean) {
    val updated = _preferences.value.copy(silentMode = silent)
    savePreferences(updated)
  }

  private fun savePreferences(prefsObj: NotificationPreferences) {
    prefs.edit()
      .putStringSet("enabled_categories", prefsObj.allowedCategories.map { it.name }.toSet())
      .putBoolean("silent_notifications", prefsObj.silentMode)
      .apply()
    _preferences.value = prefsObj
  }

  fun postNotification(
    category: CompanionNotificationCategory,
    title: String,
    message: String,
    notificationId: Int = category.ordinal + 5000
  ): Boolean {
    if (!hasNotificationPermission()) return false
    if (!_preferences.value.allowedCategories.contains(category)) return false

    val now = System.currentTimeMillis()

    // 1. Duplicate suppression
    val contentHash = "$title::$message"
    val lastSameContent = recentNotificationHashes[contentHash]
    if (lastSameContent != null && (now - lastSameContent) < DUPLICATE_SUPPRESSION_WINDOW_MS) {
      return false // Suppress duplicate content
    }

    // 2. Cooldown check per category (except ERROR and REMINDER which are high priority)
    if (category != CompanionNotificationCategory.ERROR && category != CompanionNotificationCategory.REMINDER) {
      val lastCategoryTime = lastSentTimestamps[category.name] ?: 0L
      if (now - lastCategoryTime < MIN_CATEGORY_COOLDOWN_MS) {
        return false // Rate limited
      }
    }

    lastSentTimestamps[category.name] = now
    recentNotificationHashes[contentHash] = now

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      },
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val silent = _preferences.value.silentMode

    val builder = NotificationCompat.Builder(context, category.channelId)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setSilent(silent)
      .setPriority(
        if (silent) NotificationCompat.PRIORITY_LOW
        else when (category) {
          CompanionNotificationCategory.REMINDER, CompanionNotificationCategory.ERROR -> NotificationCompat.PRIORITY_HIGH
          CompanionNotificationCategory.AGENT_STATUS -> NotificationCompat.PRIORITY_LOW
          else -> NotificationCompat.PRIORITY_DEFAULT
        }
      )

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    manager?.notify(notificationId, builder.build())
    return true
  }

  private fun createChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return
      CompanionNotificationCategory.values().forEach { category ->
        val channel = NotificationChannel(
          category.channelId,
          category.displayName,
          category.defaultImportance
        ).apply {
          description = category.description
        }
        manager.createNotificationChannel(channel)
      }
    }
  }
}
