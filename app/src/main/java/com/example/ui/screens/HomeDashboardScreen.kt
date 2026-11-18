package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.PatchBadgePill
import com.example.ui.components.TierBadge
import com.example.ui.components.WinRatePillsRow
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HomeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
  state: HomeUiState,
  onSelectTierLane: (Lane?) -> Unit,
  onOpenAnalyzer: (String?) -> Unit,
  onOpenChampionDetail: (String) -> Unit,
  onOpenDraft: () -> Unit,
  onOpenCoach: () -> Unit,
  onOpenPatchCenter: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val lang = state.preferences.language

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Tactical Command Banner
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = Color.Transparent,
          modifier =
            Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.large)
              .background(
                Brush.linearGradient(
                  listOf(Color(0xFF14213D), HextechSurfaceHigh, Color(0xFF1E293B))
                )
              ),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            PatchBadgePill(
              patchVersion = state.activePatch?.version ?: state.preferences.activePatchLabel,
              sourceLabel =
                state.activePatch?.releaseDate ?: "Curated Meta",
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = strings.appTitle,
              style = MaterialTheme.typography.displaySmall,
              color = HextechGoldBright,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.heroTagline,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))

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
              Icon(Icons.Default.Security, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.quickAnalyzeCta,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = onOpenDraft,
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("home_open_draft_button"),
              ) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.openDraftCta,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = onOpenCoach,
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("home_open_coach_button"),
              ) {
                Icon(
                  Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = ArcaneCyan,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = strings.openCoachCta,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // Quick Utility Shortcuts (Patch Center + Settings/Readability + DB Snapshot)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          HextechCard(
            modifier = Modifier.weight(1f).testTag("home_patch_card"),
            accentColor = ArcaneCyan,
            onClick = onOpenPatchCenter,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Update, contentDescription = null, tint = ArcaneCyan)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.patchCenterTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
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
              Icon(Icons.Default.Settings, contentDescription = null, tint = HextechGold)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text =
                "${state.preferences.language.displayLabel} • ${(state.preferences.fontScaleMultiplier * 100).toInt()}%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Favorites Quick Launch Strip
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = HextechGold)
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(state.favoriteChampions, key = { it.id }) { fav ->
                HextechCard(
                  modifier = Modifier.width(160.dp).testTag("home_fav_${fav.id}"),
                  accentColor = HextechGold,
                  onClick = { onOpenAnalyzer(fav.id) },
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    ChampionCrestAvatar(champion = fav, size = 38.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = fav.displayName(lang),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      TierBadge(fav.tierRank)
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Active Patch Highlights
      state.activePatch?.let { patch ->
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (lang == AppLanguage.TH) patch.headlineTh else patch.headlineEn,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = patch.version,
                style = MaterialTheme.typography.labelLarge,
                color = ArcaneCyan,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (lang == AppLanguage.TH) patch.summaryTh else patch.summaryEn,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            patch.changes.take(4).forEach { change ->
              val badgeColor =
                when (change.changeType) {
                  PatchChangeType.NEW -> HextechGold
                  PatchChangeType.BUFF -> AdvantageEmerald
                  PatchChangeType.NERF -> ThreatCrimson
                  PatchChangeType.ADJUST -> WarningAmber
                }
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Surface(
                  color = badgeColor.copy(alpha = 0.2f),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Text(
                    text = if (lang == AppLanguage.TH) change.changeType.labelTh else change.changeType.labelEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    "${change.targetName}: ${if (lang == AppLanguage.TH) change.detailTh else change.detailEn}",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // Meta Tier Snapshot by Lane
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.metaHighlightsTitle,
            style = MaterialTheme.typography.titleMedium,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            FilterChip(
              selected = state.selectedTierLane == null,
              onClick = { onSelectTierLane(null) },
              label = { Text(if (lang == AppLanguage.TH) "ทุกเลน" else "All") },
            )
            Lane.entries.forEach { lane ->
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
      ChampionCrestAvatar(champion = champion, size = 46.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = champion.displayName(language),
            style = MaterialTheme.typography.titleMedium,
          )
          TierBadge(champion.tierRank)
        }
        Spacer(modifier = Modifier.height(3.dp))
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
          text = if (language == AppLanguage.TH) "วิเคราะห์" else "1v5",
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}
