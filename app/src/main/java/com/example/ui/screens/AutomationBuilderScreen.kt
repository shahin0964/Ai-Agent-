package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.agent.AgentController
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationCategory
import com.example.core.automation.model.AutomationCondition
import com.example.core.automation.model.AutomationConfirmationPolicy
import com.example.core.automation.model.AutomationNotificationPolicy
import com.example.core.automation.model.AutomationState
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.model.ConfirmationPolicyType
import com.example.core.automation.model.ExecutionPolicy
import com.example.core.automation.model.NetworkRequirementType
import com.example.core.automation.model.RecurrenceType
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@Composable
fun AutomationBuilderScreen(
  agentController: AgentController,
  automationId: String?,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val automationEngine = agentController.automationEngine
  val scope = rememberCoroutineScope()

  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(AutomationCategory.PERSONAL) }

  // Trigger state
  var triggerTypeSelection by remember { mutableStateOf("TIME") } // TIME, BATTERY, CONNECTIVITY
  var timeHour by remember { mutableIntStateOf(8) }
  var timeMinute by remember { mutableIntStateOf(0) }
  var recurrenceType by remember { mutableStateOf(RecurrenceType.DAILY) }
  var selectedDays by remember { mutableStateOf(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) }
  var batteryThreshold by remember { mutableIntStateOf(20) }
  var triggerOnCharging by remember { mutableStateOf(false) }
  var wifiOnlyTrigger by remember { mutableStateOf(false) }

  // Conditions state
  var requireMinBattery by remember { mutableStateOf(false) }
  var minBatteryCondition by remember { mutableIntStateOf(20) }
  var requireChargingCondition by remember { mutableStateOf(false) }
  var networkRequirement by remember { mutableStateOf(NetworkRequirementType.NO_NETWORK_REQUIRED) }

  // Actions list
  var actionsList by remember { mutableStateOf<List<AgentAction>>(emptyList()) }

  // Policies
  var confirmationPolicyType by remember { mutableStateOf(ConfirmationPolicyType.NO_CONFIRMATION_FOR_SAFE_ACTIONS) }
  var notifyOnSuccess by remember { mutableStateOf(true) }
  var notifyOnFailure by remember { mutableStateOf(true) }

  // Wizard Tab
  var currentStep by remember { mutableIntStateOf(1) } // 1: Info, 2: Trigger, 3: Conditions, 4: Actions, 5: Policies, 6: Preview

  var showAddActionDialog by remember { mutableStateOf(false) }
  var isDryRunning by remember { mutableStateOf(false) }
  var dryRunSummary by remember { mutableStateOf<String?>(null) }

  // Load existing automation if editing
  LaunchedEffect(automationId) {
    if (automationId != null && automationEngine != null) {
      val existing = automationEngine.store.getAutomation(automationId)
      if (existing != null) {
        name = existing.name
        description = existing.description
        category = existing.category
        actionsList = existing.actions
        confirmationPolicyType = existing.confirmationPolicy.policyType
        notifyOnSuccess = existing.notificationPolicy.notifyOnSuccess
        notifyOnFailure = existing.notificationPolicy.notifyOnFailure

        when (val t = existing.trigger) {
          is AutomationTrigger.TimeSchedule -> {
            triggerTypeSelection = "TIME"
            timeHour = t.timeHour
            timeMinute = t.timeMinute
            recurrenceType = t.recurrence
            selectedDays = t.daysOfWeek
          }
          is AutomationTrigger.BatteryEvent -> {
            triggerTypeSelection = "BATTERY"
            batteryThreshold = t.thresholdPercent
            triggerOnCharging = t.triggerOnCharging
          }
          is AutomationTrigger.ConnectivityEvent -> {
            triggerTypeSelection = "CONNECTIVITY"
            wifiOnlyTrigger = t.wifiOnly
          }
          else -> {}
        }

        existing.conditions.forEach { cond ->
          when (cond) {
            is AutomationCondition.BatteryCondition -> {
              requireMinBattery = true
              minBatteryCondition = cond.minLevel
              requireChargingCondition = cond.requireCharging
            }
            is AutomationCondition.NetworkCondition -> {
              networkRequirement = cond.requirement
            }
            else -> {}
          }
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CyberBackgroundDark)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
          }
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (automationId == null) "NEW AUTOMATION" else "EDIT AUTOMATION",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Step Wizard Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        WizardStepChip(1, "1. Identity", currentStep == 1) { currentStep = 1 }
        WizardStepChip(2, "2. Trigger", currentStep == 2) { currentStep = 2 }
        WizardStepChip(3, "3. Conditions", currentStep == 3) { currentStep = 3 }
        WizardStepChip(4, "4. Routine", currentStep == 4) { currentStep = 4 }
        WizardStepChip(5, "5. Policies", currentStep == 5) { currentStep = 5 }
        WizardStepChip(6, "6. Preview", currentStep == 6) { currentStep = 6 }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Step Viewport
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        when (currentStep) {
          1 -> StepIdentity(
            name = name,
            onNameChange = { name = it },
            description = description,
            onDescriptionChange = { description = it },
            category = category,
            onCategoryChange = { category = it }
          )

          2 -> StepTrigger(
            triggerType = triggerTypeSelection,
            onTriggerTypeChange = { triggerTypeSelection = it },
            timeHour = timeHour,
            onHourChange = { timeHour = it },
            timeMinute = timeMinute,
            onMinuteChange = { timeMinute = it },
            recurrence = recurrenceType,
            onRecurrenceChange = { recurrenceType = it },
            selectedDays = selectedDays,
            onToggleDay = { day ->
              selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
            },
            batteryThreshold = batteryThreshold,
            onBatteryThresholdChange = { batteryThreshold = it },
            triggerOnCharging = triggerOnCharging,
            onTriggerOnChargingChange = { triggerOnCharging = it },
            wifiOnly = wifiOnlyTrigger,
            onWifiOnlyChange = { wifiOnlyTrigger = it }
          )

          3 -> StepConditions(
            requireMinBattery = requireMinBattery,
            onRequireMinBatteryChange = { requireMinBattery = it },
            minBattery = minBatteryCondition,
            onMinBatteryChange = { minBatteryCondition = it },
            requireCharging = requireChargingCondition,
            onRequireChargingChange = { requireChargingCondition = it },
            networkRequirement = networkRequirement,
            onNetworkRequirementChange = { networkRequirement = it }
          )

          4 -> StepRoutine(
            actions = actionsList,
            onAddActionClick = { showAddActionDialog = true },
            onMoveUp = { idx ->
              if (idx > 0) {
                val list = actionsList.toMutableList()
                val item = list.removeAt(idx)
                list.add(idx - 1, item)
                actionsList = list
              }
            },
            onMoveDown = { idx ->
              if (idx < actionsList.size - 1) {
                val list = actionsList.toMutableList()
                val item = list.removeAt(idx)
                list.add(idx + 1, item)
                actionsList = list
              }
            },
            onDelete = { idx ->
              val list = actionsList.toMutableList()
              list.removeAt(idx)
              actionsList = list
            }
          )

          5 -> StepPolicies(
            confirmationPolicy = confirmationPolicyType,
            onConfirmationPolicyChange = { confirmationPolicyType = it },
            notifyOnSuccess = notifyOnSuccess,
            onNotifyOnSuccessChange = { notifyOnSuccess = it },
            notifyOnFailure = notifyOnFailure,
            onNotifyOnFailureChange = { notifyOnFailure = it }
          )

          6 -> StepPreview(
            name = name,
            category = category,
            triggerSummary = buildTriggerSummary(triggerTypeSelection, timeHour, timeMinute, recurrenceType),
            actionsCount = actionsList.size,
            actions = actionsList,
            onDryRun = {
              if (automationEngine != null) {
                isDryRunning = true
                scope.launch {
                  val draft = buildAutomationEntity(
                    id = automationId ?: UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    category = category,
                    enabled = false,
                    state = AutomationState.DRAFT,
                    triggerType = triggerTypeSelection,
                    timeHour = timeHour,
                    timeMinute = timeMinute,
                    recurrence = recurrenceType,
                    days = selectedDays,
                    batteryThreshold = batteryThreshold,
                    triggerOnCharging = triggerOnCharging,
                    wifiOnly = wifiOnlyTrigger,
                    requireMinBattery = requireMinBattery,
                    minBattery = minBatteryCondition,
                    requireCharging = requireChargingCondition,
                    networkReq = networkRequirement,
                    actions = actionsList,
                    confirmationPolicy = confirmationPolicyType,
                    notifySuccess = notifyOnSuccess,
                    notifyFailure = notifyOnFailure
                  )
                  val res = automationEngine.routineEngine.executeRoutine(draft, isDryRun = true)
                  isDryRunning = false
                  dryRunSummary = "Dry Run Result: ${res.status.name} (${res.history.completedSteps}/${res.history.totalSteps} steps valid)"
                }
              }
            },
            dryRunSummary = dryRunSummary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Save Controls
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberTokens.SurfaceDark, RoundedCornerShape(8.dp))
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentStep > 1) {
          TextButton(onClick = { currentStep -= 1 }) {
            Text("Previous", color = TextSecondary)
          }
        } else {
          Spacer(modifier = Modifier.width(4.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Save Draft button
          FuturisticButton(
            text = "SAVE DRAFT",
            onClick = {
              if (name.isNotBlank() && automationEngine != null) {
                val auto = buildAutomationEntity(
                  id = automationId ?: UUID.randomUUID().toString(),
                  name = name,
                  description = description,
                  category = category,
                  enabled = false,
                  state = AutomationState.DRAFT,
                  triggerType = triggerTypeSelection,
                  timeHour = timeHour,
                  timeMinute = timeMinute,
                  recurrence = recurrenceType,
                  days = selectedDays,
                  batteryThreshold = batteryThreshold,
                  triggerOnCharging = triggerOnCharging,
                  wifiOnly = wifiOnlyTrigger,
                  requireMinBattery = requireMinBattery,
                  minBattery = minBatteryCondition,
                  requireCharging = requireChargingCondition,
                  networkReq = networkRequirement,
                  actions = actionsList,
                  confirmationPolicy = confirmationPolicyType,
                  notifySuccess = notifyOnSuccess,
                  notifyFailure = notifyOnFailure
                )
                automationEngine.saveAutomation(auto)
                onNavigateBack()
              }
            },
            isPrimary = false,
            modifier = Modifier.testTag("save_draft_button")
          )

          // Save & Enable button
          FuturisticButton(
            text = "ENABLE",
            onClick = {
              if (name.isNotBlank() && automationEngine != null) {
                val auto = buildAutomationEntity(
                  id = automationId ?: UUID.randomUUID().toString(),
                  name = name,
                  description = description,
                  category = category,
                  enabled = true,
                  state = AutomationState.ENABLED,
                  triggerType = triggerTypeSelection,
                  timeHour = timeHour,
                  timeMinute = timeMinute,
                  recurrence = recurrenceType,
                  days = selectedDays,
                  batteryThreshold = batteryThreshold,
                  triggerOnCharging = triggerOnCharging,
                  wifiOnly = wifiOnlyTrigger,
                  requireMinBattery = requireMinBattery,
                  minBattery = minBatteryCondition,
                  requireCharging = requireChargingCondition,
                  networkReq = networkRequirement,
                  actions = actionsList,
                  confirmationPolicy = confirmationPolicyType,
                  notifySuccess = notifyOnSuccess,
                  notifyFailure = notifyOnFailure
                )
                automationEngine.saveAutomation(auto)
                onNavigateBack()
              }
            },
            isPrimary = true,
            modifier = Modifier.testTag("save_enable_button")
          )
        }
      }
    }

    // Add Action Dialog
    if (showAddActionDialog) {
      AddActionDialog(
        onDismiss = { showAddActionDialog = false },
        onAddAction = { action ->
          actionsList = actionsList + action
          showAddActionDialog = false
        }
      )
    }
  }
}

