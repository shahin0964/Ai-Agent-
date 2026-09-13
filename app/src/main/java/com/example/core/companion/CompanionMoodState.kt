package com.example.core.companion

import androidx.compose.ui.graphics.Color
import com.example.core.model.AgentState

/**
 * Simulated interaction styles, emotions, and presence for the Companion UI.
 * Displays truthful operational state, emotions, and modes with neat emoji accents.
 */
enum class CompanionMoodState(
  val label: String,
  val emoji: String,
  val styleDescriptor: String,
  val description: String,
  val colorLong: Long
) {
  ONLINE("Online", "🟢", "Connected & Ready", "System active and connected", 0xFF00E676),
  HAPPY("Happy", "😊", "Cheerful Presence", "Warm, uplifting interaction mood", 0xFFFFD600),
  ANGRY("Angry", "😠", "Strict Alert", "Strict and defensive alert stance", 0xFFFF5252),
  SAD("Sad", "😔", "Subdued Mode", "Quiet, reflective, and subdued state", 0xFF64B5F6),
  SLEEP("Sleep", "😴", "Night Standby", "Low-power scheduled rest mode", 0xFFB388FF),
  CALM("Calm", "🧘", "Composed Standby", "Serene and attentive readiness style", 0xFF00F0FF),
  LISTENING("Listening", "👂", "Acoustic Stream Active", "Microphone actively capturing operator voice", 0xFF00F0FF),
  THINKING("Thinking", "🧠", "AI Engine Processing", "Generating inference across intelligence provider", 0xFFE040FB),
  SPEAKING("Speaking", "🗣️", "Acoustic Output Active", "Text-to-Speech audio synthesizer playing back", 0xFF2979FF),
  CURIOUS("Curious", "🧐", "Inquisitive Stance", "Deep-diving research and inquiry style", 0xFF40C4FF),
  ENERGETIC("Energetic", "⚡", "High Engagement", "Dynamic and proactive interaction style", 0xFFFFAB00),
  CARING("Caring", "💖", "Attentive Care", "Gentle, helpful, and considerate style", 0xFFFF4081),
  BUSY("Busy", "⚙️", "Task Pipeline Active", "Actively executing tools, research, or capabilities", 0xFFFF9100),
  OFFLINE("Offline", "⚪", "Network Disconnected", "Running in local offline capability mode", 0xFF9E9E9E),

  // Aliases for backward compatibility
  HAPPY_STYLE("Happy", "😊", "Cheerful Presence", "Warm, uplifting interaction style", 0xFFFFD600),
  EXCITED_STYLE("Energetic", "⚡", "High Engagement", "Dynamic and proactive interaction style", 0xFFFFAB00),
  CURIOUS_STYLE("Curious", "🧐", "Inquisitive Stance", "Deep-diving research and inquiry style", 0xFF40C4FF),
  CARING_STYLE("Caring", "💖", "Attentive Care", "Gentle, helpful, and considerate interaction style", 0xFFFF4081),
  SAD_STYLE("Sad", "😔", "Subdued Demeanor", "Quiet, reflective, and reserved interaction style", 0xFF64B5F6),
  SLEEPING("Sleep", "😴", "Night Standby", "Low-power scheduled rest mode; proactive triggers suppressed", 0xFFB388FF),
  WAITING("Waiting", "⏳", "Standby for Command", "Awaiting operator voice or text instruction", 0xFF00E5FF);

  val color: Color get() = Color(colorLong)

  companion object {
    /**
     * Resolves the truthful mood state from actual application and agent states.
     */
    fun fromAgentState(
      agentState: AgentState,
      isSleeping: Boolean = false,
      isOffline: Boolean = false,
      preferredStyle: CompanionMoodState = ONLINE
    ): CompanionMoodState {
      if (isOffline) return OFFLINE
      if (isSleeping) return SLEEP

      return when (agentState) {
        AgentState.LISTENING -> LISTENING
        AgentState.PROCESSING -> THINKING
        AgentState.SPEAKING -> SPEAKING
        AgentState.INITIALIZING -> BUSY
        AgentState.ERROR -> ANGRY
        AgentState.OFFLINE -> OFFLINE
        AgentState.PAUSED -> CALM
        AgentState.IDLE -> preferredStyle
      }
    }
  }
}

