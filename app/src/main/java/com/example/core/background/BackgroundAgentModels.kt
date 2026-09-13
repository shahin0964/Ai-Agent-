package com.example.core.background

/**
 * Truthful status of background assistant service.
 */
enum class BackgroundAgentState(val label: String, val description: String) {
  STOPPED("Stopped", "Background assistant service is inactive."),
  STARTING("Starting...", "Allocating foreground service notification and channel."),
  RUNNING("Running", "Active foreground service observing authorized triggers."),
  PAUSED("Paused", "Service standby; microphone pipeline released."),
  ERROR("Error", "Service encountered a configuration or system error."),
  NOT_PERMITTED("Permission Required", "Background microphone or notification permission not granted.")
}
