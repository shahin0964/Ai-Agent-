package com.example.core.action

import java.util.regex.Pattern

/**
 * Parsed intent from natural language command.
 */
data class ParsedCommand(
  val actionType: ActionType,
  val parameters: Map<String, Any?>,
  val description: String
)

/**
 * Robust, deterministic natural language command parser for Android device actions.
 * Extracts intent and entity parameters without relying on simulated LLM hallucinations.
 */
object CommandParser {

  fun parse(text: String): ParsedCommand? {
    val clean = text.trim()
    val lower = clean.lowercase()

    // 1. App Launch: "open [app]", "launch [app]", "start [app]"
    val appMatch = matchRegex(clean, "^(?:open|launch|start|run)\\s+(?:the\\s+)?([a-zA-Z0-9\\s]+)$")
    if (appMatch != null) {
      val appName = appMatch.trim()
      // Filter out non-app commands that start with "open"
      if (!appName.equals("camera", ignoreCase = true) && !appName.lowercase().contains("settings") && !appName.equals("calendar", ignoreCase = true)) {
        return ParsedCommand(
          actionType = ActionType.APP_LAUNCH,
          parameters = mapOf("appName" to appName),
          description = "Launch $appName"
        )
      }
    }

    // 2. Camera: "open camera", "take a picture", "take photo", "capture photo", "snap photo"
    if (lower == "open camera" || lower.contains("take a picture") || lower.contains("take photo") ||
      lower.contains("take a photo") || lower.contains("snap photo") || lower.contains("capture photo")
    ) {
      return ParsedCommand(
        actionType = ActionType.TAKE_PHOTO,
        parameters = emptyMap(),
        description = "Open camera viewfinder"
      )
    }

    // 3. Phone Call: "call [name/number]", "dial [name/number]"
    val callMatch = matchRegex(clean, "^(?:call|dial|phone)\\s+([a-zA-Z0-9\\+\\s]+)$")
    if (callMatch != null) {
      val target = callMatch.trim()
      val isNumeric = target.replace(" ", "").all { it.isDigit() || it == '+' }
      return ParsedCommand(
        actionType = ActionType.PHONE_CALL,
        parameters = if (isNumeric) {
          mapOf("phoneNumber" to target)
        } else {
          mapOf("contactName" to target)
        },
        description = "Call $target"
      )
    }

    // 4. SMS: "send sms to [name] [message]", "text [name] [message]", "send message to [name] [message]"
    val smsMatch = matchRegex(clean, "^(?:send\\s+sms\\s+to|send\\s+message\\s+to|text)\\s+([a-zA-Z0-9\\+\\s]+?)(?:\\s+saying|\\s+that|\\s*:\\s*|\\s+)([a-zA-Z0-9\\s\\.,!\\?]+)$")
    if (smsMatch != null) {
      val parts = smsMatch.split(Pattern.compile("\\s+saying|\\s+that|\\s*:\\s*|\\s+"), 2)
      if (parts.size >= 2) {
        val target = parts[0].trim()
        val message = parts[1].trim()
        return ParsedCommand(
          actionType = ActionType.SEND_SMS,
          parameters = mapOf("contactName" to target, "message" to message),
          description = "Send message to $target"
        )
      }
    }

    // 5. Find Contact: "find [name] in contacts", "search contact [name]", "find contact [name]"
    val contactMatch = matchRegex(lower, "^(?:find|search|lookup)\\s+(?:contact\\s+)?([a-z0-9\\s]+?)(?:\\s+in\\s+my\\s+contacts|\\s+in\\s+contacts)?$")
    if (contactMatch != null && (lower.contains("contact") || lower.startsWith("find contact"))) {
      val name = contactMatch.replace("contact", "").trim()
      if (name.isNotBlank()) {
        return ParsedCommand(
          actionType = ActionType.FIND_CONTACT,
          parameters = mapOf("name" to name),
          description = "Find contact $name"
        )
      }
    }

    // 6. Alarm: "set an alarm for 7", "set alarm for 7:30", "wake me up at 6:00"
    val alarmMatch = matchRegex(lower, "^(?:set\\s+(?:an\\s+)?alarm\\s+(?:for|at)|wake\\s+me\\s+up\\s+at)\\s+([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?$")
    if (alarmMatch != null) {
      val matcher = Pattern.compile("([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?").matcher(lower)
      if (matcher.find()) {
        var hour = matcher.group(1)?.toIntOrNull() ?: 7
        val minute = matcher.group(2)?.toIntOrNull() ?: 0
        val meridiem = matcher.group(3)
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        return ParsedCommand(
          actionType = ActionType.SET_ALARM,
          parameters = mapOf("hour" to hour, "minutes" to minute, "label" to "Agent Alarm"),
          description = "Set alarm for ${String.format("%02d:%02d", hour, minute)}"
        )
      }
    }

    // 7. Location: "where am i", "get my location", "what is my location", "current location"
    if (lower.contains("where am i") || lower.contains("my location") || lower.contains("current location") || lower == "get location") {
      return ParsedCommand(
        actionType = ActionType.GET_LOCATION,
        parameters = emptyMap(),
        description = "Query current GPS location"
      )
    }

    // 8. Bluetooth: "check bluetooth", "bluetooth status", "turn on bluetooth", "is bluetooth on"
    if (lower.contains("bluetooth")) {
      return ParsedCommand(
        actionType = ActionType.BLUETOOTH_STATUS,
        parameters = emptyMap(),
        description = "Inspect Bluetooth adapter status"
      )
    }

    // 9. Notifications: "read my notifications", "check notifications", "notification status"
    if (lower.contains("notification")) {
      return ParsedCommand(
        actionType = ActionType.READ_NOTIFICATIONS,
        parameters = emptyMap(),
        description = "Query notification subsystem status"
      )
    }

    // 10. Calendar: "show my calendar", "open calendar", "schedule meeting"
    if (lower.contains("calendar") || lower.contains("schedule meeting") || lower.contains("create meeting")) {
      return ParsedCommand(
        actionType = ActionType.CHECK_CALENDAR,
        parameters = mapOf("title" to "Agent Scheduled Event"),
        description = "Open calendar event scheduler"
      )
    }

    // 11. Clipboard: "copy [text] to clipboard", "read clipboard"
    val copyMatch = matchRegex(clean, "^copy\\s+(.+?)\\s+to\\s+clipboard$")
    if (copyMatch != null) {
      return ParsedCommand(
        actionType = ActionType.CLIPBOARD_WRITE,
        parameters = mapOf("copyText" to copyMatch.trim()),
        description = "Copy text to clipboard"
      )
    }
    if (lower == "read clipboard" || lower == "what is on my clipboard") {
      return ParsedCommand(
        actionType = ActionType.CLIPBOARD_READ,
        parameters = emptyMap(),
        description = "Read clipboard content"
      )
    }

    // 12. Share: "share this: [text]", "share [text]"
    val shareMatch = matchRegex(clean, "^share\\s+(?:this\\s*:\\s*|this\\s+)?(.+)$")
    if (shareMatch != null) {
      return ParsedCommand(
        actionType = ActionType.SHARE_CONTENT,
        parameters = mapOf("text" to shareMatch.trim()),
        description = "Share content via system sheet"
      )
    }

    // 13. Battery: "check battery", "what is my battery", "battery status", "how much battery"
    if (lower.contains("battery") || lower == "check battery level") {
      return ParsedCommand(
        actionType = ActionType.CHECK_BATTERY,
        parameters = mapOf("query" to "battery"),
        description = "Query battery telemetry"
      )
    }

    // 14. Storage / Disk Space: "check storage", "how much storage", "storage space", "internal storage"
    if (lower.contains("storage") || lower.contains("disk space") || lower.contains("free space")) {
      return ParsedCommand(
        actionType = ActionType.SYSTEM_INFO,
        parameters = mapOf("query" to "storage"),
        description = "Query internal storage telemetry"
      )
    }

    // 15. RAM / Memory: "check ram", "memory usage", "system memory", "how much ram"
    if (lower.contains("ram") || lower.contains("memory usage") || lower.contains("system memory")) {
      return ParsedCommand(
        actionType = ActionType.SYSTEM_INFO,
        parameters = mapOf("query" to "ram"),
        description = "Query RAM memory telemetry"
      )
    }

    // 16. Wi-Fi / Network: "is wifi connected", "check wifi", "network status", "internet status"
    if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("network status") || lower.contains("internet status")) {
      return ParsedCommand(
        actionType = ActionType.CHECK_CONNECTIVITY,
        parameters = mapOf("query" to "network"),
        description = "Query network and Wi-Fi telemetry"
      )
    }

    // 17. General System Telemetry: "device status", "system status", "check system", "device telemetry"
    if (lower.contains("device status") || lower.contains("system status") || lower.contains("check system") || lower.contains("device telemetry")) {
      return ParsedCommand(
        actionType = ActionType.SYSTEM_INFO,
        parameters = mapOf("query" to "all"),
        description = "Query overall device telemetry"
      )
    }

    return null
  }

  private fun matchRegex(text: String, regex: String): String? {
    val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(text)
    return if (matcher.matches() && matcher.groupCount() >= 1) {
      matcher.group(1)
    } else null
  }
}
