package com.example.core.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.core.service.AgentForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Truthful background operational states.
 * Section 16: The UI must display the actual state without claims of "Always listening"
 * unless genuinely operating in that mode.
 */
enum class BackgroundPresenceState(val label: String, val description: String) {
  NOT_RUNNING("Not Running", "Background service is inactive."),
  STARTING("Starting", "Allocating foreground service notification and channel."),
  RUNNING("Running", "Active foreground service observing authorized triggers."),
  STOPPING("Stopping", "Service shutting down cleanly."),
  BLOCKED("Blocked", "System battery optimization or background execution restrictions active."),
  PERMISSION_REQUIRED("Permission Required", "Background microphone or notification permission not granted."),
  UNAVAILABLE("Unavailable", "Background service not supported on this device profile.")
}

class BackgroundPresenceController(
  private val context: Context,
  private val onServiceRunningChanged: (Boolean) -> Unit = {}
) {
  private val _presenceState = MutableStateFlow(BackgroundPresenceState.NOT_RUNNING)
  val presenceState: StateFlow<BackgroundPresenceState> = _presenceState.asStateFlow()

  val isRunning: Boolean
    get() = _presenceState.value == BackgroundPresenceState.RUNNING

  init {
    // Check initial permission & state
    refreshStatus()
  }

  fun refreshStatus() {
    val currentFgsState = AgentForegroundService.serviceState.value
    _presenceState.value = when (currentFgsState) {
      BackgroundAgentState.RUNNING -> BackgroundPresenceState.RUNNING
      BackgroundAgentState.STARTING -> BackgroundPresenceState.STARTING
      BackgroundAgentState.STOPPED -> BackgroundPresenceState.NOT_RUNNING
      BackgroundAgentState.NOT_PERMITTED -> BackgroundPresenceState.PERMISSION_REQUIRED
      BackgroundAgentState.ERROR -> BackgroundPresenceState.BLOCKED
      BackgroundAgentState.PAUSED -> BackgroundPresenceState.NOT_RUNNING
    }
    onServiceRunningChanged(_presenceState.value == BackgroundPresenceState.RUNNING)
  }

  fun startBackgroundPresence(): BackgroundPresenceState {
    // Check permissions
    val hasMic = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    if (!hasMic || !hasNotif) {
      _presenceState.value = BackgroundPresenceState.PERMISSION_REQUIRED
      return BackgroundPresenceState.PERMISSION_REQUIRED
    }

    try {
      _presenceState.value = BackgroundPresenceState.STARTING
      AgentForegroundService.startService(context)
      _presenceState.value = BackgroundPresenceState.RUNNING
      onServiceRunningChanged(true)
      return BackgroundPresenceState.RUNNING
    } catch (e: Exception) {
      _presenceState.value = BackgroundPresenceState.BLOCKED
      onServiceRunningChanged(false)
      return BackgroundPresenceState.BLOCKED
    }
  }

  fun stopBackgroundPresence() {
    _presenceState.value = BackgroundPresenceState.STOPPING
    AgentForegroundService.stopService(context)
    _presenceState.value = BackgroundPresenceState.NOT_RUNNING
    onServiceRunningChanged(false)
  }
}
