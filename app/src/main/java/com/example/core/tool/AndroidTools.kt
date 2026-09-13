package com.example.core.tool

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.action.ToolResult
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.capability.AgentCapabilityType
import com.example.core.capability.CapabilityExecutionCheck

// 1. App Launcher Tool
class AppLauncherTool : AndroidTool {
  override val id = "tool_app_launcher"
  override val name = "App Launcher"
  override val description = "Resolves installed Android applications and launches them."
  override val requiredCapabilities = listOf(AgentCapabilityType.APP_LAUNCH)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val appNameQuery = parameters["appName"] as? String
      ?: return ToolResult.Failed("No application name specified to launch.")

    val packageManager = context.packageManager
    val targetPackage = resolvePackage(packageManager, appNameQuery)

    if (targetPackage == null) {
      return ToolResult.Failed("Could not open '$appNameQuery' because it is not installed on this device.")
    }

    val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
    if (launchIntent == null) {
      return ToolResult.Failed("Application '$appNameQuery' was found, but does not declare a launchable main activity.")
    }

    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(launchIntent)
      ToolResult.Success("Opened $appNameQuery ($targetPackage).", mapOf("package" to targetPackage))
    } catch (e: Exception) {
      ToolResult.Failed("Failed to launch $appNameQuery: ${e.message}")
    }
  }

  private fun resolvePackage(pm: PackageManager, query: String): String? {
    val normalized = query.trim().lowercase()

    // Fast mapping for common applications
    val commonMap = mapOf(
      "youtube" to "com.google.android.youtube",
      "chrome" to "com.android.chrome",
      "google chrome" to "com.android.chrome",
      "maps" to "com.google.android.apps.maps",
      "google maps" to "com.google.android.apps.maps",
      "settings" to "com.android.settings",
      "camera" to "com.google.android.GoogleCamera",
      "calendar" to "com.google.android.calendar",
      "clock" to "com.google.android.deskclock",
      "calculator" to "com.google.android.calculator",
      "messages" to "com.google.android.apps.messaging",
      "phone" to "com.google.android.dialer"
    )

    commonMap[normalized]?.let { directPkg ->
      try {
        pm.getPackageInfo(directPkg, 0)
        return directPkg
      } catch (_: Exception) {
        // Fallback to searching installed packages
      }
    }

    // Search installed applications by label
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveList = pm.queryIntentActivities(mainIntent, 0)

    for (resolveInfo in resolveList) {
      val label = resolveInfo.loadLabel(pm).toString().lowercase()
      if (label == normalized || label.contains(normalized)) {
        return resolveInfo.activityInfo.packageName
      }
    }

    return null
  }
}

// 2. Contact Tool
class ContactTool : AndroidTool {
  override val id = "tool_contacts"
  override val name = "Contact Lookup"
  override val description = "Queries real device contacts for phone numbers and profiles."
  override val requiredCapabilities = listOf(AgentCapabilityType.CONTACTS)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val nameQuery = parameters["name"] as? String
      ?: return ToolResult.Failed("No contact name specified to search.")

    val matchedContacts = queryContacts(context, nameQuery)

    if (matchedContacts.isEmpty()) {
      return ToolResult.Failed("No contact matching '$nameQuery' was found in your device contacts.")
    }

    if (matchedContacts.size > 1) {
      val names = matchedContacts.map { "${it.displayName} (${it.phoneNumber})" }
      return ToolResult.Ambiguous(
        clarificationPrompt = "Found multiple contacts matching '$nameQuery': ${names.joinToString(", ")}. Which contact do you want to select?",
        options = names
      )
    }

    val single = matchedContacts.first()
    return ToolResult.Success(
      message = "Found contact ${single.displayName}: ${single.phoneNumber}",
      data = mapOf("name" to single.displayName, "phoneNumber" to single.phoneNumber)
    )
  }

  data class ContactEntry(val displayName: String, val phoneNumber: String)

  @SuppressLint("Range")
  private fun queryContacts(context: Context, query: String): List<ContactEntry> {
    val results = mutableListOf<ContactEntry>()
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$query%")

    try {
      context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
        while (cursor.moveToNext()) {
          val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: ""
          val number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
          if (name.isNotBlank() && number.isNotBlank()) {
            results.add(ContactEntry(name, number))
          }
        }
      }
    } catch (_: Exception) {
      // Return whatever was retrieved
    }
    return results
  }
}

