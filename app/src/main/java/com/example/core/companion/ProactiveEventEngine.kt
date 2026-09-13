package com.example.core.companion

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ProactiveEventType {
  USER_ENABLED_PROACTIVE_MODE,
  USER_RETURNED,
  USER_STARTED_SESSION,
  RESEARCH_COMPLETED,
  AI_PROVIDER_STATUS_CHANGED,
  REMINDER_EVENT,
  SYSTEM_EVENT
}

data class ProactiveEvent(
  val type: ProactiveEventType,
  val payload: Map<String, Any?> = emptyMap(),
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Event-driven engine evaluating legitimate system and operational triggers.
 * Section 38 & 39: Does NOT poll continuously or run arbitrary background loops.
 */
class ProactiveEventEngine(
  private val context: Context,
  private val policyManager: ProactivePolicyManager,
  private val notificationManager: CompanionNotificationManager,
  private val sleepManager: CompanionSleepManager,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
  private var lastEventHandledTimestamps = mutableMapOf<ProactiveEventType, Long>()

  fun onEvent(event: ProactiveEvent, onInAppAction: ((String) -> Unit)? = null) {
    scope.launch {
      evaluateAndDispatch(event, onInAppAction)
    }
  }

  private fun evaluateAndDispatch(event: ProactiveEvent, onInAppAction: ((String) -> Unit)?) {
    val policy = policyManager.policy.value

    // Master proactive toggle check
    if (!policy.isProactiveCompanionEnabled) return

    // Suppress during sleep unless notifications allowed during sleep
    if (sleepManager.isSleeping && !sleepManager.sleepSchedule.value.allowSleepNotifications) {
      return
    }

    val now = System.currentTimeMillis()
    val lastTime = lastEventHandledTimestamps[event.type] ?: 0L

    when (event.type) {
      ProactiveEventType.USER_ENABLED_PROACTIVE_MODE -> {
        // Confirmation notice
        notificationManager.postNotification(
          category = CompanionNotificationCategory.AGENT_STATUS,
          title = "Proactive Companion Active",
          message = "Configured proactive behaviors are now active according to your settings."
        )
      }

      ProactiveEventType.USER_STARTED_SESSION, ProactiveEventType.USER_RETURNED -> {
        if (!policy.proactiveGreetings) return
        if (now - lastTime < 1_800_000L) return // Max once per 30 minutes

        lastEventHandledTimestamps[event.type] = now
        val greeting = generateContextualGreeting()
        onInAppAction?.invoke(greeting)
      }

      ProactiveEventType.RESEARCH_COMPLETED -> {
        if (!policy.researchUpdates) return
        val title = event.payload["title"] as? String ?: "Research Synthesis"
        val query = event.payload["query"] as? String ?: ""

        notificationManager.postNotification(
          category = CompanionNotificationCategory.RESEARCH,
          title = "Research Complete: $title",
          message = "Grounded analysis ready for: \"${query.take(40)}\""
        )
      }

      ProactiveEventType.AI_PROVIDER_STATUS_CHANGED -> {
        val provider = event.payload["provider"] as? String ?: "AI Provider"
        val status = event.payload["status"] as? String ?: "Status Changed"

        notificationManager.postNotification(
          category = CompanionNotificationCategory.AGENT_STATUS,
          title = "Provider Notice: $provider",
          message = status
        )
      }

      ProactiveEventType.REMINDER_EVENT -> {
        if (!policy.reminderMessages) return
        val reminderText = event.payload["text"] as? String ?: "Scheduled Reminder"

        notificationManager.postNotification(
          category = CompanionNotificationCategory.REMINDER,
          title = "Agent Reminder",
          message = reminderText
        )
      }

      ProactiveEventType.SYSTEM_EVENT -> {
        val msg = event.payload["message"] as? String ?: return
        notificationManager.postNotification(
          category = CompanionNotificationCategory.SYSTEM,
          title = "System Advisory",
          message = msg
        )
      }
    }
  }

  private fun generateContextualGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
      hour < 12 -> "Good morning. Ready when you are."
      hour < 17 -> "Good afternoon. How can I assist you?"
      else -> "Good evening. Core systems standing by."
    }
  }
}
