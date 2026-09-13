package com.example.core.overlay

/**
 * Architectural visual styles for future floating assistant overlay.
 */
enum class OverlayStyle(val displayName: String, val description: String) {
  MINIMAL_ORB("Minimal Orb", "Subtle glowing floating sphere with idle pulse"),
  FUTURISTIC_HUD("Futuristic HUD", "Semi-transparent sci-fi ring with telemetry indicators"),
  WAVEFORM("Waveform", "Sound-reactive horizontal audio visualization bar"),
  GLASS_ASSISTANT("Glass Assistant", "Translucent glass card displaying recent response"),
  FULL_ASSISTANT("Full Assistant", "Expandable floating command terminal with voice & actions")
}

/**
 * Configuration descriptor for the floating assistant overlay.
 */
data class OverlayAssistantConfig(
  val isOverlayEnabled: Boolean = false,
  val selectedStyle: OverlayStyle = OverlayStyle.MINIMAL_ORB,
  val autoCollapseOnInactivity: Boolean = true,
  val triggerMode: String = "Back Gesture / Long Press",
  val architectureNote: String = "Requires Android SYSTEM_ALERT_WINDOW permission"
)
