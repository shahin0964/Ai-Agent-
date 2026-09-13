package com.example.core.context

import com.example.core.model.AgentState
import com.example.core.model.ChatMessage
import java.util.UUID

/**
 * Lightweight, privacy-preserving conversation context.
 * Holds session memory without fabricating persistent or sensitive memories.
 */
data class ConversationContext(
  val sessionId: String = UUID.randomUUID().toString(),
  val recentMessages: List<ChatMessage> = emptyList(),
  val agentMode: String = "Standard Voice Assistant",
  val activeState: AgentState = AgentState.IDLE
) {
  fun addMessage(message: ChatMessage, maxHistory: Int = 20): ConversationContext {
    val updated = (recentMessages + message).takeLast(maxHistory)
    return copy(recentMessages = updated)
  }

  fun clear(): ConversationContext = copy(
    sessionId = UUID.randomUUID().toString(),
    recentMessages = emptyList(),
    activeState = AgentState.IDLE
  )
}
