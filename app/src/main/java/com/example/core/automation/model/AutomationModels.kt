package com.example.core.automation.model

import com.example.core.action.AgentAction
import java.util.UUID

/**
 * Patch 6: Automation & Routine Domain Models
 */

enum class AutomationCategory(val displayName: String, val tag: String) {
  PERSONAL("Personal", "PERS"),
  WORK("Work", "WORK"),
  RESEARCH("Research", "RSRCH"),
  COMPANION("Companion", "COMP"),
  DEVICE("Device", "DEV"),
  REMINDER("Reminder", "REM"),
  NEWS("News & Briefings", "NEWS"),
  AI("AI Intelligence", "AI"),
  VOICE("Voice", "VOX"),
  CREATOR("Creator", "CREAT")
}

enum class AutomationState(val label: String, val isExecutable: Boolean) {
  DRAFT("Draft", false),
  ENABLED("Enabled", true),
  DISABLED("Disabled", false),
  RUNNING("Running", false),
  PAUSED("Paused", false),
  COMPLETED("Completed", false),
  FAILED("Failed", false),
  CANCELLED("Cancelled", false),
  WAITING_PERMISSION("Waiting Permission", false),
  WAITING_CONFIRMATION("Waiting Confirmation", false),
  NEEDS_REVIEW("Needs Review", false)
}

enum class ConditionLogic {
  AND,
  OR,
  NOT
}

enum class StepDependency {
  ON_SUCCESS,
  ON_FAILURE,
  ALWAYS,
  CONDITIONAL
}

enum class RetryPolicy(val displayName: String, val maxRetries: Int) {
  NO_RETRY("No Retry", 0),
  RETRY_ONCE("Retry Once", 1),
  RETRY_3_TIMES("Retry 3 Times", 3),
  EXPONENTIAL_BACKOFF("Exponential Backoff", 3),
  USER_DEFINED("Custom Policy", 2)
}

enum class ConcurrencyPolicy {
  SKIP,
  QUEUE,
  CANCEL_PREVIOUS,
  ALLOW_PARALLEL
}

enum class MissedExecutionPolicy {
  SKIP_MISSED,
  RUN_ON_NEXT_AVAILABLE,
  RUN_ONCE_AFTER_MISSED
}

enum class ConfirmationPolicyType(val displayName: String) {
  ALWAYS_CONFIRM("Always Confirm"),
  CONFIRM_ON_FIRST_RUN("Confirm on First Run"),
  NO_CONFIRMATION_FOR_SAFE_ACTIONS("No Confirm for Safe Actions"),
  USER_DEFINED("User Defined")
}

data class AutomationConfirmationPolicy(
  val policyType: ConfirmationPolicyType = ConfirmationPolicyType.NO_CONFIRMATION_FOR_SAFE_ACTIONS,
  val hasConfirmedFirstRun: Boolean = false
)

data class AutomationNotificationPolicy(
  val notificationsEnabled: Boolean = true,
  val notifyOnSuccess: Boolean = true,
  val notifyOnFailure: Boolean = true,
  val showSummaryNotification: Boolean = true,
  val silentDuringQuietHours: Boolean = true
)

data class ExecutionPolicy(
  val timeoutSeconds: Long = 120,
  val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,
  val concurrencyPolicy: ConcurrencyPolicy = ConcurrencyPolicy.SKIP,
  val missedExecutionPolicy: MissedExecutionPolicy = MissedExecutionPolicy.SKIP_MISSED,
  val networkRequired: Boolean = false,
  val wifiOnly: Boolean = false,
  val chargingRequired: Boolean = false,
  val minBatteryLevel: Int = 0,
  val allowBackgroundAi: Boolean = true
)

/**
 * Triggers supported in Patch 6
 */
sealed class AutomationTrigger {
  abstract val triggerType: String
  abstract val description: String
  abstract val isAvailable: Boolean

