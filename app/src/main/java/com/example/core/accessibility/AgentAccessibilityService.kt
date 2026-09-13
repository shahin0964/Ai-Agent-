package com.example.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Production Android AccessibilityService for Nexus AI Agent.
 * Exposes real connection state to the Agent Tool & Permission System.
 */
class AgentAccessibilityService : AccessibilityService() {

  companion object {
    @Volatile
    var isServiceConnected: Boolean = false
      private set

    @Volatile
    var instance: AgentAccessibilityService? = null
      private set

    fun findAndClickText(text: String): Boolean {
      val service = instance ?: return false
      val root = service.rootInActiveWindow ?: return false
      val nodes = root.findAccessibilityNodeInfosByText(text)
      if (nodes.isNullOrEmpty()) return false

      for (node in nodes) {
        if (node.isClickable) {
          return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
          if (parent.isClickable) {
            return parent.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
          }
          parent = parent.parent
        }
      }
      return false
    }

    fun readActiveWindowText(): String {
      val service = instance ?: return "Accessibility Service inactive"
      val root = service.rootInActiveWindow ?: return "Active window content unavailable"
      val sb = StringBuilder()
      collectText(root, sb, 0)
      return if (sb.isBlank()) "No readable text elements in active window" else sb.toString()
    }

    private fun collectText(node: android.view.accessibility.AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
      if (node == null || depth > 10) return
      val text = node.text?.toString()
      val description = node.contentDescription?.toString()
      if (!text.isNullOrBlank()) {
        sb.append(text).append("\n")
      } else if (!description.isNullOrBlank()) {
        sb.append(description).append("\n")
      }
      for (i in 0 until node.childCount) {
        collectText(node.getChild(i), sb, depth + 1)
      }
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    isServiceConnected = true
    instance = this
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Accessibility events processed strictly upon authorized user action requests
  }

  override fun onInterrupt() {
    // Service interrupted
  }

  override fun onUnbind(intent: android.content.Intent?): Boolean {
    isServiceConnected = false
    instance = null
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    isServiceConnected = false
    instance = null
  }
}
