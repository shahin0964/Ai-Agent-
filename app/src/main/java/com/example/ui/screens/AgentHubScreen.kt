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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.companion.CompanionMode
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.FuturisticHeader
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.activeTheme

/**
 * Mobile Model Descriptor for registered Agents & Personas.
 */
data class AgentHubModule(
  val id: String,
  val name: String,
  val role: String,
  val description: String,
  val icon: ImageVector,
  val capabilities: List<String>,
  val targetScreen: Screen?,
  val targetCompanionMode: CompanionMode?,
  val colorAccent: Color
)

@Composable
fun AgentHubScreen(
  agentController: AgentController,
  onNavigateToScreen: (Screen) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val theme = activeTheme
  val personalityEngine = agentController.aiEngine?.personalityEngine
  val currentCompanionMode by personalityEngine?.companionMode?.collectAsState()
    ?: remember { mutableStateOf(CompanionMode.PERSONAL_ASSISTANT) }
  val routingPref by agentController.aiEngine?.routingConfig?.preferences?.collectAsState()
    ?: remember { mutableStateOf(null) }
  val automations by agentController.automationEngine?.automations?.collectAsState()
    ?: remember { mutableStateOf(emptyList()) }

  val agentModules = remember {
    listOf(
      AgentHubModule(
        id = "nexus_prime",
        name = "Nexus Prime",
        role = "Primary Multi-Provider Orchestrator",
        description = "Core intelligence conductor routing between Gemini, OpenAI, Claude, and local neural engines.",
        icon = Icons.Default.Psychology,
        capabilities = listOf("Multi-LLM", "Tool Execution", "Voice & Speech", "Context Memory"),
        targetScreen = Screen.Chat,
        targetCompanionMode = CompanionMode.PERSONAL_ASSISTANT,
        colorAccent = Color(0xFF00F0FF)
      ),
      AgentHubModule(
        id = "maya_ai",
        name = "Maya AI",
        role = "Empathetic Companion & Living Presence",
        description = "Proactive personal companion with mood dynamics, presence tracking, and adaptive conversation.",
        icon = Icons.Default.Person,
        capabilities = listOf("Proactive Check-ins", "Mood Rhythm", "Sleep Schedules", "Memory"),
        targetScreen = Screen.SettingsCompanion,
        targetCompanionMode = CompanionMode.COMPANION,
        colorAccent = Color(0xFFA855F7)
      ),
      AgentHubModule(
        id = "friday",
        name = "Friday",
        role = "Executive Assistant & Device Controller",
        description = "Structured tactical agent for calendar schedules, alarms, contacts, and rapid app actions.",
        icon = Icons.Default.Tune,
        capabilities = listOf("Calendar Insert", "Alarms", "Contacts", "SMS & Calls"),
        targetScreen = Screen.Voice,
        targetCompanionMode = CompanionMode.PROFESSIONAL,
        colorAccent = Color(0xFF38BDF8)
      ),
      AgentHubModule(
        id = "venom_core",
        name = "Venom Core",
        role = "Cyber Operations & Hardware Diagnostics",
        description = "Deep system monitoring module tracking battery, RAM, storage, network telemetry, and security.",
        icon = Icons.Default.Security,
        capabilities = listOf("Device Telemetry", "RAM & Storage", "Permission Guard", "KeyStore"),
        targetScreen = Screen.SettingsPermissions,
        targetCompanionMode = null,
        colorAccent = Color(0xFFFF0055)
      ),
      AgentHubModule(
        id = "research_agent",
        name = "Research Agent",
        role = "Deep Web Intelligence & Fact Synthesis",
        description = "Autonomous search engine synthesizing Wikipedia, multi-source citations, and global events.",
        icon = Icons.Default.Search,
        capabilities = listOf("Live Search", "Citation Extraction", "Fact Verification", "World Monitor"),
        targetScreen = Screen.Research,
        targetCompanionMode = null,
        colorAccent = Color(0xFF34D399)
      ),
      AgentHubModule(
        id = "vision_agent",
        name = "Vision Agent",
        role = "Real-Time Visual Reasoning & OCR",
        description = "Optical intelligence engine analyzing camera viewfinders, photos, and visual documents.",
        icon = Icons.Default.Visibility,
        capabilities = listOf("Camera Analysis", "OCR Text Extraction", "Object Classification"),
        targetScreen = Screen.Vision,
        targetCompanionMode = null,
        colorAccent = Color(0xFFF59E0B)
      ),
      AgentHubModule(
        id = "developer_agent",
        name = "Developer Agent",
        role = "Code Architecture & Algorithm Debugger",
        description = "Software engineering module specialized in Kotlin, Python, JS, refactoring, and logic debugging.",
        icon = Icons.Default.Code,
        capabilities = listOf("Code Generation", "Bug Resolution", "Architecture Review"),
        targetScreen = Screen.Developer,
        targetCompanionMode = null,
        colorAccent = Color(0xFF6366F1)
      ),
      AgentHubModule(
        id = "creator_agent",
        name = "Creator Agent",
        role = "UI/UX & Web Prototype Generator",
        description = "Creative web application generator building HTML, CSS, React components, and dynamic layouts.",
        icon = Icons.Default.AutoAwesome,
        capabilities = listOf("Full-Stack Web Gen", "Interactive Preview", "Design Tokens"),
        targetScreen = Screen.Creator,
        targetCompanionMode = null,
        colorAccent = Color(0xFFEC4899)
      ),
      AgentHubModule(
        id = "automation_agent",
        name = "Automation Agent",
        role = "Background Trigger & Routine Matrix",
        description = "Autonomous scheduler executing multi-step routines triggered by time, battery, and Wi-Fi.",
        icon = Icons.Default.GraphicEq,
        capabilities = listOf("Exact Alarms", "Battery Triggers", "Wi-Fi Events", "Multi-Step Chains"),
        targetScreen = Screen.Automation,
        targetCompanionMode = null,
        colorAccent = Color(0xFF10B981)
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(CyberTokens.spaceLarge),
    verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.testTag("agent_hub_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = theme.primary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "AGENT HUB",
            style = MaterialTheme.typography.titleMedium.copy(
              color = theme.primary,
              letterSpacing = 2.sp
            )
          )
          Text(
            text = "Specialized AI Modules & Personas",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(theme.primary.copy(alpha = 0.12f))
          .border(1.dp, theme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${agentModules.size} MODULES",
          style = MaterialTheme.typography.labelSmall.copy(color = theme.primary, fontSize = 10.sp)
        )
      }
    }

    // Agent Modules List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
    ) {
      items(agentModules, key = { it.id }) { agent ->
        val isActivePersona = agent.targetCompanionMode != null && agent.targetCompanionMode == currentCompanionMode
        val isAutomationRunning = agent.id == "automation_agent" && automations.any { it.enabled }
        val isProviderConfigured = routingPref?.primaryProviderId != null

        val statusLabel = when {
          isActivePersona -> "ACTIVE PERSONA"
          isAutomationRunning -> "${automations.count { it.enabled }} ROUTINES ACTIVE"
          agent.id == "nexus_prime" -> if (isProviderConfigured) "OPERATIONAL" else "STANDBY"
          else -> "READY"
        }

        val statusColor = when {
          isActivePersona -> StatusReady
          isAutomationRunning -> StatusReady
          statusLabel == "OPERATIONAL" || statusLabel == "READY" -> StatusReady
          else -> StatusWarning
        }

        FuturisticCard(
          headerText = agent.name,
          badgeText = statusLabel,
          badgeColor = statusColor,
          modifier = Modifier.testTag("agent_module_${agent.id}")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(agent.colorAccent.copy(alpha = 0.15f))
                  .border(1.dp, agent.colorAccent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = agent.icon,
                  contentDescription = agent.name,
                  tint = agent.colorAccent,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = agent.role,
                  style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary)
                )
                Text(
                  text = agent.description,
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
              }
            }

            // Capability Badges
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              agent.capabilities.take(3).forEach { cap ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = cap,
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = TextSecondary,
                      fontSize = 9.sp
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (agent.targetCompanionMode != null) {
                FuturisticButton(
                  text = if (isActivePersona) "Active Persona" else "Select Persona",
                  onClick = {
                    personalityEngine?.setCompanionMode(agent.targetCompanionMode)
                  },
                  isPrimary = isActivePersona,
                  modifier = Modifier.weight(1f)
                )
              }

              if (agent.targetScreen != null) {
                FuturisticButton(
                  text = "Launch Engine",
                  onClick = { onNavigateToScreen(agent.targetScreen) },
                  isPrimary = agent.targetCompanionMode == null,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    }
  }
}