@Composable
private fun WizardStepChip(
  stepIndex: Int,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isActive) NeonCyan.copy(alpha = 0.2f) else CyberTokens.SurfaceDark)
      .border(1.dp, if (isActive) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isActive) NeonCyan else TextSecondary,
      fontFamily = FontFamily.Monospace,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun StepIdentity(
  name: String,
  onNameChange: (String) -> Unit,
  description: String,
  onDescriptionChange: (String) -> Unit,
  category: AutomationCategory,
  onCategoryChange: (AutomationCategory) -> Unit
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "AUTOMATION IDENTITY",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      Text("Automation Name *", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("e.g., Morning Intelligence Briefing", color = TextSecondary) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = NeonCyan,
          unfocusedBorderColor = CyberTokens.BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("automation_name_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Description", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        placeholder = { Text("Optional purpose of this routine", color = TextSecondary) },
        minLines = 2,
        maxLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = NeonCyan,
          unfocusedBorderColor = CyberTokens.BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text("Category", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AutomationCategory.values().forEach { cat ->
          val isSelected = cat == category
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) NeonMagenta.copy(alpha = 0.2f) else CyberTokens.SurfaceDark)
              .border(1.dp, if (isSelected) NeonMagenta else CyberTokens.BorderSubtle, RoundedCornerShape(8.dp))
              .clickable { onCategoryChange(cat) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = cat.displayName,
              style = MaterialTheme.typography.labelSmall,
              color = if (isSelected) NeonMagenta else TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StepTrigger(
  triggerType: String,
  onTriggerTypeChange: (String) -> Unit,
  timeHour: Int,
  onHourChange: (Int) -> Unit,
  timeMinute: Int,
  onMinuteChange: (Int) -> Unit,
  recurrence: RecurrenceType,
  onRecurrenceChange: (RecurrenceType) -> Unit,
  selectedDays: List<Int>,
  onToggleDay: (Int) -> Unit,
  batteryThreshold: Int,
  onBatteryThresholdChange: (Int) -> Unit,
  triggerOnCharging: Boolean,
  onTriggerOnChargingChange: (Boolean) -> Unit,
  wifiOnly: Boolean,
  onWifiOnlyChange: (Boolean) -> Unit
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "TRIGGER CONFIGURATION",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      // Trigger Type Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TriggerTypeOption("TIME", "Schedule", triggerType == "TIME") { onTriggerTypeChange("TIME") }
        TriggerTypeOption("BATTERY", "Battery", triggerType == "BATTERY") { onTriggerTypeChange("BATTERY") }
        TriggerTypeOption("CONNECTIVITY", "Network", triggerType == "CONNECTIVITY") { onTriggerTypeChange("CONNECTIVITY") }
      }

      Spacer(modifier = Modifier.height(16.dp))

      when (triggerType) {
        "TIME" -> {
          // Time Pickers
          Text("Trigger Time: %02d:%02d".format(timeHour, timeMinute), style = MaterialTheme.typography.titleMedium, color = NeonGreen, fontFamily = FontFamily.Monospace)
          Spacer(modifier = Modifier.height(8.dp))
          Text("Hour (0-23): $timeHour", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Slider(
            value = timeHour.toFloat(),
            onValueChange = { onHourChange(it.toInt()) },
            valueRange = 0f..23f,
            steps = 22,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
          )

          Text("Minute (0-59): $timeMinute", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Slider(
            value = timeMinute.toFloat(),
            onValueChange = { onMinuteChange(it.toInt()) },
            valueRange = 0f..59f,
            steps = 58,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text("Recurrence", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            RecurrenceChip("Daily", recurrence == RecurrenceType.DAILY) { onRecurrenceChange(RecurrenceType.DAILY) }
            RecurrenceChip("Weekly", recurrence == RecurrenceType.WEEKLY) { onRecurrenceChange(RecurrenceType.WEEKLY) }
            RecurrenceChip("One-Time", recurrence == RecurrenceType.ONE_TIME) { onRecurrenceChange(RecurrenceType.ONE_TIME) }
          }

          if (recurrence == RecurrenceType.WEEKLY) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Days of Week", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              val daysMap = listOf(
                Calendar.SUNDAY to "S",
                Calendar.MONDAY to "M",
                Calendar.TUESDAY to "T",
                Calendar.WEDNESDAY to "W",
                Calendar.THURSDAY to "T",
                Calendar.FRIDAY to "F",
                Calendar.SATURDAY to "S"
              )
              daysMap.forEach { (d, label) ->
                val isSelected = selectedDays.contains(d)
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NeonGreen else CyberTokens.SurfaceDark)
                    .clickable { onToggleDay(d) },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = if (isSelected) Color.Black else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        }

        "BATTERY" -> {
          Text("Battery Threshold: $batteryThreshold%", style = MaterialTheme.typography.titleMedium, color = StatusWarning, fontFamily = FontFamily.Monospace)
          Slider(
            value = batteryThreshold.toFloat(),
            onValueChange = { onBatteryThresholdChange(it.toInt()) },
            valueRange = 5f..90f,
            steps = 16,
            colors = SliderDefaults.colors(thumbColor = StatusWarning, activeTrackColor = StatusWarning)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Trigger when charging starts", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Switch(
              checked = triggerOnCharging,
              onCheckedChange = onTriggerOnChargingChange,
              colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.3f))
            )
          }
        }

        "CONNECTIVITY" -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Trigger ONLY when connected to Wi-Fi", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Switch(
              checked = wifiOnly,
              onCheckedChange = onWifiOnlyChange,
              colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(0.3f))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StepConditions(
  requireMinBattery: Boolean,
  onRequireMinBatteryChange: (Boolean) -> Unit,
  minBattery: Int,
  onMinBatteryChange: (Int) -> Unit,
  requireCharging: Boolean,
  onRequireChargingChange: (Boolean) -> Unit,
  networkRequirement: NetworkRequirementType,
  onNetworkRequirementChange: (NetworkRequirementType) -> Unit
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "PRE-EXECUTION CONDITIONS",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Conditions must be satisfied before the routine runs. If unmet, the routine safely skips without errors.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(14.dp))

      // Battery Condition
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Require minimum battery level", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(
          checked = requireMinBattery,
          onCheckedChange = onRequireMinBatteryChange,
          colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.3f))
        )
      }

      if (requireMinBattery) {
        Text("Minimum Battery: $minBattery%", style = MaterialTheme.typography.labelSmall, color = StatusWarning)
        Slider(
          value = minBattery.toFloat(),
          onValueChange = { onMinBatteryChange(it.toInt()) },
          valueRange = 10f..80f,
          colors = SliderDefaults.colors(thumbColor = StatusWarning, activeTrackColor = StatusWarning)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Charging Condition
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Device must be plugged in / charging", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(
          checked = requireCharging,
          onCheckedChange = onRequireChargingChange,
          colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.3f))
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Network Condition
      Text("Network Requirement", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        NetworkRequirementChip("None", networkRequirement == NetworkRequirementType.NO_NETWORK_REQUIRED) {
          onNetworkRequirementChange(NetworkRequirementType.NO_NETWORK_REQUIRED)
        }
        NetworkRequirementChip("Internet", networkRequirement == NetworkRequirementType.ANY_NETWORK) {
          onNetworkRequirementChange(NetworkRequirementType.ANY_NETWORK)
        }
        NetworkRequirementChip("Wi-Fi Only", networkRequirement == NetworkRequirementType.WIFI_ONLY) {
          onNetworkRequirementChange(NetworkRequirementType.WIFI_ONLY)
        }
      }
    }
  }
}

