package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.ai.AIEngineContainer
import com.example.core.ai.model.ProviderConnectionStatus
import com.example.core.ai.provider.AIProvider
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

data class ProviderUIItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val defaultModel: String,
  val requiresKey: Boolean = true,
  val isCustom: Boolean = false
)

/**
 * Real AI Providers & API Key Architecture Screen.
 * Hardware-backed Secure KeyStore storage, live connection testing,
 * model discovery, and multi-provider failover routing.
 */
@Composable
fun AiProvidersSettingsScreen(
  onBack: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val aiEngine = agentController?.aiEngine ?: remember { AIEngineContainer.getInstance(context) }

  val routingPrefs by aiEngine.routingConfig.preferences.collectAsState()

  val providerList = remember {
    listOf(
      ProviderUIItem("google_gemini", "Google Gemini", "Gemini 2.5 Flash, Pro & Multimodal Vision", "gemini-2.5-flash"),
      ProviderUIItem("openai", "OpenAI", "GPT-4o, GPT-4o Mini & Multimodal Vision", "gpt-4o"),
      ProviderUIItem("anthropic", "Anthropic Claude", "Claude 3.5 Sonnet & Claude 3.5 Haiku", "claude-3-5-sonnet-20241022"),
      ProviderUIItem("custom_openai", "Custom Endpoint (Ollama / LocalAI)", "Self-hosted OpenAI-compatible REST server", "llama3", requiresKey = false, isCustom = true),
      ProviderUIItem("local_offline", "On-Device Neural Engine", "Embedded offline model fallback", "local-slm", requiresKey = false)
    )
  }

  var expandedProviderId by remember { mutableStateOf<String?>("google_gemini") }
  val apiKeysState = remember {
    mutableStateMapOf<String, String>().apply {
      providerList.forEach { item ->
        val existing = aiEngine.credentialStore.getApiKey(item.id) ?: ""
        put(item.id, existing)
      }
    }
  }

  var customBaseUrl by remember { mutableStateOf(routingPrefs.customBaseUrl) }
  val testStatuses = remember { mutableStateMapOf<String, String>() }
  val testingInProgress = remember { mutableStateMapOf<String, Boolean>() }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("ai_providers_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyanNeon
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "AI PROVIDER KERNEL",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = CyanNeon,
              letterSpacing = 1.sp
            )
          )
          Text(
            text = "Hardware KeyStore Encryption & Live Endpoints",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
        }
      }
    }

    // Routing Priority Architecture Card
    item {
      FuturisticCard(headerText = "Intelligent Provider Routing", badgeText = "Multi-Engine") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Primary Engine:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            StatusBadge(
              text = routingPrefs.primaryProviderId.uppercase(),
              indicatorColor = CyanNeon,
              isPulsing = false
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Secondary Engine:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            StatusBadge(
              text = routingPrefs.secondaryProviderId.uppercase(),
              indicatorColor = ElectricBlue,
              isPulsing = false
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Vision Subsystem:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            StatusBadge(
              text = routingPrefs.visionProviderId.uppercase(),
              indicatorColor = HologramViolet,
              isPulsing = false
            )
          }
        }
      }
    }

    // Provider List
    items(providerList, key = { it.id }) { item ->
      val isExpanded = expandedProviderId == item.id
      val provider = aiEngine.providers[item.id]
      val isConfigured = provider?.isConfigured ?: false
      val currentKey = apiKeysState[item.id] ?: ""
      val isTesting = testingInProgress[item.id] == true
      val statusMsg = testStatuses[item.id]

      GlassPanel(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { expandedProviderId = if (isExpanded) null else item.id }
      ) {
        Column(modifier = Modifier.padding(CyberTokens.spaceMedium)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(if (isConfigured) Color(0x3300F0FF) else Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (item.requiresKey) Icons.Default.Key else Icons.Default.Security,
                  contentDescription = null,
                  tint = if (isConfigured) CyanNeon else TextTertiary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                )
                Text(
                  text = item.defaultModel,
                  style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              StatusBadge(
                text = if (isConfigured) "CONFIGURED" else if (item.id == "local_offline") "OFFLINE ONLY" else "UNCONFIGURED",
                indicatorColor = if (isConfigured) StatusReady else StatusOffline,
                isPulsing = false
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
              )

              Spacer(modifier = Modifier.height(8.dp))

              if (item.isCustom) {
                Text("Base URL", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = customBaseUrl,
                  onValueChange = {
                    customBaseUrl = it
                    aiEngine.routingConfig.updateCustomEndpoint(it, routingPrefs.customModelId)
                  },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Color(0x3300F0FF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                  ),
                  shape = CyberTokens.shapeSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
              }

              if (item.requiresKey) {
                Text("API Key", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                  value = currentKey,
                  onValueChange = { apiKeysState[item.id] = it },
                  placeholder = { Text("Enter ${item.title} API Key...", style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)) },
                  modifier = Modifier.fillMaxWidth().testTag("key_input_${item.id}"),
                  visualTransformation = PasswordVisualTransformation(),
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Color(0x3300F0FF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                  ),
                  shape = CyberTokens.shapeSmall
                )
                Spacer(modifier = Modifier.height(10.dp))
              }

              // Buttons: Test Connection & Save Securely
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FuturisticButton(
                  text = if (isTesting) "Verifying..." else "Verify Connection",
                  onClick = {
                    val keyToTest = if (item.requiresKey) currentKey else "none"
                    if (item.requiresKey && keyToTest.isBlank()) {
                      testStatuses[item.id] = "Validation error: Key is empty."
                      return@FuturisticButton
                    }
                    testingInProgress[item.id] = true
                    scope.launch {
                      if (item.requiresKey) {
                        aiEngine.credentialStore.setApiKey(item.id, currentKey)
                      }
                      val testResult = provider?.testConnection()
                      testingInProgress[item.id] = false
                      testStatuses[item.id] = when (testResult?.status) {
                        ProviderConnectionStatus.CONNECTED -> "Verified: ${testResult.message} (${testResult.latencyMs}ms)"
                        ProviderConnectionStatus.INVALID_KEY -> "Authentication Failed: ${testResult.message}"
                        ProviderConnectionStatus.RATE_LIMITED -> "Quota / Rate Limit: ${testResult.message}"
                        ProviderConnectionStatus.NETWORK_ERROR -> "Network error: ${testResult.message}"
                        else -> testResult?.message ?: "Unknown connection status."
                      }
                    }
                  },
                  isPrimary = false,
                  modifier = Modifier.weight(1f).testTag("test_provider_${item.id}")
                )

                if (item.requiresKey) {
                  FuturisticButton(
                    text = "Save Securely",
                    onClick = {
                      if (currentKey.isNotBlank()) {
                        aiEngine.credentialStore.setApiKey(item.id, currentKey)
                        testStatuses[item.id] = "Key encrypted in hardware KeyStore."
                      } else {
                        aiEngine.credentialStore.clearApiKey(item.id)
                        testStatuses[item.id] = "Key removed from secure storage."
                      }
                    },
                    isPrimary = true,
                    modifier = Modifier.weight(1f).testTag("save_provider_${item.id}")
                  )
                }
              }

              // Set as Primary or Secondary Buttons
              if (item.id != "local_offline") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  FuturisticButton(
                    text = if (routingPrefs.primaryProviderId == item.id) "Is Primary" else "Set Primary",
                    onClick = { aiEngine.routingConfig.updatePrimary(item.id, item.defaultModel) },
                    isPrimary = routingPrefs.primaryProviderId == item.id,
                    modifier = Modifier.weight(1f)
                  )

                  FuturisticButton(
                    text = if (routingPrefs.secondaryProviderId == item.id) "Is Secondary" else "Set Secondary",
                    onClick = { aiEngine.routingConfig.updateSecondary(item.id, item.defaultModel) },
                    isPrimary = routingPrefs.secondaryProviderId == item.id,
                    modifier = Modifier.weight(1f)
                  )
                }
              }

              if (statusMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (statusMsg.startsWith("Verified") || statusMsg.startsWith("Key")) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (statusMsg.startsWith("Verified") || statusMsg.startsWith("Key")) StatusReady else StatusWarning,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = statusMsg,
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = if (statusMsg.startsWith("Verified") || statusMsg.startsWith("Key")) StatusReady else StatusWarning,
                      fontSize = 11.sp
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge)) }
  }
}
