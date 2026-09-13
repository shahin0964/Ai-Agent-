package com.example.core.companion

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.core.model.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presence states representing the genuine runtime operational mode of the agent.
 * Section 8: Presence must reflect actual application and service state.
 */
enum class PresenceState(val label: String, val description: String) {
  ACTIVE("Active", "Foreground session active with recent interaction."),
  IDLE("Idle", "Foreground session active; awaiting input."),
  AWAY("Away", "App in background without active background service."),
  SLEEPING("Sleeping", "Scheduled quiet mode active; inputs minimized."),
  OFFLINE("Offline", "Device disconnected from networks."),
  BACKGROUND("Background Presence", "Foreground service running in notification tray."),
  LISTENING("Listening", "Microphone actively capturing audio stream."),
  BUSY("Busy", "Executing inference or device tool capabilities.")
}

/**
 * Manages and synchronizes truthful presence states across application lifecycle,
 * background service, network connectivity, and agent execution.
 */
class PresenceManager(
  private val context: Context
) {
  private val _presenceState = MutableStateFlow(PresenceState.ACTIVE)
  val presenceState: StateFlow<PresenceState> = _presenceState.asStateFlow()

  private var isAppInForeground: Boolean = true
  private var isBackgroundServiceRunning: Boolean = false
  private var isDeviceSleeping: Boolean = false
  private var isNetworkAvailable: Boolean = true

  init {
    monitorNetworkState()
  }

  fun onAppForeground() {
    isAppInForeground = true
    recomputePresence(AgentState.IDLE)
  }

  fun onAppBackground() {
    isAppInForeground = false
    recomputePresence(AgentState.IDLE)
  }

  fun onBackgroundServiceStateChanged(running: Boolean) {
    isBackgroundServiceRunning = running
    recomputePresence(AgentState.IDLE)
  }

  fun onSleepStateChanged(sleeping: Boolean) {
    isDeviceSleeping = sleeping
    recomputePresence(AgentState.IDLE)
  }

  fun updateFromAgentState(agentState: AgentState) {
    recomputePresence(agentState)
  }

  private fun recomputePresence(agentState: AgentState) {
    _presenceState.value = when {
      !isNetworkAvailable && agentState != AgentState.LISTENING && agentState != AgentState.PROCESSING ->
        PresenceState.OFFLINE

      isDeviceSleeping ->
        PresenceState.SLEEPING

      agentState == AgentState.LISTENING ->
        PresenceState.LISTENING

      agentState == AgentState.PROCESSING || agentState == AgentState.INITIALIZING ->
        PresenceState.BUSY

      isAppInForeground ->
        if (agentState == AgentState.SPEAKING) PresenceState.ACTIVE else PresenceState.IDLE

      isBackgroundServiceRunning ->
        PresenceState.BACKGROUND

      else ->
        PresenceState.AWAY
    }
  }

  private fun monitorNetworkState() {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm != null) {
      val activeNetwork = cm.activeNetwork
      val caps = cm.getNetworkCapabilities(activeNetwork)
      isNetworkAvailable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

      val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

      cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          isNetworkAvailable = true
          recomputePresence(AgentState.IDLE)
        }

        override fun onLost(network: Network) {
          isNetworkAvailable = false
          recomputePresence(AgentState.IDLE)
        }
      })
    }
  }
}
