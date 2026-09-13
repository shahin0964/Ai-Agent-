package com.example.core.action

import com.example.core.capability.AgentCapabilityType
import java.util.UUID

/**
 * Lifecycle execution statuses for Agent Actions.
 */
enum class ActionStatus(val label: String) {
  PLANNED("Planned"),
  WAITING_FOR_PERMISSION("Waiting for Permission"),
  WAITING_FOR_CONFIRMATION("Waiting for Confirmation"),
  EXECUTING("Executing"),
  SUCCESS("Success"),
  FAILED("Failed"),
  CANCELLED("Cancelled")
}

/**
 * Standardized Android Action Types supported by the Agent.
 */
enum class ActionType(
  val label: String,
  val isSensitive: Boolean,
  val isStateAltering: Boolean
) {
  APP_LAUNCH("Launch App", isSensitive = false, isStateAltering = false),
  PHONE_CALL("Voice Call", isSensitive = true, isStateAltering = true),
  SEND_SMS("Send SMS", isSensitive = true, isStateAltering = true),
  FIND_CONTACT("Find Contact", isSensitive = false, isStateAltering = false),
  SET_ALARM("Set Alarm", isSensitive = false, isStateAltering = true),
  GET_LOCATION("Query Location", isSensitive = false, isStateAltering = false),
  TAKE_PHOTO("Camera Capture", isSensitive = false, isStateAltering = false),
  CHECK_CALENDAR("Query Calendar", isSensitive = false, isStateAltering = false),
  CREATE_CALENDAR_EVENT("Create Event", isSensitive = true, isStateAltering = true),
  BLUETOOTH_STATUS("Bluetooth State", isSensitive = false, isStateAltering = false),
  READ_NOTIFICATIONS("Read Notifications", isSensitive = false, isStateAltering = false),
  CLIPBOARD_READ("Read Clipboard", isSensitive = false, isStateAltering = false),
  CLIPBOARD_WRITE("Copy to Clipboard", isSensitive = false, isStateAltering = true),
  SHARE_CONTENT("System Share", isSensitive = false, isStateAltering = false),
  SYSTEM_INFO("Device Telemetry", isSensitive = false, isStateAltering = false),
  AI_REQUEST("AI Inference", isSensitive = false, isStateAltering = false),
  WEB_RESEARCH("Web Research", isSensitive = false, isStateAltering = false),
  VISION_ANALYSIS("Vision Analysis", isSensitive = false, isStateAltering = false),
  MEMORY_READ("Memory Query", isSensitive = false, isStateAltering = false),
  MEMORY_WRITE("Memory Store", isSensitive = true, isStateAltering = true),
  CODE_GENERATION("Code Generation", isSensitive = false, isStateAltering = false),
  IMAGE_GENERATION("Image Generation", isSensitive = false, isStateAltering = false),
  VIDEO_GENERATION("Video Generation", isSensitive = false, isStateAltering = false),
  SEND_NOTIFICATION("Send Notification", isSensitive = false, isStateAltering = false),
  TTS_OUTPUT("Speak Text (TTS)", isSensitive = false, isStateAltering = false),
  VOICE_OUTPUT("Voice Output", isSensitive = false, isStateAltering = false),
  CREATE_REMINDER("Create Reminder", isSensitive = false, isStateAltering = true),
  LOCATION_CHECK("Check Location", isSensitive = false, isStateAltering = false),
  CHECK_BATTERY("Check Battery", isSensitive = false, isStateAltering = false),
  CHECK_CONNECTIVITY("Check Connectivity", isSensitive = false, isStateAltering = false),
  SHOW_AGENT_UI("Show Agent UI", isSensitive = false, isStateAltering = false),
  PLAY_MEDIA("Play Media", isSensitive = false, isStateAltering = false),
  DEVICE_ACTION("Device Action", isSensitive = false, isStateAltering = false)
}

/**
 * Structured Agent Action Model.
 * Represents an intended or executing device operation with full parameterization,
 * capability prerequisites, and execution telemetry.
 */
data class AgentAction(
  val id: String = UUID.randomUUID().toString(),
  val type: ActionType,
  val description: String,
  val requiredCapabilities: List<AgentCapabilityType> = emptyList(),
  val requiresConfirmation: Boolean = false,
  val confirmationPrompt: String? = null,
  val parameters: Map<String, Any?> = emptyMap(),
  val status: ActionStatus = ActionStatus.PLANNED,
  val resultMessage: String? = null,
  val errorMessage: String? = null,
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * Structured result returned from executing an Android Tool.
 */
sealed class ToolResult {
  data class Success(
    val message: String,
    val data: Any? = null
  ) : ToolResult()

  data class Failed(
    val reason: String
  ) : ToolResult()

  data class Blocked(
    val reason: String
  ) : ToolResult()

  data class PermissionRequired(
    val permissions: List<String>,
    val explanation: String
  ) : ToolResult()

  data class ConfirmationRequired(
    val action: AgentAction,
    val confirmationPrompt: String
  ) : ToolResult()

  data class Ambiguous(
    val clarificationPrompt: String,
    val options: List<String> = emptyList()
  ) : ToolResult()

  data class Cancelled(
    val reason: String = "Action was cancelled by the operator."
  ) : ToolResult()
}
