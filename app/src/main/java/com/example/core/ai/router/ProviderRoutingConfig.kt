package com.example.core.ai.router

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoutingPreferences(
  val primaryProviderId: String = "google_gemini",
  val primaryModelId: String = "gemini-2.5-flash",
  val secondaryProviderId: String = "none",
  val secondaryModelId: String = "",
  val fallbackProviderId: String = "none",
  val visionProviderId: String = "google_gemini",
  val researchProviderId: String = "web_search",
  val customBaseUrl: String = "http://localhost:11434/v1",
  val customModelId: String = "llama3"
)

class RoutingConfigManager(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("aegis_ai_routing_prefs", Context.MODE_PRIVATE)

  private val _preferences = MutableStateFlow(loadPreferences())
  val preferences: StateFlow<RoutingPreferences> = _preferences.asStateFlow()

  private fun loadPreferences(): RoutingPreferences {
    return RoutingPreferences(
      primaryProviderId = prefs.getString("primary_provider", "google_gemini") ?: "google_gemini",
      primaryModelId = prefs.getString("primary_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
      secondaryProviderId = prefs.getString("secondary_provider", "none") ?: "none",
      secondaryModelId = prefs.getString("secondary_model", "") ?: "",
      fallbackProviderId = prefs.getString("fallback_provider", "none") ?: "none",
      visionProviderId = prefs.getString("vision_provider", "google_gemini") ?: "google_gemini",
      researchProviderId = prefs.getString("research_provider", "web_search") ?: "web_search",
      customBaseUrl = prefs.getString("custom_base_url", "http://localhost:11434/v1") ?: "http://localhost:11434/v1",
      customModelId = prefs.getString("custom_model_id", "llama3") ?: "llama3"
    )
  }

  fun updatePrimary(providerId: String, modelId: String) {
    prefs.edit()
      .putString("primary_provider", providerId)
      .putString("primary_model", modelId)
      .apply()
    _preferences.value = _preferences.value.copy(primaryProviderId = providerId, primaryModelId = modelId)
  }

  fun updateSecondary(providerId: String, modelId: String) {
    prefs.edit()
      .putString("secondary_provider", providerId)
      .putString("secondary_model", modelId)
      .apply()
    _preferences.value = _preferences.value.copy(secondaryProviderId = providerId, secondaryModelId = modelId)
  }

  fun updateFallback(providerId: String) {
    prefs.edit().putString("fallback_provider", providerId).apply()
    _preferences.value = _preferences.value.copy(fallbackProviderId = providerId)
  }

  fun updateVision(providerId: String) {
    prefs.edit().putString("vision_provider", providerId).apply()
    _preferences.value = _preferences.value.copy(visionProviderId = providerId)
  }

  fun updateCustomEndpoint(baseUrl: String, modelId: String) {
    prefs.edit()
      .putString("custom_base_url", baseUrl)
      .putString("custom_model_id", modelId)
      .apply()
    _preferences.value = _preferences.value.copy(customBaseUrl = baseUrl, customModelId = modelId)
  }
}
