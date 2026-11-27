package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppFontPreset

val NotoSansThaiFamily = FontFamily(Font(R.font.noto_sans_thai, FontWeight.Normal))
val KanitFamily = FontFamily(Font(R.font.kanit, FontWeight.Normal))
val SarabunFamily = FontFamily(Font(R.font.sarabun, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))

fun fontFamilyFor(preset: AppFontPreset): FontFamily =
  when (preset) {
    AppFontPreset.NOTO_SANS_THAI -> NotoSansThaiFamily
    AppFontPreset.KANIT -> KanitFamily
    AppFontPreset.SARABUN -> SarabunFamily
    AppFontPreset.SYSTEM -> FontFamily.Default
  }

fun createScaledTypography(
  scale: Float,
  fontPreset: AppFontPreset = AppFontPreset.NOTO_SANS_THAI,
): Typography {
  val s = scale.coerceIn(0.85f, 1.35f)
  val primaryFamily = fontFamilyFor(fontPreset)
  val numericBadgeFamily = primaryFamily

  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = numericBadgeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (28f * s).sp,
        lineHeight = (36f * s).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (22f * s).sp,
        lineHeight = (30f * s).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (19f * s).sp,
        lineHeight = (26f * s).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (18f * s).sp,
        lineHeight = (25f * s).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * s).sp,
        lineHeight = (23f * s).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * s).sp,
        lineHeight = (20f * s).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16f * s).sp,
        lineHeight = (24f * s).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14.5f * s).sp,
        lineHeight = (21.5f * s).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (13f * s).sp,
        lineHeight = (19f * s).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * s).sp,
        lineHeight = (20f * s).sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = primaryFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12.5f * s).sp,
        lineHeight = (17f * s).sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = numericBadgeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11.5f * s).sp,
        lineHeight = (16f * s).sp,
      ),
  )
}
