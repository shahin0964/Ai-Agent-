package com.example.core.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Standard Android TextToSpeech implementation of TextToSpeechEngine.
 * Queries actual installed TTS voices and handles playback callbacks.
 */
class AndroidTextToSpeechEngine(
  private val context: Context
) : TextToSpeechEngine, TextToSpeech.OnInitListener {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var textToSpeech: TextToSpeech? = null

  private val _ttsState = MutableStateFlow(TtsState.UNAVAILABLE)
  override val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

  private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
  override val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

  private val _availableLanguages = MutableStateFlow<List<Locale>>(listOf(Locale.getDefault()))
  override val availableLanguages: StateFlow<List<Locale>> = _availableLanguages.asStateFlow()

  private val _currentLanguage = MutableStateFlow(Locale.getDefault())
  override val currentLanguage: StateFlow<Locale> = _currentLanguage.asStateFlow()

  private val _currentVoiceName = MutableStateFlow<String?>(null)
  override val currentVoiceName: StateFlow<String?> = _currentVoiceName.asStateFlow()

  private val _speechRate = MutableStateFlow(1.0f)
  override val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

  private val _pitch = MutableStateFlow(1.0f)
  override val pitch: StateFlow<Float> = _pitch.asStateFlow()

  override val isAvailable: Boolean
    get() = _ttsState.value != TtsState.UNAVAILABLE

  private var activeUtteranceId: String? = null
  private var onStartCallback: (() -> Unit)? = null
  private var onCompleteCallback: (() -> Unit)? = null
  private var onErrorCallback: ((String) -> Unit)? = null

  init {
    try {
      textToSpeech = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      _ttsState.value = TtsState.UNAVAILABLE
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val tts = textToSpeech ?: return
      _ttsState.value = TtsState.IDLE

      // Set default language
      val defaultLocale = Locale.getDefault()
      val langResult = tts.setLanguage(defaultLocale)
      if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
        _currentLanguage.value = defaultLocale
      }

      // Query available voices safely
      try {
        val rawVoices = tts.voices
        if (!rawVoices.isNullOrEmpty()) {
          val mapped = rawVoices.map { voice ->
            val isFemale = when {
              voice.name.contains("female", ignoreCase = true) || voice.name.contains("-f-", ignoreCase = true) -> true
              voice.name.contains("male", ignoreCase = true) || voice.name.contains("-m-", ignoreCase = true) -> false
              else -> null
            }
            TtsVoiceOption(
              name = voice.name,
              displayName = formatVoiceDisplayName(voice),
              locale = voice.locale,
              isNetworkRequired = voice.isNetworkConnectionRequired,
              latency = voice.latency,
              quality = voice.quality,
              isFemaleEstimated = isFemale
            )
          }.sortedBy { it.displayName }
          _availableVoices.value = mapped

          val currentVoice = tts.voice
          _currentVoiceName.value = currentVoice?.name
        }
      } catch (_: Exception) {}

      // Query available languages safely
      try {
        val langs = tts.availableLanguages
        if (!langs.isNullOrEmpty()) {
          _availableLanguages.value = langs.toList().sortedBy { it.displayName }
        }
      } catch (_: Exception) {}

      // Register UtteranceProgressListener
      tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          mainHandler.post {
            _ttsState.value = TtsState.SPEAKING
            onStartCallback?.invoke()
          }
        }

        override fun onDone(utteranceId: String?) {
          mainHandler.post {
            if (_ttsState.value == TtsState.SPEAKING) {
              _ttsState.value = TtsState.IDLE
            }
            onCompleteCallback?.invoke()
          }
        }

        override fun onError(utteranceId: String?) {
          mainHandler.post {
            _ttsState.value = TtsState.ERROR
            onErrorCallback?.invoke("Playback error encountered.")
          }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          mainHandler.post {
            _ttsState.value = TtsState.ERROR
            onErrorCallback?.invoke("TTS playback error (code: $errorCode)")
          }
        }
      })
    } else {
      _ttsState.value = TtsState.UNAVAILABLE
    }
  }

  override fun speak(
    text: String,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit
  ) {
    if (text.isBlank()) {
      onComplete()
      return
    }

    val tts = textToSpeech
    if (tts == null || _ttsState.value == TtsState.UNAVAILABLE) {
      onError("TTS is unavailable on this device.")
      return
    }

    this.onStartCallback = onStart
    this.onCompleteCallback = onComplete
    this.onErrorCallback = onError

    val utteranceId = UUID.randomUUID().toString()
    activeUtteranceId = utteranceId

    val params = Bundle().apply {
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
    }

    try {
      tts.setSpeechRate(_speechRate.value)
      tts.setPitch(_pitch.value)
      val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
      if (result != TextToSpeech.SUCCESS) {
        _ttsState.value = TtsState.ERROR
        onError("Failed to queue TTS speech.")
      }
    } catch (e: Exception) {
      _ttsState.value = TtsState.ERROR
      onError(e.localizedMessage ?: "TTS speak exception")
    }
  }

  override fun stop() {
    try {
      textToSpeech?.stop()
    } catch (_: Exception) {}
    if (_ttsState.value == TtsState.SPEAKING) {
      _ttsState.value = TtsState.IDLE
    }
  }

  override fun pause() {
    // Android platform TTS doesn't support pause on older engines; stop gracefully
    stop()
  }

  override fun setLanguage(locale: Locale) {
    _currentLanguage.value = locale
    try {
      textToSpeech?.language = locale
    } catch (_: Exception) {}
  }

  override fun setVoice(voiceName: String) {
    _currentVoiceName.value = voiceName
    try {
      val voiceMatch = textToSpeech?.voices?.find { it.name == voiceName }
      if (voiceMatch != null) {
        textToSpeech?.voice = voiceMatch
      }
    } catch (_: Exception) {}
  }

  override fun setSpeechRate(rate: Float) {
    val clamped = rate.coerceIn(0.5f, 2.0f)
    _speechRate.value = clamped
    try {
      textToSpeech?.setSpeechRate(clamped)
    } catch (_: Exception) {}
  }

  override fun setPitch(pitch: Float) {
    val clamped = pitch.coerceIn(0.5f, 2.0f)
    _pitch.value = clamped
    try {
      textToSpeech?.setPitch(clamped)
    } catch (_: Exception) {}
  }

  override fun release() {
    try {
      textToSpeech?.stop()
      textToSpeech?.shutdown()
    } catch (_: Exception) {}
    textToSpeech = null
    _ttsState.value = TtsState.UNAVAILABLE
  }

  private fun formatVoiceDisplayName(voice: Voice): String {
    val country = voice.locale.displayCountry.takeIf { it.isNotBlank() } ?: voice.locale.country
    val lang = voice.locale.displayLanguage.takeIf { it.isNotBlank() } ?: voice.locale.language
    val genderTag = when {
      voice.name.contains("female", ignoreCase = true) || voice.name.contains("-f-", ignoreCase = true) -> " (Female)"
      voice.name.contains("male", ignoreCase = true) || voice.name.contains("-m-", ignoreCase = true) -> " (Male)"
      else -> ""
    }
    return "$lang ($country)$genderTag"
  }
}
