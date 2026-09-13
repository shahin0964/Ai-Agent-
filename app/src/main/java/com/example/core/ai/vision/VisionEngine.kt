package com.example.core.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.model.MediaItem
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.context.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class VisionEngine(
  private val context: Context,
  private val router: AIIntelligenceRouter
) {

  suspend fun analyzeImage(
    uri: Uri,
    query: String,
    contextState: ConversationContext = ConversationContext()
  ): VisionAnalysisResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val cleanQuery = query.ifBlank { "Describe this image in detail and identify any objects, text, or notable features." }

    val base64Data = convertUriToBase64(uri)
    if (base64Data == null) {
      return@withContext VisionAnalysisResult(
        imageUri = uri,
        prompt = cleanQuery,
        analysisText = "Failed to load or decode image from device storage.",
        latencyMs = System.currentTimeMillis() - startTime
      )
    }

    val mediaItem = MediaItem(
      mimeType = "image/jpeg",
      base64Data = base64Data,
      uriString = uri.toString()
    )

    val result = router.routeAndGenerate(
      prompt = cleanQuery,
      context = contextState,
      media = listOf(mediaItem),
      options = GenerateOptions(
        targetCapability = AICapability.VISION,
        temperature = 0.4f
      )
    )

    val (text, modelUsed) = when (result) {
      is AIProviderResult.Success -> Pair(result.text, result.modelUsed)
      is AIProviderResult.Error -> Pair("Vision analysis error: ${result.message}", null)
      is AIProviderResult.Streaming -> Pair("Analyzing image visual stream...", null)
    }

    VisionAnalysisResult(
      imageUri = uri,
      prompt = cleanQuery,
      analysisText = text,
      modelUsed = modelUsed,
      latencyMs = System.currentTimeMillis() - startTime
    )
  }

  private fun convertUriToBase64(uri: Uri): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val originalBitmap = BitmapFactory.decodeStream(inputStream)
      inputStream.close()

      if (originalBitmap == null) return null

      // Scale down if oversized (max 1280px on longest side for bandwidth efficiency)
      val maxDim = 1280
      val width = originalBitmap.width
      val height = originalBitmap.height
      val scaledBitmap = if (width > maxDim || height > maxDim) {
        val ratio = width.toFloat() / height.toFloat()
        val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
        val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
      } else {
        originalBitmap
      }

      val outputStream = ByteArrayOutputStream()
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
      val bytes = outputStream.toByteArray()
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
      null
    }
  }
}
