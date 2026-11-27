package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppDarkThemePreset

// Default Hextech Dark Obsidian & Navy Tokens (preserved for static color compatibility)
val HextechObsidian = Color(0xFF050810)
val HextechNavy = Color(0xFF0B1220)
val HextechSurface = Color(0xFF111A2E)
val HextechSurfaceHigh = Color(0xFF182542)
val HextechSurfaceHighest = Color(0xFF213256)

// Default Hextech Gold & Arcane Cyan Accents (High-Luminance for Dark Surfaces)
val HextechGold = Color(0xFFF5C443)
val HextechGoldBright = Color(0xFFFFE28A)
val HextechGoldMuted = Color(0xFFC2952B)
val ArcaneCyan = Color(0xFF56CCF2)
val ArcaneCyanSoft = Color(0xFF22B8EB)

// High-Contrast Text Tokens (Crisp white primary & luminous secondary/muted)
val TextPrimaryLight = Color(0xFFFFFFFF)
val TextSecondaryLight = Color(0xFFE6EDF7)
val TextMutedLight = Color(0xFFB8C4D9)

// Tactical Semantic Colors (shared across all dark themes for clear gameplay meaning)
val AdvantageEmerald = Color(0xFF34D399)
val ThreatCrimson = Color(0xFFF87171)
val WarningAmber = Color(0xFFFBBF24)
val SkillViolet = Color(0xFFC4B5FD)

// Brighter Outline & Rim Borders
val HextechOutline = Color(0xFF3E5887)
val HextechOutlineHighContrast = Color(0xFF7AA2E3)

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
        heroTopTint = Color(0xFF15223D),
        primary = HextechGold,
        primaryBright = HextechGoldBright,
        primaryMuted = HextechGoldMuted,
        onPrimary = Color(0xFF140E02),
        primaryContainer = Color(0xFF3B2B08),
        secondary = ArcaneCyan,
        secondarySoft = ArcaneCyanSoft,
        onSecondary = Color(0xFF031520),
        secondaryContainer = Color(0xFF0E3452),
        onSecondaryContainer = Color(0xFFD6F1FF),
        tertiary = SkillViolet,
        onTertiary = Color(0xFF180E33),
        outline = HextechOutline,
        outlineVariant = Color(0xFF2B3E63),
        outlineHighContrast = HextechOutlineHighContrast,
      )

    AppDarkThemePreset.VOID_ABYSS ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF06040E),
        surfaceNavy = Color(0xFF0E0A1D),
        surface = Color(0xFF16112B),
        surfaceHigh = Color(0xFF20193D),
        surfaceHighest = Color(0xFF2B2252),
        heroTopTint = Color(0xFF211747),
        primary = Color(0xFFD8B4FE),
        primaryBright = Color(0xFFF3E8FF),
        primaryMuted = Color(0xFFA855F7),
        onPrimary = Color(0xFF19062E),
        primaryContainer = Color(0xFF3D1D66),
        secondary = Color(0xFFF472B6),
        secondarySoft = Color(0xFFEC4899),
        onSecondary = Color(0xFF2A0819),
        secondaryContainer = Color(0xFF4D1635),
        onSecondaryContainer = Color(0xFFFCE7F3),
        tertiary = Color(0xFF7DD3FC),
        onTertiary = Color(0xFF082032),
        outline = Color(0xFF624A96),
        outlineVariant = Color(0xFF3B2C61),
        outlineHighContrast = Color(0xFFA78BFA),
      )

    AppDarkThemePreset.SHADOW_EMERALD ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF030A08),
        surfaceNavy = Color(0xFF081411),
        surface = Color(0xFF0D1F1A),
        surfaceHigh = Color(0xFF142D26),
        surfaceHighest = Color(0xFF1C3D34),
        heroTopTint = Color(0xFF112C24),
        primary = Color(0xFF4ADE80),
        primaryBright = Color(0xFFA7F3D0),
        primaryMuted = Color(0xFF10B981),
        onPrimary = Color(0xFF032112),
        primaryContainer = Color(0xFF0D422A),
        secondary = Color(0xFFFCD34D),
        secondarySoft = Color(0xFFF59E0B),
        onSecondary = Color(0xFF241802),
        secondaryContainer = Color(0xFF453007),
        onSecondaryContainer = Color(0xFFFEF3C7),
        tertiary = Color(0xFF5EEAD4),
        onTertiary = Color(0xFF04221E),
        outline = Color(0xFF2E6B59),
        outlineVariant = Color(0xFF1E473B),
        outlineHighContrast = Color(0xFF6EE7B7),
      )

    AppDarkThemePreset.TACTICAL_CARBON ->
      RiftThemePalette(
        preset = preset,
        background = Color(0xFF06070A),
        surfaceNavy = Color(0xFF0E1015),
        surface = Color(0xFF161920),
        surfaceHigh = Color(0xFF202530),
        surfaceHighest = Color(0xFF2B3140),
        heroTopTint = Color(0xFF1F242E),
        primary = Color(0xFFF8FAFC),
        primaryBright = Color(0xFFFFFFFF),
        primaryMuted = Color(0xFF94A3B8),
        onPrimary = Color(0xFF0A0E17),
        primaryContainer = Color(0xFF2F3747),
        secondary = Color(0xFFFF9E44),
        secondarySoft = Color(0xFFF97316),
        onSecondary = Color(0xFF261002),
        secondaryContainer = Color(0xFF472006),
        onSecondaryContainer = Color(0xFFFFEDD5),
        tertiary = Color(0xFF60A5FA),
        onTertiary = Color(0xFF061A33),
        outline = Color(0xFF4E586B),
        outlineVariant = Color(0xFF313847),
        outlineHighContrast = Color(0xFF94A3B8),
      )
  }
