package com.example.core.ai.research

import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.context.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ResearchEngine(
  private val router: AIIntelligenceRouter,
  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()
) {

  suspend fun conductResearch(
    query: String,
    context: ConversationContext = ConversationContext()
  ): ResearchResult = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val cleanQuery = query.trim()

    // 1. Fetch live web search results
    val sources = fetchWebSearchResults(cleanQuery)

    if (sources.isEmpty()) {
      val directInference = router.routeAndGenerate(
        prompt = "Answer the user question accurately. Be transparent that live web search returned 0 indexing results: $cleanQuery",
        context = context,
        options = GenerateOptions(targetCapability = AICapability.REASONING)
      )
      val answerText = when (directInference) {
        is AIProviderResult.Success -> directInference.text
        is AIProviderResult.Error -> "Search failed: ${directInference.message}"
        is AIProviderResult.Streaming -> "Processing research synthesis..."
      }
      return@withContext ResearchResult(
        query = cleanQuery,
        synthesizedAnswer = answerText,
        confidence = FactConfidence.UNCERTAIN,
        sources = emptyList(),
        searchLatencyMs = System.currentTimeMillis() - startTime
      )
    }

    // 2. Synthesize using AI provider
    val promptBuilder = StringBuilder()
    promptBuilder.append("You are an objective research synthesizer. Answer the query based strictly on the provided search groundings.\n")
    promptBuilder.append("Query: $cleanQuery\n\n")
    promptBuilder.append("Web Groundings:\n")
    sources.take(5).forEachIndexed { index, src ->
      promptBuilder.append("[${index + 1}] Title: ${src.title}\n")
      promptBuilder.append("URL: ${src.url}\n")
      promptBuilder.append("Snippet: ${src.snippet}\n\n")
    }
    promptBuilder.append("Instructions:\n")
    promptBuilder.append("- Cite sources using [1], [2], etc.\n")
    promptBuilder.append("- If information is conflicting, state so neutrally.\n")
    promptBuilder.append("- Be concise, clear, and direct.")

    val synthesisResult = router.routeAndGenerate(
      prompt = promptBuilder.toString(),
      context = context,
      options = GenerateOptions(
        targetCapability = AICapability.WEB_RESEARCH,
        temperature = 0.3f
      )
    )

    val (answer, modelUsed) = when (synthesisResult) {
      is AIProviderResult.Success -> Pair(synthesisResult.text, synthesisResult.modelUsed)
      is AIProviderResult.Error -> Pair("Synthesis provider error: ${synthesisResult.message}", null)
      is AIProviderResult.Streaming -> Pair("Synthesizing research...", null)
    }

    val confidence = if (sources.size >= 2 && answer.contains("[")) {
      FactConfidence.SOURCE_SUPPORTED
    } else {
      FactConfidence.AI_INTERPRETATION
    }

    ResearchResult(
      query = cleanQuery,
      synthesizedAnswer = answer,
      confidence = confidence,
      sources = sources,
      modelUsed = modelUsed,
      searchLatencyMs = System.currentTimeMillis() - startTime
    )
  }

  suspend fun fetchNews(category: NewsCategory): ResearchResult {
    return conductResearch(category.searchQuery)
  }

  private fun fetchWebSearchResults(query: String): List<WebSource> {
    val results = mutableListOf<WebSource>()
    try {
      // Use DuckDuckGo Instant Answer API first
      val encoded = URLEncoder.encode(query, "UTF-8")
      val ddgApiUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"

      val request = Request.Builder()
        .url(ddgApiUrl)
        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/115.0")
        .get()
        .build()

      val response = httpClient.newCall(request).execute()
      if (response.isSuccessful) {
        val jsonStr = response.body?.string() ?: ""
        val obj = JSONObject(jsonStr)
        val heading = obj.optString("Heading")
        val abstractText = obj.optString("AbstractText")
        val abstractUrl = obj.optString("AbstractURL")

        if (abstractText.isNotBlank()) {
          results.add(
            WebSource(
              title = heading.ifBlank { query },
              url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encoded" },
              snippet = abstractText,
              sourceDomain = if (abstractUrl.isNotBlank()) extractDomain(abstractUrl) else "DuckDuckGo"
            )
          )
        }

        val relatedTopics = obj.optJSONArray("RelatedTopics")
        if (relatedTopics != null) {
          for (i in 0 until minOf(relatedTopics.length(), 4)) {
            val topic = relatedTopics.optJSONObject(i) ?: continue
            val text = topic.optString("Text")
            val firstUrl = topic.optString("FirstURL")
            if (text.isNotBlank() && firstUrl.isNotBlank()) {
              results.add(
                WebSource(
                  title = text.take(60) + "...",
                  url = firstUrl,
                  snippet = text,
                  sourceDomain = extractDomain(firstUrl)
                )
              )
            }
          }
        }
      }

      // If instant answer was sparse, supplement with DuckDuckGo Lite HTML search
      if (results.isEmpty()) {
        val htmlUrl = "https://html.duckduckgo.com/html/?q=$encoded"
        val htmlReq = Request.Builder()
          .url(htmlUrl)
          .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
          .get()
          .build()

        val htmlResp = httpClient.newCall(htmlReq).execute()
        if (htmlResp.isSuccessful) {
          val html = htmlResp.body?.string() ?: ""
          results.addAll(parseHtmlResults(html))
        }
      }
    } catch (_: Exception) {}

    return results
  }

  private fun parseHtmlResults(html: String): List<WebSource> {
    val list = mutableListOf<WebSource>()
    try {
      // Simple regex parser for result snippet and links
      val titlePattern = Pattern.compile("<a class=\"result__url\" href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>")
      val snippetPattern = Pattern.compile("<a class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>")

      val titleMatcher = titlePattern.matcher(html)
      val snippetMatcher = snippetPattern.matcher(html)

      while (titleMatcher.find() && snippetMatcher.find() && list.size < 5) {
        val rawUrl = titleMatcher.group(1) ?: ""
        val url = if (rawUrl.startsWith("//duckduckgo.com/l/?uddg=")) {
          val decoded = rawUrl.substringAfter("uddg=").substringBefore("&")
          java.net.URLDecoder.decode(decoded, "UTF-8")
        } else rawUrl

        val snippet = snippetMatcher.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
        val domain = extractDomain(url)

        if (url.isNotBlank() && snippet.isNotBlank()) {
          list.add(
            WebSource(
              title = "Web Grounding: $domain",
              url = url,
              snippet = snippet,
              sourceDomain = domain
            )
          )
        }
      }
    } catch (_: Exception) {}
    return list
  }

  private fun extractDomain(url: String): String {
    return try {
      val uri = java.net.URI(url)
      val host = uri.host ?: ""
      host.removePrefix("www.")
    } catch (_: Exception) {
      "web"
    }
  }
}
