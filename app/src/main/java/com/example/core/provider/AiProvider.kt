package com.example.core.provider

import com.example.core.context.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Result of an AI inference operation.
 */
sealed class ProviderResult {
  data class Success(val responseText: String) : ProviderResult()
  data class Streaming(val textStream: Flow<String>) : ProviderResult()
  data class Error(val message: String, val isConfigError: Boolean = false) : ProviderResult()
}

/**
 * Core Intelligence Provider abstraction.
 * Unifies online cloud models and future offline on-device SLMs under a single interface.
 */
interface IntelligenceProvider {
  val isOnline: Boolean
  val isAvailable: Boolean
  val providerName: String
  val modelInfo: String

  suspend fun sendMessage(prompt: String, context: ConversationContext): ProviderResult
  fun streamMessage(prompt: String, context: ConversationContext): Flow<String>? = null
}

interface OnlineIntelligenceProvider : IntelligenceProvider {
  override val isOnline: Boolean get() = true
}

interface OfflineIntelligenceProvider : IntelligenceProvider {
  override val isOnline: Boolean get() = false
}

/**
 * Replaceable Real Gemini AI Provider.
 * Connects when a legitimate API key is supplied via Settings or environment.
 * If no key is configured, reports truthfully without mock responses.
 */
class GeminiIntelligenceProvider(
  private var apiKeyProvider: () -> String? = { null }
) : OnlineIntelligenceProvider {

  override val providerName: String = "Google Gemini"
  override val modelInfo: String = "gemini-2.5-flash"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  override val isAvailable: Boolean
    get() = !apiKeyProvider().isNullOrBlank()

  override suspend fun sendMessage(prompt: String, context: ConversationContext): ProviderResult =
    withContext(Dispatchers.IO) {
      val key = apiKeyProvider()
      if (key.isNullOrBlank()) {
        return@withContext ProviderResult.Error(
          message = "AI provider is not configured. Please configure an API key in Settings.",
          isConfigError = true
        )
      }

      try {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelInfo:generateContent?key=$key"

        // Build Gemini request JSON
        val contentsArray = JSONArray()

        // Include recent conversational turns for multi-turn context
        context.recentMessages.takeLast(6).forEach { msg ->
          val role = if (msg.isUser) "user" else "model"
          val parts = JSONArray().put(JSONObject().put("text", msg.text))
          contentsArray.put(JSONObject().put("role", role).put("parts", parts))
        }

        // Add current prompt
        val currentPart = JSONArray().put(JSONObject().put("text", prompt))
        contentsArray.put(JSONObject().put("role", "user").put("parts", currentPart))

        val bodyJson = JSONObject().apply {
          put("contents", contentsArray)
          put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 800)
          })
        }

        val request = Request.Builder()
          .url(endpoint)
          .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
          .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
          val errorMsg = try {
            val errObj = JSONObject(responseBody).getJSONObject("error")
            errObj.optString("message", "API Error HTTP ${response.code}")
          } catch (_: Exception) {
            "API Error HTTP ${response.code}"
          }
          return@withContext ProviderResult.Error(errorMsg)
        }

        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        if (!text.isNullOrBlank()) {
          ProviderResult.Success(text.trim())
        } else {
          ProviderResult.Error("Empty response received from AI model.")
        }
      } catch (e: Exception) {
        ProviderResult.Error(e.localizedMessage ?: "Network connection failure.")
      }
    }

  override fun streamMessage(prompt: String, context: ConversationContext): Flow<String>? {
    val key = apiKeyProvider()
    if (key.isNullOrBlank()) return null

    return flow {
      when (val result = sendMessage(prompt, context)) {
        is ProviderResult.Success -> emit(result.responseText)
        is ProviderResult.Error -> throw Exception(result.message)
        is ProviderResult.Streaming -> {}
      }
    }.flowOn(Dispatchers.IO)
  }
}

/**
 * Offline Intelligence Provider architecture.
 * Ready for on-device MediaPipe or Gemini Nano integration.
 * Truthfully reports when local weights are not installed.
 */
class LocalOfflineIntelligenceProvider : OfflineIntelligenceProvider {
  override val providerName: String = "On-Device Neural Engine"
  override val modelInfo: String = "Local SLM (Not Installed)"
  override val isAvailable: Boolean = false

  override suspend fun sendMessage(prompt: String, context: ConversationContext): ProviderResult {
    return ProviderResult.Error(
      message = "Offline AI model is not installed on this device.",
      isConfigError = true
    )
  }
}

/**
 * Composite AI Orchestration Engine.
 * Intelligently switches between online and offline providers, and truthfully reports if unconfigured.
 */
class CompositeIntelligenceProvider(
  private var onlineProvider: IntelligenceProvider = GeminiIntelligenceProvider(),
  private var offlineProvider: IntelligenceProvider = LocalOfflineIntelligenceProvider()
) : IntelligenceProvider {

  override val isOnline: Boolean get() = onlineProvider.isAvailable
  override val isAvailable: Boolean get() = onlineProvider.isAvailable || offlineProvider.isAvailable
  override val providerName: String get() = if (onlineProvider.isAvailable) onlineProvider.providerName else offlineProvider.providerName
  override val modelInfo: String get() = if (onlineProvider.isAvailable) onlineProvider.modelInfo else offlineProvider.modelInfo

  fun setOnlineProvider(provider: IntelligenceProvider) {
    onlineProvider = provider
  }

  fun setOfflineProvider(provider: IntelligenceProvider) {
    offlineProvider = provider
  }

  override suspend fun sendMessage(prompt: String, context: ConversationContext): ProviderResult {
    return if (onlineProvider.isAvailable) {
      onlineProvider.sendMessage(prompt, context)
    } else if (offlineProvider.isAvailable) {
      offlineProvider.sendMessage(prompt, context)
    } else {
      // Truthful error reporting
      ProviderResult.Error(
        message = "AI provider is not configured. Please configure an AI provider in Settings.",
        isConfigError = true
      )
    }
  }

  override fun streamMessage(prompt: String, context: ConversationContext): Flow<String>? {
    return if (onlineProvider.isAvailable) {
      onlineProvider.streamMessage(prompt, context)
    } else {
      offlineProvider.streamMessage(prompt, context)
    }
  }
}
