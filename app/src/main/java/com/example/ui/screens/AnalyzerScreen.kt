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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.SavedMatchupEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.LaneDifficulty
import com.example.data.model.SpellData
import com.example.domain.engine.EnemyMatchupAnalysis
import com.example.domain.engine.EnemyTeamThreatProfile
import com.example.domain.engine.ItemRecommendationEntry
import com.example.domain.engine.SmartBuildRecommendation
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemDetailBottomSheet
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.SpellDetailBottomSheet
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.TacticalMeterBar
import com.example.ui.components.TierBadge
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AnalyzerUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
  state: AnalyzerUiState,
  onSelectMyChampion: (ChampionData) -> Unit,
  onSelectLane: (Lane) -> Unit,
  onToggleEnemy: (String) -> Unit,
  onApplyPresetEnemies: (List<String>) -> Unit,
  onClearEnemies: () -> Unit,
  onSaveMatchup: () -> Unit,
  onLoadSavedMatchup: (SavedMatchupEntity) -> Unit,
  onDeleteSavedMatchup: (Long) -> Unit,
  onAskCoachForMatchup: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val lang = LocalUserPrefs.current.language

  var showMyChampPicker by remember { mutableStateOf(false) }
  var showEnemyPicker by remember { mutableStateOf(false) }
  var selectedReportTab by remember { mutableIntStateOf(0) }
  var inspectedItem by remember { mutableStateOf<ItemData?>(null) }
  var inspectedSpell by remember { mutableStateOf<SpellData?>(null) }

  val myChamp = state.selectedChampion

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("analyzer_lazy_column"),
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Step 1: Select Your Champion & Lane
      item {
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
          Spacer(modifier = Modifier.height(8.dp))

          if (myChamp != null) {
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(MaterialTheme.shapes.small)
                  .clickable { showMyChampPicker = true }
                  .testTag("select_my_champion_card"),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.6f)),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ChampionCrestAvatar(champion = myChamp, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = myChamp.displayName(lang),
                      style = MaterialTheme.typography.titleMedium,
                      color = HextechGoldBright,
                    )
                    TierBadge(myChamp.tierRank)
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  DamageTypeBadge(myChamp.damageType, lang)
                }
                Icon(Icons.Default.SwapHoriz, contentDescription = "Change Champion", tint = HextechGold)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = strings.selectLane,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Lane.entries.forEach { lane ->
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

      // Step 2: Select 5 Enemy Champions
      item {
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
                onClick = onClearEnemies,
                modifier = Modifier.testTag("clear_enemies_button"),
              ) {
                Text(strings.clearAll, color = ThreatCrimson)
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // 5 Enemy Slots Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            for (index in 0 until 5) {
              val enemy = state.selectedEnemies.getOrNull(index)
              Surface(
                modifier =
                  Modifier.weight(1f)
                    .height(78.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                      if (enemy != null) onToggleEnemy(enemy.id) else showEnemyPicker = true
                    }
                    .testTag("enemy_slot_$index"),
                color =
                  if (enemy != null) ThreatCrimson.copy(alpha = 0.14f)
                  else MaterialTheme.colorScheme.surfaceContainerHigh,
                border =
                  BorderStroke(
                    1.dp,
                    if (enemy != null) ThreatCrimson.copy(alpha = 0.65f)
                    else MaterialTheme.colorScheme.outline,
                  ),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                  if (enemy != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      ChampionCrestAvatar(champion = enemy, size = 34.dp)
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = enemy.nameEn,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Remove ${enemy.nameEn}",
                      modifier = Modifier.align(Alignment.TopEnd).size(14.dp),
                      tint = ThreatCrimson,
                    )
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

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = { showEnemyPicker = true },
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
            )
          }

          // Quick Enemy Comp Test Presets
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              if (lang == AppLanguage.TH) "ตัวอย่างทีมศัตรูยอดนิยม (กดทดสอบได้ทันที):"
              else "Quick Enemy Comp Scenarios:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = false,
                onClick = {
                  onApplyPresetEnemies(
                    listOf("aatrox", "sylas", "zed", "jinx", "soraka")
                  )
                },
                modifier = Modifier.testTag("preset_heal_burst"),
                label = {
                  Text(if (lang == AppLanguage.TH) "ฮีลหนัก + แอสซาซินเบิร์ส" else "Heal + Dive Burst")
                },
              )
            }
            item {
              FilterChip(
                selected = false,
                onClick = {
                  onApplyPresetEnemies(
                    listOf("dr_mundo", "rammus", "yasuo", "kalista", "leona")
                  )
                },
                modifier = Modifier.testTag("preset_tank_cc"),
                label = {
                  Text(if (lang == AppLanguage.TH) "แทงค์ถึก + CC + ตีไว" else "Tank + Heavy CC + AS")
                },
              )
            }
            item {
              FilterChip(
                selected = false,
                onClick = {
                  onApplyPresetEnemies(
                    listOf("malphite", "evelynn", "syndra", "kaisa", "lux")
                  )
                },
                modifier = Modifier.testTag("preset_ap_burst"),
                label = {
                  Text(if (lang == AppLanguage.TH) "เบิร์สเวทหนัก 4 ตัว" else "Heavy AP Burst")
                },
              )
            }
          }
        }
      }

      // Saved Matchup Presets if available
      if (state.savedMatchups.isNotEmpty() && state.selectedEnemies.isEmpty()) {
        item {
          HextechCard {
            Text(text = strings.savedMatchupsTitle, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(6.dp))
            state.savedMatchups.take(3).forEach { saved ->
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
      }

      // Empty state prompt if no enemies selected yet
      if (state.selectedEnemies.isEmpty()) {
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = ArcaneCyan)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) {
                    "เลือกฝั่งตรงข้ามอย่างน้อย 1 ตัว (แนะนำครบ 5 ตัว) เพื่อปลดล็อกการวิเคราะห์ภัยคุกคาม บิลด์ปรับแก้ทาง ลำดับการออกไอเทม และแผนเล่นต้น-กลาง-ท้ายเกม"
                  } else {
                    "Select 1 to 5 enemy champions above to unlock threat ranking, adapted counter build order, rune/spell tuning, and phase-by-phase game plans."
                  },
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }
      } else if (state.smartBuild != null && state.threatProfile != null) {
        // Tabbed Tactical Report
        item {
          val tabs =
            if (lang == AppLanguage.TH) {
              listOf("ภัยคุกคาม & Matchup", "Smart Build & ลำดับ", "รูน & แผนเล่น")
            } else {
              listOf("Matchups & Threat", "Smart Build & Order", "Runes & Plan")
            }
          SecondaryTabRow(
            selectedTabIndex = selectedReportTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ) {
            tabs.forEachIndexed { idx, label ->
              Tab(
                selected = selectedReportTab == idx,
                onClick = { selectedReportTab = idx },
                modifier = Modifier.testTag("analyzer_report_tab_$idx"),
                text = {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
              )
            }
          }
        }

        when (selectedReportTab) {
          0 -> {
            // Highest Threat Callout
            state.highestThreat?.let { top ->
              item { TopThreatSpotlightCard(top = top, lang = lang) }
            }
            // Team Threat Profile Meters
            item { ThreatProfileCard(profile = state.threatProfile, lang = lang) }
            // Individual Matchup Cards
            item {
              Text(
                text = strings.matchupVerdictTitle,
                style = MaterialTheme.typography.titleMedium,
              )
            }
            items(state.enemyMatchups, key = { it.enemy.id }) { analysis ->
              EnemyMatchupCard(analysis = analysis, lang = lang)
            }
          }

          1 -> {
            val build = state.smartBuild
            item {
              FirstRushItemCard(
                entry = build.firstRushRecommendation,
                lang = lang,
                onInspectItem = { inspectedItem = it },
              )
            }
            item {
              BuildDiffCard(
                build = build,
                lang = lang,
                onInspectItem = { inspectedItem = it },
              )
            }
            item {
              Text(text = strings.smartBuildTitle, style = MaterialTheme.typography.titleMedium)
            }
            items(build.prioritizedItems, key = { "${it.priorityLabelEn}_${it.item.id}" }) { entry ->
              SmartItemDetailCard(
                entry = entry,
                lang = lang,
                onInspectItem = { inspectedItem = it },
              )
            }
          }

          2 -> {
            val build = state.smartBuild
            item {
              RunesAndSpellsCard(
                build = build,
                skillOrder = myChamp?.skillOrder.orEmpty(),
                lang = lang,
                onInspectSpell = { inspectedSpell = it },
              )
            }
            item {
              GamePlanPhaseCard(
                build = build,
                lang = lang,
                onAskCoach = onAskCoachForMatchup,
              )
            }
          }
        }
      }
    }
  }

  if (showMyChampPicker) {
    ChampionPickerBottomSheet(
      title = strings.selectYourChampion,
      champions = state.allChampions,
      selectedIds = setOfNotNull(myChamp?.id),
      language = lang,
      onSelectChampion = {
        onSelectMyChampion(it)
        showMyChampPicker = false
      },
      onDismiss = { showMyChampPicker = false },
    )
  }

  if (showEnemyPicker) {
    ChampionPickerBottomSheet(
      title = strings.selectEnemiesTitle,
      champions = state.allChampions,
      selectedIds = state.selectedEnemies.map { it.id }.toSet(),
      disabledIds = setOfNotNull(myChamp?.id),
      language = lang,
      onSelectChampion = { onToggleEnemy(it.id) },
      onDismiss = { showEnemyPicker = false },
    )
  }

  inspectedItem?.let { item ->
    ItemDetailBottomSheet(
      item = item,
      language = lang,
      onDismiss = { inspectedItem = null },
    )
  }

  inspectedSpell?.let { spell ->
    SpellDetailBottomSheet(
      spell = spell,
      language = lang,
      onDismiss = { inspectedSpell = null },
    )
  }
}

