package com.example.core.overlay

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.core.model.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Visual modes for the Siri-like floating overlay HUD.
 * Section 23: MINIMAL_ORB, FUTURISTIC_HUD, WAVEFORM, GLASS_ASSISTANT, FULL_ASSISTANT
 */
enum class FloatingVisualMode(val displayName: String, val description: String) {
  MINIMAL_ORB("Minimal Orb", "Subtle glowing orb hovering at screen periphery."),
  FUTURISTIC_HUD("Futuristic HUD", "Sci-Fi holographic ring with live telemetry metrics."),
  WAVEFORM("Waveform Strip", "Dynamic frequency audio spectrum visualizer bar."),
  GLASS_ASSISTANT("Glass Assistant", "Translucent glass card with transcription view."),
  FULL_ASSISTANT("Full Assistant", "Expanded tactical HUD pane with interactive quick actions.")
}

/**
 * Truthful runtime states of the floating assistant.
 * Section 25: HIDDEN, VISIBLE, LISTENING, THINKING, SPEAKING, ERROR, OFFLINE
 */
enum class FloatingAssistantState(val label: String) {
  HIDDEN("Hidden"),
  VISIBLE("Visible"),
  LISTENING("Listening"),
  THINKING("Thinking"),
  SPEAKING("Speaking"),
  ERROR("Error"),
  OFFLINE("Offline")
}

enum class OverlayPosition(val label: String) {
  TOP_RIGHT("Top Right"),
  CENTER_RIGHT("Center Right"),
  BOTTOM_RIGHT("Bottom Right"),
  BOTTOM_LEFT("Bottom Left")
}

enum class OverlaySize(val label: String) {
  COMPACT("Compact (48dp)"),
  STANDARD("Standard (64dp)"),
  EXPANDED("Expanded (96dp)")
}

data class FloatingAssistantConfig(
  val isOverlayEnabled: Boolean = false,
  val visualMode: FloatingVisualMode = FloatingVisualMode.MINIMAL_ORB,
  val position: OverlayPosition = OverlayPosition.BOTTOM_RIGHT,
  val size: OverlaySize = OverlaySize.STANDARD,
  val opacity: Float = 0.9f
)

class FloatingAssistantController(private val context: Context) {
  companion object {
    @Volatile
    var activeController: FloatingAssistantController? = null
      private set
  }

  init {
    activeController = this
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences("aegis_floating_assistant_prefs", Context.MODE_PRIVATE)

  private val _config = MutableStateFlow(loadConfig())
  val config: StateFlow<FloatingAssistantConfig> = _config.asStateFlow()

  private val _state = MutableStateFlow(FloatingAssistantState.HIDDEN)
  val state: StateFlow<FloatingAssistantState> = _state.asStateFlow()

  fun hasOverlayPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(context)
    } else {
      true
    }
  }

  fun getOverlaySettingsIntent(): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
      ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(Settings.ACTION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }
  }

  private fun loadConfig(): FloatingAssistantConfig {
    val modeName = prefs.getString("visual_mode", FloatingVisualMode.MINIMAL_ORB.name)
    val mode = try { FloatingVisualMode.valueOf(modeName ?: "") } catch (_: Exception) { FloatingVisualMode.MINIMAL_ORB }

    val posName = prefs.getString("position", OverlayPosition.BOTTOM_RIGHT.name)
    val pos = try { OverlayPosition.valueOf(posName ?: "") } catch (_: Exception) { OverlayPosition.BOTTOM_RIGHT }

    val sizeName = prefs.getString("size", OverlaySize.STANDARD.name)
    val size = try { OverlaySize.valueOf(sizeName ?: "") } catch (_: Exception) { OverlaySize.STANDARD }

    return FloatingAssistantConfig(
      isOverlayEnabled = prefs.getBoolean("overlay_enabled", false),
      visualMode = mode,
      position = pos,
      size = size,
      opacity = prefs.getFloat("opacity", 0.9f)
    )
  }

  fun updateConfig(newConfig: FloatingAssistantConfig) {
    prefs.edit()
      .putBoolean("overlay_enabled", newConfig.isOverlayEnabled)
      .putString("visual_mode", newConfig.visualMode.name)
      .putString("position", newConfig.position.name)
      .putString("size", newConfig.size.name)
      .putFloat("opacity", newConfig.opacity)
      .apply()

    _config.value = newConfig
    if (newConfig.isOverlayEnabled && hasOverlayPermission()) {
      OverlayService.start(context)
    } else {
      _state.value = FloatingAssistantState.HIDDEN
      OverlayService.stop(context)
    }
  }

  fun syncWithAgentState(agentState: AgentState, isOffline: Boolean = false) {
    if (!_config.value.isOverlayEnabled || !hasOverlayPermission()) {
      _state.value = FloatingAssistantState.HIDDEN
      OverlayService.stop(context)
      return
    }

    _state.value = when {
      isOffline -> FloatingAssistantState.OFFLINE
      agentState == AgentState.LISTENING -> FloatingAssistantState.LISTENING
      agentState == AgentState.PROCESSING -> FloatingAssistantState.THINKING
      agentState == AgentState.SPEAKING -> FloatingAssistantState.SPEAKING
      agentState == AgentState.ERROR -> FloatingAssistantState.ERROR
      else -> FloatingAssistantState.VISIBLE
    }
    OverlayService.start(context)
  }
}
