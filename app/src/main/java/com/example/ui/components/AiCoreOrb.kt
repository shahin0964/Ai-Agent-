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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.model.AgentState
import com.example.core.model.CoreState
import com.example.core.model.toCoreState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusOffline
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Central Animated AI Core Orb.
 * Distinct animation language for each state:
 * - IDLE: slow breathing/pulsing, subtle orbital movement
 * - LISTENING: expanding acoustic rings, reactive amplitude
 * - PROCESSING: rotating neural/data visualization arcs & glyph nodes
 * - SPEAKING: modulated waveform rings radiating outwards
 * - OFFLINE: restrained dimmed/offline state
 * - ERROR: warning amber / dimmed status
 */
@Composable
fun AiCoreOrb(
  state: CoreState,
  modifier: Modifier = Modifier,
  size: Dp = 220.dp,
  onClick: (() -> Unit)? = null
) {
  val agentState = when (state) {
    CoreState.IDLE -> AgentState.IDLE
    CoreState.LISTENING -> AgentState.LISTENING
    CoreState.THINKING -> AgentState.PROCESSING
    CoreState.SPEAKING -> AgentState.SPEAKING
    CoreState.OFFLINE -> AgentState.OFFLINE
  }
  AiCoreOrb(
    agentState = agentState,
    modifier = modifier,
    size = size,
    amplitude = 0f,
    onClick = onClick
  )
}

@Composable
fun AiCoreOrb(
  agentState: AgentState,
  modifier: Modifier = Modifier,
  size: Dp = 220.dp,
  amplitude: Float = 0f,
  onClick: (() -> Unit)? = null
) {
  val infiniteTransition = rememberInfiniteTransition(label = "CoreAnimation")

  // Breathing pulse
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = if (agentState == AgentState.OFFLINE) 0.96f else 1.05f + (amplitude * 0.15f),
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = when (agentState) {
          AgentState.LISTENING -> 1400
          AgentState.PROCESSING -> 900
          AgentState.SPEAKING -> 800
          AgentState.OFFLINE, AgentState.ERROR -> 5000
          AgentState.IDLE, AgentState.INITIALIZING, AgentState.PAUSED -> 3000
        },
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseScale"
  )

  // Continuous rotation for orbital rings and neural nodes
  val rotationSlow by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = when (agentState) {
          AgentState.PROCESSING -> 3500
          AgentState.SPEAKING -> 4500
          AgentState.LISTENING -> 6000
          AgentState.OFFLINE, AgentState.ERROR -> 20000
          AgentState.IDLE, AgentState.INITIALIZING, AgentState.PAUSED -> 12000
        },
        easing = LinearEasing
      )
    ),
    label = "SlowRotation"
  )

  // Fast counter-rotation for thinking state
  val counterRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (agentState == AgentState.PROCESSING) 2400 else 8000,
        easing = LinearEasing
      )
    ),
    label = "CounterRotation"
  )

  // Wave expansion for listening/speaking
  val waveProgress by infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (agentState == AgentState.LISTENING) 1600 else 2200,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "WaveProgress"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .size(size)
      .testTag("ai_core_orb")
      .then(
        if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
        } else Modifier
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
      val radius = (size.toPx() / 2f) * 0.85f

      when (agentState) {
        AgentState.IDLE, AgentState.INITIALIZING, AgentState.PAUSED -> {
          drawIdleCore(center, radius, pulseScale, rotationSlow)
        }
        AgentState.LISTENING -> {
          drawListeningCore(center, radius, pulseScale + (amplitude * 0.2f), waveProgress, rotationSlow)
        }
        AgentState.PROCESSING -> {
          drawThinkingCore(center, radius, pulseScale, rotationSlow, counterRotation)
        }
        AgentState.SPEAKING -> {
          drawSpeakingCore(center, radius, pulseScale, waveProgress, rotationSlow)
        }
        AgentState.OFFLINE, AgentState.ERROR -> {
          drawOfflineCore(center, radius, pulseScale)
        }
      }
    }
  }
}


