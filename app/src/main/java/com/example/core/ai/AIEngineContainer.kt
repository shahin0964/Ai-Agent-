package com.example.core.ai

import android.content.Context
import com.example.core.ai.creator.CreatorEngine
import com.example.core.ai.memory.MemoryManager
import com.example.core.ai.personality.AgentPersonalityEngine
import com.example.core.ai.provider.AIProvider
import com.example.core.ai.provider.AnthropicProvider
import com.example.core.ai.provider.CustomOpenAIProvider
import com.example.core.ai.provider.GeminiProvider
import com.example.core.ai.provider.ModelDiscoveryService
import com.example.core.ai.provider.OfflineAIProvider
import com.example.core.ai.provider.OpenAIProvider
import com.example.core.ai.provider.SecureCredentialStore
import com.example.core.ai.research.ResearchEngine
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.ai.router.RoutingConfigManager
import com.example.core.ai.vision.VisionEngine

class AIEngineContainer(context: Context) {
  val credentialStore = SecureCredentialStore(context)
  val routingConfig = RoutingConfigManager(context)

  val geminiProvider = GeminiProvider(
    apiKeyProvider = { credentialStore.getApiKey("google_gemini") }
  )

  val openAiProvider = OpenAIProvider(
    apiKeyProvider = { credentialStore.getApiKey("openai") }
  )

  val anthropicProvider = AnthropicProvider(
    apiKeyProvider = { credentialStore.getApiKey("anthropic") }
  )

  val customOpenAiProvider = CustomOpenAIProvider(
    baseUrlProvider = { routingConfig.preferences.value.customBaseUrl },
    apiKeyProvider = { credentialStore.getApiKey("custom_openai") },
    defaultModelProvider = { routingConfig.preferences.value.customModelId }
  )

  val offlineProvider = OfflineAIProvider()

  val providers: Map<String, AIProvider> = mapOf(
    "google_gemini" to geminiProvider,
    "openai" to openAiProvider,
    "anthropic" to anthropicProvider,
    "custom_openai" to customOpenAiProvider,
    "local_offline" to offlineProvider
  )

  val modelDiscovery = ModelDiscoveryService(providers)

  val router = AIIntelligenceRouter(providers, routingConfig)

  val researchEngine = ResearchEngine(router)

  val memoryManager = MemoryManager(context)

  val visionEngine = VisionEngine(context, router)

  val creatorEngine = CreatorEngine(router)

  val personalityEngine = AgentPersonalityEngine(context)

  val companionEngine = com.example.core.companion.CompanionEngine(context, personalityEngine)

  val automationEngine by lazy { com.example.core.automation.AutomationEngine.getInstance(context, this) }

  companion object {
    @Volatile
    private var INSTANCE: AIEngineContainer? = null

    fun getInstance(context: Context): AIEngineContainer {
      return INSTANCE ?: synchronized(this) {
        val inst = AIEngineContainer(context.applicationContext)
        INSTANCE = inst
        inst
      }
    }
  }
}
