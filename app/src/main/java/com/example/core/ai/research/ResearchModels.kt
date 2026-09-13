package com.example.core.ai.research

import java.util.UUID

enum class FactConfidence(val label: String) {
  DIRECT_FACT("Direct Fact"),
  SOURCE_SUPPORTED("Source-Supported"),
  AI_INTERPRETATION("AI Synthesis"),
  UNCERTAIN("Unverified / Ambiguous")
}

data class WebSource(
  val id: String = UUID.randomUUID().toString(),
  val title: String,
  val url: String,
  val snippet: String,
  val sourceDomain: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

data class ResearchResult(
  val query: String,
  val synthesizedAnswer: String,
  val confidence: FactConfidence,
  val sources: List<WebSource>,
  val modelUsed: String? = null,
  val searchLatencyMs: Long = 0L,
  val timestamp: Long = System.currentTimeMillis()
)

enum class NewsCategory(val label: String, val searchQuery: String) {
  WORLD("World", "world breaking news today"),
  TECHNOLOGY("Technology", "technology AI software mobile news"),
  BUSINESS("Business", "markets business economy finance news"),
  SCIENCE("Science", "science space discovery research news"),
  SECURITY("Security", "cybersecurity vulnerabilities breach updates")
}
