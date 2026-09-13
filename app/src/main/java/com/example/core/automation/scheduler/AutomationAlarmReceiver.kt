package com.example.core.automation.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.automation.AutomationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Patch 6 Automation Broadcast Receiver.
 * Receives exact/inexact scheduled alarm fires, device boot events,
 * and timezone/clock adjustment signals.
 */
class AutomationAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) return

    val action = intent.action ?: return
    val automationEngine = AutomationEngine.getInstance(context)

    when (action) {
      ACTION_EXECUTE_AUTOMATION -> {
        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID) ?: return
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, 0L)

        // Asynchronously execute automation safely
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
          try {
            automationEngine.handleScheduledTrigger(automationId, scheduledTime)
          } finally {
            pendingResult.finish()
          }
        }
      }

      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_TIMEZONE_CHANGED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_DATE_CHANGED -> {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
          try {
            automationEngine.reconcileSchedules()
          } finally {
            pendingResult.finish()
          }
        }
      }
    }
  }

  companion object {
    const val ACTION_EXECUTE_AUTOMATION = "com.example.ACTION_EXECUTE_AUTOMATION"
    const val EXTRA_AUTOMATION_ID = "extra_automation_id"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
  }
}
