package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiModelSpec
import com.example.data.ai.AiProviderId
import com.example.data.ai.ExternalApiKeyStatus
import com.example.data.local.CoachCacheStats
import com.example.data.model.AppLanguage
import com.example.data.model.IconSizePreset
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.LocalIconSizePreset
import com.example.ui.theme.LocalRiftPalette
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ProviderKeyTestState

@Composable
fun AiEngineControlStrip(
  selectedModelId: String,
  keyStatus: ExternalApiKeyStatus,
  cacheStats: CoachCacheStats,
  cooldownModelIds: Set<String>,
  forVision: Boolean,
  language: AppLanguage,
  onOpenModelPicker: () -> Unit,
  onOpenApiKeys: () -> Unit,
  onOpenCacheInfo: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val prefs = LocalUserPrefs.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val arrowIconSize = if (compactIcons) 15.dp else 20.dp
  val pillIconSize = if (compactIcons) 13.dp else 17.dp
  val activeSpec = AiModelCatalog.findById(selectedModelId)
  val isInCooldown = activeSpec.id in cooldownModelIds
  val isMissingKey = activeSpec.provider.requiresApiKey && !keyStatus.hasKeyFor(activeSpec.provider)
  val dotColor =
    when {
      isMissingKey -> ThreatCrimson
      isInCooldown -> WarningAmber
      activeSpec.provider.requiresApiKey -> palette.primaryBright
      else -> AdvantageEmerald
    }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = palette.surface.copy(alpha = 0.9f),
    border =
      BorderStroke(
        width = if (prefs.highContrastCards) 1.35.dp else 1.dp,
        color =
          if (prefs.highContrastCards) palette.outlineHighContrast.copy(alpha = 0.85f)
          else palette.outline.copy(alpha = 0.75f),
      ),
    modifier = Modifier.fillMaxWidth().testTag("coach_model_bar"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Active Model Switcher Chip
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.secondary.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.5f)),
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenModelPicker)
            .testTag("coach_model_selector_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
          Text(
            text =
              if (forVision) {
                "Vision: ${activeSpec.shortBadge}"
              } else {
                activeSpec.displayName
              },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (isInCooldown) {
            Text(
              text = if (language == AppLanguage.TH) "พักลิมิต" else "Cooldown",
              style = MaterialTheme.typography.labelSmall,
              color = WarningAmber,
              maxLines = 1,
            )
          }
          Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription =
              if (language == AppLanguage.TH) "เปลี่ยนโมเดล AI" else "Switch AI Model",
            tint = palette.secondary,
            modifier = Modifier.size(arrowIconSize),
          )
        }
      }

      // Cache Savings Pill
      if (!forVision) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color =
            if (cacheStats.entryCount > 0) AdvantageEmerald.copy(alpha = 0.14f)
            else palette.surfaceHigh,
          border =
            BorderStroke(
              1.dp,
              if (cacheStats.entryCount > 0) AdvantageEmerald.copy(alpha = 0.5f)
              else palette.outline.copy(alpha = 0.6f),
            ),
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onOpenCacheInfo)
              .testTag("coach_cache_stats_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = if (language == AppLanguage.TH) "แคชประหยัดโทเคน" else "Cached responses",
              tint = if (cacheStats.entryCount > 0) AdvantageEmerald else palette.textSecondary,
              modifier = Modifier.size(pillIconSize),
            )
            Text(
              text =
                if (cacheStats.totalHitsSaved > 0) {
                  "${cacheStats.entryCount} (${cacheStats.totalHitsSaved}x)"
                } else {
                  "${cacheStats.entryCount}"
                },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (cacheStats.entryCount > 0) AdvantageEmerald else palette.textSecondary,
              maxLines = 1,
            )
          }
        }
      }

      // External Free Keys Button
      val extCount = keyStatus.configuredExternalCount
      Surface(
        shape = RoundedCornerShape(8.dp),
        color =
          if (extCount > 0) palette.primary.copy(alpha = 0.18f)
          else palette.surfaceHigh,
        border =
          BorderStroke(
            1.dp,
            if (extCount > 0) palette.primary.copy(alpha = 0.7f) else palette.secondary.copy(alpha = 0.5f),
          ),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenApiKeys)
            .testTag("coach_api_keys_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Key,
            contentDescription = if (language == AppLanguage.TH) "ตั้งค่า Free API Key" else "Manage Free API Keys",
            tint = if (extCount > 0) palette.primaryBright else palette.secondary,
            modifier = Modifier.size(pillIconSize),
          )
          Text(
            text =
              if (extCount > 0) "Key ($extCount)"
              else if (language == AppLanguage.TH) "+ Key ฟรี"
              else "+ Key",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (extCount > 0) palette.primaryBright else palette.secondary,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelPickerBottomSheet(
  selectedModelId: String,
  keyStatus: ExternalApiKeyStatus,
  cooldownModelIds: Set<String>,
  forVision: Boolean,
  language: AppLanguage,
  onSelectModel: (String) -> Unit,
  onOpenApiKeys: () -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val sheetIconSize = if (compactIcons) 15.dp else 19.dp
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.surfaceNavy,
    contentColor = palette.textPrimary,
    modifier = Modifier.testTag("model_picker_sheet"),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().testTag("model_picker_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (forVision) {
                  if (language == AppLanguage.TH) "เลือกโมเดลสำหรับ Live Coach (สแกนภาพ)"
                  else "Select Live Coach Vision Model"
                } else {
                  if (language == AppLanguage.TH) "เลือกโมเดล AI Coach (สลับได้ทันที)"
                  else "Select AI Coach Model"
                },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.primaryBright,
            )
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "เลือก Auto เพื่อสลับโมเดลฟรีอัตโนมัติ หรือเลือกใช้โมเดลเฉพาะเมื่อติดช่วงพีค"
                } else {
                  "Use Auto for smart fallback, or lock a specific free model anytime."
                },
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_model_picker_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary)
          }
        }
      }

      item {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenApiKeys()
          },
          border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.75f)),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("model_picker_manage_keys_button"),
        ) {
          Icon(Icons.Default.Key, contentDescription = null, tint = palette.primaryBright, modifier = Modifier.size(sheetIconSize))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (language == AppLanguage.TH) {
                "จัดการ Free API Keys (Groq / OpenRouter / AI Studio) • ใส่แล้ว ${keyStatus.configuredExternalCount}/3"
              } else {
                "Manage Free API Keys (Groq / OpenRouter / AI Studio) • ${keyStatus.configuredExternalCount}/3 active"
              },
            color = palette.primaryBright,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      val groupedProviders =
        listOf(
          AiProviderId.AUTO_POOL,
          AiProviderId.FIREBASE_AI,
          AiProviderId.GROQ,
          AiProviderId.OPENROUTER,
          AiProviderId.GOOGLE_AI_STUDIO,
        )

      groupedProviders.forEach { provider ->
        val models =
          AiModelCatalog.allOptions.filter {
            it.provider == provider && (!forVision || it.supportsVision)
          }
        if (models.isNotEmpty()) {
          item(key = "header_${provider.name}") {
            Text(
              text = if (language == AppLanguage.TH) provider.labelTh else provider.labelEn,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (provider.requiresApiKey) palette.primaryBright else palette.secondary,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
          models.forEach { spec ->
            item(key = "model_${spec.id}") {
              val isSelected = selectedModelId == spec.id
              val hasKey = !spec.provider.requiresApiKey || keyStatus.hasKeyFor(spec.provider)
              val isCooldown = spec.id in cooldownModelIds
              ModelOptionRow(
                spec = spec,
                isSelected = isSelected,
                hasKey = hasKey,
                isCooldown = isCooldown,
                language = language,
                onClick = {
                  if (hasKey) {
                    onSelectModel(spec.id)
                    onDismiss()
                  } else {
                    onDismiss()
                    onOpenApiKeys()
                  }
                },
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelOptionRow(
  spec: AiModelSpec,
  isSelected: Boolean,
  hasKey: Boolean,
  isCooldown: Boolean,
  language: AppLanguage,
  onClick: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val rowIconSize = if (compactIcons) 15.dp else 19.dp
  val borderColor =
    when {
      isSelected -> palette.secondary
      !hasKey -> palette.outline.copy(alpha = 0.5f)
      isCooldown -> WarningAmber.copy(alpha = 0.65f)
      else -> palette.outline
    }
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (isSelected) palette.secondary.copy(alpha = 0.14f) else palette.surfaceHigh,
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("model_option_${spec.id}"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector =
              when {
                isSelected -> Icons.Default.CheckCircle
                !hasKey -> Icons.Default.Lock
                else -> Icons.Default.AutoAwesome
              },
            contentDescription = null,
            tint =
              when {
                isSelected -> palette.secondary
                !hasKey -> WarningAmber
                else -> palette.primaryBright
              },
            modifier = Modifier.size(rowIconSize),
          )
          Text(
            text = spec.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (hasKey) palette.textPrimary else palette.textSecondary,
          )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          if (spec.supportsVision && spec.id != AiModelCatalog.AUTO_MODEL_ID) {
            StatusTagPill(
              text = "Vision",
              color = AdvantageEmerald,
            )
          }
          if (!hasKey) {
            StatusTagPill(
              text = if (language == AppLanguage.TH) "ใส่ Key ฟรี" else "Add Key",
              color = palette.primaryBright,
            )
          } else if (isCooldown) {
            StatusTagPill(
              text = if (language == AppLanguage.TH) "พักลิมิต" else "Cooldown",
              color = WarningAmber,
            )
          }
        }
      }
      Text(
        text = if (language == AppLanguage.TH) spec.descTh else spec.descEn,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary,
      )
    }
  }
}

@Composable
private fun StatusTagPill(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeApiKeysBottomSheet(
  keyStatus: ExternalApiKeyStatus,
  keyTestStates: Map<AiProviderId, ProviderKeyTestState>,
  language: AppLanguage,
  onSaveAndTestKey: (AiProviderId, String) -> Unit,
  onClearKey: (AiProviderId) -> Unit,
  onDismiss: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.surfaceNavy,
    contentColor = palette.textPrimary,
    modifier = Modifier.testTag("free_api_keys_sheet"),
  ) {
    ProvideSafeTextToolbar {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().testTag("free_keys_lazy_column"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (language == AppLanguage.TH) "เพิ่ม Free API Key สำรองช่วงพีค"
                else "Add Optional Free API Keys",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.primaryBright,
            )
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "เข้ารหัสเก็บเฉพาะบนเครื่องของคุณด้วย Android Keystore • ใช้ร่วมกับ Firebase ฟรีได้ทันที"
                } else {
                  "Encrypted on-device via Android Keystore. Expands your free quota pool alongside Firebase AI."
                },
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_free_keys_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.textSecondary)
          }
        }
      }

      AiModelCatalog.externalProviders.forEach { provider ->
        item(key = "ext_key_card_${provider.name}") {
          ProviderKeyCard(
            provider = provider,
            hasSavedKey = keyStatus.hasKeyFor(provider),
            maskedPreview = keyStatus.previewFor(provider),
            testState = keyTestStates[provider] ?: ProviderKeyTestState(),
            language = language,
            onSaveAndTest = { raw -> onSaveAndTestKey(provider, raw) },
            onClear = { onClearKey(provider) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(14.dp)) }
    }
    }
  }
}

