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

class GeminiProvider(
  private val apiKeyProvider: () -> String?,
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .build()
) : AIProvider {

  override val metadata: ProviderMetadata = ProviderMetadata(
    id = "google_gemini",
    name = "Google Gemini",
    company = "Google",
    defaultModelId = "gemini-2.5-flash",
    supportedCapabilities = setOf(
      AICapability.TEXT_CHAT,
      AICapability.REASONING,
      AICapability.WEB_RESEARCH,
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
    AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", metadata.id, metadata.supportedCapabilities, 1000000, isDefault = true, isVisionSupported = true),
    AIModel("gemini-1.5-pro", "Gemini 1.5 Pro", metadata.id, metadata.supportedCapabilities, 2000000, isVisionSupported = true),
    AIModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", metadata.id, metadata.supportedCapabilities, 1000000, isVisionSupported = true)
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
        message = "No API key configured for Google Gemini."
      )
    }

    val startTime = System.currentTimeMillis()
    try {
      val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
      val request = Request.Builder().url(url).get().build()
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
            message = "Successfully connected to Google Gemini API (${discovered.size} models discovered).",
            discoveredModels = cachedModels,
            latencyMs = elapsed
          )
        }
        400, 403 -> {
          _healthStatus = ProviderHealth.INVALID
          ProviderConnectionResult(
            status = ProviderConnectionStatus.INVALID_KEY,
            message = "Gemini API rejected credentials (HTTP $code). Verify API key.",
            latencyMs = elapsed
          )
        }
        429 -> {
          _healthStatus = ProviderHealth.RATE_LIMITED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.RATE_LIMITED,
            message = "Gemini rate limit reached (HTTP 429).",
            latencyMs = elapsed
          )
        }
        else -> {
          _healthStatus = ProviderHealth.DEGRADED
          ProviderConnectionResult(
            status = ProviderConnectionStatus.PROVIDER_UNAVAILABLE,
            message = "Gemini server error (HTTP $code): $body",
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
      val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
      val request = Request.Builder().url(url).get().build()
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
      val array = root.optJSONArray("models") ?: return emptyList()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val rawName = obj.optString("name", "") // e.g. "models/gemini-2.5-flash"
        val cleanId = rawName.removePrefix("models/")
        val dispName = obj.optString("displayName", cleanId)
        val inputTokenLimit = obj.optInt("inputTokenLimit", 32768)
        val supportedMethods = obj.optJSONArray("supportedGenerationMethods")
        var supportsGenerateContent = false
        if (supportedMethods != null) {
          for (j in 0 until supportedMethods.length()) {
            if (supportedMethods.getString(j) == "generateContent") {
              supportsGenerateContent = true
              break
            }
          }
        }
        if (supportsGenerateContent && cleanId.startsWith("gemini")) {
          result.add(
            AIModel(
              id = cleanId,
              name = dispName,
              providerId = metadata.id,
              supportedCapabilities = metadata.supportedCapabilities,
              contextWindow = inputTokenLimit,
              isDefault = cleanId == metadata.defaultModelId,
              isVisionSupported = true,
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
        message = "Google Gemini API key is not configured. Please enter your API key in Settings.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId
    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

    try {
      val contentsArray = JSONArray()

      // Conversation turns
      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "model"
        val parts = JSONArray().put(JSONObject().put("text", msg.text))
        contentsArray.put(JSONObject().put("role", role).put("parts", parts))
      }

      // Current prompt
      contentsArray.put(
        JSONObject().put("role", "user").put(
          "parts", JSONArray().put(JSONObject().put("text", prompt))
        )
      )

      val bodyJson = JSONObject().apply {
        put("contents", contentsArray)
        put("generationConfig", JSONObject().apply {
          put("temperature", options.temperature)
          put("maxOutputTokens", options.maxTokens)
        })
        if (!options.systemPrompt.isNullOrBlank()) {
          put("systemInstruction", JSONObject().put(
            "parts", JSONArray().put(JSONObject().put("text", options.systemPrompt))
          ))
        }
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val code = response.code
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          JSONObject(responseBody).getJSONObject("error").optString("message", "HTTP $code")
        } catch (_: Exception) {
          "HTTP error $code"
        }
        val errCode = when (code) {
          400, 403 -> AIErrorCode.INVALID_API_KEY
          429 -> AIErrorCode.RATE_LIMIT
          else -> AIErrorCode.PROVIDER_ERROR
        }
        return@withContext AIProviderResult.Error(
          errorCode = errCode,
          message = errorMsg,
          providerId = metadata.id
        )
      }

      val root = JSONObject(responseBody)
      val candidates = root.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val content = firstCandidate?.optJSONObject("content")
      val parts = content?.optJSONArray("parts")
      val text = parts?.optJSONObject(0)?.optString("text")

      if (!text.isNullOrBlank()) {
        val usage = root.optJSONObject("usageMetadata")?.optInt("totalTokenCount")
        AIProviderResult.Success(
          text = text.trim(),
          modelUsed = model,
          providerId = metadata.id,
          finishReason = firstCandidate.optString("finishReason", "STOP"),
          usageTokens = usage
        )
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "Received empty response from Gemini model.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Failed to connect to Gemini API.",
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
    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$key"

    return flow {
      val contentsArray = JSONArray()
      context.recentMessages.takeLast(8).forEach { msg ->
        val role = if (msg.isUser) "user" else "model"
        val parts = JSONArray().put(JSONObject().put("text", msg.text))
        contentsArray.put(JSONObject().put("role", role).put("parts", parts))
      }
      contentsArray.put(
        JSONObject().put("role", "user").put(
          "parts", JSONArray().put(JSONObject().put("text", prompt))
        )
      )

      val bodyJson = JSONObject().apply {
        put("contents", contentsArray)
        put("generationConfig", JSONObject().apply {
          put("temperature", options.temperature)
          put("maxOutputTokens", options.maxTokens)
        })
        if (!options.systemPrompt.isNullOrBlank()) {
          put("systemInstruction", JSONObject().put(
            "parts", JSONArray().put(JSONObject().put("text", options.systemPrompt))
          ))
        }
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        throw Exception("Gemini streaming failed HTTP ${response.code}")
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
              val candidates = obj.optJSONArray("candidates")
              val first = candidates?.optJSONObject(0)
              val content = first?.optJSONObject("content")
              val parts = content?.optJSONArray("parts")
              val chunk = parts?.optJSONObject(0)?.optString("text")
              if (!chunk.isNullOrEmpty()) {
                emit(chunk)
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
        message = "Google Gemini API key is not configured.",
        providerId = metadata.id,
        isConfigError = true
      )
    }

    val model = options.modelId ?: metadata.defaultModelId
    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

    try {
      val contentsArray = JSONArray()
      val partsArray = JSONArray()

      // Add media inline parts
      for (item in media) {
        val base64Str = item.base64Data ?: item.bytes?.let {
          Base64.encodeToString(it, Base64.NO_WRAP)
        }
        if (base64Str != null) {
          partsArray.put(JSONObject().put("inlineData", JSONObject().apply {
            put("mimeType", item.mimeType)
            put("data", base64Str)
          }))
        }
      }

      // Add text prompt part
      partsArray.put(JSONObject().put("text", prompt))

      contentsArray.put(JSONObject().put("role", "user").put("parts", partsArray))

      val bodyJson = JSONObject().apply {
        put("contents", contentsArray)
        put("generationConfig", JSONObject().apply {
          put("temperature", options.temperature)
          put("maxOutputTokens", options.maxTokens)
        })
      }

      val request = Request.Builder()
        .url(url)
        .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val code = response.code
      val responseBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        return@withContext AIProviderResult.Error(
          errorCode = AIErrorCode.PROVIDER_ERROR,
          message = "Gemini multimodal failed HTTP $code: $responseBody",
          providerId = metadata.id
        )
      }

      val root = JSONObject(responseBody)
      val candidates = root.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val content = firstCandidate?.optJSONObject("content")
      val parts = content?.optJSONArray("parts")
      val text = parts?.optJSONObject(0)?.optString("text")

      if (!text.isNullOrBlank()) {
        AIProviderResult.Success(
          text = text.trim(),
          modelUsed = model,
          providerId = metadata.id
        )
      } else {
        AIProviderResult.Error(
          errorCode = AIErrorCode.GENERATION_FAILED,
          message = "No visual analysis returned from Gemini.",
          providerId = metadata.id
        )
      }
    } catch (e: Exception) {
      AIProviderResult.Error(
        errorCode = AIErrorCode.NETWORK_ERROR,
        message = e.localizedMessage ?: "Multimodal network error.",
        providerId = metadata.id
      )
    }
  }
}
