package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SavedMatchupEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.components.HextechCard
import com.example.ui.components.MiniInspectHost
import com.example.ui.components.MiniInspectTarget
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.viewmodel.AnalyzerUiState

@OptIn(ExperimentalMaterial3Api::class)
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
  onSaveSmartBuildAsCustom: () -> Unit,
  onAskCoachForMatchup: () -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = LocalUserPrefs.current.language

  var showMyChampPicker by remember { mutableStateOf(false) }
  var showEnemyPicker by remember { mutableStateOf(false) }
  var selectedReportTab by remember { mutableIntStateOf(0) }
  var miniInspectTarget by remember { mutableStateOf<MiniInspectTarget?>(null) }

  val myChamp = state.selectedChampion

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("analyzer_lazy_column"),
      contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
      verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 7.dp else 10.dp),
    ) {
      item {
        AnalyzerMyChampionSetupCard(
          myChamp = myChamp,
          state = state,
          lang = lang,
          onSaveMatchup = onSaveMatchup,
          onOpenChampionDetail = onOpenChampionDetail,
          onOpenMyChampPicker = { showMyChampPicker = true },
          onSelectLane = onSelectLane,
        )
      }

      item {
        AnalyzerEnemyTeamSetupCard(
          state = state,
          lang = lang,
          onClearEnemies = onClearEnemies,
          onOpenEnemyPicker = { showEnemyPicker = true },
          onToggleEnemy = onToggleEnemy,
          onOpenChampionDetail = onOpenChampionDetail,
          onApplyPresetEnemies = onApplyPresetEnemies,
        )
      }

      if (state.savedMatchups.isNotEmpty() && state.selectedEnemies.isEmpty()) {
        item {
          AnalyzerSavedMatchupsCard(
            savedMatchups = state.savedMatchups,
            onLoadSavedMatchup = onLoadSavedMatchup,
            onDeleteSavedMatchup = onDeleteSavedMatchup,
          )
        }
      }

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
            state.highestThreat?.let { top ->
              item {
                TopThreatSpotlightCard(
                  top = top,
                  lang = lang,
                  onOpenChampionProfile = onOpenChampionDetail,
                )
              }
            }
            item { ThreatProfileCard(profile = state.threatProfile, lang = lang) }
            item {
              Text(text = strings.matchupVerdictTitle, style = MaterialTheme.typography.titleMedium)
            }
            items(state.enemyMatchups, key = { it.enemy.id }) { analysis ->
              EnemyMatchupCard(
                analysis = analysis,
                lang = lang,
                onOpenChampionProfile = onOpenChampionDetail,
              )
            }
          }

          1 -> {
            val build = state.smartBuild
            item {
              FirstRushItemCard(
                entry = build.firstRushRecommendation,
                lang = lang,
                onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
              )
            }
            item {
              BuildDiffCard(
                build = build,
                lang = lang,
                onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
              )
            }
            item {
              var justSavedCustom by remember(build) { mutableStateOf(false) }
              OutlinedButton(
                onClick = {
                  onSaveSmartBuildAsCustom()
                  justSavedCustom = true
                },
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("analyzer_save_custom_build_button"),
              ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = HextechGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    when {
                      justSavedCustom && lang == AppLanguage.TH -> "บันทึกเข้า 'เซ็ตฉัน' เรียบร้อย ✓"
                      justSavedCustom -> "Saved to My Custom Builds ✓"
                      lang == AppLanguage.TH -> "บันทึกบิลด์นี้เป็นเซ็ตของฉัน"
                      else -> "Save as My Custom Build"
                    },
                  color = if (justSavedCustom) AdvantageEmerald else HextechGoldBright,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
            item {
              Text(text = strings.smartBuildTitle, style = MaterialTheme.typography.titleMedium)
            }
            items(build.prioritizedItems, key = { "${it.priorityLabelEn}_${it.item.id}" }) { entry ->
              SmartItemDetailCard(
                entry = entry,
                lang = lang,
                onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
              )
            }
          }

          2 -> {
            val build = state.smartBuild
            item {
              RunesRecommendationCard(
                build = build,
                lang = lang,
                onInspectRune = { miniInspectTarget = MiniInspectTarget.Rune(it) },
              )
            }
            item {
              SpellsAndSkillOrderCard(
                build = build,
                skillOrder = build.skillOrder.ifBlank { myChamp?.skillOrder.orEmpty() },
                lang = lang,
                onInspectSpell = { miniInspectTarget = MiniInspectTarget.Spell(it) },
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

  MiniInspectHost(
    target = miniInspectTarget,
    language = lang,
    onDismiss = { miniInspectTarget = null },
    itemsById = state.itemsById,
  )
}