@Composable
private fun StepRoutine(
  actions: List<AgentAction>,
  onAddActionClick: () -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onDelete: (Int) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "ROUTINE PIPELINE (${actions.size} STEPS)",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      FuturisticButton(
        text = "+ ADD STEP",
        onClick = onAddActionClick,
        isPrimary = true,
        modifier = Modifier.testTag("add_routine_step_button")
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (actions.isEmpty()) {
      FuturisticCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("No actions configured in this routine.", color = TextSecondary)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Tap + ADD STEP to add notifications, research, AI prompts, or device actions.", color = TextSecondary.copy(0.7f), style = MaterialTheme.typography.bodySmall)
        }
      }
    } else {
      actions.forEachIndexed { idx, action ->
        ActionStepCard(
          stepIndex = idx + 1,
          action = action,
          isFirst = idx == 0,
          isLast = idx == actions.size - 1,
          onMoveUp = { onMoveUp(idx) },
          onMoveDown = { onMoveDown(idx) },
          onDelete = { onDelete(idx) }
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun ActionStepCard(
  stepIndex: Int,
  action: AgentAction,
  isFirst: Boolean,
  isLast: Boolean,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onDelete: () -> Unit
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "$stepIndex.",
          style = MaterialTheme.typography.titleMedium,
          color = NeonCyan,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = action.description,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${action.type.label} • ${action.parameters.entries.joinToString { "${it.key}: ${it.value}" }}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 2
          )
        }
      }

      Row {
        if (!isFirst) {
          IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextSecondary)
          }
        }
        if (!isLast) {
          IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextSecondary)
          }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.8f))
        }
      }
    }
  }
}