  data class TimeSchedule(
    val timeHour: Int,
    val timeMinute: Int,
    val recurrence: RecurrenceType = RecurrenceType.DAILY,
    val daysOfWeek: List<Int> = emptyList(), // 1=Sunday..7=Saturday (Calendar)
    val dayOfMonth: Int? = null,
    val specificTimestamp: Long? = null,
    val intervalMinutes: Int? = null
  ) : AutomationTrigger() {
    override val triggerType: String = "TIME_SCHEDULE"
    override val description: String
      get() = when (recurrence) {
        RecurrenceType.ONE_TIME -> "One-time at %02d:%02d".format(timeHour, timeMinute)
        RecurrenceType.DAILY -> "Every day at %02d:%02d".format(timeHour, timeMinute)
        RecurrenceType.WEEKLY -> "Every week on selected days at %02d:%02d".format(timeHour, timeMinute)
        RecurrenceType.MONTHLY -> "Monthly on day ${dayOfMonth ?: 1} at %02d:%02d".format(timeHour, timeMinute)
        RecurrenceType.INTERVAL_MINUTES -> "Every ${intervalMinutes ?: 30} minutes"
        RecurrenceType.CUSTOM -> "Custom time pattern at %02d:%02d".format(timeHour, timeMinute)
      }
    override val isAvailable: Boolean = true
  }

  data class BatteryEvent(
    val triggerOnLow: Boolean = false,
    val thresholdPercent: Int = 20,
    val triggerOnCharging: Boolean = false
  ) : AutomationTrigger() {
    override val triggerType: String = "BATTERY_EVENT"
    override val description: String
      get() = if (triggerOnCharging) "When device starts charging" else "When battery level drops to ${thresholdPercent}%"
    override val isAvailable: Boolean = true
  }

  data class ConnectivityEvent(
    val triggerOnConnected: Boolean = true,
    val wifiOnly: Boolean = false
  ) : AutomationTrigger() {
    override val triggerType: String = "CONNECTIVITY_EVENT"
    override val description: String
      get() = if (wifiOnly) "When connected to Wi-Fi" else if (triggerOnConnected) "When network connects" else "When network disconnects"
    override val isAvailable: Boolean = true
  }

  data class DeviceEvent(
    val eventType: String
  ) : AutomationTrigger() {
    override val triggerType: String = "DEVICE_EVENT"
    override val description: String = "On Device Event: $eventType"
    override val isAvailable: Boolean = true
  }

  data class NotificationEvent(
    val appPackage: String? = null,
    val keyword: String? = null
  ) : AutomationTrigger() {
    override val triggerType: String = "NOTIFICATION_EVENT"
    override val description: String = "On Notification${keyword?.let { " containing \"$it\"" } ?: ""}"
    override val isAvailable: Boolean = true
  }

  data class LocationEvent(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f,
    val isEntering: Boolean = true
  ) : AutomationTrigger() {
    override val triggerType: String = "LOCATION_EVENT"
    override val description: String = "When ${if (isEntering) "entering" else "exiting"} target geofence"
    override val isAvailable: Boolean = false // Flagged unavailable until background location permission is granted
  }

  data class CalendarEvent(
    val keyword: String? = null,
    val minutesBefore: Int = 15
  ) : AutomationTrigger() {
    override val triggerType: String = "CALENDAR_EVENT"
    override val description: String = "$minutesBefore min before calendar event${keyword?.let { " \"$it\"" } ?: ""}"
    override val isAvailable: Boolean = true
  }

  data class VoiceCommandTrigger(
    val phrasePattern: String
  ) : AutomationTrigger() {
    override val triggerType: String = "VOICE_COMMAND"
    override val description: String = "Voice Command: \"$phrasePattern\""
    override val isAvailable: Boolean = true
  }

  object ManualTrigger : AutomationTrigger() {
    override val triggerType: String = "MANUAL"
    override val description: String = "Manual Execution"
    override val isAvailable: Boolean = true
  }

  data class AppEvent(
    val eventName: String
  ) : AutomationTrigger() {
    override val triggerType: String = "APP_EVENT"
    override val description: String = "App Lifecycle: $eventName"
    override val isAvailable: Boolean = true
  }

  data class SystemEvent(
    val eventName: String
  ) : AutomationTrigger() {
    override val triggerType: String = "SYSTEM_EVENT"
    override val description: String = "System Event: $eventName"
    override val isAvailable: Boolean = true
  }
}

enum class RecurrenceType {
  ONE_TIME,
  DAILY,
  WEEKLY,
  MONTHLY,
  INTERVAL_MINUTES,
  CUSTOM
}

/**
 * Conditions supported in Patch 6
 */
