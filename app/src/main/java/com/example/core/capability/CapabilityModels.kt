package com.example.core.capability

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Standardized Android Capability Types recognized by the Agent.
 */
enum class AgentCapabilityType(val identifier: String) {
  MICROPHONE("microphone"),
  CAMERA("camera"),
  LOCATION("location"),
  CONTACTS("contacts"),
  PHONE("phone"),
  SMS("sms"),
  NOTIFICATIONS("notifications"),
  FILES_MEDIA("files_media"),
  CALENDAR("calendar"),
  ALARMS("alarms"),
  BLUETOOTH_NEARBY("bluetooth_nearby"),
  SCREEN_CAPTURE("screen_capture"),
  ACCESSIBILITY("accessibility"),
  APP_LAUNCH("app_launch"),
  WEB("web"),
  MEDIA("media"),
  CLIPBOARD("clipboard"),
  SHARE("share");

  companion object {
    fun fromIdentifier(id: String): AgentCapabilityType? {
      return values().find { it.identifier.equals(id, ignoreCase = true) }
    }
  }
}

/**
 * Truthful status of an Android capability.
 */
enum class CapabilityStatus(val label: String) {
  GRANTED("Granted"),
  DENIED("Denied"),
  NOT_REQUESTED("Not Requested"),
  RESTRICTED("Restricted"),
  SYSTEM_MANAGED("System Managed"),
  UNAVAILABLE("Unavailable"),
  REQUIRES_SETTINGS("Requires Settings"),
  ENABLED("Enabled"),
  DISABLED("Disabled")
}

/**
 * Categorization matching Section 33 Settings hierarchy:
 * VOICE, VISION, COMMUNICATION, DEVICE, PRODUCTIVITY, AGENT
 */
enum class CapabilityCategory(val title: String) {
  VOICE("Voice Subsystem"),
  VISION("Vision & Screen"),
  COMMUNICATION("Communication"),
  DEVICE("Device & Sensors"),
  PRODUCTIVITY("Productivity & Time"),
  AGENT("Agent & System")
}

/**
 * Intent destination to navigate user directly into appropriate Android Settings.
 */
enum class SettingsDestination {
  APP_DETAILS,
  NOTIFICATION_LISTENER,
  ACCESSIBILITY,
  BLUETOOTH,
  LOCATION,
  NONE
}

/**
 * Android Device Capability Model.
 * Strictly verifies Android state without fabricating or bypassing permissions.
 */
