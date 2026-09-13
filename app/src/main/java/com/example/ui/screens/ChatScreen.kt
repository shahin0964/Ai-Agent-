package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AgentState
import com.example.core.model.ChatMessage
import com.example.ui.components.FuturisticCard
import com.example.ui.components.GlassPanel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Futuristic Conversational Interface.
 * Connects to the Shared Brain (AgentOrchestrator).
 * Real-time inference status, error handling, and truthful reporting.
 */
@Composable
fun ChatScreen(
  messages: List<ChatMessage>,
  onSendMessage: (String) -> Unit,
  onVoiceShortcut: () -> Unit,
  modifier: Modifier = Modifier,
  agentState: AgentState? = null
) {
  var inputText by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = CyberTokens.spaceLarge, vertical = CyberTokens.spaceSmall)
  ) {
    // Header telemetry bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = CyberTokens.spaceSmall),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "TERMINAL // CONVERSATION",
        style = MaterialTheme.typography.labelSmall.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        )
      )
      StatusBadge(
        text = when (agentState) {
          AgentState.PROCESSING -> "INFERENCE: PROCESSING..."
          AgentState.SPEAKING -> "INFERENCE: SYNTHESIZING"
          AgentState.ERROR -> "INFERENCE: ERROR"
          AgentState.OFFLINE -> "INFERENCE: OFFLINE"
          else -> "INFERENCE: READY"
        },
        indicatorColor = when (agentState) {
          AgentState.PROCESSING -> CyanNeon
          AgentState.SPEAKING -> ElectricBlue
          AgentState.ERROR -> StatusAlert
          else -> HologramViolet
        },
        isPulsing = agentState == AgentState.PROCESSING
      )
    }


    // Message List or Truthful Empty State
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (messages.isEmpty()) {
        // Truthful Empty State (Explicitly required: NO fake conversations)
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(CyberTokens.spaceLarge),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(Color(0x3300F0FF))
              .border(CyberTokens.borderNormal, CyanNeon.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.SmartToy,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(34.dp)
            )
          }

          Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

          Text(
            text = "AGENT CONVERSATION CHANNEL",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = TextPrimary
            )
          )

          Spacer(modifier = Modifier.height(CyberTokens.spaceSmall))

          Text(
            text = "Ask questions, explore device capabilities, manage automations, or start a voice conversation.",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextSecondary,
              textAlign = TextAlign.Center,
              lineHeight = 20.sp
            )
          )

          Spacer(modifier = Modifier.height(CyberTokens.spaceLarge))

          GlassPanel(
            modifier = Modifier.fillMaxWidth(0.9f)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Conversational intelligence with live multi-provider reasoning and local state memory.",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextTertiary,
                  fontSize = 11.sp
                )
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(messages, key = { it.id }) { msg ->
            ChatBubble(message = msg)
          }

          if (agentState == AgentState.PROCESSING) {
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color(0x3300F0FF),
                  border = androidx.compose.foundation.BorderStroke(CyberTokens.borderHairline, CyanNeon.copy(alpha = 0.4f)),
                  modifier = Modifier.padding(start = 8.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    StatusBadge(text = "THINKING", indicatorColor = CyanNeon, isPulsing = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Reasoning through prompt...",
                      style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                  }
                }
              }
            }
          }
        }

      }
    }

    // Message Composer Bar
    GlassPanel(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = CyberTokens.spaceSmall),
      shape = CyberTokens.shapeMedium
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Voice shortcut
        IconButton(
          onClick = onVoiceShortcut,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x3300F0FF))
            .testTag("chat_voice_shortcut")
        ) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Shortcut",
            tint = CyanNeon,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Input Field
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = {
            Text(
              text = "Transmit command or query...",
              style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon.copy(alpha = 0.8f),
            unfocusedBorderColor = Color(0x3300F0FF),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanNeon
          ),
          shape = CyberTokens.shapeSmall,
          keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(
            onSend = {
              if (inputText.isNotBlank()) {
                onSendMessage(inputText)
                inputText = ""
              }
            }
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
          onClick = {
            if (inputText.isNotBlank()) {
              onSendMessage(inputText)
              inputText = ""
            }
          },
          enabled = inputText.isNotBlank(),
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (inputText.isNotBlank()) CyanNeon else Color(0x2200F0FF))
            .testTag("chat_send_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Transmit Message",
            tint = if (inputText.isNotBlank()) Color(0xFF030712) else TextTertiary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
  val isUser = message.isUser

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 2.dp,
        bottomEnd = if (isUser) 2.dp else 16.dp
      ),
      color = if (isUser) Color(0xDD0E2B4B) else if (message.isError) Color(0xDD3B141C) else Color(0xDD151C33),
      border = androidx.compose.foundation.BorderStroke(
        CyberTokens.borderHairline,
        if (isUser) CyanNeon.copy(alpha = 0.5f) else if (message.isError) StatusAlert.copy(alpha = 0.6f) else HologramViolet.copy(alpha = 0.4f)
      ),
      modifier = Modifier.fillMaxWidth(0.85f)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isUser) "OPERATOR" else if (message.isError) "SYSTEM NOTIFICATION" else "AEGIS AI",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (isUser) CyanNeon else if (message.isError) StatusAlert else HologramViolet,
              letterSpacing = 1.sp
            )
          )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
        )
      }
    }
  }
}
