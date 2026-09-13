package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Workspaces
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.companion.CompanionMode
import com.example.core.model.AgentState
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
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
 * Standard live status state for Agent Town desks.
 * Must strictly be one of: "IDLE", "WORKING", "THINKING", "SPEAKING", "OFFLINE".
 */
enum class TownAgentStatus(
  val label: String,
  val indicatorColor: Color
) {
  IDLE("IDLE", Color(0xFF10B981)),       // Emerald Green
  WORKING("WORKING", Color(0xFF38BDF8)), // Sky Blue
  THINKING("THINKING", Color(0xFFA855F7)), // Holographic Violet
  SPEAKING("SPEAKING", Color(0xFF00F0FF)), // Luminous Cyan
  OFFLINE("OFFLINE", Color(0xFF64748B))  // Muted Slate
}

/**
 * Sector zones dividing the 2D office floor plan.
 */
enum class OfficeSector(val title: String, val badge: String) {
  ALL("All Stations", "9 STATIONS"),
  EXECUTIVE("Executive Plaza", "3 STATIONS"),
  OPERATIONS("Operations Bay", "2 STATIONS"),
  LABS("Intelligence Labs", "4 STATIONS")
}

/**
 * 2D Desk/Station data container.
 */
data class AgentTownDesk(
  val id: String,
  val deskCode: String,
  val name: String,
  val role: String,
  val sector: OfficeSector,
  val icon: ImageVector,
  val accentColor: Color,
  val targetScreen: Screen?,
  val targetCompanionMode: CompanionMode?,
  val liveStatus: TownAgentStatus,
  val currentTask: String?,
  val activeSkill: String?
)

