package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.AppContainer
import com.example.data.ai.CoachStreamEvent
import com.example.data.ai.GeminiCoachService
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.CoachPlaybookSection
import com.example.domain.engine.OfflineCoachEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GeminiUiState {
  data object Idle : GeminiUiState

  data object Loading : GeminiUiState

  data class Streaming(val text: String) : GeminiUiState

  data class Success(val text: String) : GeminiUiState

  data class Error(val message: String) : GeminiUiState
}

data class CoachChatMessage(val isUser: Boolean, val text: String)

data class CoachUiState(
  val allChampions: List<ChampionData> = emptyList(),
  val selectedChampion: ChampionData? = null,
  val selectedLane: Lane = Lane.MID,
  val enemyContext: List<ChampionData> = emptyList(),
  val offlinePlaybook: List<CoachPlaybookSection> = emptyList(),
  val activePatchVersion: String = "7.3f",
  val language: AppLanguage = AppLanguage.TH,
)

class CoachViewModel(
  private val repository: GameDataRepository,
  private val preferencesRepository: PreferencesRepository,
  private val offlineCoachEngine: OfflineCoachEngine,
  private val geminiCoachService: GeminiCoachService,
  private val appContainer: AppContainer,
) : ViewModel() {

  private val _selectedChampId = MutableStateFlow<String?>(null)
  private val _selectedLane = MutableStateFlow(Lane.MID)
  private val _enemyIds = MutableStateFlow<List<String>>(emptyList())

  private val _geminiState = MutableStateFlow<GeminiUiState>(GeminiUiState.Idle)
  val geminiState: StateFlow<GeminiUiState> = _geminiState.asStateFlow()

  private val _messages = MutableStateFlow<List<CoachChatMessage>>(emptyList())
  val messages: StateFlow<List<CoachChatMessage>> = _messages.asStateFlow()

  private var streamJob: Job? = null

  val uiState: StateFlow<CoachUiState> =
    combine(
        repository.champions,
        preferencesRepository.preferencesFlow,
        _selectedChampId,
        _selectedLane,
        _enemyIds,
      ) { champions, prefs, champId, lane, enemyIds ->
        val map = champions.associateBy { it.id }
        val selected = map[champId] ?: champions.firstOrNull()
        val enemies = enemyIds.mapNotNull { map[it] }
        val playbook =
          if (selected != null) {
            offlineCoachEngine.buildPlaybook(selected, lane, enemies)
          } else emptyList()

        CoachUiState(
          allChampions = champions,
          selectedChampion = selected,
          selectedLane = lane,
          enemyContext = enemies,
          offlinePlaybook = playbook,
          activePatchVersion = prefs.activePatchLabel,
          language = prefs.language,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoachUiState(),
      )

  init {
    viewModelScope.launch {
      appContainer.sharedMatchupContext.collect { ctx ->
        if (ctx.myChampionId != null) {
          _selectedChampId.value = ctx.myChampionId
          _selectedLane.value = ctx.lane
          _enemyIds.value = ctx.enemyIds
        }
      }
    }
  }

  fun selectChampion(champ: ChampionData) {
    _selectedChampId.value = champ.id
    _selectedLane.value = champ.lanes.firstOrNull() ?: Lane.MID
  }

  fun selectLane(lane: Lane) {
    _selectedLane.value = lane
  }

  fun askGeminiCoach(question: String) {
    val trimmed = question.trim()
    if (trimmed.isBlank()) return
    val current = uiState.value
    streamJob?.cancel()

    _messages.value = _messages.value + CoachChatMessage(isUser = true, text = trimmed)
    _geminiState.value = GeminiUiState.Loading

    streamJob =
      viewModelScope.launch {
        geminiCoachService
          .streamCoachingAdvice(
            question = trimmed,
            champion = current.selectedChampion,
            lane = current.selectedLane,
            enemies = current.enemyContext,
            patchVersion = current.activePatchVersion,
            language = current.language,
          )
          .collect { event ->
            when (event) {
              is CoachStreamEvent.Chunk -> {
                _geminiState.value = GeminiUiState.Streaming(event.accumulatedText)
              }
              is CoachStreamEvent.Completed -> {
                _geminiState.value = GeminiUiState.Success(event.finalText)
                _messages.value =
                  _messages.value + CoachChatMessage(isUser = false, text = event.finalText)
              }
              is CoachStreamEvent.Error -> {
                _geminiState.value = GeminiUiState.Error(event.message)
              }
            }
          }
      }
  }

  fun clearChat() {
    streamJob?.cancel()
    _messages.value = emptyList()
    _geminiState.value = GeminiUiState.Idle
  }

  class Factory(
    private val repository: GameDataRepository,
    private val preferencesRepository: PreferencesRepository,
    private val offlineCoachEngine: OfflineCoachEngine,
    private val geminiCoachService: GeminiCoachService,
    private val appContainer: AppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      CoachViewModel(
        repository,
        preferencesRepository,
        offlineCoachEngine,
        geminiCoachService,
        appContainer,
      ) as T
  }
}