@Composable
private fun TopThreatSpotlightCard(top: EnemyMatchupAnalysis, lang: AppLanguage) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = ThreatCrimson, modifier = Modifier.testTag("top_threat_card")) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Warning, contentDescription = null, tint = ThreatCrimson)
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = strings.topThreatLabel,
        style = MaterialTheme.typography.labelLarge,
        color = ThreatCrimson,
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      ChampionCrestAvatar(champion = top.enemy, size = 50.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = top.enemy.displayName(lang),
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = top.dangerAbilities(lang),
          style = MaterialTheme.typography.bodySmall,
          color = WarningAmber,
        )
      }
      Surface(
        color = ThreatCrimson.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
      ) {
        Text(
          text = "${top.threatScore}/100",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ThreatCrimson,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = top.tacticalTip(lang), style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ThreatProfileCard(profile: EnemyTeamThreatProfile, lang: AppLanguage) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = ArcaneCyan) {
    Text(text = strings.teamThreatProfileTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "เบิร์สเวท (AP Burst)" else "AP Burst",
        valuePercent = profile.apBurstScore,
        barColor = ArcaneCyan,
      )
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "ดาเมจกายภาพ (AD DPS)" else "Physical AD DPS",
        valuePercent = profile.adDpsScore,
        barColor = WarningAmber,
      )
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "ฮีล / ดูดเลือด (Healing)" else "Healing / Sustain",
        valuePercent = profile.healingScore,
        barColor = AdvantageEmerald,
      )
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "ความถึกแทงค์ (Tankiness)" else "Frontline Tanks",
        valuePercent = profile.tankinessScore,
        barColor = HextechGold,
      )
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "CC ล็อก (Crowd Control)" else "Hard CC",
        valuePercent = profile.ccScore,
        barColor = SkillViolet,
      )
      TacticalMeterBar(
        label = if (lang == AppLanguage.TH) "ความเร็วโจมตี (Attack Speed)" else "Attack Speed",
        valuePercent = profile.attackSpeedScore,
        barColor = ThreatCrimson,
      )
    }
  }
}

