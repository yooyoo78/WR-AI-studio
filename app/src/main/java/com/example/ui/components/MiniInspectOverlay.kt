package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AbilityInfo
import com.example.data.model.AppLanguage
import com.example.data.model.ItemData
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.TextPrimaryLight

sealed interface MiniInspectTarget {
  data class Item(val item: ItemData) : MiniInspectTarget

  data class Rune(val rune: RuneData) : MiniInspectTarget

  data class Spell(val spell: SpellData) : MiniInspectTarget

  data class Skill(val ability: AbilityInfo, val iconUrl: String) : MiniInspectTarget
}

/**
 * Two-stage LoLegacy-style inspection overlay:
 * Stage 1 renders a compact floating Mini Text Box with core stats & summary.
 * Stage 2 expands into the full detail Bottom Sheet when the user taps "Expand".
 */
@Composable
fun MiniInspectHost(
  target: MiniInspectTarget?,
  language: AppLanguage,
  onDismiss: () -> Unit,
) {
  var expandedTarget by remember { mutableStateOf<MiniInspectTarget?>(null) }

  LaunchedEffect(target) {
    if (target == null) {
      expandedTarget = null
    } else if (expandedTarget != null && expandedTarget != target) {
      expandedTarget = null
    }
  }

  val activeExpanded = expandedTarget
  if (activeExpanded != null) {
    val closeAll = {
      expandedTarget = null
      onDismiss()
    }
    when (activeExpanded) {
      is MiniInspectTarget.Item ->
        ItemDetailBottomSheet(item = activeExpanded.item, language = language, onDismiss = closeAll)
      is MiniInspectTarget.Rune ->
        RuneDetailBottomSheet(rune = activeExpanded.rune, language = language, onDismiss = closeAll)
      is MiniInspectTarget.Spell ->
        SpellDetailBottomSheet(spell = activeExpanded.spell, language = language, onDismiss = closeAll)
      is MiniInspectTarget.Skill ->
        SkillDetailBottomSheet(
          ability = activeExpanded.ability,
          iconUrl = activeExpanded.iconUrl,
          language = language,
          onDismiss = closeAll,
        )
    }
  } else if (target != null) {
    MiniInfoBoxDialog(
      target = target,
      language = language,
      onExpand = { expandedTarget = target },
      onDismiss = onDismiss,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniInfoBoxDialog(
  target: MiniInspectTarget,
  language: AppLanguage,
  onExpand: () -> Unit,
  onDismiss: () -> Unit,
) {
  val accentColor =
    when (target) {
      is MiniInspectTarget.Item -> HextechGold
      is MiniInspectTarget.Rune -> SkillViolet
      is MiniInspectTarget.Spell -> ArcaneCyan
      is MiniInspectTarget.Skill ->
        when (target.ability.slot.uppercase()) {
          "P" -> ArcaneCyan
          "4", "R" -> HextechGold
          else -> SkillViolet
        }
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = HextechSurfaceHigh,
      tonalElevation = 6.dp,
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
      modifier = Modifier.fillMaxWidth().widthIn(max = 330.dp).testTag("mini_info_box"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (target) {
            is MiniInspectTarget.Item ->
              ItemIconSlot(
                item = target.item,
                language = language,
                onClick = {},
                size = 44.dp,
                showCost = false,
                showName = false,
              )
            is MiniInspectTarget.Rune ->
              RuneIconSlot(
                rune = target.rune,
                language = language,
                onClick = {},
                size = 44.dp,
                showLabel = false,
              )
            is MiniInspectTarget.Spell ->
              SpellIconSlot(
                spell = target.spell,
                language = language,
                onClick = {},
                size = 44.dp,
                showLabel = false,
              )
            is MiniInspectTarget.Skill ->
              AbilityIconBadge(
                slot = target.ability.slot,
                iconUrl = target.iconUrl,
                contentDescription =
                  if (language == AppLanguage.TH) target.ability.nameTh else target.ability.nameEn,
                size = 44.dp,
              )
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val title =
              when (target) {
                is MiniInspectTarget.Item -> target.item.displayName(language)
                is MiniInspectTarget.Rune -> target.rune.displayName(language)
                is MiniInspectTarget.Spell -> target.spell.displayName(language)
                is MiniInspectTarget.Skill ->
                  if (language == AppLanguage.TH) target.ability.nameTh else target.ability.nameEn
              }
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = accentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              when (target) {
                is MiniInspectTarget.Item -> {
                  MiniPill("${target.item.cost} Gold", HextechGoldBright)
                  MiniPill(target.item.category.name, ArcaneCyan)
                }
                is MiniInspectTarget.Rune -> {
                  val isKs = target.rune.slotType.equals("KEYSTONE", ignoreCase = true)
                  MiniPill(
                    if (isKs) "Keystone" else (target.rune.tree.ifBlank { "Rune" }),
                    SkillViolet,
                  )
                }
                is MiniInspectTarget.Spell -> {
                  if (target.spell.cooldownNote.isNotBlank()) {
                    MiniPill("CD ${target.spell.cooldownNote}", ArcaneCyan)
                  }
                }
                is MiniInspectTarget.Skill -> {
                  val slotLabel =
                    if (target.ability.slot.equals("P", true)) "Passive"
                    else "Skill ${target.ability.slot}"
                  MiniPill(slotLabel, accentColor)
                  if (target.ability.cooldownNote.isNotBlank()) {
                    MiniPill("CD ${target.ability.cooldownNote}", ArcaneCyan)
                  }
                }
              }
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_box_close_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = if (language == AppLanguage.TH) "ปิด" else "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        // Concise mini stats + summary
        when (target) {
          is MiniInspectTarget.Item -> {
            if (target.item.statsSummary(language).isNotBlank()) {
              Text(
                text = target.item.statsSummary(language),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ArcaneCyan,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = target.item.passiveDesc(language),
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimaryLight,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
          is MiniInspectTarget.Rune -> {
            Text(
              text = target.rune.desc(language),
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimaryLight,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
            )
          }
          is MiniInspectTarget.Spell -> {
            Text(
              text = target.spell.desc(language),
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimaryLight,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
            )
          }
          is MiniInspectTarget.Skill -> {
            Text(
              text =
                if (language == AppLanguage.TH) target.ability.summaryTh
                else target.ability.summaryEn,
              style = MaterialTheme.typography.bodySmall,
              color = TextPrimaryLight,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Expand action row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onExpand,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("mini_box_expand_button"),
          ) {
            Icon(
              imageVector = Icons.Default.OpenInFull,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (language == AppLanguage.TH) "ดูรายละเอียดเต็ม" else "Full Details",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MiniPill(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(5.dp),
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(0.75.dp, color.copy(alpha = 0.4f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SkillDetailBottomSheet(
  ability: AbilityInfo,
  iconUrl: String,
  language: AppLanguage,
  onDismiss: () -> Unit,
) {
  val slotColor =
    when (ability.slot.uppercase()) {
      "P" -> ArcaneCyan
      "4", "R" -> HextechGold
      else -> SkillViolet
    }
  val abilityName = if (language == AppLanguage.TH) ability.nameTh else ability.nameEn
  val abilitySummary = if (language == AppLanguage.TH) ability.summaryTh else ability.summaryEn

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("skill_detail_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AbilityIconBadge(
          slot = ability.slot,
          iconUrl = iconUrl,
          contentDescription = abilityName,
          size = 58.dp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = abilityName,
            style = MaterialTheme.typography.titleLarge,
            color = slotColor,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            MiniPill(
              if (ability.slot.equals("P", true)) "Passive" else "Skill ${ability.slot}",
              slotColor,
            )
            if (ability.cooldownNote.isNotBlank()) {
              MiniPill("CD ${ability.cooldownNote}", ArcaneCyan)
            }
            if (ability.costNote.isNotBlank() && ability.costNote != "0") {
              MiniPill(ability.costNote, HextechGoldBright)
            }
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_skill_sheet_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.75.dp, slotColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = if (language == AppLanguage.TH) "รายละเอียดสกิล (Wild Rift)" else "Ability Details (Wild Rift)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = abilitySummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
