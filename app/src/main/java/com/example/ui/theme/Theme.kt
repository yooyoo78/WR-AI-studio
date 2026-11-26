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
import com.example.data.local.UserPreferences
import com.example.data.model.AppDarkThemePreset
import com.example.data.model.AppLanguage
import com.example.data.model.IconSizePreset

val LocalUserPrefs = staticCompositionLocalOf { UserPreferences() }
val LocalUiStrings = staticCompositionLocalOf { AppStrings.get(AppLanguage.TH) }
val LocalInGameMiniMode = staticCompositionLocalOf { false }
val LocalRiftPalette =
  staticCompositionLocalOf { riftPaletteFor(AppDarkThemePreset.HEXTECH_OBSIDIAN) }
val LocalIconSizePreset = staticCompositionLocalOf { IconSizePreset.COMPACT }

private fun buildColorScheme(palette: RiftThemePalette, highContrast: Boolean) =
  darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.primaryBright,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.secondaryContainer,
    onSecondaryContainer = palette.onSecondaryContainer,
    tertiary = palette.tertiary,
    onTertiary = palette.onTertiary,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surfaceNavy,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surface,
    onSurfaceVariant = palette.textSecondary,
    surfaceContainerLow = palette.surfaceNavy,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.surfaceHigh,
    surfaceContainerHighest = palette.surfaceHighest,
    outline = if (highContrast) palette.outlineHighContrast else palette.outline,
    outlineVariant = if (highContrast) palette.primaryMuted else palette.outlineVariant,
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
  val palette = riftPaletteFor(preferences.darkThemePreset)
  val colorScheme = buildColorScheme(palette, preferences.highContrastCards)
  val typography = createScaledTypography(preferences.fontScaleMultiplier, preferences.fontPreset)

  CompositionLocalProvider(
    LocalUserPrefs provides preferences,
    LocalUiStrings provides strings,
    LocalInGameMiniMode provides inGameMiniMode,
    LocalRiftPalette provides palette,
    LocalIconSizePreset provides preferences.iconSizePreset,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = HextechShapes,
      content = content,
    )
  }
}