@Composable
private fun EnemyMatchupCard(analysis: EnemyMatchupAnalysis, lang: AppLanguage) {
  val (verdictColor, verdictLabel) =
    when (analysis.verdict) {
      LaneDifficulty.EASY ->
        AdvantageEmerald to (if (lang == AppLanguage.TH) "ได้เปรียบ" else "Advantage")
      LaneDifficulty.HARD ->
        ThreatCrimson to (if (lang == AppLanguage.TH) "เสียเปรียบ" else "Disadvantage")
      LaneDifficulty.SKILL ->
        WarningAmber to (if (lang == AppLanguage.TH) "วัดฝีมือ" else "Skill Matchup")
      LaneDifficulty.EVEN ->
        ArcaneCyan to (if (lang == AppLanguage.TH) "สูสี" else "Even")
    }

  HextechCard(accentColor = verdictColor) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ChampionCrestAvatar(champion = analysis.enemy, size = 42.dp)
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = analysis.enemy.displayName(lang),
            style = MaterialTheme.typography.titleMedium,
          )
          if (analysis.isLikelyLaneOpponent) {
            Surface(
              color = HextechGold.copy(alpha = 0.2f),
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = if (lang == AppLanguage.TH) "คู่เลน" else "Lane",
                style = MaterialTheme.typography.labelSmall,
                color = HextechGoldBright,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(
          text =
            "${if (lang == AppLanguage.TH) "จุดพีค:" else "Spike:"} ${analysis.keySpike(lang)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        color = verdictColor.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.5f)),
      ) {
        Text(
          text = verdictLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = verdictColor,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = analysis.dangerAbilities(lang),
      style = MaterialTheme.typography.bodySmall,
      color = WarningAmber,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = analysis.tacticalTip(lang), style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun FirstRushItemCard(
  entry: ItemRecommendationEntry,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = HextechGold, modifier = Modifier.testTag("first_rush_item_card")) {
    Text(
      text = strings.buyFirstLabel,
      style = MaterialTheme.typography.labelLarge,
      color = HextechGoldBright,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ItemIconSlot(
        item = entry.item,
        language = lang,
        onClick = { onInspectItem(entry.item) },
        size = 56.dp,
        accentColor = HextechGold,
        badgeText = "#1",
        badgeColor = HextechGold,
        showCost = false,
        showName = false,
        modifier = Modifier.testTag("analyzer_rush_item_icon"),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = entry.item.displayName(lang),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${entry.item.cost}g",
            style = MaterialTheme.typography.titleSmall,
            color = HextechGold,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = entry.item.statsSummary(lang),
          style = MaterialTheme.typography.bodySmall,
          color = ArcaneCyan,
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = entry.reason(lang), style = MaterialTheme.typography.bodyMedium)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildDiffCard(
  build: SmartBuildRecommendation,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  val strings = LocalUiStrings.current
  val diff = build.buildDiff
  HextechCard(accentColor = ArcaneCyan) {
    Text(text = strings.buildDiffTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) diff.adaptationHeadlineTh else diff.adaptationHeadlineEn,
      style = MaterialTheme.typography.bodyMedium,
      color = ArcaneCyan,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "กดที่รูปไอเทมเพื่อดูค่าพลัง ราคา และสกิลติดตัว"
        else "Tap any item icon to inspect stats, cost & passives",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = if (lang == AppLanguage.TH) "บิลด์มาตรฐาน:" else "Standard Core:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      diff.standardCoreItems.forEach { item ->
        val isSwappedOut = diff.swappedOutItems.any { it.id == item.id }
        ItemIconSlot(
          item = item,
          language = lang,
          onClick = { onInspectItem(item) },
          size = 48.dp,
          accentColor = if (isSwappedOut) ThreatCrimson else MaterialTheme.colorScheme.outline,
          badgeText = if (isSwappedOut) "−" else null,
          badgeColor = ThreatCrimson,
          showCost = true,
          showName = true,
          modifier = Modifier.testTag("diff_std_item_${item.id}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = if (lang == AppLanguage.TH) "บิลด์ที่ปรับสำหรับไฟต์นี้:" else "Adapted Core:",
      style = MaterialTheme.typography.labelMedium,
      color = AdvantageEmerald,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      diff.adaptedCoreItems.forEach { item ->
        val isSwappedIn = diff.swappedInItems.any { it.id == item.id }
        ItemIconSlot(
          item = item,
          language = lang,
          onClick = { onInspectItem(item) },
          size = 48.dp,
          accentColor = if (isSwappedIn) AdvantageEmerald else HextechGold,
          badgeText = if (isSwappedIn) "+" else null,
          badgeColor = AdvantageEmerald,
          showCost = true,
          showName = true,
          modifier = Modifier.testTag("diff_adapted_item_${item.id}"),
        )
      }
    }
  }
}

@Composable
private fun SmartItemDetailCard(
  entry: ItemRecommendationEntry,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  var expanded by remember { mutableStateOf(entry.isAdapted) }
  val accent = if (entry.isAdapted) HextechGold else MaterialTheme.colorScheme.outline

  HextechCard(
    accentColor = if (entry.isAdapted) HextechGold else null,
    onClick = { expanded = !expanded },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ItemIconSlot(
        item = entry.item,
        language = lang,
        onClick = { onInspectItem(entry.item) },
        size = 48.dp,
        accentColor = if (entry.isAdapted) HextechGold else ArcaneCyan,
        showCost = false,
        showName = false,
        modifier = Modifier.testTag("analyzer_item_icon_${entry.item.id}"),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color =
              if (entry.isAdapted) HextechGold.copy(alpha = 0.22f)
              else ArcaneCyan.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = entry.priorityLabel(lang),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (entry.isAdapted) HextechGoldBright else ArcaneCyan,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${entry.item.cost}g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = entry.item.displayName(lang),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = entry.item.statsSummary(lang),
          style = MaterialTheme.typography.bodySmall,
          color = accent,
        )
      }
      Icon(
        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = "Toggle item details",
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "${if (lang == AppLanguage.TH) "แก้ทาง:" else "Targets:"} ${entry.targetThreat(lang)}",
      style = MaterialTheme.typography.labelMedium,
      color = WarningAmber,
    )

    if (expanded) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "เหตุผล:" else "Why:"} ${entry.reason(lang)}",
        style = MaterialTheme.typography.bodySmall,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "จังหวะซื้อ:" else "Timing:"} ${entry.buyTiming(lang)}",
        style = MaterialTheme.typography.bodySmall,
        color = ArcaneCyan,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "ข้อได้เปรียบ:" else "Advantage:"} ${entry.tacticalBenefit(lang)}",
        style = MaterialTheme.typography.bodySmall,
        color = AdvantageEmerald,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunesAndSpellsCard(
  build: SmartBuildRecommendation,
  skillOrder: String,
  lang: AppLanguage,
  onInspectSpell: (SpellData) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = SkillViolet) {
    Text(text = strings.runesAndSpellsTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (lang == AppLanguage.TH) build.runeSpellReasonTh else build.runeSpellReasonEn,
      style = MaterialTheme.typography.bodySmall,
      color = ArcaneCyan,
    )
    Spacer(modifier = Modifier.height(10.dp))

    build.keystone?.let { ks ->
      Text(
        text = "Keystone: ${ks.displayName(lang)}",
        style = MaterialTheme.typography.titleSmall,
        color = HextechGoldBright,
      )
      Text(text = ks.desc(lang), style = MaterialTheme.typography.bodySmall)
      Spacer(modifier = Modifier.height(8.dp))
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      build.minorRunes.forEach { rune ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, SkillViolet.copy(alpha = 0.5f)),
        ) {
          Text(
            text = rune.displayName(lang),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = if (lang == AppLanguage.TH) "Summoner Spells & อัปสกิล ($skillOrder):" else "Summoner Spells & Skill Order ($skillOrder):",
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "กดที่รูป Spell เพื่อดูคูลดาวน์และรายละเอียด"
        else "Tap a spell icon to view cooldown & usage",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      build.spells.forEach { spell ->
        SpellIconSlot(
          spell = spell,
          language = lang,
          onClick = { onInspectSpell(spell) },
          modifier = Modifier.testTag("analyzer_spell_${spell.id}"),
        )
      }
    }
  }
}

@Composable
private fun GamePlanPhaseCard(
  build: SmartBuildRecommendation,
  lang: AppLanguage,
  onAskCoach: () -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = AdvantageEmerald) {
    Text(text = strings.gamePlanTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(10.dp))

    PhaseBlock(
      label = strings.earlyPhase,
      color = ArcaneCyan,
      body = if (lang == AppLanguage.TH) build.earlyGamePlanTh else build.earlyGamePlanEn,
    )
    Spacer(modifier = Modifier.height(10.dp))
    PhaseBlock(
      label = strings.midPhase,
      color = HextechGold,
      body = if (lang == AppLanguage.TH) build.midGamePlanTh else build.midGamePlanEn,
    )
    Spacer(modifier = Modifier.height(10.dp))
    PhaseBlock(
      label = strings.latePhase,
      color = AdvantageEmerald,
      body = if (lang == AppLanguage.TH) build.lateGamePlanTh else build.lateGamePlanEn,
    )

    Spacer(modifier = Modifier.height(14.dp))
    OutlinedButton(
      onClick = onAskCoach,
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("analyzer_ask_coach_button"),
    ) {
      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArcaneCyan)
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.askCoachAboutMatchup)
    }
  }
}

@Composable
private fun PhaseBlock(label: String, color: Color, body: String) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Text(text = label, style = MaterialTheme.typography.titleSmall, color = color)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
