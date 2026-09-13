package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CoreState
import com.example.ui.components.AnimatedVoiceButton
import com.example.ui.navigation.Screen
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary

/**
 * Futuristic Bottom Navigation Bar.
 * Holds exactly five primary destinations:
 * 1. Home
 * 2. Chat
 * 3. Voice (Animated Voice Core Button in visual center)
 * 4. Vision
 * 5. Agent Town
 * Strictly obeys WindowInsets navigation bar padding.
 */
@Composable
fun FuturisticBottomBar(
  currentScreen: Screen,
  coreState: CoreState,
  onNavigate: (Screen) -> Unit,
  onVoiceCoreClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 6.dp, vertical = 6.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    // Glass Surface Container
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(CyberTokens.bottomNavHeight)
        .clip(CyberTokens.shapeLarge)
        .background(CyberTokens.glassBrush)
        .border(
          CyberTokens.borderNormal,
          CyberTokens.borderMutedGlow,
          CyberTokens.shapeLarge
        )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(CyberTokens.bottomNavHeight)
          .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Destination 1: Home
        BottomNavItem(
          screen = Screen.MyAgent,
          displayTitle = "Home",
          isSelected = currentScreen == Screen.MyAgent,
          onClick = { onNavigate(Screen.MyAgent) },
          modifier = Modifier.weight(1f)
        )

        // Destination 2: Chat
        BottomNavItem(
          screen = Screen.Chat,
          displayTitle = "Chat",
          isSelected = currentScreen == Screen.Chat,
          onClick = { onNavigate(Screen.Chat) },
          modifier = Modifier.weight(1f)
        )

        // Destination 3: Voice Core Center Slot
        BottomNavItem(
          screen = Screen.Voice,
          displayTitle = "Voice",
          isSelected = currentScreen == Screen.Voice,
          onClick = {
            if (currentScreen != Screen.Voice) {
              onNavigate(Screen.Voice)
            }
            onVoiceCoreClick()
          },
          modifier = Modifier.weight(1f)
        )

        // Destination 4: Vision
        BottomNavItem(
          screen = Screen.Vision,
          displayTitle = "Vision",
          isSelected = currentScreen == Screen.Vision,
          onClick = { onNavigate(Screen.Vision) },
          modifier = Modifier.weight(1f)
        )

        // Destination 5: Agent Town
        BottomNavItem(
          screen = Screen.AgentTown,
          displayTitle = "Agent Town",
          isSelected = currentScreen == Screen.AgentTown,
          onClick = { onNavigate(Screen.AgentTown) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Destination 3: Central Animated Voice Core Button (Elevated above bottom bar center)
    AnimatedVoiceButton(
      coreState = coreState,
      onClick = onVoiceCoreClick,
      modifier = Modifier.align(Alignment.TopCenter)
    )
  }
}

@Composable
private fun BottomNavItem(
  screen: Screen,
  displayTitle: String = screen.title,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }

  Column(
    modifier = modifier
      .height(CyberTokens.bottomNavHeight)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .testTag("bottom_nav_${screen.route}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = screen.icon,
      contentDescription = displayTitle,
      tint = if (isSelected) CyanNeon else TextTertiary,
      modifier = Modifier.size(20.dp)
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = displayTitle.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 8.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = 0.4.sp,
        color = if (isSelected) CyanNeon else TextTertiary
      ),
      maxLines = 1,
      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
    )
  }
}
