package com.example.core.provider

/**
 * Supported provider domains for future AI orchestration.
 */
enum class ProviderDomain(val title: String, val description: String) {
  PRIMARY_AI("Primary AI", "Core reasoning, dialogue orchestration, and tool calling"),
  SECONDARY_AI("Secondary AI", "Fallback engine and fast lightweight completions"),
  OFFLINE_AI("Offline AI", "On-device model (e.g. MediaPipe / Gemini Nano / GGUF) for offline privacy"),
  VOICE_PROVIDER("Voice & Speech", "Speech-to-Text (STT) and Text-to-Speech (TTS) synthesis"),
  VISION_PROVIDER("Vision AI", "Image understanding, OCR, visual spatial analysis"),
  IMAGE_PROVIDER("Image Generation", "Diffusion model integration for concept rendering"),
  VIDEO_PROVIDER("Video Provider", "Future video comprehension and generation engine"),
  RESEARCH_PROVIDER("Research & Search", "Web groundings, citations, and search indexing")
}

/**
 * Status of an AI provider. Truthful states only!
 */
enum class ProviderStatus(val label: String) {
  NOT_CONFIGURED("Not configured"),
  TESTING("Testing connection..."),
  VERIFIED("Verified"),
  AUTHENTICATION_FAILED("Key invalid or rejected"),
  NETWORK_ERROR("Network unreachable")
}

/**
 * Provider descriptor entity.
 */
data class ProviderConfig(
  val domain: ProviderDomain,
  val selectedProviderName: String = "Unconfigured",
  val availableOptions: List<String> = listOf("None", "Google Gemini", "OpenAI", "Anthropic Claude", "Custom Endpoint", "Local On-Device"),
  val status: ProviderStatus = ProviderStatus.NOT_CONFIGURED,
  val hasApiKey: Boolean = false,
  val lastTestedTimestamp: Long? = null
)

/**
 * Repository of provider configurations.
 */
object ProviderRegistry {
  private val secureKeyStore = mutableMapOf<ProviderDomain, String>()

  fun setApiKeyForDomain(domain: ProviderDomain, key: String) {
    if (key.isNotBlank()) {
      secureKeyStore[domain] = key.trim()
    } else {
      secureKeyStore.remove(domain)
    }
  }

  fun getApiKeyForDomain(domain: ProviderDomain): String? {
    return secureKeyStore[domain]
  }

  val defaultConfigs: List<ProviderConfig> = listOf(
    ProviderConfig(
      domain = ProviderDomain.PRIMARY_AI,
      selectedProviderName = "Google Gemini",
      availableOptions = listOf("Google Gemini", "OpenAI", "Anthropic Claude", "Custom OpenAI-Compatible")
    ),
    ProviderConfig(
      domain = ProviderDomain.SECONDARY_AI,
      selectedProviderName = "None (Optional)",
      availableOptions = listOf("None (Optional)", "Google Gemini Flash", "Groq Fast Inference", "Local SLM")
    ),
    ProviderConfig(
      domain = ProviderDomain.OFFLINE_AI,
      selectedProviderName = "Local On-Device",
      availableOptions = listOf("Local On-Device", "MediaPipe LLM", "Embedded Whisper", "Disabled")
    ),
    ProviderConfig(
      domain = ProviderDomain.VOICE_PROVIDER,
      selectedProviderName = "System Android TTS/STT",
      availableOptions = listOf("System Android TTS/STT", "ElevenLabs", "Deepgram", "OpenAI Whisper/TTS", "Cartesia")
    ),
    ProviderConfig(
      domain = ProviderDomain.VISION_PROVIDER,
      selectedProviderName = "Gemini Vision",
      availableOptions = listOf("Gemini Vision", "OpenAI GPT-4o", "Claude 3.5 Sonnet", "On-device MobileNet")
    ),
    ProviderConfig(
      domain = ProviderDomain.IMAGE_PROVIDER,
      selectedProviderName = "Imagen 3",
      availableOptions = listOf("Imagen 3", "DALL-E 3", "Stable Diffusion API", "Flux")
    ),
    ProviderConfig(
      domain = ProviderDomain.VIDEO_PROVIDER,
      selectedProviderName = "Veo / Gemini Video",
      availableOptions = listOf("Veo / Gemini Video", "Runway Gen-3", "Luma Dream Machine", "Not configured")
    ),
    ProviderConfig(
      domain = ProviderDomain.RESEARCH_PROVIDER,
      selectedProviderName = "Google Search Grounding",
      availableOptions = listOf("Google Search Grounding", "Tavily Search", "Perplexity API", "Brave Search")
    )
  )
}