data class DeviceCapability(
  val id: String,
  val type: AgentCapabilityType,
  val name: String,
  val description: String,
  val category: CapabilityCategory,
  val requiredPermissions: List<String> = emptyList(),
  val requiresUserAction: Boolean = false,
  val settingsDestination: SettingsDestination = SettingsDestination.NONE,
  val unavailableReason: String? = null,
  val stateNote: String = "Controlled via Android OS permissions",
  val isAvailableOnDeviceChecker: (Context) -> Boolean = { true },
  val customStatusChecker: ((Context) -> CapabilityStatus)? = null
) {
  // Backwards compatibility property for Phase 1 & 2
  val androidPermission: String?
    get() = requiredPermissions.firstOrNull()

  val requiresSystemSettings: Boolean
    get() = settingsDestination != SettingsDestination.NONE

  val isSpecialPermission: Boolean
    get() = settingsDestination == SettingsDestination.NOTIFICATION_LISTENER || settingsDestination == SettingsDestination.ACCESSIBILITY

  fun isAvailableOnDevice(context: Context): Boolean {
    return isAvailableOnDeviceChecker(context)
  }

  fun isCurrentlyGranted(context: Context): Boolean {
    if (!isAvailableOnDevice(context)) return false
    if (customStatusChecker != null) {
      return customStatusChecker.invoke(context) == CapabilityStatus.GRANTED
    }
    if (requiredPermissions.isEmpty()) return true
    return requiredPermissions.all { perm ->
      ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
  }

  fun checkStatus(context: Context, isEnabledInAgent: Boolean): CapabilityStatus {
    if (!isAvailableOnDevice(context)) {
      return CapabilityStatus.UNAVAILABLE
    }

    if (!isEnabledInAgent) {
      return CapabilityStatus.DISABLED
    }

    if (customStatusChecker != null) {
      return customStatusChecker.invoke(context)
    }

    if (requiredPermissions.isEmpty()) {
      return CapabilityStatus.GRANTED
    }

    val allGranted = requiredPermissions.all { perm ->
      ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    return if (allGranted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED
  }
}

/**
 * Centralized Capability Registry.
 * Exposes all supported Android capabilities and verifies real system states.
 */
object CapabilityRegistry {

  val allCapabilities: List<DeviceCapability> = listOf(
    // 1. VOICE
    DeviceCapability(
      id = "microphone",
      type = AgentCapabilityType.MICROPHONE,
      name = "Microphone",
      description = "Required for voice interaction, live speech-to-text, and acoustic monitoring.",
      category = CapabilityCategory.VOICE,
      requiredPermissions = listOf(Manifest.permission.RECORD_AUDIO),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Android Record Audio Runtime Permission",
      isAvailableOnDeviceChecker = { context ->
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
      }
    ),

    // 2. VISION
    DeviceCapability(
      id = "camera",
      type = AgentCapabilityType.CAMERA,
      name = "Camera",
      description = "Required for Vision AI visual question answering and photo capture.",
      category = CapabilityCategory.VISION,
      requiredPermissions = listOf(Manifest.permission.CAMERA),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Android Camera Runtime Permission",
      isAvailableOnDeviceChecker = { context ->
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
      }
    ),
    DeviceCapability(
      id = "screen_capture",
      type = AgentCapabilityType.SCREEN_CAPTURE,
      name = "Screen Capture",
      description = "Visual context ingestion using Android MediaProjection token.",
      category = CapabilityCategory.VISION,
      requiresUserAction = true,
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Requires explicit runtime user consent on each session"
    ),

    // 3. COMMUNICATION
    DeviceCapability(
      id = "contacts",
      type = AgentCapabilityType.CONTACTS,
      name = "Contacts",
      description = "Enables authorized name lookups and hands-free communication dispatch.",
      category = CapabilityCategory.COMMUNICATION,
      requiredPermissions = listOf(Manifest.permission.READ_CONTACTS),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Android Contacts Read Permission"
    ),
    DeviceCapability(
      id = "phone",
      type = AgentCapabilityType.PHONE,
      name = "Phone & Calling",
      description = "Enables voice-initiated calls with explicit user authorization.",
      category = CapabilityCategory.COMMUNICATION,
      requiredPermissions = listOf(Manifest.permission.CALL_PHONE),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Uses system dialer contract or direct call with confirmation",
      isAvailableOnDeviceChecker = { context ->
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
      }
    ),
    DeviceCapability(
      id = "sms",
      type = AgentCapabilityType.SMS,
      name = "SMS & Messaging",
      description = "Allows composition and delivery of text messages through Android SMS API.",
      category = CapabilityCategory.COMMUNICATION,
      requiredPermissions = listOf(Manifest.permission.SEND_SMS),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Requires confirmation policy check before transmission",
      isAvailableOnDeviceChecker = { context ->
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
      }
    ),
    DeviceCapability(
      id = "notifications",
      type = AgentCapabilityType.NOTIFICATIONS,
      name = "Notifications",
      description = "Enables agent alerts, reminder delivery, and notification summary access.",
      category = CapabilityCategory.COMMUNICATION,
      requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
      } else emptyList(),
      settingsDestination = SettingsDestination.NOTIFICATION_LISTENER,
      stateNote = "Requires notification post permission and optional listener access",
      customStatusChecker = { context ->
        val postGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        if (postGranted && listenerEnabled) {
          CapabilityStatus.GRANTED
        } else if (postGranted) {
          CapabilityStatus.REQUIRES_SETTINGS
        } else {
          CapabilityStatus.DENIED
        }
      }
    ),

    // 4. DEVICE
    DeviceCapability(
      id = "bluetooth",
      type = AgentCapabilityType.BLUETOOTH_NEARBY,
      name = "Bluetooth & Nearby",
      description = "Checks Bluetooth state and connects to external audio accessories.",
      category = CapabilityCategory.DEVICE,
      requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_CONNECT)
      } else emptyList(),
      settingsDestination = SettingsDestination.BLUETOOTH,
      stateNote = "Requires Bluetooth hardware and runtime connect permission",
      isAvailableOnDeviceChecker = { context ->
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bm?.adapter != null
      },
      customStatusChecker = { context ->
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter
        if (adapter == null) {
          CapabilityStatus.UNAVAILABLE
        } else {
          val permGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
          } else true
          if (permGranted) {
            if (adapter.isEnabled) CapabilityStatus.GRANTED else CapabilityStatus.DISABLED
          } else {
            CapabilityStatus.DENIED
          }
        }
      }
    ),
    DeviceCapability(
      id = "location",
      type = AgentCapabilityType.LOCATION,
      name = "Location",
      description = "Enables context-aware assistance, localized queries, and navigation dispatch.",
      category = CapabilityCategory.DEVICE,
      requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ),
      settingsDestination = SettingsDestination.LOCATION,
      stateNote = "Android Location Permissions",
      isAvailableOnDeviceChecker = { context ->
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)
      },
      customStatusChecker = { context ->
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED
      }
    ),

    // 5. PRODUCTIVITY
    DeviceCapability(
      id = "calendar",
      type = AgentCapabilityType.CALENDAR,
      name = "Calendar",
      description = "Enables event scheduling, agenda queries, and meeting management.",
      category = CapabilityCategory.PRODUCTIVITY,
      requiredPermissions = listOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
      ),
      settingsDestination = SettingsDestination.APP_DETAILS,
      stateNote = "Android Calendar Read/Write Permissions"
    ),
    DeviceCapability(
      id = "alarms",
      type = AgentCapabilityType.ALARMS,
      name = "Alarms & Timers",
      description = "Sets system alarms and countdown timers via AlarmClock intent contracts.",
      category = CapabilityCategory.PRODUCTIVITY,
      requiredPermissions = emptyList(),
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Uses Android AlarmClock intent contracts & AlarmManager",
      customStatusChecker = { CapabilityStatus.GRANTED }
    ),

    // 6. AGENT & SYSTEM
    DeviceCapability(
      id = "app_launch",
      type = AgentCapabilityType.APP_LAUNCH,
      name = "App Interaction & Launcher",
      description = "Verifies installed applications and launches them through official Android APIs.",
      category = CapabilityCategory.AGENT,
      requiredPermissions = emptyList(),
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Uses Android PackageManager intent contracts",
      customStatusChecker = { CapabilityStatus.GRANTED }
    ),
    DeviceCapability(
      id = "accessibility",
      type = AgentCapabilityType.ACCESSIBILITY,
      name = "Accessibility Service",
      description = "Optional automated assistance via Android Accessibility framework.",
      category = CapabilityCategory.AGENT,
      requiresUserAction = true,
      settingsDestination = SettingsDestination.ACCESSIBILITY,
      stateNote = "Must be enabled explicitly by user in Android System Settings",
      customStatusChecker = { context ->
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am == null) {
          CapabilityStatus.UNAVAILABLE
        } else {
          val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
          val isOurServiceEnabled = enabledServices.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
          if (isOurServiceEnabled) CapabilityStatus.GRANTED else CapabilityStatus.REQUIRES_SETTINGS
        }
      }
    ),
    DeviceCapability(
      id = "files_media",
      type = AgentCapabilityType.FILES_MEDIA,
      name = "Files & Media",
      description = "Accesses user-selected files and documents via Storage Access Framework.",
      category = CapabilityCategory.AGENT,
      requiredPermissions = emptyList(),
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Uses Android Storage Access Framework (SAF)",
      customStatusChecker = { CapabilityStatus.GRANTED }
    ),
    DeviceCapability(
      id = "clipboard",
      type = AgentCapabilityType.CLIPBOARD,
      name = "Clipboard",
      description = "Copies and reads clipboard data with user awareness.",
      category = CapabilityCategory.AGENT,
      requiredPermissions = emptyList(),
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Uses Android ClipboardManager",
      customStatusChecker = { CapabilityStatus.GRANTED }
    ),
    DeviceCapability(
      id = "share",
      type = AgentCapabilityType.SHARE,
      name = "System Share",
      description = "Dispatches text and media through Android system share sheets.",
      category = CapabilityCategory.AGENT,
      requiredPermissions = emptyList(),
      settingsDestination = SettingsDestination.NONE,
      stateNote = "Uses Android Intent.ACTION_SEND contracts",
      customStatusChecker = { CapabilityStatus.GRANTED }
    )
  )

  fun findByType(type: AgentCapabilityType): DeviceCapability? {
    return allCapabilities.find { it.type == type }
  }

  fun findById(id: String): DeviceCapability? {
    return allCapabilities.find { it.id.equals(id, ignoreCase = true) }
  }
}