@Composable
fun AgentTownScreen(
  agentController: AgentController,
  onNavigateToScreen: (Screen) -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val theme = activeTheme
  var selectedSector by remember { mutableStateOf(OfficeSector.ALL) }

  // Observe live real-time state from AgentController
  val agentState by agentController.agentState.collectAsState()
  val systemActivity by agentController.systemActivity.collectAsState()
  val pendingAction by agentController.pendingConfirmationAction.collectAsState()
  val personalityEngine = agentController.aiEngine?.personalityEngine
  val currentCompanionMode by personalityEngine?.companionMode?.collectAsState()
    ?: remember { mutableStateOf(CompanionMode.PERSONAL_ASSISTANT) }
  val automations by agentController.automationEngine?.automations?.collectAsState()
    ?: remember { mutableStateOf(emptyList()) }
  val routingPref by agentController.aiEngine?.routingConfig?.preferences?.collectAsState()
    ?: remember { mutableStateOf(null) }

  // Compute real status for Nexus Prime
  val nexusStatus = when (agentState) {
    AgentState.PROCESSING -> TownAgentStatus.THINKING
    AgentState.SPEAKING -> TownAgentStatus.SPEAKING
    AgentState.LISTENING -> TownAgentStatus.WORKING
    AgentState.OFFLINE, AgentState.ERROR -> TownAgentStatus.OFFLINE
    AgentState.IDLE, AgentState.INITIALIZING, AgentState.PAUSED -> TownAgentStatus.IDLE
  }
  val nexusTask = when {
    pendingAction != null -> pendingAction?.confirmationPrompt ?: pendingAction?.description
    agentState == AgentState.PROCESSING || agentState == AgentState.SPEAKING -> {
      systemActivity.detail.takeIf { it.isNotBlank() && it != "System initialized and awaiting command" }
    }
    else -> null
  }
  val nexusSkill = when {
    pendingAction != null -> pendingAction?.type?.label
    routingPref?.primaryProviderId != null -> "Multi-LLM (${routingPref?.primaryProviderId?.replace("_", " ")?.uppercase()})"
    else -> "Orchestration & Voice Engine"
  }

  // Compute real status for Maya AI
  val isMayaActive = currentCompanionMode == CompanionMode.COMPANION
  val mayaStatus = when {
    isMayaActive && agentState == AgentState.SPEAKING -> TownAgentStatus.SPEAKING
    isMayaActive && agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    isMayaActive && agentState == AgentState.LISTENING -> TownAgentStatus.WORKING
    isMayaActive -> TownAgentStatus.IDLE
    else -> TownAgentStatus.IDLE
  }
  val mayaTask = if (isMayaActive) "Active companion persona" else null
  val mayaSkill = "Empathetic Presence & Mood Rhythm"

  // Compute real status for Friday
  val isFridayActive = currentCompanionMode == CompanionMode.PROFESSIONAL
  val fridayStatus = when {
    isFridayActive && agentState == AgentState.SPEAKING -> TownAgentStatus.SPEAKING
    isFridayActive && agentState == AgentState.LISTENING -> TownAgentStatus.WORKING
    isFridayActive && agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    pendingAction != null -> TownAgentStatus.WORKING
    else -> TownAgentStatus.IDLE
  }
  val fridayTask = pendingAction?.description
  val fridaySkill = "Executive Device Controls & System APIs"

  // Compute real status for Venom Core
  val venomStatus = when {
    pendingAction != null -> TownAgentStatus.WORKING
    agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    else -> TownAgentStatus.IDLE
  }
  val venomTask = pendingAction?.description ?: "Capability & Permission Security active"
  val venomSkill = "Android Telemetry & Permission Guard"

  // Compute real status for Automation Agent
  val enabledAutomationsCount = automations.count { it.enabled }
  val automationStatus = when {
    agentState == AgentState.PROCESSING && systemActivity.label.contains("Routine", ignoreCase = true) -> TownAgentStatus.WORKING
    enabledAutomationsCount > 0 -> TownAgentStatus.IDLE
    automations.isNotEmpty() -> TownAgentStatus.IDLE
    else -> TownAgentStatus.OFFLINE
  }
  val automationTask = if (enabledAutomationsCount > 0) {
    "$enabledAutomationsCount background routine(s) scheduled"
  } else null
  val automationSkill = if (automations.isNotEmpty()) "Exact Alarms & Hardware Event Triggers" else null

  // Compute real status for Research Agent
  val isResearching = systemActivity.label.contains("Research", ignoreCase = true) || systemActivity.label.contains("Web", ignoreCase = true)
  val researchStatus = when {
    isResearching && agentState == AgentState.PROCESSING -> TownAgentStatus.WORKING
    agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    else -> TownAgentStatus.IDLE
  }
  val researchTask = if (isResearching) systemActivity.detail else null
  val researchSkill = "Multi-Source Web Citation Engine"

  // Compute real status for Vision Agent
  val isVisionActive = systemActivity.label.contains("Vision", ignoreCase = true) || systemActivity.label.contains("Image", ignoreCase = true)
  val visionStatus = when {
    isVisionActive && agentState == AgentState.PROCESSING -> TownAgentStatus.WORKING
    agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    else -> TownAgentStatus.IDLE
  }
  val visionTask = if (isVisionActive) systemActivity.detail else null
  val visionSkill = "Optical Recognition & Frame Reasoning"

  // Compute real status for Developer Agent
  val isCoding = systemActivity.label.contains("Code", ignoreCase = true) || systemActivity.label.contains("Dev", ignoreCase = true)
  val devStatus = when {
    isCoding && agentState == AgentState.PROCESSING -> TownAgentStatus.WORKING
    agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    else -> TownAgentStatus.IDLE
  }
  val devTask = if (isCoding) systemActivity.detail else null
  val devSkill = "Kotlin & Algorithm Synthesis"

  // Compute real status for Creator Agent
  val isCreating = systemActivity.label.contains("UI", ignoreCase = true) || systemActivity.label.contains("Create", ignoreCase = true)
  val creatorStatus = when {
    isCreating && agentState == AgentState.PROCESSING -> TownAgentStatus.WORKING
    agentState == AgentState.PROCESSING -> TownAgentStatus.THINKING
    else -> TownAgentStatus.IDLE
  }
  val creatorTask = if (isCreating) systemActivity.detail else null
  val creatorSkill = "Dynamic Web Component Studio"

  // All 9 Desks with real data bindings
  val desks = remember(
    nexusStatus, nexusTask, nexusSkill,
    mayaStatus, mayaTask,
    fridayStatus, fridayTask,
    venomStatus, venomTask,
    automationStatus, automationTask, automationSkill,
    researchStatus, researchTask,
    visionStatus, visionTask,
    devStatus, devTask,
    creatorStatus, creatorTask
  ) {
    listOf(
      AgentTownDesk(
        id = "nexus_prime",
        deskCode = "DESK 01",
        name = "Nexus Prime",
        role = "Primary AI Orchestrator",
        sector = OfficeSector.EXECUTIVE,
        icon = Icons.Default.Psychology,
        accentColor = Color(0xFF00F0FF),
        targetScreen = Screen.Chat,
        targetCompanionMode = CompanionMode.PERSONAL_ASSISTANT,
        liveStatus = nexusStatus,
        currentTask = nexusTask,
        activeSkill = nexusSkill
      ),
      AgentTownDesk(
        id = "maya_ai",
        deskCode = "DESK 02",
        name = "Maya AI",
        role = "Empathetic Companion",
        sector = OfficeSector.EXECUTIVE,
        icon = Icons.Default.Person,
        accentColor = Color(0xFFA855F7),
        targetScreen = Screen.SettingsCompanion,
        targetCompanionMode = CompanionMode.COMPANION,
        liveStatus = mayaStatus,
        currentTask = mayaTask,
        activeSkill = mayaSkill
      ),
      AgentTownDesk(
        id = "friday",
        deskCode = "DESK 03",
        name = "Friday",
        role = "Executive Assistant",
        sector = OfficeSector.EXECUTIVE,
        icon = Icons.Default.Tune,
        accentColor = Color(0xFF38BDF8),
        targetScreen = Screen.Voice,
        targetCompanionMode = CompanionMode.PROFESSIONAL,
        liveStatus = fridayStatus,
        currentTask = fridayTask,
        activeSkill = fridaySkill
      ),
      AgentTownDesk(
        id = "venom_core",
        deskCode = "DESK 04",
        name = "Venom Core",
        role = "Cyber Security & Telemetry",
        sector = OfficeSector.OPERATIONS,
        icon = Icons.Default.Security,
        accentColor = Color(0xFFFF0055),
        targetScreen = Screen.SettingsPermissions,
        targetCompanionMode = null,
        liveStatus = venomStatus,
        currentTask = venomTask,
        activeSkill = venomSkill
      ),
      AgentTownDesk(
        id = "automation_agent",
        deskCode = "DESK 05",
        name = "Automation Agent",
        role = "Background Routine Matrix",
        sector = OfficeSector.OPERATIONS,
        icon = Icons.Default.GraphicEq,
        accentColor = Color(0xFF10B981),
        targetScreen = Screen.Automation,
        targetCompanionMode = null,
        liveStatus = automationStatus,
        currentTask = automationTask,
        activeSkill = automationSkill
      ),
      AgentTownDesk(
        id = "research_agent",
        deskCode = "DESK 06",
        name = "Research Agent",
        role = "Deep Web Intelligence",
        sector = OfficeSector.LABS,
        icon = Icons.Default.Search,
        accentColor = Color(0xFF34D399),
        targetScreen = Screen.Research,
        targetCompanionMode = null,
        liveStatus = researchStatus,
        currentTask = researchTask,
        activeSkill = researchSkill
      ),
      AgentTownDesk(
        id = "vision_agent",
        deskCode = "DESK 07",
        name = "Vision Agent",
        role = "Visual Reasoning & OCR",
        sector = OfficeSector.LABS,
        icon = Icons.Default.Visibility,
        accentColor = Color(0xFFF59E0B),
        targetScreen = Screen.Vision,
        targetCompanionMode = null,
        liveStatus = visionStatus,
        currentTask = visionTask,
        activeSkill = visionSkill
      ),
      AgentTownDesk(
        id = "developer_agent",
        deskCode = "DESK 08",
        name = "Developer Agent",
        role = "Code Architecture & Logic",
        sector = OfficeSector.LABS,
        icon = Icons.Default.Code,
        accentColor = Color(0xFF6366F1),
        targetScreen = Screen.Developer,
        targetCompanionMode = null,
        liveStatus = devStatus,
        currentTask = devTask,
        activeSkill = devSkill
      ),
      AgentTownDesk(
        id = "creator_agent",
        deskCode = "DESK 09",
        name = "Creator Agent",
        role = "Web UI & UX Prototype Gen",
        sector = OfficeSector.LABS,
        icon = Icons.Default.AutoAwesome,
        accentColor = Color(0xFFEC4899),
        targetScreen = Screen.Creator,
        targetCompanionMode = null,
        liveStatus = creatorStatus,
        currentTask = creatorTask,
        activeSkill = creatorSkill
      )
    )
  }

  val filteredDesks = remember(selectedSector, desks) {
    if (selectedSector == OfficeSector.ALL) desks
    else desks.filter { it.sector == selectedSector }
  }

  val activeDesksCount = remember(desks) {
    desks.count { it.liveStatus != TownAgentStatus.OFFLINE }
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.size(36.dp).testTag("agent_town_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = theme.primary
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
          Text(
            text = "AGENT TOWN",
            style = MaterialTheme.typography.titleMedium.copy(
              color = theme.primary,
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "2D Mobile Workspace & Stations",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(theme.primary.copy(alpha = 0.12f))
          .border(1.dp, theme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "$activeDesksCount/9 ACTIVE",
          style = MaterialTheme.typography.labelSmall.copy(
            color = theme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
          )
        )
      }
    }

    // 2D Office Floor Map Overview (Compact Futuristic Blueprint)
    OfficeFloorMinimap(
      desks = desks,
      onSelectDesk = { desk ->
        if (desk.targetCompanionMode != null && personalityEngine != null) {
          personalityEngine.setCompanionMode(desk.targetCompanionMode)
        }
        desk.targetScreen?.let { onNavigateToScreen(it) }
      }
    )

    // Sector Filter Tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      OfficeSector.entries.forEach { sector ->
        val isSelected = sector == selectedSector
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) theme.primary.copy(alpha = 0.2f) else Color(0xFF0F172A))
            .border(
              1.dp,
              if (isSelected) theme.primary else Color(0xFF334155),
              RoundedCornerShape(6.dp)
            )
            .clickable { selectedSector = sector }
            .padding(vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = when (sector) {
              OfficeSector.ALL -> "ALL"
              OfficeSector.EXECUTIVE -> "EXEC"
              OfficeSector.OPERATIONS -> "OPS"
              OfficeSector.LABS -> "LABS"
            },
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSelected) theme.primary else TextSecondary,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 10.sp
            )
          )
        }
      }
    }

    // 2D Desk Stations List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(filteredDesks, key = { it.id }) { desk ->
        AgentStationDeskCard(
          desk = desk,
          onClick = {
            if (desk.targetCompanionMode != null && personalityEngine != null) {
              personalityEngine.setCompanionMode(desk.targetCompanionMode)
            }
            desk.targetScreen?.let { onNavigateToScreen(it) }
          }
        )
      }
    }
  }
}

