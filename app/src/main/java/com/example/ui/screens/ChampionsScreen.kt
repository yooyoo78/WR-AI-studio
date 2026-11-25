package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.example.data.ai.CoachSpeechState
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionRole
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemCategory
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import com.example.data.model.RuneData
import com.example.ui.components.CustomBuildEditorSheet
import com.example.ui.components.MiniInspectHost
import com.example.ui.components.MiniInspectTarget
import com.example.ui.components.buildDefaultCustomDraft
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.viewmodel.ChampionSortMode
import com.example.ui.viewmodel.ChampionsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionsScreen(
  state: ChampionsUiState,
  speechState: CoachSpeechState = CoachSpeechState(),
  onSearchChange: (String) -> Unit,
  onSelectLane: (Lane?) -> Unit,
  onSelectRole: (ChampionRole?) -> Unit,
  onToggleOnlyFavorites: (Boolean) -> Unit,
  onSortModeChange: (ChampionSortMode) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onSaveCustomBuild: (CustomBuildData) -> Unit,
  onDeleteCustomBuild: (Long) -> Unit,
  onOpenDetail: (String) -> Unit,
  onOpenPostGameScanner: () -> Unit = {},
  onToggleJournalSpeak: (String, String) -> Unit = { _, _ -> },
  onAskCoachAboutMatch: (MatchJournalEntryData) -> Unit = {},
  onDeleteJournalEntry: (Long) -> Unit = {},
) {
  val lang = LocalUserPrefs.current.language

  var selectedCatalogTab by remember { mutableIntStateOf(0) }
  var itemSearchQuery by remember { mutableStateOf("") }
  var selectedItemCategory by remember { mutableStateOf<ItemCategory?>(null) }
  var runeSearchQuery by remember { mutableStateOf("") }
  var selectedRuneTree by remember { mutableStateOf<String?>(null) }
  var miniInspectTarget by remember { mutableStateOf<MiniInspectTarget?>(null) }
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
              text = if (lang == AppLanguage.TH) "ตัวละคร" else "Champs",
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
              text = if (lang == AppLanguage.TH) "ไอเทม" else "Items",
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
              text = if (lang == AppLanguage.TH) "รูน" else "Runes",
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
              text = if (lang == AppLanguage.TH) "เซ็ตฉัน" else "Custom",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 3) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
        Tab(
          selected = selectedCatalogTab == 4,
          onClick = { selectedCatalogTab = 4 },
          modifier = Modifier.testTag("catalog_tab_journal"),
          text = {
            Text(
              text =
                if (lang == AppLanguage.TH) "บันทึก"
                else "Journal",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selectedCatalogTab == 4) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
      }

      when (selectedCatalogTab) {
        0 ->
          ChampionsRosterTab(
            state = state,
            language = lang,
            onSearchChange = onSearchChange,
            onSelectLane = onSelectLane,
            onSelectRole = onSelectRole,
            onToggleOnlyFavorites = onToggleOnlyFavorites,
            onSortModeChange = onSortModeChange,
            onToggleFavorite = onToggleFavorite,
            onOpenDetail = onOpenDetail,
          )
        1 ->
          ChampionsItemsCatalogTab(
            filteredItems = filteredItems,
            searchQuery = itemSearchQuery,
            selectedCategory = selectedItemCategory,
            language = lang,
            onSearchChange = { itemSearchQuery = it },
            onSelectCategory = { selectedItemCategory = it },
            onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
          )
        2 ->
          ChampionsRunesCatalogTab(
            filteredRunes = filteredRunes,
            searchQuery = runeSearchQuery,
            selectedTree = selectedRuneTree,
            language = lang,
            onSearchChange = { runeSearchQuery = it },
            onSelectTree = { selectedRuneTree = it },
            onInspectRune = { miniInspectTarget = MiniInspectTarget.Rune(it) },
          )
        3 ->
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
              onSaveCustomBuild(build.copy(id = 0L, name = "${build.name} (Copy)"))
            },
            onDeleteBuild = onDeleteCustomBuild,
            onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
            onInspectRune = { miniInspectTarget = MiniInspectTarget.Rune(it) },
            onInspectSpell = { miniInspectTarget = MiniInspectTarget.Spell(it) },
            onOpenChampionDetail = onOpenDetail,
          )
        else ->
          MatchJournalTab(
            entries = state.matchJournal,
            summary = state.journalSummary,
            championsById = championsById,
            itemsById = state.itemsById,
            language = lang,
            speechState = speechState,
            onOpenPostGameScanner = onOpenPostGameScanner,
            onToggleSpeak = onToggleJournalSpeak,
            onAskCoachAboutMatch = onAskCoachAboutMatch,
            onDeleteEntry = onDeleteJournalEntry,
          )
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

  MiniInspectHost(
    target = miniInspectTarget,
    language = lang,
    onDismiss = { miniInspectTarget = null },
    itemsById = state.itemsById,
  )
}
