package com.example.core.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Standard Android SpeechRecognizer implementation of VoiceEngine.
 * Uses official Android Speech API with full lifecycle safety.
 */
class AndroidSpeechRecognizerEngine(
  private val context: Context
) : VoiceEngine {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var speechRecognizer: SpeechRecognizer? = null

  private val _isListening = MutableStateFlow(false)
  override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
  override val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

  private val _audioAmplitude = MutableStateFlow(0f)
  override val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

  override val isAvailable: Boolean
    get() = SpeechRecognizer.isRecognitionAvailable(context)

  private var onPartialCallback: ((String) -> Unit)? = null
  private var onResultCallback: ((String) -> Unit)? = null
  private var onErrorCallback: ((VoiceError) -> Unit)? = null

  override fun startListening(
    onPartialResult: (String) -> Unit,
    onResult: (String) -> Unit,
    onError: (VoiceError) -> Unit
  ) {
    mainHandler.post {
      // 1. Verify Microphone Permission
      val hasMic = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED

      if (!hasMic) {
        val err = VoiceError.PermissionDenied
        _transcriptionState.value = TranscriptionState.Error(err)
        onError(err)
        return@post
      }

      // 2. Verify SpeechRecognizer Availability
      if (!isAvailable) {
        val err = VoiceError.Unavailable
        _transcriptionState.value = TranscriptionState.Error(err)
        onError(err)
        return@post
      }

      this.onPartialCallback = onPartialResult
      this.onResultCallback = onResult
      this.onErrorCallback = onError

      // Recreate recognizer cleanly if needed
      safeDestroyRecognizer()

      try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
          speechRecognizer = it
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _audioAmplitude.value = 0f
          }

          override fun onBeginningOfSpeech() {
            _isListening.value = true
          }

          override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical RMS range (approx -2dB to 10dB) to 0.0f..1.0f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioAmplitude.value = normalized
          }

          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            _isListening.value = false
            _audioAmplitude.value = 0f
          }

          override fun onError(error: Int) {
            _isListening.value = false
            _audioAmplitude.value = 0f
            val voiceError = mapErrorCode(error)
            _transcriptionState.value = TranscriptionState.Error(voiceError)
            onErrorCallback?.invoke(voiceError)
          }

          override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioAmplitude.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""
            if (recognizedText.isNotBlank()) {
              _transcriptionState.value = TranscriptionState.Final(recognizedText)
              onResultCallback?.invoke(recognizedText)
            } else {
              val err = VoiceError.NoMatch
              _transcriptionState.value = TranscriptionState.Error(err)
              onErrorCallback?.invoke(err)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim() ?: ""
            if (partialText.isNotBlank()) {
              _transcriptionState.value = TranscriptionState.Partial(partialText)
              onPartialCallback?.invoke(partialText)
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.startListening(intent)
      } catch (e: Exception) {
        _isListening.value = false
        _audioAmplitude.value = 0f
        val err = VoiceError.Unknown(-1, e.localizedMessage ?: "Failed to start speech recognizer")
        _transcriptionState.value = TranscriptionState.Error(err)
        onError(err)
      }
    }
  }

  override fun stopListening() {
    mainHandler.post {
      try {
        speechRecognizer?.stopListening()
      } catch (_: Exception) {}
      _isListening.value = false
      _audioAmplitude.value = 0f
    }
  }

  override fun cancelListening() {
    mainHandler.post {
      try {
        speechRecognizer?.cancel()
      } catch (_: Exception) {}
      _isListening.value = false
      _audioAmplitude.value = 0f
      _transcriptionState.value = TranscriptionState.Idle
    }
  }

  override fun release() {
    mainHandler.post {
      safeDestroyRecognizer()
      _isListening.value = false
      _audioAmplitude.value = 0f
      _transcriptionState.value = TranscriptionState.Idle
    }
  }

  private fun safeDestroyRecognizer() {
    try {
      speechRecognizer?.setRecognitionListener(null)
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
    speechRecognizer = null
  }

  private fun mapErrorCode(error: Int): VoiceError = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> VoiceError.AudioRecordingError
    SpeechRecognizer.ERROR_CLIENT -> VoiceError.ClientError
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PermissionDenied
    SpeechRecognizer.ERROR_NETWORK -> VoiceError.NetworkError
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceError.NetworkError
    SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.NoMatch
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.RecognizerBusy
    SpeechRecognizer.ERROR_SERVER -> VoiceError.NetworkError
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.Timeout
    else -> VoiceError.Unknown(error, "Speech recognition error code: $error")
  }
}