@Composable
private fun ProviderKeyCard(
  provider: AiProviderId,
  hasSavedKey: Boolean,
  maskedPreview: String,
  testState: ProviderKeyTestState,
  language: AppLanguage,
  onSaveAndTest: (String) -> Unit,
  onClear: () -> Unit,
) {
  val palette = LocalRiftPalette.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val btnIconSize = if (compactIcons) 15.dp else 19.dp
  var keyInput by remember(provider) { mutableStateOf("") }
  var showKey by remember(provider) { mutableStateOf(false) }

  val guideHint =
    when (provider) {
      AiProviderId.GROQ ->
        if (language == AppLanguage.TH) {
          "สมัครฟรีที่ console.groq.com -> API Keys (ขึ้นต้นด้วย gsk_...) โควตาเร็วมาก"
        } else {
          "Free at console.groq.com -> API Keys (starts with gsk_...). Ultra-fast LPU inference."
        }
      AiProviderId.OPENROUTER ->
        if (language == AppLanguage.TH) {
          "สมัครฟรีที่ openrouter.ai/keys (ขึ้นต้นด้วย sk-or-...) ใช้กับโมเดล :free ได้ฟรี"
        } else {
          "Free at openrouter.ai/keys (starts with sk-or-...). Works with $0 :free models."
        }
      AiProviderId.GOOGLE_AI_STUDIO ->
        if (language == AppLanguage.TH) {
          "สร้าง Key ฟรีที่ aistudio.google.com/apikey (ขึ้นต้นด้วย AIza...) รองรับทั้งแชทและ Live Vision"
        } else {
          "Free at aistudio.google.com/apikey (starts with AIza...). Supports Chat + Live Vision."
        }
      else -> ""
    }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.surfaceHigh,
    border =
      BorderStroke(
        1.dp,
        if (hasSavedKey) AdvantageEmerald.copy(alpha = 0.6f)
        else palette.outline,
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = if (language == AppLanguage.TH) provider.labelTh else provider.labelEn,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = palette.primaryBright,
          modifier = Modifier.weight(1f),
        )
        StatusTagPill(
          text =
            if (hasSavedKey) {
              if (language == AppLanguage.TH) "บันทึกแล้ว ($maskedPreview)" else "Saved ($maskedPreview)"
            } else {
              if (language == AppLanguage.TH) "ยังไม่ใส่ Key" else "Not set"
            },
          color = if (hasSavedKey) AdvantageEmerald else palette.textSecondary,
        )
      }

      Text(
        text = guideHint,
        style = MaterialTheme.typography.bodySmall,
        color = palette.textSecondary,
      )

      OutlinedTextField(
        value = keyInput,
        onValueChange = { keyInput = it },
        singleLine = true,
        visualTransformation =
          if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        placeholder = {
          Text(
            text =
              if (hasSavedKey) {
                if (language == AppLanguage.TH) "วาง Key ใหม่เพื่อเปลี่ยน..." else "Paste new key to replace..."
              } else {
                if (language == AppLanguage.TH) "วาง API Key ที่นี่..." else "Paste API Key here..."
              },
            maxLines = 1,
          )
        },
        trailingIcon = {
          IconButton(onClick = { showKey = !showKey }) {
            Icon(
              imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (showKey) "Hide key" else "Show key",
            )
          }
        },
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.secondary,
            unfocusedBorderColor = palette.outline,
          ),
        modifier = Modifier.fillMaxWidth().testTag("api_key_input_${provider.name}"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = { onSaveAndTest(keyInput) },
          enabled = keyInput.isNotBlank() && !testState.isTesting,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.secondary,
              contentColor = palette.onSecondary,
            ),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("api_key_save_button_${provider.name}"),
        ) {
          if (testState.isTesting) {
            CircularProgressIndicator(
              color = palette.onSecondary,
              modifier = Modifier.size(btnIconSize),
              strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (language == AppLanguage.TH) "กำลังทดสอบ..." else "Testing...")
          } else {
            Text(
              text = if (language == AppLanguage.TH) "ทดสอบ & บันทึก Key" else "Test & Save Key",
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (hasSavedKey) {
          OutlinedButton(
            onClick = {
              keyInput = ""
              onClear()
            },
            border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.7f)),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("api_key_clear_button_${provider.name}"),
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = if (language == AppLanguage.TH) "ลบ Key" else "Clear key",
              tint = ThreatCrimson,
              modifier = Modifier.size(btnIconSize),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (language == AppLanguage.TH) "ลบ" else "Clear", color = ThreatCrimson)
          }
        }
      }

      testState.lastResult?.let { res ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color =
            if (res.isValid) AdvantageEmerald.copy(alpha = 0.14f)
            else ThreatCrimson.copy(alpha = 0.14f),
          border =
            BorderStroke(
              1.dp,
              if (res.isValid) AdvantageEmerald.copy(alpha = 0.6f)
              else ThreatCrimson.copy(alpha = 0.6f),
            ),
          modifier = Modifier.fillMaxWidth().testTag("api_key_test_result_${provider.name}"),
        ) {
          Text(
            text = res.message,
            style = MaterialTheme.typography.labelSmall,
            color = if (res.isValid) AdvantageEmerald else ThreatCrimson,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}
