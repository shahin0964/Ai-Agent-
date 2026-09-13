package com.example.core.companion

import android.content.Context
import com.example.core.ai.AIEngineContainer
import com.example.core.ai.personality.AgentPersonalityEngine
import com.example.core.background.BackgroundPresenceController
import com.example.core.background.BackgroundVoiceController
import com.example.core.model.AgentState
import com.example.core.orchestrator.AgentOrchestrator
import com.example.core.overlay.FloatingAssistantController
import com.example.core.wakeword.DefaultWakeWordEngine
import com.example.core.wakeword.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Master Companion Engine adhering to Section 2 of Patch 5.
 * Responsibilities:
 * - Personality state & Behavioral parameters
 * - Relationship & Operational modes (CompanionMode)
 * - True presence state management (PresenceManager)
 * - Proactive behavior policy & safe event dispatching (ProactivePolicy & ProactiveEventEngine)
 * - Notification management with rate-limiting and privacy (CompanionNotificationManager)
 * - Scheduled sleep mode & DND integration (CompanionSleepManager)
 * - Background presence & voice controllers (truthful status)
 * - Siri-like Floating Assistant controller & visual styles
 *
 * It does NOT directly execute Android capabilities; it coordinates with AgentOrchestrator,
 * MemoryManager, CapabilityRegistry, and the Voice pipeline.
 */
class CompanionEngine(
  private val context: Context,
  val personalityEngine: AgentPersonalityEngine,
  val wakeWordEngine: WakeWordEngine = DefaultWakeWordEngine(),
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
  // 1. Presence & Background
  val presenceManager = PresenceManager(context)
  val backgroundPresenceController = BackgroundPresenceController(context) { running ->
    presenceManager.onBackgroundServiceStateChanged(running)
  }
  val backgroundVoiceController = BackgroundVoiceController(context, wakeWordEngine, backgroundPresenceController)

  // 2. Sleep Management
  val sleepManager = CompanionSleepManager(context) { sleeping ->
    presenceManager.onSleepStateChanged(sleeping)
    updateMoodState(currentAgentState)
  }

  // 3. Proactive Policy & Event Engine
  val proactivePolicyManager = ProactivePolicyManager(context)
  val notificationManager = CompanionNotificationManager(context)
  val proactiveEventEngine = ProactiveEventEngine(
    context = context,
    policyManager = proactivePolicyManager,
    notificationManager = notificationManager,
    sleepManager = sleepManager
  )

  // 4. Floating Overlay Assistant Controller
  val floatingAssistantController = FloatingAssistantController(context)

  // 5. Emotional-style & Mood state (Truthful mapping)
  private var currentAgentState: AgentState = AgentState.IDLE
  private var preferredEmotion: CompanionMoodState = CompanionMoodState.ONLINE
  private val _moodState = MutableStateFlow(CompanionMoodState.ONLINE)
  val moodState: StateFlow<CompanionMoodState> = _moodState.asStateFlow()

  // 6. Session Greeting state
  private val _greetingMessage = MutableStateFlow<String?>(null)
  val greetingMessage: StateFlow<String?> = _greetingMessage.asStateFlow()

  init {
    updateMoodState(AgentState.IDLE)
  }

  fun bindOrchestrator(orchestrator: AgentOrchestrator) {
    scope.launch {
      orchestrator.agentState.collect { state ->
        currentAgentState = state
        presenceManager.updateFromAgentState(state)
        floatingAssistantController.syncWithAgentState(state)
        updateMoodState(state)
      }
    }
  }

  fun setEmotion(mood: CompanionMoodState) {
    preferredEmotion = mood
    updateMoodState(currentAgentState)
  }

  fun onSessionStarted(onGreeting: ((String) -> Unit)? = null) {
    presenceManager.onAppForeground()
    sleepManager.checkAndUpdateState()

    proactiveEventEngine.onEvent(
      ProactiveEvent(ProactiveEventType.USER_STARTED_SESSION)
    ) { greeting ->
      _greetingMessage.value = greeting
      onGreeting?.invoke(greeting)
    }
  }

  fun onSessionStopped() {
    presenceManager.onAppBackground()
  }

  fun dismissGreeting() {
    _greetingMessage.value = null
  }

  private fun updateMoodState(agentState: AgentState) {
    _moodState.value = CompanionMoodState.fromAgentState(
      agentState = agentState,
      isSleeping = sleepManager.isSleeping,
      isOffline = presenceManager.presenceState.value == PresenceState.OFFLINE,
      preferredStyle = preferredEmotion
    )
  }

  /**
   * Generates a context-aware greeting strictly when requested or authorized,
   * incorporating user identity and current time.
   */
  fun generateContextualGreeting(): String {
    val identity = personalityEngine.identity.value
    val mode = personalityEngine.companionMode.value
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val timeGreeting = when {
      hour < 12 -> "Good morning"
      hour < 17 -> "Good afternoon"
      else -> "Good evening"
    }

    return when (mode) {
      CompanionMode.ISLAMIC_ORIENTED ->
        "As-salamu alaykum, ${identity.nickname}. $timeGreeting. How can I assist you with your day?"

      CompanionMode.FRIEND ->
        "Hey ${identity.nickname}! $timeGreeting. Great to see you. What's on your mind?"

      CompanionMode.COMPANION ->
        "$timeGreeting, ${identity.nickname}. I'm here and ready to assist whenever you need me."

      CompanionMode.PROFESSIONAL ->
        "$timeGreeting, ${identity.nickname}. Core systems operational. Ready for instructions."

      CompanionMode.PERSONAL_ASSISTANT ->
        "$timeGreeting, ${identity.nickname}. All systems ready. What shall we tackle next?"
    }
  }
}
