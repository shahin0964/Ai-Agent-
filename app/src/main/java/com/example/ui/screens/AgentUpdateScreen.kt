package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.update.GitHubRelease
import com.example.core.update.UpdateManager
import com.example.core.update.UpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentUpdateScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val updateManager = remember { UpdateManager(context) }
  val updateState by updateManager.updateState.collectAsState()
  val latestRelease by updateManager.latestRelease.collectAsState()

  LaunchedEffect(Unit) {
    updateManager.checkForUpdates(notifyUserIfNew = false)
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text("Agent Update", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = {
            kotlinx.coroutines.MainScope().launch {
              updateManager.checkForUpdates(notifyUserIfNew = false)
            }
          }) {
            Icon(Icons.Default.Refresh, contentDescription = "Check for Updates")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Version Status Header Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.SystemUpdate,
              contentDescription = null,
              modifier = Modifier.size(32.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Nexus AI",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Installed Version: ${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          latestRelease?.let { release ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Latest Release: ${release.tagName}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // State Action Card
      when (val state = updateState) {
        is UpdateState.Checking -> {
          StatusCard(
            icon = Icons.Default.Refresh,
            title = "Checking for Updates...",
            subtitle = "Querying official GitHub release repository..."
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          }
        }

        is UpdateState.UpToDate -> {
          StatusCard(
            icon = Icons.Default.CheckCircle,
            title = "You're Up to Date",
            subtitle = "Version ${state.currentVersion} is the latest available build."
          ) {
            Button(
              onClick = {
                kotlinx.coroutines.MainScope().launch {
                  updateManager.checkForUpdates(notifyUserIfNew = false)
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Check Again")
            }
          }
        }

        is UpdateState.UpdateAvailable -> {
          UpdateAvailableCard(
            release = state.release,
            onDownload = {
              kotlinx.coroutines.MainScope().launch {
                updateManager.downloadUpdate(state.release)
              }
            }
          )
        }

        is UpdateState.Downloading -> {
          DownloadingCard(progress = state.progress)
        }

        is UpdateState.ReadyToInstall -> {
          ReadyToInstallCard(
            release = state.release,
            onInstall = {
              updateManager.installUpdate(state.apkFile, state.release)
            }
          )
        }

        is UpdateState.Installing -> {
          StatusCard(
            icon = Icons.Default.InstallMobile,
            title = "Installing Update...",
            subtitle = "Launching package installer for ${state.release.tagName}"
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          }
        }

        is UpdateState.Error -> {
          StatusCard(
            icon = Icons.Default.Info,
            title = "Update Error",
            subtitle = state.message
          ) {
            Button(
              onClick = {
                kotlinx.coroutines.MainScope().launch {
                  updateManager.checkForUpdates(notifyUserIfNew = false)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
              Text("Retry")
            }
          }
        }

        is UpdateState.Offline -> {
          StatusCard(
            icon = Icons.Default.CloudOff,
            title = "Offline / Connection Error",
            subtitle = "Unable to connect to GitHub releases. Please check your network connection."
          ) {
            Button(
              onClick = {
                kotlinx.coroutines.MainScope().launch {
                  updateManager.checkForUpdates(notifyUserIfNew = false)
                }
              }
            ) {
              Text("Retry Connection")
            }
          }
        }

        is UpdateState.Idle -> {
          StatusCard(
            icon = Icons.Default.SystemUpdate,
            title = "Update Status Ready",
            subtitle = "Tap below to check for the latest version."
          ) {
            Button(
              onClick = {
                kotlinx.coroutines.MainScope().launch {
                  updateManager.checkForUpdates(notifyUserIfNew = false)
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Check for Updates")
            }
          }
        }
      }
    }
  }
}

@Composable
fun StatusCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  content: @Composable (() -> Unit)? = null
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (content != null) {
        Spacer(modifier = Modifier.height(16.dp))
        content()
      }
    }
  }
}

@Composable
fun UpdateAvailableCard(
  release: GitHubRelease,
  onDownload: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.SystemUpdate,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "New Update Available: ${release.tagName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          if (release.publishedAt.isNotBlank()) {
            Text(
              text = "Released: ${release.publishedAt.take(10)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Changelog / Release Notes:",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = release.body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onDownload,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download & Install Update")
      }
    }
  }
}

@Composable
fun DownloadingCard(progress: Float) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Downloading Update...",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(16.dp))
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun ReadyToInstallCard(
  release: GitHubRelease,
  onInstall: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.InstallMobile,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.tertiary
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Ready to Install: ${release.tagName}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Download complete. Tap below to launch the package installer.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onInstall,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Install Update Now")
      }
    }
  }
}
