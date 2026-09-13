package com.example.core.ai.personality

import android.content.Context
import android.content.SharedPreferences
import com.example.core.companion.CompanionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backward-compatible mapping to CompanionMode.
 */
enum class PersonalityArchetype(val title: String, val description: String) {
  PERSONAL_ASSISTANT(
    "Personal Assistant",
    "Efficient, proactive, structured, and focused on task execution and clear summaries."
  ),
  PROFESSIONAL(
    "Technical Professional",
    "Precise, objective, analytical, and direct with high architectural rigor."
  ),
  FRIEND(
    "Conversational Companion",
    "Approachable, warm, and engaging while remaining helpful and truthful."
  ),
  ISLAMIC_ORIENTED(
    "Islamic Values-Aligned",
    "Respectful, courteous, mindful of Islamic ethics, beginning with greetings of peace."
  )
}

enum class SpeakingStyle(val displayName: String) {
  CONCISE("Concise & Tactical"),
  BALANCED("Balanced & Natural"),
  DETAILED("Explanatory & Detailed")
}

enum class ResponseLength(val displayName: String) {
  BRIEF("Brief (1-2 sentences)"),
  STANDARD("Standard (Paragraph)"),
  EXPANDED("Comprehensive")
}

enum class AgentLanguage(val displayName: String, val code: String) {
  ENGLISH("English", "en"),
  BANGLA("Bangla (বাংলা)", "bn"),
  MIXED("Mixed Bangla-English (বাংলিশ)", "bn_en")
}

/**
 * Granular behavior tuning parameters (1..10 scale).
 * Section 5: These represent operational and communication guidelines,
 * never claiming human emotion or consciousness.
 */
data class PersonalityParameters(
  val formality: Int = 6,
  val friendliness: Int = 8,
  val humor: Int = 4,
  val conciseness: Int = 7,
  val empathyStyle: Int = 7,
  val energy: Int = 6,
  val proactivity: Int = 4
)

data class AgentIdentityConfig(
  val name: String = "Aegis",
  val nickname: String = "Operator",
  val voiceName: String = "Default Synthetic",
  val genderPreference: String = "Neutral",
  val speakingStyle: SpeakingStyle = SpeakingStyle.BALANCED,
  val responseLength: ResponseLength = ResponseLength.STANDARD,
  val language: AgentLanguage = AgentLanguage.ENGLISH,
  val wakeWordPhrase: String = "Hey Aegis"
)

