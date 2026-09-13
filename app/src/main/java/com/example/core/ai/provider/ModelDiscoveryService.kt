package com.example.core.ai.provider

import com.example.core.ai.model.AIModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModelDiscoveryService(
  private val providers: Map<String, AIProvider>
) {
  private val _discoveredModels = MutableStateFlow<Map<String, List<AIModel>>>(emptyMap())
  val discoveredModels: StateFlow<Map<String, List<AIModel>>> = _discoveredModels.asStateFlow()

  suspend fun refreshAll() {
    val result = mutableMapOf<String, List<AIModel>>()
    for ((id, provider) in providers) {
      if (provider.isConfigured) {
        val models = provider.discoverModels()
        result[id] = models
      } else {
        result[id] = provider.availableModels
      }
    }
    _discoveredModels.value = result
  }

  suspend fun refreshProvider(providerId: String): List<AIModel> {
    val provider = providers[providerId] ?: return emptyList()
    val models = if (provider.isConfigured) provider.discoverModels() else provider.availableModels
    val current = _discoveredModels.value.toMutableMap()
    current[providerId] = models
    _discoveredModels.value = current
    return models
  }

  fun getModelsForProvider(providerId: String): List<AIModel> {
    return _discoveredModels.value[providerId] ?: providers[providerId]?.availableModels ?: emptyList()
  }
}
