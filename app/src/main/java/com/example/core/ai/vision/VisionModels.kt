package com.example.core.ai.vision

import android.net.Uri

enum class VisionState {
  NO_IMAGE,
  IMAGE_SELECTED,
  CAMERA_READY,
  ANALYZING,
  RESULT,
  ERROR,
  OFFLINE
}

data class VisionAnalysisResult(
  val imageUri: Uri? = null,
  val prompt: String,
  val analysisText: String,
  val modelUsed: String? = null,
  val latencyMs: Long = 0L,
  val timestamp: Long = System.currentTimeMillis()
)
