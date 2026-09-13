package com.example.core.agent

import android.content.Context
import com.example.core.action.AgentAction
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.model.AgentIdentity
import com.example.core.model.AgentState
import com.example.core.model.ChatMessage
import com.example.core.model.CoreState
import com.example.core.model.SystemActivity
import com.example.core.model.toCoreState
import com.example.core.orchestrator.AgentOrchestrator
import com.example.core.provider.CompositeIntelligenceProvider
import com.example.core.provider.GeminiIntelligenceProvider
import com.example.core.provider.IntelligenceProvider
import com.example.core.provider.LocalOfflineIntelligenceProvider
import com.example.core.provider.ProviderRegistry
import com.example.core.tts.AndroidTextToSpeechEngine
import com.example.core.tts.TextToSpeechEngine
import com.example.core.voice.AndroidSpeechRecognizerEngine
import com.example.core.voice.TranscriptionState
import com.example.core.voice.VoiceEngine
import com.example.core.voice.VoiceError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Unified Agent Controller contract.
 * Exposes single source of truth for AgentState, Voice pipeline, Android Actions, and Capability Policy.
 */
interface AgentController {
  val agentState: StateFlow<AgentState>
  val coreState: StateFlow<CoreState>
  val systemActivity: StateFlow<SystemActivity>
  val identity: StateFlow<AgentIdentity>
  val agentIdentity: StateFlow<AgentIdentity> get() = identity
  val messages: StateFlow<List<ChatMessage>>
  val chatMessages: StateFlow<List<ChatMessage>> get() = messages
  val liveTranscription: StateFlow<String>
  val audioAmplitude: StateFlow<Float>
  val errorMessage: StateFlow<String?>
  val orchestrator: AgentOrchestrator?

  // Patch 3 Capability & Action flows
  val policy: AgentCapabilityPolicy
  val pendingConfirmationAction: StateFlow<AgentAction?>
  val recentActions: StateFlow<List<AgentAction>>

  // Patch 4 AI Intelligence Engine
  val aiEngine: com.example.core.ai.AIEngineContainer? get() = null

  // Patch 5 Companion Engine
  val companionEngine: com.example.core.companion.CompanionEngine? get() = null

  // Patch 6 Automation Engine
  val automationEngine: com.example.core.automation.AutomationEngine? get() = aiEngine?.automationEngine

  fun setCoreState(state: CoreState)
  fun setAgentState(state: AgentState)
  fun toggleVoiceInteraction()
  fun startVoiceSession()
  fun stopVoiceSession()
  fun cancelVoiceSession()
  fun sendUserMessage(text: String)
  fun sendMessage(text: String) = sendUserMessage(text)
  fun setAgentName(name: String)

  // Confirmation actions
  fun confirmPendingAction()
  fun cancelPendingAction()

  fun release() {}
}

/**
 * Real Production Agent Controller for Patch 2 & 3.
 * Wires real Android SpeechRecognizer, TextToSpeech, Capability Registry, and Tool execution.
 */
