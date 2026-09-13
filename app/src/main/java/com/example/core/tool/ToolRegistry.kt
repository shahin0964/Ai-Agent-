package com.example.core.tool

import com.example.core.action.ActionType
import com.example.core.capability.AgentCapabilityType

/**
 * Centralized Tool Registry containing all supported Android tools.
 */
object ToolRegistry {

  val appLauncherTool = AppLauncherTool()
  val contactTool = ContactTool()
  val phoneTool = PhoneTool()
  val smsTool = SmsTool()
  val alarmTool = AlarmTool()
  val locationTool = LocationTool()
  val cameraTool = CameraTool()
  val bluetoothTool = BluetoothTool()
  val notificationTool = NotificationTool()
  val calendarTool = CalendarTool()
  val clipboardTool = ClipboardTool()
  val shareTool = ShareTool()
  val accessibilityTool = AccessibilityTool()
  val screenCaptureTool = ScreenCaptureTool()
  val systemTelemetryTool = SystemTelemetryTool()

  val allTools: List<AndroidTool> = listOf(
    appLauncherTool,
    contactTool,
    phoneTool,
    smsTool,
    alarmTool,
    locationTool,
    cameraTool,
    bluetoothTool,
    notificationTool,
    calendarTool,
    clipboardTool,
    shareTool,
    accessibilityTool,
    screenCaptureTool,
    systemTelemetryTool
  )

  fun findById(toolId: String): AndroidTool? {
    return allTools.find { it.id == toolId }
  }

  fun findForActionType(actionType: ActionType): AndroidTool? {
    return when (actionType) {
      ActionType.APP_LAUNCH -> appLauncherTool
      ActionType.PHONE_CALL -> phoneTool
      ActionType.SEND_SMS -> smsTool
      ActionType.FIND_CONTACT -> contactTool
      ActionType.SET_ALARM -> alarmTool
      ActionType.GET_LOCATION -> locationTool
      ActionType.TAKE_PHOTO -> cameraTool
      ActionType.CHECK_CALENDAR, ActionType.CREATE_CALENDAR_EVENT -> calendarTool
      ActionType.BLUETOOTH_STATUS -> bluetoothTool
      ActionType.READ_NOTIFICATIONS -> notificationTool
      ActionType.CLIPBOARD_READ, ActionType.CLIPBOARD_WRITE -> clipboardTool
      ActionType.SHARE_CONTENT -> shareTool
      ActionType.SYSTEM_INFO, ActionType.CHECK_BATTERY, ActionType.CHECK_CONNECTIVITY -> systemTelemetryTool
      else -> null
    }
  }

  fun findForCapability(capabilityType: AgentCapabilityType): List<AndroidTool> {
    return allTools.filter { it.requiredCapabilities.contains(capabilityType) }
  }
}