sealed class AutomationCondition {
  abstract val conditionType: String
  abstract val description: String

  data class TimeWindow(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
  ) : AutomationCondition() {
    override val conditionType: String = "TIME_WINDOW"
    override val description: String = "Time between %02d:%02d and %02d:%02d".format(startHour, startMinute, endHour, endMinute)
  }

  data class DayOfWeekCondition(
    val days: List<Int> // 1=Sunday..7=Saturday
  ) : AutomationCondition() {
    override val conditionType: String = "DAY_OF_WEEK"
    override val description: String = "Only on selected days of the week"
  }

  data class BatteryCondition(
    val minLevel: Int = 20,
    val requireCharging: Boolean = false
  ) : AutomationCondition() {
    override val conditionType: String = "BATTERY"
    override val description: String = if (requireCharging) "Device must be charging" else "Battery level > ${minLevel}%"
  }

  data class NetworkCondition(
    val requirement: NetworkRequirementType
  ) : AutomationCondition() {
    override val conditionType: String = "NETWORK"
    override val description: String = when (requirement) {
      NetworkRequirementType.ANY_NETWORK -> "Internet connection required"
      NetworkRequirementType.WIFI_ONLY -> "Wi-Fi connection required"
      NetworkRequirementType.NO_NETWORK_REQUIRED -> "No network required"
    }
  }

  data class BluetoothCondition(
    val requireConnected: Boolean = true
  ) : AutomationCondition() {
    override val conditionType: String = "BLUETOOTH"
    override val description: String = if (requireConnected) "Bluetooth device connected" else "Bluetooth disconnected"
  }

  data class MemoryKeyCondition(
    val key: String,
    val expectedValue: String? = null
  ) : AutomationCondition() {
    override val conditionType: String = "MEMORY_KEY"
    override val description: String = "Memory contains key \"$key\""
  }

  data class AiProviderCondition(
    val requireOnline: Boolean = true
  ) : AutomationCondition() {
    override val conditionType: String = "AI_PROVIDER"
    override val description: String = if (requireOnline) "Online AI provider available" else "AI capability available"
  }
}

enum class NetworkRequirementType {
  ANY_NETWORK,
  WIFI_ONLY,
  NO_NETWORK_REQUIRED
}

/**
 * Main Automation Entity
 */
data class Automation(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val description: String = "",
  val category: AutomationCategory = AutomationCategory.PERSONAL,
  val enabled: Boolean = false,
  val state: AutomationState = AutomationState.DRAFT,
  val trigger: AutomationTrigger = AutomationTrigger.ManualTrigger,
  val conditions: List<AutomationCondition> = emptyList(),
  val conditionLogic: ConditionLogic = ConditionLogic.AND,
  val actions: List<AgentAction> = emptyList(),
  val stepDependencies: Map<String, StepDependency> = emptyMap(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val lastRunAt: Long? = null,
  val nextRunAt: Long? = null,
  val runCount: Int = 0,
  val failureCount: Int = 0,
  val executionPolicy: ExecutionPolicy = ExecutionPolicy(),
  val notificationPolicy: AutomationNotificationPolicy = AutomationNotificationPolicy(),
  val confirmationPolicy: AutomationConfirmationPolicy = AutomationConfirmationPolicy(),
  val timezone: String = java.util.TimeZone.getDefault().id
)

/**
 * Execution History & Telemetry Models
 */
enum class AutomationExecutionStatus {
  SUCCESS,
  PARTIAL_SUCCESS,
  FAILED,
  SKIPPED,
  CANCELLED
}

data class StepExecutionRecord(
  val stepIndex: Int,
  val actionTitle: String,
  val actionType: String,
  val timestamp: Long = System.currentTimeMillis(),
  val status: String,
  val details: String? = null
)

data class AutomationHistory(
  val executionId: String = UUID.randomUUID().toString(),
  val automationId: String,
  val automationName: String,
  val triggerType: String,
  val status: AutomationExecutionStatus,
  val startedAt: Long,
  val durationMs: Long,
  val completedSteps: Int,
  val totalSteps: Int,
  val failedStep: String? = null,
  val errorMessage: String? = null,
  val providerUsed: String? = null,
  val stepsLog: List<StepExecutionRecord> = emptyList()
)