// 3. Phone / Call Tool
class PhoneTool : AndroidTool {
  override val id = "tool_phone"
  override val name = "Phone & Calling"
  override val description = "Initiates voice phone calls through Android telephony."
  override val requiredCapabilities = listOf(AgentCapabilityType.PHONE)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val phoneNumber = parameters["phoneNumber"] as? String
      ?: return ToolResult.Failed("No phone number provided to call.")
    val contactName = parameters["contactName"] as? String ?: phoneNumber
    val isConfirmed = parameters["confirmed"] as? Boolean ?: false

    // Check confirmation policy
    if (!isConfirmed && policy.requiresConfirmation(ActionType.PHONE_CALL)) {
      val action = AgentAction(
        type = ActionType.PHONE_CALL,
        description = "Call $contactName at $phoneNumber",
        requiredCapabilities = requiredCapabilities,
        requiresConfirmation = true,
        confirmationPrompt = "Confirm outgoing voice call to $contactName ($phoneNumber)?",
        parameters = parameters
      )
      return ToolResult.ConfirmationRequired(
        action = action,
        confirmationPrompt = "Would you like me to call $contactName at $phoneNumber?"
      )
    }

    // Supported Android call flow: dispatch via ACTION_DIAL with pre-filled number
    val callIntent = Intent(Intent.ACTION_DIAL).apply {
      data = Uri.parse("tel:${Uri.encode(phoneNumber)}")
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(callIntent)
      ToolResult.Success("Opened dialer for $contactName ($phoneNumber).")
    } catch (e: Exception) {
      ToolResult.Failed("Unable to start call to $phoneNumber: ${e.message}")
    }
  }
}

// 4. SMS Tool
class SmsTool : AndroidTool {
  override val id = "tool_sms"
  override val name = "SMS Messaging"
  override val description = "Composes and prepares SMS text messages."
  override val requiredCapabilities = listOf(AgentCapabilityType.SMS)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val phoneNumber = parameters["phoneNumber"] as? String
      ?: return ToolResult.Failed("No phone number or contact specified for SMS.")
    val messageText = parameters["message"] as? String ?: ""
    val contactName = parameters["contactName"] as? String ?: phoneNumber
    val isConfirmed = parameters["confirmed"] as? Boolean ?: false

    if (!isConfirmed && policy.requiresConfirmation(ActionType.SEND_SMS)) {
      val action = AgentAction(
        type = ActionType.SEND_SMS,
        description = "Send SMS to $contactName: \"$messageText\"",
        requiredCapabilities = requiredCapabilities,
        requiresConfirmation = true,
        confirmationPrompt = "Confirm sending message to $contactName: \"$messageText\"?",
        parameters = parameters
      )
      return ToolResult.ConfirmationRequired(
        action = action,
        confirmationPrompt = "Confirm sending SMS to $contactName: \"$messageText\"?"
      )
    }

    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
      putExtra("sms_body", messageText)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(smsIntent)
      ToolResult.Success("Prepared SMS to $contactName ($phoneNumber): \"$messageText\".")
    } catch (e: Exception) {
      ToolResult.Failed("Failed to prepare SMS: ${e.message}")
    }
  }
}

