package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.CoreState
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet

/**
 * Animated Voice Core Button positioned prominently in the bottom navigation.
 * Not a static microphone icon - acts as a miniature holographic voice orb.
 */
@Composable
fun AnimatedVoiceButton(
  coreState: CoreState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "VoiceButtonTransition")

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = if (coreState == CoreState.LISTENING || coreState == CoreState.SPEAKING) 1.15f else 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (coreState == CoreState.LISTENING) 900 else 2400,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseVoiceBtn"
  )

  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (coreState == CoreState.THINKING) 2500 else 8000,
        easing = LinearEasing
      )
    ),
    label = "RotateVoiceBtn"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .offset(y = (-14).dp) // Elevate visually above bottom bar
      .size(CyberTokens.voiceOrbButtonSize)
      .testTag("animated_voice_button"),
    contentAlignment = Alignment.Center
  ) {
    // Holographic outer aura & orbiting rings canvas
    Canvas(modifier = Modifier.size(CyberTokens.voiceOrbButtonSize)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = (size.width / 2f) * 0.95f

      // Ambient radial glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            when (coreState) {
              CoreState.THINKING -> HologramViolet.copy(alpha = 0.4f)
              CoreState.LISTENING -> CyanNeon.copy(alpha = 0.5f)
              CoreState.SPEAKING -> ElectricBlue.copy(alpha = 0.45f)
              else -> CyanNeon.copy(alpha = 0.25f)
            },
            Color.Transparent
          ),
          center = center,
          radius = radius * pulseScale
        ),
        radius = radius * pulseScale,
        center = center
      )

      // Orbiting segmented ring
      rotate(rotation, pivot = center) {
        drawCircle(
          color = when (coreState) {
            CoreState.THINKING -> HologramViolet
            CoreState.LISTENING -> CyanNeon
            CoreState.SPEAKING -> ElectricBlue
            else -> CyanNeon.copy(alpha = 0.5f)
          },
          radius = radius * 0.92f,
          center = center,
          style = Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 18f), 0f)
          )
        )
      }
    }

    // Inner Button Orb
    Box(
      modifier = Modifier
        .size(CyberTokens.voiceOrbInnerSize)
        .clip(CircleShape)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        ),
      contentAlignment = Alignment.Center
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White,
              when (coreState) {
                CoreState.THINKING -> HologramViolet
                CoreState.LISTENING -> CyanNeon
                CoreState.SPEAKING -> ElectricBlue
                else -> CyanNeon
              },
              Color(0xFF071224)
            ),
            center = center,
            radius = radius * 1.1f
          ),
          radius = radius,
          center = center
        )

        drawCircle(
          color = CyanNeon.copy(alpha = 0.8f),
          radius = radius - 1.dp.toPx(),
          center = center,
          style = Stroke(width = 1.2.dp.toPx())
        )
      }

      // Icon overlay
      Icon(
        imageVector = if (coreState == CoreState.SPEAKING) Icons.Default.GraphicEq else Icons.Default.Mic,
        contentDescription = "Voice AI Core Trigger",
        tint = Color(0xFF030712),
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
