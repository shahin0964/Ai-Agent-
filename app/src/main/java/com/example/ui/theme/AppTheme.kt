package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mobile-native Theme Identifiers for Futuristic AI Agent.
 */
enum class AppThemeId(
  val id: String,
  val displayName: String,
  val description: String,
  val primaryColor: Color,
  val secondaryColor: Color,
  val accentColor: Color,
  val surfaceColor: Color,
  val backgroundColor: Color
) {
  NEXUS_CYAN(
    id = "nexus_cyan",
    displayName = "Nexus Cyan",
    description = "Signature holographic cyan neon with electric blue telemetry",
    primaryColor = Color(0xFF00F0FF),
    secondaryColor = Color(0xFF38BDF8),
    accentColor = Color(0xFFA855F7),
    surfaceColor = Color(0xFF0E172A),
    backgroundColor = Color(0xFF050811)
  ),
  CYBER_VIOLET(
    id = "cyber_violet",
    displayName = "Cyber Violet",
    description = "Deep synthetic violet with luminous purple and neon cyan highlights",
    primaryColor = Color(0xFFA855F7),
    secondaryColor = Color(0xFFC084FC),
    accentColor = Color(0xFF00F0FF),
    surfaceColor = Color(0xFF160E2A),
    backgroundColor = Color(0xFF080512)
  ),
  CRIMSON_COMMAND(
    id = "crimson_command",
    displayName = "Crimson Command",
    description = "High-contrast tactical crimson with amber indicators",
    primaryColor = Color(0xFFFF0055),
    secondaryColor = Color(0xFFFB7185),
    accentColor = Color(0xFFF59E0B),
    surfaceColor = Color(0xFF240E14),
    backgroundColor = Color(0xFF0F0407)
  ),
  EMERALD_MATRIX(
    id = "emerald_matrix",
    displayName = "Emerald Matrix",
    description = "Cybernetic matrix green with luminous mint telemetry",
    primaryColor = Color(0xFF10B981),
    secondaryColor = Color(0xFF34D399),
    accentColor = Color(0xFF00F0FF),
    surfaceColor = Color(0xFF0B2117),
    backgroundColor = Color(0xFF030E09)
  ),
  ARCTIC_FROST(
    id = "arctic_frost",
    displayName = "Arctic Frost",
    description = "Crystalline glacial ice blue with cool cerulean accents",
    primaryColor = Color(0xFF38BDF8),
    secondaryColor = Color(0xFF93C5FD),
    accentColor = Color(0xFF67E8F9),
    surfaceColor = Color(0xFF0C192E),
    backgroundColor = Color(0xFF040A14)
  ),
  DARK_COMMAND(
    id = "dark_command",
    displayName = "Dark Command",
    description = "Stealth obsidian slate with brushed titanium accents",
    primaryColor = Color(0xFF94A3B8),
    secondaryColor = Color(0xFFCBD5E1),
    accentColor = Color(0xFF38BDF8),
    surfaceColor = Color(0xFF111827),
    backgroundColor = Color(0xFF030712)
  );

  companion object {
    fun fromId(id: String): AppThemeId {
      return entries.find { it.id == id } ?: NEXUS_CYAN
    }
  }
}

/**
 * Dynamic theme configuration holder.
 */
data class ThemePalette(
  val themeId: AppThemeId = AppThemeId.NEXUS_CYAN,
  val primary: Color = themeId.primaryColor,
  val secondary: Color = themeId.secondaryColor,
  val accent: Color = themeId.accentColor,
  val surface: Color = themeId.surfaceColor,
  val background: Color = themeId.backgroundColor,
  val glassBorder: Color = themeId.primaryColor.copy(alpha = 0.25f),
  val glowPrimary: Color = themeId.primaryColor.copy(alpha = 0.35f),
  val glowAccent: Color = themeId.accentColor.copy(alpha = 0.30f)
)

val LocalAppTheme = compositionLocalOf { ThemePalette() }

object AppThemeState {
  @Volatile
  private var prefs: SharedPreferences? = null
  private val _currentTheme = MutableStateFlow(AppThemeId.NEXUS_CYAN)
  val currentTheme: StateFlow<AppThemeId> = _currentTheme.asStateFlow()

  fun init(context: Context) {
    if (prefs == null) {
      prefs = context.applicationContext.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)
      val savedId = prefs?.getString("theme_id", AppThemeId.NEXUS_CYAN.id) ?: AppThemeId.NEXUS_CYAN.id
      _currentTheme.value = AppThemeId.fromId(savedId)
    }
  }

  fun setTheme(themeId: AppThemeId) {
    _currentTheme.value = themeId
    prefs?.edit()?.putString("theme_id", themeId.id)?.apply()
  }
}

/**
 * Convenience accessor for the current active theme palette in Composables.
 */
val activeTheme: ThemePalette
  @Composable
  @ReadOnlyComposable
  get() = LocalAppTheme.current
