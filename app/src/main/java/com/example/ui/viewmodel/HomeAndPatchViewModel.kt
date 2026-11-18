package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.PatchNoteData
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
import com.example.data.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
  val isLoading: Boolean = true,
  val activePatch: PatchNoteData? = null,
  val allPatchNotes: List<PatchNoteData> = emptyList(),
  val totalChampions: Int = 0,
  val totalItems: Int = 0,
  val favoriteChampions: List<ChampionData> = emptyList(),
  val topMetaChampions: List<ChampionData> = emptyList(),
  val selectedTierLane: Lane? = null,
  val preferences: UserPreferences = UserPreferences(),
)

sealed interface PatchActionStatus {
  data object Idle : PatchActionStatus

  data object Working : PatchActionStatus

  data class Message(val isError: Boolean, val text: String) : PatchActionStatus
}

class HomeAndPatchViewModel(
  private val repository: GameDataRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  private val _selectedTierLane = MutableStateFlow<Lane?>(null)
  private val _patchActionStatus = MutableStateFlow<PatchActionStatus>(PatchActionStatus.Idle)
  val patchActionStatus: StateFlow<PatchActionStatus> = _patchActionStatus.asStateFlow()

  private val _validationResult = MutableStateFlow<ValidationResult?>(null)
  val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

  val preferences: StateFlow<UserPreferences> =
    preferencesRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserPreferences(),
    )

  val homeUiState: StateFlow<HomeUiState> =
    combine(
        repository.champions,
        repository.items,
        repository.patchNotes,
        repository.favoriteChampionIds,
        preferences,
        _selectedTierLane,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val champions = values[0] as List<ChampionData>
        @Suppress("UNCHECKED_CAST")
        val items = values[1] as List<com.example.data.model.ItemData>
        @Suppress("UNCHECKED_CAST")
        val patchNotes = values[2] as List<PatchNoteData>
        @Suppress("UNCHECKED_CAST")
        val favIds = values[3] as Set<String>
        val prefs = values[4] as UserPreferences
        val tierLane = values[5] as Lane?

        val favorites = champions.filter { it.id in favIds }
        val filteredMeta =
          champions
            .filter { tierLane == null || tierLane in it.lanes }
            .sortedWith(
              compareBy<ChampionData> { it.tierRank.sortOrder }.thenByDescending { it.winRatePct }
            )

        HomeUiState(
          isLoading = champions.isEmpty(),
          activePatch = patchNotes.firstOrNull { it.isActive } ?: patchNotes.firstOrNull(),
          allPatchNotes = patchNotes,
          totalChampions = champions.size,
          totalItems = items.size,
          favoriteChampions = favorites,
          topMetaChampions = filteredMeta,
          selectedTierLane = tierLane,
          preferences = prefs,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
      )

  init {
    viewModelScope.launch { repository.ensureSeeded() }
  }

  fun selectTierLane(lane: Lane?) {
    _selectedTierLane.value = lane
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { preferencesRepository.setLanguage(language) }
  }

  fun setFontScale(multiplier: Float) {
    viewModelScope.launch { preferencesRepository.setFontScaleMultiplier(multiplier) }
  }

  fun setHighContrastCards(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setHighContrastCards(enabled) }
  }

  fun importPatchJsonUri(uri: Uri) {
    viewModelScope.launch {
      _patchActionStatus.value = PatchActionStatus.Working
      when (val result = repository.importPatchFromUri(uri)) {
        is ImportResult.Success -> {
          _validationResult.value = result.validation
          _patchActionStatus.value =
            PatchActionStatus.Message(
              isError = false,
              text =
                "Imported Patch ${result.patchVersion}: +${result.validation.championsCount} champs, +${result.validation.itemsCount} items, +${result.validation.matchupsCount} matchups.",
            )
        }
        is ImportResult.Invalid -> {
          _validationResult.value = result.validation
          _patchActionStatus.value = PatchActionStatus.Message(isError = true, text = result.message)
        }
      }
    }
  }

  fun verifyDatabaseIntegrity() {
    viewModelScope.launch {
      _patchActionStatus.value = PatchActionStatus.Working
      val result = repository.validateCurrentDatabase()
      _validationResult.value = result
      _patchActionStatus.value =
        if (result.isValid) {
          PatchActionStatus.Message(
            isError = false,
            text =
              "Integrity OK (${result.championsCount} champions, ${result.itemsCount} items, ${result.runesCount} runes, ${result.matchupsCount} rules).",
          )
        } else {
          PatchActionStatus.Message(
            isError = true,
            text = result.errors.joinToString("\n"),
          )
        }
    }
  }

  fun resetToBundledSeed() {
    viewModelScope.launch {
      _patchActionStatus.value = PatchActionStatus.Working
      when (val result = repository.resetToBundledSeed()) {
        is ImportResult.Success -> {
          _validationResult.value = result.validation
          _patchActionStatus.value =
            PatchActionStatus.Message(
              isError = false,
              text = "Restored bundled Patch ${result.patchVersion} dataset.",
            )
        }
        is ImportResult.Invalid -> {
          _patchActionStatus.value = PatchActionStatus.Message(isError = true, text = result.message)
        }
      }
    }
  }

  fun clearPatchStatus() {
    _patchActionStatus.value = PatchActionStatus.Idle
  }

  fun getSampleSchemaJson(): String = repository.buildSamplePatchPackTemplateJson()

  class Factory(
    private val repository: GameDataRepository,
    private val preferencesRepository: PreferencesRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      HomeAndPatchViewModel(repository, preferencesRepository) as T
  }
}
