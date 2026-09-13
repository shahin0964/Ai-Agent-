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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.core.agent.AgentController
import com.example.core.automation.interpreter.InterpreterResult
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationCategory
import com.example.core.automation.model.AutomationExecutionStatus
import com.example.core.automation.model.AutomationHistory
import com.example.core.automation.model.AutomationState
import com.example.core.automation.routine.RoutineExecutionResult
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutomationHubScreen(
  agentController: AgentController,
  onNavigateBack: () -> Unit,
  onOpenBuilder: (String?) -> Unit,
  modifier: Modifier = Modifier
) {
  val automationEngine = agentController.automationEngine
  val automations by (automationEngine?.automations?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
  val historyList by (automationEngine?.history?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
  val scope = rememberCoroutineScope()

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<AutomationCategory?>(null) }
  var selectedStatusTab by remember { mutableStateOf("ALL") } // ALL, ENABLED, DISABLED, DRAFTS, HISTORY

  var runningAutomationId by remember { mutableStateOf<String?>(null) }
  var activeExecutionResult by remember { mutableStateOf<RoutineExecutionResult?>(null) }
  var viewingHistoryItem by remember { mutableStateOf<AutomationHistory?>(null) }
  var dryRunResult by remember { mutableStateOf<RoutineExecutionResult?>(null) }
  var deleteConfirmId by remember { mutableStateOf<String?>(null) }
  var showAiPromptDialog by remember { mutableStateOf(false) }
  var aiPromptText by remember { mutableStateOf("") }
  var aiPromptMessage by remember { mutableStateOf<String?>(null) }
  var showExportDialog by remember { mutableStateOf(false) }
  var exportJsonText by remember { mutableStateOf("") }

  val filteredAutomations = remember(automations, searchQuery, selectedCategory, selectedStatusTab) {
    automations.filter { auto ->
      val matchesSearch = searchQuery.isBlank() ||
        auto.name.contains(searchQuery, ignoreCase = true) ||
        auto.description.contains(searchQuery, ignoreCase = true)
      val matchesCategory = selectedCategory == null || auto.category == selectedCategory
      val matchesStatus = when (selectedStatusTab) {
        "ENABLED" -> auto.enabled
        "DISABLED" -> !auto.enabled && auto.state != AutomationState.DRAFT
        "DRAFTS" -> auto.state == AutomationState.DRAFT
        else -> true
      }
      matchesSearch && matchesCategory && matchesStatus
    }
  }

  val activeCount = automations.count { it.enabled }
  val draftCount = automations.count { it.state == AutomationState.DRAFT }
  val totalRuns = historyList.size
  val successRuns = historyList.count { it.status == AutomationExecutionStatus.SUCCESS }
  val successRate = if (totalRuns > 0) "${(successRuns * 100) / totalRuns}%" else "100%"

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
      // Top Navigation Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = NeonCyan
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column {
            Text(
              text = "AUTOMATION HUB",
              style = MaterialTheme.typography.titleMedium,
              color = NeonCyan,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Schedules • Routines • Triggers • Scheduled Agent",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }
        }

        Row {
          IconButton(
            onClick = {
              if (automationEngine != null) {
                exportJsonText = automationEngine.exportAutomations()
                showExportDialog = true
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.FileUpload,
              contentDescription = "Export Automations",
              tint = TextSecondary
            )
          }
          IconButton(
            onClick = { showAiPromptDialog = true }
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Create with AI",
              tint = NeonMagenta
            )
          }
        }
      }

      // Telemetry Summary Card
      FuturisticCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          TelemetryStat(label = "TOTAL", value = "${automations.size}", color = TextPrimary)
          TelemetryStat(label = "ACTIVE", value = "$activeCount", color = NeonGreen)
          TelemetryStat(label = "DRAFTS", value = "$draftCount", color = StatusWarning)
          TelemetryStat(label = "SUCCESS", value = successRate, color = NeonCyan)
        }
      }

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search automations, triggers, actions...", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = NeonCyan,
          unfocusedBorderColor = CyberTokens.BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("automation_search_field")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Category Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        CategoryChip(
          label = "ALL",
          isSelected = selectedCategory == null,
          onClick = { selectedCategory = null }
        )
        AutomationCategory.values().forEach { cat ->
          CategoryChip(
            label = cat.displayName,
            isSelected = selectedCategory == cat,
            onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Status Filter Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberTokens.SurfaceDark, RoundedCornerShape(8.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        StatusTabItem("ALL", "All", selectedStatusTab == "ALL") { selectedStatusTab = "ALL" }
        StatusTabItem("ENABLED", "Active ($activeCount)", selectedStatusTab == "ENABLED") { selectedStatusTab = "ENABLED" }
        StatusTabItem("DRAFTS", "Drafts ($draftCount)", selectedStatusTab == "DRAFTS") { selectedStatusTab = "DRAFTS" }
        StatusTabItem("HISTORY", "Logs (${historyList.size})", selectedStatusTab == "HISTORY") { selectedStatusTab = "HISTORY" }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Content List
      if (selectedStatusTab == "HISTORY") {
        // Render Execution Logs
        if (historyList.isEmpty()) {
          EmptyStateView(
            message = "No execution history recorded yet.",
            subMessage = "Trigger or run an automation to view real-time telemetry."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(historyList, key = { it.executionId }) { historyItem ->
              HistoryItemCard(
                history = historyItem,
                onClick = { viewingHistoryItem = historyItem }
              )
            }
          }
        }
      } else {
        // Render Automations List
        if (filteredAutomations.isEmpty()) {
          EmptyStateView(
            message = if (searchQuery.isBlank()) "No automations in this category." else "No matching automations found.",
            subMessage = "Tap the + button below or ask the AI to draft a routine."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredAutomations, key = { it.id }) { auto ->
              AutomationCardItem(
                automation = auto,
                isRunning = runningAutomationId == auto.id,
                onToggleEnabled = { isEnabled ->
                  automationEngine?.setAutomationEnabled(auto.id, isEnabled)
                },
                onRunNow = {
                  if (automationEngine != null && runningAutomationId == null) {
                    runningAutomationId = auto.id
                    scope.launch {
                      val res = automationEngine.runAutomationNow(auto.id)
                      runningAutomationId = null
                      activeExecutionResult = res
                    }
                  }
                },
                onDryRun = {
                  if (automationEngine != null) {
                    scope.launch {
                      val res = automationEngine.dryRunAutomation(auto.id)
                      dryRunResult = res
                    }
                  }
                },
                onEdit = { onOpenBuilder(auto.id) },
                onDuplicate = {
                  automationEngine?.duplicateAutomation(auto.id)
                },
                onDelete = { deleteConfirmId = auto.id },
                modifier = Modifier.testTag("automation_card_${auto.id}")
              )
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
          }
        }
      }
    }

    // Floating Action Buttons
    FloatingActionButton(
      onClick = { onOpenBuilder(null) },
      containerColor = NeonCyan,
      contentColor = Color.Black,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("create_automation_fab")
    ) {
      Icon(Icons.Default.Add, contentDescription = "Create Automation")
    }

    // Delete Confirmation Dialog
    if (deleteConfirmId != null) {
      val targetId = deleteConfirmId!!
      val targetAuto = automations.firstOrNull { it.id == targetId }
      AlertDialog(
        onDismissRequest = { deleteConfirmId = null },
        title = { Text("Delete Automation", color = TextPrimary) },
        text = {
          Text(
            "Are you sure you want to permanently delete \"${targetAuto?.name ?: "this automation"}\"? This action cannot be undone.",
            color = TextSecondary
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              automationEngine?.deleteAutomation(targetId)
              deleteConfirmId = null
            }
          ) {
            Text("Delete", color = StatusError, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { deleteConfirmId = null }) {
            Text("Cancel", color = TextSecondary)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }

    // AI Routine Prompt Dialog
    if (showAiPromptDialog) {
      AlertDialog(
        onDismissRequest = {
          showAiPromptDialog = false
          aiPromptMessage = null
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonMagenta)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Natural Language Automation", color = TextPrimary)
          }
        },
        text = {
          Column {
            Text(
              "Describe a schedule or routine. Aegis will parse it into a safe DRAFT automation for you to review.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = aiPromptText,
              onValueChange = { aiPromptText = it },
              placeholder = { Text("e.g., Every Friday at 8 PM give me the latest tech news", color = TextSecondary) },
              minLines = 2,
              maxLines = 4,
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonMagenta,
                unfocusedBorderColor = CyberTokens.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
            if (aiPromptMessage != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = aiPromptMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = NeonGreen
              )
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              if (aiPromptText.isNotBlank() && automationEngine != null) {
                val res = automationEngine.interpretUserCommand(aiPromptText)
                when (res) {
                  is InterpreterResult.DraftCreated -> {
                    aiPromptMessage = "Created draft: ${res.automation.name}"
                    aiPromptText = ""
                  }
                  is InterpreterResult.ManageCommand -> {
                    aiPromptMessage = res.responseText
                    aiPromptText = ""
                  }
                  is InterpreterResult.ClarificationNeeded -> {
                    aiPromptMessage = res.prompt
                  }
                  InterpreterResult.NotAnAutomationCommand -> {
                    aiPromptMessage = "Could not parse routine pattern. Try 'Every day at 8 AM...'"
                  }
                }
              }
            }
          ) {
            Text("Draft Routine", color = NeonMagenta, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showAiPromptDialog = false
              aiPromptMessage = null
            }
          ) {
            Text("Close", color = TextSecondary)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }

    // Execution Result Dialog
    if (activeExecutionResult != null) {
      val res = activeExecutionResult!!
      AlertDialog(
        onDismissRequest = { activeExecutionResult = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, tint) = when (res.status) {
              AutomationExecutionStatus.SUCCESS -> Icons.Default.CheckCircle to NeonGreen
              AutomationExecutionStatus.PARTIAL_SUCCESS -> Icons.Default.CheckCircle to StatusWarning
              AutomationExecutionStatus.SKIPPED -> Icons.Default.HourglassEmpty to StatusWarning
              else -> Icons.Default.ErrorOutline to StatusError
            }
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execution: ${res.status.name}", color = TextPrimary)
          }
        },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
              text = "Automation: ${res.history.automationName}",
              style = MaterialTheme.typography.bodyMedium,
              color = NeonCyan,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Duration: ${res.history.durationMs} ms • Steps: ${res.history.completedSteps}/${res.history.totalSteps}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (res.outputMessage != null) {
              Text(
                text = "Output:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
              )
              Text(
                text = res.outputMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
              )
            }
            if (res.history.errorMessage != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Error: ${res.history.errorMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = StatusError
              )
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { activeExecutionResult = null }) {
            Text("OK", color = NeonCyan)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }

    // Dry Run Result Dialog
    if (dryRunResult != null) {
      val res = dryRunResult!!
      AlertDialog(
        onDismissRequest = { dryRunResult = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dry Run Preview", color = TextPrimary)
          }
        },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
              text = "Simulated run for: ${res.history.automationName}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            res.history.stepsLog.forEach { step ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${step.stepIndex}. ",
                  color = NeonCyan,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Column {
                  Text(
                    text = step.actionTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                  )
                  Text(
                    text = "${step.actionType} • ${step.details ?: "OK"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { dryRunResult = null }) {
            Text("Close", color = NeonCyan)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }

    // History Item Detail Dialog
    if (viewingHistoryItem != null) {
      val hist = viewingHistoryItem!!
      val timeFormat = SimpleDateFormat("MMM d, yyyy • hh:mm:ss a", Locale.getDefault())
      AlertDialog(
        onDismissRequest = { viewingHistoryItem = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execution Record", color = TextPrimary)
          }
        },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
              text = hist.automationName,
              style = MaterialTheme.typography.titleSmall,
              color = NeonCyan,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Time: ${timeFormat.format(Date(hist.startedAt))}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
            Text(
              text = "Status: ${hist.status.name} • Duration: ${hist.durationMs}ms",
              style = MaterialTheme.typography.bodySmall,
              color = if (hist.status == AutomationExecutionStatus.SUCCESS) NeonGreen else StatusError
            )
            if (hist.providerUsed != null) {
              Text(
                text = "Provider: ${hist.providerUsed}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
            if (hist.errorMessage != null) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Error: ${hist.errorMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = StatusError
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Step Log:", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            hist.stepsLog.forEach { step ->
              Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text("${step.stepIndex}. ", color = NeonCyan, fontFamily = FontFamily.Monospace)
                Column {
                  Text(step.actionTitle, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                  Text("${step.status}: ${step.details ?: ""}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { viewingHistoryItem = null }) {
            Text("Dismiss", color = NeonCyan)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }

    // Export Dialog
    if (showExportDialog) {
      AlertDialog(
        onDismissRequest = { showExportDialog = false },
        title = { Text("Export Automations", color = TextPrimary) },
        text = {
          Column {
            Text(
              "Below is your exported automation configuration JSON (API keys and sensitive tokens excluded):",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = exportJsonText,
              onValueChange = {},
              readOnly = true,
              minLines = 4,
              maxLines = 8,
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberTokens.BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showExportDialog = false }) {
            Text("Done", color = NeonCyan)
          }
        },
        containerColor = CyberBackgroundDark
      )
    }
  }
}

@Composable
private fun TelemetryStat(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = color,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
private fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberTokens.SurfaceDark)
      .border(1.dp, if (isSelected) NeonCyan else CyberTokens.BorderSubtle, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) NeonCyan else TextSecondary,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
private fun StatusTabItem(
  tag: String,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) NeonCyan else TextSecondary,
      fontFamily = FontFamily.Monospace,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun AutomationCardItem(
  automation: Automation,
  isRunning: Boolean,
  onToggleEnabled: (Boolean) -> Unit,
  onRunNow: () -> Unit,
  onDryRun: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val statusColor = when (automation.state) {
    AutomationState.ENABLED -> NeonGreen
    AutomationState.RUNNING -> NeonCyan
    AutomationState.DRAFT -> StatusWarning
    AutomationState.FAILED -> StatusError
    AutomationState.DISABLED -> TextSecondary
    else -> TextSecondary
  }

  val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  FuturisticCard(
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Top Row: Title, Category Badge, Enabled Switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = automation.name,
              style = MaterialTheme.typography.titleSmall,
              color = TextPrimary,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${automation.category.displayName} • ${automation.state.label}",
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontFamily = FontFamily.Monospace
          )
        }

        Switch(
          checked = automation.enabled,
          onCheckedChange = onToggleEnabled,
          colors = SwitchDefaults.colors(
            checkedThumbColor = NeonGreen,
            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
            uncheckedThumbColor = TextSecondary,
            uncheckedTrackColor = CyberTokens.SurfaceDark
          ),
          modifier = Modifier.testTag("automation_toggle_${automation.id}")
        )
      }

      if (automation.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = automation.description,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Trigger & Step Info
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CyberTokens.SurfaceDark, RoundedCornerShape(6.dp))
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Schedule, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = automation.trigger.description,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
          )
        }
        Text(
          text = "${automation.actions.size} step(s)",
          style = MaterialTheme.typography.labelSmall,
          color = NeonMagenta,
          fontFamily = FontFamily.Monospace
        )
      }

      // Next Run / Last Run info
      if (automation.nextRunAt != null || automation.lastRunAt != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          if (automation.nextRunAt != null) {
            Text(
              text = "Next: ${timeFormat.format(Date(automation.nextRunAt))}",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }
          if (automation.runCount > 0) {
            Text(
              text = "Runs: ${automation.runCount} (${automation.failureCount} failed)",
              style = MaterialTheme.typography.labelSmall,
              color = if (automation.failureCount > 0) StatusWarning else TextSecondary,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Action Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row {
          // Run Now Button
          FuturisticButton(
            text = if (isRunning) "RUNNING..." else "RUN NOW",
            onClick = onRunNow,
            enabled = !isRunning,
            isPrimary = true,
            modifier = Modifier.testTag("run_now_${automation.id}")
          )
          Spacer(modifier = Modifier.width(6.dp))
          // Dry Run Button
          IconButton(
            onClick = onDryRun,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = "Dry Run Preview", tint = NeonCyan)
          }
        }

        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Automation", tint = TextSecondary)
          }
          IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Automation", tint = TextSecondary)
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Automation", tint = StatusError)
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryItemCard(
  history: AutomationHistory,
  onClick: () -> Unit
) {
  val timeFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
  val statusColor = when (history.status) {
    AutomationExecutionStatus.SUCCESS -> NeonGreen
    AutomationExecutionStatus.PARTIAL_SUCCESS -> StatusWarning
    AutomationExecutionStatus.SKIPPED -> StatusWarning
    else -> StatusError
  }

  FuturisticCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = history.automationName,
          style = MaterialTheme.typography.titleSmall,
          color = TextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${timeFormat.format(Date(history.startedAt))} • ${history.durationMs}ms",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
          fontFamily = FontFamily.Monospace
        )
        if (history.errorMessage != null) {
          Text(
            text = history.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = StatusError,
            maxLines = 1
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(statusColor.copy(alpha = 0.2f))
          .border(1.dp, statusColor, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = history.status.name,
          style = MaterialTheme.typography.labelSmall,
          color = statusColor,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun EmptyStateView(
  message: String,
  subMessage: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = Icons.Default.Schedule,
      contentDescription = null,
      tint = CyberTokens.BorderSubtle,
      modifier = Modifier.size(48.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.titleSmall,
      color = TextSecondary,
      fontFamily = FontFamily.Monospace
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = subMessage,
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary.copy(alpha = 0.7f)
    )
  }
}