/**
 * 2D Interactive Office Floor Minimap showing station nodes and live status indicators.
 */
@Composable
private fun OfficeFloorMinimap(
  desks: List<AgentTownDesk>,
  onSelectDesk: (AgentTownDesk) -> Unit
) {
  val theme = activeTheme

  FuturisticCard(
    headerText = "2D Workspace Layout",
    badgeText = "Command Floor",
    badgeColor = theme.secondary
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "OFFICE SECTOR MAP",
          style = MaterialTheme.typography.labelSmall.copy(
            color = TextTertiary,
            fontSize = 9.sp,
            letterSpacing = 1.sp
          )
        )
        Text(
          text = "Tap station to navigate",
          style = MaterialTheme.typography.labelSmall.copy(
            color = TextTertiary,
            fontSize = 9.sp
          )
        )
      }

      // Compact 2D Floor Plan Grid
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF070B14))
          .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          // Sector 1: Executive Plaza (3 Desks)
          MinimapSectorRow(
            sectorTitle = "EXEC",
            sectorDesks = desks.filter { it.sector == OfficeSector.EXECUTIVE },
            onSelectDesk = onSelectDesk
          )

          // Sector 2: Operations Bay (2 Desks)
          MinimapSectorRow(
            sectorTitle = "OPS",
            sectorDesks = desks.filter { it.sector == OfficeSector.OPERATIONS },
            onSelectDesk = onSelectDesk
          )

          // Sector 3: Intelligence Labs (4 Desks)
          MinimapSectorRow(
            sectorTitle = "LABS",
            sectorDesks = desks.filter { it.sector == OfficeSector.LABS },
            onSelectDesk = onSelectDesk
          )
        }
      }
    }
  }
}

