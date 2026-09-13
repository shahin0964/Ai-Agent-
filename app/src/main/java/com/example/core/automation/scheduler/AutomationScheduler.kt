package com.example.core.automation.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.model.RecurrenceType
import java.util.Calendar
import java.util.TimeZone

/**
 * Patch 6 Automation Scheduler.
 * Manages device AlarmManager scheduling with exact alarm safety checks,
 * boot recovery, timezone adaptation, and deduplication protection.
 */
class AutomationScheduler(private val context: Context) {

  private val alarmManager: AlarmManager? =
    context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  fun isExactAlarmPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return alarmManager?.canScheduleExactAlarms() == true
    }
    return true
  }

  fun scheduleAutomation(automation: Automation): Long? {
    if (!automation.enabled) {
      cancelSchedule(automation.id)
      return null
    }

    val trigger = automation.trigger as? AutomationTrigger.TimeSchedule ?: return null
    val nextRunAt = computeNextRunTime(trigger, automation.timezone)

    if (nextRunAt <= System.currentTimeMillis()) {
      return null
    }

    val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
      action = AutomationAlarmReceiver.ACTION_EXECUTE_AUTOMATION
      putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, automation.id)
      putExtra(AutomationAlarmReceiver.EXTRA_SCHEDULED_TIME, nextRunAt)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      automation.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    if (alarmManager != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
          // Fallback to inexact alarm while idle
          alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextRunAt,
            pendingIntent
          )
        } else {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextRunAt,
            pendingIntent
          )
        }
      } else {
        alarmManager.setExact(
          AlarmManager.RTC_WAKEUP,
          nextRunAt,
          pendingIntent
        )
      }
    }

    return nextRunAt
  }

  fun cancelSchedule(automationId: String) {
    val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
      action = AutomationAlarmReceiver.ACTION_EXECUTE_AUTOMATION
      putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, automationId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      automationId.hashCode(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null && alarmManager != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun reconcileAllSchedules(automations: List<Automation>) {
    for (auto in automations) {
      if (auto.enabled && auto.trigger is AutomationTrigger.TimeSchedule) {
        scheduleAutomation(auto)
      } else {
        cancelSchedule(auto.id)
      }
    }
  }

  fun computeNextRunTime(trigger: AutomationTrigger.TimeSchedule, tzId: String): Long {
    val tz = try { TimeZone.getTimeZone(tzId) } catch (_: Exception) { TimeZone.getDefault() }
    val now = Calendar.getInstance(tz)

    return when (trigger.recurrence) {
      RecurrenceType.ONE_TIME -> {
        val target = Calendar.getInstance(tz).apply {
          set(Calendar.HOUR_OF_DAY, trigger.timeHour)
          set(Calendar.MINUTE, trigger.timeMinute)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
          target.add(Calendar.DAY_OF_YEAR, 1)
        }
        target.timeInMillis
      }
      RecurrenceType.DAILY -> {
        val target = Calendar.getInstance(tz).apply {
          set(Calendar.HOUR_OF_DAY, trigger.timeHour)
          set(Calendar.MINUTE, trigger.timeMinute)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
          target.add(Calendar.DAY_OF_YEAR, 1)
        }
        target.timeInMillis
      }
      RecurrenceType.WEEKLY -> {
        val allowedDays = if (trigger.daysOfWeek.isNotEmpty()) trigger.daysOfWeek else listOf(now.get(Calendar.DAY_OF_WEEK))
        var closestTarget: Calendar? = null
        for (day in allowedDays) {
          val target = Calendar.getInstance(tz).apply {
            set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, trigger.timeHour)
            set(Calendar.MINUTE, trigger.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }
          if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
          }
          if (closestTarget == null || target.timeInMillis < closestTarget.timeInMillis) {
            closestTarget = target
          }
        }
        closestTarget?.timeInMillis ?: (now.timeInMillis + 86400000L)
      }
      RecurrenceType.MONTHLY -> {
        val target = Calendar.getInstance(tz).apply {
          set(Calendar.DAY_OF_MONTH, trigger.dayOfMonth ?: 1)
          set(Calendar.HOUR_OF_DAY, trigger.timeHour)
          set(Calendar.MINUTE, trigger.timeMinute)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
          target.add(Calendar.MONTH, 1)
        }
        target.timeInMillis
      }
      RecurrenceType.INTERVAL_MINUTES -> {
        val minutes = trigger.intervalMinutes ?: 30
        now.timeInMillis + (minutes * 60 * 1000L)
      }
      RecurrenceType.CUSTOM -> {
        val target = Calendar.getInstance(tz).apply {
          set(Calendar.HOUR_OF_DAY, trigger.timeHour)
          set(Calendar.MINUTE, trigger.timeMinute)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
          target.add(Calendar.DAY_OF_YEAR, 1)
        }
        target.timeInMillis
      }
    }
  }
}