// 5. Alarm Tool
class AlarmTool : AndroidTool {
  override val id = "tool_alarm"
  override val name = "Alarm & Timer"
  override val description = "Sets system alarms and timers using AlarmClock intent contract."
  override val requiredCapabilities = listOf(AgentCapabilityType.ALARMS)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val hour = (parameters["hour"] as? Number)?.toInt() ?: 7
    val minutes = (parameters["minutes"] as? Number)?.toInt() ?: 0
    val label = parameters["label"] as? String ?: "Agent Alarm"

    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
      putExtra(AlarmClock.EXTRA_HOUR, hour)
      putExtra(AlarmClock.EXTRA_MINUTES, minutes)
      putExtra(AlarmClock.EXTRA_MESSAGE, label)
      putExtra(AlarmClock.EXTRA_SKIP_UI, false)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(alarmIntent)
      ToolResult.Success("Alarm set for ${String.format("%02d:%02d", hour, minutes)} ($label).")
    } catch (e: Exception) {
      ToolResult.Failed("Could not set alarm: ${e.message}")
    }
  }
}

// 6. Location Tool
class LocationTool : AndroidTool {
  override val id = "tool_location"
  override val name = "Location Service"
  override val description = "Queries device location telemetry through Android LocationManager."
  override val requiredCapabilities = listOf(AgentCapabilityType.LOCATION)

  @SuppressLint("MissingPermission")
  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return ToolResult.Failed("Location service is unavailable on this device.")

    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!isGpsEnabled && !isNetworkEnabled) {
      return ToolResult.Failed("Device location services are currently turned off in Android system settings.")
    }

    var bestLocation: Location? = null
    try {
      if (isGpsEnabled) {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      }
      if (bestLocation == null && isNetworkEnabled) {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      }
    } catch (e: Exception) {
      return ToolResult.Failed("Unable to retrieve location: ${e.message}")
    }

    return if (bestLocation != null) {
      ToolResult.Success(
        message = "Current coordinates: Lat ${String.format("%.4f", bestLocation.latitude)}, Lon ${String.format("%.4f", bestLocation.longitude)}.",
        data = mapOf("latitude" to bestLocation.latitude, "longitude" to bestLocation.longitude)
      )
    } else {
      ToolResult.Success("Location services are active. Waiting for satellite / network fix.")
    }
  }
}

// 7. Camera Tool
class CameraTool : AndroidTool {
  override val id = "tool_camera"
  override val name = "Camera Subsystem"
  override val description = "Launches camera capture interface via MediaStore contract."
  override val requiredCapabilities = listOf(AgentCapabilityType.CAMERA)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(cameraIntent)
      ToolResult.Success("Camera capture viewfinder opened.")
    } catch (e: Exception) {
      ToolResult.Failed("Could not launch camera: ${e.message}")
    }
  }
}

// 8. Bluetooth Tool
class BluetoothTool : AndroidTool {
  override val id = "tool_bluetooth"
  override val name = "Bluetooth Telemetry"
  override val description = "Inspects Bluetooth adapter state and connectivity."
  override val requiredCapabilities = listOf(AgentCapabilityType.BLUETOOTH_NEARBY)

  @SuppressLint("MissingPermission")
  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = bm?.adapter

    if (adapter == null) {
      return ToolResult.Failed("Bluetooth hardware is not present on this device.")
    }

    val isEnabled = adapter.isEnabled
    val bondedCount = try { adapter.bondedDevices?.size ?: 0 } catch (_: Exception) { 0 }

    return ToolResult.Success(
      message = if (isEnabled) {
        "Bluetooth is active. $bondedCount paired accessory device(s) registered."
      } else {
        "Bluetooth adapter is powered off."
      },
      data = mapOf("enabled" to isEnabled, "pairedCount" to bondedCount)
    )
  }
}

// 9. Notification Tool
class NotificationTool : AndroidTool {
  override val id = "tool_notifications"
  override val name = "Notification Subsystem"
  override val description = "Checks notification status and listener permissions."
  override val requiredCapabilities = listOf(AgentCapabilityType.NOTIFICATIONS)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val isListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    return ToolResult.Success(
      message = "Notification posting is ${if (areNotificationsEnabled) "ENABLED" else "DISABLED"}. " +
          "Notification listener access is ${if (isListenerEnabled) "GRANTED" else "NOT CONFIGURED"}.",
      data = mapOf("posting" to areNotificationsEnabled, "listener" to isListenerEnabled)
    )
  }
}

