package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.agent.AgentController
import com.example.core.agent.InMemoryAgentController
import com.example.ui.components.FuturisticButton
import com.example.ui.components.FuturisticCard
import com.example.ui.components.HudTelemetryHeader
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Root Composable orchestrating Top HUD, Central Viewport,
 * Action Confirmation HUD Overlay, Bottom Futuristic Navigation, and Slide-out Sidebar.
 */
@Composable
fun MainAppScreen(
  agentController: AgentController = remember { InMemoryAgentController() }
) {
  var currentScreen by remember { mutableStateOf<Screen>(Screen.Voice) }
  var isSidebarOpen by remember { mutableStateOf(false) }
  var editingAutomationId by remember { mutableStateOf<String?>(null) }

  val coreState by agentController.coreState.collectAsState()
  val agentState by agentController.agentState.collectAsState()
  val identity by agentController.identity.collectAsState()
  val activity by agentController.systemActivity.collectAsState()
  val chatMessages by agentController.messages.collectAsState()
  val liveTranscription by agentController.liveTranscription.collectAsState()
  val audioAmplitude by agentController.audioAmplitude.collectAsState()
  val errorMessage by agentController.errorMessage.collectAsState()
  val pendingConfirmationAction by agentController.pendingConfirmationAction.collectAsState()

  val greetingMessage by agentController.companionEngine?.greetingMessage?.collectAsState() ?: remember {
    mutableStateOf(null)
  }
  val moodState by agentController.companionEngine?.moodState?.collectAsState() ?: remember {
    mutableStateOf(com.example.core.companion.CompanionMoodState.ONLINE)
  }

  LaunchedEffect(Unit) {
    agentController.companionEngine?.onSessionStarted()
  }

  // Handle system back button if sidebar is open or in sub-screen
  BackHandler(enabled = isSidebarOpen || currentScreen == Screen.AgentTown || currentScreen == Screen.AgentHub || currentScreen == Screen.WorldMonitor || currentScreen == Screen.AutomationBuilder || currentScreen == Screen.Automation || currentScreen is Screen.SettingsPermissions || currentScreen.route.startsWith("settings/")) {
    if (isSidebarOpen) {
      isSidebarOpen = false
    } else if (currentScreen == Screen.AgentTown || currentScreen == Screen.AgentHub || currentScreen == Screen.WorldMonitor) {
      currentScreen = Screen.MyAgent
    } else if (currentScreen == Screen.AutomationBuilder) {
      currentScreen = Screen.Automation
    } else if (currentScreen == Screen.Automation) {
      currentScreen = Screen.MyAgent
    } else if (currentScreen.route.startsWith("settings/")) {
      currentScreen = Screen.Settings
    } else {
      currentScreen = Screen.Voice
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(com.example.ui.theme.activeTheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      // Top HUD Telemetry Bar (Header) with dynamic custom Agent Name
      HudTelemetryHeader(
        agentName = identity.name,
        coreState = coreState,
        moodState = moodState,
        onOpenSidebar = { isSidebarOpen = true }
      )

      // Main Viewport (Screens)
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxSize()
      ) {
        when (currentScreen) {
          Screen.Voice -> VoiceScreen(
            coreState = coreState,
            onStateChange = { agentController.setCoreState(it) },
            onToggleVoice = {
              agentController.toggleVoiceInteraction()
            },
            agentState = agentState,
            liveTranscription = liveTranscription,
            audioAmplitude = audioAmplitude,
            errorMessage = errorMessage
          )
          Screen.Chat -> ChatScreen(
            messages = chatMessages,
            onSendMessage = { agentController.sendUserMessage(it) },
            onVoiceShortcut = {
              currentScreen = Screen.Voice
              agentController.toggleVoiceInteraction()
            },
            agentState = agentState
          )
          Screen.Vision -> VisionScreen(
            onNavigateToAiProviders = { currentScreen = Screen.SettingsAiProviders },
            agentController = agentController
          )
          Screen.MyAgent -> MyAgentScreen(
            identity = identity,
            coreState = coreState,
            activity = activity,
            agentController = agentController,
            onNavigateToScreen = { currentScreen = it }
          )
          Screen.AgentTown -> AgentTownScreen(
            agentController = agentController,
            onNavigateToScreen = { currentScreen = it },
            onNavigateBack = { currentScreen = Screen.MyAgent }
          )
          Screen.AgentHub -> AgentHubScreen(
            agentController = agentController,
            onNavigateToScreen = { currentScreen = it },
            onNavigateBack = { currentScreen = Screen.MyAgent }
          )
          Screen.WorldMonitor -> WorldMonitorScreen(
            agentController = agentController,
            onNavigateBack = { currentScreen = Screen.MyAgent }
          )
          Screen.Automation -> AutomationHubScreen(
            agentController = agentController,
            onNavigateBack = { currentScreen = Screen.MyAgent },
            onOpenBuilder = { id ->
              editingAutomationId = id
              currentScreen = Screen.AutomationBuilder
            }
          )
          Screen.AutomationBuilder -> AutomationBuilderScreen(
            agentController = agentController,
            automationId = editingAutomationId,
            onNavigateBack = { currentScreen = Screen.Automation }
          )
          Screen.Memory -> MemoryScreen(agentController = agentController)
          Screen.Research -> ResearchScreen(agentController = agentController)
          Screen.Developer -> DeveloperScreen(agentController = agentController)
          Screen.Creator -> CreatorScreen(agentController = agentController)
          Screen.Settings -> SettingsHomeScreen(
            onNavigateTo = { currentScreen = it }
          )
          Screen.SettingsCompanion -> CompanionSettingsScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsPermissions -> PermissionCenterScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsPrivacy -> PrivacyCenterScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsVoiceSpeech -> VoiceSpeechSettingsScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )

          Screen.SettingsAiProviders -> AiProvidersSettingsScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsOverlay -> OverlayAssistantSettingsScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsAgentIdentity -> AgentIdentitySettingsScreen(
            onBack = { currentScreen = Screen.Settings },
            agentController = agentController
          )
          Screen.SettingsPersonality -> PersonalitySettingsScreen(
            onBack = { currentScreen = Screen.Settings }
          )
          Screen.SettingsOffline -> OfflineSettingsScreen(
            onBack = { currentScreen = Screen.Settings }
          )
          Screen.SettingsAppearance -> AppearanceSettingsScreen(
            onBack = { currentScreen = Screen.Settings }
          )
          Screen.SettingsAbout -> AboutSettingsScreen(
            onBack = { currentScreen = Screen.Settings }
          )
          Screen.SettingsUpdate -> AgentUpdateScreen(
            onBack = { currentScreen = Screen.Settings }
          )
        }

        // Proactive Greeting Banner
        if (greetingMessage != null) {
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .fillMaxWidth()
              .padding(CyberTokens.spaceMedium)
          ) {
            FuturisticCard(
              headerText = "AGENT PRESENCE GREETING",
              badgeText = "Personalized"
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = greetingMessage ?: "",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FuturisticButton(
                  text = "Dismiss",
                  onClick = { agentController.companionEngine?.dismissGreeting() },
                  isPrimary = true
                )
              }
            }
          }
        }

        // Action Confirmation HUD Overlay
        if (pendingConfirmationAction != null) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(CyberTokens.spaceMedium)
          ) {
            val action = pendingConfirmationAction!!
            FuturisticCard(
              headerText = "ACTION CONFIRMATION REQUIRED",
              badgeText = "Policy Verification"
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                  )
                }

                Text(
                  text = action.confirmationPrompt ?: "Operator authorization required to proceed with this Android system action.",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  FuturisticButton(
                    text = "Authorize Action",
                    onClick = { agentController.confirmPendingAction() },
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                  )
                  FuturisticButton(
                    text = "Cancel",
                    onClick = { agentController.cancelPendingAction() },
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }
      }

      // Bottom Navigation Bar (Shown on top-level screens)
      FuturisticBottomBar(
        currentScreen = currentScreen,
        coreState = coreState,
        onNavigate = { currentScreen = it },
        onVoiceCoreClick = {
          if (currentScreen != Screen.Voice) {
            currentScreen = Screen.Voice
          }
          agentController.toggleVoiceInteraction()
        }
      )
    }

    // Slide-out Navigation Drawer
    FuturisticSidebar(
      isOpen = isSidebarOpen,
      currentScreen = currentScreen,
      onNavigate = { destination ->
        currentScreen = destination
      },
      onClose = { isSidebarOpen = false }
    )
  }
}
