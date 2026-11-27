package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.model.AppDarkThemePreset
import com.example.data.model.AppFontPreset
import com.example.data.model.AppLanguage
import com.example.data.model.IconSizePreset
import com.example.data.model.TextScalePreset
import com.example.ui.theme.LocalRiftPalette
import com.example.ui.theme.fontFamilyFor
import com.example.ui.theme.riftPaletteFor
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoachAppearanceQuickSheet(
  preferences: UserPreferences,
  onSelectThemePreset: (AppDarkThemePreset) -> Unit,
  onSelectFontPreset: (AppFontPreset) -> Unit,
  onChangeFontScale: (Float) -> Unit,
  onSelectIconSizePreset: (IconSizePreset) -> Unit,
  onToggleHighContrast: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val lang = preferences.language
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.surfaceNavy,
    contentColor = palette.textPrimary,
    modifier = Modifier.testTag("coach_appearance_sheet"),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().testTag("coach_appearance_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = palette.primaryBright,
              modifier = Modifier.size(20.dp),
            )
            Column {
              Text(
                text =
                  if (lang == AppLanguage.TH) "ปรับลุค AI Coach ด่วน"
                  else "AI Coach Quick Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryBright,
              )
              Text(
                text =
                  if (lang == AppLanguage.TH) "ซิงค์ตรงกับการตั้งค่า Settings ทันที"
                  else "Syncs live with global Settings",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("close_coach_appearance_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary)
          }
        }
      }

      // Live Preview Card inside sheet
      item {
        val sampleBody = MaterialTheme.typography.bodyMedium
        val previewIconSize =
          if (preferences.iconSizePreset == IconSizePreset.COMPACT) 16.dp else 22.dp
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = palette.surfaceHigh,
          border =
            BorderStroke(
              width = if (preferences.highContrastCards) 1.75.dp else 1.15.dp,
              color =
                if (preferences.highContrastCards) palette.outlineHighContrast
                else palette.secondary.copy(alpha = 0.7f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("coach_appearance_preview_card"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (lang == AppLanguage.TH) "ตัวอย่างการ์ด AI Coach (${(preferences.fontScaleMultiplier * 100).toInt()}%)"
                  else "Live AI Card Preview (${(preferences.fontScaleMultiplier * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.secondary,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                AbilityIconBadge(
                  slot = "Q",
                  iconUrl = "",
                  contentDescription = if (lang == AppLanguage.TH) "สกิล Q" else "Skill Q",
                  size = 32.dp,
                )
                Icon(
                  imageVector = Icons.Default.PhotoSizeSelectSmall,
                  contentDescription = null,
                  tint = palette.primaryBright,
                  modifier = Modifier.size(previewIconSize),
                )
              }
            }
            Text(
              text =
                if (lang == AppLanguage.TH) {
                  "กดคอมโบ 1-2 เพื่อคุมเลนต้นเกม และรีบออก Stasis ก่อนไฟต์มังกร"
                } else {
                  "Use Q-E poke early and rush Stasis before first Dragon skirmish."
                },
              style = sampleBody.copy(lineHeight = sampleBody.fontSize * 1.46f),
              color = palette.textPrimary,
            )
          }
        }
      }

      // 4 Dark Themes
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = if (lang == AppLanguage.TH) "โทนสีมืด (4 รูปแบบ)" else "Dark Theme Palette",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.primaryBright,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AppDarkThemePreset.entries.forEach { preset ->
              val p = riftPaletteFor(preset)
              val selected = preferences.darkThemePreset == preset
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = p.surface,
                border =
                  BorderStroke(
                    width = if (selected) 1.75.dp else 1.dp,
                    color = if (selected) p.primaryBright else p.outline,
                  ),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectThemePreset(preset) }
                    .testTag("coach_quick_theme_${preset.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(13.dp)
                        .clip(CircleShape)
                        .background(p.primary)
                        .border(1.dp, p.textPrimary.copy(alpha = 0.55f), CircleShape)
                  )
                  Box(
                    modifier = Modifier.size(11.dp).clip(CircleShape).background(p.secondary)
                  )
                  Text(
                    text = preset.displayLabel(lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) p.primaryBright else p.textPrimary,
                  )
                  if (selected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      tint = p.primaryBright,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 4 Font Families
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.TextFields,
              contentDescription = null,
              tint = palette.secondary,
              modifier = Modifier.size(17.dp),
            )
            Text(
              text = if (lang == AppLanguage.TH) "รูปแบบฟอนต์ (อ่านชัด)" else "Typography Font",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.secondary,
            )
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            AppFontPreset.entries.forEach { preset ->
              FilterChip(
                selected = preferences.fontPreset == preset,
                onClick = { onSelectFontPreset(preset) },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("coach_quick_font_${preset.id}"),
                label = {
                  Text(
                    text = preset.displayLabel(lang),
                    fontFamily = fontFamilyFor(preset),
                  )
                },
              )
            }
          }
        }
      }

      // Font Scale Slider + Presets
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (lang == AppLanguage.TH) "ขนาดตัวอักษร AI Coach" else "Text Size Scale",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            Text(
              text = "${(preferences.fontScaleMultiplier * 100).toInt()}%",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = palette.primaryBright,
            )
          }
          Slider(
            value = preferences.fontScaleMultiplier,
            onValueChange = onChangeFontScale,
            valueRange = 0.85f..1.35f,
            steps = 9,
            colors =
              SliderDefaults.colors(
                thumbColor = palette.primaryBright,
                activeTrackColor = palette.primary,
                inactiveTrackColor = palette.surfaceHighest,
              ),
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            TextScalePreset.PRESETS.forEach { preset ->
              FilterChip(
                selected = abs(preferences.fontScaleMultiplier - preset.multiplier) < 0.03f,
                onClick = { onChangeFontScale(preset.multiplier) },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("coach_quick_scale_${preset.id}"),
                label = {
                  Text(if (lang == AppLanguage.TH) preset.labelTh else preset.labelEn)
                },
              )
            }
          }
        }
      }

      // Icon Size & High Contrast Borders
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = if (lang == AppLanguage.TH) "ขนาดไอคอน & เส้นขอบ" else "Icon Size & Contrast",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconSizePreset.entries.forEach { preset ->
              FilterChip(
                selected = preferences.iconSizePreset == preset,
                onClick = { onSelectIconSizePreset(preset) },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("coach_quick_icon_${preset.id}"),
                label = { Text(preset.displayLabel(lang)) },
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (lang == AppLanguage.TH) "เส้นขอบการ์ดคอนทราสต์สูง"
                else "High-Contrast Card Borders",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.textPrimary,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = preferences.highContrastCards,
              onCheckedChange = onToggleHighContrast,
              modifier = Modifier.testTag("coach_quick_high_contrast"),
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}
