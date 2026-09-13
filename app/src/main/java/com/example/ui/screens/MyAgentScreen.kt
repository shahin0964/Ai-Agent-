package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.action.ActionStatus
import com.example.core.action.AgentAction
import com.example.core.agent.AgentController
import com.example.core.capability.AgentCapabilityType
import com.example.core.model.AgentIdentity
import com.example.core.model.CoreState
import com.example.core.model.SystemActivity
import com.example.core.telemetry.AndroidSystemTelemetry
import com.example.core.telemetry.DeviceTelemetrySnapshot
import com.example.core.tool.ToolRegistry
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.activeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * My Agent Screen - Living Core & Operational Command Center.
 * Visually connects Identity, Memory, Capabilities, Tool registry,
 * Active Providers, System Telemetry, and Running Automations.
 */
@Composable
fun MyAgentScreen(
  identity: AgentIdentity,
  coreState: CoreState,
  activity: SystemActivity,
  agentController: AgentController? = null,
  onNavigateToScreen: (Screen) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val theme = activeTheme
  val scrollState = rememberScrollState()

  val recentActions: List<AgentAction> = if (agentController != null) {
    agentController.recentActions.collectAsState().value
  } else {
    emptyList()
  }

  val pendingConfirmation: AgentAction? = if (agentController != null) {
    agentController.pendingConfirmationAction.collectAsState().value
  } else {
    null
  }

  // AI Container & Engines state
  val personalityEngine = agentController?.aiEngine?.personalityEngine
  val companionMode by personalityEngine?.companionMode?.collectAsState()
    ?: remember { mutableStateOf(null) }
  val routingPref by agentController?.aiEngine?.routingConfig?.preferences?.collectAsState()
    ?: remember { mutableStateOf(null) }
  val memoryItems by (agentController?.aiEngine?.memoryManager?.allMemoriesFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
  val automations by agentController?.automationEngine?.automations?.collectAsState()
    ?: remember { mutableStateOf(emptyList()) }

  // Real Hardware Telemetry Snapshot
  var telemetry by remember { mutableStateOf(AndroidSystemTelemetry.getSnapshot(context)) }
  LaunchedEffect(Unit) {
    telemetry = AndroidSystemTelemetry.getSnapshot(context)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = CyberTokens.spaceSmall),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "LIVING AI CORE // NEXUS",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.2.sp,
          fontWeight = FontWeight.Bold,
          color = theme.primary
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false)
      )
      Spacer(modifier = Modifier.width(6.dp))
      StatusBadge(
        text = identity.statusTruth,
        indicatorColor = StatusReady
      )
    }

    // Quick Navigation (Agent Hub)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = CyberTokens.spaceMedium)
    ) {
      FuturisticButton(
        text = "Open Agent Hub",
        onClick = { onNavigateToScreen(Screen.AgentHub) },
        isPrimary = true,
        modifier = Modifier.fillMaxWidth().testTag("quick_nav_agent_hub")
      )
    }

    // Pending Confirmation Banner
    if (pendingConfirmation != null) {
      FuturisticCard(
        headerText = "Action Approval Required",
        badgeText = "Policy Guard",
        badgeColor = StatusAlert
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = pendingConfirmation.confirmationPrompt
              ?: pendingConfirmation.description,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
          )
          Text(
            text = "Sensitive Android action requires explicit operator approval.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FuturisticButton(
              text = "Authorize",
              onClick = { agentController?.confirmPendingAction() },
              isPrimary = true,
              modifier = Modifier.weight(1f)
            )
            FuturisticButton(
              text = "Cancel",
              onClick = { agentController?.cancelPendingAction() },
              isPrimary = false,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))
    }

    // 1. Living Core: "Who is my agent?" (Identity & Active Persona)
    FuturisticCard(
      headerText = "Living Agent Identity",
      badgeText = companionMode?.displayName ?: identity.architectureVersion,
      badgeColor = theme.primary
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(theme.primary.copy(alpha = 0.15f))
              .border(1.dp, theme.glassBorder, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              tint = theme.primary,
              modifier = Modifier.size(32.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = identity.name,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            )
            Text(
              text = identity.designation,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LivingCoreAttribute(
            label = "Active Persona",
            value = companionMode?.displayName ?: "Personal Assistant",
            modifier = Modifier.weight(1f)
          )
          LivingCoreAttribute(
            label = "Primary LLM",
            value = routingPref?.primaryProviderId?.replace("_", " ")?.uppercase() ?: "Gemini",
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceMedium))

    // 2. Living Core: "What does it remember & What tools can it use?"
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(CyberTokens.spaceMedium)
    ) {
      FuturisticCard(
        headerText = "Long-Term Memory",
        badgeText = "${memoryItems.size} Facts",
        badgeColor = theme.secondary,
        modifier = Modifier.weight(1f)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "${memoryItems.size} facts indexed in secure local Room database.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          FuturisticButton(
            text = "View Memory",
            onClick = { onNavigateToScreen(Screen.Memory) },
            isPrimary = false,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      FuturisticCard(
        headerText = "Tool Registry",
        badgeText = "${ToolRegistry.allTools.size} Tools",
        badgeColor = theme.accent,
        modifier = Modifier.weight(1f)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "${AgentCapabilityType.values().size} Android capabilities mapped.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          FuturisticButton(
            text = "Permissions",
            onClick = { onNavigateToScreen(Screen.SettingsPermissions) },
            isPrimary = false,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

    // 3. Android System Telemetry Hub (Real Hardware APIs)
    FuturisticCard(
      headerText = "Android System Telemetry",
      badgeText = telemetry.deviceModel,
      badgeColor = if (telemetry.isNetworkConnected) StatusReady else StatusWarning
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Battery & Power
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
              contentDescription = null,
              tint = if (telemetry.batteryLevel < 20) StatusAlert else theme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Battery: ${telemetry.batteryLevel}%",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
              )
            )
          }
          Text(
            text = telemetry.powerSource,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
          )
        }

        // RAM & Storage
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TelemetryMetricBox(
            label = "RAM In Use",
            value = "${telemetry.ramUsagePercentage}%",
            subtext = "${AndroidSystemTelemetry.formatBytes(telemetry.availableRamBytes)} free",
            color = theme.primary,
            modifier = Modifier.weight(1f)
          )
          TelemetryMetricBox(
            label = "Internal Storage",
            value = "${telemetry.storageUsagePercentage}%",
            subtext = "${AndroidSystemTelemetry.formatBytes(telemetry.availableStorageBytes)} free",
            color = theme.secondary,
            modifier = Modifier.weight(1f)
          )
        }

        // Network & Uptime
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TelemetryMetricBox(
            label = "Network",
            value = telemetry.networkType,
            subtext = if (telemetry.isInternetValidated) "Validated Online" else "Local Connection",
            color = if (telemetry.isNetworkConnected) StatusReady else StatusOffline,
            modifier = Modifier.weight(1f)
          )
          TelemetryMetricBox(
            label = "Device Uptime",
            value = telemetry.deviceUptimeFormatted,
            subtext = "Android ${telemetry.androidVersion} (API ${telemetry.sdkInt})",
            color = theme.accent,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

    // 4. Futuristic Neural / Energy Mesh
    FuturisticCard(headerText = "Cognitive Mesh & Bus Architecture", badgeText = "Real-time Node Status") {
      NeuralEnergyVisualization(
        coreState = coreState,
        primaryColor = theme.primary,
        accentColor = theme.accent,
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
      )
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

    // 5. System Activity Stream
    FuturisticCard(headerText = "Activity Stream", badgeText = "Operational") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = theme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = activity.label,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
          )
        }

        Text(
          text = activity.detail,
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
      }
    }

    Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

    // 6. Recent Android Device Actions
    if (recentActions.isNotEmpty()) {
      FuturisticCard(headerText = "Recent Device Actions", badgeText = "${recentActions.size} Operations") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          recentActions.take(4).forEach { action ->
            ActionHistoryRow(action)
          }
        }
      }
      Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))
    }

    // 7. Subsystems Status Row
    Text(
      text = "CORE SUBSYSTEMS STATUS",
      style = MaterialTheme.typography.labelSmall.copy(
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        color = theme.primary
      ),
      modifier = Modifier.padding(bottom = 8.dp)
    )

    SubsystemStatusRow(
      name = "Dialogue Routing",
      status = "Active (Multi-Provider)",
      icon = Icons.Default.Sensors,
      color = theme.accent
    )

    Spacer(modifier = Modifier.height(6.dp))

    SubsystemStatusRow(
      name = "Speech Recognition (STT)",
      status = "Android Speech Engine",
      icon = Icons.Default.RecordVoiceOver,
      color = theme.primary
    )

    Spacer(modifier = Modifier.height(6.dp))

    SubsystemStatusRow(
      name = "Acoustic Synthesizer (TTS)",
      status = "Android Text-to-Speech",
      icon = Icons.Default.Bolt,
      color = theme.secondary
    )

    Spacer(modifier = Modifier.height(6.dp))

    SubsystemStatusRow(
      name = "Automations & Routines",
      status = "${automations.count { it.enabled }} Enabled",
      icon = Icons.Default.Timer,
      color = StatusReady
    )

    Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
  }
}

