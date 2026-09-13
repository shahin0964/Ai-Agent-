package com.example.core.orchestrator

import android.content.Context
import com.example.core.action.ActionStatus
import com.example.core.action.ActionType
import com.example.core.action.AgentAction
import com.example.core.action.CommandParser
import com.example.core.action.ToolResult
import com.example.core.ai.AIEngineContainer
import com.example.core.ai.memory.MemoryCommandResult
import com.example.core.ai.model.AICapability
import com.example.core.ai.model.AIProviderResult
import com.example.core.ai.model.GenerateOptions
import com.example.core.capability.AgentCapabilityPolicy
import com.example.core.capability.CapabilityExecutionCheck
import com.example.core.context.ConversationContext
import com.example.core.model.AgentIdentity
import com.example.core.model.AgentState
import com.example.core.model.ChatMessage
import com.example.core.model.SystemActivity
import com.example.core.provider.CompositeIntelligenceProvider
import com.example.core.provider.IntelligenceProvider
import com.example.core.provider.ProviderResult
import com.example.core.tool.ToolRegistry
import com.example.core.tts.TextToSpeechEngine
import com.example.core.voice.VoiceEngine
import com.example.core.voice.VoiceError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Central Orchestrator coordinating:
 * - Voice Input (STT) & Speech Output (TTS)
 * - Android Capability Policy & Permissions (Section 3 & 4)
 * - Natural Language Command Parsing & Tool Execution (Section 9)
 * - AI Intelligence Provider & Multi-Provider Router (Patch 4)
 * - Memory Subsystem (Short & Long Term) (Patch 4)
 * - Research Engine (Patch 4)
 * - Agent State Machine & Multi-turn Context
 */
