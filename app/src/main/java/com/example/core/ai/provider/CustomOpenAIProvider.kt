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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class CustomOpenAIProvider(
  private val baseUrlProvider: () -> String,
  private val apiKeyProvider: () -> String?,
  private val defaultModelProvider: () -> String = { "default" }
) : AIProvider {

  override val metadata: ProviderMetadata = ProviderMetadata(
    id = "custom_openai",
    name = "Custom OpenAI-Compatible",
    company = "Custom / Self-Hosted",
    defaultModelId = defaultModelProvider(),
    supportedCapabilities = setOf(
      AICapability.TEXT_CHAT,
      AICapability.REASONING,
      AICapability.CODE_GENERATION,
      AICapability.SUMMARIZATION,
      AICapability.TRANSLATION
    ),
    requiresApiKey = false,
    baseUrlConfigurable = true
  )

  private val delegate: OpenAIProvider
    get() = OpenAIProvider(
      apiKeyProvider = apiKeyProvider,
      baseUrl = baseUrlProvider().trimEnd('/')
    )

  override val isConfigured: Boolean
    get() = baseUrlProvider().isNotBlank()

  override val availableModels: List<AIModel>
    get() = delegate.availableModels

  override val healthStatus: ProviderHealth
    get() = delegate.healthStatus

  override suspend fun testConnection(): ProviderConnectionResult {
    if (!isConfigured) {
      return ProviderConnectionResult(
        status = ProviderConnectionStatus.NOT_CONFIGURED,
        message = "Custom endpoint URL is not configured."
      )
    }
    return delegate.testConnection()
  }

  override suspend fun discoverModels(): List<AIModel> = delegate.discoverModels()

  override suspend fun generate(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult = delegate.generate(prompt, context, options.copy(modelId = options.modelId ?: defaultModelProvider()))

  override fun generateStream(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): Flow<String>? = delegate.generateStream(prompt, context, options.copy(modelId = options.modelId ?: defaultModelProvider()))

  override suspend fun generateMultimodal(
    prompt: String,
    media: List<MediaItem>,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult = delegate.generateMultimodal(prompt, media, context, options)
}
