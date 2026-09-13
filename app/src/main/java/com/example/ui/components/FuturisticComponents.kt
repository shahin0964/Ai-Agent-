package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.CoreState
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberTokens
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HologramViolet
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Reusable Glassmorphism Surface Container with holographic border.
 */
@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  shape: Shape = CyberTokens.shapeMedium,
  borderBrush: Brush = CyberTokens.borderMutedGlow,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(CyberTokens.glassBrush)
      .border(CyberTokens.borderNormal, borderBrush, shape)
  ) {
    // Subtle sci-fi corner bracket accents
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height
      val cornerLen = 10.dp.toPx()
      val strokeW = 1.dp.toPx()

      // Top-left bracket
      drawLine(CyanNeon.copy(alpha = 0.4f), Offset(4f, 4f), Offset(4f + cornerLen, 4f), strokeW)
      drawLine(CyanNeon.copy(alpha = 0.4f), Offset(4f, 4f), Offset(4f, 4f + cornerLen), strokeW)

      // Bottom-right bracket
      drawLine(CyanNeon.copy(alpha = 0.4f), Offset(w - 4f, h - 4f), Offset(w - 4f - cornerLen, h - 4f), strokeW)
      drawLine(CyanNeon.copy(alpha = 0.4f), Offset(w - 4f, h - 4f), Offset(w - 4f, h - 4f - cornerLen), strokeW)
    }

    content()
  }
}

/**
 * Futuristic card with interactive tap support and cyber styling.
 */
@Composable
fun FuturisticCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  headerText: String? = null,
  headerColor: Color? = null,
  badgeText: String? = null,
  badgeColor: Color = CyanNeon,
  content: @Composable () -> Unit
) {
  GlassPanel(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(CyberTokens.spaceMedium)
    ) {
      if (headerText != null || badgeText != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (headerText != null) {
            Text(
              text = headerText.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor ?: CyanNeon
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
          } else {
            Spacer(modifier = Modifier.width(1.dp))
          }

          if (badgeText != null) {
            Spacer(modifier = Modifier.width(6.dp))
            StatusBadge(text = badgeText, indicatorColor = badgeColor)
          }
        }
        Spacer(modifier = Modifier.height(CyberTokens.spaceSmall))
      }

      content()
    }
  }
}

/**
 * Reusable Futuristic Header with navigation back button and status title.
 */
@Composable
fun FuturisticHeader(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  badgeText: String? = null,
  badgeColor: Color = CyanNeon,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f, fill = false)
    ) {
      if (onBack != null) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(36.dp).testTag("header_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = CyanNeon
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
      }

      Column(modifier = Modifier.weight(1f, fill = false)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TextPrimary
          ),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    if (badgeText != null) {
      Spacer(modifier = Modifier.width(6.dp))
      StatusBadge(text = badgeText, indicatorColor = badgeColor)
    }
  }
}

/**
 * Sci-Fi Status Badge indicator with optional emoji support.
 */
