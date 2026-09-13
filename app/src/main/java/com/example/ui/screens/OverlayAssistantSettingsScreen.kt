package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.overlay.FloatingVisualMode
import com.example.core.overlay.OverlayPosition
import com.example.core.overlay.OverlaySize
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Dedicated Overlay Assistant Architecture Screen.
 * Prepares the floating window entry point for future Android WindowManager overlay.
 * Section 23-26: Truthful state, visual modes, permission routing.
 */
@Composable
fun OverlayAssistantSettingsScreen(
  onBack: () -> Unit,
  agentController: AgentController? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val floatingController = agentController?.companionEngine?.floatingAssistantController

  val config by floatingController?.config?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.overlay.FloatingAssistantConfig())
  }
  val assistantState by floatingController?.state?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.overlay.FloatingAssistantState.HIDDEN)
  }

  val hasOverlayPermission = floatingController?.hasOverlayPermission() ?: false

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
          modifier = Modifier.testTag("overlay_assistant_back_button")
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
            text = "FLOATING HUD // OVERLAY",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            )
          )
          Text(
            text = "Floating Assistant HUD",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
        }
      }
    }

    // Permission & Safety Notice
    item {
      FuturisticCard(
        headerText = "Android Overlay Policy",
        badgeText = if (hasOverlayPermission) "PERMISSION GRANTED" else "ACCESS REQUIRED"
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (hasOverlayPermission) Icons.Default.DisplaySettings else Icons.Default.Warning,
            contentDescription = null,
            tint = if (hasOverlayPermission) CyanNeon else StatusWarning,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (hasOverlayPermission) {
              "Overlay permission is active. Floating HUD can hover across third-party applications."
            } else {
              "Overlay access required. Android requires explicit permission in Special App Access to draw above apps."
            },
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
          )
        }

        if (!hasOverlayPermission) {
          Spacer(modifier = Modifier.height(12.dp))
          FuturisticButton(
            text = "Open Android Overlay Settings",
            onClick = {
              floatingController?.getOverlaySettingsIntent()?.let {
                try { context.startActivity(it) } catch (_: Exception) {}
              }
            },
            modifier = Modifier.fillMaxWidth(),
            isPrimary = false
          )
        }
      }
    }

    // Master Toggle & Runtime State
    item {
      GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Enable Floating Assistant",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              )
              Spacer(modifier = Modifier.width(8.dp))
              StatusBadge(
                text = assistantState.label,
                indicatorColor = if (config.isOverlayEnabled) StatusReady else TextTertiary,
                isPulsing = false
              )
            }
            Text(
              text = "Render floating quick summon presence",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
              )
            )
          }

          Switch(
            checked = config.isOverlayEnabled,
            onCheckedChange = {
              floatingController?.updateConfig(config.copy(isOverlayEnabled = it))
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color(0xFF030712),
              checkedTrackColor = CyanNeon,
              uncheckedThumbColor = TextTertiary,
              uncheckedTrackColor = Color(0xFF1E293B)
            )
          )
        }
      }
    }

    // Visual Modes
    item {
      Text(
        text = "OVERLAY VISUAL STYLE (SECTION 23)",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        ),
        modifier = Modifier.padding(top = 8.dp)
      )
    }

    items(FloatingVisualMode.values()) { mode ->
      val isSelected = config.visualMode == mode
      GlassPanel(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            floatingController?.updateConfig(config.copy(visualMode = mode))
          }
          .testTag("overlay_style_${mode.name.lowercase()}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CyberTokens.shapeSmall,
            color = if (isSelected) CyanNeon else Color(0x3300F0FF),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color(0xFF030712),
                  modifier = Modifier.size(20.dp)
                )
              } else {
                Text(
                  text = mode.displayName.take(1),
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon
                  )
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = mode.displayName,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) CyanNeon else TextPrimary
              )
            )
            Text(
              text = mode.description,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 11.sp
              )
            )
          }
        }
      }
    }

    // Position & Sizing
    item {
      Text(
        text = "SCREEN DOCKING & SCALE",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        ),
        modifier = Modifier.padding(top = 8.dp)
      )
    }

    item {
      FuturisticCard(headerText = "Layout Configuration", badgeText = "Customizable") {
        Text("Docking Anchor", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OverlayPosition.values().forEach { pos ->
            val isSelected = config.position == pos
            FuturisticButton(
              text = pos.label,
              onClick = { floatingController?.updateConfig(config.copy(position = pos)) },
              modifier = Modifier.weight(1f),
              isPrimary = isSelected
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Orb Scale", style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon))
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OverlaySize.values().forEach { size ->
            val isSelected = config.size == size
            FuturisticButton(
              text = size.label,
              onClick = { floatingController?.updateConfig(config.copy(size = size)) },
              modifier = Modifier.weight(1f),
              isPrimary = isSelected
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(CyberTokens.spaceExtraLarge))
    }
  }
}
