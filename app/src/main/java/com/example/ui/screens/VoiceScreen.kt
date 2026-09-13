package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AgentState
import com.example.core.model.CoreState
import com.example.ui.components.AiCoreOrb
import com.example.ui.components.FuturisticCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated Voice Screen focusing on the central AI Core, acoustic waveform,
 * real-time transcription, and centralized AgentState.
 */
@Composable
fun VoiceScreen(
  coreState: CoreState,
  onStateChange: (CoreState) -> Unit,
  onToggleVoice: () -> Unit,
  modifier: Modifier = Modifier,
  agentState: AgentState? = null,
  liveTranscription: String = "",
  audioAmplitude: Float = 0f,
  errorMessage: String? = null,
  userName: String = "Shahin"
) {
  val scrollState = rememberScrollState()
  val activeAgentState = agentState ?: when (coreState) {
    CoreState.IDLE -> AgentState.IDLE
    CoreState.LISTENING -> AgentState.LISTENING
    CoreState.THINKING -> AgentState.PROCESSING
    CoreState.SPEAKING -> AgentState.SPEAKING
    CoreState.OFFLINE -> AgentState.OFFLINE
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top Personal Assistant Greeting Notice
    Surface(
      shape = CyberTokens.shapeSmall,
      color = Color(0x660E1A36),
      border = BorderStroke(CyberTokens.borderHairline, CyanNeon.copy(alpha = 0.3f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = CyberTokens.spaceMedium)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.VolumeUp,
          contentDescription = null,
          tint = if (errorMessage != null) StatusAlert else CyanNeon,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column {
          Text(
            text = "Welcome back, $userName",
            style = MaterialTheme.typography.labelMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              letterSpacing = 0.3.sp
            )
          )
          Spacer(modifier = Modifier.height(1.dp))
          Text(
            text = if (errorMessage != null) {
              "VOICE ERROR: $errorMessage"
            } else {
              "Your personal AI is ready."
            },
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (errorMessage != null) StatusAlert else TextSecondary,
              fontSize = 11.sp,
              letterSpacing = 0.2.sp
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1. Central Animated AI Core
    AiCoreOrb(
      agentState = activeAgentState,
      amplitude = audioAmplitude,
      size = 180.dp,
      onClick = onToggleVoice
    )

    if (liveTranscription.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "\"$liveTranscription\"",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = CyanNeon,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center
        ),
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Voice Waveform Area
    FuturisticCard(headerText = "Acoustic Aperture", badgeText = "STT / TTS Bus") {
      WaveformVisualizer(
        coreState = coreState,
        modifier = Modifier.padding(vertical = CyberTokens.spaceSmall)
      )
      Text(
        text = if (activeAgentState == AgentState.LISTENING) {
          "Audio input buffer listening for prompt cadence..."
        } else if (activeAgentState == AgentState.SPEAKING) {
          "Synthesized vocal stream executing..."
        } else {
          "Audio hardware bus in low-power standby mode."
        },
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextTertiary,
          textAlign = TextAlign.Center,
          fontSize = 11.5.sp
        ),
        modifier = Modifier.fillMaxWidth()
      )
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))
  }
}

