package com.example.core.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.core.model.AgentState
import com.example.core.orchestrator.AgentOrchestrator
import com.example.core.wakeword.WakeWordEngine
import com.example.core.wakeword.WakeWordState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller orchestrating background voice processing according to Section 18.
 * Continuous/background microphone capture strictly checks permissions, foreground-service status,
 * Android OS restrictions, and user settings.
 */
class BackgroundVoiceController(
  private val context: Context,
  val wakeWordEngine: WakeWordEngine,
  private val backgroundPresenceController: BackgroundPresenceController
) {
  private val _isBackgroundVoiceActive = MutableStateFlow(false)
  val isBackgroundVoiceActive: StateFlow<Boolean> = _isBackgroundVoiceActive.asStateFlow()

  private val _statusMessage = MutableStateFlow("Background voice standby")
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  fun canOperateBackgroundVoice(): Boolean {
    // 1. Microphone permission
    val hasMic = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasMic) return false

    // 2. Background service must be running for background microphone access
    if (!backgroundPresenceController.isRunning) return false

    // 3. Wake word engine must be available
    if (!wakeWordEngine.isAvailable) return false

    return true
  }

  fun startBackgroundListening(onWakeWordTriggered: (String) -> Unit): Boolean {
    if (!wakeWordEngine.config.value.isBackgroundDetectionEnabled) {
      _statusMessage.value = "Background detection disabled in settings"
      return false
    }

    if (!canOperateBackgroundVoice()) {
      _statusMessage.value = if (!wakeWordEngine.isAvailable) {
        "Wake word model not installed"
      } else if (!backgroundPresenceController.isRunning) {
        "Background service required"
      } else {
        "Microphone permission required"
      }
      return false
    }

    wakeWordEngine.start { phrase ->
      onWakeWordTriggered(phrase)
    }
    _isBackgroundVoiceActive.value = true
    _statusMessage.value = "Active background listening"
    return true
  }

  fun stopBackgroundListening() {
    wakeWordEngine.stop()
    _isBackgroundVoiceActive.value = false
    _statusMessage.value = "Background voice stopped"
  }
}