@Composable
private fun LivingCoreAttribute(label: String, value: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF0F172A))
      .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
      .padding(8.dp)
  ) {
    Column {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextTertiary,
          fontSize = 9.sp,
          letterSpacing = 0.8.sp
        )
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        ),
        maxLines = 1
      )
    }
  }
}

@Composable
private fun TelemetryMetricBox(
  label: String,
  value: String,
  subtext: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF0A0F1D))
      .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
      .padding(8.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextTertiary,
          fontSize = 8.5.sp,
          letterSpacing = 0.6.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Bold,
          color = color,
          fontSize = 13.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtext,
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextSecondary,
          fontSize = 8.5.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ActionHistoryRow(action: AgentAction) {
  val theme = activeTheme
  val statusColor = when (action.status) {
    ActionStatus.SUCCESS -> StatusReady
    ActionStatus.WAITING_FOR_CONFIRMATION -> StatusWarning
    ActionStatus.WAITING_FOR_PERMISSION -> StatusWarning
    ActionStatus.EXECUTING -> theme.primary
    ActionStatus.FAILED -> StatusAlert
    ActionStatus.CANCELLED -> StatusOffline
    ActionStatus.PLANNED -> TextTertiary
  }

  val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
  val formattedTime = remember(action.timestamp) { timeFormat.format(Date(action.timestamp)) }

  GlassPanel(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Icon(
            imageVector = when (action.status) {
              ActionStatus.SUCCESS -> Icons.Default.CheckCircle
              ActionStatus.FAILED -> Icons.Default.Error
              else -> Icons.Default.PhoneAndroid
            },
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = action.description,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(6.dp))
        StatusBadge(
          text = action.status.label.uppercase(),
          indicatorColor = statusColor,
          isPulsing = action.status == ActionStatus.EXECUTING
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      val detail = action.resultMessage ?: action.errorMessage ?: "Action registered at $formattedTime"
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall.copy(
          color = if (action.errorMessage != null) StatusAlert else TextSecondary,
          fontSize = 11.sp
        )
      )

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "$formattedTime // ID: ${action.id.take(8)}",
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextTertiary,
          fontSize = 9.sp
        )
      )
    }
  }
}

