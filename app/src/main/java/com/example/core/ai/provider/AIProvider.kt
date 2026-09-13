package com.example.core.ai.provider

import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIModel
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.model.MediaItem
import com.example.core.ai.model.ProviderConnectionResult
import com.example.core.ai.model.ProviderHealth
import com.example.core.ai.model.ProviderMetadata
import com.example.core.context.ConversationContext
import kotlinx.coroutines.flow.Flow

/**
 * Unified AI Provider Contract.
 * Abstracts Gemini, OpenAI, Anthropic, Custom endpoints, and Local SLMs.
 */
interface AIProvider {
  val metadata: ProviderMetadata
  val providerId: String get() = metadata.id
  val providerName: String get() = metadata.name
  val capabilities: Set<AICapability> get() = metadata.supportedCapabilities
  val isConfigured: Boolean
  val availableModels: List<AIModel>
  val healthStatus: ProviderHealth

  suspend fun testConnection(): ProviderConnectionResult
  suspend fun discoverModels(): List<AIModel>

  suspend fun generate(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions = GenerateOptions()
  ): AIProviderResult

  fun generateStream(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions = GenerateOptions()
  ): Flow<String>?

  suspend fun generateMultimodal(
    prompt: String,
    media: List<MediaItem>,
    context: ConversationContext,
    options: GenerateOptions = GenerateOptions()
  ): AIProviderResult
}
