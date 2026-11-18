package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.TextScalePreset
import com.example.ui.components.HextechCard
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalUiStrings
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  preferences: UserPreferences,
  onSelectLanguage: (AppLanguage) -> Unit,
  onChangeFontScale: (Float) -> Unit,
  onToggleHighContrast: (Boolean) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val strings = LocalUiStrings.current
  val lang = preferences.language

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(strings.settingsTitle, modifier = Modifier.testTag("settings_title_text")) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Language Selector (Thai default + English)
        item {
          HextechCard(accentColor = HextechGold) {
            Text(text = strings.languageSectionTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AppLanguage.entries.forEach { option ->
                FilterChip(
                  selected = preferences.language == option,
                  onClick = { onSelectLanguage(option) },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("settings_lang_${option.code}"),
                  label = { Text(option.displayLabel) },
                )
              }
            }
          }
        }

        // Adjustable Text Readability Controls
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = strings.textSizeSectionTitle,
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "${(preferences.fontScaleMultiplier * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = ArcaneCyan,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              TextScalePreset.PRESETS.forEach { preset ->
                val isSelected = abs(preferences.fontScaleMultiplier - preset.multiplier) < 0.03f
                FilterChip(
                  selected = isSelected,
                  onClick = { onChangeFontScale(preset.multiplier) },
                  modifier = Modifier.testTag("settings_scale_${preset.id}"),
                  label = {
                    Text(if (lang == AppLanguage.TH) preset.labelTh else preset.labelEn)
                  },
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Slider(
              value = preferences.fontScaleMultiplier,
              onValueChange = onChangeFontScale,
              valueRange = 0.88f..1.30f,
              modifier = Modifier.fillMaxWidth().testTag("settings_font_slider"),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) {
                  "ตัวอย่างข้อความ: ออก Stasis Enchant ก่อนไฟต์มังกรที่ 2 เพื่อกันอัลติของ Zed และเก็บ E ไว้หนี"
                } else {
                  "Preview: Prioritize Stasis Enchant before 2nd Dragon to nullify Zed's Death Mark combo."
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        // High-Contrast Card Borders
        item {
          HextechCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = strings.contrastSectionTitle,
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text =
                    if (lang == AppLanguage.TH) {
                      "เพิ่มความชัดของเส้นขอบการ์ดและคอนทราสต์ตัวอักษรเพื่อให้อ่านไวขณะเลือกตัวละคร"
                    } else {
                      "Boosts card rim borders and separation for fast scanning under draft timers."
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = preferences.highContrastCards,
                onCheckedChange = onToggleHighContrast,
                modifier = Modifier.testTag("settings_contrast_switch"),
              )
            }
          }
        }
      }
    }
  }
}
