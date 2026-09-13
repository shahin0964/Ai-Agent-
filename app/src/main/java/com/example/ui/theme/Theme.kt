package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val currentThemeId by AppThemeState.currentTheme.collectAsState()
  val palette = ThemePalette(
    themeId = currentThemeId,
    primary = currentThemeId.primaryColor,
    secondary = currentThemeId.secondaryColor,
    accent = currentThemeId.accentColor,
    surface = currentThemeId.surfaceColor,
    background = currentThemeId.backgroundColor,
    glassBorder = currentThemeId.primaryColor.copy(alpha = 0.25f),
    glowPrimary = currentThemeId.primaryColor.copy(alpha = 0.35f),
    glowAccent = currentThemeId.accentColor.copy(alpha = 0.30f)
  )

  val dynamicColorScheme = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.background,
    primaryContainer = palette.surface,
    onPrimaryContainer = palette.primary,
    secondary = palette.secondary,
    onSecondary = palette.background,
    secondaryContainer = palette.surface,
    onSecondaryContainer = palette.secondary,
    tertiary = palette.accent,
    onTertiary = PureWhite,
    tertiaryContainer = palette.surface,
    onTertiaryContainer = palette.accent,
    background = palette.background,
    onBackground = TextPrimary,
    surface = palette.surface,
    onSurface = TextPrimary,
    surfaceVariant = palette.surface,
    onSurfaceVariant = TextSecondary,
    outline = palette.glassBorder,
    outlineVariant = Color(0xFF1E293B)
  )

  CompositionLocalProvider(LocalAppTheme provides palette) {
    MaterialTheme(
      colorScheme = dynamicColorScheme,
      typography = Typography,
      content = content
    )
  }
}

