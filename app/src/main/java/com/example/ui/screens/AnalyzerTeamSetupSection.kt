package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SavedMatchupEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.TierBadge
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.AnalyzerUiState

@Composable
fun AnalyzerMyChampionSetupCard(
  myChamp: ChampionData?,
  state: AnalyzerUiState,
  lang: AppLanguage,
  onSaveMatchup: () -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onOpenMyChampPicker: () -> Unit,
  onSelectLane: (Lane) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = HextechGold) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = strings.selectYourChampion, style = MaterialTheme.typography.titleMedium)
      if (state.selectedEnemies.isNotEmpty()) {
        TextButton(
          onClick = onSaveMatchup,
          modifier = Modifier.testTag("save_matchup_button"),
        ) {
          Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.saveMatchupButton)
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))

    if (myChamp != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(0.75.dp, HextechGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenChampionDetail(myChamp.id) }
                .padding(2.dp)
                .testTag("my_champion_profile_button"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ChampionCrestAvatar(champion = myChamp, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = myChamp.displayName(lang),
                  style = MaterialTheme.typography.titleMedium,
                  color = HextechGoldBright,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                TierBadge(myChamp.tierRank)
              }
              Spacer(modifier = Modifier.height(2.dp))
              DamageTypeBadge(myChamp.damageType, lang)
            }
          }

          OutlinedButton(
            onClick = onOpenMyChampPicker,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("select_my_champion_card"),
          ) {
            Icon(
              Icons.Default.SwapHoriz,
              contentDescription = "Change Champion",
              tint = HextechGold,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (lang == AppLanguage.TH) "สลับตัว" else "Swap",
              style = MaterialTheme.typography.labelMedium,
              color = HextechGoldBright,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(Lane.entries, key = { it.name }) { lane ->
        FilterChip(
          selected = state.selectedLane == lane,
          onClick = { onSelectLane(lane) },
          modifier = Modifier.testTag("analyzer_lane_${lane.name}"),
          label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
        )
      }
    }
  }
}

@Composable
fun AnalyzerEnemyTeamSetupCard(
  state: AnalyzerUiState,
  lang: AppLanguage,
  onClearEnemies: () -> Unit,
  onOpenEnemyPicker: () -> Unit,
  onToggleEnemy: (String) -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onApplyPresetEnemies: (List<String>) -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  var presetsExpanded by remember(state.selectedEnemies.isEmpty()) {
    mutableStateOf(state.selectedEnemies.isEmpty())
  }

  HextechCard(accentColor = ThreatCrimson) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${strings.selectEnemiesTitle} (${state.selectedEnemies.size}/5)",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.weight(1f),
      )
      if (state.selectedEnemies.isNotEmpty()) {
        TextButton(
          onClick = { presetsExpanded = !presetsExpanded },
          modifier = Modifier.testTag("analyzer_presets_toggle"),
        ) {
          Text(if (lang == AppLanguage.TH) "ทีมสำเร็จรูป" else "Presets")
          Icon(
            imageVector = if (presetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        }
        TextButton(
          onClick = onClearEnemies,
          modifier = Modifier.testTag("clear_enemies_button"),
        ) {
          Text(strings.clearAll, color = ThreatCrimson)
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(if (isMiniMode) 4.dp else 8.dp),
    ) {
      for (index in 0 until 5) {
        val enemy = state.selectedEnemies.getOrNull(index)
        Surface(
          modifier =
            Modifier.weight(1f)
              .height(if (isMiniMode) 60.dp else 72.dp)
              .clip(MaterialTheme.shapes.small)
              .then(if (enemy == null) Modifier.clickable { onOpenEnemyPicker() } else Modifier)
              .testTag("enemy_slot_$index"),
          color =
            if (enemy != null) ThreatCrimson.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
          border =
            BorderStroke(
              0.75.dp,
              if (enemy != null) ThreatCrimson.copy(alpha = 0.5f)
              else MaterialTheme.colorScheme.outline,
            ),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(3.dp)) {
            if (enemy != null) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier =
                  Modifier.fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenChampionDetail(enemy.id) }
                    .padding(top = 4.dp)
                    .testTag("enemy_slot_profile_$index"),
              ) {
                ChampionCrestAvatar(champion = enemy, size = 34.dp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = enemy.nameEn,
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Box(
                modifier =
                  Modifier.align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                    .clickable { onToggleEnemy(enemy.id) }
                    .testTag("enemy_slot_remove_$index"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Remove ${enemy.nameEn}",
                  modifier = Modifier.size(13.dp),
                  tint = ThreatCrimson,
                )
              }
            } else {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.Add,
                  contentDescription = "Add enemy",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "#${index + 1}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
      onClick = onOpenEnemyPicker,
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("pick_enemies_button"),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
      Icon(Icons.Default.Add, contentDescription = null)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text =
          if (lang == AppLanguage.TH) "เลือก / แก้ไข 5 แชมเปี้ยนฝั่งตรงข้าม"
          else "Pick / Edit 5 Enemy Champions",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    AnimatedVisibility(visible = presetsExpanded) {
      LazyRow(
        modifier = Modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          FilterChip(
            selected = false,
            onClick = { onApplyPresetEnemies(listOf("aatrox", "sylas", "zed", "jinx", "soraka")) },
            modifier = Modifier.testTag("preset_heal_burst"),
            label = { Text(if (lang == AppLanguage.TH) "ฮีลหนัก + เบิร์ส" else "Heal + Burst") },
          )
        }
        item {
          FilterChip(
            selected = false,
            onClick = {
              onApplyPresetEnemies(listOf("dr_mundo", "rammus", "yasuo", "kalista", "leona"))
            },
            modifier = Modifier.testTag("preset_tank_cc"),
            label = { Text(if (lang == AppLanguage.TH) "แทงค์ + CC + ตีไว" else "Tank + CC + AS") },
          )
        }
        item {
          FilterChip(
            selected = false,
            onClick = {
              onApplyPresetEnemies(listOf("malphite", "evelynn", "syndra", "kaisa", "lux"))
            },
            modifier = Modifier.testTag("preset_ap_burst"),
            label = { Text(if (lang == AppLanguage.TH) "เบิร์สเวทหนัก" else "Heavy AP") },
          )
        }
      }
    }
  }
}

@Composable
fun AnalyzerSavedMatchupsCard(
  savedMatchups: List<SavedMatchupEntity>,
  onLoadSavedMatchup: (SavedMatchupEntity) -> Unit,
  onDeleteSavedMatchup: (Long) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard {
    Text(text = strings.savedMatchupsTitle, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(6.dp))
    savedMatchups.take(3).forEach { saved ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onLoadSavedMatchup(saved) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${saved.label} (${saved.lane.labelEn})",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onDeleteSavedMatchup(saved.id) }) {
          Icon(
            Icons.Default.DeleteOutline,
            contentDescription = "Delete preset",
            tint = ThreatCrimson,
          )
        }
      }
    }
  }
}