@Composable
private fun SubsystemStatusRow(
  name: String,
  status: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color
) {
  GlassPanel(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = name,
          style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      StatusBadge(
        text = status,
        indicatorColor = color,
        isPulsing = false
      )
    }
  }
}

@Composable
private fun NeuralEnergyVisualization(
  coreState: CoreState,
  primaryColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "NeuralMesh")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "NeuralPulse"
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val node1 = Offset(w * 0.15f, h * 0.5f)
    val node2 = Offset(w * 0.38f, h * 0.25f)
    val node3 = Offset(w * 0.38f, h * 0.75f)
    val node4 = Offset(w * 0.65f, h * 0.35f)
    val node5 = Offset(w * 0.65f, h * 0.65f)
    val node6 = Offset(w * 0.88f, h * 0.5f)

    val nodes = listOf(node1, node2, node3, node4, node5, node6)
    val edges = listOf(
      node1 to node2, node1 to node3,
      node2 to node4, node3 to node5,
      node2 to node3, node4 to node5,
      node4 to node6, node5 to node6
    )

    edges.forEach { (start, end) ->
      drawLine(
        color = primaryColor.copy(alpha = 0.25f * pulse),
        start = start,
        end = end,
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
      )
    }

    nodes.forEachIndexed { index, pt ->
      val nodeColor = if (index % 2 == 0) primaryColor else accentColor
      drawCircle(
        color = nodeColor.copy(alpha = 0.2f),
        radius = 10.dp.toPx() * pulse,
        center = pt
      )
      drawCircle(
        color = nodeColor,
        radius = 3.5.dp.toPx(),
        center = pt
      )
      drawCircle(
        color = Color.White,
        radius = 1.5.dp.toPx(),
        center = pt
      )
    }
  }
}
