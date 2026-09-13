package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AgentIdentitySettingsScreen(
  onBack: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val personality = agentController?.aiEngine?.personalityEngine
  val currentIdentity by personality?.identity?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.ai.personality.AgentIdentityConfig())
  }

  var agentName by remember(currentIdentity) { mutableStateOf(currentIdentity.name) }
  var nickname by remember(currentIdentity) { mutableStateOf(currentIdentity.nickname) }
  var selectedStyle by remember(currentIdentity) { mutableStateOf(currentIdentity.speakingStyle) }
  var selectedLength by remember(currentIdentity) { mutableStateOf(currentIdentity.responseLength) }
  var selectedLanguage by remember(currentIdentity) { mutableStateOf(currentIdentity.language) }
  var savedStatus by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    SubSettingsHeader(title = "Agent Identity", onBack = onBack)

    FuturisticCard(headerText = "Agent Nomenclature", badgeText = "Customizable") {
      Text("Agent Display Name", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = agentName,
        onValueChange = { agentName = it; savedStatus = false },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanNeon,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Operator Nickname (How Agent Addresses You)", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = nickname,
        onValueChange = { nickname = it; savedStatus = false },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanNeon,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Speaking Style", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.core.ai.personality.SpeakingStyle.values().forEach { style ->
          val isSel = selectedStyle == style
          FuturisticButton(
            text = style.displayName.split(" ").first(),
            onClick = { selectedStyle = style; savedStatus = false },
            modifier = Modifier.weight(1f),
            isPrimary = isSel
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text("Response Length Target", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.core.ai.personality.ResponseLength.values().forEach { len ->
          val isSel = selectedLength == len
          FuturisticButton(
            text = len.displayName.split(" ").first(),
            onClick = { selectedLength = len; savedStatus = false },
            modifier = Modifier.weight(1f),
            isPrimary = isSel
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text("Language & Dialect", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.core.ai.personality.AgentLanguage.values().forEach { lang ->
          val isSel = selectedLanguage == lang
          FuturisticButton(
            text = lang.displayName.split(" ").first(),
            onClick = { selectedLanguage = lang; savedStatus = false },
            modifier = Modifier.weight(1f),
            isPrimary = isSel
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      FuturisticButton(
        text = "Save Identity Configuration",
        onClick = {
          val updated = currentIdentity.copy(
            name = agentName.trim(),
            nickname = nickname.trim(),
            speakingStyle = selectedStyle,
            responseLength = selectedLength,
            language = selectedLanguage
          )
          personality?.updateIdentity(updated)
          agentController?.setAgentName(agentName.trim())
          savedStatus = true
        },
        modifier = Modifier.fillMaxWidth()
      )

      if (savedStatus) {
        Spacer(modifier = Modifier.height(8.dp))
        StatusBadge(text = "Identity Updated Locally", indicatorColor = ElectricBlue, isPulsing = false)
      }
    }
  }
}

@Composable
fun PersonalitySettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    SubSettingsHeader(title = "Personality & Modes", onBack = onBack)

    FuturisticCard(headerText = "Tactical Brevity Mode", badgeText = "Sci-Fi HUD") {
      Text(
        text = "Optimized for concise telemetry responses, high information density, and low conversational fluff.",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
      )
    }

    FuturisticCard(headerText = "Autonomy Boundaries", badgeText = "Enforced") {
      Text(
        text = "Actions that alter device state, perform outbound calls, or transmit external data will always require explicit confirmation prompt.",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
      )
    }
  }
}

@Composable
fun OfflineSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    SubSettingsHeader(title = "Online / Offline Routing", onBack = onBack)

    FuturisticCard(headerText = "Hybrid Dual-Core Strategy", badgeText = "Privacy First") {
      Text(
        text = "When offline or in airplane mode, the architecture automatically falls back to local on-device models (e.g. MediaPipe / Gemini Nano / GGUF) for localized device control.",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
      )
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Local offline on-device inference engine with automatic network status detection.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    }
  }
}

@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val currentThemeId by com.example.ui.theme.AppThemeState.currentTheme.collectAsState()
  val theme = com.example.ui.theme.activeTheme

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    SubSettingsHeader(title = "Appearance & Theme System", onBack = onBack)

    Text(
      text = "CYBERNETIC COLOR PALETTES",
      style = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        color = theme.primary
      )
    )

    com.example.ui.theme.AppThemeId.entries.forEach { appTheme ->
      val isSelected = appTheme == currentThemeId
      FuturisticCard(
        headerText = appTheme.displayName,
        badgeText = if (isSelected) "ACTIVE PALETTE" else "AVAILABLE",
        badgeColor = if (isSelected) com.example.ui.theme.StatusReady else appTheme.primaryColor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = appTheme.description,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )

          // Color Swatches Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ThemeSwatch(color = appTheme.primaryColor, label = "Primary")
            ThemeSwatch(color = appTheme.secondaryColor, label = "Secondary")
            ThemeSwatch(color = appTheme.accentColor, label = "Accent")
            ThemeSwatch(color = appTheme.surfaceColor, label = "Surface")
          }

          Spacer(modifier = Modifier.height(4.dp))

          FuturisticButton(
            text = if (isSelected) "Currently Active" else "Apply ${appTheme.displayName}",
            onClick = {
              com.example.ui.theme.AppThemeState.setTheme(appTheme)
            },
            isPrimary = isSelected,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    FuturisticCard(headerText = "Dynamic Motion Engine", badgeText = "Hardware-Accelerated") {
      Text(
        text = "Hardware-accelerated 60/120fps vector transitions, radar sweep loops, and acoustic waveforms engineered for high efficiency and low battery consumption.",
        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
      )
    }
  }
}

@Composable
private fun ThemeSwatch(color: Color, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(color)
        .border(1.dp, Color(0xFF334155), CircleShape)
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        color = TextTertiary,
        fontSize = 8.sp
      )
    )
  }
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    SubSettingsHeader(title = "About Aegis AI", onBack = onBack)

    FuturisticCard(headerText = "System Specification", badgeText = "Release v1.0.0") {
      Text(
        text = "Aegis Personal AI Agent",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Version: 1.0.0\nTarget Android SDK: 36\nUI Framework: Jetpack Compose Material 3\nArchitecture: Clean Layered (UI → Agent → Capability → Intelligence → Automation)",
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextSecondary,
          fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
          lineHeight = 20.sp
        )
      )
    }

    FuturisticCard(headerText = "Integrated Capabilities", badgeText = "Operational") {
      Text(
        text = "• Multi-Provider AI (Gemini, Claude, OpenAI, Custom Endpoints)\n• Natural Voice Recognition & Neural TTS\n• Android System Tool Bridge & Capability Policy\n• Encrypted KeyStore Credentials & Memory Store\n• Real-Time Vision AI & OCR\n• Background Automation & Routine Engine",
        style = MaterialTheme.typography.bodySmall.copy(
          color = CyanNeon,
          lineHeight = 20.sp
        )
      )
    }
  }
}

@Composable
private fun SubSettingsHeader(title: String, onBack: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onBack, modifier = Modifier.testTag("subsettings_back_button")) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon)
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
    )
  }
}
