package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.agent.AgentController
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PrivacyCenterScreen(
  onBack: () -> Unit,
  agentController: AgentController,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val companion = agentController.companionEngine
  val memorySettings by agentController.aiEngine?.memoryManager?.settings?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.ai.memory.MemorySettings())
  }
  val bgPresence by companion?.backgroundPresenceController?.presenceState?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.background.BackgroundPresenceState.NOT_RUNNING)
  }

  val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
  val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
  val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
  val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  } else {
    true
  }
  val hasOverlayPermission = companion?.floatingAssistantController?.hasOverlayPermission() ?: false

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_center_back")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "SECURITY & AUDIT // PRIVACY",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            )
          )
          Text(
            text = "Privacy & Access Center",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
        }
      }
    }

    item {
      FuturisticCard(headerText = "Three-Tiered Security Governance", badgeText = "Policy Separated") {
        Text(
          text = "Aegis enforces a strict boundary between OS Permissions, Agent Capabilities, and Proactive Autonomous execution. Even if an Android permission is granted, actions require capability policy approval.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    }

    item {
      Text(
        text = "ANDROID OS HARDWARE PERMISSIONS",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      PrivacyItemRow(
        icon = Icons.Default.Mic,
        title = "Microphone Stream",
        description = "Voice recognition & conversational audio input",
        isGranted = hasMicPermission
      )
    }

    item {
      PrivacyItemRow(
        icon = Icons.Default.CameraAlt,
        title = "Camera Vision",
        description = "Photo & visual scene grounding analyzer",
        isGranted = hasCameraPermission
      )
    }

    item {
      PrivacyItemRow(
        icon = Icons.Default.LocationOn,
        title = "Fine Location",
        description = "Geographic coordinates for localized weather & maps",
        isGranted = hasLocationPermission
      )
    }

    item {
      PrivacyItemRow(
        icon = Icons.Default.Notifications,
        title = "Post Notifications",
        description = "System notifications tray delivery",
        isGranted = hasNotifPermission
      )
    }

    item {
      PrivacyItemRow(
        icon = Icons.Default.DisplaySettings,
        title = "Overlay Window (SYSTEM_ALERT_WINDOW)",
        description = "Floating HUD presence above third-party applications",
        isGranted = hasOverlayPermission
      )
    }

    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "SUB-SYSTEM PRIVACY MATRICES",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      FuturisticCard(headerText = "Memory Substrate Privacy", badgeText = if (memorySettings.memoryEnabled) "ACTIVE" else "OFF") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Long-Term Memory Substrate", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            Text("Stored locally in encrypted database. Never synchronized to cloud without policy.", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp))
          }
          StatusBadge(text = if (memorySettings.memoryEnabled) "LOCAL ENCRYPTED" else "DISABLED", indicatorColor = if (memorySettings.memoryEnabled) StatusReady else StatusWarning, isPulsing = false)
        }
      }
    }

    item {
      FuturisticCard(headerText = "Background Service Execution", badgeText = bgPresence.label) {
        Text(
          text = "Background service operates only with an active user-visible foreground service notification. No silent tracking or hidden microphone capture.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
    }
  }
}

@Composable
private fun PrivacyItemRow(
  icon: ImageVector,
  title: String,
  description: String,
  isGranted: Boolean
) {
  GlassPanel(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isGranted) CyanNeon else StatusWarning,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
        )
      }
      StatusBadge(
        text = if (isGranted) "GRANTED" else "RESTRICTED",
        indicatorColor = if (isGranted) StatusReady else StatusWarning,
        isPulsing = false
      )
    }
  }
}