@Composable
fun StatusBadge(
  text: String,
  modifier: Modifier = Modifier,
  indicatorColor: Color = CyanNeon,
  emoji: String? = null,
  isPulsing: Boolean = true
) {
  val infiniteTransition = rememberInfiniteTransition(label = "BadgePulse")
  val alpha by if (isPulsing) {
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "BadgeDotAlpha"
    )
  } else {
    rememberInfiniteTransition(label = "Static").animateFloat(
      initialValue = 1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1000)), label = ""
    )
  }

  Surface(
    modifier = modifier,
    shape = CyberTokens.shapePill,
    color = indicatorColor.copy(alpha = 0.14f),
    border = BorderStroke(CyberTokens.borderHairline, indicatorColor.copy(alpha = 0.45f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (emoji != null && emoji.isNotBlank()) {
        Text(
          text = emoji,
          fontSize = 11.sp,
          modifier = Modifier.padding(end = 4.dp)
        )
      } else {
        Box(
          modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(indicatorColor.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(5.dp))
      }
      Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.6.sp,
          color = indicatorColor
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Reusable Futuristic Button.
 */
@Composable
fun FuturisticButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .height(42.dp)
      .testTag("futuristic_button_${text.lowercase().replace(" ", "_")}"),
    shape = CyberTokens.shapeMedium,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isPrimary) CyanNeon else Color(0xFF14213D),
      contentColor = if (isPrimary) Color(0xFF030712) else CyanNeon,
      disabledContainerColor = Color(0xFF111827),
      disabledContentColor = TextTertiary
    ),
    border = if (!isPrimary) BorderStroke(CyberTokens.borderNormal, CyanNeon.copy(alpha = 0.6f)) else null
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelMedium.copy(
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Telemetry HUD Top Header bar showing agent identity and live emotion/mood presence.
 */
@Composable
fun HudTelemetryHeader(
  agentName: String = "Aegis",
  coreState: CoreState = CoreState.IDLE,
  moodState: com.example.core.companion.CompanionMoodState = com.example.core.companion.CompanionMoodState.ONLINE,
  onOpenSidebar: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = CyberTokens.spaceMedium, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Menu trigger with sci-fi look
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CyberTokens.shapeSmall)
        .background(Color(0x880E1B38))
        .border(CyberTokens.borderHairline, CyberGlassBorder, CyberTokens.shapeSmall)
        .clickable(onClick = onOpenSidebar)
        .testTag("sidebar_hamburger_button"),
      contentAlignment = Alignment.Center
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(modifier = Modifier.size(width = 16.dp, height = 2.dp).background(CyanNeon))
        Box(modifier = Modifier.size(width = 10.dp, height = 2.dp).background(ElectricBlue))
        Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(CyanNeon))
      }
    }

    // Telemetry Brand / State with Custom Dynamic Agent Name
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.weight(1f, fill = false).padding(horizontal = 8.dp)
    ) {
      val displayName = if (agentName.isNotBlank()) agentName.uppercase() else "AEGIS"
      Text(
        text = "$displayName // CORE",
        style = MaterialTheme.typography.labelMedium.copy(
          letterSpacing = 1.5.sp,
          fontWeight = FontWeight.Bold,
          color = CyanNeon
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = "INTELLIGENCE ENGINE",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 8.5.sp,
          color = TextTertiary,
          letterSpacing = 0.8.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Live Emotion & Mood State Pill with Emoji (Online, Offline, Happy, Angry, Sad, Sleep, etc.)
    StatusBadge(
      text = moodState.label,
      emoji = moodState.emoji,
      indicatorColor = moodState.color
    )
  }
}

/**
 * Animated Sound Waveform Visualizer bars.
 */
@Composable
fun WaveformVisualizer(
  coreState: CoreState,
  modifier: Modifier = Modifier,
  barCount: Int = 18,
  height: Dp = 48.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "WaveformTransition")

  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (coreState == CoreState.SPEAKING) 800 else 2000,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "WavePhase"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
      val w = size.width
      val h = size.height
      val barWidth = 3.dp.toPx()
      val totalGap = (w - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

      for (i in 0 until barCount) {
        val x = i * (barWidth + totalGap)
        val waveFactor = when (coreState) {
          CoreState.SPEAKING -> (kotlin.math.sin(phase + (i * 0.4f)) * 0.4f + 0.55f).coerceIn(0.15f, 1f)
          CoreState.LISTENING -> (kotlin.math.sin(phase + (i * 0.25f)) * 0.25f + 0.35f).coerceIn(0.1f, 0.7f)
          CoreState.THINKING -> (kotlin.math.sin(phase * 2f + (i * 0.5f)) * 0.2f + 0.3f).coerceIn(0.1f, 0.5f)
          CoreState.OFFLINE -> 0.05f
          CoreState.IDLE -> (kotlin.math.sin(phase + (i * 0.15f)) * 0.1f + 0.15f).coerceIn(0.08f, 0.3f)
        }

        val barH = h * waveFactor
        val top = (h - barH) / 2f

        val color = when {
          coreState == CoreState.THINKING -> HologramViolet
          coreState == CoreState.SPEAKING -> ElectricBlue
          coreState == CoreState.OFFLINE -> StatusOffline
          else -> CyanNeon
        }

        drawRoundRect(
          color = color.copy(alpha = if (coreState == CoreState.OFFLINE) 0.3f else 0.85f),
          topLeft = Offset(x, top),
          size = androidx.compose.ui.geometry.Size(barWidth, barH),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
      }
    }
  }
}
