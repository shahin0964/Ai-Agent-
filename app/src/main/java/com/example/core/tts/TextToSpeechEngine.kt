package com.example.core.tts

import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Observable operational states of the TTS engine.
 */
enum class TtsState {
  IDLE,
  SPEAKING,
  ERROR,
  UNAVAILABLE
}

/**
 * Representation of an available voice on the device.
 */
data class TtsVoiceOption(
  val name: String,
  val displayName: String,
  val locale: Locale,
  val isNetworkRequired: Boolean,
  val latency: Int = 0,
  val quality: Int = 0,
  val isFemaleEstimated: Boolean? = null
)

/**
 * Replaceable TTS Engine interface.
 * Abstracts Android platform TTS, allowing future external voice APIs.
 */
interface TextToSpeechEngine {
  val ttsState: StateFlow<TtsState>
  val availableVoices: StateFlow<List<TtsVoiceOption>>
  val availableLanguages: StateFlow<List<Locale>>
  val currentLanguage: StateFlow<Locale>
  val currentVoiceName: StateFlow<String?>
  val speechRate: StateFlow<Float>
  val pitch: StateFlow<Float>
  val isAvailable: Boolean

  fun speak(
    text: String,
    onStart: () -> Unit = {},
    onComplete: () -> Unit = {},
    onError: (String) -> Unit = {}
  )

  fun stop()
  fun pause()
  fun setLanguage(locale: Locale)
  fun setVoice(voiceName: String)
  fun setSpeechRate(rate: Float) // 0.5f to 2.0f
  fun setPitch(pitch: Float) // 0.5f to 2.0f
  fun release()
}
