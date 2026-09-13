package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.action.ActionType
import com.example.core.action.CommandParser
import com.example.core.agent.Phase1AgentController
import com.example.core.capability.ActionConfirmationMode
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.capability.AgentCapabilityType
import com.example.core.capability.CapabilityCategory
import com.example.core.capability.CapabilityExecutionCheck
import com.example.core.capability.CapabilityRegistry
import com.example.core.capability.CapabilityStatus
import com.example.core.model.AgentState
import com.example.core.model.CoreState
import com.example.core.model.toCoreState
import com.example.core.provider.ProviderRegistry
import com.example.core.tool.ToolRegistry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Agent", appName)
  }

  @Test
  fun `agent controller initializes in idle state without fake backend claims`() {
    val controller = Phase1AgentController()
    assertEquals(CoreState.IDLE, controller.coreState.value)
    assertEquals("Nexus Core", controller.agentIdentity.value.name)
    assertEquals(0, controller.chatMessages.value.size)
  }

  @Test
  fun `agent controller cycles voice interaction states correctly`() {
    val controller = Phase1AgentController()
    controller.toggleVoiceInteraction()
    assertEquals(CoreState.LISTENING, controller.coreState.value)

    controller.toggleVoiceInteraction()
    assertEquals(CoreState.THINKING, controller.coreState.value)

    controller.toggleVoiceInteraction()
    assertEquals(CoreState.SPEAKING, controller.coreState.value)

    controller.toggleVoiceInteraction()
    assertEquals(CoreState.IDLE, controller.coreState.value)
  }

  @Test
  fun `capability registry catalogs android capabilities truthfully`() {
    val capabilities = CapabilityRegistry.allCapabilities
    assertTrue(capabilities.size >= 14)
    assertNotNull(capabilities.find { it.id == "microphone" })
    assertNotNull(capabilities.find { it.id == "camera" })
    assertNotNull(capabilities.find { it.id == "contacts" })
    assertNotNull(capabilities.find { it.id == "phone" })
    assertNotNull(capabilities.find { it.id == "sms" })
    assertNotNull(capabilities.find { it.id == "alarms" })
    assertNotNull(capabilities.find { it.id == "bluetooth" })
    assertNotNull(capabilities.find { it.id == "notifications" })
  }

  @Test
  fun `command parser extracts device actions accurately`() {
    val launch = CommandParser.parse("open youtube")
    assertNotNull(launch)
    assertEquals(ActionType.APP_LAUNCH, launch?.actionType)
    assertEquals("youtube", launch?.parameters?.get("appName"))

    val call = CommandParser.parse("call Rahim")
    assertNotNull(call)
    assertEquals(ActionType.PHONE_CALL, call?.actionType)
    assertEquals("Rahim", call?.parameters?.get("contactName"))

    val alarm = CommandParser.parse("set alarm for 7:30")
    assertNotNull(alarm)
    assertEquals(ActionType.SET_ALARM, alarm?.actionType)
    assertEquals(7, alarm?.parameters?.get("hour"))
    assertEquals(30, alarm?.parameters?.get("minutes"))

    val location = CommandParser.parse("where am i")
    assertNotNull(location)
    assertEquals(ActionType.GET_LOCATION, location?.actionType)

    val bluetooth = CommandParser.parse("check bluetooth status")
    assertNotNull(bluetooth)
    assertEquals(ActionType.BLUETOOTH_STATUS, bluetooth?.actionType)

    val camera = CommandParser.parse("take a picture")
    assertNotNull(camera)
    assertEquals(ActionType.TAKE_PHOTO, camera?.actionType)

    val nonAction = CommandParser.parse("What is the capital of France?")
    assertEquals(null, nonAction)
  }

  @Test
  fun `tool registry resolves tools for all action types`() {
    val appTool = ToolRegistry.findForActionType(ActionType.APP_LAUNCH)
    assertNotNull(appTool)
    assertEquals("tool_app_launcher", appTool?.id)

    val phoneTool = ToolRegistry.findForActionType(ActionType.PHONE_CALL)
    assertNotNull(phoneTool)
    assertEquals("tool_phone", phoneTool?.id)

    val smsTool = ToolRegistry.findForActionType(ActionType.SEND_SMS)
    assertNotNull(smsTool)
    assertEquals("tool_sms", smsTool?.id)

    val alarmTool = ToolRegistry.findForActionType(ActionType.SET_ALARM)
    assertNotNull(alarmTool)
    assertEquals("tool_alarm", alarmTool?.id)
  }

  @Test
  fun `agent capability policy blocks disabled capability truthfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val policy = AgentCapabilityPolicy()

    // Default is enabled
    assertTrue(policy.isCapabilityEnabled(AgentCapabilityType.MICROPHONE))

    // Disable in-app
    policy.setCapabilityEnabled(AgentCapabilityType.MICROPHONE, false)
    assertFalse(policy.isCapabilityEnabled(AgentCapabilityType.MICROPHONE))

    val check = policy.checkExecution(context, AgentCapabilityType.MICROPHONE)
    assertTrue(check is CapabilityExecutionCheck.BlockedByUserInAgent)
    val blocked = check as CapabilityExecutionCheck.BlockedByUserInAgent
    assertTrue(blocked.reason.contains("disabled in Agent Settings"))
  }

  @Test
  fun `action confirmation modes respect sensitivity rules`() {
    val policy = AgentCapabilityPolicy()

    // Default NORMAL mode: Sensitive actions require confirmation, low-risk do not
    policy.setConfirmationMode(com.example.core.capability.ActionConfirmationMode.NORMAL)
    assertTrue(policy.requiresConfirmation(ActionType.PHONE_CALL))
    assertTrue(policy.requiresConfirmation(ActionType.SEND_SMS))
    assertFalse(policy.requiresConfirmation(ActionType.APP_LAUNCH))
    assertFalse(policy.requiresConfirmation(ActionType.GET_LOCATION))

    // STRICT mode: State altering and sensitive both require confirmation
    policy.setConfirmationMode(com.example.core.capability.ActionConfirmationMode.STRICT)
    assertTrue(policy.requiresConfirmation(ActionType.PHONE_CALL))
    assertTrue(policy.requiresConfirmation(ActionType.SET_ALARM))

    // DIRECT mode: Direct execution without confirmation dialogs
    policy.setConfirmationMode(com.example.core.capability.ActionConfirmationMode.DIRECT)
    assertFalse(policy.requiresConfirmation(ActionType.PHONE_CALL))
    assertFalse(policy.requiresConfirmation(ActionType.SET_ALARM))
  }

  @Test
  fun `agent state machine maps to legacy core state correctly`() {
    assertEquals(CoreState.IDLE, com.example.core.model.AgentState.IDLE.toCoreState())
    assertEquals(CoreState.LISTENING, com.example.core.model.AgentState.LISTENING.toCoreState())
    assertEquals(CoreState.THINKING, com.example.core.model.AgentState.PROCESSING.toCoreState())
    assertEquals(CoreState.SPEAKING, com.example.core.model.AgentState.SPEAKING.toCoreState())
    assertEquals(CoreState.OFFLINE, com.example.core.model.AgentState.OFFLINE.toCoreState())
    assertEquals(CoreState.OFFLINE, com.example.core.model.AgentState.ERROR.toCoreState())
  }

  @Test
  fun `unconfigured ai provider truthfully reports error without mock hallucination`() = kotlinx.coroutines.test.runTest {
    val provider = com.example.core.provider.CompositeIntelligenceProvider()
    val context = com.example.core.context.ConversationContext()
    val result = provider.sendMessage("Hello", context)

    assertTrue(result is com.example.core.provider.ProviderResult.Error)
    val errorResult = result as com.example.core.provider.ProviderResult.Error
    assertTrue(errorResult.isConfigError)
    assertTrue(errorResult.message.contains("AI provider is not configured"))
  }

  @Test
  fun `wake word engine truthfully reports unavailable until dedicated engine is installed`() {
    val wakeWordEngine = com.example.core.wakeword.DefaultWakeWordEngine()
    assertFalse(wakeWordEngine.isAvailable)
    assertTrue(wakeWordEngine.supportedPhrases.contains("Hey Nexus"))
    assertTrue(wakeWordEngine.supportedPhrases.contains("Hey Maya"))
  }

  @Test
  fun `background agent state exposes required lifecycle states`() {
    val states = com.example.core.background.BackgroundAgentState.values()
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.STOPPED))
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.STARTING))
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.RUNNING))
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.PAUSED))
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.ERROR))
    assertTrue(states.contains(com.example.core.background.BackgroundAgentState.NOT_PERMITTED))
  }

  @Test
  fun `provider registry maps core inference domains`() {
    val configs = ProviderRegistry.defaultConfigs
    assertEquals(8, configs.size)
  }

  @Test
  fun `companion modes and presence states operate correctly`() {
    val modes = com.example.core.companion.CompanionMode.values()
    assertTrue(modes.contains(com.example.core.companion.CompanionMode.PERSONAL_ASSISTANT))
    assertTrue(modes.contains(com.example.core.companion.CompanionMode.FRIEND))
    assertTrue(modes.contains(com.example.core.companion.CompanionMode.COMPANION))
    assertTrue(modes.contains(com.example.core.companion.CompanionMode.PROFESSIONAL))
    assertTrue(modes.contains(com.example.core.companion.CompanionMode.ISLAMIC_ORIENTED))
  }

  @Test
  fun `proactive policy is off by default for privacy compliance`() {
    val policy = com.example.core.companion.ProactivePolicy()
    assertFalse("Proactive master toggle must be off by default", policy.isProactiveCompanionEnabled)
    assertFalse("Proactive notifications must be off by default", policy.proactiveNotifications)
    assertFalse("Proactive greetings must be off by default", policy.proactiveGreetings)
    assertFalse("Reminder messages must be off by default", policy.reminderMessages)
    assertFalse("Idle check-ins must be off by default", policy.idleCheckIns)
    assertFalse("Sleep mode messages must be off by default", policy.sleepModeMessages)
  }

  @Test
  fun `personality engine updates identity safely and locally`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val personalityEngine = com.example.core.ai.personality.AgentPersonalityEngine(context)
    val initial = personalityEngine.identity.value

    assertEquals("Aegis", initial.name)
    personalityEngine.updateIdentity(initial.copy(name = "Nexus Core", speakingStyle = com.example.core.ai.personality.SpeakingStyle.CONCISE))

    assertEquals("Nexus Core", personalityEngine.identity.value.name)
    assertEquals(com.example.core.ai.personality.SpeakingStyle.CONCISE, personalityEngine.identity.value.speakingStyle)
  }

  @Test
  fun `companion sleep manager initializes and updates schedule correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sleepManager = com.example.core.companion.CompanionSleepManager(context)
    val initialSchedule = sleepManager.sleepSchedule.value

    assertEquals(23, initialSchedule.sleepHour)
    assertEquals(7, initialSchedule.wakeHour)
    assertFalse(initialSchedule.isScheduleEnabled)

    val updatedSchedule = initialSchedule.copy(
      isScheduleEnabled = true,
      sleepHour = 22,
      wakeHour = 6
    )
    sleepManager.updateSchedule(updatedSchedule)

    assertEquals(22, sleepManager.sleepSchedule.value.sleepHour)
    assertEquals(6, sleepManager.sleepSchedule.value.wakeHour)
    assertTrue(sleepManager.sleepSchedule.value.isScheduleEnabled)
  }
}
