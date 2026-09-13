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

class AnthropicProvider(
  private val apiKeyProvider: () -> String?,
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .build()
) : AIProvider {

  override val metadata: ProviderMetadata = ProviderMetadata(
    id = "anthropic",
    name = "Anthropic Claude",
    company = "Anthropic",
    defaultModelId = "claude-3-5-sonnet-20241022",
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
    AIModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", metadata.id, metadata.supportedCapabilities, 200000, isDefault = true, isVisionSupported = true),
    AIModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", metadata.id, metadata.supportedCapabilities, 200000, isVisionSupported = true),
    AIModel("claude-3-opus-20240229", "Claude 3 Opus", metadata.id, metadata.supportedCapabilities, 200000, isVisionSupported = true)
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
        message = "No API key configured for Anthropic Claude."
      )
    }

    val startTime = System.currentTimeMillis()
    try {
      // Test with minimal message
      val bodyJson = JSONObject().apply {
        put("model", metadata.defaultModelId)
        put("max_tokens", 5)
        put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))
      }

      val request = Request.Builder()
        .url("https://api.anthropic.com/v1/messages")
        .header("x-api-key", key)
        .header("anthropic-version", "2023-06-01")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val elapsed = System.currentTimeMillis() - startTime
      val code = response.code
      val body = response.body?.string() ?: ""

      when (code) {
        200 -> {
          _healthStatus = ProviderHealth.AVAILABLE
          ProviderConnectionResult(
            status = ProviderConnectionStatus.CONNECTED,
            message = "Successfully authenticated with Anthropic Claude API.",
            discoveredModels = cachedModels,
            latencyMs = elapsed
          )
        }
        401 -> {
          _healthStatus = ProviderHealth.INVALID
          ProviderConnectionResult(
            status = ProviderConnectionStatus.INVALID_KEY,
            message = "Anthropic API key invalid (HTTP 401 Unauthorized).",
            latencyMs = elapsed
          )
        }
        429 -> {
          _healthStatus = ProviderHealth.RATE_LIMITED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.RATE_LIMITED,
            message = "Anthropic rate limit reached or credits exhausted (HTTP 429).",
            latencyMs = elapsed
          )
        }
        else -> {
          _healthStatus = ProviderHealth.DEGRADED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.PROVIDER_UNAVAILABLE,
            message = "Anthropic API returned HTTP $code: $body",
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

  override suspend fun discoverModels(): List<AIModel> {
    return cachedModels
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
        message = "Anthropic Claude API key is not configured.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId

    try {
      val messagesArray = JSONArray()
      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "assistant"
        messagesArray.put(JSONObject().put("role", role).put("content", msg.text))
      }
      messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("max_tokens", options.maxTokens)
        put("temperature", options.temperature)
        if (!options.systemPrompt.isNullOrBlank()) {
          put("system", options.systemPrompt)
        }
      }

      val request = Request.Builder()
        .url("https://api.anthropic.com/v1/messages")
        .header("x-api-key", key)
        .header("anthropic-version", "2023-06-01")
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
      val contentArray = root.optJSONArray("content")
      val first = contentArray?.optJSONObject(0)
      val text = first?.optString("text")

      if (!text.isNullOrBlank()) {
        val usage = root.optJSONObject("usage")?.optInt("output_tokens")
        AIProviderResult.Success(
          text = text.trim(),
          modelUsed = model,
          providerId = metadata.id,
          finishReason = root.optString("stop_reason"),
          usageTokens = usage
        )
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "Received empty response from Claude.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Network error connecting to Anthropic.",
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
      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "assistant"
        messagesArray.put(JSONObject().put("role", role).put("content", msg.text))
      }
      messagesArray.put(JSONObject().put("role", "user").put("content", prompt))

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("max_tokens", options.maxTokens)
        put("stream", true)
        if (!options.systemPrompt.isNullOrBlank()) {
          put("system", options.systemPrompt)
        }
      }

      val request = Request.Builder()
        .url("https://api.anthropic.com/v1/messages")
        .header("x-api-key", key)
        .header("anthropic-version", "2023-06-01")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        throw Exception("Anthropic streaming failed HTTP ${response.code}")
      }

      val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        val l = line?.trim() ?: continue
        if (l.startsWith("data:")) {
          val data = l.removePrefix("data:").trim()
          if (data.isNotBlank()) {
            try {
              val obj = JSONObject(data)
              val type = obj.optString("type")
              if (type == "content_block_delta") {
                val delta = obj.optJSONObject("delta")
                val text = delta?.optString("text")
                if (!text.isNullOrEmpty()) {
                  emit(text)
                }
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
        message = "Anthropic Claude API key is not configured.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId

    try {
      val contentParts = JSONArray()

      for (item in media) {
        val b64 = item.base64Data ?: item.bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        if (b64 != null) {
          contentParts.put(JSONObject().apply {
            put("type", "image")
            put("source", JSONObject().apply {
              put("type", "base64")
              put("media_type", item.mimeType)
              put("data", b64)
            })
          })
        }
      }

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
        .url("https://api.anthropic.com/v1/messages")
        .header("x-api-key", key)
        .header("anthropic-version", "2023-06-01")
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val code = response.code
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext AIProviderResult.Error(
          errorCode = AIErrorCode.PROVIDER_ERROR,
          message = "Claude Vision error HTTP $code: $responseBody",
          providerId = metadata.id
        )
      }

      val root = JSONObject(responseBody)
      val contentArray = root.optJSONArray("content")
      val text = contentArray?.optJSONObject(0)?.optString("text")

      if (!text.isNullOrBlank()) {
        AIProviderResult.Success(text = text.trim(), modelUsed = model, providerId = metadata.id)
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "Received empty visual response from Claude.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Network error connecting to Anthropic Vision.",
        providerId = metadata.id
      )
    }
  }
}
