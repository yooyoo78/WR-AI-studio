package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionRole
import com.example.data.model.Lane
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.TierBadge
import com.example.ui.components.WinRatePillsRow
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.viewmodel.ChampionSortMode
import com.example.ui.viewmodel.ChampionsUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChampionsRosterTab(
  state: ChampionsUiState,
  language: AppLanguage,
  onSearchChange: (String) -> Unit,
  onSelectLane: (Lane?) -> Unit,
  onSelectRole: (ChampionRole?) -> Unit,
  onToggleOnlyFavorites: (Boolean) -> Unit,
  onSortModeChange: (ChampionSortMode) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenDetail: (String) -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  var showFilterSheet by remember { mutableStateOf(false) }

  val activeFilterCount =
    (if (state.selectedLane != null) 1 else 0) +
      (if (state.selectedRole != null) 1 else 0) +
      (if (state.onlyFavorites) 1 else 0)

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("champions_lazy_column"),
    contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 8.dp),
  ) {
    item {
      OutlinedTextField(
        value = state.searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().testTag("champions_search_input"),
        placeholder = { Text(strings.searchChampionHint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LazyRow(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            FilterChip(
              selected = state.sortMode == ChampionSortMode.TIER,
              onClick = { onSortModeChange(ChampionSortMode.TIER) },
              modifier = Modifier.testTag("sort_tier_chip"),
              label = { Text(if (language == AppLanguage.TH) "Tier" else "Sort: Tier") },
            )
          }
          item {
            FilterChip(
              selected = state.sortMode == ChampionSortMode.WIN_RATE,
              onClick = { onSortModeChange(ChampionSortMode.WIN_RATE) },
              modifier = Modifier.testTag("sort_winrate_chip"),
              label = { Text(if (language == AppLanguage.TH) "Winrate" else "Sort: WR") },
            )
          }
          item {
            FilterChip(
              selected = activeFilterCount > 0,
              onClick = { showFilterSheet = true },
              modifier = Modifier.testTag("champions_filter_button"),
              leadingIcon = {
                Icon(
                  Icons.Default.FilterList,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = {
                val labelBase = if (language == AppLanguage.TH) "ตัวกรอง" else "Filter"
                Text(if (activeFilterCount > 0) "$labelBase ($activeFilterCount)" else labelBase)
              },
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          color = HextechGold.copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("champions_patch_badge"),
        ) {
          Text(
            text = "Patch ${state.activePatchVersion}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = HextechGoldBright,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          )
        }
      }
    }

    if (state.filteredChampions.isEmpty()) {
      item {
        HextechCard {
          Text(
            text =
              if (language == AppLanguage.TH) "ไม่พบแชมเปี้ยนที่ตรงกับเงื่อนไข"
              else "No champions match the selected filters.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    } else {
      items(state.filteredChampions, key = { it.id }) { champ ->
        val isFav = champ.id in state.favoriteIds
        HextechCard(
          modifier = Modifier.fillMaxWidth().testTag("champ_card_${champ.id}"),
          onClick = { onOpenDetail(champ.id) },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            ChampionCrestAvatar(champion = champ, size = 40.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = champ.displayName(language),
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                TierBadge(champ.tierRank)
              }
              Spacer(modifier = Modifier.height(2.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                DamageTypeBadge(champ.damageType, language)
                Text(
                  text =
                    champ.lanes.joinToString(" • ") {
                      if (language == AppLanguage.TH) it.labelTh else it.labelEn
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.height(5.dp))
              WinRatePillsRow(champion = champ, language = language)
            }
            IconButton(
              onClick = { onToggleFavorite(champ.id) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("fav_btn_${champ.id}"),
            ) {
              Icon(
                if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Toggle favorite",
                tint = if (isFav) HextechGold else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showFilterSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showFilterSheet = false },
      sheetState = sheetState,
      containerColor = HextechSurface,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .testTag("champions_filter_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (language == AppLanguage.TH) "กรองแชมเปี้ยน" else "Filter Champions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HextechGoldBright,
          )
          if (activeFilterCount > 0) {
            TextButton(
              onClick = {
                onSelectLane(null)
                onSelectRole(null)
                onToggleOnlyFavorites(false)
              },
            ) {
              Text(if (language == AppLanguage.TH) "ล้างทั้งหมด" else "Reset", color = ArcaneCyan)
            }
          }
        }

        FilterChip(
          selected = state.onlyFavorites,
          onClick = { onToggleOnlyFavorites(!state.onlyFavorites) },
          leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
          label = { Text(if (language == AppLanguage.TH) "เฉพาะตัวโปรด (Favorites)" else "Favorites Only") },
        )

        Text(
          text = if (language == AppLanguage.TH) "ตำแหน่งเลน (Lane)" else "Lane",
          style = MaterialTheme.typography.labelLarge,
          color = HextechGold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Lane.entries.forEach { lane ->
            FilterChip(
              selected = state.selectedLane == lane,
              onClick = { onSelectLane(if (state.selectedLane == lane) null else lane) },
              label = { Text(if (language == AppLanguage.TH) lane.labelTh else lane.labelEn) },
            )
          }
        }

        Text(
          text = if (language == AppLanguage.TH) "บทบาท (Role)" else "Role",
          style = MaterialTheme.typography.labelLarge,
          color = ArcaneCyan,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ChampionRole.entries.forEach { role ->
            FilterChip(
              selected = state.selectedRole == role,
              onClick = { onSelectRole(if (state.selectedRole == role) null else role) },
              label = { Text(if (language == AppLanguage.TH) role.labelTh else role.labelEn) },
            )
          }
        }

        Button(
          onClick = { showFilterSheet = false },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
        ) {
          Text(if (language == AppLanguage.TH) "ตกลง (${state.filteredChampions.size} ตัว)" else "Apply (${state.filteredChampions.size})")
        }
        Spacer(modifier = Modifier.height(10.dp))
      }
    }
  }
}
