package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

data class SettingsCategoryItem(
  val id: String,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val targetScreen: Screen,
  val badge: String? = null
)

val SettingsCategories = listOf(
  SettingsCategoryItem(
    id = "companion",
    title = "Companion & Presence",
    description = "Operational modes, proactive behavior, behavioral parameters, and sleep schedule",
    icon = Icons.Default.Tune,
    targetScreen = Screen.SettingsCompanion,
    badge = "Active Matrix"
  ),
  SettingsCategoryItem(
    id = "permissions",
    title = "Permission Center",
    description = "Audit Android capabilities, runtime grants, and OS access controls",
    icon = Icons.Default.Security,
    targetScreen = Screen.SettingsPermissions,
    badge = "12 Capabilities"
  ),
  SettingsCategoryItem(
    id = "privacy",
    title = "Privacy Center",
    description = "Hardware microphone, camera, location, notifications, and memory privacy audit",
    icon = Icons.Default.Security,
    targetScreen = Screen.SettingsPrivacy,
    badge = "Protected"
  ),
  SettingsCategoryItem(
    id = "ai_providers",
    title = "AI Providers & API Keys",
    description = "Primary, fallback, vision, voice, and offline inference providers",
    icon = Icons.Default.Key,
    targetScreen = Screen.SettingsAiProviders,
    badge = "Architecture Ready"
  ),
  SettingsCategoryItem(
    id = "voice_speech",
    title = "Voice & Speech",
    description = "TTS/STT voices, speed, pitch, language, and audio hardware pipeline",
    icon = Icons.Default.RecordVoiceOver,
    targetScreen = Screen.SettingsVoiceSpeech
  ),
  SettingsCategoryItem(
    id = "overlay",
    title = "Overlay Assistant",
    description = "Floating HUD orb, waveform, and system-wide assistant architectures",
    icon = Icons.Default.DisplaySettings,
    targetScreen = Screen.SettingsOverlay,
    badge = "Future"
  ),
  SettingsCategoryItem(
    id = "identity",
    title = "Agent Identity",
    description = "Name, title, callsign, and display designations",
    icon = Icons.Default.Person,
    targetScreen = Screen.SettingsAgentIdentity
  ),
  SettingsCategoryItem(
    id = "personality",
    title = "Personality & Modes",
    description = "Tactical response brevity, conversational temperament, and autonomy",
    icon = Icons.Default.Tune,
    targetScreen = Screen.SettingsPersonality
  ),
  SettingsCategoryItem(
    id = "offline",
    title = "Online / Offline",
    description = "Local on-device inference routing and network fallback policy",
    icon = Icons.Default.WifiOff,
    targetScreen = Screen.SettingsOffline
  ),
  SettingsCategoryItem(
    id = "appearance",
    title = "Appearance",
    description = "Cyber space palette, HUD telemetry density, and animation speeds",
    icon = Icons.Default.Palette,
    targetScreen = Screen.SettingsAppearance
  ),
  SettingsCategoryItem(
    id = "about",
    title = "About Aegis AI",
    description = "System architecture manifest and release version",
    icon = Icons.Default.Extension,
    targetScreen = Screen.SettingsAbout
  ),
  SettingsCategoryItem(
    id = "update",
    title = "Agent Update",
    description = "Check official GitHub release updates, changelogs, and APK installer",
    icon = Icons.Default.SystemUpdate,
    targetScreen = Screen.SettingsUpdate,
    badge = "OTA Available"
  )
)

/**
 * Settings Home Screen with category cards that navigate to dedicated pages.
 */
@Composable
fun SettingsHomeScreen(
  onNavigateTo: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    item {
      Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
          text = "CONFIGURATION MATRIX // SETTINGS",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            color = CyanNeon
          )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Modular settings architecture for personal agent foundation.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    }

    items(SettingsCategories, key = { it.id }) { category ->
      GlassPanel(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onNavigateTo(category.targetScreen) }
          .testTag("settings_category_${category.id}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = category.icon,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
              )

              if (category.badge != null) {
                StatusBadge(text = category.badge, indicatorColor = ElectricBlue, isPulsing = false)
              }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = category.description,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
              )
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
    }
  }
}
