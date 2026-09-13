package com.example.core.wakeword

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Operational mode of voice activation.
 */
enum class VoiceTriggerMode(val label: String, val description: String) {
  MANUAL("Manual Tap Mode", "Tap central AI core button to trigger voice listening."),
  WAKE_WORD("Wake-Word Mode", "Continuous on-device wake-word detection (Requires dedicated model engine).")
}

/**
 * Wake Word Architecture States adhering to Section 19.
 */
enum class WakeWordState(val label: String, val description: String) {
  DISABLED("Disabled", "Wake word detection is turned off in settings."),
  ARMED("Armed", "Engine armed and awaiting acoustic keyword match."),
  DETECTED("Detected", "Keyword detected above confidence threshold."),
  LISTENING("Listening", "Passing audio stream to Speech Recognizer."),
  PROCESSING("Processing", "Speech input being processed by AI Core."),
  TIMEOUT("Timeout", "Keyword spotting window timed out without utterance."),
  ERROR("Error", "Wake word unavailable: Keyword spotter model not installed on device.")
}

/**
 * Configuration options for Wake-Word engine (Section 20 & 21).
 */
data class WakeWordConfig(
  val isWakeWordEnabled: Boolean = false,
  val activePhrase: String = "Hey Nexus",
  val customPhrase: String = "",
  val sensitivity: Int = 75, // 1..100
  val confidenceThreshold: Float = 0.82f, // False activation protection
  val cooldownSeconds: Int = 4, // Duplicate-trigger protection
  val isBackgroundDetectionEnabled: Boolean = false,
  val isVoiceConfirmationEnabled: Boolean = true,
  val language: String = "English"
)

/**
 * Replaceable Wake Word Engine abstraction.
 * Prepares the architecture for on-device keyword spotting (e.g. Porcupine, OpenWakeWord)
 * without fabricating working detection when uninstalled.
 */
interface WakeWordEngine {
  val isAvailable: Boolean
  val statusMessage: String
  val state: StateFlow<WakeWordState>
  val isListening: StateFlow<Boolean>
  val supportedPhrases: List<String>
  val activePhrase: StateFlow<String>
  val config: StateFlow<WakeWordConfig>

  fun start(onWakeWordDetected: (String) -> Unit)
  fun stop()
  fun setActivePhrase(phrase: String)
  fun updateConfig(config: WakeWordConfig)
}

/**
 * Truthful default wake-word engine.
 * Transparently reports that an on-device keyword model is unconfigured,
 * fulfilling Section 19: "show 'Wake word unavailable' instead of pretending it works."
 */
class DefaultWakeWordEngine : WakeWordEngine {
  override val isAvailable: Boolean = false
  override val statusMessage: String = "Wake word unavailable (On-device keyword model not installed)"

  private val _state = MutableStateFlow(WakeWordState.ERROR)
  override val state: StateFlow<WakeWordState> = _state.asStateFlow()

  private val _isListening = MutableStateFlow(false)
  override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  override val supportedPhrases: List<String> = listOf(
    "Hey Nexus",
    "Nexus",
    "Hey Aegis",
    "Aegis",
    "Hey Maya",
    "Computer"
  )

  private val _activePhrase = MutableStateFlow("Hey Nexus")
  override val activePhrase: StateFlow<String> = _activePhrase.asStateFlow()

  private val _config = MutableStateFlow(WakeWordConfig())
  override val config: StateFlow<WakeWordConfig> = _config.asStateFlow()

  override fun start(onWakeWordDetected: (String) -> Unit) {
    // Truthfully report error/unavailable without fake detection
    _isListening.value = false
    _state.value = WakeWordState.ERROR
  }

  override fun stop() {
    _isListening.value = false
    _state.value = WakeWordState.DISABLED
  }

  override fun setActivePhrase(phrase: String) {
    if (phrase in supportedPhrases || phrase.isNotBlank()) {
      _activePhrase.value = phrase
      _config.value = _config.value.copy(activePhrase = phrase)
    }
  }

  override fun updateConfig(config: WakeWordConfig) {
    _config.value = config
    _activePhrase.value = config.activePhrase
    if (!config.isWakeWordEnabled) {
      _state.value = WakeWordState.DISABLED
    } else if (!isAvailable) {
      _state.value = WakeWordState.ERROR
    }
  }
}
