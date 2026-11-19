package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.AppStrings
import com.example.core.UiStrings
import com.example.data.local.UserPreferences

val LocalUserPrefs = staticCompositionLocalOf { UserPreferences() }
val LocalUiStrings = staticCompositionLocalOf { AppStrings.get(com.example.data.model.AppLanguage.TH) }
val LocalInGameMiniMode = staticCompositionLocalOf { false }

private fun buildHextechColorScheme(highContrast: Boolean) =
  darkColorScheme(
    primary = HextechGold,
    onPrimary = Color(0xFF140F02),
    primaryContainer = Color(0xFF332609),
    onPrimaryContainer = HextechGoldBright,
    secondary = ArcaneCyan,
    onSecondary = Color(0xFF031520),
    secondaryContainer = Color(0xFF0D2B42),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = SkillViolet,
    onTertiary = Color(0xFF180E33),
    background = HextechObsidian,
    onBackground = TextPrimaryLight,
    surface = HextechNavy,
    onSurface = TextPrimaryLight,
    surfaceVariant = HextechSurface,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = HextechNavy,
    surfaceContainer = HextechSurface,
    surfaceContainerHigh = HextechSurfaceHigh,
    surfaceContainerHighest = HextechSurfaceHighest,
    outline = if (highContrast) HextechOutlineHighContrast else HextechOutline,
    outlineVariant = if (highContrast) HextechGoldMuted else Color(0xFF1F2D4A),
    error = ThreatCrimson,
    onError = Color.White,
  )

private val HextechShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun RiftCompanionTheme(
  preferences: UserPreferences = UserPreferences(),
  inGameMiniMode: Boolean = preferences.inGameMiniMode,
  content: @Composable () -> Unit,
) {
  val strings = AppStrings.get(preferences.language)
  val colorScheme = buildHextechColorScheme(preferences.highContrastCards)
  val typography = createScaledTypography(preferences.fontScaleMultiplier)

  CompositionLocalProvider(
    LocalUserPrefs provides preferences,
    LocalUiStrings provides strings,
    LocalInGameMiniMode provides inGameMiniMode,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = HextechShapes,
      content = content,
    )
  }
}