class AgentOrchestrator(
  private val context: Context? = null,
  private val voiceEngine: VoiceEngine,
  private val ttsEngine: TextToSpeechEngine,
  private val aiProvider: IntelligenceProvider = CompositeIntelligenceProvider(),
  val policy: AgentCapabilityPolicy = AgentCapabilityPolicy(),
  val aiEngine: AIEngineContainer? = null,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

  private val _agentState = MutableStateFlow(AgentState.IDLE)
  val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

  private val _identity = MutableStateFlow(
    AgentIdentity(
      name = aiEngine?.personalityEngine?.identity?.value?.name?.takeIf { it.isNotBlank() } ?: "Aegis"
    )
  )
  val identity: StateFlow<AgentIdentity> = _identity.asStateFlow()

  init {
    if (aiEngine != null) {
      coroutineScope.launch {
        aiEngine.personalityEngine.identity.collect { config ->
          if (config.name.isNotBlank() && _identity.value.name != config.name) {
            _identity.value = _identity.value.copy(name = config.name)
          }
        }
      }
    }
  }

  private val _systemActivity = MutableStateFlow(
    SystemActivity(label = "Ready", detail = "Subsystems active")
  )
  val systemActivity: StateFlow<SystemActivity> = _systemActivity.asStateFlow()

  private val _context = MutableStateFlow(ConversationContext())
  val contextState: StateFlow<ConversationContext> = _context.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _liveTranscription = MutableStateFlow("")
  val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  val audioAmplitude: StateFlow<Float> = voiceEngine.audioAmplitude

  // Action telemetry & Confirmation queue (Patch 3)
  private val _recentActions = MutableStateFlow<List<AgentAction>>(emptyList())
  val recentActions: StateFlow<List<AgentAction>> = _recentActions.asStateFlow()

  private val _pendingConfirmationAction = MutableStateFlow<AgentAction?>(null)
  val pendingConfirmationAction: StateFlow<AgentAction?> = _pendingConfirmationAction.asStateFlow()

  fun setAgentName(name: String) {
    if (name.isNotBlank()) {
      val trimmed = name.trim()
      _identity.value = _identity.value.copy(name = trimmed)
      aiEngine?.personalityEngine?.let { engine ->
        engine.updateIdentity(engine.identity.value.copy(name = trimmed))
      }
    }
  }

  fun setAgentState(state: AgentState) {
    _agentState.value = state
    _systemActivity.value = SystemActivity(
      label = state.label,
      detail = state.statusDescription
    )
  }

  /**
   * Voice Interaction Flow
   */
  fun startVoiceSession() {
    if (_agentState.value == AgentState.SPEAKING) {
      ttsEngine.stop()
      setAgentState(AgentState.IDLE)
      return
    }

    if (_agentState.value == AgentState.LISTENING) {
      voiceEngine.stopListening()
      return
    }

    _errorMessage.value = null
    _liveTranscription.value = ""
    setAgentState(AgentState.LISTENING)

    voiceEngine.startListening(
      onPartialResult = { partial ->
        _liveTranscription.value = partial
      },
      onResult = { finalResult ->
        _liveTranscription.value = finalResult
        processVoiceInput(finalResult)
      },
      onError = { error ->
        handleVoiceError(error)
      }
    )
  }

  fun stopVoiceSession() {
    voiceEngine.stopListening()
    if (_agentState.value == AgentState.LISTENING) {
      setAgentState(AgentState.IDLE)
    }
  }

  fun cancelVoiceSession() {
    voiceEngine.cancelListening()
    ttsEngine.stop()
    _liveTranscription.value = ""
    setAgentState(AgentState.IDLE)
  }

  /**
   * Text message entry point (ChatScreen)
   */
  fun sendTextMessage(text: String, speakResponse: Boolean = false) {
    if (text.isBlank()) return

    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      isUser = true,
      text = text.trim()
    )
    appendMessage(userMsg)

    processInputPipeline(userMsg.text, speakResponse = speakResponse)
  }

  private fun processVoiceInput(spokenText: String) {
    if (spokenText.isBlank()) {
      setAgentState(AgentState.IDLE)
      return
    }

    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      isUser = true,
      text = spokenText.trim()
    )
    appendMessage(userMsg)

    processInputPipeline(spokenText, speakResponse = true)
  }

  /**
   * Unified Input Processing Pipeline:
   * 1. Check if user is answering a pending confirmation ("yes", "confirm", "no", "cancel")
   * 2. Memory commands check (Patch 4: "remember that...", "what do you remember", "forget...")
   * 3. Try CommandParser for Android Device Actions
   * 4. Route to Web Research Engine if applicable
   * 5. Route to AI Intelligence Router / Provider
   */
  private fun processInputPipeline(input: String, speakResponse: Boolean) {
    val clean = input.trim().lowercase()

    // 1. Check pending confirmation
    val pending = _pendingConfirmationAction.value
    if (pending != null) {
      if (clean == "yes" || clean == "confirm" || clean == "proceed" || clean == "do it" || clean == "call" || clean == "send") {
        confirmPendingAction(speakResponse)
        return
      } else if (clean == "no" || clean == "cancel" || clean == "abort" || clean == "stop") {
        cancelPendingAction(speakResponse)
        return
      }
    }

    // 2. Memory commands check
    if (aiEngine != null) {
      coroutineScope.launch {
        val memResult = aiEngine.memoryManager.handleMemoryCommand(input)
        when (memResult) {
          is MemoryCommandResult.Remembered -> {
            recordAction(
              AgentAction(
                type = ActionType.MEMORY_WRITE,
                description = "Remembered: ${memResult.item.content.take(35)}",
                requiredCapabilities = emptyList(),
                status = ActionStatus.SUCCESS,
                resultMessage = "Saved into long-term memory."
              )
            )
            respondToUser("Recorded in long-term memory: \"${memResult.item.content}\"", isError = false, speakResponse = speakResponse)
            return@launch
          }
          is MemoryCommandResult.Forgotten -> {
            recordAction(
              AgentAction(
                type = ActionType.MEMORY_WRITE,
                description = "Forgotten: ${memResult.description}",
                requiredCapabilities = emptyList(),
                status = ActionStatus.SUCCESS,
                resultMessage = "Removed memory entries."
              )
            )
            respondToUser("Cleared ${memResult.description} from long-term memory.", isError = false, speakResponse = speakResponse)
            return@launch
          }
          is MemoryCommandResult.Recalled -> {
            recordAction(
              AgentAction(
                type = ActionType.MEMORY_READ,
                description = "Queried memory substrate",
                requiredCapabilities = emptyList(),
                status = ActionStatus.SUCCESS
              )
            )
            val msg = if (memResult.memories.isEmpty()) {
              "No long-term memories are currently recorded."
            } else {
              "Here is what I have stored in memory:\n" +
                memResult.memories.mapIndexed { idx, it -> "${idx + 1}. ${it.content}" }.joinToString("\n")
            }
            respondToUser(msg, isError = false, speakResponse = speakResponse)
            return@launch
          }
          is MemoryCommandResult.Error -> {
            respondToUser(memResult.reason, isError = true, speakResponse = speakResponse)
            return@launch
          }
          MemoryCommandResult.NotAMemoryCommand -> {
            dispatchStandardPipeline(input, speakResponse)
          }
        }
      }
      return
    }

    dispatchStandardPipeline(input, speakResponse)
  }

  private fun dispatchStandardPipeline(input: String, speakResponse: Boolean) {
    // 2.5 Check if input is an automation creation or routine management command
    if (aiEngine != null) {
      val autoResult = aiEngine.automationEngine.interpretUserCommand(input)
      when (autoResult) {
        is com.example.core.automation.interpreter.InterpreterResult.DraftCreated -> {
          recordAction(
            AgentAction(
              type = ActionType.AI_REQUEST,
              description = "Drafted Routine: ${autoResult.automation.name}",
              requiredCapabilities = emptyList(),
              status = ActionStatus.SUCCESS,
              resultMessage = "Automation saved to drafts."
            )
          )
          respondToUser(autoResult.explanation, isError = false, speakResponse = speakResponse)
          return
        }
        is com.example.core.automation.interpreter.InterpreterResult.ManageCommand -> {
          recordAction(
            AgentAction(
              type = ActionType.AI_REQUEST,
              description = "Automation Command: ${autoResult.commandType.name}",
              requiredCapabilities = emptyList(),
              status = ActionStatus.SUCCESS,
              resultMessage = autoResult.responseText
            )
          )
          respondToUser(autoResult.responseText, isError = false, speakResponse = speakResponse)
          return
        }
        is com.example.core.automation.interpreter.InterpreterResult.ClarificationNeeded -> {
          respondToUser(autoResult.prompt, isError = false, speakResponse = speakResponse)
          return
        }
        com.example.core.automation.interpreter.InterpreterResult.NotAnAutomationCommand -> {
          // Proceed to standard pipeline
        }
      }
    }

    // 3. Parse command for device action
    val parsed = CommandParser.parse(input)
    if (parsed != null && context != null) {
      executeDeviceAction(parsed.actionType, parsed.parameters, parsed.description, speakResponse)
      return
    }

    // 4. Check if prompt targets Web Research
    if (aiEngine != null && aiEngine.router.classifyCapability(input) == AICapability.WEB_RESEARCH) {
      executeWebResearch(input, speakResponse)
      return
    }

    // 5. Default AI inference
    executeInference(input, speakResponse)
  }

  private fun executeWebResearch(query: String, speakResponse: Boolean) {
    if (aiEngine == null) {
      executeInference(query, speakResponse)
      return
    }

    setAgentState(AgentState.PROCESSING)
    _systemActivity.value = SystemActivity(
      label = "Web Research",
      detail = "Grounding query: ${query.take(30)}..."
    )

    val researchAction = AgentAction(
      type = ActionType.WEB_RESEARCH,
      description = "Research: ${query.take(40)}",
      requiredCapabilities = emptyList(),
      status = ActionStatus.EXECUTING
    )
    recordAction(researchAction)

    coroutineScope.launch {
      val result = aiEngine.researchEngine.conductResearch(query, _context.value)
      val finishedAction = researchAction.copy(
        status = if (result.sources.isNotEmpty()) ActionStatus.SUCCESS else ActionStatus.FAILED,
        resultMessage = "Found ${result.sources.size} grounded sources"
      )
      recordAction(finishedAction)

      val formattedResponse = buildString {
        append(result.synthesizedAnswer)
        if (result.sources.isNotEmpty()) {
          append("\n\nSources:")
          result.sources.take(3).forEachIndexed { i, s ->
            append("\n[${i + 1}] ${s.title} (${s.sourceDomain})")
          }
        }
      }

      respondToUser(formattedResponse, isError = false, speakResponse = speakResponse)
    }
  }

  /**
   * Executes a parsed device action under strict capability policy checks.
   */
  private fun executeDeviceAction(
    actionType: com.example.core.action.ActionType,
    parameters: Map<String, Any?>,
    description: String,
    speakResponse: Boolean
  ) {
    if (context == null) return

    val tool = ToolRegistry.findForActionType(actionType)
    if (tool == null) {
      executeInference(description, speakResponse)
      return
    }

    val initialAction = AgentAction(
      type = actionType,
      description = description,
      requiredCapabilities = tool.requiredCapabilities,
      parameters = parameters,
      status = ActionStatus.PLANNED
    )
    recordAction(initialAction)

    setAgentState(AgentState.PROCESSING)
    _systemActivity.value = SystemActivity(
      label = "Device Action",
      detail = "Evaluating capability for $description"
    )

    // Check capability policy
    val check = tool.canExecute(context, policy)
    when (check) {
      is CapabilityExecutionCheck.BlockedByUserInAgent -> {
        val failedAction = initialAction.copy(
          status = ActionStatus.FAILED,
          errorMessage = check.reason
        )
        recordAction(failedAction)
        respondToUser(check.reason, isError = true, speakResponse = speakResponse)
        return
      }

      is CapabilityExecutionCheck.PermissionRequired -> {
        val permAction = initialAction.copy(
          status = ActionStatus.WAITING_FOR_PERMISSION,
          errorMessage = check.explanation
        )
        recordAction(permAction)
        respondToUser(check.explanation, isError = true, speakResponse = speakResponse)
        return
      }

      is CapabilityExecutionCheck.RequiresSettings -> {
        val settingsAction = initialAction.copy(
          status = ActionStatus.WAITING_FOR_PERMISSION,
          errorMessage = check.explanation
        )
        recordAction(settingsAction)
        respondToUser(check.explanation, isError = true, speakResponse = speakResponse)
        return
      }

      is CapabilityExecutionCheck.Unavailable -> {
        val unavailAction = initialAction.copy(
          status = ActionStatus.FAILED,
          errorMessage = check.reason
        )
        recordAction(unavailAction)
        respondToUser(check.reason, isError = true, speakResponse = speakResponse)
        return
      }

      CapabilityExecutionCheck.Allowed -> {
        // Capability is allowed. Now check confirmation policy.
        val needsConfirmation = policy.requiresConfirmation(actionType)
        if (needsConfirmation) {
          val confirmPrompt = "Confirm $description?"
          val waitingAction = initialAction.copy(
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            requiresConfirmation = true,
            confirmationPrompt = confirmPrompt
          )
          recordAction(waitingAction)
          _pendingConfirmationAction.value = waitingAction

          val msg = "Please confirm: Would you like me to $description?"
          respondToUser(msg, isError = false, speakResponse = speakResponse)
          return
        }

        // Execute directly
        performToolExecution(tool, initialAction, parameters, speakResponse)
      }
    }
  }

  private fun performToolExecution(
    tool: com.example.core.tool.AndroidTool,
    action: AgentAction,
    parameters: Map<String, Any?>,
    speakResponse: Boolean
  ) {
    if (context == null) return

    val executingAction = action.copy(status = ActionStatus.EXECUTING)
    recordAction(executingAction)

    coroutineScope.launch {
      val result = tool.execute(context, parameters, policy)
      when (result) {
        is ToolResult.Success -> {
          val finishedAction = executingAction.copy(
            status = ActionStatus.SUCCESS,
            resultMessage = result.message
          )
          recordAction(finishedAction)
          _systemActivity.value = SystemActivity(
            label = "Action Complete",
            detail = finishedAction.description
          )
          respondToUser(result.message, isError = false, speakResponse = speakResponse)
        }

        is ToolResult.Failed -> {
          val failedAction = executingAction.copy(
            status = ActionStatus.FAILED,
            errorMessage = result.reason
          )
          recordAction(failedAction)
          respondToUser(result.reason, isError = true, speakResponse = speakResponse)
        }

        is ToolResult.Blocked -> {
          val blockedAction = executingAction.copy(
            status = ActionStatus.FAILED,
            errorMessage = result.reason
          )
          recordAction(blockedAction)
          respondToUser(result.reason, isError = true, speakResponse = speakResponse)
        }

        is ToolResult.PermissionRequired -> {
          val permAction = executingAction.copy(
            status = ActionStatus.WAITING_FOR_PERMISSION,
            errorMessage = result.explanation
          )
          recordAction(permAction)
          respondToUser(result.explanation, isError = true, speakResponse = speakResponse)
        }

        is ToolResult.ConfirmationRequired -> {
          val waitAction = result.action.copy(
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            requiresConfirmation = true,
            confirmationPrompt = result.confirmationPrompt
          )
          recordAction(waitAction)
          _pendingConfirmationAction.value = waitAction
          respondToUser(result.confirmationPrompt, isError = false, speakResponse = speakResponse)
        }

        is ToolResult.Ambiguous -> {
          respondToUser(result.clarificationPrompt, isError = false, speakResponse = speakResponse)
        }

        is ToolResult.Cancelled -> {
          val cancelledAction = executingAction.copy(
            status = ActionStatus.CANCELLED,
            errorMessage = result.reason
          )
          recordAction(cancelledAction)
          respondToUser(result.reason, isError = false, speakResponse = speakResponse)
        }
      }
    }
  }

  fun confirmPendingAction(speakResponse: Boolean = false) {
    val action = _pendingConfirmationAction.value ?: return
    _pendingConfirmationAction.value = null

    val tool = ToolRegistry.findForActionType(action.type) ?: return
    val confirmedParams = action.parameters.toMutableMap().apply { put("confirmed", true) }
    performToolExecution(tool, action, confirmedParams, speakResponse)
  }

  fun cancelPendingAction(speakResponse: Boolean = false) {
    val action = _pendingConfirmationAction.value ?: return
    _pendingConfirmationAction.value = null

    val cancelledAction = action.copy(
      status = ActionStatus.CANCELLED,
      errorMessage = "Action was cancelled by the operator."
    )
    recordAction(cancelledAction)
    respondToUser("Action cancelled: ${action.description}", isError = false, speakResponse = speakResponse)
  }

  private fun recordAction(action: AgentAction) {
    val current = _recentActions.value.toMutableList()
    val index = current.indexOfFirst { it.id == action.id }
    if (index != -1) {
      current[index] = action
    } else {
      current.add(0, action)
      if (current.size > 20) {
        current.removeAt(current.lastIndex)
      }
    }
    _recentActions.value = current
  }

  private fun executeInference(prompt: String, speakResponse: Boolean) {
    setAgentState(AgentState.PROCESSING)

    if (aiEngine != null) {
      val targetCap = aiEngine.router.classifyCapability(prompt)
      val actionType = when (targetCap) {
        AICapability.CODE_GENERATION, AICapability.WEBSITE_GENERATION -> ActionType.CODE_GENERATION
        else -> ActionType.AI_REQUEST
      }

      val initialAction = AgentAction(
        type = actionType,
        description = "AI Inference: ${prompt.take(40)}...",
        requiredCapabilities = emptyList(),
        status = ActionStatus.EXECUTING
      )
      recordAction(initialAction)

      coroutineScope.launch {
        // Retrieve optional long term memory context
        val memoryContext = aiEngine.memoryManager.getRelevantContextPrompt(prompt)
        val systemPrompt = buildString {
          append(aiEngine.personalityEngine.buildSystemPrompt(_identity.value.name))
          if (!memoryContext.isNullOrBlank()) {
            append("\n\n").append(memoryContext)
          }
        }

        val result = aiEngine.router.routeAndGenerate(
          prompt = prompt,
          context = _context.value,
          options = GenerateOptions(
            systemPrompt = systemPrompt,
            targetCapability = targetCap
          )
        )

        when (result) {
          is AIProviderResult.Success -> {
            val finishedAction = initialAction.copy(
              status = ActionStatus.SUCCESS,
              resultMessage = "Model: ${result.modelUsed} (${result.providerId})"
            )
            recordAction(finishedAction)
            respondToUser(result.text, isError = false, speakResponse = speakResponse)
          }

          is AIProviderResult.Error -> {
            val failedAction = initialAction.copy(
              status = ActionStatus.FAILED,
              errorMessage = result.message
            )
            recordAction(failedAction)
            _errorMessage.value = result.message
            respondToUser(result.message, isError = true, speakResponse = speakResponse)
          }

          is AIProviderResult.Streaming -> {
            setAgentState(AgentState.IDLE)
          }
        }
      }
      return
    }

    coroutineScope.launch {
      val result = aiProvider.sendMessage(prompt, _context.value)

      when (result) {
        is ProviderResult.Success -> {
          respondToUser(result.responseText, isError = false, speakResponse = speakResponse)
        }

        is ProviderResult.Error -> {
          _errorMessage.value = result.message
          respondToUser(result.message, isError = true, speakResponse = speakResponse)
        }

        is ProviderResult.Streaming -> {
          setAgentState(AgentState.IDLE)
        }
      }
    }
  }

  private fun respondToUser(text: String, isError: Boolean, speakResponse: Boolean) {
    val assistantMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      isUser = false,
      text = text,
      isError = isError
    )
    appendMessage(assistantMsg)

    if (speakResponse && ttsEngine.isAvailable) {
      setAgentState(AgentState.SPEAKING)
      ttsEngine.speak(
        text = text,
        onStart = { setAgentState(AgentState.SPEAKING) },
        onComplete = { setAgentState(if (isError) AgentState.ERROR else AgentState.IDLE) },
        onError = { _ -> setAgentState(if (isError) AgentState.ERROR else AgentState.IDLE) }
      )
    } else {
      setAgentState(if (isError) AgentState.ERROR else AgentState.IDLE)
    }
  }

  private fun handleVoiceError(error: VoiceError) {
    _errorMessage.value = error.userFriendlyMessage
    _systemActivity.value = SystemActivity(
      label = "Voice Error",
      detail = error.userFriendlyMessage
    )
    setAgentState(AgentState.ERROR)
  }

  private fun appendMessage(message: ChatMessage) {
    val updated = _messages.value + message
    _messages.value = updated
    _context.value = _context.value.addMessage(message)
  }

  fun clearHistory() {
    _messages.value = emptyList()
    _context.value = _context.value.clear()
    _liveTranscription.value = ""
    _errorMessage.value = null
    _pendingConfirmationAction.value = null
    setAgentState(AgentState.IDLE)
  }

  fun release() {
    voiceEngine.release()
    ttsEngine.release()
    setAgentState(AgentState.OFFLINE)
  }
}
