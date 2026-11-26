package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.PostGameEvaluationResult
import com.example.data.ai.PostGameScanDraft
import com.example.data.ai.PostGameScanOutcome
import com.example.data.ai.PostGameVisionService
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import com.example.data.model.MatchupRuleData
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.DraftAnalysisReport
import com.example.domain.engine.DraftAssistantEngine
import com.example.domain.engine.MatchJournalAggregator
import com.example.domain.engine.MatchJournalSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ChampionSortMode {
  TIER,
  WIN_RATE,
}

data class PostGameReviewUiState(
  val isSheetOpen: Boolean = false,
  val isScanning: Boolean = false,
  val previewBitmap: Bitmap? = null,
  val evaluation: PostGameEvaluationResult? = null,
  val errorMessage: String? = null,
  val isSaved: Boolean = false,
  val lastSavedEntry: MatchJournalEntryData? = null,
)

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
  val customBuilds: List<CustomBuildData> = emptyList(),
  val matchJournal: List<MatchJournalEntryData> = emptyList(),
  val journalSummary: MatchJournalSummary = MatchJournalSummary(),
  val language: AppLanguage = AppLanguage.TH,
)

class ChampionsViewModel(
  private val repository: GameDataRepository,
  private val postGameVisionService: PostGameVisionService? = null,
  private val preferencesRepository: PreferencesRepository? = null,
  private val appContainer: AppContainer? = null,
) : ViewModel() {
  private val _searchQuery = MutableStateFlow("")
  private val _selectedLane = MutableStateFlow<Lane?>(null)
  private val _selectedRole = MutableStateFlow<ChampionRole?>(null)
  private val _onlyFavorites = MutableStateFlow(false)
  private val _sortMode = MutableStateFlow(ChampionSortMode.TIER)

  private val _postGameState = MutableStateFlow(PostGameReviewUiState())
  val postGameState: StateFlow<PostGameReviewUiState> = _postGameState.asStateFlow()
  private var postGameScanJob: Job? = null

  private val _allRunes = MutableStateFlow<List<RuneData>>(emptyList())
  private val _allSpells = MutableStateFlow<List<SpellData>>(emptyList())
  private val _allRules = MutableStateFlow<List<MatchupRuleData>>(emptyList())

  init {
    viewModelScope.launch { repository.runes.collect { _allRunes.value = it } }
    viewModelScope.launch { repository.spells.collect { _allSpells.value = it } }
    viewModelScope.launch { repository.matchupRules.collect { _allRules.value = it } }
  }

  val uiState: StateFlow<ChampionsUiState> =
    combine(
        repository.champions,
        repository.items,
        repository.runes,
        repository.spells,
        repository.favoriteChampionIds,
        repository.customBuilds,
        repository.matchJournal,
        _searchQuery,
        _selectedLane,
        _selectedRole,
        _onlyFavorites,
        _sortMode,
        preferencesRepository?.preferencesFlow
          ?: kotlinx.coroutines.flow.flowOf(com.example.data.local.UserPreferences()),
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
        @Suppress("UNCHECKED_CAST")
        val customBuilds = values[5] as List<CustomBuildData>
        @Suppress("UNCHECKED_CAST")
        val journalEntries = values[6] as List<MatchJournalEntryData>
        val query = (values[7] as String).trim().lowercase()
        val lane = values[8] as Lane?
        val role = values[9] as ChampionRole?
        val onlyFav = values[10] as Boolean
        val sortMode = values[11] as ChampionSortMode
        val prefs = values[12] as com.example.data.local.UserPreferences

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

        val champMap = champions.associateBy { it.id }
        val journalSummary =
          MatchJournalAggregator.summarize(journalEntries, champMap, prefs.language)

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
          customBuilds = customBuilds,
          matchJournal = journalEntries,
          journalSummary = journalSummary,
          language = prefs.language,
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

  fun saveCustomBuild(build: CustomBuildData) {
    viewModelScope.launch { repository.saveCustomBuild(build) }
  }

  fun deleteCustomBuild(id: Long) {
    viewModelScope.launch { repository.deleteCustomBuild(id) }
  }

  fun openPostGameSheet(preselectedChamp: ChampionData? = null) {
    val state = uiState.value
    val activeCtx = appContainer?.sharedMatchupContext?.value
    val champ =
      preselectedChamp
        ?: state.allChampions.firstOrNull { it.id == activeCtx?.myChampionId }
        ?: state.allChampions.firstOrNull()
    val lane = activeCtx?.lane ?: champ?.lanes?.firstOrNull() ?: Lane.MID
    val enemies =
      activeCtx?.enemyIds?.mapNotNull { id -> state.allChampions.firstOrNull { it.id == id } }.orEmpty()
    val draft =
      PostGameScanDraft(
        isWin = true,
        myChampion = champ,
        detectedLane = lane,
        kills = 6,
        deaths = 3,
        assists = 8,
        goldEarned = 14200,
        durationMinutes = 18,
        boughtItems = emptyList(),
        enemyChampions = enemies,
        lessons = emptyList(),
        spokenBriefing = "",
      )
    val eval =
      postGameVisionService?.evaluateManualOrEdited(
        draft = draft,
        items = state.itemsById.values.toList(),
        runes = _allRunes.value,
        spells = _allSpells.value,
        matchupRules = _allRules.value,
        language = state.language,
        regenerateLessonsFromEngine = true,
      ) ?: PostGameEvaluationResult(draft, null)

    _postGameState.value =
      PostGameReviewUiState(
        isSheetOpen = true,
        isScanning = false,
        previewBitmap = null,
        evaluation = eval,
        errorMessage = null,
        isSaved = false,
      )
  }

  fun dismissPostGameSheet() {
    postGameScanJob?.cancel()
    _postGameState.update { it.copy(isSheetOpen = false, isScanning = false) }
  }

  fun scanPostGameUri(uri: Uri, preview: Bitmap? = null) {
    val service = postGameVisionService ?: return
    postGameScanJob?.cancel()
    postGameScanJob =
      viewModelScope.launch {
        repository.ensureSeeded()
        val state = uiState.value
        val activeCtx = appContainer?.sharedMatchupContext?.value
        val prefs = preferencesRepository?.preferencesFlow?.first()
        val lang = prefs?.language ?: state.language
        val modelId = prefs?.selectedLiveModelId ?: AiModelCatalog.AUTO_MODEL_ID
        val fallbackChamp =
          state.allChampions.firstOrNull { it.id == activeCtx?.myChampionId }
            ?: state.allChampions.firstOrNull()
        val fallbackLane = activeCtx?.lane ?: fallbackChamp?.lanes?.firstOrNull() ?: Lane.MID

        _postGameState.update {
          it.copy(
            isSheetOpen = true,
            isScanning = true,
            previewBitmap = preview ?: it.previewBitmap,
            errorMessage = null,
            isSaved = false,
          )
        }

        val outcome =
          service.scanScoreboardUri(
            uri = uri,
            champions = state.allChampions,
            items = state.itemsById.values.toList(),
            runes = _allRunes.value.ifEmpty { repository.runes.first() },
            spells = _allSpells.value.ifEmpty { repository.spells.first() },
            matchupRules = _allRules.value.ifEmpty { repository.matchupRules.first() },
            fallbackChampion = fallbackChamp,
            fallbackLane = fallbackLane,
            patchVersion = prefs?.activePatchLabel ?: "7.3g",
            language = lang,
            preferredModelId = modelId,
          )
        handlePostGameOutcome(outcome)
      }
  }

  fun scanPostGameBitmap(bitmap: Bitmap, preview: Bitmap? = bitmap) {
    val service = postGameVisionService ?: return
    postGameScanJob?.cancel()
    postGameScanJob =
      viewModelScope.launch {
        repository.ensureSeeded()
        val state = uiState.value
        val activeCtx = appContainer?.sharedMatchupContext?.value
        val prefs = preferencesRepository?.preferencesFlow?.first()
        val lang = prefs?.language ?: state.language
        val modelId = prefs?.selectedLiveModelId ?: AiModelCatalog.AUTO_MODEL_ID
        val fallbackChamp =
          state.allChampions.firstOrNull { it.id == activeCtx?.myChampionId }
            ?: state.allChampions.firstOrNull()
        val fallbackLane = activeCtx?.lane ?: fallbackChamp?.lanes?.firstOrNull() ?: Lane.MID

        _postGameState.update {
          it.copy(
            isSheetOpen = true,
            isScanning = true,
            previewBitmap = preview ?: it.previewBitmap,
            errorMessage = null,
            isSaved = false,
          )
        }

        val outcome =
          service.scanScoreboardBitmap(
            sourceBitmap = bitmap,
            champions = state.allChampions,
            items = state.itemsById.values.toList(),
            runes = _allRunes.value.ifEmpty { repository.runes.first() },
            spells = _allSpells.value.ifEmpty { repository.spells.first() },
            matchupRules = _allRules.value.ifEmpty { repository.matchupRules.first() },
            fallbackChampion = fallbackChamp,
            fallbackLane = fallbackLane,
            patchVersion = prefs?.activePatchLabel ?: "7.3g",
            language = lang,
            preferredModelId = modelId,
          )
        handlePostGameOutcome(outcome)
      }
  }

  private fun handlePostGameOutcome(outcome: PostGameScanOutcome) {
    when (outcome) {
      is PostGameScanOutcome.Success -> {
        _postGameState.update {
          it.copy(
            isScanning = false,
            evaluation = outcome.result,
            errorMessage = null,
            isSaved = false,
          )
        }
      }
      is PostGameScanOutcome.Failure -> {
        _postGameState.update {
          it.copy(
            isScanning = false,
            errorMessage = outcome.message,
          )
        }
      }
    }
  }

  fun updatePostGameDraft(
    regenerateLessons: Boolean = false,
    transform: (PostGameScanDraft) -> PostGameScanDraft,
  ) {
    val currentEval = _postGameState.value.evaluation ?: return
    val updatedDraft = transform(currentEval.draft)
    val state = uiState.value
    val reEvaluated =
      postGameVisionService?.evaluateManualOrEdited(
        draft = updatedDraft,
        items = state.itemsById.values.toList(),
        runes = _allRunes.value,
        spells = _allSpells.value,
        matchupRules = _allRules.value,
        language = state.language,
        regenerateLessonsFromEngine = regenerateLessons,
      ) ?: currentEval.copy(draft = updatedDraft)

    _postGameState.update {
      it.copy(evaluation = reEvaluated, isSaved = false)
    }
  }

  fun savePostGameToJournal(onSaved: (MatchJournalEntryData) -> Unit = {}) {
    val eval = _postGameState.value.evaluation ?: return
    val draft = eval.draft
    val champ = draft.myChampion ?: return
    val report = eval.accuracyReport
    val state = uiState.value

    val entry =
      MatchJournalEntryData(
        championId = champ.id,
        lane = draft.detectedLane,
        isWin = draft.isWin,
        kills = draft.kills,
        deaths = draft.deaths,
        assists = draft.assists,
        goldEarned = draft.goldEarned,
        durationMinutes = draft.durationMinutes,
        boughtItemIds = draft.boughtItems.map { it.id },
        recommendedItemIds = report?.recommendedItems?.map { it.id }.orEmpty(),
        enemyChampionIds = draft.enemyChampions.map { it.id },
        buildAccuracyPct = report?.accuracyPct ?: 75,
        lessons = draft.lessons.ifEmpty {
          report?.feedback(state.language).orEmpty().take(3)
        },
        mistakeTags = report?.mistakeTags.orEmpty(),
        patchVersion = preferencesRepository?.preferencesFlow?.let { "7.3g" } ?: "7.3g",
      )

    viewModelScope.launch {
      val patchLabel =
        preferencesRepository?.preferencesFlow?.first()?.activePatchLabel ?: entry.patchVersion
      val finalEntry = entry.copy(patchVersion = patchLabel)
      val id = repository.saveMatchJournalEntry(finalEntry)
      val persisted = finalEntry.copy(id = id)
      _postGameState.update { it.copy(isSaved = true, lastSavedEntry = persisted) }
      onSaved(persisted)
    }
  }

  fun deleteMatchJournalEntry(id: Long) {
    viewModelScope.launch { repository.deleteMatchJournalEntry(id) }
  }

  class Factory(
    private val repository: GameDataRepository,
    private val postGameVisionService: PostGameVisionService? = null,
    private val preferencesRepository: PreferencesRepository? = null,
    private val appContainer: AppContainer? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      ChampionsViewModel(
        repository = repository,
        postGameVisionService = postGameVisionService,
        preferencesRepository = preferencesRepository,
        appContainer = appContainer,
      ) as T
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
  private val appContainer: AppContainer? = null,
) : ViewModel() {
  private val _allyIds = MutableStateFlow<List<String>>(emptyList())
  private val _enemyIds = MutableStateFlow<List<String>>(emptyList())
  private val _intendedLane = MutableStateFlow(Lane.MID)
  private val _preferredRole = MutableStateFlow<ChampionRole?>(null)
  private var lastAppliedExternalSeq = 0L

  init {
    appContainer?.let { container ->
      viewModelScope.launch {
        container.sharedDraftContext.collect { ctx ->
          if (ctx.externalUpdateSeq > lastAppliedExternalSeq) {
            lastAppliedExternalSeq = ctx.externalUpdateSeq
            _allyIds.value = ctx.allyIds
            _enemyIds.value = ctx.enemyIds
            _intendedLane.value = ctx.intendedLane
            _preferredRole.value = ctx.preferredRole
          }
        }
      }
    }
  }

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
    private val appContainer: AppContainer? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      DraftViewModel(repository, draftEngine, appContainer) as T
  }
}
