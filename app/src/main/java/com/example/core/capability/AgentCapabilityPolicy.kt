package com.example.core.capability

import android.content.Context
import com.example.core.action.ActionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Configurable Action Confirmation Mode.
 * Determines when user confirmation dialogs are mandatory before executing device actions.
 */
enum class ActionConfirmationMode(val label: String, val description: String) {
  STRICT(
    label = "Strict",
    description = "Mandatory user confirmation for all sensitive and state-altering device actions."
  ),
  NORMAL(
    label = "Normal",
    description = "Low-risk actions execute directly; sensitive actions (Calls, SMS, System Alarms) require confirmation."
  ),
  DIRECT(
    label = "Direct",
    description = "Direct execution for all supported actions where permissible. Never bypasses OS security dialogs."
  )
}

/**
 * Result of evaluating whether a capability can be executed under current policy and system permissions.
 */
sealed class CapabilityExecutionCheck {
  object Allowed : CapabilityExecutionCheck()

  data class BlockedByUserInAgent(
    val capabilityType: AgentCapabilityType,
    val capabilityName: String,
    val reason: String
  ) : CapabilityExecutionCheck()

  data class PermissionRequired(
    val capabilityType: AgentCapabilityType,
    val permissions: List<String>,
    val explanation: String
  ) : CapabilityExecutionCheck()

  data class RequiresSettings(
    val capabilityType: AgentCapabilityType,
    val destination: SettingsDestination,
    val explanation: String
  ) : CapabilityExecutionCheck()

  data class Unavailable(
    val capabilityType: AgentCapabilityType,
    val reason: String
  ) : CapabilityExecutionCheck()
}

/**
 * Centralized Agent Capability Policy.
 * Decoupled from the UI. Enforces in-app user preference toggles, permission checks,
 * and confirmation requirements before any Android tool executes.
 */
class AgentCapabilityPolicy {

  // In-app capability enablement toggles (default enabled)
  private val _enabledCapabilities = MutableStateFlow<Map<AgentCapabilityType, Boolean>>(
    AgentCapabilityType.values().associateWith { true }
  )
  val enabledCapabilities: StateFlow<Map<AgentCapabilityType, Boolean>> = _enabledCapabilities.asStateFlow()

  // Confirmation mode
  private val _confirmationMode = MutableStateFlow(ActionConfirmationMode.NORMAL)
  val confirmationMode: StateFlow<ActionConfirmationMode> = _confirmationMode.asStateFlow()

  fun setCapabilityEnabled(type: AgentCapabilityType, enabled: Boolean) {
    val current = _enabledCapabilities.value.toMutableMap()
    current[type] = enabled
    _enabledCapabilities.value = current
  }

  fun isCapabilityEnabled(type: AgentCapabilityType): Boolean {
    return _enabledCapabilities.value[type] ?: true
  }

  fun setConfirmationMode(mode: ActionConfirmationMode) {
    _confirmationMode.value = mode
  }

  /**
   * Determines if the specific action requires user confirmation under current policy.
   */
  fun requiresConfirmation(actionType: ActionType): Boolean {
    val mode = _confirmationMode.value
    return when (mode) {
      ActionConfirmationMode.STRICT -> {
        actionType.isSensitive || actionType.isStateAltering
      }
      ActionConfirmationMode.NORMAL -> {
        actionType.isSensitive
      }
      ActionConfirmationMode.DIRECT -> {
        false
      }
    }
  }

  /**
   * Evaluates if a capability is allowed to execute right now.
   * Strict precedence:
   * 1. Hardware/OS Availability
   * 2. In-App User Toggle (Agent-level restriction)
   * 3. Android Runtime Permissions & Special Settings
   */
  fun checkExecution(context: Context, type: AgentCapabilityType): CapabilityExecutionCheck {
    val capability = CapabilityRegistry.findByType(type)
      ?: return CapabilityExecutionCheck.Unavailable(type, "Capability '${type.name}' is not registered.")

    // 1. Check in-app policy toggle first
    val isEnabledInAgent = isCapabilityEnabled(type)
    if (!isEnabledInAgent) {
      return CapabilityExecutionCheck.BlockedByUserInAgent(
        capabilityType = type,
        capabilityName = capability.name,
        reason = "${capability.name} access is granted or available in Android, but Agent ${capability.name.lowercase()} capability has been disabled in Agent Settings."
      )
    }

    // 2. Check Hardware/OS Availability
    if (!capability.isAvailableOnDevice(context)) {
      return CapabilityExecutionCheck.Unavailable(
        type,
        capability.unavailableReason ?: "${capability.name} is not supported or available on this hardware."
      )
    }

    // Check special system settings
    if (capability.settingsDestination == SettingsDestination.NOTIFICATION_LISTENER) {
      val status = capability.checkStatus(context, isEnabledInAgent = true)
      if (status == CapabilityStatus.REQUIRES_SETTINGS) {
        return CapabilityExecutionCheck.RequiresSettings(
          capabilityType = type,
          destination = SettingsDestination.NOTIFICATION_LISTENER,
          explanation = "Notification Listener Access must be enabled in Android System Settings to read notifications."
        )
      }
    } else if (capability.settingsDestination == SettingsDestination.ACCESSIBILITY) {
      val status = capability.checkStatus(context, isEnabledInAgent = true)
      if (status == CapabilityStatus.REQUIRES_SETTINGS) {
        return CapabilityExecutionCheck.RequiresSettings(
          capabilityType = type,
          destination = SettingsDestination.ACCESSIBILITY,
          explanation = "Accessibility Service must be enabled by the operator in Android System Settings."
        )
      }
    }

    // Check standard runtime permissions
    if (capability.requiredPermissions.isNotEmpty()) {
      val missingPermissions = capability.requiredPermissions.filter { perm ->
        androidx.core.content.ContextCompat.checkSelfPermission(context, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED
      }
      if (missingPermissions.isNotEmpty()) {
        return CapabilityExecutionCheck.PermissionRequired(
          capabilityType = type,
          permissions = missingPermissions,
          explanation = "Android permission '${missingPermissions.first().substringAfterLast('.')}' is required to proceed with ${capability.name}."
        )
      }
    }

    return CapabilityExecutionCheck.Allowed
  }
}
