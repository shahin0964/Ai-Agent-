package com.example.core.automation.interpreter

import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationCategory
import com.example.core.automation.model.AutomationConfirmationPolicy
import com.example.core.automation.model.AutomationNotificationPolicy
import com.example.core.automation.model.AutomationState
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.model.ConfirmationPolicyType
import com.example.core.automation.model.ExecutionPolicy
import com.example.core.automation.model.RecurrenceType
import java.util.Calendar
import java.util.UUID
import java.util.regex.Pattern

sealed class InterpreterResult {
  data class DraftCreated(
    val automation: Automation,
    val explanation: String
  ) : InterpreterResult()

  data class ManageCommand(
    val commandType: ManageCommandType,
    val targetNameOrId: String? = null,
    val responseText: String
  ) : InterpreterResult()

  data class ClarificationNeeded(
    val prompt: String
  ) : InterpreterResult()

  object NotAnAutomationCommand : InterpreterResult()
}

enum class ManageCommandType {
  LIST_ALL,
  ENABLE,
  DISABLE,
  DELETE,
  RUN_NOW
}

/**
 * Patch 6 Natural Language Automation Interpreter.
 * Parses user phrases into structured DRAFT automations or management actions.
 * Drafts always require explicit user review before activation.
 */
class AutomationInterpreter {

  fun interpret(query: String): InterpreterResult {
    val trimmed = query.trim()
    val lower = trimmed.lowercase()

    // 1. Check management commands
    if (lower.startsWith("show automations") || lower.startsWith("list automations") ||
      lower.startsWith("show routines") || lower.startsWith("list routines") ||
      lower.startsWith("list all routines") || lower.startsWith("show all routines") ||
      lower.startsWith("list all automations") || lower.startsWith("show all automations") ||
      lower == "my automations" || lower == "my routines"
    ) {
      return InterpreterResult.ManageCommand(
        commandType = ManageCommandType.LIST_ALL,
        responseText = "Opening your Aegis Automation & Routine Hub."
      )
    }

    if (lower.startsWith("pause ") || lower.startsWith("disable ")) {
      val target = trimmed.substringAfter(" ").trim()
      return InterpreterResult.ManageCommand(
        commandType = ManageCommandType.DISABLE,
        targetNameOrId = target,
        responseText = "Disabling automation \"$target\"."
      )
    }

    if (lower.startsWith("resume ") || lower.startsWith("enable ")) {
      val target = trimmed.substringAfter(" ").trim()
      return InterpreterResult.ManageCommand(
        commandType = ManageCommandType.ENABLE,
        targetNameOrId = target,
        responseText = "Enabling automation \"$target\"."
      )
    }

    if (lower.startsWith("run automation ") || lower.startsWith("trigger routine ") || lower.startsWith("execute routine ")) {
      val target = trimmed.substringAfterLast(" ").trim()
      return InterpreterResult.ManageCommand(
        commandType = ManageCommandType.RUN_NOW,
        targetNameOrId = target,
        responseText = "Executing routine \"$target\" now."
      )
    }

    if (lower.startsWith("delete automation ") || lower.startsWith("remove routine ")) {
      val target = trimmed.substringAfterLast(" ").trim()
      return InterpreterResult.ManageCommand(
        commandType = ManageCommandType.DELETE,
        targetNameOrId = target,
        responseText = "Deleting automation \"$target\"."
      )
    }

    // 2. Check for automation creation triggers (time schedule, recurring, battery, reminders)
    val isScheduleQuery = lower.contains("every morning") || lower.contains("every evening") ||
      lower.contains("every night") || lower.contains("every day") || lower.contains("daily at") ||
      lower.contains("every monday") || lower.contains("every tuesday") || lower.contains("every wednesday") ||
      lower.contains("every thursday") || lower.contains("every friday") || lower.contains("every saturday") ||
      lower.contains("every sunday") || lower.contains("remind me to") || lower.contains("remind me at") ||
      lower.contains("schedule a routine") || lower.contains("create automation") || lower.contains("when battery") ||
      lower.contains("when connected to wifi")

    if (!isScheduleQuery) {
      return InterpreterResult.NotAnAutomationCommand
    }

    // Parse time
    val (hour, minute, recurrence, days) = extractTimeAndRecurrence(lower)

    // Parse actions
    val actions = extractActions(trimmed, lower)
    val name = generateAutomationName(trimmed, actions, hour, minute)
    val category = categorizeAutomation(lower, actions)

    val trigger: AutomationTrigger = if (lower.contains("when battery")) {
      val thresh = extractNumber(lower, default = 20)
      AutomationTrigger.BatteryEvent(triggerOnLow = true, thresholdPercent = thresh)
    } else if (lower.contains("when connected to wifi") || lower.contains("on wifi connected")) {
      AutomationTrigger.ConnectivityEvent(triggerOnConnected = true, wifiOnly = true)
    } else {
      AutomationTrigger.TimeSchedule(
        timeHour = hour,
        timeMinute = minute,
        recurrence = recurrence,
        daysOfWeek = days
      )
    }

    val draft = Automation(
      id = UUID.randomUUID().toString(),
      name = name,
      description = "Generated from: \"$trimmed\"",
      category = category,
      enabled = false,
      state = AutomationState.DRAFT,
      trigger = trigger,
      actions = actions,
      executionPolicy = ExecutionPolicy(networkRequired = actions.any { it.type == ActionType.WEB_RESEARCH || it.type == ActionType.AI_REQUEST }),
      notificationPolicy = AutomationNotificationPolicy(notificationsEnabled = true),
      confirmationPolicy = AutomationConfirmationPolicy(
        policyType = if (actions.any { it.type.isSensitive }) ConfirmationPolicyType.ALWAYS_CONFIRM else ConfirmationPolicyType.NO_CONFIRMATION_FOR_SAFE_ACTIONS
      )
    )

    val explanation = "I've drafted a new automation **${draft.name}** triggered by ${draft.trigger.description}. It contains ${draft.actions.size} action step(s). You can review and enable it in the Automation Hub."

    return InterpreterResult.DraftCreated(draft, explanation)
  }

