package com.example.core.ai.memory

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class MemoryCommandResult {
  data class Remembered(val item: MemoryEntity) : MemoryCommandResult()
  data class Forgotten(val count: Int, val description: String) : MemoryCommandResult()
  data class Recalled(val memories: List<MemoryEntity>) : MemoryCommandResult()
  data class Error(val reason: String) : MemoryCommandResult()
  object NotAMemoryCommand : MemoryCommandResult()
}

class MemoryManager(
  private val context: Context,
  private val database: MemoryDatabase = MemoryDatabase.getInstance(context)
) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_memory_settings", Context.MODE_PRIVATE)

  private val _settings = MutableStateFlow(loadSettings())
  val settings: StateFlow<MemorySettings> = _settings.asStateFlow()

  val allMemoriesFlow: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemoriesFlow()

  private fun loadSettings(): MemorySettings {
    return MemorySettings(
      memoryEnabled = prefs.getBoolean("memory_enabled", true),
      longTermMemoryEnabled = prefs.getBoolean("long_term_memory_enabled", true),
      autoSaveEnabled = prefs.getBoolean("auto_save_enabled", false),
      researchContextEnabled = prefs.getBoolean("research_context_enabled", true),
      voiceContextEnabled = prefs.getBoolean("voice_context_enabled", true)
    )
  }

  fun updateSettings(newSettings: MemorySettings) {
    prefs.edit()
      .putBoolean("memory_enabled", newSettings.memoryEnabled)
      .putBoolean("long_term_memory_enabled", newSettings.longTermMemoryEnabled)
      .putBoolean("auto_save_enabled", newSettings.autoSaveEnabled)
      .putBoolean("research_context_enabled", newSettings.researchContextEnabled)
      .putBoolean("voice_context_enabled", newSettings.voiceContextEnabled)
      .apply()
    _settings.value = newSettings
  }

  suspend fun handleMemoryCommand(input: String): MemoryCommandResult {
    val clean = input.trim().lowercase()

    // 1. "Remember that ..."
    if (clean.startsWith("remember that ") || clean.startsWith("remember: ") || clean.startsWith("remember ")) {
      val rawContent = input.trim().substringAfter("remember that ", "")
        .ifBlank { input.trim().substringAfter("remember: ", "") }
        .ifBlank { input.trim().substringAfter("remember ", "") }

      if (rawContent.isBlank()) {
        return MemoryCommandResult.Error("Please specify what you would like me to remember.")
      }

      if (!_settings.value.memoryEnabled || !_settings.value.longTermMemoryEnabled) {
        return MemoryCommandResult.Error("Long-term memory is currently disabled in Memory settings.")
      }

      val entity = MemoryEntity(
        content = rawContent.trim(),
        category = categorize(rawContent),
        source = "Operator Command",
        userApproved = true
      )
      database.memoryDao().insertMemory(entity)
      return MemoryCommandResult.Remembered(entity)
    }

    // 2. "What do you remember about me" / "What do you remember"
    if (clean.contains("what do you remember") || clean.contains("list my memories") || clean.contains("show memory")) {
      val all = database.memoryDao().getAllMemories()
      return MemoryCommandResult.Recalled(all)
    }

    // 3. "Forget everything about ..." / "Forget that ..."
    if (clean.startsWith("forget everything about ") || clean.startsWith("forget that ") || clean.startsWith("forget ")) {
      val target = input.trim().substringAfter("forget everything about ", "")
        .ifBlank { input.trim().substringAfter("forget that ", "") }
        .ifBlank { input.trim().substringAfter("forget ", "") }

      if (target.isBlank() || target.contains("everything") || target.contains("all")) {
        database.memoryDao().deleteAll()
        return MemoryCommandResult.Forgotten(1, "all long-term memories")
      }

      val deletedCount = database.memoryDao().deleteMatching(target.trim())
      return MemoryCommandResult.Forgotten(deletedCount, target.trim())
    }

    // 4. "Delete all memory"
    if (clean == "delete all memories" || clean == "clear all memories" || clean == "delete all memory") {
      database.memoryDao().deleteAll()
      return MemoryCommandResult.Forgotten(1, "all stored memories")
    }

    return MemoryCommandResult.NotAMemoryCommand
  }

  suspend fun saveMemoryExplicit(content: String, category: MemoryCategory = MemoryCategory.GENERAL) {
    val entity = MemoryEntity(
      content = content.trim(),
      category = category.name,
      userApproved = true
    )
    database.memoryDao().insertMemory(entity)
  }

  suspend fun deleteMemory(id: String) {
    database.memoryDao().deleteById(id)
  }

  suspend fun deleteAll() {
    database.memoryDao().deleteAll()
  }

  suspend fun getRelevantContextPrompt(query: String): String? {
    if (!_settings.value.memoryEnabled || !_settings.value.longTermMemoryEnabled) return null

    val all = database.memoryDao().getAllMemories()
    if (all.isEmpty()) return null

    val sb = StringBuilder("User Known Long-Term Memories:\n")
    all.take(6).forEach { mem ->
      sb.append("- ").append(mem.content).append("\n")
    }
    return sb.toString()
  }

  private fun categorize(text: String): String {
    val lower = text.lowercase()
    return when {
      lower.contains("prefer") || lower.contains("like") || lower.contains("dislike") || lower.contains("favorite") ->
        MemoryCategory.USER_PREFERENCE.name
      lower.contains("project") || lower.contains("app") || lower.contains("work") || lower.contains("code") ->
        MemoryCategory.PROJECT.name
      lower.contains("my name") || lower.contains("i am") || lower.contains("live in") || lower.contains("birthday") ->
        MemoryCategory.FACTUAL.name
      else -> MemoryCategory.GENERAL.name
    }
  }
}