@Composable
private fun MinimapSectorRow(
  sectorTitle: String,
  sectorDesks: List<AgentTownDesk>,
  onSelectDesk: (AgentTownDesk) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = sectorTitle,
      style = MaterialTheme.typography.labelSmall.copy(
        color = TextTertiary,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold
      ),
      modifier = Modifier.width(28.dp)
    )

    Row(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      sectorDesks.forEach { desk ->
        MinimapDeskNode(
          desk = desk,
          modifier = Modifier.weight(1f),
          onClick = { onSelectDesk(desk) }
        )
      }
    }
  }
}

@Composable
private fun MinimapDeskNode(
  desk: AgentTownDesk,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "minimap_pulse")
  val isAnimated = desk.liveStatus == TownAgentStatus.WORKING ||
      desk.liveStatus == TownAgentStatus.THINKING ||
      desk.liveStatus == TownAgentStatus.SPEAKING

  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = if (isAnimated) 0.35f else 1f,
    targetValue = if (isAnimated) 1f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "desk_node_alpha"
  )

  Box(
    modifier = modifier
      .height(28.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(Color(0xFF0F172A))
      .border(
        1.dp,
        desk.liveStatus.indicatorColor.copy(alpha = pulseAlpha * 0.7f),
        RoundedCornerShape(4.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(5.dp)
          .clip(CircleShape)
          .background(desk.liveStatus.indicatorColor.copy(alpha = pulseAlpha))
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = desk.name.split(" ").first(),
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextPrimary,
          fontSize = 8.sp,
          fontWeight = FontWeight.SemiBold
        ),
        maxLines = 1
      )
    }
  }
}

