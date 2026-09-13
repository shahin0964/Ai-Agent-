package com.example.core.ai.creator

import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.context.ConversationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class CreatorEngine(
  private val router: AIIntelligenceRouter
) {

  suspend fun generateCode(
    prompt: String,
    language: String = "Kotlin",
    context: ConversationContext = ConversationContext()
  ): CreatorOutput = withContext(Dispatchers.IO) {
    val fullPrompt = """
      Write high-quality, production-ready $language code for the following specification:
      $prompt
      
      Format your response with clean markdown code blocks:
      ```$language
      // code here
      ```
      Include concise comments explaining key architectural decisions.
    """.trimIndent()

    val result = router.routeAndGenerate(
      prompt = fullPrompt,
      context = context,
      options = GenerateOptions(
        targetCapability = AICapability.CODE_GENERATION,
        temperature = 0.2f,
        maxTokens = 2048
      )
    )

    val (text, modelUsed) = when (result) {
      is AIProviderResult.Success -> Pair(result.text, result.modelUsed)
      is AIProviderResult.Error -> Pair("Code generation error: ${result.message}", null)
      is AIProviderResult.Streaming -> Pair("// Generating code stream...", null)
    }

    val extractedFiles = extractCodeBlocks(text, language)

    CreatorOutput(
      prompt = prompt,
      type = CreatorOutputType.CODE,
      title = "$language Code: ${prompt.take(40)}...",
      files = extractedFiles,
      primaryText = text,
      modelUsed = modelUsed
    )
  }

  suspend fun debugCode(
    code: String,
    errorDescription: String
  ): CreatorOutput = withContext(Dispatchers.IO) {
    val fullPrompt = """
      Analyze the following code and resolve the reported issue:
      Reported Issue / Trace:
      $errorDescription
      
      Code Snippet:
      $code
      
      Provide:
      1. Root cause diagnosis.
      2. Corrected code block in markdown format.
    """.trimIndent()

    val result = router.routeAndGenerate(
      prompt = fullPrompt,
      options = GenerateOptions(
        targetCapability = AICapability.CODE_GENERATION,
        temperature = 0.2f,
        maxTokens = 2048
      )
    )

    val (text, modelUsed) = when (result) {
      is AIProviderResult.Success -> Pair(result.text, result.modelUsed)
      is AIProviderResult.Error -> Pair("Analysis error: ${result.message}", null)
      is AIProviderResult.Streaming -> Pair("// Analyzing trace...", null)
    }

    val extractedFiles = extractCodeBlocks(text, "Kotlin")

    CreatorOutput(
      prompt = errorDescription,
      type = CreatorOutputType.CODE,
      title = "Diagnostic Analysis: ${errorDescription.take(40)}",
      files = extractedFiles,
      primaryText = text,
      modelUsed = modelUsed
    )
  }

  suspend fun generateWebsiteProject(
    concept: String
  ): CreatorOutput = withContext(Dispatchers.IO) {
    val fullPrompt = """
      Generate a complete, modern responsive single-page web application for:
      $concept
      
      Output three distinct markdown code blocks:
      1. `index.html` (Semantic HTML5 markup)
      2. `styles.css` (Clean, modern CSS styling with neon accents and mobile responsiveness)
      3. `app.js` (Interactive JavaScript logic)
    """.trimIndent()

    val result = router.routeAndGenerate(
      prompt = fullPrompt,
      options = GenerateOptions(
        targetCapability = AICapability.WEBSITE_GENERATION,
        temperature = 0.4f,
        maxTokens = 3000
      )
    )

    val (text, modelUsed) = when (result) {
      is AIProviderResult.Success -> Pair(result.text, result.modelUsed)
      is AIProviderResult.Error -> Pair("Project generation error: ${result.message}", null)
      is AIProviderResult.Streaming -> Pair("Building web project files...", null)
    }

    val files = extractCodeBlocks(text, "html")

    CreatorOutput(
      prompt = concept,
      type = CreatorOutputType.PROJECT,
      title = "Web Project: ${concept.take(35)}",
      files = files,
      primaryText = text,
      modelUsed = modelUsed
    )
  }

  private fun extractCodeBlocks(markdown: String, defaultLang: String): List<GeneratedFile> {
    val files = mutableListOf<GeneratedFile>()
    val pattern = Pattern.compile("```([a-zA-Z0-9_-]+)?\\s*\\n([\\s\\S]*?)```")
    val matcher = pattern.matcher(markdown)

    var fileIndex = 1
    while (matcher.find()) {
      val lang = matcher.group(1)?.lowercase() ?: defaultLang.lowercase()
      val code = matcher.group(2)?.trim() ?: ""

      val ext = when (lang) {
        "kotlin", "kt" -> "kt"
        "java" -> "java"
        "python", "py" -> "py"
        "html" -> "html"
        "css" -> "css"
        "javascript", "js" -> "js"
        "json" -> "json"
        else -> "txt"
      }

      val fileName = when (ext) {
        "html" -> "index.html"
        "css" -> "styles.css"
        "js" -> "app.js"
        "kt" -> if (fileIndex == 1) "MainComponent.kt" else "Component$fileIndex.kt"
        else -> "Source$fileIndex.$ext"
      }

      files.add(GeneratedFile(fileName, lang, code))
      fileIndex++
    }

    if (files.isEmpty() && markdown.isNotBlank()) {
      files.add(GeneratedFile("Output.txt", "text", markdown))
    }

    return files
  }
}
