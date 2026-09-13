package com.example.core.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder

import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity

class OverlayService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {

  private var windowManager: WindowManager? = null
  private var overlayView: View? = null
  private var isAttached = false
  private var windowParams: WindowManager.LayoutParams? = null

  private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
  private val viewModelStoreInstance = ViewModelStore()
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle get() = lifecycleRegistry
  override val viewModelStore: ViewModelStore get() = viewModelStoreInstance
  override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

  companion object {
    const val ACTION_START_OVERLAY = "com.example.action.START_OVERLAY"
    const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

    fun start(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        return
      }
      val intent = Intent(context, OverlayService::class.java).apply {
        action = ACTION_START_OVERLAY
      }
      try {
        context.startService(intent)
      } catch (_: Exception) {}
    }

    fun stop(context: Context) {
      val intent = Intent(context, OverlayService::class.java).apply {
        action = ACTION_STOP_OVERLAY
      }
      try {
        context.stopService(intent)
      } catch (_: Exception) {}
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP_OVERLAY -> {
        removeOverlayView()
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        showOverlayView()
      }
    }
    return START_STICKY
  }

  private fun showOverlayView() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      stopSelf()
      return
    }

    if (isAttached || windowManager == null) return

    val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val controller = FloatingAssistantController.activeController ?: FloatingAssistantController(this.applicationContext)
    val initPosition = controller.config.value.position
    val initGravity = when (initPosition) {
      OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
      OverlayPosition.CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
      OverlayPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
      OverlayPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      windowType,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = initGravity
      x = 32
      y = 120
    }
    windowParams = params

    val composeView = ComposeView(this).apply {
      setViewTreeLifecycleOwner(this@OverlayService)
      setViewTreeViewModelStoreOwner(this@OverlayService)
      setViewTreeSavedStateRegistryOwner(this@OverlayService)

      setContent {
        val configState = controller.config.collectAsState()
        val currentConfig = configState.value

        val assistantStateVal = controller.state.collectAsState()
        val currentAssistantState = assistantStateVal.value

        androidx.compose.runtime.LaunchedEffect(currentConfig.position) {
          val newGravity = when (currentConfig.position) {
            OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
            OverlayPosition.CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
            OverlayPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
            OverlayPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
          }
          windowParams?.let { p ->
            if (p.gravity != newGravity) {
              p.gravity = newGravity
              if (isAttached && overlayView != null) {
                try {
                  windowManager?.updateViewLayout(overlayView, p)
                } catch (_: Exception) {}
              }
            }
          }
        }

        val gravityVal = when (currentConfig.position) {
          OverlayPosition.TOP_RIGHT -> Gravity.TOP or Gravity.END
          OverlayPosition.CENTER_RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
          OverlayPosition.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
          OverlayPosition.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
        }

        val baseSizeDp = when (currentConfig.size) {
          OverlaySize.COMPACT -> 48.dp
          OverlaySize.STANDARD -> 64.dp
          OverlaySize.EXPANDED -> 96.dp
        }

        val stateColor = when (currentAssistantState) {
          FloatingAssistantState.LISTENING -> Color(0xFF00F0FF)
          FloatingAssistantState.THINKING -> Color(0xFFA855F7)
          FloatingAssistantState.SPEAKING -> Color(0xFF10B981)
          FloatingAssistantState.ERROR -> Color(0xFFEF4444)
          FloatingAssistantState.OFFLINE -> Color(0xFF6B7280)
          else -> Color(0xFF00F0FF)
        }

        val openIntentAction = {
          val openIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
          }
          startActivity(openIntent)
        }

        Box(
          modifier = Modifier
            .graphicsLayer(alpha = currentConfig.opacity)
            .clickable { openIntentAction() },
          contentAlignment = Alignment.Center
        ) {
          when (currentConfig.visualMode) {
            FloatingVisualMode.MINIMAL_ORB -> {
              Box(
                modifier = Modifier
                  .size(baseSizeDp)
                  .background(stateColor, shape = CircleShape)
                  .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = "Floating Assistant Orb",
                  tint = Color(0xFF030712),
                  modifier = Modifier.size(baseSizeDp * 0.5f)
                )
              }
            }

            FloatingVisualMode.FUTURISTIC_HUD -> {
              Box(
                modifier = Modifier
                  .size(baseSizeDp + 16.dp)
                  .background(Color(0xCC030712), shape = CircleShape)
                  .border(2.dp, stateColor, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "HUD Center",
                    tint = stateColor,
                    modifier = Modifier.size(baseSizeDp * 0.45f)
                  )
                  Text(
                    text = currentAssistantState.label.uppercase(),
                    style = androidx.compose.ui.text.TextStyle(
                      color = stateColor,
                      fontSize = 8.sp,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                  )
                }
              }
            }

            FloatingVisualMode.WAVEFORM -> {
              Row(
                modifier = Modifier
                  .height(baseSizeDp)
                  .background(Color(0xEE030712), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                  .border(1.dp, stateColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                  .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                repeat(5) { index ->
                  val barHeight = when (currentAssistantState) {
                    FloatingAssistantState.LISTENING -> (12 + (index * 6) % 20).dp
                    FloatingAssistantState.SPEAKING -> (16 + (index * 8) % 24).dp
                    FloatingAssistantState.THINKING -> 14.dp
                    else -> 8.dp
                  }
                  Box(
                    modifier = Modifier
                      .width(4.dp)
                      .height(barHeight)
                      .background(stateColor, shape = CircleShape)
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = "Waveform Assistant",
                  tint = stateColor,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            FloatingVisualMode.GLASS_ASSISTANT -> {
              Row(
                modifier = Modifier
                  .background(Color(0xDD0F172A), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                  .border(1.5.dp, stateColor.copy(alpha = 0.8f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                  .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .background(stateColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "NEXUS AI",
                    style = androidx.compose.ui.text.TextStyle(
                      color = Color.White,
                      fontSize = 11.sp,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                  )
                  Text(
                    text = currentAssistantState.label,
                    style = androidx.compose.ui.text.TextStyle(
                      color = stateColor,
                      fontSize = 9.sp
                    )
                  )
                }
              }
            }

            FloatingVisualMode.FULL_ASSISTANT -> {
              Column(
                modifier = Modifier
                  .width(160.dp)
                  .background(Color(0xF0030712), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                  .border(1.5.dp, stateColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                  .padding(12.dp),
                horizontalAlignment = Alignment.Start
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "NEXUS TACTICAL HUD",
                    style = androidx.compose.ui.text.TextStyle(
                      color = Color.White,
                      fontSize = 9.sp,
                      fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Status: ${currentAssistantState.label}",
                  style = androidx.compose.ui.text.TextStyle(
                    color = stateColor,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                  )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Tap to open assistant panel",
                  style = androidx.compose.ui.text.TextStyle(
                    color = Color.LightGray,
                    fontSize = 8.sp
                  )
                )
              }
            }
          }
        }
      }
    }

    try {
      windowManager?.addView(composeView, params)
      overlayView = composeView
      isAttached = true
    } catch (_: Exception) {
      stopSelf()
    }
  }

  private fun removeOverlayView() {
    if (isAttached && overlayView != null) {
      try {
        windowManager?.removeView(overlayView)
      } catch (_: Exception) {}
      overlayView = null
      isAttached = false
    }
  }

  override fun onDestroy() {
    removeOverlayView()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    super.onDestroy()
  }
}
