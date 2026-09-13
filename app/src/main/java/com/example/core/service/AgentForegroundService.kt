package com.example.core.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.background.BackgroundAgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Standard Android Foreground Service for voice background assistant.
 * Adheres strictly to Android 14+ foreground service types and truthful state reporting.
 */
class AgentForegroundService : Service() {

  companion object {
    const val CHANNEL_ID = "nexus_agent_foreground_channel"
    const val NOTIFICATION_ID = 4042

    const val ACTION_START = "com.example.action.START_AGENT_SERVICE"
    const val ACTION_STOP = "com.example.action.STOP_AGENT_SERVICE"

    private val _serviceState = MutableStateFlow(BackgroundAgentState.STOPPED)
    val serviceState: StateFlow<BackgroundAgentState> = _serviceState.asStateFlow()

    fun startService(context: Context) {
      val intent = Intent(context, AgentForegroundService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, AgentForegroundService::class.java).apply {
        action = ACTION_STOP
      }
      context.stopService(intent)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopCleanly()
        return START_NOT_STICKY
      }
      else -> {
        startForegroundExecution()
      }
    }
    return START_STICKY
  }

  private fun startForegroundExecution() {
    _serviceState.value = BackgroundAgentState.STARTING

    // Check permissions
    val hasMic = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasMic) {
      _serviceState.value = BackgroundAgentState.NOT_PERMITTED
      stopSelf()
      return
    }

    try {
      val notification = buildNotification("Agent Active", "Voice assistant listening for authorized triggers")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
          )
        } else {
          startForeground(NOTIFICATION_ID, notification)
        }
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
      _serviceState.value = BackgroundAgentState.RUNNING
      com.example.core.ai.AIEngineContainer.getInstance(this.applicationContext)
    } catch (e: Exception) {
      _serviceState.value = BackgroundAgentState.ERROR
      stopSelf()
    }
  }

  private fun stopCleanly() {
    _serviceState.value = BackgroundAgentState.STOPPED
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
    stopSelf()
  }

  override fun onDestroy() {
    super.onDestroy()
    _serviceState.value = BackgroundAgentState.STOPPED
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "AI Agent Background Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Displays running status of the AI voice assistant"
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(title: String, content: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(content)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }
}
