package com.example.core.automation

import android.content.Context
import com.example.core.ai.AIEngineContainer
import com.example.core.automation.condition.ConditionEngine
import com.example.core.automation.interpreter.AutomationInterpreter
import com.example.core.automation.interpreter.InterpreterResult
import com.example.core.automation.interpreter.ManageCommandType
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationExecutionStatus
import com.example.core.automation.model.AutomationHistory
import com.example.core.automation.model.AutomationState
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.routine.RoutineEngine
import com.example.core.automation.routine.RoutineExecutionResult
import com.example.core.automation.scheduler.AutomationScheduler
import com.example.core.automation.store.AutomationStore
import com.example.core.tool.ToolRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Patch 6 Automation Engine Core.
 * Coordinates persistence, AlarmManager scheduling, condition checks,
 * multi-step routine execution, and natural language command parsing.
 */
class AutomationEngine(
  private val context: Context,
  private val aiContainer: AIEngineContainer? = null
) {

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val executionMutex = Mutex()

  val store: AutomationStore = AutomationStore(context)
  val scheduler: AutomationScheduler = AutomationScheduler(context)

  val conditionEngine: ConditionEngine = ConditionEngine(
    context = context,
    memoryManager = aiContainer?.memoryManager,
    aiRouter = aiContainer?.router
  )

  val routineEngine: RoutineEngine = RoutineEngine(
    context = context,
    aiRouter = aiContainer?.router,
    researchEngine = aiContainer?.researchEngine,
    memoryManager = aiContainer?.memoryManager
  )

  val interpreter: AutomationInterpreter = AutomationInterpreter()

  val automations: StateFlow<List<Automation>> = store.automations
  val history: StateFlow<List<AutomationHistory>> = store.history

  init {
    reconcileSchedules()
  }

  fun reconcileSchedules() {
    val currentList = store.automations.value
    scheduler.reconcileAllSchedules(currentList)
  }

  fun saveAutomation(automation: Automation) {
    store.saveAutomation(automation)
    if (automation.enabled) {
      val nextRun = scheduler.scheduleAutomation(automation)
      if (nextRun != null) {
        store.updateRunStats(automation.id, success = true, nextRunAt = nextRun)
      }
    } else {
      scheduler.cancelSchedule(automation.id)
    }
  }

  fun setAutomationEnabled(id: String, enabled: Boolean) {
    val state = if (enabled) AutomationState.ENABLED else AutomationState.DISABLED
    store.updateState(id, state)
    val auto = store.getAutomation(id) ?: return
    if (enabled) {
      val nextRun = scheduler.scheduleAutomation(auto)
      if (nextRun != null) {
        store.updateRunStats(id, success = true, nextRunAt = nextRun)
      }
    } else {
      scheduler.cancelSchedule(id)
    }
  }

  fun deleteAutomation(id: String) {
    scheduler.cancelSchedule(id)
    store.deleteAutomation(id)
  }

  fun duplicateAutomation(id: String): Automation? {
    return store.duplicateAutomation(id)
  }

  suspend fun runAutomationNow(id: String): RoutineExecutionResult {
    val auto = store.getAutomation(id)
      ?: return createFailedResult("Automation not found with ID: $id")

    return executeAutomationInternal(auto, isDryRun = false, triggerSource = "MANUAL")
  }

  suspend fun dryRunAutomation(id: String): RoutineExecutionResult {
    val auto = store.getAutomation(id)
      ?: return createFailedResult("Automation not found with ID: $id")

    return routineEngine.executeRoutine(auto, isDryRun = true, triggerSource = "DRY_RUN")
  }

  suspend fun handleScheduledTrigger(automationId: String, scheduledTime: Long) {
    val auto = store.getAutomation(automationId) ?: return
    if (!auto.enabled) return

    // Execute routine
    executeAutomationInternal(auto, isDryRun = false, triggerSource = auto.trigger.triggerType)

    // Reschedule next occurrence if recurring
    if (auto.trigger is AutomationTrigger.TimeSchedule) {
      val nextRun = scheduler.scheduleAutomation(auto)
      store.updateRunStats(automationId, success = true, nextRunAt = nextRun)
    }
  }

  private suspend fun executeAutomationInternal(
    automation: Automation,
    isDryRun: Boolean,
    triggerSource: String
  ): RoutineExecutionResult = executionMutex.withLock {
    // 1. Condition verification
    val conditionResult = conditionEngine.evaluateAll(automation.conditions, automation.conditionLogic)
    if (!conditionResult.satisfied) {
      val skipHistory = AutomationHistory(
        automationId = automation.id,
        automationName = automation.name,
        triggerType = triggerSource,
        status = AutomationExecutionStatus.SKIPPED,
        startedAt = System.currentTimeMillis(),
        durationMs = 0L,
        completedSteps = 0,
        totalSteps = automation.actions.size,
        errorMessage = conditionResult.failureReason ?: "Conditions not met"
      )
      store.recordHistory(skipHistory)
      return RoutineExecutionResult(
        status = AutomationExecutionStatus.SKIPPED,
        history = skipHistory,
        outputMessage = "Routine skipped: ${conditionResult.failureReason}"
      )
    }

    // 2. Set RUNNING state
    store.updateState(automation.id, AutomationState.RUNNING)

    // 3. Execute routine chain
    val result = routineEngine.executeRoutine(automation, isDryRun, triggerSource)

    // 4. Record history & update stats
    store.recordHistory(result.history)
    val success = result.status == AutomationExecutionStatus.SUCCESS || result.status == AutomationExecutionStatus.PARTIAL_SUCCESS
    store.updateRunStats(automation.id, success, nextRunAt = automation.nextRunAt)

    // 5. Restore ENABLED state
    val finalState = if (automation.enabled) AutomationState.ENABLED else AutomationState.DISABLED
    store.updateState(automation.id, finalState)

    result
  }

  fun interpretUserCommand(query: String): InterpreterResult {
    val result = interpreter.interpret(query)
    when (result) {
      is InterpreterResult.DraftCreated -> {
        // Save as draft so user can see it in UI
        store.saveAutomation(result.automation)
      }
      is InterpreterResult.ManageCommand -> {
        handleManageCommand(result)
      }
      else -> {}
    }
    return result
  }

  private fun handleManageCommand(cmd: InterpreterResult.ManageCommand) {
    when (cmd.commandType) {
      ManageCommandType.ENABLE -> {
        val target = cmd.targetNameOrId ?: return
        val match = findAutomationByNameOrId(target) ?: return
        setAutomationEnabled(match.id, true)
      }
      ManageCommandType.DISABLE -> {
        val target = cmd.targetNameOrId ?: return
        val match = findAutomationByNameOrId(target) ?: return
        setAutomationEnabled(match.id, false)
      }
      ManageCommandType.DELETE -> {
        val target = cmd.targetNameOrId ?: return
        val match = findAutomationByNameOrId(target) ?: return
        deleteAutomation(match.id)
      }
      ManageCommandType.RUN_NOW -> {
        val target = cmd.targetNameOrId ?: return
        val match = findAutomationByNameOrId(target) ?: return
        scope.launch { runAutomationNow(match.id) }
      }
      ManageCommandType.LIST_ALL -> {}
    }
  }

  private fun findAutomationByNameOrId(query: String): Automation? {
    val list = store.automations.value
    return list.firstOrNull { it.id == query }
      ?: list.firstOrNull { it.name.contains(query, ignoreCase = true) }
  }

  fun exportAutomations(): String = store.exportAutomations()

  fun importAutomations(json: String): Int = store.importAutomations(json)

  fun clearHistory(automationId: String? = null) = store.clearHistory(automationId)

  private fun createFailedResult(error: String): RoutineExecutionResult {
    val history = AutomationHistory(
      automationId = "unknown",
      automationName = "Unknown",
      triggerType = "MANUAL",
      status = AutomationExecutionStatus.FAILED,
      startedAt = System.currentTimeMillis(),
      durationMs = 0L,
      completedSteps = 0,
      totalSteps = 0,
      errorMessage = error
    )
    return RoutineExecutionResult(
      status = AutomationExecutionStatus.FAILED,
      history = history,
      outputMessage = error
    )
  }

  companion object {
    @Volatile
    private var INSTANCE: AutomationEngine? = null

    fun getInstance(context: Context, aiContainer: AIEngineContainer? = null): AutomationEngine {
      return INSTANCE ?: synchronized(this) {
        val container = aiContainer ?: AIEngineContainer.getInstance(context)
        val inst = AutomationEngine(context.applicationContext, container)
        INSTANCE = inst
        inst
      }
    }
  }
}