private fun DrawScope.drawIdleCore(
  center: Offset,
  radius: Float,
  pulse: Float,
  rotation: Float
) {
  // Ambient Outer Aura
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        CyanNeon.copy(alpha = 0.15f),
        ElectricBlue.copy(alpha = 0.05f),
        Color.Transparent
      ),
      center = center,
      radius = radius * 1.15f * pulse
    ),
    radius = radius * 1.15f * pulse,
    center = center
  )

  // Outer Orbital Ring with dashed ticks
  rotate(rotation, pivot = center) {
    drawCircle(
      color = CyanNeon.copy(alpha = 0.35f),
      radius = radius * 0.95f,
      center = center,
      style = Stroke(
        width = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
      )
    )

    // Orbital satellite nodes
    val nodeAngle = Math.toRadians(45.0)
    val nodeX = center.x + (radius * 0.95f) * cos(nodeAngle).toFloat()
    val nodeY = center.y + (radius * 0.95f) * sin(nodeAngle).toFloat()
    drawCircle(
      color = CyanNeon,
      radius = 3.dp.toPx(),
      center = Offset(nodeX, nodeY)
    )
  }

  // Inner Reverse Orbital Ring
  rotate(-rotation * 0.7f, pivot = center) {
    drawCircle(
      color = HologramViolet.copy(alpha = 0.3f),
      radius = radius * 0.75f,
      center = center,
      style = Stroke(
        width = 1.2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 40f), 0f)
      )
    )
  }

  // Glowing Core Sphere
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color.White,
        CyanNeon,
        ElectricBlue.copy(alpha = 0.8f),
        Color(0xFF0D1B36)
      ),
      center = center,
      radius = radius * 0.5f * pulse
    ),
    radius = radius * 0.5f * pulse,
    center = center
  )

  // Reticle Crosshairs
  val crosshairLen = 8.dp.toPx()
  drawLine(
    color = CyanNeon.copy(alpha = 0.8f),
    start = Offset(center.x - crosshairLen, center.y),
    end = Offset(center.x + crosshairLen, center.y),
    strokeWidth = 1.5.dp.toPx(),
    cap = StrokeCap.Round
  )
  drawLine(
    color = CyanNeon.copy(alpha = 0.8f),
    start = Offset(center.x, center.y - crosshairLen),
    end = Offset(center.x, center.y + crosshairLen),
    strokeWidth = 1.5.dp.toPx(),
    cap = StrokeCap.Round
  )
}

private fun DrawScope.drawListeningCore(
  center: Offset,
  radius: Float,
  pulse: Float,
  waveProgress: Float,
  rotation: Float
) {
  // Expanding acoustic sound waves
  val waveRadius1 = radius * (0.5f + waveProgress * 0.55f)
  val waveAlpha1 = (1f - waveProgress).coerceIn(0f, 1f) * 0.7f
  drawCircle(
    color = CyanNeon.copy(alpha = waveAlpha1),
    radius = waveRadius1,
    center = center,
    style = Stroke(width = 2.dp.toPx())
  )

  val waveProgress2 = ((waveProgress + 0.5f) % 1.0f)
  val waveRadius2 = radius * (0.5f + waveProgress2 * 0.55f)
  val waveAlpha2 = (1f - waveProgress2).coerceIn(0f, 1f) * 0.5f
  drawCircle(
    color = ElectricBlue.copy(alpha = waveAlpha2),
    radius = waveRadius2,
    center = center,
    style = Stroke(width = 1.5.dp.toPx())
  )

  // Outer reactive ring
  rotate(rotation * 1.5f, pivot = center) {
    drawCircle(
      color = CyanNeon,
      radius = radius * 0.85f * pulse,
      center = center,
      style = Stroke(
        width = 2.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f, 40f, 15f), 0f)
      )
    )
  }

  // Active Center
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color.White,
        CyanNeon,
        ElectricBlue,
        Color(0xFF032B44)
      ),
      center = center,
      radius = radius * 0.55f * pulse
    ),
    radius = radius * 0.55f * pulse,
    center = center
  )
}

