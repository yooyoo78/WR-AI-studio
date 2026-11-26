package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.Lane
import com.example.data.remote.MetaRefreshState
import com.example.data.remote.MetaServerRegion
import com.example.domain.engine.TopMetaChampionEntry
import com.example.domain.engine.TopMetaSortMode
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechNavy
import com.example.ui.theme.HextechObsidian
import com.example.ui.theme.HextechOutline
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopPlayedMetaWidget(
  entries: List<TopMetaChampionEntry>,
  selectedLane: Lane?,
  selectedSortMode: TopMetaSortMode,
  avgWinRatePct: Double,
  avgPickRatePct: Double,
  patchVersion: String,
  language: AppLanguage,
  selectedServer: MetaServerRegion = MetaServerRegion.CN_DIAMOND_PLUS,
  metaRefreshState: MetaRefreshState = MetaRefreshState.Idle,
  lastSyncedMs: Long = 0L,
  onSelectServer: (MetaServerRegion) -> Unit = {},
  onRefreshMeta: () -> Unit = {},
  onSelectLane: (Lane?) -> Unit,
  onSelectSortMode: (TopMetaSortMode) -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onOpenAnalyzer: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isMiniMode = LocalInGameMiniMode.current
  var expandedChampionId by remember { mutableStateOf<String?>(null) }
  var serverMenuExpanded by remember { mutableStateOf(false) }

  Surface(
    shape = MaterialTheme.shapes.large,
    color = Color.Transparent,
    tonalElevation = 4.dp,
    border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.42f)),
    modifier =
      modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF121E36), HextechNavy, HextechObsidian)
          )
        )
        .testTag("top_played_meta_widget"),
  ) {
    Column(
      modifier = Modifier.padding(if (isMiniMode) 10.dp else 14.dp),
      verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
    ) {
      // Widget Header + Patch Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = HextechGold.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.55f)),
            modifier = Modifier.size(if (isMiniMode) 30.dp else 34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = null,
                tint = HextechGoldBright,
                modifier = Modifier.size(if (isMiniMode) 16.dp else 18.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (language == AppLanguage.TH) "Top 5 เมต้า & Tier List"
                else "Top 5 Ranked Meta",
              style =
                if (isMiniMode) MaterialTheme.typography.titleSmall
                else MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = HextechGoldBright,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (language == AppLanguage.TH) "สถิติ Win Rate • Pick • Ban แรงก์"
                else "Win Rate • Tier • Pick/Ban",
              style = MaterialTheme.typography.labelSmall,
              color = TextMutedLight,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Spacer(modifier = Modifier.width(6.dp))
        PatchBadgePill(
          patchVersion = patchVersion,
          sourceLabel = selectedServer.shortLabel,
        )
      }

      // Per-Server Live Data Selector + Refresh Control
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(modifier = Modifier.weight(1f)) {
            Surface(
              onClick = { serverMenuExpanded = true },
              shape = RoundedCornerShape(10.dp),
              color = HextechSurfaceHigh,
              border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("meta_server_selector_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Public,
                  contentDescription = null,
                  tint = ArcaneCyan,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text =
                    if (language == AppLanguage.TH) selectedServer.labelTh
                    else selectedServer.labelEn,
                  style = MaterialTheme.typography.labelMedium,
                  color = ArcaneCyan,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Select Server",
                  tint = ArcaneCyan,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
            DropdownMenu(
              expanded = serverMenuExpanded,
              onDismissRequest = { serverMenuExpanded = false },
            ) {
              MetaServerRegion.entries.forEach { server ->
                val isSelected = server == selectedServer
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (language == AppLanguage.TH) server.labelTh else server.labelEn,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) ArcaneCyan else MaterialTheme.colorScheme.onSurface,
                    )
                  },
                  trailingIcon =
                    if (isSelected) {
                      {
                        Icon(
                          Icons.Default.Check,
                          contentDescription = null,
                          tint = ArcaneCyan,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  onClick = {
                    onSelectServer(server)
                    serverMenuExpanded = false
                  },
                  modifier = Modifier.testTag("meta_server_option_${server.name}"),
                )
              }
            }
          }

          val isRefreshing = metaRefreshState is MetaRefreshState.Refreshing
          OutlinedButton(
            onClick = onRefreshMeta,
            enabled = !isRefreshing,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("meta_refresh_button"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AdvantageEmerald,
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = if (language == AppLanguage.TH) "รีเฟรชข้อมูลเมต้า" else "Refresh Meta Stats",
                tint = AdvantageEmerald,
                modifier = Modifier.size(16.dp),
              )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text =
                when {
                  isRefreshing && language == AppLanguage.TH -> "กำลังซิงค์"
                  isRefreshing -> "Syncing"
                  language == AppLanguage.TH -> "รีเฟรช"
                  else -> "Refresh"
                },
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = AdvantageEmerald,
              maxLines = 1,
            )
          }
        }

        val statusInfo: Pair<String, Color> =
          when (metaRefreshState) {
            is MetaRefreshState.Refreshing ->
              (if (language == AppLanguage.TH) "กำลังดึงสถิติ ${metaRefreshState.server.shortLabel} (Patch $patchVersion)..."
              else "Syncing ${metaRefreshState.server.shortLabel} ranked stats (Patch $patchVersion)...") to ArcaneCyan
            is MetaRefreshState.Synced -> {
              val timeStr =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(metaRefreshState.syncedAtMs))
              "${metaRefreshState.note} • Patch ${metaRefreshState.patchVersion} ($timeStr)" to AdvantageEmerald
            }
            is MetaRefreshState.Failed ->
              metaRefreshState.message to ThreatCrimson
            MetaRefreshState.Idle -> {
              if (lastSyncedMs > 0L) {
                val timeStr =
                  SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(lastSyncedMs))
                (if (language == AppLanguage.TH) "ซิงค์ล่าสุด $timeStr • ${selectedServer.shortLabel} • Patch $patchVersion"
                else "Synced $timeStr • ${selectedServer.shortLabel} • Patch $patchVersion") to TextMutedLight
              } else {
                (if (language == AppLanguage.TH) "ฐานข้อมูล Patch $patchVersion • กดรีเฟรชเพื่อซิงค์สถิติ ${selectedServer.shortLabel}"
                else "Patch $patchVersion matrix • Tap Refresh to sync ${selectedServer.shortLabel}") to TextMutedLight
              }
            }
          }
        Text(
          text = statusInfo.first,
          style = MaterialTheme.typography.labelSmall,
          color = statusInfo.second,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 2.dp).testTag("meta_refresh_status"),
        )
      }

      // Metric Mode Pill Bar + Aggregate Stats Summary
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        TopMetaSortMode.entries.forEach { mode ->
          val selected = selectedSortMode == mode
          FilterChip(
            selected = selected,
            onClick = { onSelectSortMode(mode) },
            modifier = Modifier.testTag("top_meta_sort_${mode.name}"),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = HextechGold.copy(alpha = 0.22f),
                selectedLabelColor = HextechGoldBright,
              ),
            label = {
              Text(
                text = if (language == AppLanguage.TH) mode.labelTh else mode.labelEn,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
              )
            },
          )
        }
      }

      if (entries.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          MetricSummaryMiniPill(
            label = if (language == AppLanguage.TH) "Top 5 เฉลี่ย WR" else "Top 5 Avg WR",
            value = "${"%.1f".format(avgWinRatePct)}%",
            accent = AdvantageEmerald,
            modifier = Modifier.weight(1f),
          )
          MetricSummaryMiniPill(
            label = if (language == AppLanguage.TH) "เฉลี่ย Pick Rate" else "Top 5 Avg PR",
            value = "${"%.1f".format(avgPickRatePct)}%",
            accent = ArcaneCyan,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Lane Filter Rail (synchronized with Home Meta Tier Lane filter)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
          FilterChip(
            selected = selectedLane == null,
            onClick = { onSelectLane(null) },
            modifier = Modifier.testTag("top_meta_lane_ALL"),
            label = {
              Text(
                text = if (language == AppLanguage.TH) "ทุกเลน" else "All",
                style = MaterialTheme.typography.labelMedium,
              )
            },
          )
        }
        items(Lane.entries, key = { "top_lane_${it.name}" }) { lane ->
          FilterChip(
            selected = selectedLane == lane,
            onClick = { onSelectLane(if (selectedLane == lane) null else lane) },
            modifier = Modifier.testTag("top_meta_lane_${lane.name}"),
            label = {
              Text(
                text = if (language == AppLanguage.TH) lane.labelTh else lane.labelEn,
                style = MaterialTheme.typography.labelMedium,
              )
            },
          )
        }
      }

      if (entries.isEmpty()) {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = HextechSurface.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, HextechOutline),
          modifier = Modifier.fillMaxWidth().testTag("top_meta_empty_state"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = HextechGold,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text =
                if (language == AppLanguage.TH) "ไม่พบข้อมูล Ranked Meta ในเลนนี้"
                else "No Ranked Meta data matched this lane filter",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
              onClick = { onSelectLane(null) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Text(if (language == AppLanguage.TH) "แสดงทุกเลน" else "Show All Lanes")
            }
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          entries.forEach { entry ->
            val isExpanded = expandedChampionId == entry.champion.id
            TopMetaChampionRow(
              entry = entry,
              isExpanded = isExpanded,
              isMiniMode = isMiniMode,
              language = language,
              onToggleExpand = {
                expandedChampionId = if (isExpanded) null else entry.champion.id
              },
              onOpenKit = { onOpenChampionDetail(entry.champion.id) },
              onOpenAnalyzer = { onOpenAnalyzer(entry.champion.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricSummaryMiniPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = accent.copy(alpha = 0.1f),
    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = accent,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopMetaChampionRow(
  entry: TopMetaChampionEntry,
  isExpanded: Boolean,
  isMiniMode: Boolean,
  language: AppLanguage,
  onToggleExpand: () -> Unit,
  onOpenKit: () -> Unit,
  onOpenAnalyzer: () -> Unit,
) {
  val champ = entry.champion
  val rankColor =
    when (entry.rank) {
      1 -> HextechGoldBright
      2 -> ArcaneCyan
      3 -> SkillViolet
      else -> TextMutedLight
    }
  val wrColor =
    when {
      champ.winRatePct >= 52.0 -> AdvantageEmerald
      champ.winRatePct >= 50.0 -> HextechGoldBright
      else -> ThreatCrimson
    }

  Surface(
    onClick = onToggleExpand,
    shape = MaterialTheme.shapes.medium,
    color = if (entry.rank == 1) Color(0xFF17243D) else HextechSurface,
    border =
      BorderStroke(
        width = if (entry.rank == 1) 1.25.dp else 0.75.dp,
        color = if (entry.rank == 1) HextechGold.copy(alpha = 0.65f) else HextechOutline,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .testTag("top_meta_row_${champ.id}"),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Rank Pill (#1..#5)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = rankColor.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, rankColor.copy(alpha = 0.55f)),
          modifier = Modifier.width(34.dp),
        ) {
          Box(
            modifier = Modifier.padding(vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "#${entry.rank}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = rankColor,
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))

        ChampionCrestAvatar(
          champion = champ,
          size = if (isMiniMode) 34.dp else 40.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = champ.displayName(language),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            TierBadge(champ.tierRank)
          }
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            DamageTypeBadge(champ.damageType, language)
            Text(
              text =
                champ.lanes.joinToString("/") {
                  if (language == AppLanguage.TH) it.labelTh else it.labelEn
                },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (entry.personalGames > 0 && entry.personalWinRatePct != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = AdvantageEmerald.copy(alpha = 0.14f),
              modifier = Modifier.testTag("top_meta_personal_${champ.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = AdvantageEmerald,
                  modifier = Modifier.size(11.dp),
                )
                Text(
                  text =
                    if (language == AppLanguage.TH) {
                      "คุณเล่น ${entry.personalGames} เกม (${entry.personalWinRatePct}% WR)"
                    } else {
                      "You: ${entry.personalGames}G • ${entry.personalWinRatePct}% WR"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = AdvantageEmerald,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          OutlinedButton(
            onClick = onOpenAnalyzer,
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("top_meta_analyze_${champ.id}"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Text(
              text = "1v5",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = HextechGoldBright,
            )
          }
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse meta row" else "Expand meta row",
            tint = TextMutedLight,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(7.dp))

      // Proportional WinRate + PickRate + BanRate telemetry bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "WR ${"%.1f".format(champ.winRatePct)}%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = wrColor,
            )
            Text(
              text = "PR ${"%.1f".format(champ.pickRatePct)}%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = ArcaneCyan,
            )
            Text(
              text = "BR ${"%.1f".format(champ.banRatePct)}%",
              style = MaterialTheme.typography.labelSmall,
              color = if (champ.banRatePct >= 15.0) ThreatCrimson else WarningAmber,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val wrProgress = ((champ.winRatePct - 42.0) / 16.0).toFloat().coerceIn(0.08f, 1f)
            val prProgress = (champ.pickRatePct / 28.0).toFloat().coerceIn(0.08f, 1f)
            LinearProgressIndicator(
              progress = { wrProgress },
              color = wrColor,
              trackColor = wrColor.copy(alpha = 0.16f),
              modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)),
            )
            LinearProgressIndicator(
              progress = { prProgress },
              color = ArcaneCyan,
              trackColor = ArcaneCyan.copy(alpha = 0.16f),
              modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)),
            )
          }
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = 8.dp)
              .testTag("top_meta_expand_${champ.id}"),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          HorizontalDivider(color = HextechOutline.copy(alpha = 0.6f))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            PhaseScoreChip(
              label = if (language == AppLanguage.TH) "ต้นเกม" else "Early",
              score = champ.earlyPower,
            )
            PhaseScoreChip(
              label = if (language == AppLanguage.TH) "กลางเกม" else "Mid",
              score = champ.midPower,
            )
            PhaseScoreChip(
              label = if (language == AppLanguage.TH) "ท้ายเกม" else "Late",
              score = champ.latePower,
            )
            PhaseScoreChip(
              label = if (language == AppLanguage.TH) "CC" else "CC",
              score = champ.ccRating,
            )
          }
          champ.strengths(language).firstOrNull()?.let { strength ->
            Text(
              text = "• $strength",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "ความยาก ${champ.difficulty}/3 • แพตช์ ${champ.patchVersion}"
                } else {
                  "Difficulty ${champ.difficulty}/3 • Patch ${champ.patchVersion}"
                },
              style = MaterialTheme.typography.labelSmall,
              color = TextMutedLight,
            )
            OutlinedButton(
              onClick = onOpenKit,
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .testTag("top_meta_kit_${champ.id}"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Text(
                text = if (language == AppLanguage.TH) "ดูสกิล & บิลด์" else "View Kit",
                style = MaterialTheme.typography.labelMedium,
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PhaseScoreChip(label: String, score: Int) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = HextechSurfaceHigh,
    border = BorderStroke(0.5.dp, HextechOutline),
  ) {
    Text(
      text = "$label $score/10",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}
