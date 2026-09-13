package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.automation.AutomationEngine
import com.example.core.automation.condition.ConditionEngine
import com.example.core.automation.condition.DeviceTelemetrySnapshot
import com.example.core.automation.interpreter.AutomationInterpreter
import com.example.core.automation.interpreter.InterpreterResult
import com.example.core.automation.model.Automation
import com.example.core.automation.model.AutomationCategory
import com.example.core.automation.model.AutomationCondition
import com.example.core.automation.model.AutomationExecutionStatus
import com.example.core.automation.model.AutomationNotificationPolicy
import com.example.core.automation.model.AutomationState
import com.example.core.automation.model.AutomationTrigger
import com.example.core.automation.model.ExecutionPolicy
import com.example.core.automation.model.NetworkRequirementType
import com.example.core.automation.model.RecurrenceType
import com.example.core.automation.store.AutomationStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutomationEngineTest {

  private lateinit var context: Context
  private lateinit var store: AutomationStore
  private lateinit var interpreter: AutomationInterpreter
  private lateinit var conditionEngine: ConditionEngine

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    store = AutomationStore(context)
    interpreter = AutomationInterpreter()
    conditionEngine = ConditionEngine(context)
  }

  @Test
  fun `automation store preserves CRUD operations and history`() {
    val auto = Automation(
      id = "test-auto-1",
      name = "Daily Standup Brief",
      description = "Morning standup report",
      category = AutomationCategory.WORK,
      enabled = true,
      state = AutomationState.ENABLED,
      trigger = AutomationTrigger.TimeSchedule(
        timeHour = 9,
        timeMinute = 0,
        recurrence = RecurrenceType.DAILY
      ),
      actions = listOf(
        AgentAction(
          id = UUID.randomUUID().toString(),
          type = ActionType.SEND_NOTIFICATION,
          description = "Send daily standup notification",
          parameters = mapOf("title" to "Standup", "body" to "Time for standup"),
          requiredCapabilities = emptyList()
        )
      )
    )

    store.saveAutomation(auto)
    val retrieved = store.getAutomation("test-auto-1")
    assertNotNull(retrieved)
    assertEquals("Daily Standup Brief", retrieved?.name)
    assertEquals(AutomationCategory.WORK, retrieved?.category)
    assertTrue(retrieved?.enabled == true)

    // Update
    val updated = retrieved!!.copy(name = "Daily Standup Brief V2", enabled = false)
    store.saveAutomation(updated)
    assertEquals("Daily Standup Brief V2", store.getAutomation("test-auto-1")?.name)
    assertFalse(store.getAutomation("test-auto-1")?.enabled == true)

    // Delete
    store.deleteAutomation("test-auto-1")
    assertEquals(null, store.getAutomation("test-auto-1"))
  }

  @Test
  fun `interpreter correctly parses schedule prompt into draft automation`() {
    val result = interpreter.interpret("Every morning at 8:30 AM search tech news and notify me")
    assertTrue(result is InterpreterResult.DraftCreated)
    val draft = (result as InterpreterResult.DraftCreated).automation
    assertNotNull(draft)
    assertEquals(AutomationState.DRAFT, draft.state)
    assertFalse(draft.enabled) // Drafts are strictly disabled by default!
    val trigger = draft.trigger as AutomationTrigger.TimeSchedule
    assertEquals(8, trigger.timeHour)
    assertEquals(30, trigger.timeMinute)
    assertEquals(RecurrenceType.DAILY, trigger.recurrence)
    assertTrue(draft.actions.isNotEmpty())
  }

  @Test
  fun `interpreter handles automation list and status commands`() {
    val listResult = interpreter.interpret("List all routines")
    assertTrue(listResult is InterpreterResult.ManageCommand)

    val nonAuto = interpreter.interpret("What is the capital of France?")
    assertTrue(nonAuto is InterpreterResult.NotAnAutomationCommand)
  }

  @Test
  fun `condition engine evaluates device battery and network requirements accurately`() {
    val telemetry = DeviceTelemetrySnapshot(
      batteryLevel = 45,
      isCharging = false,
      isConnected = true,
      isWifi = true,
      timeHour = 14,
      timeMinute = 30
    )

    // Satisfied condition
    val validCondition = listOf(
      AutomationCondition.BatteryCondition(minLevel = 30, requireCharging = false),
      AutomationCondition.NetworkCondition(requirement = NetworkRequirementType.ANY_NETWORK)
    )
    val check1 = conditionEngine.evaluateConditions(validCondition, telemetry)
    assertTrue(check1.satisfied)

    // Failing condition: requires charging
    val failCharging = listOf(
      AutomationCondition.BatteryCondition(minLevel = 20, requireCharging = true)
    )
    val check2 = conditionEngine.evaluateConditions(failCharging, telemetry)
    assertFalse(check2.satisfied)
    assertNotNull(check2.failureReason)

    // Failing condition: requires battery >= 80%
    val failBattery = listOf(
      AutomationCondition.BatteryCondition(minLevel = 80, requireCharging = false)
    )
    val check3 = conditionEngine.evaluateConditions(failBattery, telemetry)
    assertFalse(check3.satisfied)
  }

  @Test
  fun `dry run execution validates sequence without performing real side effects`() = runBlocking {
    val engine = AutomationEngine(context)
    val auto = Automation(
      id = "test-dry-run",
      name = "Simulated Pipeline",
      description = "Test pipeline for simulation",
      category = AutomationCategory.PERSONAL,
      enabled = true,
      state = AutomationState.ENABLED,
      trigger = AutomationTrigger.ManualTrigger,
      actions = listOf(
        AgentAction(
          id = "step-1",
          type = ActionType.CHECK_BATTERY,
          description = "Telemetry step",
          parameters = emptyMap(),
          requiredCapabilities = emptyList()
        ),
        AgentAction(
          id = "step-2",
          type = ActionType.SEND_NOTIFICATION,
          description = "Send battery notification",
          parameters = mapOf("title" to "Battery", "body" to "Battery is \$battery"),
          requiredCapabilities = emptyList()
        )
      )
    )
    engine.store.saveAutomation(auto)

    val result = engine.dryRunAutomation("test-dry-run")
    assertNotNull(result)
    assertEquals(AutomationExecutionStatus.SUCCESS, result.status)
    assertEquals(2, result.history.totalSteps)
    assertEquals(2, result.history.completedSteps)
  }
}
