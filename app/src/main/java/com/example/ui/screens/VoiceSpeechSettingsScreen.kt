package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.wakeword.WakeWordConfig
import com.example.core.wakeword.WakeWordEngine
import com.example.core.wakeword.WakeWordState
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated Voice & Speech Settings Page.
 * Configures TTS gender, speed, pitch, language, voice provider,
 * Wake Word engine (Section 19-21), and Background Voice (Section 18).
 */
@Composable
fun VoiceSpeechSettingsScreen(
  onBack: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val companion = agentController?.companionEngine
  val wakeWordEngine = companion?.wakeWordEngine

  val ttsEngine = remember {
    com.example.core.tts.AndroidTextToSpeechEngine(context)
  }

  val wakeWordConfig by wakeWordEngine?.config?.collectAsState() ?: remember {
    mutableStateOf(WakeWordConfig())
  }
  val wakeWordState by wakeWordEngine?.state?.collectAsState() ?: remember {
    mutableStateOf(WakeWordState.ERROR)
  }

  val bgVoiceActive by companion?.backgroundVoiceController?.isBackgroundVoiceActive?.collectAsState() ?: remember {
    mutableStateOf(false)
  }
  val bgVoiceStatus by companion?.backgroundVoiceController?.statusMessage?.collectAsState() ?: remember {
    mutableStateOf("Standby")
  }

  var selectedGender by remember { mutableStateOf("Female") }
  var selectedProvider by remember { mutableStateOf("Android System TTS") }
  var speakingRate by remember { mutableFloatStateOf(1.0f) }
  var voicePitch by remember { mutableFloatStateOf(1.0f) }
  var testNotice by remember { mutableStateOf<String?>(null) }
  var customWakeWordInput by remember { mutableStateOf(wakeWordConfig.activePhrase) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("voice_speech_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyanNeon
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "VOCAL SYNTHESIS // SPEECH",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            )
          )
          Text(
            text = "Voice & Acoustic Parameters",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
        }
      }
    }

    // 1. Wake Word Architecture (Section 19, 20, 21)
    item {
      Text(
        text = "WAKE WORD DETECTION (SECTION 19-21)",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      FuturisticCard(
        headerText = "Acoustic Wake Word Engine",
        badgeText = wakeWordState.label
      ) {
        // Truthful reporting notice
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (wakeWordEngine?.isAvailable == true) Icons.Default.Mic else Icons.Default.Warning,
            contentDescription = null,
            tint = if (wakeWordEngine?.isAvailable == true) CyanNeon else StatusWarning,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = wakeWordEngine?.statusMessage ?: "Wake word unavailable: Keyword model not installed on device",
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (wakeWordEngine?.isAvailable == true) CyanNeon else StatusWarning
            )
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wake word toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Wake Word Activation",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = "Continuous on-device keyword spotting",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
          }
          Switch(
            checked = wakeWordConfig.isWakeWordEnabled,
            onCheckedChange = {
              wakeWordEngine?.updateConfig(wakeWordConfig.copy(isWakeWordEnabled = it))
            },
            colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
          )
        }

        if (wakeWordConfig.isWakeWordEnabled) {
          Spacer(modifier = Modifier.height(10.dp))

          // Wake word phrase selection
          Text("Active Wake Phrase", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("Hey Aegis", "Hey Nexus", "Computer").forEach { phrase ->
              val isSelected = wakeWordConfig.activePhrase == phrase
              FuturisticButton(
                text = phrase,
                onClick = {
                  wakeWordEngine?.updateConfig(wakeWordConfig.copy(activePhrase = phrase))
                  customWakeWordInput = phrase
                },
                modifier = Modifier.weight(1f),
                isPrimary = isSelected
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sensitivity slider
          Text(
            text = "Detection Sensitivity: ${wakeWordConfig.sensitivity}%",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
          )
          Slider(
            value = wakeWordConfig.sensitivity.toFloat(),
            onValueChange = {
              wakeWordEngine?.updateConfig(wakeWordConfig.copy(sensitivity = it.toInt()))
            },
            valueRange = 10f..100f,
            steps = 9,
            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
          )

          // Background detection & confirmation toggles
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Background Detection", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Switch(
              checked = wakeWordConfig.isBackgroundDetectionEnabled,
              onCheckedChange = {
                wakeWordEngine?.updateConfig(wakeWordConfig.copy(isBackgroundDetectionEnabled = it))
              },
              colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Voice Audio Confirmation", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Switch(
              checked = wakeWordConfig.isVoiceConfirmationEnabled,
              onCheckedChange = {
                wakeWordEngine?.updateConfig(wakeWordConfig.copy(isVoiceConfirmationEnabled = it))
              },
              colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
            )
          }
        }
      }
    }

    // 2. Background Voice Controller (Section 18)
    item {
      FuturisticCard(
        headerText = "Background Voice Controller",
        badgeText = if (bgVoiceActive) "ACTIVE" else "STANDBY"
      ) {
        Text(
          text = "Background voice coordinates the microphone pipeline across Android version constraints, foreground service types, and power management states.",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Status: $bgVoiceStatus",
            style = MaterialTheme.typography.bodySmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold)
          )
          if (bgVoiceActive) {
            FuturisticButton(
              text = "Stop Mic",
              onClick = { companion?.backgroundVoiceController?.stopBackgroundListening() },
              isPrimary = false
            )
          } else {
            FuturisticButton(
              text = "Start Mic",
              onClick = {
                companion?.backgroundVoiceController?.startBackgroundListening { phrase ->
                  agentController?.startVoiceSession()
                }
              },
              isPrimary = true
            )
          }
        }
      }
    }

    // 3. Voice Gender / Timbre
    item {
      FuturisticCard(headerText = "Voice Timbre & Character", badgeText = selectedGender) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("Female (Aegis-Alpha)", "Male (Nexus-Prime)", "Synthetic Neural").forEach { genderOption ->
            val isSelected = selectedGender.startsWith(genderOption.take(4))
            Surface(
              shape = CyberTokens.shapeSmall,
              color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color(0x33101E3D),
              border = BorderStroke(
                CyberTokens.borderHairline,
                if (isSelected) CyanNeon else Color(0x3300F0FF)
              ),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedGender = genderOption }
                .testTag("timbre_${genderOption.take(4).lowercase()}")
            ) {
              Text(
                text = genderOption.split(" ").first(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) CyanNeon else TextSecondary
                ),
                modifier = Modifier.padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
      }
    }

    // 4. Rate & Pitch Sliders
    item {
      FuturisticCard(headerText = "Speaking Speed: ${String.format("%.1f", speakingRate)}x") {
        Slider(
          value = speakingRate,
          onValueChange = { speakingRate = it },
          valueRange = 0.5f..2.0f,
          steps = 15,
          colors = SliderDefaults.colors(
            thumbColor = CyanNeon,
            activeTrackColor = CyanNeon,
            inactiveTrackColor = Color(0x3300F0FF)
          )
        )
      }
    }

    item {
      FuturisticCard(headerText = "Voice Pitch Modulation: ${String.format("%.1f", voicePitch)}x") {
        Slider(
          value = voicePitch,
          onValueChange = { voicePitch = it },
          valueRange = 0.5f..1.5f,
          steps = 10,
          colors = SliderDefaults.colors(
            thumbColor = HologramViolet,
            activeTrackColor = HologramViolet,
            inactiveTrackColor = Color(0x33A855F7)
          )
        )
      }
    }

    // 5. Voice Preview Action
    item {
      FuturisticCard(headerText = "Preview Voice Synthesis", badgeText = "Engine Ready") {
        Text(
          text = "Preview phrase: 'Aegis system online. Standing by for command parameters.'",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FuturisticButton(
          text = "Play Voice Sample",
          onClick = {
            if (ttsEngine.isAvailable) {
              testNotice = "Synthesizing voice sample..."
              ttsEngine.setSpeechRate(speakingRate)
              ttsEngine.setPitch(voicePitch)
              ttsEngine.speak(
                text = "Aegis system operational. Speech synthesis pipeline verified.",
                onStart = { testNotice = "Playing acoustic voice sample..." },
                onComplete = { testNotice = "Voice playback completed." },
                onError = { err -> testNotice = "TTS playback error: $err" }
              )
            } else {
              testNotice = "Text-to-Speech engine initializing or unavailable on this device."
            }
          },
          modifier = Modifier.fillMaxWidth()
        )

        if (testNotice != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = testNotice ?: "",
              style = MaterialTheme.typography.labelSmall.copy(color = StatusWarning)
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
    }
  }
}
