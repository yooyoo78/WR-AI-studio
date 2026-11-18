package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.DraftAnalysisReport
import com.example.domain.engine.DraftAssistantEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChampionSortMode {
  TIER,
  WIN_RATE,
}

data class ChampionsUiState(
  val allChampions: List<ChampionData> = emptyList(),
  val filteredChampions: List<ChampionData> = emptyList(),
  val favoriteIds: Set<String> = emptySet(),
  val searchQuery: String = "",
  val selectedLane: Lane? = null,
  val selectedRole: ChampionRole? = null,
  val onlyFavorites: Boolean = false,
  val sortMode: ChampionSortMode = ChampionSortMode.TIER,
  val itemsById: Map<String, ItemData> = emptyMap(),
  val runesById: Map<String, RuneData> = emptyMap(),
  val spellsById: Map<String, SpellData> = emptyMap(),
)

class ChampionsViewModel(private val repository: GameDataRepository) : ViewModel() {
  private val _searchQuery = MutableStateFlow("")
  private val _selectedLane = MutableStateFlow<Lane?>(null)
  private val _selectedRole = MutableStateFlow<ChampionRole?>(null)
  private val _onlyFavorites = MutableStateFlow(false)
  private val _sortMode = MutableStateFlow(ChampionSortMode.TIER)

  val uiState: StateFlow<ChampionsUiState> =
    combine(
        repository.champions,
        repository.items,
        repository.runes,
        repository.spells,
        repository.favoriteChampionIds,
        _searchQuery,
        _selectedLane,
        _selectedRole,
        _onlyFavorites,
        _sortMode,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val champions = values[0] as List<ChampionData>
        @Suppress("UNCHECKED_CAST")
        val items = values[1] as List<ItemData>
        @Suppress("UNCHECKED_CAST")
        val runes = values[2] as List<RuneData>
        @Suppress("UNCHECKED_CAST")
        val spells = values[3] as List<SpellData>
        @Suppress("UNCHECKED_CAST")
        val favIds = values[4] as Set<String>
        val query = (values[5] as String).trim().lowercase()
        val lane = values[6] as Lane?
        val role = values[7] as ChampionRole?
        val onlyFav = values[8] as Boolean
        val sortMode = values[9] as ChampionSortMode

        val filtered =
          champions
            .filter { champ ->
              val matchesQuery =
                query.isBlank() ||
                  champ.nameEn.lowercase().contains(query) ||
                  champ.nameTh.lowercase().contains(query) ||
                  champ.titleEn.lowercase().contains(query)
              val matchesLane = lane == null || lane in champ.lanes
              val matchesRole = role == null || role in champ.roles
              val matchesFav = !onlyFav || champ.id in favIds
              matchesQuery && matchesLane && matchesRole && matchesFav
            }
            .let { list ->
              when (sortMode) {
                ChampionSortMode.TIER ->
                  list.sortedWith(
                    compareBy<ChampionData> { it.tierRank.sortOrder }.thenByDescending { it.winRatePct }
                  )
                ChampionSortMode.WIN_RATE ->
                  list.sortedWith(
                    compareByDescending<ChampionData> { it.winRatePct }.thenBy { it.tierRank.sortOrder }
                  )
              }
            }

        ChampionsUiState(
          allChampions = champions,
          filteredChampions = filtered,
          favoriteIds = favIds,
          searchQuery = query,
          selectedLane = lane,
          selectedRole = role,
          onlyFavorites = onlyFav,
          sortMode = sortMode,
          itemsById = items.associateBy { it.id },
          runesById = runes.associateBy { it.id },
          spellsById = spells.associateBy { it.id },
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChampionsUiState(),
      )

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSelectedLane(lane: Lane?) {
    _selectedLane.value = lane
  }

  fun setSelectedRole(role: ChampionRole?) {
    _selectedRole.value = role
  }

  fun setOnlyFavorites(onlyFavorites: Boolean) {
    _onlyFavorites.value = onlyFavorites
  }

  fun setSortMode(mode: ChampionSortMode) {
    _sortMode.value = mode
  }

  fun toggleFavorite(championId: String) {
    val isFav = championId in uiState.value.favoriteIds
    viewModelScope.launch { repository.toggleFavorite(championId, isFav) }
  }

  class Factory(private val repository: GameDataRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      ChampionsViewModel(repository) as T
  }
}

data class DraftUiState(
  val allChampions: List<ChampionData> = emptyList(),
  val allies: List<ChampionData> = emptyList(),
  val enemies: List<ChampionData> = emptyList(),
  val intendedLane: Lane = Lane.MID,
  val preferredRole: ChampionRole? = null,
  val report: DraftAnalysisReport? = null,
)

class DraftViewModel(
  private val repository: GameDataRepository,
  private val draftEngine: DraftAssistantEngine,
) : ViewModel() {
  private val _allyIds = MutableStateFlow<List<String>>(emptyList())
  private val _enemyIds = MutableStateFlow<List<String>>(emptyList())
  private val _intendedLane = MutableStateFlow(Lane.MID)
  private val _preferredRole = MutableStateFlow<ChampionRole?>(null)

  val uiState: StateFlow<DraftUiState> =
    combine(
        repository.champions,
        repository.matchupRules,
        _allyIds,
        _enemyIds,
        _intendedLane,
        _preferredRole,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val champions = values[0] as List<ChampionData>
        @Suppress("UNCHECKED_CAST")
        val rules = values[1] as List<com.example.data.model.MatchupRuleData>
        @Suppress("UNCHECKED_CAST")
        val allyIds = values[2] as List<String>
        @Suppress("UNCHECKED_CAST")
        val enemyIds = values[3] as List<String>
        val lane = values[4] as Lane
        val role = values[5] as ChampionRole?

        val champMap = champions.associateBy { it.id }
        val allies = allyIds.mapNotNull { champMap[it] }
        val enemies = enemyIds.mapNotNull { champMap[it] }
        val report =
          if (champions.isNotEmpty()) {
            draftEngine.analyzeDraft(allies, enemies, lane, role, champions, rules)
          } else null

        DraftUiState(
          allChampions = champions,
          allies = allies,
          enemies = enemies,
          intendedLane = lane,
          preferredRole = role,
          report = report,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DraftUiState(),
      )

  fun addOrRemoveAlly(champId: String) {
    val current = _allyIds.value.toMutableList()
    if (champId in current) {
      current.remove(champId)
    } else if (current.size < 5 && champId !in _enemyIds.value) {
      current.add(champId)
    }
    _allyIds.value = current
  }

  fun addOrRemoveEnemy(champId: String) {
    val current = _enemyIds.value.toMutableList()
    if (champId in current) {
      current.remove(champId)
    } else if (current.size < 5 && champId !in _allyIds.value) {
      current.add(champId)
    }
    _enemyIds.value = current
  }

  fun setIntendedLane(lane: Lane) {
    _intendedLane.value = lane
  }

  fun setPreferredRole(role: ChampionRole?) {
    _preferredRole.value = role
  }

  fun clearDraft() {
    _allyIds.value = emptyList()
    _enemyIds.value = emptyList()
  }

  class Factory(
    private val repository: GameDataRepository,
    private val draftEngine: DraftAssistantEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      DraftViewModel(repository, draftEngine) as T
  }
}
