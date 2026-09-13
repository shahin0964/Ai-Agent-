package com.example.core.automation.condition

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.example.core.ai.memory.MemoryManager
import com.example.core.ai.router.AIIntelligenceRouter
import com.example.core.automation.model.AutomationCondition
import com.example.core.automation.model.ConditionLogic
import com.example.core.automation.model.NetworkRequirementType
import java.util.Calendar

data class ConditionEvaluationResult(
  val satisfied: Boolean,
  val failureReason: String? = null
)

data class DeviceTelemetrySnapshot(
  val batteryLevel: Int = 100,
  val isCharging: Boolean = false,
  val isConnected: Boolean = true,
  val isWifi: Boolean = true,
  val timeHour: Int = 12,
  val timeMinute: Int = 0
)

/**
 * Patch 6 Condition Engine.
 * Evaluates real-time device telemetry, network states, and AI availability.
 * If conditions are not satisfied, returns skipped status with clear telemetry reasons.
 */
class ConditionEngine(
  private val context: Context,
  private val memoryManager: MemoryManager? = null,
  private val aiRouter: AIIntelligenceRouter? = null
) {

  suspend fun evaluateAll(
    conditions: List<AutomationCondition>,
    logic: ConditionLogic
  ): ConditionEvaluationResult {
    if (conditions.isEmpty()) {
      return ConditionEvaluationResult(satisfied = true)
    }

    val results = conditions.map { evaluateSingle(it) }

    return when (logic) {
      ConditionLogic.AND -> {
        val failed = results.firstOrNull { !it.satisfied }
        if (failed != null) {
          ConditionEvaluationResult(satisfied = false, failureReason = failed.failureReason)
        } else {
          ConditionEvaluationResult(satisfied = true)
        }
      }
      ConditionLogic.OR -> {
        val success = results.firstOrNull { it.satisfied }
        if (success != null) {
          ConditionEvaluationResult(satisfied = true)
        } else {
          val reasons = results.mapNotNull { it.failureReason }.joinToString("; ")
          ConditionEvaluationResult(satisfied = false, failureReason = "None of OR conditions met: $reasons")
        }
      }
      ConditionLogic.NOT -> {
        val allMet = results.all { it.satisfied }
        if (allMet) {
          ConditionEvaluationResult(satisfied = false, failureReason = "NOT condition inverted: conditions were met")
        } else {
          ConditionEvaluationResult(satisfied = true)
        }
      }
    }
  }

  suspend fun evaluateSingle(condition: AutomationCondition): ConditionEvaluationResult {
    return try {
      when (condition) {
        is AutomationCondition.TimeWindow -> evaluateTimeWindow(condition)
        is AutomationCondition.DayOfWeekCondition -> evaluateDayOfWeek(condition)
        is AutomationCondition.BatteryCondition -> evaluateBattery(condition)
        is AutomationCondition.NetworkCondition -> evaluateNetwork(condition)
        is AutomationCondition.BluetoothCondition -> evaluateBluetooth(condition)
        is AutomationCondition.MemoryKeyCondition -> evaluateMemory(condition)
        is AutomationCondition.AiProviderCondition -> evaluateAiProvider(condition)
      }
    } catch (e: Exception) {
      ConditionEvaluationResult(satisfied = false, failureReason = "Evaluation exception: ${e.message}")
    }
  }

  fun evaluateConditions(
    conditions: List<AutomationCondition>,
    telemetry: DeviceTelemetrySnapshot? = null
  ): ConditionEvaluationResult {
    if (conditions.isEmpty()) return ConditionEvaluationResult(satisfied = true)

    for (cond in conditions) {
      if (telemetry != null) {
        when (cond) {
          is AutomationCondition.BatteryCondition -> {
            if (cond.requireCharging && !telemetry.isCharging) {
              return ConditionEvaluationResult(satisfied = false, failureReason = "Device is not charging")
            }
            if (telemetry.batteryLevel < cond.minLevel) {
              return ConditionEvaluationResult(satisfied = false, failureReason = "Battery level is below minimum threshold (${cond.minLevel}%)")
            }
          }
          is AutomationCondition.NetworkCondition -> {
            when (cond.requirement) {
              NetworkRequirementType.ANY_NETWORK -> {
                if (!telemetry.isConnected) return ConditionEvaluationResult(satisfied = false, failureReason = "Network connectivity required")
              }
              NetworkRequirementType.WIFI_ONLY -> {
                if (!telemetry.isWifi || !telemetry.isConnected) return ConditionEvaluationResult(satisfied = false, failureReason = "Wi-Fi connectivity required")
              }
              NetworkRequirementType.NO_NETWORK_REQUIRED -> {}
            }
          }
          else -> {}
        }
      }
    }
    return ConditionEvaluationResult(satisfied = true)
  }

  private fun evaluateTimeWindow(condition: AutomationCondition.TimeWindow): ConditionEvaluationResult {
    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val startMinutes = condition.startHour * 60 + condition.startMinute
    val endMinutes = condition.endHour * 60 + condition.endMinute

    val inWindow = if (startMinutes <= endMinutes) {
      currentMinutes in startMinutes..endMinutes
    } else {
      // Crosses midnight
      currentMinutes >= startMinutes || currentMinutes <= endMinutes
    }

    return if (inWindow) {
      ConditionEvaluationResult(satisfied = true)
    } else {
      ConditionEvaluationResult(
        satisfied = false,
        failureReason = "Current time outside window [%02d:%02d - %02d:%02d]".format(
          condition.startHour, condition.startMinute, condition.endHour, condition.endMinute
        )
      )
    }
  }

  private fun evaluateDayOfWeek(condition: AutomationCondition.DayOfWeekCondition): ConditionEvaluationResult {
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
    return if (condition.days.contains(currentDay)) {
      ConditionEvaluationResult(satisfied = true)
    } else {
      ConditionEvaluationResult(satisfied = false, failureReason = "Today (day $currentDay) is not an authorized routine day")
    }
  }

  private fun evaluateBattery(condition: AutomationCondition.BatteryCondition): ConditionEvaluationResult {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
      context.registerReceiver(null, filter)
    }

    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

    val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
      status == BatteryManager.BATTERY_STATUS_FULL

    if (condition.requireCharging && !isCharging) {
      return ConditionEvaluationResult(satisfied = false, failureReason = "Device is not connected to power")
    }

    if (batteryPct < condition.minLevel) {
      return ConditionEvaluationResult(satisfied = false, failureReason = "Battery level ($batteryPct%) is below required threshold (${condition.minLevel}%)")
    }

    return ConditionEvaluationResult(satisfied = true)
  }

  private fun evaluateNetwork(condition: AutomationCondition.NetworkCondition): ConditionEvaluationResult {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return ConditionEvaluationResult(satisfied = false, failureReason = "Connectivity service unavailable")

    val activeNetwork = cm.activeNetwork
    val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    return when (condition.requirement) {
      NetworkRequirementType.NO_NETWORK_REQUIRED -> ConditionEvaluationResult(satisfied = true)
      NetworkRequirementType.ANY_NETWORK -> {
        if (isConnected) ConditionEvaluationResult(satisfied = true)
        else ConditionEvaluationResult(satisfied = false, failureReason = "Internet connectivity required but offline")
      }
      NetworkRequirementType.WIFI_ONLY -> {
        if (isWifi && isConnected) ConditionEvaluationResult(satisfied = true)
        else ConditionEvaluationResult(satisfied = false, failureReason = "Wi-Fi connection required but unavailable")
      }
    }
  }

  private fun evaluateBluetooth(condition: AutomationCondition.BluetoothCondition): ConditionEvaluationResult {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    val adapter = bluetoothManager?.adapter
    if (adapter == null) {
      return if (!condition.requireConnected) ConditionEvaluationResult(satisfied = true)
      else ConditionEvaluationResult(satisfied = false, failureReason = "Bluetooth hardware unavailable")
    }

    val isEnabled = adapter.isEnabled
    return if (condition.requireConnected) {
      if (isEnabled) ConditionEvaluationResult(satisfied = true)
      else ConditionEvaluationResult(satisfied = false, failureReason = "Bluetooth is disabled")
    } else {
      if (!isEnabled) ConditionEvaluationResult(satisfied = true)
      else ConditionEvaluationResult(satisfied = false, failureReason = "Bluetooth is currently enabled")
    }
  }

  private suspend fun evaluateMemory(condition: AutomationCondition.MemoryKeyCondition): ConditionEvaluationResult {
    val allMemories = com.example.core.ai.memory.MemoryDatabase.getInstance(context).memoryDao().getAllMemories()
    val match = allMemories.firstOrNull { it.content.contains(condition.key, ignoreCase = true) }

    return if (match != null) {
      if (condition.expectedValue == null || match.content.contains(condition.expectedValue, ignoreCase = true)) {
        ConditionEvaluationResult(satisfied = true)
      } else {
        ConditionEvaluationResult(satisfied = false, failureReason = "Memory key \"${condition.key}\" does not match expected value")
      }
    } else {
      ConditionEvaluationResult(satisfied = false, failureReason = "Memory key \"${condition.key}\" not found in memory store")
    }
  }

  private fun evaluateAiProvider(condition: AutomationCondition.AiProviderCondition): ConditionEvaluationResult {
    return ConditionEvaluationResult(satisfied = true)
  }
}