/**
 * 2D Desk Card for each agent station in the office.
 */
@Composable
private fun AgentStationDeskCard(
  desk: AgentTownDesk,
  onClick: () -> Unit
) {
  val theme = activeTheme
  val infiniteTransition = rememberInfiniteTransition(label = "station_glow")
  val isBusy = desk.liveStatus == TownAgentStatus.WORKING ||
      desk.liveStatus == TownAgentStatus.THINKING ||
      desk.liveStatus == TownAgentStatus.SPEAKING

  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = if (isBusy) 0.4f else 0.8f,
    targetValue = if (isBusy) 1.0f else 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF0A0F1D))
      .border(
        1.dp,
        if (isBusy) desk.liveStatus.indicatorColor.copy(alpha = glowAlpha)
        else Color(0xFF1E293B),
        RoundedCornerShape(12.dp)
      )
      .clickable(onClick = onClick)
      .padding(CyberTokens.spaceMedium)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Station Header: Code & Live Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(desk.accentColor.copy(alpha = 0.12f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = desk.deskCode,
              style = MaterialTheme.typography.labelSmall.copy(
                color = desk.accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
              )
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = desk.sector.title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextTertiary,
              fontSize = 9.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Live Status Badge with beacon dot
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(desk.liveStatus.indicatorColor.copy(alpha = 0.15f))
            .border(
              1.dp,
              desk.liveStatus.indicatorColor.copy(alpha = glowAlpha),
              RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(desk.liveStatus.indicatorColor.copy(alpha = glowAlpha))
            )
            Text(
              text = desk.liveStatus.label,
              style = MaterialTheme.typography.labelSmall.copy(
                color = desk.liveStatus.indicatorColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                letterSpacing = 0.8.sp
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Agent Identity Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(desk.accentColor.copy(alpha = 0.15f))
            .border(1.dp, desk.accentColor.copy(alpha = 0.4f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = desk.icon,
            contentDescription = desk.name,
            tint = desk.accentColor,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = desk.name,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
          Text(
            text = desk.role,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontSize = 11.sp
            )
          )
        }
      }

      // Live Current Task (Only shown if real task exists)
      if (!desk.currentTask.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Sensors,
              contentDescription = null,
              tint = desk.liveStatus.indicatorColor,
              modifier = Modifier.size(14.dp)
            )
            Column {
              Text(
                text = "CURRENT TASK",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextTertiary,
                  fontSize = 8.sp,
                  letterSpacing = 0.8.sp
                )
              )
              Text(
                text = desk.currentTask,
                style = MaterialTheme.typography.bodySmall.copy(
                  color = TextPrimary,
                  fontSize = 11.sp
                ),
                maxLines = 2
              )
            }
          }
        }
      }

      // Active Tool / Skill (Only shown if real skill data exists)
      if (!desk.activeSkill.isNullOrBlank()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "ACTIVE TOOL",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextTertiary,
              fontSize = 8.sp,
              letterSpacing = 0.8.sp
            )
          )
          Text(
            text = desk.activeSkill,
            style = MaterialTheme.typography.labelSmall.copy(
              color = desk.accentColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
          )
        }
      }

      // Action Footer: Tap to open station
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Open Station Details →",
          style = MaterialTheme.typography.labelSmall.copy(
            color = theme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
          )
        )
      }
    }
  }
}
