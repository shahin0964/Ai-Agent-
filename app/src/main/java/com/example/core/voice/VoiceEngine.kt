package com.example.core.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * High-level error descriptor for voice recognition.
 * Truthfully exposes specific error reasons without exposing raw crashes.
 */
sealed class VoiceError(val userFriendlyMessage: String) {
  object PermissionDenied : VoiceError("Microphone permission is required to listen.")
  object Unavailable : VoiceError("Speech recognition is unavailable on this device.")
  object Timeout : VoiceError("No speech detected. Timeout.")
  object NoMatch : VoiceError("Could not recognize speech. Please try again.")
  object NetworkError : VoiceError("Network communication error during speech recognition.")
  object AudioRecordingError : VoiceError("Microphone hardware or audio recording error.")
  object RecognizerBusy : VoiceError("Speech recognition subsystem is busy.")
  object ClientError : VoiceError("Speech client error encountered.")
  data class Unknown(val code: Int, val details: String) : VoiceError(details)
}

/**
 * Reactive transcription state holding partial and final speech recognition.
 */
sealed class TranscriptionState {
  object Idle : TranscriptionState()
  data class Partial(val text: String) : TranscriptionState()
  data class Final(val text: String) : TranscriptionState()
  data class Error(val error: VoiceError) : TranscriptionState()
}

/**
 * Replaceable Voice Engine abstraction.
 * Allows decoupling UI from platform SpeechRecognizer, enabling future offline or cloud engines.
 */
interface VoiceEngine {
  val isListening: StateFlow<Boolean>
  val transcriptionState: StateFlow<TranscriptionState>
  val audioAmplitude: StateFlow<Float> // Normalized 0.0f..1.0f from real hardware RMS
  val isAvailable: Boolean

  fun startListening(
    onPartialResult: (String) -> Unit = {},
    onResult: (String) -> Unit,
    onError: (VoiceError) -> Unit
  )

  fun stopListening()
  fun cancelListening()
  fun release()
}