// 10. Calendar Tool
class CalendarTool : AndroidTool {
  override val id = "tool_calendar"
  override val name = "Calendar Events"
  override val description = "Schedules events and meetings via CalendarContract."
  override val requiredCapabilities = listOf(AgentCapabilityType.CALENDAR)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val title = parameters["title"] as? String ?: "Agent Meeting"
    val description = parameters["description"] as? String ?: "Scheduled by Agent"

    val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.Events.TITLE, title)
      putExtra(CalendarContract.Events.DESCRIPTION, description)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(calendarIntent)
      ToolResult.Success("Opened calendar event scheduler for '$title'.")
    } catch (e: Exception) {
      ToolResult.Failed("Could not launch calendar: ${e.message}")
    }
  }
}

// 11. Clipboard Tool
class ClipboardTool : AndroidTool {
  override val id = "tool_clipboard"
  override val name = "Clipboard Manager"
  override val description = "Copies and reads clipboard data."
  override val requiredCapabilities = listOf(AgentCapabilityType.CLIPBOARD)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      ?: return ToolResult.Failed("Clipboard service is not available.")

    val textToCopy = parameters["copyText"] as? String
    if (textToCopy != null) {
      val clip = ClipData.newPlainText("Agent Copied Text", textToCopy)
      clipboard.setPrimaryClip(clip)
      return ToolResult.Success("Copied to clipboard: \"$textToCopy\".")
    }

    val clip = clipboard.primaryClip
    val text = if (clip != null && clip.itemCount > 0) {
      clip.getItemAt(0).text?.toString() ?: ""
    } else ""

    return if (text.isNotBlank()) {
      ToolResult.Success("Clipboard content: \"$text\".", mapOf("text" to text))
    } else {
      ToolResult.Success("Clipboard is currently empty.")
    }
  }
}

// 12. Share Tool
class ShareTool : AndroidTool {
  override val id = "tool_share"
  override val name = "System Share"
  override val description = "Shares text content through Android system share sheet."
  override val requiredCapabilities = listOf(AgentCapabilityType.SHARE)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val textToShare = parameters["text"] as? String ?: ""
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, textToShare)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share via Agent").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(shareIntent)
      ToolResult.Success("Dispatched system share sheet for \"$textToShare\".")
    } catch (e: Exception) {
      ToolResult.Failed("Failed to open share sheet: ${e.message}")
    }
  }
}

// 13. Accessibility Tool
class AccessibilityTool : AndroidTool {
  override val id = "tool_accessibility"
  override val name = "Accessibility System"
  override val description = "Interacts with active screen elements via AccessibilityNodeInfo and checks service enablement."
  override val requiredCapabilities = listOf(AgentCapabilityType.ACCESSIBILITY)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val isConnected = com.example.core.accessibility.AgentAccessibilityService.isServiceConnected
    if (!isConnected) {
      val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
      val enabledList = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()
      val isAppEnabled = enabledList.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
      if (!isAppEnabled) {
        return ToolResult.Blocked("Accessibility Service is not enabled in Android Settings. Please enable Nexus AI in Accessibility Settings first.")
      }
    }

    val clickTarget = parameters["click_text"]?.toString() ?: parameters["target"]?.toString()
    val action = parameters["action"]?.toString()

    return when {
      !clickTarget.isNullOrBlank() || action == "click" -> {
        val target = clickTarget ?: ""
        val success = com.example.core.accessibility.AgentAccessibilityService.findAndClickText(target)
        if (success) {
          ToolResult.Success("Successfully performed accessibility click on element matching '$target'.")
        } else {
          ToolResult.Failed("Could not find or click clickable node matching '$target' in active window.")
        }
      }
      action == "read_screen" || action == "inspect" -> {
        val content = com.example.core.accessibility.AgentAccessibilityService.readActiveWindowText()
        ToolResult.Success("Active Screen Content:\n$content")
      }
      else -> {
        ToolResult.Success("Nexus AI Accessibility Service is active and connected to system node engine.")
      }
    }
  }
}

