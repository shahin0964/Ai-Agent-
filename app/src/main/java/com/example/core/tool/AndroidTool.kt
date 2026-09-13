package com.example.core.tool

import android.content.Context
import com.example.core.action.ToolResult
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.capability.AgentCapabilityType
import com.example.core.capability.CapabilityExecutionCheck

/**
 * Base interface for all Android Agent Tools.
 * Follows the core principle:
 * Understand -> Check Capability/Policy -> Verify Permission -> Execute -> Verify Result -> Report.
 */
interface AndroidTool {
  val id: String
  val name: String
  val description: String
  val requiredCapabilities: List<AgentCapabilityType>

  /**
   * Evaluates if the tool is authorized and technically capable of executing right now.
   */
  fun canExecute(context: Context, policy: AgentCapabilityPolicy): CapabilityExecutionCheck {
    for (cap in requiredCapabilities) {
      val check = policy.checkExecution(context, cap)
      if (check !is CapabilityExecutionCheck.Allowed) {
        return check
      }
    }
    return CapabilityExecutionCheck.Allowed
  }

  /**
   * Executes the tool with the provided arguments.
   */
  suspend fun execute(
    context: Context,
    parameters: Map<String, Any?>,
    policy: AgentCapabilityPolicy
  ): ToolResult
}
