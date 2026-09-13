package com.example.core.ai.model

import kotlinx.coroutines.flow.Flow

/**
 * Standardized AI Capabilities supported across multi-provider routing.
 */
enum class AICapability(val displayName: String) {
  TEXT_CHAT("Text & Conversational Chat"),
  REASONING("Deep Reasoning & Problem Solving"),
  WEB_RESEARCH("Web Grounding & Real-time Synthesis"),
  VISION("Visual Scene Understanding & Q&A"),
  OCR("Optical Character Recognition"),
  IMAGE_GENERATION("Image Generation & Synthesis"),
  VIDEO_GENERATION("Video Synthesis & Comprehension"),
  CODE_GENERATION("Code Generation & Engineering"),
  WEBSITE_GENERATION("Website & Full Project Assembly"),
  SUMMARIZATION("Document & Context Summarization"),
  TRANSLATION("Multilingual Translation"),
  ANALYSIS("Structured Data & Log Analysis"),
  VOICE_RESPONSE("Direct Audio & Voice Output")
}

/**
 * Descriptor for a specific model available from a provider.
 */
data class AIModel(
  val id: String,
  val name: String,
  val providerId: String,
  val supportedCapabilities: Set<AICapability>,
  val contextWindow: Int = 32768,
  val isDefault: Boolean = false,
  val isVisionSupported: Boolean = false,
  val isStreamingSupported: Boolean = true
)

/**
 * Media input item for multimodal reasoning (Vision / Audio / Document).
 */
data class MediaItem(
  val mimeType: String,
  val base64Data: String? = null,
  val bytes: ByteArray? = null,
  val uriString: String? = null,
  val description: String? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as MediaItem
    if (mimeType != other.mimeType) return false
    if (base64Data != other.base64Data) return false
    if (bytes != null) {
      if (other.bytes == null) return false
      if (!bytes.contentEquals(other.bytes)) return false
    } else if (other.bytes != null) return false
    if (uriString != other.uriString) return false
    return true
  }

  override fun hashCode(): Int {
    var result = mimeType.hashCode()
    result = 31 * result + (base64Data?.hashCode() ?: 0)
    result = 31 * result + (bytes?.contentHashCode() ?: 0)
    result = 31 * result + (uriString?.hashCode() ?: 0)
    return result
  }
}

/**
 * Execution parameters for generation requests.
 */
data class GenerateOptions(
  val modelId: String? = null,
  val temperature: Float = 0.7f,
  val maxTokens: Int = 1024,
  val systemPrompt: String? = null,
  val targetCapability: AICapability = AICapability.TEXT_CHAT,
  val requireStreaming: Boolean = false
)

/**
 * Provider connection test outcome.
 * Truthful reporting: no fake CONNECTED states.
 */
enum class ProviderConnectionStatus {
  CONNECTED,
  INVALID_KEY,
  UNAUTHORIZED,
  RATE_LIMITED,
  NETWORK_ERROR,
  PROVIDER_UNAVAILABLE,
  MODEL_UNAVAILABLE,
  UNSUPPORTED,
  NOT_CONFIGURED,
  UNKNOWN_ERROR
}

data class ProviderConnectionResult(
  val status: ProviderConnectionStatus,
  val message: String,
  val discoveredModels: List<AIModel> = emptyList(),
  val latencyMs: Long = 0L
)

/**
 * Provider health tracker.
 */
enum class ProviderHealth {
  AVAILABLE,
  DEGRADED,
  RATE_LIMITED,
  INVALID,
  OFFLINE,
  UNAVAILABLE,
  UNKNOWN
}

/**
 * Metadata about an AI Provider.
 */
data class ProviderMetadata(
  val id: String,
  val name: String,
  val company: String,
  val defaultModelId: String,
  val supportedCapabilities: Set<AICapability>,
  val requiresApiKey: Boolean = true,
  val baseUrlConfigurable: Boolean = false,
  val isLocalOffline: Boolean = false
)

/**
 * Standardized AI Errors.
 */
enum class AIErrorCode(val description: String) {
  NO_PROVIDER("No provider is configured for this capability."),
  NO_MODEL("The requested model is not available or supported."),
  INVALID_API_KEY("The provided API key was rejected or is invalid."),
  RATE_LIMIT("Provider rate limit reached. Please wait and retry."),
  NETWORK_ERROR("Unable to reach provider endpoint. Check internet connection."),
  TIMEOUT("Inference request timed out."),
  CONTENT_UNSUPPORTED("The requested content type is not supported by this model."),
  MEDIA_UNSUPPORTED("Multimodal media inputs are not supported by this provider."),
  PROVIDER_ERROR("The remote provider returned an error response."),
  OFFLINE_UNAVAILABLE("No on-device offline AI model is installed on this device."),
  GENERATION_FAILED("Failed to generate content."),
  RESEARCH_FAILED("Web research engine encountered an error."),
  MEMORY_ERROR("Memory storage subsystem error.")
}

/**
 * Provider inference response.
 */
sealed class AIProviderResult {
  data class Success(
    val text: String,
    val modelUsed: String,
    val providerId: String,
    val finishReason: String? = null,
    val usageTokens: Int? = null
  ) : AIProviderResult()

  data class Streaming(
    val flow: Flow<String>,
    val modelUsed: String,
    val providerId: String
  ) : AIProviderResult()

  data class Error(
    val errorCode: AIErrorCode,
    val message: String,
    val providerId: String,
    val isConfigError: Boolean = false
  ) : AIProviderResult()
}
