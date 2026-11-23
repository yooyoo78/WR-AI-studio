package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiProviderId
import com.example.data.ai.ExternalApiKeyStatus
import com.example.data.local.CoachCacheStats
import com.example.data.local.UserPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.TextScalePreset
import com.example.ui.components.FreeApiKeysBottomSheet
import com.example.ui.components.HextechCard
import com.example.ui.components.ModelPickerBottomSheet
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.ProviderKeyTestState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  preferences: UserPreferences,
  keyStatus: ExternalApiKeyStatus = ExternalApiKeyStatus(),
  keyTestStates: Map<AiProviderId, ProviderKeyTestState> = emptyMap(),
  cacheStats: CoachCacheStats = CoachCacheStats(),
  cooldownModelIds: Set<String> = emptySet(),
  onSelectLanguage: (AppLanguage) -> Unit,
  onChangeFontScale: (Float) -> Unit,
  onToggleHighContrast: (Boolean) -> Unit,
  onToggleMiniMode: (Boolean) -> Unit,
  onSelectCoachModel: (String) -> Unit = {},
  onSaveAndTestApiKey: (AiProviderId, String) -> Unit = { _, _ -> },
  onClearApiKey: (AiProviderId) -> Unit = {},
  onClearAiCache: () -> Unit = {},
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = preferences.language
  var showModelPicker by remember { mutableStateOf(false) }
  var showKeysSheet by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("settings_lazy_column"),
        contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 12.dp),
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

        // AI Coach Free Models, External Keys & Persistent Cache
        item {
          val activeSpec = AiModelCatalog.findById(preferences.selectedCoachModelId)
          HextechCard(
            accentColor = ArcaneCyan,
            modifier = Modifier.testTag("settings_ai_providers_card"),
          ) {
            Text(
              text =
                if (lang == AppLanguage.TH) "AI Coach: โมเดลฟรี, Key เสริม & แคชประหยัดโทเคน"
                else "AI Coach: Free Models, Keys & Response Cache",
              style = MaterialTheme.typography.titleMedium,
              color = ArcaneCyan,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) {
                  "• โมเดลที่เลือก: ${activeSpec.displayName}\n" +
                    "• Free API Key ที่ใส่แล้ว: ${keyStatus.configuredExternalCount}/3 (Groq / OpenRouter / AI Studio)\n" +
                    "• คำตอบที่แคชลงเครื่อง: ${cacheStats.entryCount} รายการ • ประหยัดซ้ำแล้ว ${cacheStats.totalHitsSaved} ครั้ง (0 โทเคน)"
                } else {
                  "• Active Model: ${activeSpec.displayName}\n" +
                    "• External Free Keys: ${keyStatus.configuredExternalCount}/3 (Groq / OpenRouter / AI Studio)\n" +
                    "• Persistent Cache: ${cacheStats.entryCount} answers saved • ${cacheStats.totalHitsSaved} repeat hits served free"
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = { showModelPicker = true },
                border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.7f)),
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("settings_select_model_button"),
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArcaneCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (lang == AppLanguage.TH) "เลือกโมเดล AI" else "Select AI Model",
                  color = ArcaneCyan,
                )
              }
              OutlinedButton(
                onClick = { showKeysSheet = true },
                border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.75f)),
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("settings_manage_keys_button"),
              ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = HextechGoldBright, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (lang == AppLanguage.TH) "จัดการ Free Keys" else "Manage Free Keys",
                  color = HextechGoldBright,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              OutlinedButton(
                onClick = onClearAiCache,
                enabled = cacheStats.entryCount > 0,
                border =
                  BorderStroke(
                    1.dp,
                    if (cacheStats.entryCount > 0) ThreatCrimson.copy(alpha = 0.7f)
                    else AdvantageEmerald.copy(alpha = 0.35f),
                  ),
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("settings_clear_cache_button"),
              ) {
                Icon(
                  Icons.Default.DeleteSweep,
                  contentDescription = null,
                  tint = if (cacheStats.entryCount > 0) ThreatCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (lang == AppLanguage.TH) "ล้างแคช AI" else "Clear AI Cache",
                  color = if (cacheStats.entryCount > 0) ThreatCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
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
                  "ตัวอย่างข้อความ: ออก Stasis Enchant ก่อนไฟต์มังกรที่ 2 เพื่อกันอัลติของ Zed และเก็บสกิล E ไว้หนี"
                } else {
                  "Preview: Prioritize Stasis Enchant before 2nd Dragon to nullify Zed's Death Mark combo."
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        // In-Game Mini-Window Mode
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                  text = strings.miniModeTitle,
                  style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = strings.miniModeDesc,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = preferences.inGameMiniMode,
                onCheckedChange = onToggleMiniMode,
                modifier = Modifier.testTag("settings_mini_mode_switch"),
              )
            }
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

  if (showModelPicker) {
    ModelPickerBottomSheet(
      selectedModelId = preferences.selectedCoachModelId,
      keyStatus = keyStatus,
      cooldownModelIds = cooldownModelIds,
      forVision = false,
      language = lang,
      onSelectModel = onSelectCoachModel,
      onOpenApiKeys = { showKeysSheet = true },
      onDismiss = { showModelPicker = false },
    )
  }

  if (showKeysSheet) {
    FreeApiKeysBottomSheet(
      keyStatus = keyStatus,
      keyTestStates = keyTestStates,
      language = lang,
      onSaveAndTestKey = onSaveAndTestApiKey,
      onClearKey = onClearApiKey,
      onDismiss = { showKeysSheet = false },
    )
  }
}