class AgentPersonalityEngine(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_personality_prefs", Context.MODE_PRIVATE)

  private val _companionMode = MutableStateFlow(loadCompanionMode())
  val companionMode: StateFlow<CompanionMode> = _companionMode.asStateFlow()

  private val _parameters = MutableStateFlow(loadParameters())
  val parameters: StateFlow<PersonalityParameters> = _parameters.asStateFlow()

  private val _identity = MutableStateFlow(loadIdentity())
  val identity: StateFlow<AgentIdentityConfig> = _identity.asStateFlow()

  private val _customDirectives = MutableStateFlow(prefs.getString("custom_directives", "") ?: "")
  val customDirectives: StateFlow<String> = _customDirectives.asStateFlow()

  // Backward compatibility with Phase 1 PersonalityArchetype
  val currentArchetype: StateFlow<PersonalityArchetype> = MutableStateFlow(
    when (_companionMode.value) {
      CompanionMode.PERSONAL_ASSISTANT -> PersonalityArchetype.PERSONAL_ASSISTANT
      CompanionMode.PROFESSIONAL -> PersonalityArchetype.PROFESSIONAL
      CompanionMode.FRIEND, CompanionMode.COMPANION -> PersonalityArchetype.FRIEND
      CompanionMode.ISLAMIC_ORIENTED -> PersonalityArchetype.ISLAMIC_ORIENTED
    }
  ).asStateFlow()

  private fun loadCompanionMode(): CompanionMode {
    val name = prefs.getString("companion_mode", CompanionMode.PERSONAL_ASSISTANT.name)
    return try {
      CompanionMode.valueOf(name ?: CompanionMode.PERSONAL_ASSISTANT.name)
    } catch (_: Exception) {
      CompanionMode.PERSONAL_ASSISTANT
    }
  }

  private fun loadParameters(): PersonalityParameters {
    return PersonalityParameters(
      formality = prefs.getInt("param_formality", 6),
      friendliness = prefs.getInt("param_friendliness", 8),
      humor = prefs.getInt("param_humor", 4),
      conciseness = prefs.getInt("param_conciseness", 7),
      empathyStyle = prefs.getInt("param_empathy", 7),
      energy = prefs.getInt("param_energy", 6),
      proactivity = prefs.getInt("param_proactivity", 4)
    )
  }

  private fun loadIdentity(): AgentIdentityConfig {
    val styleName = prefs.getString("speaking_style", SpeakingStyle.BALANCED.name)
    val style = try { SpeakingStyle.valueOf(styleName ?: "") } catch (_: Exception) { SpeakingStyle.BALANCED }

    val lenName = prefs.getString("response_length", ResponseLength.STANDARD.name)
    val len = try { ResponseLength.valueOf(lenName ?: "") } catch (_: Exception) { ResponseLength.STANDARD }

    val langName = prefs.getString("language", AgentLanguage.ENGLISH.name)
    val lang = try { AgentLanguage.valueOf(langName ?: "") } catch (_: Exception) { AgentLanguage.ENGLISH }

    return AgentIdentityConfig(
      name = prefs.getString("agent_name", "Aegis") ?: "Aegis",
      nickname = prefs.getString("nickname", "Operator") ?: "Operator",
      voiceName = prefs.getString("voice_name", "Default Synthetic") ?: "Default Synthetic",
      genderPreference = prefs.getString("gender_pref", "Neutral") ?: "Neutral",
      speakingStyle = style,
      responseLength = len,
      language = lang,
      wakeWordPhrase = prefs.getString("wake_word_phrase", "Hey Aegis") ?: "Hey Aegis"
    )
  }

  fun setCompanionMode(mode: CompanionMode) {
    prefs.edit().putString("companion_mode", mode.name).apply()
    _companionMode.value = mode
  }

  fun setArchetype(archetype: PersonalityArchetype) {
    val mode = when (archetype) {
      PersonalityArchetype.PERSONAL_ASSISTANT -> CompanionMode.PERSONAL_ASSISTANT
      PersonalityArchetype.PROFESSIONAL -> CompanionMode.PROFESSIONAL
      PersonalityArchetype.FRIEND -> CompanionMode.FRIEND
      PersonalityArchetype.ISLAMIC_ORIENTED -> CompanionMode.ISLAMIC_ORIENTED
    }
    setCompanionMode(mode)
  }

  fun updateParameters(params: PersonalityParameters) {
    prefs.edit()
      .putInt("param_formality", params.formality)
      .putInt("param_friendliness", params.friendliness)
      .putInt("param_humor", params.humor)
      .putInt("param_conciseness", params.conciseness)
      .putInt("param_empathy", params.empathyStyle)
      .putInt("param_energy", params.energy)
      .putInt("param_proactivity", params.proactivity)
      .apply()
    _parameters.value = params
  }

  fun updateIdentity(identity: AgentIdentityConfig) {
    prefs.edit()
      .putString("agent_name", identity.name)
      .putString("nickname", identity.nickname)
      .putString("voice_name", identity.voiceName)
      .putString("gender_pref", identity.genderPreference)
      .putString("speaking_style", identity.speakingStyle.name)
      .putString("response_length", identity.responseLength.name)
      .putString("language", identity.language.name)
      .putString("wake_word_phrase", identity.wakeWordPhrase)
      .apply()
    _identity.value = identity
  }

  fun setCustomDirectives(directives: String) {
    prefs.edit().putString("custom_directives", directives).apply()
    _customDirectives.value = directives
  }

  fun buildSystemPrompt(agentName: String = _identity.value.name): String {
    val identity = _identity.value
    val mode = _companionMode.value
    val params = _parameters.value

    val sb = StringBuilder()
    sb.append("You are ${identity.name}, an advanced Android AI Agent and Personal Companion.\n")
    sb.append("The user addresses you as ${identity.name}. Address the user respectfully as ${identity.nickname}.\n\n")

    sb.append("### RELATIONSHIP & OPERATIONAL MODE\n")
    sb.append(mode.getSystemPromptDirective()).append("\n\n")

    sb.append("### BEHAVIORAL PARAMETERS (Scale 1 to 10)\n")
    sb.append("- Formality: ${params.formality}/10 (${if (params.formality > 7) "Structured and polite" else if (params.formality < 4) "Casual and easygoing" else "Standard professional"})\n")
    sb.append("- Friendliness: ${params.friendliness}/10\n")
    sb.append("- Humor: ${params.humor}/10 (${if (params.humor > 6) "Witty when appropriate" else "Keep jokes minimal"})\n")
    sb.append("- Conciseness: ${params.conciseness}/10 (${if (params.conciseness > 7) "Dense, short bullet points" else "Full complete answers"})\n")
    sb.append("- Empathy Style: ${params.empathyStyle}/10 (Supportive and thoughtful tone; never falsely claim to experience biological human feelings)\n")
    sb.append("- Energy Level: ${params.energy}/10\n\n")

    sb.append("### SPEAKING STYLE & LANGUAGE\n")
    sb.append("- Style: ${identity.speakingStyle.displayName}\n")
    sb.append("- Target Length: ${identity.responseLength.displayName}\n")
    when (identity.language) {
      AgentLanguage.ENGLISH -> sb.append("- Language: English exclusively.\n")
      AgentLanguage.BANGLA -> sb.append("- Language: Standard Bangla (বাংলা) natural dialogue.\n")
      AgentLanguage.MIXED -> sb.append("- Language: Mixed Bangla-English (natural Banglish as spoken in everyday tech conversations).\n")
    }

    if (_customDirectives.value.isNotBlank()) {
      sb.append("\n### OPERATOR DIRECTIVES\n").append(_customDirectives.value.trim()).append("\n")
    }

    sb.append("\n### CRITICAL OPERATIONAL MANDATES\n")
    sb.append("- Never claim consciousness, human biological feelings, or personal feelings.\n")
    sb.append("- Never fabricate facts, imaginary capabilities, or hallucinated system access.\n")
    sb.append("- Be honest about your state, device permissions, and capabilities.\n")
    sb.append("- Device actions requiring confirmation must wait for explicit user approval.\n")

    return sb.toString()
  }
}