  private fun extractTimeAndRecurrence(lower: String): TimeRecurrenceResult {
    var hour = 8
    var minute = 0
    var recurrence = RecurrenceType.DAILY
    val days = mutableListOf<Int>()

    // Check specific days
    if (lower.contains("monday")) days.add(Calendar.MONDAY)
    if (lower.contains("tuesday")) days.add(Calendar.TUESDAY)
    if (lower.contains("wednesday")) days.add(Calendar.WEDNESDAY)
    if (lower.contains("thursday")) days.add(Calendar.THURSDAY)
    if (lower.contains("friday")) days.add(Calendar.FRIDAY)
    if (lower.contains("saturday")) days.add(Calendar.SATURDAY)
    if (lower.contains("sunday")) days.add(Calendar.SUNDAY)

    if (days.isNotEmpty()) {
      recurrence = RecurrenceType.WEEKLY
    } else if (lower.contains("tomorrow") || lower.contains("once at")) {
      recurrence = RecurrenceType.ONE_TIME
    }

    // Look for explicit time formats like "8:30 am", "8 am", "10 pm", "22:00"
    val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?", Pattern.CASE_INSENSITIVE)
    val matcher = timePattern.matcher(lower)
    while (matcher.find()) {
      val rawHour = matcher.group(1)?.toIntOrNull() ?: continue
      val rawMin = matcher.group(2)?.toIntOrNull() ?: 0
      val ampm = matcher.group(3)?.lowercase()

      if (rawHour in 1..24) {
        var h = rawHour
        if (ampm != null) {
          if (ampm.startsWith("p") && h < 12) h += 12
          if (ampm.startsWith("a") && h == 12) h = 0
        } else if (h <= 12 && (lower.contains("night") || lower.contains("evening")) && h < 12) {
          h += 12
        }
        hour = h
        minute = rawMin
        break
      }
    }

    if (lower.contains("morning") && !lower.contains("am") && !lower.contains("pm")) {
      hour = 8
      minute = 0
    } else if (lower.contains("night") && !lower.contains("am") && !lower.contains("pm")) {
      hour = 22
      minute = 0
    }

    return TimeRecurrenceResult(hour, minute, recurrence, days)
  }

