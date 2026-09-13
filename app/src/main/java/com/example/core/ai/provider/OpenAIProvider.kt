package com.example.core.ai.provider

import android.util.Base64
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class OpenAIProvider(
  private val apiKeyProvider: () -> String?,
  private val baseUrl: String = "https://api.openai.com/v1",
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .build()
) : AIProvider {

  override val metadata: ProviderMetadata = ProviderMetadata(
    id = "openai",
    name = "OpenAI",
    company = "OpenAI",
    defaultModelId = "gpt-4o",
    supportedCapabilities = setOf(
      AICapability.TEXT_CHAT,
      AICapability.REASONING,
      AICapability.VISION,
      AICapability.OCR,
      AICapability.CODE_GENERATION,
      AICapability.WEBSITE_GENERATION,
      AICapability.SUMMARIZATION,
      AICapability.TRANSLATION,
      AICapability.ANALYSIS
    ),
    requiresApiKey = true
  )

  override val isConfigured: Boolean
    get() = !apiKeyProvider().isNullOrBlank()

  private var cachedModels: List<AIModel> = listOf(
    AIModel("gpt-4o", "GPT-4o", metadata.id, metadata.supportedCapabilities, 128000, isDefault = true, isVisionSupported = true),
    AIModel("gpt-4o-mini", "GPT-4o Mini", metadata.id, metadata.supportedCapabilities, 128000, isVisionSupported = true),
    AIModel("o1-mini", "o1-mini", metadata.id, setOf(AICapability.TEXT_CHAT, AICapability.REASONING, AICapability.CODE_GENERATION), 128000)
  )

  override val availableModels: List<AIModel>
    get() = cachedModels

  private var _healthStatus: ProviderHealth = ProviderHealth.UNKNOWN
  override val healthStatus: ProviderHealth
    get() = if (!isConfigured) ProviderHealth.INVALID else _healthStatus

  override suspend fun testConnection(): ProviderConnectionResult = withContext(Dispatchers.IO) {
    val key = apiKeyProvider()
    if (key.isNullOrBlank()) {
      return@withContext ProviderConnectionResult(
        status = ProviderConnectionStatus.NOT_CONFIGURED,
        message = "No API key configured for OpenAI."
      )
    }

    val startTime = System.currentTimeMillis()
    try {
      val request = Request.Builder()
        .url("$baseUrl/models")
        .header("Authorization", "Bearer $key")
        .get()
        .build()

      val response = client.newCall(request).execute()
      val elapsed = System.currentTimeMillis() - startTime
      val code = response.code
      val body = response.body?.string() ?: ""

      when (code) {
        200 -> {
          _healthStatus = ProviderHealth.AVAILABLE
          val discovered = parseModels(body)
          if (discovered.isNotEmpty()) {
            cachedModels = discovered
          }
          ProviderConnectionResult(
            status = ProviderConnectionStatus.CONNECTED,
            message = "Successfully authenticated with OpenAI (${discovered.size} models available).",
            discoveredModels = cachedModels,
            latencyMs = elapsed
          )
        }
        401 -> {
          _healthStatus = ProviderHealth.INVALID
          ProviderConnectionResult(
            status = ProviderConnectionStatus.INVALID_KEY,
            message = "OpenAI rejected key (HTTP 401 Unauthorized).",
            latencyMs = elapsed
          )
        }
        429 -> {
          _healthStatus = ProviderHealth.RATE_LIMITED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.RATE_LIMITED,
            message = "OpenAI rate limit reached or quota exhausted (HTTP 429).",
            latencyMs = elapsed
          )
        }
        else -> {
          _healthStatus = ProviderHealth.DEGRADED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.PROVIDER_UNAVAILABLE,
            message = "OpenAI API returned HTTP $code: $body",
            latencyMs = elapsed
          )
        }
      }
    } catch (e: Exception) {
      _healthStatus = ProviderHealth.OFFLINE
      ProviderConnectionResult(
        status = ProviderConnectionStatus.NETWORK_ERROR,
        message = "Network connection failure: ${e.localizedMessage}"
      )
    }
  }

  override suspend fun discoverModels(): List<AIModel> = withContext(Dispatchers.IO) {
    val key = apiKeyProvider() ?: return@withContext cachedModels
    try {
      val request = Request.Builder()
        .url("$baseUrl/models")
        .header("Authorization", "Bearer $key")
        .get()
        .build()
      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        val body = response.body?.string() ?: ""
        val models = parseModels(body)
        if (models.isNotEmpty()) {
          cachedModels = models
        }
      }
    } catch (_: Exception) {}
    cachedModels
  }

  private fun parseModels(jsonStr: String): List<AIModel> {
    val result = mutableListOf<AIModel>()
    try {
      val root = JSONObject(jsonStr)
      val array = root.optJSONArray("data") ?: return emptyList()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val id = obj.optString("id", "")
        if (id.startsWith("gpt-") || id.startsWith("o1-") || id.startsWith("chatgpt-")) {
          result.add(
            AIModel(
              id = id,
              name = id.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
              providerId = metadata.id,
              supportedCapabilities = metadata.supportedCapabilities,
              contextWindow = 128000,
              isDefault = id == metadata.defaultModelId,
              isVisionSupported = id.contains("4o") || id.contains("vision"),
              isStreamingSupported = true
            )
          )
        }
      }
    } catch (_: Exception) {}
    return result
  }

  override suspend fun generate(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult = withContext(Dispatchers.IO) {
    val key = apiKeyProvider()
    if (key.isNullOrBlank()) {
      return@withContext AIProviderResult.Error(
        errorCode = AIErrorCode.INVALID_API_KEY,
        message = "OpenAI API key is not configured.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId

    try {
      val messagesArray = JSONArray()

      if (!options.systemPrompt.isNullOrBlank()) {
        messagesArray.put(JSONObject().put("role", "system").put("content", options.systemPrompt))
      }

      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "assistant"
        messagesArray.put(JSONObject().put("role", role).put("content", msg.text))
      }

      messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("temperature", options.temperature)
        put("max_tokens", options.maxTokens)
      }

      val request = Request.Builder()
        .url("$baseUrl/chat/completions")
        .header("Authorization", "Bearer $key")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val code = response.code
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errCode = when (code) {
          401 -> AIErrorCode.INVALID_API_KEY
          429 -> AIErrorCode.RATE_LIMIT
          else -> AIErrorCode.PROVIDER_ERROR
        }
        val msg = try {
          JSONObject(responseBody).getJSONObject("error").optString("message", "HTTP $code")
        } catch (_: Exception) {
          "HTTP $code"
        }
        return@withContext AIProviderResult.Error(
          errorCode = errCode,
          message = msg,
          providerId = metadata.id
        )
      }

      val root = JSONObject(responseBody)
      val choices = root.optJSONArray("choices")
      val first = choices?.optJSONObject(0)
      val msgObj = first?.optJSONObject("message")
      val text = msgObj?.optString("content")
      val tokens = root.optJSONObject("usage")?.optInt("total_tokens")

      if (!text.isNullOrBlank()) {
        AIProviderResult.Success(
          text = text.trim(),
          modelUsed = model,
          providerId = metadata.id,
          finishReason = first.optString("finish_reason"),
          usageTokens = tokens
        )
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "Received empty response from OpenAI.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Network error connecting to OpenAI.",
        providerId = metadata.id
      )
    }
  }

  override fun generateStream(
    prompt: String,
    context: ConversationContext,
    options: GenerateOptions
  ): Flow<String>? {
    val key = apiKeyProvider()
    if (key.isNullOrBlank()) return null

    val model = options.modelId ?: metadata.defaultModelId

    return flow {
      val messagesArray = JSONArray()
      if (!options.systemPrompt.isNullOrBlank()) {
        messagesArray.put(JSONObject().put("role", "system").put("content", options.systemPrompt))
      }
      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "assistant"
        messagesArray.put(JSONObject().put("role", role).put("content", msg.text))
      }
      messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("temperature", options.temperature)
        put("max_tokens", options.maxTokens)
        put("stream", true)
      }

      val request = Request.Builder()
        .url("$baseUrl/chat/completions")
        .header("Authorization", "Bearer $key")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        throw Exception("OpenAI streaming failed HTTP ${response.code}")
      }

      val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        val l = line?.trim() ?: continue
        if (l.startsWith("data:")) {
          val data = l.removePrefix("data:").trim()
          if (data == "[DONE]") break
          if (data.isNotBlank()) {
            try {
              val obj = JSONObject(data)
              val choices = obj.optJSONArray("choices")
              val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
              val content = delta?.optString("content")
              if (!content.isNullOrEmpty()) {
                emit(content)
              }
            } catch (_: Exception) {}
          }
        }
      }
    }.flowOn(Dispatchers.IO)
  }

  override suspend fun generateMultimodal(
    prompt: String,
    media: List<MediaItem>,
    context: ConversationContext,
    options: GenerateOptions
  ): AIProviderResult = withContext(Dispatchers.IO) {
    val key = apiKeyProvider()
    if (key.isNullOrBlank()) {
      return@withContext AIProviderResult.Error(
        errorCode = AIErrorCode.INVALID_API_KEY,
        message = "OpenAI API key is not configured.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId

    try {
      val contentParts = JSONArray()

      // Add media items as image_url objects
      for (item in media) {
        val b64 = item.base64Data ?: item.bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        if (b64 != null) {
          val dataUrl = "data:${item.mimeType};base64,$b64"
          contentParts.put(JSONObject().apply {
            put("type", "image_url")
            put("image_url", JSONObject().put("url", dataUrl))
          })
        }
      }

      // Add text
      contentParts.put(JSONObject().apply {
        put("type", "text")
        put("text", prompt)
      })

      val messagesArray = JSONArray().put(JSONObject().apply {
        put("role", "user")
        put("content", contentParts)
      })

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("max_tokens", options.maxTokens)
      }

      val request = Request.Builder()
        .url("$baseUrl/chat/completions")
        .header("Authorization", "Bearer $key")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val code = response.code
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext AIProviderResult.Error(
          errorCode = AIErrorCode.PROVIDER_ERROR,
          message = "OpenAI Vision failed HTTP $code: $responseBody",
          providerId = metadata.id
        )
      }

      val root = JSONObject(responseBody)
      val choices = root.optJSONArray("choices")
      val text = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")

      if (!text.isNullOrBlank()) {
        AIProviderResult.Success(text = text.trim(), modelUsed = model, providerId = metadata.id)
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "Received empty visual analysis from OpenAI.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Multimodal network error with OpenAI.",
        providerId = metadata.id
      )
    }
  }
}