class RealAgentController(
  context: Context,
  voiceEngine: VoiceEngine = AndroidSpeechRecognizerEngine(context),
  ttsEngine: TextToSpeechEngine = AndroidTextToSpeechEngine(context),
  aiProvider: IntelligenceProvider? = null,
  scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : AgentController {

  override val aiEngine: com.example.core.ai.AIEngineContainer = com.example.core.ai.AIEngineContainer.getInstance(context)

  override val orchestrator: AgentOrchestrator = AgentOrchestrator(
    context = context.applicationContext,
    voiceEngine = voiceEngine,
    ttsEngine = ttsEngine,
    aiProvider = aiProvider ?: CompositeIntelligenceProvider(
      onlineProvider = GeminiIntelligenceProvider(
        apiKeyProvider = {
          ProviderRegistry.getApiKeyForDomain(com.example.core.provider.ProviderDomain.PRIMARY_AI)
        }
      ),
      offlineProvider = LocalOfflineIntelligenceProvider()
    ),
    aiEngine = aiEngine,
    coroutineScope = scope
  )

  override val companionEngine: com.example.core.companion.CompanionEngine =
    aiEngine.companionEngine.apply {
      bindOrchestrator(orchestrator)
    }

  override val agentState: StateFlow<AgentState> = orchestrator.agentState

  override val coreState: StateFlow<CoreState> = orchestrator.agentState.map { it.toCoreState() }
    .stateIn(scope, SharingStarted.Eagerly, CoreState.IDLE)

  override val systemActivity: StateFlow<SystemActivity> = orchestrator.systemActivity
  override val identity: StateFlow<AgentIdentity> = orchestrator.identity
  override val messages: StateFlow<List<ChatMessage>> = orchestrator.messages
  override val liveTranscription: StateFlow<String> = orchestrator.liveTranscription
  override val audioAmplitude: StateFlow<Float> = orchestrator.audioAmplitude
  override val errorMessage: StateFlow<String?> = orchestrator.errorMessage

  override val policy: AgentCapabilityPolicy = orchestrator.policy
  override val pendingConfirmationAction: StateFlow<AgentAction?> = orchestrator.pendingConfirmationAction
  override val recentActions: StateFlow<List<AgentAction>> = orchestrator.recentActions

  override fun setCoreState(state: CoreState) {
    val mapped = when (state) {
      CoreState.IDLE -> AgentState.IDLE
      CoreState.LISTENING -> AgentState.LISTENING
      CoreState.THINKING -> AgentState.PROCESSING
      CoreState.SPEAKING -> AgentState.SPEAKING
      CoreState.OFFLINE -> AgentState.OFFLINE
    }
    orchestrator.setAgentState(mapped)
  }

  override fun setAgentState(state: AgentState) {
    orchestrator.setAgentState(state)
  }

  override fun toggleVoiceInteraction() {
    when (orchestrator.agentState.value) {
      AgentState.IDLE, AgentState.ERROR, AgentState.PAUSED -> {
        orchestrator.startVoiceSession()
      }
      AgentState.LISTENING -> {
        orchestrator.stopVoiceSession()
      }
      AgentState.SPEAKING, AgentState.PROCESSING -> {
        orchestrator.cancelVoiceSession()
      }
      AgentState.OFFLINE, AgentState.INITIALIZING -> {
        orchestrator.setAgentState(AgentState.IDLE)
      }
    }
  }

  override fun startVoiceSession() {
    orchestrator.startVoiceSession()
  }

  override fun stopVoiceSession() {
    orchestrator.stopVoiceSession()
  }

  override fun cancelVoiceSession() {
    orchestrator.cancelVoiceSession()
  }

  override fun sendUserMessage(text: String) {
    orchestrator.sendTextMessage(text, speakResponse = false)
  }

  override fun setAgentName(name: String) {
    orchestrator.setAgentName(name)
  }

  override fun confirmPendingAction() {
    orchestrator.confirmPendingAction()
  }

  override fun cancelPendingAction() {
    orchestrator.cancelPendingAction()
  }

  override fun release() {
    orchestrator.release()
  }
}

/**
 * Lightweight in-memory controller for JVM testing and previewing.
 */
class InMemoryAgentController : AgentController {
  private val _agentState = MutableStateFlow(AgentState.IDLE)
  override val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

  private val _coreState = MutableStateFlow(CoreState.IDLE)
  override val coreState: StateFlow<CoreState> = _coreState.asStateFlow()

  private val _systemActivity = MutableStateFlow(
    SystemActivity(
      label = "UI Architecture Ready",
      detail = "AI provider not configured"
    )
  )
  override val systemActivity: StateFlow<SystemActivity> = _systemActivity.asStateFlow()

  private val _identity = MutableStateFlow(AgentIdentity())
  override val identity: StateFlow<AgentIdentity> = _identity.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _liveTranscription = MutableStateFlow("")
  override val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

  private val _audioAmplitude = MutableStateFlow(0f)
  override val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  override val orchestrator: AgentOrchestrator? = null

  override val policy: AgentCapabilityPolicy = AgentCapabilityPolicy()
  private val _pendingConfirmationAction = MutableStateFlow<AgentAction?>(null)
  override val pendingConfirmationAction: StateFlow<AgentAction?> = _pendingConfirmationAction.asStateFlow()

  private val _recentActions = MutableStateFlow<List<AgentAction>>(emptyList())
  override val recentActions: StateFlow<List<AgentAction>> = _recentActions.asStateFlow()

  override fun setCoreState(state: CoreState) {
    _coreState.value = state
    _agentState.value = when (state) {
      CoreState.IDLE -> AgentState.IDLE
      CoreState.LISTENING -> AgentState.LISTENING
      CoreState.THINKING -> AgentState.PROCESSING
      CoreState.SPEAKING -> AgentState.SPEAKING
      CoreState.OFFLINE -> AgentState.OFFLINE
    }
  }

  override fun setAgentState(state: AgentState) {
    _agentState.value = state
    _coreState.value = state.toCoreState()
  }

  override fun toggleVoiceInteraction() {
    val next = when (_coreState.value) {
      CoreState.IDLE -> CoreState.LISTENING
      CoreState.LISTENING -> CoreState.THINKING
      CoreState.THINKING -> CoreState.SPEAKING
      CoreState.SPEAKING -> CoreState.IDLE
      CoreState.OFFLINE -> CoreState.IDLE
    }
    setCoreState(next)
  }

  override fun startVoiceSession() {
    setCoreState(CoreState.LISTENING)
  }

  override fun stopVoiceSession() {
    setCoreState(CoreState.THINKING)
  }

  override fun cancelVoiceSession() {
    setCoreState(CoreState.IDLE)
  }

  override fun sendUserMessage(text: String) {
    if (text.isBlank()) return
    val userMsg = ChatMessage(
      id = System.currentTimeMillis().toString(),
      isUser = true,
      text = text.trim()
    )
    _messages.value = _messages.value + userMsg
  }

  override fun setAgentName(name: String) {
    if (name.isNotBlank()) {
      _identity.value = _identity.value.copy(name = name)
    }
  }

  override fun confirmPendingAction() {
    _pendingConfirmationAction.value = null
  }

  override fun cancelPendingAction() {
    _pendingConfirmationAction.value = null
  }
}

typealias Phase1AgentController = InMemoryAgentController