@Composable
private fun StepPolicies(
  confirmationPolicy: ConfirmationPolicyType,
  onConfirmationPolicyChange: (ConfirmationPolicyType) -> Unit,
  notifyOnSuccess: Boolean,
  onNotifyOnSuccessChange: (Boolean) -> Unit,
  notifyOnFailure: Boolean,
  onNotifyOnFailureChange: (Boolean) -> Unit
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "CONFIRMATION & NOTIFICATION POLICIES",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      Text("Confirmation Policy", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ConfirmationPolicyType.values().forEach { pol ->
          val isSelected = pol == confirmationPolicy
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else CyberTokens.SurfaceDark)
              .border(1.dp, if (isSelected) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(8.dp))
              .clickable { onConfirmationPolicyChange(pol) }
              .padding(12.dp)
          ) {
            Column {
              Text(
                text = pol.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) NeonCyan else TextPrimary,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Notification Toggles
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Notification on routine success", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(
          checked = notifyOnSuccess,
          onCheckedChange = onNotifyOnSuccessChange,
          colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.3f))
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Notification on routine failure", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(
          checked = notifyOnFailure,
          onCheckedChange = onNotifyOnFailureChange,
          colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(0.3f))
        )
      }
    }
  }
}

@Composable
private fun StepPreview(
  name: String,
  category: AutomationCategory,
  triggerSummary: String,
  actionsCount: Int,
  actions: List<AgentAction>,
  onDryRun: () -> Unit,
  dryRunSummary: String?
) {
  FuturisticCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "PLAN PREVIEW & DRY RUN",
        style = MaterialTheme.typography.titleSmall,
        color = NeonCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Name: ${if (name.isBlank()) "[Untitled]" else name}",
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Category: ${category.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = NeonMagenta,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "Trigger: $triggerSummary",
        style = MaterialTheme.typography.bodySmall,
        color = NeonGreen,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = "Actions: $actionsCount sequential step(s)",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Spacer(modifier = Modifier.height(12.dp))

      FuturisticButton(
        text = "EXECUTE DRY RUN SIMULATION",
        onClick = onDryRun,
        isPrimary = false,
        modifier = Modifier.fillMaxWidth()
      )

      if (dryRunSummary != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = dryRunSummary,
          style = MaterialTheme.typography.bodySmall,
          color = NeonGreen,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

@Composable
private fun AddActionDialog(
  onDismiss: () -> Unit,
  onAddAction: (AgentAction) -> Unit
) {
  var selectedType by remember { mutableStateOf(ActionType.SEND_NOTIFICATION) }
  var actionDesc by remember { mutableStateOf("") }
  var paramText1 by remember { mutableStateOf("") }
  var paramText2 by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Routine Step", color = TextPrimary) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Action Type", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        val supportedTypes = listOf(
          ActionType.SEND_NOTIFICATION,
          ActionType.WEB_RESEARCH,
          ActionType.AI_REQUEST,
          ActionType.TTS_OUTPUT,
          ActionType.MEMORY_WRITE,
          ActionType.CHECK_BATTERY,
          ActionType.CHECK_CONNECTIVITY
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          supportedTypes.forEach { type ->
            val isSelected = type == selectedType
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) NeonCyan.copy(0.2f) else CyberTokens.SurfaceDark)
                .border(1.dp, if (isSelected) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(6.dp))
                .clickable { selectedType = type }
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text(type.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) NeonCyan else TextSecondary)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedType) {
          ActionType.SEND_NOTIFICATION -> {
            Text("Notification Title", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText1,
              onValueChange = { paramText1 = it },
              placeholder = { Text("e.g., Morning Briefing", color = TextSecondary) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Notification Body (use \$previousResult, \$researchResult, \$battery)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText2,
              onValueChange = { paramText2 = it },
              placeholder = { Text("\$previousResult", color = TextSecondary) },
              minLines = 2,
              modifier = Modifier.fillMaxWidth()
            )
          }

          ActionType.WEB_RESEARCH -> {
            Text("Search Query (supports variables)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText1,
              onValueChange = { paramText1 = it },
              placeholder = { Text("e.g., top tech news today", color = TextSecondary) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          ActionType.AI_REQUEST -> {
            Text("AI Prompt Template (supports variables)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText1,
              onValueChange = { paramText1 = it },
              placeholder = { Text("e.g., Summarize top 3 points: \$researchResult", color = TextSecondary) },
              minLines = 2,
              modifier = Modifier.fillMaxWidth()
            )
          }

          ActionType.TTS_OUTPUT -> {
            Text("Spoken Text", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText1,
              onValueChange = { paramText1 = it },
              placeholder = { Text("Good morning! Here is today's schedule.", color = TextSecondary) },
              minLines = 2,
              modifier = Modifier.fillMaxWidth()
            )
          }

          ActionType.MEMORY_WRITE -> {
            Text("Memory Key", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText1,
              onValueChange = { paramText1 = it },
              placeholder = { Text("last_briefing_time", color = TextSecondary) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Memory Value", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            OutlinedTextField(
              value = paramText2,
              onValueChange = { paramText2 = it },
              placeholder = { Text("\$currentTime", color = TextSecondary) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          else -> {}
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val params = mutableMapOf<String, Any?>()
          val desc = when (selectedType) {
            ActionType.SEND_NOTIFICATION -> {
              params["title"] = if (paramText1.isNotBlank()) paramText1 else "Aegis Routine"
              params["body"] = if (paramText2.isNotBlank()) paramText2 else "\$previousResult"
              "Send Notification: ${params["title"]}"
            }
            ActionType.WEB_RESEARCH -> {
              params["query"] = if (paramText1.isNotBlank()) paramText1 else "latest news"
              "Web Research: ${params["query"]}"
            }
            ActionType.AI_REQUEST -> {
              params["prompt"] = if (paramText1.isNotBlank()) paramText1 else "Summarize information"
              "AI Query: ${params["prompt"]}"
            }
            ActionType.TTS_OUTPUT -> {
              params["text"] = if (paramText1.isNotBlank()) paramText1 else "Routine completed"
              "Speak Text (TTS)"
            }
            ActionType.MEMORY_WRITE -> {
              params["key"] = if (paramText1.isNotBlank()) paramText1 else "note"
              params["value"] = paramText2
              "Store Memory: ${params["key"]}"
            }
            ActionType.CHECK_BATTERY -> {
              "Check Battery Level"
            }
            ActionType.CHECK_CONNECTIVITY -> {
              "Verify Network Connectivity"
            }
            else -> selectedType.label
          }

          val action = AgentAction(
            id = UUID.randomUUID().toString(),
            type = selectedType,
            description = desc,
            parameters = params,
            requiredCapabilities = emptyList()
          )
          onAddAction(action)
        }
      ) {
        Text("Add Step", color = NeonCyan, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = CyberBackgroundDark
  )
}

@Composable
private fun TriggerTypeOption(tag: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) NeonCyan.copy(0.2f) else CyberTokens.SurfaceDark)
      .border(1.dp, if (isSelected) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(label, color = if (isSelected) NeonCyan else TextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun RecurrenceChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) NeonGreen.copy(0.2f) else CyberTokens.SurfaceDark)
      .border(1.dp, if (isSelected) NeonGreen else CyberTokens.BorderSubtle, RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(label, color = if (isSelected) NeonGreen else TextSecondary, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
  }
}

@Composable
private fun NetworkRequirementChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) NeonCyan.copy(0.2f) else CyberTokens.SurfaceDark)
      .border(1.dp, if (isSelected) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(label, color = if (isSelected) NeonCyan else TextSecondary, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
  }
}

private fun buildTriggerSummary(
  triggerType: String,
  hour: Int,
  minute: Int,
  recurrence: RecurrenceType
): String {
  return when (triggerType) {
    "TIME" -> when (recurrence) {
      RecurrenceType.DAILY -> "Daily at %02d:%02d".format(hour, minute)
      RecurrenceType.WEEKLY -> "Weekly at %02d:%02d".format(hour, minute)
      RecurrenceType.ONE_TIME -> "One-time at %02d:%02d".format(hour, minute)
      else -> "Schedule at %02d:%02d".format(hour, minute)
    }
    "BATTERY" -> "Device Battery Event"
    "CONNECTIVITY" -> "Network Connectivity Event"
    else -> "Manual Trigger"
  }
}

private fun buildAutomationEntity(
  id: String,
  name: String,
  description: String,
  category: AutomationCategory,
  enabled: Boolean,
  state: AutomationState,
  triggerType: String,
  timeHour: Int,
  timeMinute: Int,
  recurrence: RecurrenceType,
  days: List<Int>,
  batteryThreshold: Int,
  triggerOnCharging: Boolean,
  wifiOnly: Boolean,
  requireMinBattery: Boolean,
  minBattery: Int,
  requireCharging: Boolean,
  networkReq: NetworkRequirementType,
  actions: List<AgentAction>,
  confirmationPolicy: ConfirmationPolicyType,
  notifySuccess: Boolean,
  notifyFailure: Boolean
): Automation {
  val trigger: AutomationTrigger = when (triggerType) {
    "TIME" -> AutomationTrigger.TimeSchedule(
      timeHour = timeHour,
      timeMinute = timeMinute,
      recurrence = recurrence,
      daysOfWeek = days
    )
    "BATTERY" -> AutomationTrigger.BatteryEvent(
      triggerOnLow = true,
      thresholdPercent = batteryThreshold,
      triggerOnCharging = triggerOnCharging
    )
    "CONNECTIVITY" -> AutomationTrigger.ConnectivityEvent(
      triggerOnConnected = true,
      wifiOnly = wifiOnly
    )
    else -> AutomationTrigger.ManualTrigger
  }

  val conditions = mutableListOf<AutomationCondition>()
  if (requireMinBattery || requireCharging) {
    conditions.add(AutomationCondition.BatteryCondition(minLevel = if (requireMinBattery) minBattery else 0, requireCharging = requireCharging))
  }
  if (networkReq != NetworkRequirementType.NO_NETWORK_REQUIRED) {
    conditions.add(AutomationCondition.NetworkCondition(requirement = networkReq))
  }

  return Automation(
    id = id,
    name = name,
    description = description,
    category = category,
    enabled = enabled,
    state = state,
    trigger = trigger,
    conditions = conditions,
    actions = actions,
    executionPolicy = ExecutionPolicy(
      networkRequired = networkReq != NetworkRequirementType.NO_NETWORK_REQUIRED || actions.any { it.type == ActionType.WEB_RESEARCH || it.type == ActionType.AI_REQUEST },
      chargingRequired = requireCharging,
      minBatteryLevel = if (requireMinBattery) minBattery else 0
    ),
    notificationPolicy = AutomationNotificationPolicy(
      notificationsEnabled = notifySuccess || notifyFailure,
      notifyOnSuccess = notifySuccess,
      notifyOnFailure = notifyFailure
    ),
    confirmationPolicy = AutomationConfirmationPolicy(
      policyType = confirmationPolicy
    )
  )
}
