package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemCategory
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.remote.DDragonIconResolver
import com.example.ui.components.AbilityIconBadge
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.CustomBuildEditorSheet
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemDetailBottomSheet
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.PatchBadgePill
import com.example.ui.components.RuneDetailBottomSheet
import com.example.ui.components.RuneIconSlot
import com.example.ui.components.SpellDetailBottomSheet
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.TacticalMeterBar
import com.example.ui.components.TierBadge
import com.example.ui.components.WinRatePillsRow
import com.example.ui.components.buildDefaultCustomDraft
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.ChampionSortMode
import com.example.ui.viewmodel.ChampionsUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChampionsScreen(
  state: ChampionsUiState,
  onSearchChange: (String) -> Unit,
  onSelectLane: (Lane?) -> Unit,
  onSelectRole: (ChampionRole?) -> Unit,
  onToggleOnlyFavorites: (Boolean) -> Unit,
  onSortModeChange: (ChampionSortMode) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onSaveCustomBuild: (CustomBuildData) -> Unit,
  onDeleteCustomBuild: (Long) -> Unit,
  onOpenDetail: (String) -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = LocalUserPrefs.current.language

  var selectedCatalogTab by remember { mutableIntStateOf(0) }
  var itemSearchQuery by remember { mutableStateOf("") }
  var selectedItemCategory by remember { mutableStateOf<ItemCategory?>(null) }
  var runeSearchQuery by remember { mutableStateOf("") }
  var selectedRuneTree by remember { mutableStateOf<String?>(null) }
  var inspectedItem by remember { mutableStateOf<ItemData?>(null) }
  var inspectedRune by remember { mutableStateOf<RuneData?>(null) }
  var inspectedSpell by remember { mutableStateOf<SpellData?>(null) }
  var editingCustomBuild by remember { mutableStateOf<CustomBuildData?>(null) }

  val championsById = remember(state.allChampions) { state.allChampions.associateBy { it.id } }

  val allItemsList = remember(state.itemsById) {
    state.itemsById.values.sortedWith(
      compareBy<ItemData> { it.category.ordinal }.thenBy { it.cost }.thenBy { it.nameEn }
    )
  }
  val allRunesList = remember(state.runesById) {
    state.runesById.values.sortedWith(
      compareBy<RuneData> { if (it.slotType == "KEYSTONE") 0 else 1 }
        .thenBy { it.tree }
        .thenBy { it.nameEn }
    )
  }

  val filteredItems = remember(allItemsList, itemSearchQuery, selectedItemCategory) {
    val q = itemSearchQuery.trim().lowercase()
    allItemsList.filter { item ->
      val matchesCat = selectedItemCategory == null || item.category == selectedItemCategory
      val matchesQ =
        q.isBlank() ||
          item.nameEn.lowercase().contains(q) ||
          item.nameTh.lowercase().contains(q) ||
          item.statsSummaryEn.lowercase().contains(q) ||
          item.passiveDescTh.lowercase().contains(q)
      matchesCat && matchesQ
    }
  }

  val filteredRunes = remember(allRunesList, runeSearchQuery, selectedRuneTree) {
    val q = runeSearchQuery.trim().lowercase()
    allRunesList.filter { rune ->
      val matchesTree =
        selectedRuneTree == null || rune.tree.equals(selectedRuneTree, ignoreCase = true)
      val matchesQ =
        q.isBlank() ||
          rune.nameEn.lowercase().contains(q) ||
          rune.nameTh.lowercase().contains(q) ||
          rune.descTh.lowercase().contains(q) ||
          rune.descEn.lowercase().contains(q)
      matchesTree && matchesQ
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)) {
      SecondaryTabRow(
        selectedTabIndex = selectedCatalogTab,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = HextechGoldBright,
      ) {
        Tab(
          selected = selectedCatalogTab == 0,
          onClick = { selectedCatalogTab = 0 },
          modifier = Modifier.testTag("catalog_tab_champions"),
          text = {
            Text(
              text =
                if (lang == AppLanguage.TH) "แชมเปี้ยน"
                else "Champs",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 0) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
        Tab(
          selected = selectedCatalogTab == 1,
          onClick = { selectedCatalogTab = 1 },
          modifier = Modifier.testTag("catalog_tab_items"),
          text = {
            Text(
              text =
                if (lang == AppLanguage.TH) "ไอเทม (${allItemsList.size})"
                else "Items (${allItemsList.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 1) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
        Tab(
          selected = selectedCatalogTab == 2,
          onClick = { selectedCatalogTab = 2 },
          modifier = Modifier.testTag("catalog_tab_runes"),
          text = {
            Text(
              text =
                if (lang == AppLanguage.TH) "รูน (${allRunesList.size})"
                else "Runes (${allRunesList.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 2) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
        Tab(
          selected = selectedCatalogTab == 3,
          onClick = { selectedCatalogTab = 3 },
          modifier = Modifier.testTag("catalog_tab_custom_builds"),
          text = {
            Text(
              text =
                if (lang == AppLanguage.TH) "เซ็ตฉัน (${state.customBuilds.size})"
                else "Custom (${state.customBuilds.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 3) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
      }

      when (selectedCatalogTab) {
        0 -> {
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
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                FilterChip(
                  selected = state.sortMode == ChampionSortMode.TIER,
                  onClick = { onSortModeChange(ChampionSortMode.TIER) },
                  modifier = Modifier.testTag("sort_tier_chip"),
                  label = { Text(if (lang == AppLanguage.TH) "เรียงตาม Tier" else "Sort: Tier") },
                )
                FilterChip(
                  selected = state.sortMode == ChampionSortMode.WIN_RATE,
                  onClick = { onSortModeChange(ChampionSortMode.WIN_RATE) },
                  modifier = Modifier.testTag("sort_winrate_chip"),
                  label = { Text(if (lang == AppLanguage.TH) "เรียงตาม Winrate" else "Sort: Win Rate") },
                )
                FilterChip(
                  selected = state.onlyFavorites,
                  onClick = { onToggleOnlyFavorites(!state.onlyFavorites) },
                  leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                  label = { Text(if (lang == AppLanguage.TH) "ตัวโปรด" else "Favorites") },
                )
                Lane.entries.forEach { lane ->
                  FilterChip(
                    selected = state.selectedLane == lane,
                    onClick = { onSelectLane(if (state.selectedLane == lane) null else lane) },
                    label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
                  )
                }
                ChampionRole.entries.forEach { role ->
                  FilterChip(
                    selected = state.selectedRole == role,
                    onClick = { onSelectRole(if (state.selectedRole == role) null else role) },
                    label = { Text(if (lang == AppLanguage.TH) role.labelTh else role.labelEn) },
                  )
                }
              }
            }

            if (state.filteredChampions.isEmpty()) {
              item {
                HextechCard {
                  Text(
                    text =
                      if (lang == AppLanguage.TH) "ไม่พบแชมเปี้ยนที่ตรงกับเงื่อนไข"
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
                          text = champ.displayName(lang),
                          style = MaterialTheme.typography.titleMedium,
                        )
                        TierBadge(champ.tierRank)
                      }
                      Text(
                        text = champ.displayTitle(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        DamageTypeBadge(champ.damageType, lang)
                        Text(
                          text =
                            champ.lanes.joinToString(" • ") {
                              if (lang == AppLanguage.TH) it.labelTh else it.labelEn
                            },
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Spacer(modifier = Modifier.height(6.dp))
                      WinRatePillsRow(champion = champ, language = lang)
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
        }

        1 -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("items_catalog_lazy_column"),
            contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 8.dp),
          ) {
            item {
              OutlinedTextField(
                value = itemSearchQuery,
                onValueChange = { itemSearchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("items_search_input"),
                placeholder = {
                  Text(if (lang == AppLanguage.TH) "ค้นหาไอเทม Wild Rift..." else "Search Wild Rift items...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
              )
            }
            item {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                FilterChip(
                  selected = selectedItemCategory == null,
                  onClick = { selectedItemCategory = null },
                  label = { Text(if (lang == AppLanguage.TH) "ทั้งหมด" else "All") },
                )
                ItemCategory.entries.forEach { cat ->
                  FilterChip(
                    selected = selectedItemCategory == cat,
                    onClick = {
                      selectedItemCategory = if (selectedItemCategory == cat) null else cat
                    },
                    label = { Text(if (lang == AppLanguage.TH) cat.labelTh else cat.labelEn) },
                  )
                }
              }
            }
            items(filteredItems, key = { it.id }) { item ->
              HextechCard(
                modifier = Modifier.fillMaxWidth().testTag("catalog_item_${item.id}"),
                onClick = { inspectedItem = item },
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { inspectedItem = item },
                    size = 48.dp,
                    showCost = false,
                    showName = false,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = item.displayName(lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = HextechGoldBright,
                        modifier = Modifier.weight(1f),
                      )
                      Text(
                        text = "${item.cost}g",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HextechGold,
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = item.statsSummary(lang),
                      style = MaterialTheme.typography.labelSmall,
                      color = ArcaneCyan,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                      text = item.passiveDesc(lang),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                    )
                  }
                }
              }
            }
          }
        }

        2 -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("runes_catalog_lazy_column"),
            contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 8.dp),
          ) {
            item {
              OutlinedTextField(
                value = runeSearchQuery,
                onValueChange = { runeSearchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("runes_search_input"),
                placeholder = {
                  Text(if (lang == AppLanguage.TH) "ค้นหารูน Wild Rift..." else "Search Wild Rift runes...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
              )
            }
            item {
              val trees = listOf("PRECISION", "DOMINATION", "RESOLVE", "INSPIRATION")
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                FilterChip(
                  selected = selectedRuneTree == null,
                  onClick = { selectedRuneTree = null },
                  label = { Text(if (lang == AppLanguage.TH) "ทุกสาย" else "All Trees") },
                )
                trees.forEach { tree ->
                  FilterChip(
                    selected = selectedRuneTree == tree,
                    onClick = { selectedRuneTree = if (selectedRuneTree == tree) null else tree },
                    label = { Text(tree.lowercase().replaceFirstChar { it.uppercase() }) },
                  )
                }
              }
            }
            items(filteredRunes, key = { it.id }) { rune ->
              val isKeystone = rune.slotType.equals("KEYSTONE", ignoreCase = true)
              HextechCard(
                modifier = Modifier.fillMaxWidth().testTag("catalog_rune_${rune.id}"),
                accentColor = if (isKeystone) HextechGold else null,
                onClick = { inspectedRune = rune },
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RuneIconSlot(
                    rune = rune,
                    language = lang,
                    onClick = { inspectedRune = rune },
                    size = 46.dp,
                    showLabel = false,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = rune.displayName(lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isKeystone) HextechGoldBright else MaterialTheme.colorScheme.onSurface,
                      )
                      Surface(
                        shape = RoundedCornerShape(5.dp),
                        color =
                          if (isKeystone) HextechGold.copy(alpha = 0.2f)
                          else SkillViolet.copy(alpha = 0.16f),
                      ) {
                        Text(
                          text = if (isKeystone) "Keystone" else (rune.tree.ifBlank { "Minor" }),
                          style = MaterialTheme.typography.labelSmall,
                          color = if (isKeystone) HextechGoldBright else SkillViolet,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = rune.desc(lang),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        else -> {
          CustomBuildsTab(
            customBuilds = state.customBuilds,
            championsById = championsById,
            itemsById = state.itemsById,
            runesById = state.runesById,
            spellsById = state.spellsById,
            language = lang,
            onCreateNew = {
              state.allChampions.firstOrNull()?.let { firstChamp ->
                editingCustomBuild = buildDefaultCustomDraft(firstChamp)
              }
            },
            onEditBuild = { build -> editingCustomBuild = build },
            onDuplicateBuild = { build ->
              onSaveCustomBuild(
                build.copy(id = 0L, name = "${build.name} (Copy)")
              )
            },
            onDeleteBuild = onDeleteCustomBuild,
            onInspectItem = { inspectedItem = it },
            onInspectRune = { inspectedRune = it },
            onInspectSpell = { inspectedSpell = it },
          )
        }
      }
    }
  }

  editingCustomBuild?.let { draft ->
    CustomBuildEditorSheet(
      initialBuild = draft,
      allChampions = state.allChampions,
      itemsById = state.itemsById,
      runesById = state.runesById,
      spellsById = state.spellsById,
      language = lang,
      onSave = { saved ->
        onSaveCustomBuild(saved)
        editingCustomBuild = null
      },
      onDismiss = { editingCustomBuild = null },
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

  inspectedRune?.let { rune ->
    RuneDetailBottomSheet(
      rune = rune,
      language = lang,
      onDismiss = { inspectedRune = null },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChampionDetailScreen(
  champion: ChampionData,
  state: ChampionsUiState,
  onToggleFavorite: (String) -> Unit,
  onSaveCustomBuild: (CustomBuildData) -> Unit,
  onDeleteCustomBuild: (Long) -> Unit,
  onAnalyze1v5: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val isMiniMode = LocalInGameMiniMode.current
  val lang = LocalUserPrefs.current.language
  val isFav = champion.id in state.favoriteIds
  val champMap = state.allChampions.associateBy { it.id }
  val champCustomBuilds =
    remember(state.customBuilds, champion.id) {
      state.customBuilds.filter { it.championId == champion.id }
    }

  var inspectedItem by remember { mutableStateOf<ItemData?>(null) }
  var inspectedRune by remember { mutableStateOf<RuneData?>(null) }
  var inspectedSpell by remember { mutableStateOf<SpellData?>(null) }
  var editingCustomBuild by remember { mutableStateOf<CustomBuildData?>(null) }
  var resolvedAbilityIcons by remember(champion.id) {
    mutableStateOf<Map<String, String>>(emptyMap())
  }
  LaunchedEffect(champion.id) {
    resolvedAbilityIcons = DDragonIconResolver.resolveAbilityIcons(champion.id, champion.nameEn)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(champion.displayName(lang)) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { onToggleFavorite(champion.id) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Favorite",
              tint = if (isFav) HextechGold else MaterialTheme.colorScheme.onSurface,
            )
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("champion_detail_lazy_column"),
        contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 7.dp else 10.dp),
      ) {
        // Identity Header
        item {
          HextechCard(accentColor = HextechGold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ChampionCrestAvatar(champion = champion, size = 48.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = champion.displayName(lang),
                    style = MaterialTheme.typography.headlineSmall,
                  )
                  TierBadge(champion.tierRank)
                }
                Text(
                  text = champion.displayTitle(lang),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                DamageTypeBadge(champion.damageType, lang)
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            WinRatePillsRow(champion = champion, language = lang, showBanRate = true)
            Spacer(modifier = Modifier.height(6.dp))
            PatchBadgePill(
              patchVersion = champion.patchVersion,
              sourceLabel =
                if (lang == AppLanguage.TH) champion.sourceType.labelTh
                else champion.sourceType.labelEn,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onAnalyze1v5(champion.id) },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("detail_analyze_button"),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = HextechGold,
                  contentColor = Color(0xFF0A0F1A),
                ),
            ) {
              Icon(Icons.Default.Security, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) "วิเคราะห์ 1v5 & จัดบิลด์แก้ทางให้ ${champion.nameTh}"
                  else "Analyze 1v5 Matchup for ${champion.nameEn}",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        // 3-Phase Interactive Game Plan Card (Early / Mid / Late)
        item {
          var selectedGamePlanTab by remember { mutableIntStateOf(0) }
          HextechCard(accentColor = ArcaneCyan) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (lang == AppLanguage.TH) "Game Plan • แผนการเดินเกม" else "Game Plan & Strategy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("detail_game_plan_title"),
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = ArcaneCyan.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.5f)),
              ) {
                Text(
                  text = if (lang == AppLanguage.TH) "3 ช่วงเกม" else "3 Phases",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = ArcaneCyan,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryTabRow(
              selectedTabIndex = selectedGamePlanTab,
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = HextechGoldBright,
              modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            ) {
              Tab(
                selected = selectedGamePlanTab == 0,
                onClick = { selectedGamePlanTab = 0 },
                modifier = Modifier.testTag("game_plan_tab_0"),
                text = {
                  Text(
                    text = if (lang == AppLanguage.TH) "ต้นเกม (Early)" else "Early Game",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedGamePlanTab == 0) FontWeight.Bold else FontWeight.Normal,
                  )
                },
              )
              Tab(
                selected = selectedGamePlanTab == 1,
                onClick = { selectedGamePlanTab = 1 },
                modifier = Modifier.testTag("game_plan_tab_1"),
                text = {
                  Text(
                    text = if (lang == AppLanguage.TH) "กลางเกม (Mid)" else "Mid Game",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedGamePlanTab == 1) FontWeight.Bold else FontWeight.Normal,
                  )
                },
              )
              Tab(
                selected = selectedGamePlanTab == 2,
                onClick = { selectedGamePlanTab = 2 },
                modifier = Modifier.testTag("game_plan_tab_2"),
                text = {
                  Text(
                    text = if (lang == AppLanguage.TH) "ท้ายเกม (Late)" else "Late Game",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedGamePlanTab == 2) FontWeight.Bold else FontWeight.Normal,
                  )
                },
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedGamePlanTab) {
              0 -> {
                TacticalMeterBar(
                  label = if (lang == AppLanguage.TH) "ระดับพลังต้นเกม (Early Power)" else "Early Game Power",
                  valuePercent = champion.earlyPower * 10,
                  barColor = ArcaneCyan,
                  trailingLabel = "${champion.earlyPower}/10",
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = champion.earlyPlan(lang),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("game_plan_content_0"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = ArcaneCyan.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        if (lang == AppLanguage.TH)
                          "💡 ทริคคุมเลน: เน้น Last-hit จัดการเวฟให้อยู่หน้าป้อม ระวังการโดน Jungle แก๊งค์ และสู้เฉพาะตอนสกิลเทรดพร้อม"
                        else
                          "💡 Laning Trick: Manage wave near your turret, avoid overextending before level 5, and only trade when key cooldowns are ready.",
                      style = MaterialTheme.typography.bodySmall,
                      color = ArcaneCyan,
                    )
                  }
                }
              }
              1 -> {
                TacticalMeterBar(
                  label = if (lang == AppLanguage.TH) "ระดับพลังกลางเกม (Mid Power)" else "Mid Game Power",
                  valuePercent = champion.midPower * 10,
                  barColor = HextechGold,
                  trailingLabel = "${champion.midPower}/10",
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = champion.midPlan(lang),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("game_plan_content_1"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = HextechGold.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        if (lang == AppLanguage.TH)
                          "💡 ทริคคุมออบเจกต์: รวมทีมชิง Dragon และ Herald ดันไซด์เลนแล้วหมุนตัวช่วยทีมเมื่อไอเทม Core ชิ้นที่ 1-2 เสร็จ"
                        else
                          "💡 Objective Trick: Group for Dragon/Herald fights, push out side lanes, and rotate when 1st-2nd core spikes are online.",
                      style = MaterialTheme.typography.bodySmall,
                      color = HextechGoldBright,
                    )
                  }
                }
              }
              else -> {
                TacticalMeterBar(
                  label = if (lang == AppLanguage.TH) "ระดับพลังท้ายเกม (Late Power)" else "Late Game Power",
                  valuePercent = champion.latePower * 10,
                  barColor = AdvantageEmerald,
                  trailingLabel = "${champion.latePower}/10",
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = champion.latePlan(lang),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("game_plan_content_2"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = AdvantageEmerald.copy(alpha = 0.12f),
                  border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text =
                        if (lang == AppLanguage.TH)
                          "💡 ทริคทีมไฟต์ 5v5: ห้ามเดินหน้าคนเดียวในจุดที่ไม่มีวิชั่น ยืนตำแหน่งตามหน้าที่ (หน้าชน / หลังทำดาเมจ) และโฟกัส Baron / Elder"
                        else
                          "💡 5v5 Teamfight Trick: Never wander alone into fog of war, stick strictly to your role positioning, and contest Baron/Elder Dragon.",
                      style = MaterialTheme.typography.bodySmall,
                      color = AdvantageEmerald,
                    )
                  }
                }
              }
            }
          }
        }

        // Skill Section (P, 1, 2, 3, 4)
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "Skill • ลำดับอัป ${champion.skillOrder}"
                else "Skill • Order ${champion.skillOrder}",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.testTag("detail_skill_title"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              champion.abilities.forEach { ab ->
                val iconUrl = ab.iconUrl.ifBlank { resolvedAbilityIcons[ab.slot].orEmpty() }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    AbilityIconBadge(
                      slot = ab.slot,
                      iconUrl = iconUrl,
                      contentDescription = if (lang == AppLanguage.TH) ab.nameTh else ab.nameEn,
                      size = 40.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (lang == AppLanguage.TH) ab.nameTh else ab.nameEn,
                        style = MaterialTheme.typography.titleSmall,
                        color = HextechGoldBright,
                      )
                      Spacer(modifier = Modifier.height(3.dp))
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        if (ab.cooldownNote.isNotBlank()) {
                          Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = ArcaneCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
                          ) {
                            Text(
                              text = "CD ${ab.cooldownNote}",
                              style = MaterialTheme.typography.labelSmall,
                              color = ArcaneCyan,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                        }
                        if (ab.costNote.isNotBlank() && ab.costNote != "0") {
                          Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = HextechGold.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
                          ) {
                            Text(
                              text = ab.costNote,
                              style = MaterialTheme.typography.labelSmall,
                              color = HextechGoldBright,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = if (lang == AppLanguage.TH) ab.summaryTh else ab.summaryEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Strengths & Weaknesses
        item {
          HextechCard {
            Text(
              text = if (lang == AppLanguage.TH) "จุดแข็ง & จุดอ่อน" else "Strengths & Weaknesses",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            champion.strengths(lang).forEach { s ->
              Text(
                text = "+ $s",
                style = MaterialTheme.typography.bodySmall,
                color = AdvantageEmerald,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            champion.weaknesses(lang).forEach { w ->
              Text(
                text = "− $w",
                style = MaterialTheme.typography.bodySmall,
                color = ThreatCrimson,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
          }
        }

        // Standard Build, Runes & Spells (Interactive Icon Slots)
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "บิลด์มาตรฐาน รูน และ Spell"
                else "Standard Build, Runes & Spells",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) "กดที่รูปไอเทมหรือ Spell เพื่อดูค่าพลัง ราคา และสกิลติดตัว"
                else "Tap any item or spell icon to view stats, cost & passive details",
              style = MaterialTheme.typography.labelSmall,
              color = ArcaneCyan,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val starterItem = state.itemsById[champion.starterItemId]
            val coreItems = champion.coreItemIds.mapNotNull { state.itemsById[it] }
            val bootsItem = state.itemsById[champion.defaultBootsId]
            val enchantItem = state.itemsById[champion.defaultEnchantId]
            val keystone = state.runesById[champion.defaultKeystoneId]
            val minorRunes = champion.defaultMinorRuneIds.mapNotNull { state.runesById[it] }
            val spells = champion.defaultSpellIds.mapNotNull { state.spellsById[it] }

            Text(
              text = if (lang == AppLanguage.TH) "ไอเทมเริ่มต้น & ไอเทมหลัก (Core):" else "Starter & Core Build:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              starterItem?.let { item ->
                item(key = "starter_${item.id}") {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { inspectedItem = item },
                    accentColor = ArcaneCyan,
                    badgeText = "Start",
                    badgeColor = ArcaneCyan,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${item.id}"),
                  )
                }
              }
              items(coreItems.size, key = { idx -> "core_${idx}_${coreItems[idx].id}" }) { idx ->
                val item = coreItems[idx]
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = HextechGold,
                  badgeText = "#${idx + 1}",
                  badgeColor = HextechGold,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (lang == AppLanguage.TH) "รองเท้า & ไอเทมกดใช้ (Active):" else "Boots & Active Item:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              bootsItem?.let { item ->
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = HextechGold,
                  badgeText = if (lang == AppLanguage.TH) "รองเท้า" else "Boots",
                  badgeColor = HextechGold,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
              enchantItem?.let { item ->
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = ArcaneCyan,
                  badgeText = "Active",
                  badgeColor = ArcaneCyan,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) "รูนหลัก (Keystone) & รูนรอง:"
                else "Keystone & Minor Runes:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              keystone?.let { ks ->
                RuneIconSlot(
                  rune = ks,
                  language = lang,
                  onClick = { inspectedRune = ks },
                  size = 46.dp,
                  modifier = Modifier.testTag("detail_rune_${ks.id}"),
                )
              }
              minorRunes.forEach { rune ->
                RuneIconSlot(
                  rune = rune,
                  language = lang,
                  onClick = { inspectedRune = rune },
                  size = 40.dp,
                  modifier = Modifier.testTag("detail_rune_${rune.id}"),
                )
              }
            }

            if (spells.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Summoner Spells:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                spells.forEach { spell ->
                  SpellIconSlot(
                    spell = spell,
                    language = lang,
                    onClick = { inspectedSpell = spell },
                    modifier = Modifier.testTag("detail_spell_${spell.id}"),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
              onClick = { editingCustomBuild = buildDefaultCustomDraft(champion) },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("detail_customize_build_button"),
            ) {
              Icon(Icons.Default.Build, contentDescription = null, tint = HextechGold)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) "จัดเซ็ตไอเทมเอง (Custom Build)"
                  else "Customize Items & Runes",
                color = HextechGoldBright,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        if (champCustomBuilds.isNotEmpty()) {
          item {
            Text(
              text =
                if (lang == AppLanguage.TH) "เซ็ตไอเทมของฉัน (${champCustomBuilds.size})"
                else "My Custom Builds (${champCustomBuilds.size})",
              style = MaterialTheme.typography.titleMedium,
              color = HextechGoldBright,
            )
          }
          items(champCustomBuilds, key = { "detail_cb_${it.id}" }) { cb ->
            CustomBuildCard(
              build = cb,
              champion = champion,
              itemsById = state.itemsById,
              runesById = state.runesById,
              spellsById = state.spellsById,
              language = lang,
              onEdit = { editingCustomBuild = cb },
              onDuplicate = {
                onSaveCustomBuild(cb.copy(id = 0L, name = "${cb.name} (Copy)"))
              },
              onDelete = { onDeleteCustomBuild(cb.id) },
              onInspectItem = { inspectedItem = it },
              onInspectRune = { inspectedRune = it },
              onInspectSpell = { inspectedSpell = it },
            )
          }
        }

        // Counters & Favorable Matchups
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "ตัวแก้ทางและตัวที่ชนะทาง"
                else "Counters & Favorable Matchups",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (lang == AppLanguage.TH) "แพ้ทาง:" else "Countered By:",
              style = MaterialTheme.typography.labelMedium,
              color = ThreatCrimson,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              champion.counterChampionIds.mapNotNull { champMap[it] }.forEach { c ->
                Surface(
                  color = ThreatCrimson.copy(alpha = 0.16f),
                  shape = MaterialTheme.shapes.small,
                  border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = c.displayName(lang),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (lang == AppLanguage.TH) "ชนะทาง:" else "Strong Against:",
              style = MaterialTheme.typography.labelMedium,
              color = AdvantageEmerald,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              champion.favorableAgainstIds.mapNotNull { champMap[it] }.forEach { c ->
                Surface(
                  color = AdvantageEmerald.copy(alpha = 0.16f),
                  shape = MaterialTheme.shapes.small,
                  border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = c.displayName(lang),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  editingCustomBuild?.let { draft ->
    CustomBuildEditorSheet(
      initialBuild = draft,
      allChampions = state.allChampions,
      itemsById = state.itemsById,
      runesById = state.runesById,
      spellsById = state.spellsById,
      language = lang,
      onSave = { saved ->
        onSaveCustomBuild(saved)
        editingCustomBuild = null
      },
      onDismiss = { editingCustomBuild = null },
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

  inspectedRune?.let { rune ->
    RuneDetailBottomSheet(
      rune = rune,
      language = lang,
      onDismiss = { inspectedRune = null },
    )
  }
}
