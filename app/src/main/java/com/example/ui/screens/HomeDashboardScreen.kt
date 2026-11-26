package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.remote.MetaRefreshState
import com.example.data.remote.MetaServerRegion
import com.example.data.remote.OtaCheckStatus
import com.example.domain.engine.TopMetaSortMode
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.TierBadge
import com.example.ui.components.TopPlayedMetaWidget
import com.example.ui.components.WinRatePillsRow
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechOutline
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.TextMutedLight
import com.example.ui.viewmodel.HomeUiState

@Composable
fun HomeDashboardScreen(
  state: HomeUiState,
  otaStatus: OtaCheckStatus = OtaCheckStatus.Idle,
  metaRefreshState: MetaRefreshState = MetaRefreshState.Idle,
  onSelectTierLane: (Lane?) -> Unit,
  onSelectMetaSortMode: (TopMetaSortMode) -> Unit = {},
  onSelectMetaServer: (MetaServerRegion) -> Unit = {},
  onRefreshMeta: () -> Unit = {},
  onOpenAnalyzer: (String?) -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onOpenChampions: () -> Unit = {},
  onOpenDraft: () -> Unit,
  onOpenCoach: () -> Unit,
  onOpenQuickScan: () -> Unit = {},
  onOpenPatchCenter: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = state.preferences.language
  val activePatchVersion =
    state.preferences.activePatchLabel.ifBlank { state.activePatch?.version ?: "7.3f" }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("home_lazy_column"),
      contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
      verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
    ) {
      // 1. Top Quick Shortcuts Row (1-tap navigation to core tools without crowding screen)
      item {
        LazyRow(
          modifier = Modifier.fillMaxWidth().testTag("home_shortcuts_row"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            QuickShortcutTile(
              icon = Icons.Default.Security,
              label = if (lang == AppLanguage.TH) "วิเคราะห์ 1v5" else "1v5 Build",
              accent = HextechGoldBright,
              filled = true,
              onClick = { onOpenAnalyzer(null) },
              modifier = Modifier.testTag("home_launch_analyzer_button"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.Groups,
              label = if (lang == AppLanguage.TH) "ดราฟต์" else "Draft",
              accent = ArcaneCyan,
              onClick = onOpenDraft,
              modifier = Modifier.testTag("home_open_draft_button"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.AutoAwesome,
              label = if (lang == AppLanguage.TH) "AI โค้ช" else "Coach",
              accent = ArcaneCyan,
              onClick = onOpenCoach,
              modifier = Modifier.testTag("home_open_coach_button"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.CameraAlt,
              label = if (lang == AppLanguage.TH) "สแกนจอ" else "Scan",
              accent = AdvantageEmerald,
              onClick = onOpenQuickScan,
              modifier = Modifier.testTag("home_quick_scan_button"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.Shield,
              label = if (lang == AppLanguage.TH) "แชมเปี้ยน" else "Champs",
              accent = HextechGold,
              onClick = onOpenChampions,
              modifier = Modifier.testTag("home_open_champions_shortcut"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.Update,
              label = if (lang == AppLanguage.TH) "แพตช์" else "Patch",
              accent = ArcaneCyan,
              onClick = onOpenPatchCenter,
              modifier = Modifier.testTag("home_patch_card"),
            )
          }
          item {
            QuickShortcutTile(
              icon = Icons.Default.Settings,
              label = if (lang == AppLanguage.TH) "ตั้งค่า" else "Settings",
              accent = HextechGold,
              onClick = onOpenSettings,
              modifier = Modifier.testTag("home_settings_card"),
            )
          }
        }
      }

      // 2. Slim Single-Line Active Patch Version Pill (Tap to view patch details in Patch Center)
      item {
        val hasOtaUpdate = otaStatus is OtaCheckStatus.UpdateReady
        val pillAccent = if (hasOtaUpdate) AdvantageEmerald else ArcaneCyan
        Surface(
          onClick = onOpenPatchCenter,
          shape = MaterialTheme.shapes.medium,
          color = HextechSurfaceHigh,
          border = BorderStroke(1.dp, pillAccent.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_active_patch_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = HextechGold.copy(alpha = 0.16f),
                border = BorderStroke(0.75.dp, HextechGold.copy(alpha = 0.55f)),
              ) {
                Text(
                  text = "Patch $activePatchVersion",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = HextechGoldBright,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
              Text(
                text =
                  when {
                    hasOtaUpdate && lang == AppLanguage.TH ->
                      "พบ OTA Patch ${(otaStatus as OtaCheckStatus.UpdateReady).download.diff.incomingPatchVersion} • แตะดูรายละเอียด"
                    hasOtaUpdate ->
                      "OTA Patch ${(otaStatus as OtaCheckStatus.UpdateReady).download.diff.incomingPatchVersion} Ready • Tap to view"
                    lang == AppLanguage.TH -> "แตะเพื่อดูรายละเอียดแพตช์ & อัปเดต"
                    else -> "Tap for Patch Notes & OTA Details"
                  },
                style = MaterialTheme.typography.labelMedium,
                color = if (hasOtaUpdate) AdvantageEmerald else TextMutedLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = strings.patchCenterTitle,
              tint = pillAccent,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      // 3. Compact Favorites Quick Strip (only bulky if populated; single-line pill when empty)
      if (state.favoriteChampions.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.Star, contentDescription = null, tint = HextechGold, modifier = Modifier.size(16.dp))
              Text(text = strings.favoritesTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(state.favoriteChampions, key = { it.id }) { fav ->
                HextechCard(
                  modifier = Modifier.width(166.dp).testTag("home_fav_${fav.id}"),
                  accentColor = HextechGold,
                  onClick = { onOpenChampionDetail(fav.id) },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    ChampionCrestAvatar(champion = fav, size = 34.dp)
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = fav.displayName(lang),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      TierBadge(fav.tierRank)
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = HextechGold.copy(alpha = 0.14f),
                      border = BorderStroke(0.75.dp, HextechGold.copy(alpha = 0.45f)),
                      modifier =
                        Modifier.minimumInteractiveComponentSize()
                          .clip(RoundedCornerShape(8.dp))
                          .clickable { onOpenAnalyzer(fav.id) }
                          .testTag("home_fav_analyze_${fav.id}"),
                    ) {
                      Text(
                        text = "1v5",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HextechGoldBright,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 4. Top 5 Ranked Meta & Win Rate Widget (with Per-Server Selector + Refresh)
      item {
        TopPlayedMetaWidget(
          entries = state.topPlayedMeta,
          selectedLane = state.selectedTierLane,
          selectedSortMode = state.selectedMetaSortMode,
          avgWinRatePct = state.topMetaAvgWinRatePct,
          avgPickRatePct = state.topMetaAvgPickRatePct,
          patchVersion = activePatchVersion,
          language = lang,
          selectedServer = state.selectedMetaServer,
          metaRefreshState = metaRefreshState,
          lastSyncedMs = state.lastMetaSyncMs,
          onSelectServer = onSelectMetaServer,
          onRefreshMeta = onRefreshMeta,
          onSelectLane = onSelectTierLane,
          onSelectSortMode = onSelectMetaSortMode,
          onOpenChampionDetail = onOpenChampionDetail,
          onOpenAnalyzer = { champId -> onOpenAnalyzer(champId) },
        )
      }

      // 5. Tier List & Win Rate Snapshot by Lane
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = strings.metaHighlightsTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = onRefreshMeta,
              enabled = metaRefreshState !is MetaRefreshState.Refreshing,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("tierlist_refresh_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = if (lang == AppLanguage.TH) "รีเฟรช Tier List" else "Refresh Tier List",
                tint = AdvantageEmerald,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = state.selectedTierLane == null,
                onClick = { onSelectTierLane(null) },
                label = { Text(if (lang == AppLanguage.TH) "ทุกเลน" else "All") },
              )
            }
            items(Lane.entries, key = { it.name }) { lane ->
              FilterChip(
                selected = state.selectedTierLane == lane,
                onClick = {
                  onSelectTierLane(if (state.selectedTierLane == lane) null else lane)
                },
                label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
              )
            }
          }
        }
      }

      items(state.topMetaChampions.take(8), key = { it.id }) { champ ->
        MetaChampionRowCard(
          champion = champ,
          language = lang,
          onOpenProfile = { onOpenChampionDetail(champ.id) },
          onAnalyzeClick = { onOpenAnalyzer(champ.id) },
        )
      }
    }
  }
}

@Composable
private fun QuickShortcutTile(
  icon: ImageVector,
  label: String,
  accent: Color,
  filled: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = if (filled) HextechGold.copy(alpha = 0.18f) else HextechSurface,
    border =
      BorderStroke(
        width = if (filled) 1.2.dp else 0.9.dp,
        color = if (filled) HextechGold else HextechOutline,
      ),
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (filled) HextechGoldBright else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun MetaChampionRowCard(
  champion: ChampionData,
  language: AppLanguage,
  onOpenProfile: () -> Unit,
  onAnalyzeClick: () -> Unit,
) {
  HextechCard(
    modifier = Modifier.fillMaxWidth().testTag("meta_champ_${champion.id}"),
    onClick = onOpenProfile,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ChampionCrestAvatar(champion = champion, size = 38.dp)
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = champion.displayName(language),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          TierBadge(champion.tierRank)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DamageTypeBadge(champion.damageType, language)
          Text(
            text =
              champion.lanes.joinToString("/") {
                if (language == AppLanguage.TH) it.labelTh else it.labelEn
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.height(5.dp))
        WinRatePillsRow(champion = champion, language = language)
      }
      OutlinedButton(
        onClick = onAnalyzeClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_analyze_${champion.id}"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Text(
          text = "1v5",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
