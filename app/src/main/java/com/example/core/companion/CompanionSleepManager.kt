package com.example.core.companion

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class SleepState(val label: String, val description: String) {
  AWAKE("Awake", "Full active standby; all authorized features responsive."),
  RESTING("Resting", "Transitioning into quiet schedule."),
  SLEEPING("Sleeping", "Scheduled quiet mode; proactive alerts and voice capture suppressed."),
  WAKING("Waking", "Transitioning out of sleep; ready for morning briefing if authorized.")
}

data class SleepSchedule(
  val isScheduleEnabled: Boolean = false,
  val sleepHour: Int = 23,
  val sleepMinute: Int = 0,
  val wakeHour: Int = 7,
  val wakeMinute: Int = 0,
  val allowSleepNotifications: Boolean = false,
  val wakeGreetingEnabled: Boolean = true,
  val dndIntegration: Boolean = false
)

class CompanionSleepManager(
  private val context: Context,
  private val onSleepStateChanged: (Boolean) -> Unit = {}
) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_companion_sleep_prefs", Context.MODE_PRIVATE)

  private val _sleepSchedule = MutableStateFlow(loadSchedule())
  val sleepSchedule: StateFlow<SleepSchedule> = _sleepSchedule.asStateFlow()

  private val _sleepState = MutableStateFlow(computeCurrentSleepState())
  val sleepState: StateFlow<SleepState> = _sleepState.asStateFlow()

  val isSleeping: Boolean
    get() = _sleepState.value == SleepState.SLEEPING || _sleepState.value == SleepState.RESTING

  private fun loadSchedule(): SleepSchedule {
    return SleepSchedule(
      isScheduleEnabled = prefs.getBoolean("sched_enabled", false),
      sleepHour = prefs.getInt("sleep_hour", 23),
      sleepMinute = prefs.getInt("sleep_min", 0),
      wakeHour = prefs.getInt("wake_hour", 7),
      wakeMinute = prefs.getInt("wake_min", 0),
      allowSleepNotifications = prefs.getBoolean("allow_notifications", false),
      wakeGreetingEnabled = prefs.getBoolean("wake_greeting", true),
      dndIntegration = prefs.getBoolean("dnd_integration", false)
    )
  }

  fun updateSchedule(schedule: SleepSchedule) {
    prefs.edit()
      .putBoolean("sched_enabled", schedule.isScheduleEnabled)
      .putInt("sleep_hour", schedule.sleepHour)
      .putInt("sleep_min", schedule.sleepMinute)
      .putInt("wake_hour", schedule.wakeHour)
      .putInt("wake_min", schedule.wakeMinute)
      .putBoolean("allow_notifications", schedule.allowSleepNotifications)
      .putBoolean("wake_greeting", schedule.wakeGreetingEnabled)
      .putBoolean("dnd_integration", schedule.dndIntegration)
      .apply()

    _sleepSchedule.value = schedule
    val newState = computeCurrentSleepState()
    _sleepState.value = newState
    onSleepStateChanged(isSleeping)
  }

  fun manualSetSleep(sleeping: Boolean) {
    val newState = if (sleeping) SleepState.SLEEPING else SleepState.AWAKE
    _sleepState.value = newState
    onSleepStateChanged(sleeping)
  }

  fun checkAndUpdateState(): SleepState {
    val newState = computeCurrentSleepState()
    if (_sleepState.value != newState) {
      _sleepState.value = newState
      onSleepStateChanged(isSleeping)
    }
    return newState
  }

  private fun computeCurrentSleepState(): SleepState {
    val schedule = _sleepSchedule.value
    if (!schedule.isScheduleEnabled) {
      // Check DND if enabled
      if (schedule.dndIntegration && isSystemInDnd()) {
        return SleepState.SLEEPING
      }
      return SleepState.AWAKE
    }

    val cal = Calendar.getInstance()
    val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val sleepMinuteOfDay = schedule.sleepHour * 60 + schedule.sleepMinute
    val wakeMinuteOfDay = schedule.wakeHour * 60 + schedule.wakeMinute

    val inSleepWindow = if (sleepMinuteOfDay > wakeMinuteOfDay) {
      // Crosses midnight (e.g. 23:00 to 07:00)
      currentMinuteOfDay >= sleepMinuteOfDay || currentMinuteOfDay < wakeMinuteOfDay
    } else {
      currentMinuteOfDay in sleepMinuteOfDay until wakeMinuteOfDay
    }

    return if (inSleepWindow) SleepState.SLEEPING else SleepState.AWAKE
  }

  fun isSystemInDnd(): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      nm?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    } else {
      false
    }
  }
}
