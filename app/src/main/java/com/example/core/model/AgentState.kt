package com.example.core.model

/**
 * Centralized Single Source of Truth for the Agent State Machine.
 * All UI, voice pipelines, and background services observe this state.
 */
enum class AgentState(
  val label: String,
  val statusDescription: String
) {
  INITIALIZING("INITIALIZING", "Initializing subsystems..."),
  IDLE("IDLE", "Ready"),
  LISTENING("LISTENING", "Listening..."),
  PROCESSING("PROCESSING", "Processing..."),
  SPEAKING("SPEAKING", "Speaking..."),
  PAUSED("PAUSED", "Session paused"),
  ERROR("ERROR", "Voice unavailable"),
  OFFLINE("OFFLINE", "Offline")
}

/**
 * Bridge mapper to preserve backward compatibility with Phase 1 CoreState.
 */
fun AgentState.toCoreState(): CoreState = when (this) {
  AgentState.INITIALIZING -> CoreState.IDLE
  AgentState.IDLE -> CoreState.IDLE
  AgentState.LISTENING -> CoreState.LISTENING
  AgentState.PROCESSING -> CoreState.THINKING
  AgentState.SPEAKING -> CoreState.SPEAKING
  AgentState.PAUSED -> CoreState.IDLE
  AgentState.ERROR -> CoreState.OFFLINE
  AgentState.OFFLINE -> CoreState.OFFLINE
}
