package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.Lane
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.components.HextechCard
import com.example.ui.components.TacticalMeterBar
import com.example.ui.components.TierBadge
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DraftUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DraftScreen(
  state: DraftUiState,
  onToggleAlly: (String) -> Unit,
  onToggleEnemy: (String) -> Unit,
  onSelectLane: (Lane) -> Unit,
  onSelectRole: (ChampionRole?) -> Unit,
  onClearDraft: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val lang = LocalUserPrefs.current.language

  var pickingAllies by remember { mutableStateOf(false) }
  var pickingEnemies by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Allied & Enemy 5v5 Board
      item {
        HextechCard(accentColor = ArcaneCyan) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = strings.draftAlliesTitle, style = MaterialTheme.typography.titleMedium, color = ArcaneCyan)
            if (state.allies.isNotEmpty() || state.enemies.isNotEmpty()) {
              TextButton(onClick = onClearDraft, modifier = Modifier.testTag("draft_clear_button")) {
                Text(strings.clearAll, color = ThreatCrimson)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          DraftFiveSlotRow(
            champions = state.allies,
            accent = ArcaneCyan,
            tagPrefix = "draft_ally",
            onSlotClick = { champ ->
              if (champ != null) onToggleAlly(champ.id) else pickingAllies = true
            },
          )

          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = strings.draftEnemiesTitle,
            style = MaterialTheme.typography.titleMedium,
            color = ThreatCrimson,
          )
          Spacer(modifier = Modifier.height(6.dp))
          DraftFiveSlotRow(
            champions = state.enemies,
            accent = ThreatCrimson,
            tagPrefix = "draft_enemy",
            onSlotClick = { champ ->
              if (champ != null) onToggleEnemy(champ.id) else pickingEnemies = true
            },
          )
        }
      }

      // Intended Lane & Role Filter for Recommendations
      item {
        HextechCard {
          Text(
            text =
              if (lang == AppLanguage.TH) "ตำแหน่งที่คุณกำลังจะเลือกเล่น (แนะนำตัวแก้ทางและเข้ากับทีม)"
              else "Your Intended Lane & Role for Pick Suggestions",
            style = MaterialTheme.typography.titleSmall,
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Lane.entries.forEach { lane ->
              FilterChip(
                selected = state.intendedLane == lane,
                onClick = { onSelectLane(lane) },
                label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = state.preferredRole == null,
              onClick = { onSelectRole(null) },
              label = { Text(if (lang == AppLanguage.TH) "ทุกบทบาท" else "Any Role") },
            )
            ChampionRole.entries.forEach { role ->
              FilterChip(
                selected = state.preferredRole == role,
                onClick = {
                  onSelectRole(if (state.preferredRole == role) null else role)
                },
                label = { Text(if (lang == AppLanguage.TH) role.labelTh else role.labelEn) },
              )
            }
          }
        }
      }

      // Draft Analysis Report
      state.report?.let { report ->
        if (report.warnings.isNotEmpty()) {
          item {
            HextechCard(accentColor = WarningAmber) {
              Text(text = strings.draftWarningsTitle, style = MaterialTheme.typography.titleMedium, color = WarningAmber)
              Spacer(modifier = Modifier.height(8.dp))
              report.warnings.forEach { w ->
                Row(
                  modifier = Modifier.padding(vertical = 4.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (w.severityCritical) ThreatCrimson else WarningAmber,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(text = w.title(lang), style = MaterialTheme.typography.titleSmall)
                    Text(
                      text = w.detail(lang),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        item {
          HextechCard(accentColor = HextechGold) {
            Text(text = strings.draftBalanceTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text =
                "AD ${report.physicalSharePct}%  •  AP ${report.magicSharePct}%  •  Hybrid/True ${report.trueHybridSharePct}%",
              style = MaterialTheme.typography.labelLarge,
              color = HextechGold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "แนวหน้ารับดาเมจ (Frontline)" else "Frontline",
                valuePercent = report.frontlineScore,
                barColor = HextechGold,
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "สกิลหยุดศัตรู (Crowd Control)" else "Crowd Control",
                valuePercent = report.ccScore,
                barColor = SkillViolet,
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "พลังเปิดไฟต์ (Engage)" else "Engage Potential",
                valuePercent = report.engageScore,
                barColor = ThreatCrimson,
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "พลังกันตัวล้วง (Disengage)" else "Disengage / Peel",
                valuePercent = report.disengageScore,
                barColor = ArcaneCyan,
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "ศักยภาพทีมไฟต์ 5v5" else "Teamfight Capability",
                valuePercent = report.teamfightScore,
                barColor = AdvantageEmerald,
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "พลังดันแยกเลนข้าง (Split-Push)" else "Split-Push",
                valuePercent = report.splitPushScore,
                barColor = WarningAmber,
              )
            }
          }
        }

        item {
          Text(text = strings.draftSuggestionsTitle, style = MaterialTheme.typography.titleMedium)
        }

        items(report.recommendedPicks, key = { it.champion.id }) { suggestion ->
          HextechCard(
            accentColor = AdvantageEmerald,
            modifier = Modifier.testTag("draft_suggestion_${suggestion.champion.id}"),
            onClick = { onToggleAlly(suggestion.champion.id) },
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ChampionCrestAvatar(champion = suggestion.champion, size = 44.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = suggestion.champion.displayName(lang),
                    style = MaterialTheme.typography.titleMedium,
                  )
                  TierBadge(suggestion.champion.tierRank)
                }
                Text(
                  text = suggestion.reason(lang),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "${suggestion.totalScore}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AdvantageEmerald,
              )
            }
          }
        }
      }
    }
  }

  if (pickingAllies) {
    ChampionPickerBottomSheet(
      title = strings.draftAlliesTitle,
      champions = state.allChampions,
      selectedIds = state.allies.map { it.id }.toSet(),
      disabledIds = state.enemies.map { it.id }.toSet(),
      language = lang,
      onSelectChampion = { onToggleAlly(it.id) },
      onDismiss = { pickingAllies = false },
    )
  }

  if (pickingEnemies) {
    ChampionPickerBottomSheet(
      title = strings.draftEnemiesTitle,
      champions = state.allChampions,
      selectedIds = state.enemies.map { it.id }.toSet(),
      disabledIds = state.allies.map { it.id }.toSet(),
      language = lang,
      onSelectChampion = { onToggleEnemy(it.id) },
      onDismiss = { pickingEnemies = false },
    )
  }
}

@Composable
private fun DraftFiveSlotRow(
  champions: List<ChampionData>,
  accent: Color,
  tagPrefix: String,
  onSlotClick: (ChampionData?) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    for (i in 0 until 5) {
      val champ = champions.getOrNull(i)
      Surface(
        modifier =
          Modifier.weight(1f)
            .height(74.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onSlotClick(champ) }
            .testTag("${tagPrefix}_slot_$i"),
        color =
          if (champ != null) accent.copy(alpha = 0.14f)
          else MaterialTheme.colorScheme.surfaceContainerHigh,
        border =
          BorderStroke(
            1.dp,
            if (champ != null) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline,
          ),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
          if (champ != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              ChampionCrestAvatar(champion = champ, size = 32.dp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = champ.nameEn,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Icon(
              Icons.Default.Close,
              contentDescription = "Remove",
              tint = accent,
              modifier = Modifier.align(Alignment.TopEnd).size(13.dp),
            )
          } else {
            Icon(
              Icons.Default.Add,
              contentDescription = "Add",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
