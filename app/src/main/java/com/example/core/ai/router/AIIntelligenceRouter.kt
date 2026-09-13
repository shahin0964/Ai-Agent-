package com.example.core.ai.router

import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIErrorCode
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.model.MediaItem
import com.example.core.ai.provider.AIProvider
import com.example.core.context.ConversationContext
import kotlinx.coroutines.flow.Flow

class AIIntelligenceRouter(
  private val providers: Map<String, AIProvider>,
  private val routingConfig: RoutingConfigManager
) {

  fun classifyCapability(prompt: String, hasMedia: Boolean = false): AICapability {
    if (hasMedia) return AICapability.VISION

    val p = prompt.trim().lowercase()
    return when {
      p.contains("write code") || p.contains("generate code") || p.contains("kotlin") ||
        p.contains("python") || p.contains("function ") || p.contains("debug this") ||
        p.contains("code review") || p.contains("syntax error") -> AICapability.CODE_GENERATION

      p.contains("build a website") || p.contains("create a website") || p.contains("html") ||
        p.contains("landing page") || p.contains("web app") -> AICapability.WEBSITE_GENERATION

      p.contains("search web") || p.contains("latest news") || p.contains("today's news") ||
        p.contains("current weather") || p.contains("search the internet") ||
        p.contains("recent events") || p.contains("browse the web") ||
        p.contains("look up online") -> AICapability.WEB_RESEARCH

      p.contains("summarize") || p.contains("tldr") || p.contains("key takeaways") -> AICapability.SUMMARIZATION
      p.contains("translate") || p.contains("in spanish") || p.contains("in french") || p.contains("in arabic") -> AICapability.TRANSLATION
      p.contains("why does") || p.contains("analyze") || p.contains("diagnose") -> AICapability.REASONING
      else -> AICapability.TEXT_CHAT
    }
  }

  suspend fun routeAndGenerate(
    prompt: String,
    context: ConversationContext = ConversationContext(),
    media: List<MediaItem> = emptyList(),
    options: GenerateOptions = GenerateOptions()
  ): AIProviderResult {
    val capability = if (media.isNotEmpty()) AICapability.VISION else options.targetCapability
    val prefs = routingConfig.preferences.value

    // Candidate providers in priority order
    val candidateIds = mutableListOf<String>()

    if (capability == AICapability.VISION) {
      candidateIds.add(prefs.visionProviderId)
      if (prefs.primaryProviderId != prefs.visionProviderId) {
        candidateIds.add(prefs.primaryProviderId)
      }
    } else {
      candidateIds.add(prefs.primaryProviderId)
      if (prefs.secondaryProviderId != "none" && prefs.secondaryProviderId.isNotBlank()) {
        candidateIds.add(prefs.secondaryProviderId)
      }
      if (prefs.fallbackProviderId != "none" && prefs.fallbackProviderId.isNotBlank()) {
        candidateIds.add(prefs.fallbackProviderId)
      }
    }

    // Always fallback to on-device offline if everything else is exhausted
    candidateIds.add("local_offline")

    var lastError: AIProviderResult.Error? = null

    for (providerId in candidateIds.distinct()) {
      val provider = providers[providerId] ?: continue
      if (!provider.isConfigured && !provider.metadata.isLocalOffline) {
        continue
      }

      if (!provider.capabilities.contains(capability) && capability != AICapability.TEXT_CHAT) {
        continue
      }

      val modelId = if (providerId == prefs.primaryProviderId && prefs.primaryModelId.isNotBlank()) {
        prefs.primaryModelId
      } else if (providerId == prefs.secondaryProviderId && prefs.secondaryModelId.isNotBlank()) {
        prefs.secondaryModelId
      } else {
        provider.metadata.defaultModelId
      }

      val execOptions = options.copy(
        modelId = modelId,
        targetCapability = capability
      )

      val result = if (media.isNotEmpty()) {
        provider.generateMultimodal(prompt, media, context, execOptions)
      } else {
        provider.generate(prompt, context, execOptions)
      }

      when (result) {
        is AIProviderResult.Success -> return result
        is AIProviderResult.Streaming -> return result
        is AIProviderResult.Error -> {
          lastError = result
          // If it was a config error or critical, try next candidate
        }
      }
    }

    return lastError ?: AIProviderResult.Error(
      errorCode = AIErrorCode.NO_PROVIDER,
      message = "No AI provider is configured for ${capability.displayName}. Please configure an API key in Settings.",
      providerId = "router",
      isConfigError = true
    )
  }

  fun routeAndStream(
    prompt: String,
    context: ConversationContext = ConversationContext(),
    options: GenerateOptions = GenerateOptions()
  ): Flow<String>? {
    val prefs = routingConfig.preferences.value
    val primaryProvider = providers[prefs.primaryProviderId]

    if (primaryProvider != null && primaryProvider.isConfigured) {
      val modelId = prefs.primaryModelId.ifBlank { primaryProvider.metadata.defaultModelId }
      val stream = primaryProvider.generateStream(prompt, context, options.copy(modelId = modelId))
      if (stream != null) return stream
    }

    val secondary = providers[prefs.secondaryProviderId]
    if (secondary != null && secondary.isConfigured) {
      val modelId = prefs.secondaryModelId.ifBlank { secondary.metadata.defaultModelId }
      return secondary.generateStream(prompt, context, options.copy(modelId = modelId))
    }

    return null
  }
}
