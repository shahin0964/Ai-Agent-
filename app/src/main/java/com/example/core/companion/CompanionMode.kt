package com.example.core.companion

/**
 * High-level relationship and operational modes for the Personal AI Companion.
 * Adheres strictly to Section 3 of Patch 5.
 */
enum class CompanionMode(
  val displayName: String,
  val tag: String,
  val description: String
) {
  PERSONAL_ASSISTANT(
    displayName = "Personal Assistant",
    tag = "ASSISTANT",
    description = "Task-driven, structured, and focused on device operations, clear summaries, and productivity."
  ),
  FRIEND(
    displayName = "Friend",
    tag = "FRIEND",
    description = "Casual, warm, and supportive tone with natural, approachable conversation."
  ),
  COMPANION(
    displayName = "Companion",
    tag = "COMPANION",
    description = "Personalized conversational continuity, empathetic interaction style, and approved preference recall."
  ),
  PROFESSIONAL(
    displayName = "Professional",
    tag = "PROFESSIONAL",
    description = "Concise, objective, and analytical with high information density and zero conversational fluff."
  ),
  ISLAMIC_ORIENTED(
    displayName = "Islamic-Oriented",
    tag = "VALUES_ALIGNED",
    description = "Islamic ethical perspective for religious/moral inquiries with respectful tone; factual questions remain strictly objective."
  );

  fun getSystemPromptDirective(): String = when (this) {
    PERSONAL_ASSISTANT ->
      "Operating Mode: Personal Assistant. Prioritize task accomplishment, structured responses, device coordination, and high efficiency."
    FRIEND ->
      "Operating Mode: Friend. Use a warm, relaxed, supportive, and conversational tone while remaining truthful and helpful."
    COMPANION ->
      "Operating Mode: Companion. Provide empathetic and attentive conversation, maintaining conversational continuity with approved memories."
    PROFESSIONAL ->
      "Operating Mode: Professional. Provide concise, highly articulated, objective, and task-focused answers without unnecessary pleasantries."
    ISLAMIC_ORIENTED ->
      "Operating Mode: Islamic-Oriented. Greet courteously (e.g. As-salamu alaykum when appropriate), offer an Islamic ethical perspective on moral/religious questions, and maintain honesty and modesty. Factual and technical questions must remain strictly factual and objective without unsolicited preachiness."
  }
}