private fun DrawScope.drawThinkingCore(
  center: Offset,
  radius: Float,
  pulse: Float,
  rotation: Float,
  counterRotation: Float
) {
  // Neural Data Aura
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        HologramViolet.copy(alpha = 0.25f),
        CyanNeon.copy(alpha = 0.1f),
        Color.Transparent
      ),
      center = center,
      radius = radius * 1.1f
    ),
    radius = radius * 1.1f,
    center = center
  )

  // Clockwise Neural Ring
  rotate(rotation, pivot = center) {
    drawCircle(
      color = HologramViolet,
      radius = radius * 0.9f,
      center = center,
      style = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
      )
    )

    // Data packet nodes
    for (i in 0 until 4) {
      val angle = Math.toRadians((i * 90.0))
      val x = center.x + (radius * 0.9f) * cos(angle).toFloat()
      val y = center.y + (radius * 0.9f) * sin(angle).toFloat()
      drawCircle(
        color = CyanNeon,
        radius = 3.5.dp.toPx(),
        center = Offset(x, y)
      )
    }
  }

  // Counter-rotating Inner Ring
  rotate(counterRotation, pivot = center) {
    drawCircle(
      color = CyanNeon.copy(alpha = 0.8f),
      radius = radius * 0.7f,
      center = center,
      style = Stroke(
        width = 1.8.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(50f, 25f), 0f)
      )
    )
  }

  // Shifting Violet-Cyan Core
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color.White,
        HologramViolet,
        Color(0xFF4C1D95),
        Color(0xFF0F172A)
      ),
      center = center,
      radius = radius * 0.5f * pulse
    ),
    radius = radius * 0.5f * pulse,
    center = center
  )
}

private fun DrawScope.drawSpeakingCore(
  center: Offset,
  radius: Float,
  pulse: Float,
  waveProgress: Float,
  rotation: Float
) {
  // Modulated voice waveform rings
  val ringCount = 3
  for (i in 0 until ringCount) {
    val phase = (waveProgress + (i.toFloat() / ringCount)) % 1f
    val currentR = radius * (0.5f + phase * 0.5f)
    val currentA = (1f - phase).coerceIn(0f, 1f) * 0.6f
    drawCircle(
      color = if (i % 2 == 0) CyanNeon.copy(alpha = currentA) else HologramViolet.copy(alpha = currentA),
      radius = currentR,
      center = center,
      style = Stroke(width = 2.dp.toPx())
    )
  }

  // Rotating audio aperture
  rotate(rotation, pivot = center) {
    drawCircle(
      color = ElectricBlue.copy(alpha = 0.9f),
      radius = radius * 0.85f * pulse,
      center = center,
      style = Stroke(
        width = 2.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f, 5f, 10f), 0f)
      )
    )
  }

  // Speaking core
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color.White,
        CyanNeon,
        HologramViolet,
        Color(0xFF0A1128)
      ),
      center = center,
      radius = radius * 0.52f * pulse
    ),
    radius = radius * 0.52f * pulse,
    center = center
  )
}

private fun DrawScope.drawOfflineCore(
  center: Offset,
  radius: Float,
  pulse: Float
) {
  // Restrained, dimmed, quiet state
  drawCircle(
    color = StatusOffline.copy(alpha = 0.2f),
    radius = radius * 0.85f,
    center = center,
    style = Stroke(
      width = 1.dp.toPx(),
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 0f)
    )
  )

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        StatusOffline.copy(alpha = 0.5f),
        Color(0xFF1E293B),
        Color(0xFF070B14)
      ),
      center = center,
      radius = radius * 0.45f * pulse
    ),
    radius = radius * 0.45f * pulse,
    center = center
  )
}
