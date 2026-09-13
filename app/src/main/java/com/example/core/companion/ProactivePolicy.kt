package com.example.core.companion

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Granular user-controlled policies for proactive companion behavior.
 * Section 10 & 11: Proactive Companion is OFF by default.
 * Each behavior requires its own explicit permission toggle.
 */
data class ProactivePolicy(
  val isProactiveCompanionEnabled: Boolean = false, // Master toggle: OFF by default
  val proactiveNotifications: Boolean = false,
  val proactiveGreetings: Boolean = false,
  val reminderMessages: Boolean = false,
  val idleCheckIns: Boolean = false,
  val sleepModeMessages: Boolean = false,
  val dailyBriefing: Boolean = false,
  val researchUpdates: Boolean = false,
  val silentNotifications: Boolean = false
)

class ProactivePolicyManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_proactive_policy_prefs", Context.MODE_PRIVATE)

  private val _policy = MutableStateFlow(loadPolicy())
  val policy: StateFlow<ProactivePolicy> = _policy.asStateFlow()

  private fun loadPolicy(): ProactivePolicy {
    return ProactivePolicy(
      isProactiveCompanionEnabled = prefs.getBoolean("master_proactive_enabled", false),
      proactiveNotifications = prefs.getBoolean("proactive_notifications", false),
      proactiveGreetings = prefs.getBoolean("proactive_greetings", false),
      reminderMessages = prefs.getBoolean("reminder_messages", false),
      idleCheckIns = prefs.getBoolean("idle_checkins", false),
      sleepModeMessages = prefs.getBoolean("sleep_messages", false),
      dailyBriefing = prefs.getBoolean("daily_briefing", false),
      researchUpdates = prefs.getBoolean("research_updates", false),
      silentNotifications = prefs.getBoolean("silent_notifications", false)
    )
  }

  fun updatePolicy(newPolicy: ProactivePolicy) {
    prefs.edit()
      .putBoolean("master_proactive_enabled", newPolicy.isProactiveCompanionEnabled)
      .putBoolean("proactive_notifications", newPolicy.proactiveNotifications)
      .putBoolean("proactive_greetings", newPolicy.proactiveGreetings)
      .putBoolean("reminder_messages", newPolicy.reminderMessages)
      .putBoolean("idle_checkins", newPolicy.idleCheckIns)
      .putBoolean("sleep_messages", newPolicy.sleepModeMessages)
      .putBoolean("daily_briefing", newPolicy.dailyBriefing)
      .putBoolean("research_updates", newPolicy.researchUpdates)
      .putBoolean("silent_notifications", newPolicy.silentNotifications)
      .apply()

    _policy.value = newPolicy
  }

  fun setMasterEnabled(enabled: Boolean) {
    updatePolicy(_policy.value.copy(isProactiveCompanionEnabled = enabled))
  }

  fun isBehaviorAllowed(behaviorCheck: (ProactivePolicy) -> Boolean): Boolean {
    val current = _policy.value
    if (!current.isProactiveCompanionEnabled) return false
    return behaviorCheck(current)
  }
}
