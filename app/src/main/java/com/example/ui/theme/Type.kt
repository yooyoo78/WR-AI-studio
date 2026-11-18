package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansThaiFamily = FontFamily(Font(R.font.noto_sans_thai, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))

fun createScaledTypography(scale: Float): Typography {
  val s = scale.coerceIn(0.85f, 1.35f)
  return Typography(
    displaySmall =
      TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (28f * s).sp,
        lineHeight = (36f * s).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (22f * s).sp,
        lineHeight = (30f * s).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (19f * s).sp,
        lineHeight = (26f * s).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (18f * s).sp,
        lineHeight = (25f * s).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * s).sp,
        lineHeight = (23f * s).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * s).sp,
        lineHeight = (20f * s).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16f * s).sp,
        lineHeight = (24f * s).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14.5f * s).sp,
        lineHeight = (21.5f * s).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (13f * s).sp,
        lineHeight = (19f * s).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14f * s).sp,
        lineHeight = (20f * s).sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = NotoSansThaiFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12.5f * s).sp,
        lineHeight = (17f * s).sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11.5f * s).sp,
        lineHeight = (16f * s).sp,
      ),
  )
}
