package com.example.core.model

/**
 * Structural states for the central AI Core.
 * For Phase 1, states represent UI and operational modes.
 */
enum class CoreState(
  val label: String,
  val statusDescription: String
) {
  IDLE("IDLE", "UI Ready - Standby"),
  LISTENING("LISTENING", "Awaiting Audio Stream"),
  THINKING("THINKING", "Processing Pipeline"),
  SPEAKING("SPEAKING", "Synthesizing Response"),
  OFFLINE("OFFLINE", "Core Disconnected")
}

/**
 * Safe, high-level system activity representation.
 * Exposes only clean operational state without fake claims.
 */
data class SystemActivity(
  val label: String = "Standby",
  val detail: String = "System initialized and awaiting command",
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Agent Identity descriptor.
 */
data class AgentIdentity(
  val name: String = "Nexus Core",
  val designation: String = "Personal AI System",
  val architectureVersion: String = "1.0.0",
  val statusTruth: String = "Standing by for instructions"
)

/**
 * Conversational message entity ready for Phase 2 chat engine.
 */
data class ChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isError: Boolean = false,
  val isStreaming: Boolean = false
)

