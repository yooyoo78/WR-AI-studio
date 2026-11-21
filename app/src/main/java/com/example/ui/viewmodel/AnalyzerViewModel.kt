package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.data.local.SavedMatchupEntity
import com.example.data.model.ChampionData
import com.example.data.model.CustomBuildData
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.EnemyMatchupAnalysis
import com.example.domain.engine.EnemyTeamThreatProfile
import com.example.domain.engine.MatchupCounterEngine
import com.example.domain.engine.SmartBuildRecommendation
import com.example.domain.engine.SmartItemEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnalyzerUiState(
  val allChampions: List<ChampionData> = emptyList(),
  val selectedChampion: ChampionData? = null,
  val selectedLane: Lane = Lane.BARON,
  val selectedEnemies: List<ChampionData> = emptyList(),
  val enemyMatchups: List<EnemyMatchupAnalysis> = emptyList(),
  val highestThreat: EnemyMatchupAnalysis? = null,
  val threatProfile: EnemyTeamThreatProfile? = null,
  val smartBuild: SmartBuildRecommendation? = null,
  val savedMatchups: List<SavedMatchupEntity> = emptyList(),
  val customBuildsForChampion: List<CustomBuildData> = emptyList(),
)

class AnalyzerViewModel(
  private val repository: GameDataRepository,
  private val matchupEngine: MatchupCounterEngine,
  private val smartItemEngine: SmartItemEngine,
  private val appContainer: AppContainer,
) : ViewModel() {

  private val _selectedChampId = MutableStateFlow<String?>(null)
  private val _selectedLane = MutableStateFlow(Lane.BARON)
  private val _selectedEnemyIds = MutableStateFlow<List<String>>(emptyList())
  private var lastAppliedExternalSeq = 0L

  init {
    viewModelScope.launch {
      appContainer.sharedMatchupContext.collect { ctx ->
        if (ctx.externalUpdateSeq > lastAppliedExternalSeq) {
          lastAppliedExternalSeq = ctx.externalUpdateSeq
          if (ctx.myChampionId != null) {
            _selectedChampId.value = ctx.myChampionId
          }
          _selectedLane.value = ctx.lane
          _selectedEnemyIds.value = ctx.enemyIds
        }
      }
    }
  }

  val uiState: StateFlow<AnalyzerUiState> =
    combine(
        repository.champions,
        repository.items,
        repository.runes,
        repository.spells,
        repository.matchupRules,
        repository.savedMatchups,
        repository.customBuilds,
        _selectedChampId,
        _selectedLane,
        _selectedEnemyIds,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val champions = values[0] as List<ChampionData>
        @Suppress("UNCHECKED_CAST")
        val items = values[1] as List<com.example.data.model.ItemData>
        @Suppress("UNCHECKED_CAST")
        val runes = values[2] as List<com.example.data.model.RuneData>
        @Suppress("UNCHECKED_CAST")
        val spells = values[3] as List<com.example.data.model.SpellData>
        @Suppress("UNCHECKED_CAST")
        val rules = values[4] as List<com.example.data.model.MatchupRuleData>
        @Suppress("UNCHECKED_CAST")
        val saved = values[5] as List<SavedMatchupEntity>
        @Suppress("UNCHECKED_CAST")
        val allCustomBuilds = values[6] as List<CustomBuildData>
        val myChampId = values[7] as String?
        val lane = values[8] as Lane
        @Suppress("UNCHECKED_CAST")
        val enemyIds = values[9] as List<String>

        val champMap = champions.associateBy { it.id }
        val myChamp = champMap[myChampId] ?: champions.firstOrNull()
        val enemies = enemyIds.mapNotNull { champMap[it] }

        if (myChamp == null || items.isEmpty()) {
          AnalyzerUiState(allChampions = champions, savedMatchups = saved)
        } else {
          val effectiveEnemies = enemies
          val (matchups, profile) =
            matchupEngine.analyzeMatchups(
              myChampion = myChamp,
              myLane = lane,
              enemies = effectiveEnemies,
              explicitRules = rules,
            )
          val smartBuild =
            smartItemEngine.buildRecommendation(
              myChampion = myChamp,
              myLane = lane,
              enemies = effectiveEnemies,
              matchups = matchups,
              profile = profile,
              allItems = items,
              allRunes = runes,
              allSpells = spells,
            )

          AnalyzerUiState(
            allChampions = champions,
            selectedChampion = myChamp,
            selectedLane = lane,
            selectedEnemies = effectiveEnemies,
            enemyMatchups = matchups,
            highestThreat = matchups.firstOrNull(),
            threatProfile = profile,
            smartBuild = smartBuild,
            savedMatchups = saved,
            customBuildsForChampion = allCustomBuilds.filter { it.championId == myChamp.id },
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyzerUiState(),
      )

  fun selectMyChampion(champion: ChampionData) {
    _selectedChampId.value = champion.id
    _selectedLane.value = champion.lanes.firstOrNull() ?: Lane.BARON
    _selectedEnemyIds.value = _selectedEnemyIds.value.filter { it != champion.id }
    syncSharedContext()
  }

  fun selectMyChampionById(championId: String) {
    val found = uiState.value.allChampions.firstOrNull { it.id == championId } ?: return
    selectMyChampion(found)
  }

  fun selectLane(lane: Lane) {
    _selectedLane.value = lane
    syncSharedContext()
  }

  fun toggleEnemyChampion(championId: String) {
    val current = _selectedEnemyIds.value.toMutableList()
    if (championId in current) {
      current.remove(championId)
    } else if (current.size < 5 && championId != uiState.value.selectedChampion?.id) {
      current.add(championId)
    }
    _selectedEnemyIds.value = current
    syncSharedContext()
  }

  fun applyQuickEnemyPreset(enemyIds: List<String>) {
    val myId = uiState.value.selectedChampion?.id
    _selectedEnemyIds.value = enemyIds.filter { it != myId }.distinct().take(5)
    syncSharedContext()
  }

  fun clearEnemies() {
    _selectedEnemyIds.value = emptyList()
    syncSharedContext()
  }

  fun saveCurrentMatchup() {
    val state = uiState.value
    val myChamp = state.selectedChampion ?: return
    if (state.selectedEnemies.isEmpty()) return
    val label =
      "${myChamp.nameEn} vs ${state.selectedEnemies.take(3).joinToString(", ") { it.nameEn }}"
    viewModelScope.launch {
      repository.saveMatchupPreset(
        myChampionId = myChamp.id,
        lane = state.selectedLane,
        enemyIds = state.selectedEnemies.map { it.id },
        label = label,
      )
    }
  }

  fun loadSavedMatchup(saved: SavedMatchupEntity) {
    _selectedChampId.value = saved.myChampionId
    _selectedLane.value = saved.lane
    _selectedEnemyIds.value = saved.enemyIds
    syncSharedContext()
  }

  fun deleteSavedMatchup(id: Long) {
    viewModelScope.launch { repository.deleteSavedMatchup(id) }
  }

  fun saveSmartBuildAsCustom(customName: String? = null) {
    val state = uiState.value
    val myChamp = state.selectedChampion ?: return
    val build = state.smartBuild ?: return
    val enemySuffix =
      if (state.selectedEnemies.isNotEmpty()) {
        " vs ${state.selectedEnemies.take(2).joinToString("/") { it.nameEn }}"
      } else {
        " Counter"
      }
    val buildName = customName?.takeIf { it.isNotBlank() } ?: "${myChamp.nameEn}$enemySuffix"
    val entity =
      CustomBuildData(
        name = buildName,
        championId = myChamp.id,
        lane = state.selectedLane,
        starterItemId = build.starterItem?.id ?: myChamp.starterItemId,
        coreItemIds =
          build.buildDiff.adaptedCoreItems
            .map { it.id }
            .ifEmpty { myChamp.coreItemIds },
        bootsId = build.recommendedBoots.item.id,
        enchantId = build.recommendedEnchant.item.id,
        keystoneId = build.keystone?.id ?: myChamp.defaultKeystoneId,
        minorRuneIds =
          build.minorRunes.map { it.id }.ifEmpty { myChamp.defaultMinorRuneIds },
        spellIds = build.spells.map { it.id }.ifEmpty { myChamp.defaultSpellIds },
        notes = build.buildDiff.adaptationHeadlineTh,
      )
    viewModelScope.launch { repository.saveCustomBuild(entity) }
  }

  fun syncSharedContext() {
    val champId = _selectedChampId.value ?: uiState.value.selectedChampion?.id
    appContainer.updateSharedMatchupContext(
      myChampionId = champId,
      lane = _selectedLane.value,
      enemyIds = _selectedEnemyIds.value,
      isExternal = false,
    )
  }

  class Factory(
    private val repository: GameDataRepository,
    private val matchupEngine: MatchupCounterEngine,
    private val smartItemEngine: SmartItemEngine,
    private val appContainer: AppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      AnalyzerViewModel(repository, matchupEngine, smartItemEngine, appContainer) as T
  }
}
