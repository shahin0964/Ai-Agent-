package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Design System Constants for the Futuristic AI Agent.
 * Avoids scattered hardcoded values across screens.
 */
object CyberTokens {
  // Spacing System (Compact Android Mobile 4/6/10/14/18/24dp grid)
  val spaceExtraSmall: Dp = 4.dp
  val spaceSmall: Dp = 6.dp
  val spaceMedium: Dp = 10.dp
  val spaceLarge: Dp = 14.dp
  val spaceExtraLarge: Dp = 18.dp
  val spaceSection: Dp = 24.dp

  // Corner Radii
  val radiusSmall: Dp = 6.dp
  val radiusMedium: Dp = 10.dp
  val radiusLarge: Dp = 16.dp
  val radiusFull: Dp = 100.dp

  val shapeSmall = RoundedCornerShape(radiusSmall)
  val shapeMedium = RoundedCornerShape(radiusMedium)
  val shapeLarge = RoundedCornerShape(radiusLarge)
  val shapePill = RoundedCornerShape(radiusFull)

  // Border Dimensions
  val borderHairline: Dp = 0.8.dp
  val borderNormal: Dp = 1.dp
  val borderThick: Dp = 1.8.dp

  // Navigation Dimensions (Optimized for Android Portrait Phones)
  val bottomBarHeight: Dp = 62.dp
  val bottomNavHeight: Dp = 62.dp
  val voiceOrbButtonSize: Dp = 54.dp
  val voiceOrbInnerSize: Dp = 44.dp
  val sidebarWidth: Dp = 275.dp
  val topBarHeight: Dp = 48.dp
  val minTouchTarget: Dp = 48.dp

  // Animation Durations (ms)
  const val animDurationFast: Int = 180
  const val animDurationNormal: Int = 300
  const val animDurationSlow: Int = 600
  const val animDurationOrbBreathing: Int = 3200
  const val animDurationWaveform: Int = 800
  const val animDurationOrbit: Int = 6000

  // Surface and Border Tokens
  val SurfaceDark: Color = CyberSurface
  val BorderSubtle: Color = CyberGlassBorder
  val NeonCyan: Color = CyanNeon
  val NeonGreen: Color = StatusReady
  val NeonMagenta: Color = HologramViolet
  val StatusError: Color = StatusAlert

  // Glass Surfaces and Hologram Gradients
  val glassBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xEE0E1A33),
      Color(0xDD080E1D)
    )
  )

  val cardGradient = Brush.linearGradient(
    colors = listOf(
      Color(0xCC111E38),
      Color(0xAA0B1324)
    )
  )

  val borderCyanGlow = Brush.linearGradient(
    colors = listOf(
      CyanNeon.copy(alpha = 0.7f),
      ElectricBlue.copy(alpha = 0.3f),
      HologramViolet.copy(alpha = 0.6f)
    )
  )

  val borderMutedGlow = Brush.linearGradient(
    colors = listOf(
      CyanNeon.copy(alpha = 0.25f),
      Color.Transparent,
      HologramViolet.copy(alpha = 0.25f)
    )
  )

  val activeVoiceGlow = Brush.radialGradient(
    colors = listOf(
      CyanNeon.copy(alpha = 0.45f),
      ElectricBlue.copy(alpha = 0.2f),
      Color.Transparent
    )
  )
}
