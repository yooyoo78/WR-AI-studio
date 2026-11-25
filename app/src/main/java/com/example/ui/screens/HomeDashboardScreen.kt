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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.PatchChangeType
import com.example.data.remote.OtaCheckStatus
import com.example.domain.engine.TopMetaSortMode
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.PatchBadgePill
import com.example.ui.components.TierBadge
import com.example.ui.components.TopPlayedMetaWidget
import com.example.ui.components.WinRatePillsRow
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HomeUiState

@Composable
fun HomeDashboardScreen(
  state: HomeUiState,
  otaStatus: OtaCheckStatus = OtaCheckStatus.Idle,
  onSelectTierLane: (Lane?) -> Unit,
  onSelectMetaSortMode: (TopMetaSortMode) -> Unit = {},
  onOpenAnalyzer: (String?) -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onOpenDraft: () -> Unit,
  onOpenCoach: () -> Unit,
  onOpenQuickScan: () -> Unit = {},
  onOpenPatchCenter: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = state.preferences.language
  var patchExpanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("home_lazy_column"),
      contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
      verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
    ) {
      // Hero Tactical Command Banner (slim quick-bar in Mini-Window Mode)
      item {
        if (isMiniMode) {
          HextechCard(accentColor = HextechGold) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Button(
                onClick = { onOpenAnalyzer(null) },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("home_launch_analyzer_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = HextechGold,
                    contentColor = Color(0xFF0A0F1A),
                  ),
              ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.navAnalyzer,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = onOpenDraft,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("home_open_draft_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(Icons.Default.Groups, contentDescription = strings.openDraftCta, modifier = Modifier.size(16.dp))
              }
              OutlinedButton(
                onClick = onOpenCoach,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("home_open_coach_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(
                  Icons.Default.AutoAwesome,
                  contentDescription = strings.openCoachCta,
                  tint = ArcaneCyan,
                  modifier = Modifier.size(16.dp),
                )
              }
              OutlinedButton(
                onClick = onOpenQuickScan,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("home_quick_scan_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(
                  Icons.Default.CameraAlt,
                  contentDescription = strings.quickScanCta,
                  tint = AdvantageEmerald,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        } else {
          Surface(
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent,
            border = BorderStroke(0.75.dp, HextechGold.copy(alpha = 0.25f)),
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                  Brush.linearGradient(
                    listOf(Color(0xFF101B30), HextechSurfaceHigh, Color(0xFF142138))
                  )
                ),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = HextechGoldBright,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = strings.heroTagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                PatchBadgePill(
                  patchVersion = state.activePatch?.version ?: state.preferences.activePatchLabel,
                  sourceLabel = state.activePatch?.releaseDate ?: "Meta",
                )
              }

              Button(
                onClick = { onOpenAnalyzer(null) },
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("home_launch_analyzer_button"),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = HextechGold,
                    contentColor = Color(0xFF0A0F1A),
                  ),
              ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = strings.quickAnalyzeCta,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onOpenDraft,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("home_open_draft_button"),
                ) {
                  Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = strings.openDraftCta,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                OutlinedButton(
                  onClick = onOpenCoach,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("home_open_coach_button"),
                ) {
                  Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ArcaneCyan,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = strings.openCoachCta,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                OutlinedButton(
                  onClick = onOpenQuickScan,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("home_quick_scan_button"),
                ) {
                  Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = AdvantageEmerald,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (lang == AppLanguage.TH) "สแกนจอ" else "Scan",
                    style = MaterialTheme.typography.labelMedium,
                    color = AdvantageEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      if (otaStatus is OtaCheckStatus.UpdateReady) {
        item {
          val diff = otaStatus.download.diff
          HextechCard(
            accentColor = AdvantageEmerald,
            onClick = onOpenPatchCenter,
            modifier = Modifier.fillMaxWidth().testTag("home_ota_update_banner"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (lang == AppLanguage.TH) {
                      "พบ OTA Patch ${diff.incomingPatchVersion} (ยืนยันลายเซ็นแล้ว ✓)"
                    } else {
                      "Signed OTA Patch ${diff.incomingPatchVersion} Ready ✓"
                    },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = AdvantageEmerald,
                )
                Text(
                  text =
                    if (lang == AppLanguage.TH) "แตะเพื่อดู Diff และอัปเดตใน Patch Center"
                    else "Tap to inspect diff & apply in Patch Center",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open Patch Center",
                tint = AdvantageEmerald,
              )
            }
          }
        }
      }

      // Quick Utility Shortcuts (hidden in Mini-Window Mode)
      if (!isMiniMode) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            HextechCard(
              modifier = Modifier.weight(1f).testTag("home_patch_card"),
              accentColor = ArcaneCyan,
              onClick = onOpenPatchCenter,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Update, contentDescription = null, tint = ArcaneCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.patchCenterTitle,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${state.totalChampions} Champs • ${state.totalItems} Items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            HextechCard(
              modifier = Modifier.weight(1f).testTag("home_settings_card"),
              accentColor = HextechGold,
              onClick = onOpenSettings,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = HextechGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.settingsTitle,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text =
                  "${state.preferences.language.displayLabel} • ${(state.preferences.fontScaleMultiplier * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Favorites Quick Launch Strip (tapping portrait/card opens Champion Profile)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = HextechGold, modifier = Modifier.size(18.dp))
            Text(text = strings.favoritesTitle, style = MaterialTheme.typography.titleMedium)
          }

          if (state.favoriteChampions.isEmpty()) {
            HextechCard {
              Text(
                text = strings.noFavoritesHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(state.favoriteChampions, key = { it.id }) { fav ->
                HextechCard(
                  modifier = Modifier.width(172.dp).testTag("home_fav_${fav.id}"),
                  accentColor = HextechGold,
                  onClick = { onOpenChampionDetail(fav.id) },
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    ChampionCrestAvatar(champion = fav, size = 36.dp)
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = fav.displayName(lang),
                        style = MaterialTheme.typography.titleSmall,
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

      // Top 5 Ranked Played Meta & Win Rate Widget
      item {
        TopPlayedMetaWidget(
          entries = state.topPlayedMeta,
          selectedLane = state.selectedTierLane,
          selectedSortMode = state.selectedMetaSortMode,
          avgWinRatePct = state.topMetaAvgWinRatePct,
          avgPickRatePct = state.topMetaAvgPickRatePct,
          patchVersion = state.activePatch?.version ?: state.preferences.activePatchLabel,
          language = lang,
          onSelectLane = onSelectTierLane,
          onSelectSortMode = onSelectMetaSortMode,
          onOpenChampionDetail = onOpenChampionDetail,
          onOpenAnalyzer = { champId -> onOpenAnalyzer(champId) },
        )
      }

      // Active Patch Highlights (collapsible for compact breathing room)
      state.activePatch?.let { patch ->
        item {
          HextechCard(
            accentColor = ArcaneCyan,
            onClick = { patchExpanded = !patchExpanded },
            modifier = Modifier.testTag("home_patch_highlights_card"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (lang == AppLanguage.TH) patch.headlineTh else patch.headlineEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = patch.version,
                  style = MaterialTheme.typography.labelMedium,
                  color = ArcaneCyan,
                  fontWeight = FontWeight.Bold,
                )
                Icon(
                  imageVector = if (patchExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = if (patchExpanded) "Collapse patch" else "Expand patch",
                  tint = ArcaneCyan,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (lang == AppLanguage.TH) patch.summaryTh else patch.summaryEn,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = if (patchExpanded) 4 else 2,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            patch.changes.take(2).forEach { change ->
              PatchChangeMiniRow(targetName = change.targetName, change = change, lang = lang)
            }
            AnimatedVisibility(visible = patchExpanded && patch.changes.size > 2) {
              Column {
                patch.changes.drop(2).take(4).forEach { change ->
                  PatchChangeMiniRow(targetName = change.targetName, change = change, lang = lang)
                }
              }
            }
          }
        }
      }

      // Meta Tier Snapshot by Lane (clean single-line horizontal chip rail)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = strings.metaHighlightsTitle,
            style = MaterialTheme.typography.titleMedium,
          )
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
private fun PatchChangeMiniRow(
  targetName: String,
  change: com.example.data.model.PatchChangeEntry,
  lang: AppLanguage,
) {
  val badgeColor =
    when (change.changeType) {
      PatchChangeType.NEW -> HextechGold
      PatchChangeType.BUFF -> AdvantageEmerald
      PatchChangeType.NERF -> ThreatCrimson
      PatchChangeType.ADJUST -> WarningAmber
    }
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Surface(
      color = badgeColor.copy(alpha = 0.16f),
      shape = MaterialTheme.shapes.small,
    ) {
      Text(
        text = if (lang == AppLanguage.TH) change.changeType.labelTh else change.changeType.labelEn,
        style = MaterialTheme.typography.labelSmall,
        color = badgeColor,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "$targetName: ${if (lang == AppLanguage.TH) change.detailTh else change.detailEn}",
      style = MaterialTheme.typography.bodySmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
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
      ChampionCrestAvatar(champion = champion, size = 40.dp)
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
          text = if (language == AppLanguage.TH) "1v5" else "1v5",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
