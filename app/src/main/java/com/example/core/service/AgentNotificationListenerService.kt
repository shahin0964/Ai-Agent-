package com.example.core.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AgentNotificationListenerService : NotificationListenerService() {
  companion object {
    private const val TAG = "AgentNotifListener"
    var activeListenerInstance: AgentNotificationListenerService? = null
        private set
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    activeListenerInstance = this
    Log.i(TAG, "Notification listener connected")
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    if (activeListenerInstance == this) {
      activeListenerInstance = null
    }
    Log.i(TAG, "Notification listener disconnected")
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    super.onNotificationPosted(sbn)
    if (sbn == null) return
    val packageName = sbn.packageName
    val extras = sbn.notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    Log.d(TAG, "Notification posted from $packageName: $title - $text")
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
  }
}
