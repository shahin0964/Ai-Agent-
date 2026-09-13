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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.agent.AgentController
import com.example.core.companion.CompanionMode
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CompanionSettingsScreen(
  onBack: () -> Unit,
  agentController: AgentController,
  modifier: Modifier = Modifier
) {
  val companion = agentController.companionEngine
  val personality = agentController.aiEngine?.personalityEngine

  val currentMode by personality?.companionMode?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(CompanionMode.PERSONAL_ASSISTANT)
  }

  val params by personality?.parameters?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.ai.personality.PersonalityParameters())
  }

  val proactivePolicy by companion?.proactivePolicyManager?.policy?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.companion.ProactivePolicy())
  }

  val sleepSchedule by companion?.sleepManager?.sleepSchedule?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.companion.SleepSchedule())
  }

  val sleepState by companion?.sleepManager?.sleepState?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.companion.SleepState.AWAKE)
  }

  val bgPresenceState by companion?.backgroundPresenceController?.presenceState?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.background.BackgroundPresenceState.NOT_RUNNING)
  }

  val presenceState by companion?.presenceManager?.presenceState?.collectAsState() ?: androidx.compose.runtime.remember {
    androidx.compose.runtime.mutableStateOf(com.example.core.companion.PresenceState.ACTIVE)
  }

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
        IconButton(onClick = onBack, modifier = Modifier.testTag("companion_settings_back")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CyanNeon)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "COMPANION MATRIX // COGNITION",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 1.5.sp,
              fontWeight = FontWeight.Bold,
              color = CyanNeon
            )
          )
          Text(
            text = "Companion & Presence",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          )
        }
      }
    }

    // 1. Live Operational Presence Card
    item {
      FuturisticCard(
        headerText = "Real-Time Runtime Presence",
        badgeText = presenceState.label
      ) {
        Text(
          text = presenceState.description,
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Background Service",
              style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
            )
            Text(
              text = bgPresenceState.label,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
          }

          if (bgPresenceState == com.example.core.background.BackgroundPresenceState.RUNNING) {
            FuturisticButton(
              text = "Stop Background",
              onClick = { companion?.backgroundPresenceController?.stopBackgroundPresence() },
              isPrimary = false
            )
          } else {
            FuturisticButton(
              text = "Start Background",
              onClick = { companion?.backgroundPresenceController?.startBackgroundPresence() },
              isPrimary = true
            )
          }
        }
      }
    }

    // 2. Companion Modes
    item {
      Text(
        text = "RELATIONSHIP & OPERATIONAL MODE",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        ),
        modifier = Modifier.padding(top = 6.dp)
      )
    }

    items(CompanionMode.values()) { mode ->
      val isSelected = currentMode == mode
      GlassPanel(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { personality?.setCompanionMode(mode) }
          .testTag("mode_${mode.name.lowercase()}")
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
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF030712), modifier = Modifier.size(20.dp))
              } else {
                Text(mode.displayName.take(1), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = CyanNeon))
              }
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = mode.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) CyanNeon else TextPrimary
                )
              )
              StatusBadge(text = mode.tag, indicatorColor = if (isSelected) CyanNeon else ElectricBlue, isPulsing = false)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = mode.description,
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
          }
        }
      }
    }

    // 3. Proactive Companion (Master & Sub-toggles)
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "PROACTIVE COMPANION (OFF BY DEFAULT)",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      FuturisticCard(
        headerText = "Proactive Autonomy Policy",
        badgeText = if (proactivePolicy.isProactiveCompanionEnabled) "ACTIVE" else "DISABLED"
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Proactive Companion Master",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = "When OFF, agent strictly waits for your interaction. Sub-toggles require master to be enabled.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
          }
          Switch(
            checked = proactivePolicy.isProactiveCompanionEnabled,
            onCheckedChange = { companion?.proactivePolicyManager?.setMasterEnabled(it) },
            colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sub-toggles
        ProactiveToggleItem(
          title = "Proactive Notifications",
          subtitle = "Allow agent to send verified updates to system tray",
          enabled = proactivePolicy.isProactiveCompanionEnabled,
          checked = proactivePolicy.proactiveNotifications,
          onChecked = {
            companion?.proactivePolicyManager?.updatePolicy(
              proactivePolicy.copy(proactiveNotifications = it)
            )
          }
        )

        ProactiveToggleItem(
          title = "Personalized Greetings",
          subtitle = "Display welcome greeting upon session initialization",
          enabled = proactivePolicy.isProactiveCompanionEnabled,
          checked = proactivePolicy.proactiveGreetings,
          onChecked = {
            companion?.proactivePolicyManager?.updatePolicy(
              proactivePolicy.copy(proactiveGreetings = it)
            )
          }
        )

        ProactiveToggleItem(
          title = "Reminder Messages",
          subtitle = "Deliver operator-scheduled task reminders",
          enabled = proactivePolicy.isProactiveCompanionEnabled,
          checked = proactivePolicy.reminderMessages,
          onChecked = {
            companion?.proactivePolicyManager?.updatePolicy(
              proactivePolicy.copy(reminderMessages = it)
            )
          }
        )

        ProactiveToggleItem(
          title = "Research Updates",
          subtitle = "Alert when background web research synthesis finishes",
          enabled = proactivePolicy.isProactiveCompanionEnabled,
          checked = proactivePolicy.researchUpdates,
          onChecked = {
            companion?.proactivePolicyManager?.updatePolicy(
              proactivePolicy.copy(researchUpdates = it)
            )
          }
        )

        ProactiveToggleItem(
          title = "Silent Companion Notifications",
          subtitle = "Deliver notifications without audible chime where OS allows",
          enabled = proactivePolicy.isProactiveCompanionEnabled,
          checked = proactivePolicy.silentNotifications,
          onChecked = {
            companion?.proactivePolicyManager?.updatePolicy(
              proactivePolicy.copy(silentNotifications = it)
            )
          }
        )
      }
    }

    // 4. Personality Tuning (1..10 Parameters)
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "BEHAVIORAL TUNING (1..10 PARAMETERS)",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      FuturisticCard(
        headerText = "Communication Directives",
        badgeText = "Heuristic Parameters"
      ) {
        ParameterSlider(
          label = "Formality",
          value = params.formality,
          description = if (params.formality > 7) "Structured & polite" else if (params.formality < 4) "Casual & easygoing" else "Standard",
          onValueChange = { personality?.updateParameters(params.copy(formality = it)) }
        )

        ParameterSlider(
          label = "Friendliness",
          value = params.friendliness,
          description = if (params.friendliness > 7) "Warm & expressive" else "Professional & direct",
          onValueChange = { personality?.updateParameters(params.copy(friendliness = it)) }
        )

        ParameterSlider(
          label = "Humor Level",
          value = params.humor,
          description = if (params.humor > 6) "Witty & playful" else "Serious & factual",
          onValueChange = { personality?.updateParameters(params.copy(humor = it)) }
        )

        ParameterSlider(
          label = "Conciseness",
          value = params.conciseness,
          description = if (params.conciseness > 7) "Dense bullet points" else "Full narrative context",
          onValueChange = { personality?.updateParameters(params.copy(conciseness = it)) }
        )

        ParameterSlider(
          label = "Empathy Style",
          value = params.empathyStyle,
          description = "Supportive tone (Never claims biological human feelings)",
          onValueChange = { personality?.updateParameters(params.copy(empathyStyle = it)) }
        )

        ParameterSlider(
          label = "Energy",
          value = params.energy,
          description = if (params.energy > 7) "Dynamic & enthusiastic" else "Calm & steady",
          onValueChange = { personality?.updateParameters(params.copy(energy = it)) }
        )
      }
    }

    // 5. Sleep Schedule & Mode
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "SLEEP & STANDBY REST MATRIX",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
    }

    item {
      FuturisticCard(
        headerText = "Scheduled Sleep Engine",
        badgeText = sleepState.label
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Enable Scheduled Sleep Window",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
            Text(
              text = "During sleep: proactive queries and mic capture are suppressed.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
          }
          Switch(
            checked = sleepSchedule.isScheduleEnabled,
            onCheckedChange = {
              companion?.sleepManager?.updateSchedule(sleepSchedule.copy(isScheduleEnabled = it))
            },
            colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
          )
        }

        if (sleepSchedule.isScheduleEnabled) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Schedule: %02d:%02d to %02d:%02d".format(
              sleepSchedule.sleepHour, sleepSchedule.sleepMinute,
              sleepSchedule.wakeHour, sleepSchedule.wakeMinute
            ),
            style = MaterialTheme.typography.bodyMedium.copy(color = CyanNeon, fontWeight = FontWeight.Bold)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Allow Notifications During Sleep",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Switch(
              checked = sleepSchedule.allowSleepNotifications,
              onCheckedChange = {
                companion?.sleepManager?.updateSchedule(sleepSchedule.copy(allowSleepNotifications = it))
              },
              colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
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

@Composable
private fun ProactiveToggleItem(
  title: String,
  subtitle: String,
  enabled: Boolean,
  checked: Boolean,
  onChecked: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.SemiBold,
          color = if (enabled) TextPrimary else TextTertiary
        )
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = if (enabled) TextSecondary else TextTertiary,
          fontSize = 10.sp
        )
      )
    }
    Switch(
      checked = checked && enabled,
      onCheckedChange = onChecked,
      enabled = enabled,
      colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon)
    )
  }
}

@Composable
private fun ParameterSlider(
  label: String,
  value: Int,
  description: String,
  onValueChange: (Int) -> Unit
) {
  Column(modifier = Modifier.padding(vertical = 6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
      )
      Text(
        text = "$value / 10",
        style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon, fontWeight = FontWeight.Bold)
      )
    }
    Text(
      text = description,
      style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
    )
    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.toInt()) },
      valueRange = 1f..10f,
      steps = 8,
      colors = SliderDefaults.colors(
        thumbColor = CyanNeon,
        activeTrackColor = CyanNeon,
        inactiveTrackColor = Color(0xFF1E293B)
      )
    )
  }
}
