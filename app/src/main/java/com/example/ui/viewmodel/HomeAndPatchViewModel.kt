package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiCoachService
import com.example.data.local.CoachCacheStats
import com.example.data.local.GameDao
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.PatchNoteData
import com.example.data.model.PatchSnapshotSummary
import com.example.data.remote.OtaCheckStatus
import com.example.data.remote.OtaPatchRepository
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
import com.example.data.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
  private val gameDao: GameDao? = null,
  private val geminiCoachService: GeminiCoachService? = null,
  private val otaPatchRepository: OtaPatchRepository? = null,
) : ViewModel() {

  private val _selectedTierLane = MutableStateFlow<Lane?>(null)
  private val _patchActionStatus = MutableStateFlow<PatchActionStatus>(PatchActionStatus.Idle)
  val patchActionStatus: StateFlow<PatchActionStatus> = _patchActionStatus.asStateFlow()

  private val _validationResult = MutableStateFlow<ValidationResult?>(null)
  val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

  private val _otaStatus = MutableStateFlow<OtaCheckStatus>(OtaCheckStatus.Idle)
  val otaStatus: StateFlow<OtaCheckStatus> = _otaStatus.asStateFlow()

  private var startupOtaChecked = false

  val preferences: StateFlow<UserPreferences> =
    preferencesRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserPreferences(),
    )

  val coachCacheStats: StateFlow<CoachCacheStats> =
    (gameDao?.observeCoachCacheStats() ?: flowOf(CoachCacheStats())).stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = CoachCacheStats(),
    )

  val patchSnapshots: StateFlow<List<PatchSnapshotSummary>> =
    repository.patchSnapshots.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
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

  fun setInGameMiniMode(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setInGameMiniMode(enabled) }
  }

  fun clearCoachCache() {
    viewModelScope.launch {
      geminiCoachService?.clearPersistentCache() ?: gameDao?.clearAllCoachCache()
    }
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

  fun clearOtaStatus() {
    _otaStatus.value = OtaCheckStatus.Idle
  }

  fun runStartupOtaCheckIfDue() {
    if (startupOtaChecked) return
    val repo = otaPatchRepository ?: return
    startupOtaChecked = true
    viewModelScope.launch {
      val lang = preferencesRepository.preferencesFlow.first().language
      val res = repo.checkForUpdatesOnLaunchIfDue(lang)
      if (res != null) {
        _otaStatus.value = res
      }
    }
  }

  fun saveOtaConfig(repoSlug: String, publicKeyBase64: String, autoCheckOnLaunch: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setOtaConfig(
        repoSlug = repoSlug.trim(),
        publicKeyBase64 = publicKeyBase64.trim(),
        autoCheckOnLaunch = autoCheckOnLaunch,
      )
    }
  }

  fun checkOtaUpdates(repoSlugOverride: String? = null, publicKeyOverride: String? = null) {
    val repo = otaPatchRepository ?: return
    viewModelScope.launch {
      val lang = preferencesRepository.preferencesFlow.first().language
      if (repoSlugOverride != null || publicKeyOverride != null) {
        val current = preferencesRepository.preferencesFlow.first()
        preferencesRepository.setOtaConfig(
          repoSlug = (repoSlugOverride ?: current.otaRepoSlug).trim(),
          publicKeyBase64 = (publicKeyOverride ?: current.otaPublicKeyBase64).trim(),
          autoCheckOnLaunch = current.autoCheckOtaOnLaunch,
        )
      }
      _otaStatus.value = OtaCheckStatus.Checking
      _otaStatus.value =
        repo.checkAndVerifyLatestRelease(
          repoSlugOverride = repoSlugOverride,
          publicKeyOverride = publicKeyOverride,
          language = lang,
        )
    }
  }

  fun applyVerifiedOtaPack() {
    val repo = otaPatchRepository ?: return
    val ready = (_otaStatus.value as? OtaCheckStatus.UpdateReady)?.download ?: return
    viewModelScope.launch {
      val lang = preferencesRepository.preferencesFlow.first().language
      _otaStatus.value = OtaCheckStatus.Checking
      val outcome = repo.applyVerifiedOtaPack(ready, lang)
      _otaStatus.value = outcome
      if (outcome is OtaCheckStatus.Applied) {
        _validationResult.value = ready.validation
        _patchActionStatus.value =
          PatchActionStatus.Message(
            isError = false,
            text =
              if (lang == AppLanguage.TH) {
                "อัปเดต OTA Patch ${outcome.patchVersion} สำเร็จ (บันทึกจุด Rollback แล้ว)"
              } else {
                "Applied signed OTA Patch ${outcome.patchVersion} (Rollback snapshot saved)."
              },
          )
      }
    }
  }

  fun rollbackToPreviousPatch() {
    viewModelScope.launch {
      val lang = preferencesRepository.preferencesFlow.first().language
      _patchActionStatus.value = PatchActionStatus.Working
      when (val result = repository.rollbackToLatestSnapshot()) {
        is ImportResult.Success -> {
          _validationResult.value = result.validation
          _otaStatus.value = OtaCheckStatus.Idle
          _patchActionStatus.value =
            PatchActionStatus.Message(
              isError = false,
              text =
                if (lang == AppLanguage.TH) {
                  "ย้อนกลับเป็น Patch ${result.patchVersion} เรียบร้อย"
                } else {
                  "Rolled back cleanly to Patch ${result.patchVersion}."
                },
            )
        }
        is ImportResult.Invalid -> {
          _patchActionStatus.value =
            PatchActionStatus.Message(isError = true, text = result.message)
        }
      }
    }
  }

  fun getSampleSchemaJson(): String = repository.buildSamplePatchPackTemplateJson()

  class Factory(
    private val repository: GameDataRepository,
    private val preferencesRepository: PreferencesRepository,
    private val gameDao: GameDao? = null,
    private val geminiCoachService: GeminiCoachService? = null,
    private val otaPatchRepository: OtaPatchRepository? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      HomeAndPatchViewModel(
        repository = repository,
        preferencesRepository = preferencesRepository,
        gameDao = gameDao,
        geminiCoachService = geminiCoachService,
        otaPatchRepository = otaPatchRepository,
      ) as T
  }
}
