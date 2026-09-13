package com.example.core.ai.creator

import java.util.UUID

enum class CreatorOutputType {
  TEXT,
  CODE,
  IMAGE,
  VIDEO,
  PROJECT,
  DOCUMENT
}

data class GeneratedFile(
  val fileName: String,
  val language: String,
  val content: String
)

data class CreatorOutput(
  val id: String = UUID.randomUUID().toString(),
  val prompt: String,
  val type: CreatorOutputType,
  val title: String,
  val files: List<GeneratedFile> = emptyList(),
  val primaryText: String = "",
  val modelUsed: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)
