package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level and sub-navigation screen routes.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
  // Primary Bottom Nav Destinations
  object MyAgent : Screen("my_agent", "Home", Icons.Default.Home)
  object Chat : Screen("chat", "Chat", Icons.Default.ChatBubbleOutline)
  object Voice : Screen("voice", "Voice", Icons.Default.Mic)
  object Vision : Screen("vision", "Vision", Icons.Default.Visibility)
  object AgentTown : Screen("agent_town", "Agent Town", Icons.Default.Domain)

  // Sidebar Destinations
  object AgentHub : Screen("agent_hub", "Agent Hub", Icons.Default.Hub)
  object WorldMonitor : Screen("world_monitor", "World Monitor", Icons.Default.Public)
  object Automation : Screen("automation", "Automations & Routines", Icons.Default.GraphicEq)
  object AutomationBuilder : Screen("automation/builder", "Automation Builder", Icons.Default.Tune)
  object Memory : Screen("memory", "Memory", Icons.Default.Storage)
  object Research : Screen("research", "Research", Icons.Default.Search)
  object Developer : Screen("developer", "Developer", Icons.Default.Code)
  object Creator : Screen("creator", "Creator", Icons.Default.AutoAwesome)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)

  // Settings Sub-screens
  object SettingsCompanion : Screen("settings/companion", "Companion & Presence", Icons.Default.Tune)
  object SettingsPermissions : Screen("settings/permissions", "Permission Center", Icons.Default.Security)
  object SettingsPrivacy : Screen("settings/privacy", "Privacy Center", Icons.Default.Security)
  object SettingsVoiceSpeech : Screen("settings/voice_speech", "Voice & Speech", Icons.Default.RecordVoiceOver)
  object SettingsAiProviders : Screen("settings/ai_providers", "AI Providers & API Keys", Icons.Default.Key)
  object SettingsOverlay : Screen("settings/overlay", "Overlay Assistant", Icons.Default.DisplaySettings)
  object SettingsAgentIdentity : Screen("settings/identity", "Agent Identity", Icons.Default.Person)
  object SettingsPersonality : Screen("settings/personality", "Personality & Modes", Icons.Default.Tune)
  object SettingsOffline : Screen("settings/offline", "Online / Offline", Icons.Default.WifiOff)
  object SettingsAppearance : Screen("settings/appearance", "Appearance", Icons.Default.Palette)
  object SettingsAbout : Screen("settings/about", "About Aegis AI", Icons.Default.Extension)
  object SettingsUpdate : Screen("settings/update", "Agent Update", Icons.Default.SystemUpdate)
}

val BottomNavDestinations = listOf(
  Screen.MyAgent,
  Screen.Chat,
  Screen.Voice,
  Screen.Vision,
  Screen.AgentTown
)

val SidebarDestinations = listOf(
  Screen.MyAgent,
  Screen.AgentTown,
  Screen.AgentHub,
  Screen.WorldMonitor,
  Screen.Automation,
  Screen.Chat,
  Screen.Voice,
  Screen.Vision,
  Screen.Memory,
  Screen.Research,
  Screen.Developer,
  Screen.Creator,
  Screen.Settings
)
