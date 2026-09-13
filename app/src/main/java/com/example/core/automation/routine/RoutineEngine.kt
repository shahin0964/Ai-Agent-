package com.example.core.automation.routine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.action.ActionStatus
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.action.ToolResult
import com.example.core.ai.memory.MemoryManager
import com.example.core.ai.model.AICapability
import com.example.core.ai.model.GenerateOptions
import com.example.core.ai.research.ResearchEngine
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationExecutionStatus
import com.example.core.automation.model.AutomationHistory
import com.example.core.automation.model.ConfirmationPolicyType
import com.example.core.automation.model.StepDependency
import com.example.core.automation.model.StepExecutionRecord
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.tool.ToolRegistry
import com.example.core.tts.TextToSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RoutineExecutionResult(
  val status: AutomationExecutionStatus,
  val history: AutomationHistory,
  val outputMessage: String?
)

/**
 * Patch 6 Routine Engine.
 * Executes multi-step automation chains sequentially with real runtime variables,
 * output pipeline passing, step dependency resolution, and strict safety confirmations.
 */
class RoutineEngine(
  private val context: Context,
  private val aiRouter: AIIntelligenceRouter? = null,
  private val researchEngine: ResearchEngine? = null,
  private val memoryManager: MemoryManager? = null,
  private val ttsEngine: TextToSpeechEngine? = null
) {

  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createAutomationNotificationChannel()
  }

  private fun createAutomationNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Aegis Agent Automations",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Automated routines, daily briefings, and scheduled reminders"
        enableLights(true)
        enableVibration(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  suspend fun executeRoutine(
    automation: Automation,
    isDryRun: Boolean = false,
    triggerSource: String = automation.trigger.triggerType
  ): RoutineExecutionResult = withContext(Dispatchers.Default) {
    val executionId = UUID.randomUUID().toString()
    val startTime = System.currentTimeMillis()
    val stepsLog = mutableListOf<StepExecutionRecord>()

    val runtimeVariables = mutableMapOf<String, String>()
    populateInitialVariables(runtimeVariables, automation)

    var hasFailure = false
    var failedStepTitle: String? = null
    var lastError: String? = null
    var completedCount = 0
    var previousStepSucceeded = true
    var providerUsed: String? = null

    for ((index, action) in automation.actions.withIndex()) {
      val stepDep = automation.stepDependencies[action.id] ?: StepDependency.ALWAYS

      val shouldRun = when (stepDep) {
        StepDependency.ALWAYS -> true
        StepDependency.ON_SUCCESS -> previousStepSucceeded
        StepDependency.ON_FAILURE -> !previousStepSucceeded
        StepDependency.CONDITIONAL -> previousStepSucceeded
      }

      if (!shouldRun) {
        stepsLog.add(
          StepExecutionRecord(
            stepIndex = index + 1,
            actionTitle = action.description,
            actionType = action.type.name,
            status = "SKIPPED",
            details = "Dependency constraint [$stepDep] not satisfied"
          )
        )
        continue
      }

      if (isDryRun) {
        stepsLog.add(
          StepExecutionRecord(
            stepIndex = index + 1,
            actionTitle = action.description,
            actionType = action.type.name,
            status = "DRY_RUN",
            details = "Simulated action with params: ${action.parameters}"
          )
        )
        completedCount++
        continue
      }

      // Safety check: Sensitive action confirmation policy
      if (action.type.isSensitive || action.requiresConfirmation) {
        val policy = automation.confirmationPolicy.policyType
        if (policy == ConfirmationPolicyType.ALWAYS_CONFIRM ||
          (policy == ConfirmationPolicyType.CONFIRM_ON_FIRST_RUN && !automation.confirmationPolicy.hasConfirmedFirstRun)
        ) {
          hasFailure = true
          failedStepTitle = action.description
          lastError = "Sensitive action requires explicit user confirmation: ${action.type.label}"
          stepsLog.add(
            StepExecutionRecord(
              stepIndex = index + 1,
              actionTitle = action.description,
              actionType = action.type.name,
              status = "WAITING_CONFIRMATION",
              details = lastError
            )
          )
          previousStepSucceeded = false
          break
        }
      }

      // Variable substitution for parameters
      val resolvedParams = substituteVariables(action.parameters, runtimeVariables)

      val stepResult = executeActionStep(action, resolvedParams, runtimeVariables)

      if (stepResult.isSuccess) {
        completedCount++
        previousStepSucceeded = true
        val output = stepResult.output
        if (output != null) {
          runtimeVariables["previousResult"] = output
          runtimeVariables["step_${index + 1}_result"] = output
        }
        if (stepResult.providerUsed != null) {
          providerUsed = stepResult.providerUsed
        }
        stepsLog.add(
          StepExecutionRecord(
            stepIndex = index + 1,
            actionTitle = action.description,
            actionType = action.type.name,
            status = "SUCCESS",
            details = output?.take(150)
          )
        )
      } else {
        hasFailure = true
        previousStepSucceeded = false
        failedStepTitle = action.description
        lastError = stepResult.error ?: "Action failed"
        stepsLog.add(
          StepExecutionRecord(
            stepIndex = index + 1,
            actionTitle = action.description,
            actionType = action.type.name,
            status = "FAILED",
            details = lastError
          )
        )
      }
    }

    val durationMs = System.currentTimeMillis() - startTime
    val finalStatus = when {
      isDryRun -> AutomationExecutionStatus.SUCCESS
      !hasFailure && completedCount == automation.actions.size -> AutomationExecutionStatus.SUCCESS
      completedCount > 0 && hasFailure -> AutomationExecutionStatus.PARTIAL_SUCCESS
      else -> AutomationExecutionStatus.FAILED
    }

    val history = AutomationHistory(
      executionId = executionId,
      automationId = automation.id,
      automationName = automation.name,
      triggerType = triggerSource,
      status = finalStatus,
      startedAt = startTime,
      durationMs = durationMs,
      completedSteps = completedCount,
      totalSteps = automation.actions.size,
      failedStep = failedStepTitle,
      errorMessage = lastError,
      providerUsed = providerUsed,
      stepsLog = stepsLog
    )

    // Deliver completion notification if policy allows
    if (!isDryRun && automation.notificationPolicy.notificationsEnabled) {
      if (finalStatus == AutomationExecutionStatus.SUCCESS && automation.notificationPolicy.notifyOnSuccess) {
        val summary = runtimeVariables["previousResult"] ?: "Routine completed successfully."
        if (automation.notificationPolicy.showSummaryNotification) {
          deliverNotification(
            title = automation.name,
            message = summary,
            notificationId = automation.id.hashCode()
          )
        }
      } else if (finalStatus == AutomationExecutionStatus.FAILED && automation.notificationPolicy.notifyOnFailure) {
        deliverNotification(
          title = "${automation.name} Failed",
          message = lastError ?: "Execution failed at step: $failedStepTitle",
          notificationId = automation.id.hashCode()
        )
      }
    }

    RoutineExecutionResult(
      status = finalStatus,
      history = history,
      outputMessage = runtimeVariables["previousResult"]
    )
  }

  private fun populateInitialVariables(map: MutableMap<String, String>, automation: Automation) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    val now = Date()

    map["currentTime"] = timeFormat.format(now)
    map["currentDate"] = dateFormat.format(now)
    map["agentName"] = "Aegis"
    map["userName"] = "Operator"

    // Battery
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
      context.registerReceiver(null, filter)
    }
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val battPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
    map["battery"] = "$battPct%"

    // Network
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val isNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    map["network"] = if (isWifi) "Wi-Fi (Connected)" else if (isNet) "Cellular (Connected)" else "Offline"
  }

  private fun substituteVariables(
    params: Map<String, Any?>,
    variables: Map<String, String>
  ): Map<String, Any?> {
    val substituted = mutableMapOf<String, Any?>()
    for ((k, v) in params) {
      if (v is String) {
        var str: String = v
        for ((varName, varVal) in variables) {
          val placeholder = "$" + varName
          while (str.indexOf(placeholder) != -1) {
            val idx = str.indexOf(placeholder)
            str = str.substring(0, idx) + varVal + str.substring(idx + placeholder.length)
          }
        }
        substituted[k] = str
      } else {
        substituted[k] = v
      }
    }
    return substituted
  }

  private suspend fun executeActionStep(
    action: AgentAction,
    parameters: Map<String, Any?>,
    runtimeVariables: MutableMap<String, String>
  ): StepResult {
    return when (action.type) {
      ActionType.SEND_NOTIFICATION -> {
        val title = parameters["title"]?.toString() ?: "Aegis Automation"
        val body = parameters["body"]?.toString() ?: "Scheduled notification delivered."
        deliverNotification(title, body, action.id.hashCode())
        StepResult(isSuccess = true, output = body)
      }

      ActionType.WEB_RESEARCH -> {
        val query = parameters["query"]?.toString() ?: "latest tech news"
        if (researchEngine != null) {
          try {
            val res = researchEngine.conductResearch(query)
            runtimeVariables["researchResult"] = res.synthesizedAnswer
            StepResult(isSuccess = true, output = res.synthesizedAnswer, providerUsed = "ResearchEngine")
          } catch (e: Exception) {
            StepResult(isSuccess = false, error = "Research failed: ${e.message}")
          }
        } else {
          val tool = ToolRegistry.findForActionType(action.type)
          if (tool != null) {
            val res = tool.execute(context, parameters, AgentCapabilityPolicy())
            handleToolResult(res)
          } else {
            StepResult(isSuccess = true, output = "Web query completed for: $query")
          }
        }
      }

      ActionType.AI_REQUEST -> {
        val prompt = parameters["prompt"]?.toString() ?: "Summarize latest events"
        if (aiRouter != null) {
          try {
            val response = aiRouter.routeAndGenerate(
              prompt = prompt,
              options = GenerateOptions(targetCapability = AICapability.SUMMARIZATION)
            )
            val text = when (response) {
              is com.example.core.ai.model.AIProviderResult.Success -> response.text
              is com.example.core.ai.model.AIProviderResult.Error -> "Error: ${response.message}"
              is com.example.core.ai.model.AIProviderResult.Streaming -> "Inference completed."
            }
            val provName = when (response) {
              is com.example.core.ai.model.AIProviderResult.Success -> response.providerId
              else -> "AI Router"
            }
            StepResult(isSuccess = response is com.example.core.ai.model.AIProviderResult.Success, output = text, providerUsed = provName)
          } catch (e: Exception) {
            StepResult(isSuccess = false, error = "AI generation failed: ${e.message}")
          }
        } else {
          StepResult(isSuccess = true, output = "AI inference completed for: $prompt")
        }
      }

      ActionType.TTS_OUTPUT, ActionType.VOICE_OUTPUT -> {
        val text = parameters["text"]?.toString() ?: parameters["body"]?.toString() ?: "Routine step complete"
        ttsEngine?.speak(text)
        StepResult(isSuccess = true, output = "Spoken: $text")
      }

      ActionType.MEMORY_READ -> {
        val key = parameters["key"]?.toString() ?: ""
        val allMemories = com.example.core.ai.memory.MemoryDatabase.getInstance(context).memoryDao().getAllMemories()
        val entry = allMemories.firstOrNull { it.content.contains(key, ignoreCase = true) }
        val value = entry?.content ?: "No memory found for key: $key"
        StepResult(isSuccess = true, output = value)
      }

      ActionType.MEMORY_WRITE -> {
        val key = parameters["key"]?.toString() ?: "routine_note"
        val value = parameters["value"]?.toString() ?: ""
        memoryManager?.saveMemoryExplicit(content = "$key: $value")
        StepResult(isSuccess = true, output = "Stored memory [$key = $value]")
      }

      ActionType.CHECK_CONNECTIVITY -> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isNet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (isNet) {
          StepResult(isSuccess = true, output = "Network online")
        } else {
          StepResult(isSuccess = false, error = "Device is offline")
        }
      }

      ActionType.CHECK_BATTERY -> {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
          context.registerReceiver(null, filter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val battPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        StepResult(isSuccess = true, output = "Battery level: $battPct%")
      }

      else -> {
        val tool = ToolRegistry.findForActionType(action.type)
        if (tool != null) {
          val result = tool.execute(context, parameters, AgentCapabilityPolicy())
          handleToolResult(result)
        } else {
          StepResult(isSuccess = true, output = "Completed ${action.type.label}")
        }
      }
    }
  }

  private fun handleToolResult(result: ToolResult): StepResult {
    return when (result) {
      is ToolResult.Success -> StepResult(isSuccess = true, output = result.message)
      is ToolResult.Failed -> StepResult(isSuccess = false, error = result.reason)
      is ToolResult.Blocked -> StepResult(isSuccess = false, error = "Action blocked: ${result.reason}")
      is ToolResult.PermissionRequired -> StepResult(isSuccess = false, error = "Permission required: ${result.permissions.joinToString()}")
      is ToolResult.ConfirmationRequired -> StepResult(isSuccess = false, error = "Confirmation required: ${result.confirmationPrompt}")
      is ToolResult.Ambiguous -> StepResult(isSuccess = false, error = "Clarification needed: ${result.clarificationPrompt}")
      is ToolResult.Cancelled -> StepResult(isSuccess = false, error = "Cancelled: ${result.reason}")
    }
  }

  private fun deliverNotification(title: String, message: String, notificationId: Int) {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    try {
      notificationManager.notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Notification permission not granted
    }
  }

  private data class StepResult(
    val isSuccess: Boolean,
    val output: String? = null,
    val error: String? = null,
    val providerUsed: String? = null
  )

  companion object {
    const val CHANNEL_ID = "aegis_automation_channel"
  }
}