// 14. Screen Capture Tool
class ScreenCaptureTool : AndroidTool {
  override val id = "tool_screen_capture"
  override val name = "Screen Capture (MediaProjection)"
  override val description = "Interacts with MediaProjection architecture."
  override val requiredCapabilities = listOf(AgentCapabilityType.SCREEN_CAPTURE)

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    // Truthful reporting: MediaProjection requires interactive runtime token
    return ToolResult.Blocked(
      "Screen capture requires operator runtime consent prompt on each session via Android MediaProjection."
    )
  }
}

// 15. System Telemetry Tool
class SystemTelemetryTool : AndroidTool {
  override val id = "tool_system_telemetry"
  override val name = "Device & System Telemetry"
  override val description = "Retrieves real hardware and OS status (battery, RAM, storage, network, uptime)."
  override val requiredCapabilities: List<AgentCapabilityType> = emptyList()

  override suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult {
    val check = canExecute(context, policy)
    if (check !is CapabilityExecutionCheck.Allowed) {
      return handleCheckFailure(check)
    }

    val snapshot = com.example.core.telemetry.AndroidSystemTelemetry.getSnapshot(context)
    val queryType = parameters["query"] as? String ?: "all"

    val summaryMessage = when (queryType) {
      "battery" -> "Battery level is ${snapshot.batteryLevel}% (${if (snapshot.isCharging) "Charging via ${snapshot.powerSource}" else "Discharging"})."
      "storage" -> "Internal storage: ${com.example.core.telemetry.AndroidSystemTelemetry.formatBytes(snapshot.availableStorageBytes)} free of ${com.example.core.telemetry.AndroidSystemTelemetry.formatBytes(snapshot.totalStorageBytes)} (${snapshot.storageUsagePercentage}% used)."
      "ram", "memory" -> "System RAM: ${com.example.core.telemetry.AndroidSystemTelemetry.formatBytes(snapshot.availableRamBytes)} available of ${com.example.core.telemetry.AndroidSystemTelemetry.formatBytes(snapshot.totalRamBytes)} total (${snapshot.ramUsagePercentage}% in use)."
      "network", "wifi" -> "Network status: ${snapshot.networkType} (${if (snapshot.isNetworkConnected) "Connected / Online" else "Offline"})."
      else -> "Device Telemetry for ${snapshot.deviceModel}: Battery ${snapshot.batteryLevel}%, Storage ${snapshot.storageUsagePercentage}% used, RAM ${snapshot.ramUsagePercentage}% used, Network: ${snapshot.networkType}, Uptime: ${snapshot.deviceUptimeFormatted}."
    }

    return ToolResult.Success(
      message = summaryMessage,
      data = mapOf(
        "batteryLevel" to snapshot.batteryLevel,
        "isCharging" to snapshot.isCharging,
        "powerSource" to snapshot.powerSource,
        "storageUsagePct" to snapshot.storageUsagePercentage,
        "ramUsagePct" to snapshot.ramUsagePercentage,
        "networkType" to snapshot.networkType,
        "uptime" to snapshot.deviceUptimeFormatted,
        "model" to snapshot.deviceModel,
        "androidVersion" to snapshot.androidVersion
      )
    )
  }
}

private fun handleCheckFailure(check: CapabilityExecutionCheck): ToolResult {
  return when (check) {
    is CapabilityExecutionCheck.BlockedByUserInAgent -> {
      ToolResult.Blocked(check.reason)
    }
    is CapabilityExecutionCheck.PermissionRequired -> {
      ToolResult.PermissionRequired(check.permissions, check.explanation)
    }
    is CapabilityExecutionCheck.RequiresSettings -> {
      ToolResult.Blocked(check.explanation)
    }
    is CapabilityExecutionCheck.Unavailable -> {
      ToolResult.Failed(check.reason)
    }
    CapabilityExecutionCheck.Allowed -> {
      ToolResult.Failed("Unknown capability state.")
    }
  }
}
