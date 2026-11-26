package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.CoachSpeechState
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ItemData
import com.example.data.model.MatchJournalEntryData
import com.example.domain.engine.MatchJournalAggregator
import com.example.domain.engine.MatchJournalSummary
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemIconSlot
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MatchJournalTab(
  entries: List<MatchJournalEntryData>,
  summary: MatchJournalSummary,
  championsById: Map<String, ChampionData>,
  itemsById: Map<String, ItemData>,
  language: AppLanguage,
  speechState: CoachSpeechState,
  onOpenPostGameScanner: () -> Unit,
  onToggleSpeak: (String, String) -> Unit,
  onAskCoachAboutMatch: (MatchJournalEntryData) -> Unit,
  onDeleteEntry: (Long) -> Unit,
) {
  val isMiniMode = LocalInGameMiniMode.current
  var selectedEntry by remember { mutableStateOf<MatchJournalEntryData?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("match_journal_lazy_column"),
    contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
  ) {
    // Primary Scan CTA (compact header)
    item {
      HextechCard(accentColor = HextechGold) {
        Text(
          text = "Match Journal & Post-Game Review",
          style = MaterialTheme.typography.titleMedium,
          color = HextechGoldBright,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = onOpenPostGameScanner,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("journal_scan_post_game_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = HextechGold,
              contentColor = Color(0xFF140F02),
            ),
        ) {
          Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (language == AppLanguage.TH) "+ สแกน / บันทึกผลหลังจบเกม"
              else "+ Scan / Log Post-Game Match",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (entries.isEmpty()) {
      item {
        HextechCard {
          Text(
            text =
              if (language == AppLanguage.TH) {
                "ยังไม่มีประวัติแมตช์ที่บันทึก กด '+ สแกน / บันทึกผลหลังจบเกม' ด้านบน"
              } else {
                "No matches logged yet. Tap '+ Scan / Log Post-Game Match' above."
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      // Overall Performance Snapshot
      item {
        HextechCard(accentColor = ArcaneCyan, modifier = Modifier.testTag("journal_summary_card")) {
          Text(
            text =
              if (language == AppLanguage.TH) "สถิติรวม (${summary.totalGames} เกม)"
              else "Overall Stats (${summary.totalGames} Games)",
            style = MaterialTheme.typography.titleMedium,
            color = ArcaneCyan,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            JournalMetricChip(
              label = "Winrate",
              value = "${summary.winRatePct}% (${summary.wins}W-${summary.losses}L)",
              accent = if (summary.winRatePct >= 50) AdvantageEmerald else WarningAmber,
              modifier = Modifier.weight(1f),
            )
            JournalMetricChip(
              label = "Avg KDA",
              value = String.format(Locale.US, "%.1f", summary.avgKda),
              accent = HextechGoldBright,
              modifier = Modifier.weight(0.8f),
            )
            JournalMetricChip(
              label = if (language == AppLanguage.TH) "ความแม่นบิลด์" else "Build Acc",
              value = "${summary.avgBuildAccuracyPct}%",
              accent = if (summary.avgBuildAccuracyPct >= 75) AdvantageEmerald else ArcaneCyan,
              modifier = Modifier.weight(1f),
            )
          }

          if (summary.perChampionStats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(summary.perChampionStats.take(6), key = { it.championId }) { cStat ->
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(0.75.dp, HextechGold.copy(alpha = 0.4f)),
                ) {
                  Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                      text = cStat.championName,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = HextechGoldBright,
                    )
                    Text(
                      text = "${cStat.wins}W-${cStat.games - cStat.wins}L • ${cStat.avgBuildAccuracyPct}% build",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (summary.recurringMistakes.isNotEmpty()) {
        item {
          HextechCard(accentColor = WarningAmber, modifier = Modifier.testTag("journal_mistakes_card")) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningAmber)
              Text(
                text =
                  if (language == AppLanguage.TH) "จุดพลาดที่พบบ่อย"
                  else "Recurring Mistakes",
                style = MaterialTheme.typography.titleSmall,
                color = WarningAmber,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            summary.recurringMistakes.take(2).forEach { m ->
              Text(
                text = "• ${m.label(language)} (${m.count}x)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
          }
        }
      }

      // Concise Match Summary Cards (tap to open full Review Detail Sheet)
      items(entries, key = { it.id }) { entry ->
        val champ = championsById[entry.championId]
        val statusColor = if (entry.isWin) AdvantageEmerald else ThreatCrimson
        val dateText =
          remember(entry.playedAt) {
            SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(entry.playedAt))
          }

        HextechCard(
          accentColor = statusColor,
          modifier = Modifier.fillMaxWidth().testTag("journal_entry_card_${entry.id}"),
          onClick = { selectedEntry = entry },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (champ != null) {
              ChampionCrestAvatar(champion = champ, size = 40.dp)
              Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = statusColor.copy(alpha = 0.2f),
                ) {
                  Text(
                    text = if (entry.isWin) "WIN" else "LOSS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                  )
                }
                Text(
                  text = champ?.displayName(language) ?: entry.championId,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (language == AppLanguage.TH) entry.lane.labelTh else entry.lane.labelEn,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text =
                  "${entry.kills}/${entry.deaths}/${entry.assists} • Build ${entry.buildAccuracyPct}% • $dateText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { onDeleteEntry(entry.id) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete match",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  selectedEntry?.let { entry ->
    val champ = championsById[entry.championId]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { selectedEntry = null },
      sheetState = sheetState,
      containerColor = HextechSurface,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("journal_entry_detail_sheet"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (champ != null) {
            ChampionCrestAvatar(champion = champ, size = 44.dp)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                "${champ?.displayName(language) ?: entry.championId} (${if (entry.isWin) "WIN" else "LOSS"})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = if (entry.isWin) AdvantageEmerald else ThreatCrimson,
            )
            Text(
              text = "${entry.kills}/${entry.deaths}/${entry.assists} • Build ${entry.buildAccuracyPct}%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (entry.boughtItemIds.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(entry.boughtItemIds.mapNotNull { itemsById[it] }, key = { "jb_${entry.id}_${it.id}" }) { item ->
              ItemIconSlot(
                item = item,
                language = language,
                onClick = {},
                size = 34.dp,
              )
            }
          }
        }

        if (entry.mistakeTags.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            entry.mistakeTags.forEach { tag ->
              val stat = MatchJournalAggregator.mapMistakeStat(tag, 1)
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = WarningAmber.copy(alpha = 0.16f),
              ) {
                Text(
                  text = stat.label(language),
                  style = MaterialTheme.typography.labelSmall,
                  color = WarningAmber,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }
          }
        }

        if (entry.lessons.isNotEmpty()) {
          entry.lessons.forEachIndexed { idx, lesson ->
            Text(
              text = "${idx + 1}. $lesson",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(vertical = 2.dp),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val ttsKey = "journal_entry_${entry.id}"
          val isSpeaking = speechState.isSpeaking && speechState.activeMessageId == ttsKey
          OutlinedButton(
            onClick = { onToggleSpeak(ttsKey, entry.lessons.joinToString(". ")) },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("journal_tts_${entry.id}"),
          ) {
            Icon(
              imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isSpeaking) "Stop" else if (language == AppLanguage.TH) "ฟังสรุป" else "Speak")
          }
          Button(
            onClick = {
              selectedEntry = null
              onAskCoachAboutMatch(entry)
            },
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("journal_ask_coach_${entry.id}"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = ArcaneCyan,
                contentColor = Color(0xFF031520),
              ),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (language == AppLanguage.TH) "ถาม AI โค้ช" else "Ask Coach",
              fontWeight = FontWeight.Bold,
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}

@Composable
private fun JournalMetricChip(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = accent.copy(alpha = 0.12f),
    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Box(contentAlignment = Alignment.CenterStart) {
        Text(
          text = value,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = accent,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
