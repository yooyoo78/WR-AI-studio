package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppDarkThemePreset

// Default Hextech Dark Obsidian & Navy Tokens (preserved for static color compatibility)
val HextechObsidian = Color(0xFF070B14)
val HextechNavy = Color(0xFF0D1424)
val HextechSurface = Color(0xFF131D33)
val HextechSurfaceHigh = Color(0xFF1A2742)
val HextechSurfaceHighest = Color(0xFF233354)

// Default Hextech Gold & Arcane Cyan Accents
val HextechGold = Color(0xFFE5B842)
val HextechGoldBright = Color(0xFFF6D776)
val HextechGoldMuted = Color(0xFF9E7B27)
val ArcaneCyan = Color(0xFF38BDF8)
val ArcaneCyanSoft = Color(0xFF0EA5E9)

// High-Contrast Text Tokens
val TextPrimaryLight = Color(0xFFF8FAFC)
val TextSecondaryLight = Color(0xFFD1D9E6)
val TextMutedLight = Color(0xFF9AA8BF)

// Tactical Semantic Colors (shared across all dark themes for clear gameplay meaning)
val AdvantageEmerald = Color(0xFF22C55E)
val ThreatCrimson = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val SkillViolet = Color(0xFFA78BFA)

// Outline & Rim Borders
val HextechOutline = Color(0xFF2C3E63)
val HextechOutlineHighContrast = Color(0xFF647CA8)

data class RiftThemePalette(
  val preset: AppDarkThemePreset,
  val background: Color,
  val surfaceNavy: Color,
  val surface: Color,
  val surfaceHigh: Color,
  val surfaceHighest: Color,
  val heroTopTint: Color,
  val primary: Color,
  val primaryBright: Color,
  val primaryMuted: Color,
  val onPrimary: Color,
  val primaryContainer: Color,
  val secondary: Color,
  val secondarySoft: Color,
  val onSecondary: Color,
  val secondaryContainer: Color,
  val onSecondaryContainer: Color,
  val tertiary: Color,
  val onTertiary: Color,
  val outline: Color,
  val outlineVariant: Color,
  val outlineHighContrast: Color,
  val textPrimary: Color = TextPrimaryLight,
  val textSecondary: Color = TextSecondaryLight,
  val textMuted: Color = TextMutedLight,
)

fun riftPaletteFor(preset: AppDarkThemePreset): RiftThemePalette =
  when (preset) {
    AppDarkThemePreset.HEXTECH_OBSIDIAN ->
      RiftThemePalette(
        preset = preset,
        background = HextechObsidian,
        surfaceNavy = HextechNavy,
        surface = HextechSurface,
        surfaceHigh = HextechSurfaceHigh,
        surfaceHighest = HextechSurfaceHighest,
        heroTopTint = Color(0xFF121E36),
        primary = HextechGold,
        primaryBright = HextechGoldBright,
        primaryMuted = HextechGoldMuted,
        onPrimary = Color(0xFF140F02),
        primaryContainer = Color(0xFF332609),
        secondary = ArcaneCyan,
        secondarySoft = ArcaneCyanSoft,
        onSecondary = Color(0xFF031520),
        secondaryContainer = Color(0xFF0D2B42),
        onSecondaryContainer = Color(0xFFBAE6FD),
        tertiary = SkillViolet,
        onTertiary = Color(0xFF180E33),
        outline = Color(0xFF23324F),
        outlineVariant = Color(0xFF18243B),
        outlineHighContrast = HextechOutlineHighContrast,
      )

    AppDarkThemePreset.VOID_ABYSS ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF090714),
        surfaceNavy = Color(0xFF110D22),
        surface = Color(0xFF17132B),
        surfaceHigh = Color(0xFF211B3D),
        surfaceHighest = Color(0xFF2B234F),
        heroTopTint = Color(0xFF1E163B),
        primary = Color(0xFFC084FC),
        primaryBright = Color(0xFFE9D5FF),
        primaryMuted = Color(0xFF7E22CE),
        onPrimary = Color(0xFF1D0733),
        primaryContainer = Color(0xFF341857),
        secondary = Color(0xFFF472B6),
        secondarySoft = Color(0xFFEC4899),
        onSecondary = Color(0xFF2A0819),
        secondaryContainer = Color(0xFF42142D),
        onSecondaryContainer = Color(0xFFFBCFE8),
        tertiary = Color(0xFF60A5FA),
        onTertiary = Color(0xFF0B1C38),
        outline = Color(0xFF332957),
        outlineVariant = Color(0xFF241D40),
        outlineHighContrast = Color(0xFF8B74C4),
      )

    AppDarkThemePreset.SHADOW_EMERALD ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF050D0B),
        surfaceNavy = Color(0xFF0A1714),
        surface = Color(0xFF0F211C),
        surfaceHigh = Color(0xFF162E27),
        surfaceHighest = Color(0xFF1E3D34),
        heroTopTint = Color(0xFF112922),
        primary = Color(0xFF34D399),
        primaryBright = Color(0xFF6EE7B7),
        primaryMuted = Color(0xFF059669),
        onPrimary = Color(0xFF032117),
        primaryContainer = Color(0xFF0B3829),
        secondary = Color(0xFFFBBF24),
        secondarySoft = Color(0xFFD97706),
        onSecondary = Color(0xFF241802),
        secondaryContainer = Color(0xFF3A2806),
        onSecondaryContainer = Color(0xFFFDE68A),
        tertiary = Color(0xFF2DD4BF),
        onTertiary = Color(0xFF04221E),
        outline = Color(0xFF1F3D34),
        outlineVariant = Color(0xFF162C25),
        outlineHighContrast = Color(0xFF4F8C78),
      )

    AppDarkThemePreset.TACTICAL_CARBON ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF090A0D),
        surfaceNavy = Color(0xFF111318),
        surface = Color(0xFF17191E),
        surfaceHigh = Color(0xFF20232B),
        surfaceHighest = Color(0xFF2A2E38),
        heroTopTint = Color(0xFF1D2028),
        primary = Color(0xFFE2E8F0),
        primaryBright = Color(0xFFF8FAFC),
        primaryMuted = Color(0xFF94A3B8),
        onPrimary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF2A313D),
        secondary = Color(0xFFFB923C),
        secondarySoft = Color(0xFFF97316),
        onSecondary = Color(0xFF291102),
        secondaryContainer = Color(0xFF421E07),
        onSecondaryContainer = Color(0xFFFED7AA),
        tertiary = Color(0xFF38BDF8),
        onTertiary = Color(0xFF031520),
        outline = Color(0xFF2E333F),
        outlineVariant = Color(0xFF21252E),
        outlineHighContrast = Color(0xFF788296),
      )
  }