  private fun extractActions(original: String, lower: String): List<AgentAction> {
    val actions = mutableListOf<AgentAction>()

    if (lower.contains("weather")) {
      actions.add(
        AgentAction(
          type = ActionType.WEB_RESEARCH,
          description = "Research today's weather forecast",
          parameters = mapOf("query" to "current local weather forecast", "depth" to "fast")
        )
      )
      actions.add(
        AgentAction(
          type = ActionType.SEND_NOTIFICATION,
          description = "Deliver weather briefing notification",
          parameters = mapOf("title" to "Daily Weather Briefing", "body" to "\$researchResult")
        )
      )
    } else if (lower.contains("news") || lower.contains("briefing")) {
      actions.add(
        AgentAction(
          type = ActionType.WEB_RESEARCH,
          description = "Research latest breaking news",
          parameters = mapOf("query" to "top global and tech news today", "depth" to "standard")
        )
      )
      actions.add(
        AgentAction(
          type = ActionType.AI_REQUEST,
          description = "Summarize top news into bullet points",
          parameters = mapOf("prompt" to "Provide a 3-bullet morning briefing based on: \$researchResult")
        )
      )
      actions.add(
        AgentAction(
          type = ActionType.SEND_NOTIFICATION,
          description = "Deliver news briefing notification",
          parameters = mapOf("title" to "Morning Intelligence Briefing", "body" to "\$previousResult")
        )
      )
    } else if (lower.contains("sleep") || lower.contains("bedtime") || lower.contains("wind down")) {
      actions.add(
        AgentAction(
          type = ActionType.SEND_NOTIFICATION,
          description = "Send bedtime sleep reminder",
          parameters = mapOf("title" to "Rest & Sleep Reminder", "body" to "Time to wind down and prepare for sleep. Goodnight!")
        )
      )
    } else if (lower.contains("call ") && !lower.contains("reminder")) {
      val contactName = original.substringAfter("call ").trim().split(" ").firstOrNull() ?: "Contact"
      actions.add(
        AgentAction(
          type = ActionType.PHONE_CALL,
          description = "Call $contactName",
          requiresConfirmation = true,
          parameters = mapOf("contact" to contactName)
        )
      )
    } else {
      // General reminder or text notification
      val reminderText = extractReminderText(original)
      actions.add(
        AgentAction(
          type = ActionType.SEND_NOTIFICATION,
          description = "Deliver scheduled reminder",
          parameters = mapOf("title" to "Scheduled Reminder", "body" to reminderText)
        )
      )
    }

    return actions
  }

  private fun extractReminderText(original: String): String {
    val lower = original.lowercase()
    return if (lower.contains("remind me to ")) {
      original.substringAfter("remind me to ").trim()
    } else if (lower.contains("remind me that ")) {
      original.substringAfter("remind me that ").trim()
    } else {
      original
    }
  }

  private fun generateAutomationName(
    original: String,
    actions: List<AgentAction>,
    hour: Int,
    minute: Int
  ): String {
    val firstAction = actions.firstOrNull()
    return when {
      original.lowercase().contains("weather") -> "Daily Weather Briefing"
      original.lowercase().contains("news") -> "Intelligence News Briefing"
      original.lowercase().contains("sleep") -> "Bedtime Sleep Reminder"
      firstAction?.type == ActionType.PHONE_CALL -> "Scheduled Call Routine"
      else -> "Scheduled Routine [%02d:%02d]".format(hour, minute)
    }
  }

  private fun categorizeAutomation(lower: String, actions: List<AgentAction>): AutomationCategory {
    return when {
      lower.contains("news") || lower.contains("research") -> AutomationCategory.RESEARCH
      lower.contains("sleep") || lower.contains("companion") -> AutomationCategory.COMPANION
      lower.contains("battery") || lower.contains("wifi") || lower.contains("device") -> AutomationCategory.DEVICE
      lower.contains("remind") -> AutomationCategory.REMINDER
      actions.any { it.type == ActionType.AI_REQUEST } -> AutomationCategory.AI
      actions.any { it.type == ActionType.PHONE_CALL || it.type == ActionType.SEND_SMS } -> AutomationCategory.PERSONAL
      else -> AutomationCategory.PERSONAL
    }
  }

  private fun extractNumber(str: String, default: Int): Int {
    val matcher = Pattern.compile("\\b(\\d{1,3})\\b").matcher(str)
    return if (matcher.find()) {
      matcher.group(1)?.toIntOrNull() ?: default
    } else default
  }

  private data class TimeRecurrenceResult(
    val hour: Int,
    val minute: Int,
    val recurrence: RecurrenceType,
    val days: List<Int>
  )
}
