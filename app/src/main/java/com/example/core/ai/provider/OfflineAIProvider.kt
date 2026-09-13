package com.example.core.ai.provider

import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIErrorCode
import com.example.core.ai.model.AIModel
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.model.MediaItem
import com.example.core.ai.model.ProviderConnectionResult
import com.example.core.ai.model.ProviderConnectionStatus
import com.example.core.ai.model.ProviderHealth
import com.example.core.ai.model.ProviderMetadata
import com.example.core.context.ConversationContext
import kotlinx.coroutines.flow.Flow

/**
 * On-device Offline Neural Provider.
 * Truthfully reports when on-device LLM (Gemini Nano / MediaPipe) weights are not present.
 */
class OfflineAIProvider : AIProvider {

  override val metadata: ProviderMetadata = ProviderMetadata(
    id = "local_offline",
    name = "On-Device Neural Engine",
    company = "Local System",
    defaultModelId = "local-slm",
    supportedCapabilities = setOf(
      AICapability.TEXT_CHAT,
      AICapability.SUMMARIZATION,
      AICapability.TRANSLATION
    ),
    requiresApiKey = false,
    isLocalOffline = true
  )

  override val isConfigured: Boolean = false

  override val availableModels: List<AIModel> = listOf(
    AIModel("local-slm", "Local SLM (Not Installed)", metadata.id, metadata.supportedCapabilities, 2048, isDefault = true)
  )

  override val healthStatus: ProviderHealth = ProviderHealth.OFFLINE

  override suspend fun testConnection(): ProviderConnectionResult {
    return ProviderConnectionResult(
      status = ProviderConnectionStatus.PROVIDER_UNAVAILABLE,
      message = "On-device neural weights (MediaPipe / Gemini Nano) are not installed on this system partition."
    )
  }

  override suspend fun discoverModels(): List<AIModel> = availableModels

  override suspend fun generate(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult {
    return AIProviderResult.Error(
      errorCode = AIErrorCode.OFFLINE_UNAVAILABLE,
      message = "No on-device offline AI model is installed on this device.",
      providerId = metadata.id,
      isConfigError = true
    )
  }

  override fun generateStream(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): Flow<String>? = null

  override suspend fun generateMultimodal(
    prompt: String,
    media: List<MediaItem>,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult {
    return AIProviderResult.Error(
      errorCode = AIErrorCode.MEDIA_UNSUPPORTED,
      message = "Offline on-device multimodal models are not installed.",
      providerId = metadata.id
    )
  }
}
