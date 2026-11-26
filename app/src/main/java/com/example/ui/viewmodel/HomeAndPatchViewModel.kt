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
import com.example.data.model.MatchJournalEntryData
import com.example.data.model.PatchNoteData
import com.example.data.model.PatchSnapshotSummary
import com.example.data.remote.LiveVersionCheckState
import com.example.data.remote.MetaRefreshState
import com.example.data.remote.MetaServerRegion
import com.example.data.remote.OfficialPatchParser
import com.example.data.remote.OfficialPatchVersionChecker
import com.example.data.remote.OtaCheckStatus
import com.example.data.remote.OtaPatchRepository
import com.example.data.remote.ServerMetaSyncService
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
import com.example.data.validation.ValidationResult
import com.example.domain.engine.TopMetaChampionEntry
import com.example.domain.engine.TopMetaRankingEngine
import com.example.domain.engine.TopMetaSortMode
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
  val topPlayedMeta: List<TopMetaChampionEntry> = emptyList(),
  val selectedMetaSortMode: TopMetaSortMode = TopMetaSortMode.MOST_PLAYED,
  val topMetaAvgWinRatePct: Double = 0.0,
  val topMetaAvgPickRatePct: Double = 0.0,
  val selectedTierLane: Lane? = null,
  val selectedMetaServer: MetaServerRegion = MetaServerRegion.CN_DIAMOND_PLUS,
  val lastMetaSyncMs: Long = 0L,
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
  private val officialPatchChecker: OfficialPatchVersionChecker? = null,
  private val serverMetaSyncService: ServerMetaSyncService? = null,
) : ViewModel() {

  private val _selectedTierLane = MutableStateFlow<Lane?>(null)
  private val _selectedMetaSortMode = MutableStateFlow(TopMetaSortMode.MOST_PLAYED)
  private val _patchActionStatus = MutableStateFlow<PatchActionStatus>(PatchActionStatus.Idle)
  val patchActionStatus: StateFlow<PatchActionStatus> = _patchActionStatus.asStateFlow()

  private val _validationResult = MutableStateFlow<ValidationResult?>(null)
  val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

  private val _otaStatus = MutableStateFlow<OtaCheckStatus>(OtaCheckStatus.Idle)
  val otaStatus: StateFlow<OtaCheckStatus> = _otaStatus.asStateFlow()

  private val _liveVersionState =
    MutableStateFlow<LiveVersionCheckState>(LiveVersionCheckState.Idle(appPatch = "7.3f"))
  val liveVersionState: StateFlow<LiveVersionCheckState> = _liveVersionState.asStateFlow()

  private val _metaRefreshState = MutableStateFlow<MetaRefreshState>(MetaRefreshState.Idle)
  val metaRefreshState: StateFlow<MetaRefreshState> = _metaRefreshState.asStateFlow()

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
        repository.matchJournal,
        _selectedMetaSortMode,
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
        @Suppress("UNCHECKED_CAST")
        val journalEntries = values[6] as List<MatchJournalEntryData>
        val metaSortMode = values[7] as TopMetaSortMode

        val favorites = champions.filter { it.id in favIds }
        val filteredMeta =
          champions
            .filter { tierLane == null || tierLane in it.lanes }
            .sortedWith(
              compareBy<ChampionData> { it.tierRank.sortOrder }.thenByDescending { it.winRatePct }
            )
        val topPlayedSlice =
          TopMetaRankingEngine.computeTopChampions(
            champions = champions,
            journalEntries = journalEntries,
            laneFilter = tierLane,
            sortMode = metaSortMode,
            limit = 5,
          )

        HomeUiState(
          isLoading = champions.isEmpty(),
          activePatch = patchNotes.firstOrNull { it.isActive } ?: patchNotes.firstOrNull(),
          allPatchNotes = patchNotes,
          totalChampions = champions.size,
          totalItems = items.size,
          favoriteChampions = favorites,
          topMetaChampions = filteredMeta,
          topPlayedMeta = topPlayedSlice.entries,
          selectedMetaSortMode = metaSortMode,
          topMetaAvgWinRatePct = topPlayedSlice.avgWinRatePct,
          topMetaAvgPickRatePct = topPlayedSlice.avgPickRatePct,
          selectedTierLane = tierLane,
          selectedMetaServer = MetaServerRegion.fromId(prefs.selectedMetaServerId),
          lastMetaSyncMs = prefs.lastMetaSyncMs,
          preferences = prefs,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
      )

  init {
    viewModelScope.launch {
      repository.ensureSeeded()
      val prefs = preferencesRepository.preferencesFlow.first()
      if (_liveVersionState.value is LiveVersionCheckState.Idle) {
        _liveVersionState.value =
          LiveVersionCheckState.Idle(
            appPatch = prefs.activePatchLabel,
            lastKnownOfficialPatch = prefs.lastKnownOfficialPatch.takeIf { it.isNotBlank() },
          )
      }
    }
  }

  fun selectTierLane(lane: Lane?) {
    _selectedTierLane.value = lane
  }

  fun selectMetaSortMode(mode: TopMetaSortMode) {
    _selectedMetaSortMode.value = mode
  }

  fun selectMetaServer(server: MetaServerRegion) {
    viewModelScope.launch {
      preferencesRepository.setSelectedMetaServerId(server.id)
      _metaRefreshState.value = MetaRefreshState.Idle
    }
  }

  fun checkLiveGameVersion() {
    val checker = officialPatchChecker ?: return
    viewModelScope.launch {
      val prefs = preferencesRepository.preferencesFlow.first()
      val appPatch = prefs.activePatchLabel.ifBlank { homeUiState.value.activePatch?.version ?: "7.3f" }
      val lang = prefs.language
      _liveVersionState.value = LiveVersionCheckState.Checking(appPatch)

      val officialResult = checker.fetchLatestOfficialPatch()
      if (officialResult.isFailure) {
        val msg = officialResult.exceptionOrNull()?.localizedMessage ?: "Network unavailable"
        _liveVersionState.value =
          LiveVersionCheckState.Error(
            appPatch = appPatch,
            reason =
              if (lang == AppLanguage.TH) "ตรวจสอบเวอร์ชั่นทางการไม่สำเร็จ: $msg"
              else "Failed to check official patch notes: $msg",
          )
        return@launch
      }

      val official = officialResult.getOrThrow()
      val now = System.currentTimeMillis()
      preferencesRepository.setOfficialPatchCheck(official.patchVersion, now)

      val cmp = OfficialPatchParser.comparePatchVersions(official.patchVersion, appPatch)
      if (cmp <= 0) {
        _liveVersionState.value =
          LiveVersionCheckState.Matched(
            appPatch = appPatch,
            officialPatch = official.patchVersion,
            officialUrl = official.articleUrl,
            checkedAtMs = now,
          )
      } else {
        var otaReady: com.example.data.remote.VerifiedOtaDownload? = null
        var otaHint: String? = null
        val otaRepo = otaPatchRepository
        if (otaRepo != null && otaRepo.isValidRepoSlug(prefs.otaRepoSlug) && prefs.otaPublicKeyBase64.isNotBlank()) {
          when (val otaRes = otaRepo.checkAndVerifyLatestRelease(language = lang)) {
            is OtaCheckStatus.UpdateReady -> {
              otaReady = otaRes.download
              _otaStatus.value = otaRes
            }
            is OtaCheckStatus.UpToDate -> {
              otaHint =
                if (lang == AppLanguage.TH) {
                  "OTA Repo (${otaRes.remoteTag}) ยังไม่อัปเดตเป็น Patch ${official.patchVersion}"
                } else {
                  "OTA Repo (${otaRes.remoteTag}) has not published Patch ${official.patchVersion} yet."
                }
            }
            is OtaCheckStatus.Error -> {
              otaHint = otaRes.message
            }
            else -> Unit
          }
        } else {
          otaHint =
            if (lang == AppLanguage.TH) {
              "ตั้งค่า OTA Repo ใน Patch Center หรือนำเข้าไฟล์ .json เพื่ออัปเดตเป็น ${official.patchVersion}"
            } else {
              "Configure OTA Repo in Patch Center or import a .json pack to update to ${official.patchVersion}."
            }
        }

        _liveVersionState.value =
          LiveVersionCheckState.Outdated(
            appPatch = appPatch,
            officialPatch = official.patchVersion,
            officialUrl = official.articleUrl,
            otaReady = otaReady,
            otaMessage = otaHint,
            checkedAtMs = now,
          )
      }
    }
  }

  fun applyUpdateFromLiveVersionCheck() {
    val repo = otaPatchRepository ?: return
    val outdated = _liveVersionState.value as? LiveVersionCheckState.Outdated ?: return
    val ready = outdated.otaReady ?: return
    viewModelScope.launch {
      val lang = preferencesRepository.preferencesFlow.first().language
      _liveVersionState.value = LiveVersionCheckState.Checking(outdated.appPatch)
      when (val outcome = repo.applyVerifiedOtaPack(ready, lang)) {
        is OtaCheckStatus.Applied -> {
          _otaStatus.value = outcome
          _validationResult.value = ready.validation
          val syncedCount = autoSyncServerMetaAfterPatch(outcome.patchVersion, lang)
          val now = System.currentTimeMillis()
          val cmp = OfficialPatchParser.comparePatchVersions(outdated.officialPatch, outcome.patchVersion)
          val syncedSuffix =
            if (syncedCount > 0) {
              if (lang == AppLanguage.TH) " • ซิงค์ Winrate/Tier ${syncedCount} แชมป์"
              else " • Synced $syncedCount champ stats"
            } else ""
          _liveVersionState.value =
            if (cmp <= 0) {
              LiveVersionCheckState.Matched(
                appPatch = outcome.patchVersion,
                officialPatch = outdated.officialPatch,
                officialUrl = outdated.officialUrl,
                checkedAtMs = now,
              )
            } else {
              LiveVersionCheckState.Outdated(
                appPatch = outcome.patchVersion,
                officialPatch = outdated.officialPatch,
                officialUrl = outdated.officialUrl,
                otaReady = null,
                otaMessage =
                  if (lang == AppLanguage.TH) {
                    "อัปเดตเป็น ${outcome.patchVersion} แล้ว$syncedSuffix (ทางการอยู่ที่ ${outdated.officialPatch})"
                  } else {
                    "Updated to ${outcome.patchVersion}$syncedSuffix (Live game is ${outdated.officialPatch})."
                  },
                checkedAtMs = now,
              )
            }
        }
        is OtaCheckStatus.Error -> {
          _liveVersionState.value =
            LiveVersionCheckState.Error(appPatch = outdated.appPatch, reason = outcome.message)
        }
        else -> Unit
      }
    }
  }

  private suspend fun autoSyncServerMetaAfterPatch(appliedPatch: String, lang: AppLanguage): Int {
    val service = serverMetaSyncService ?: return 0
    return runCatching {
      val champs = repository.champions.first()
      val result = service.fetchCnRankedStats(champs)
      if (result.isSuccess) {
        val payload = result.getOrThrow()
        val updated = repository.updateChampionMetaMetrics(payload.updates, patchLabelOverride = appliedPatch)
        val now = System.currentTimeMillis()
        preferencesRepository.setMetaSyncTimestamp(now)
        _metaRefreshState.value =
          MetaRefreshState.Synced(
            server = MetaServerRegion.CN_DIAMOND_PLUS,
            patchVersion = appliedPatch,
            updatedCount = updated,
            syncedAtMs = now,
            note =
              if (lang == AppLanguage.TH) {
                "ซิงค์ Winrate & Tier List (${updated} แชมป์) ตามแพทช์ $appliedPatch แล้ว"
              } else {
                "Auto-synced Winrate & Tier List ($updated champs) for Patch $appliedPatch"
              },
          )
        updated
      } else {
        0
      }
    }.getOrDefault(0)
  }

  private suspend fun syncLiveVersionStateToPatch(appliedPatch: String) {
    val current = _liveVersionState.value
    val prefs = preferencesRepository.preferencesFlow.first()
    val knownOfficial =
      when (current) {
        is LiveVersionCheckState.Matched -> current.officialPatch
        is LiveVersionCheckState.Outdated -> current.officialPatch
        else -> prefs.lastKnownOfficialPatch.takeIf { it.isNotBlank() }
      }
    val officialUrl =
      when (current) {
        is LiveVersionCheckState.Matched -> current.officialUrl
        is LiveVersionCheckState.Outdated -> current.officialUrl
        else -> com.example.data.remote.OfficialPatchVersionChecker.OFFICIAL_GAME_UPDATES_URL
      }
    if (knownOfficial != null && current !is LiveVersionCheckState.Idle) {
      val cmp = OfficialPatchParser.comparePatchVersions(knownOfficial, appliedPatch)
      val now = System.currentTimeMillis()
      _liveVersionState.value =
        if (cmp <= 0) {
          LiveVersionCheckState.Matched(
            appPatch = appliedPatch,
            officialPatch = knownOfficial,
            officialUrl = officialUrl,
            checkedAtMs = now,
          )
        } else {
          LiveVersionCheckState.Outdated(
            appPatch = appliedPatch,
            officialPatch = knownOfficial,
            officialUrl = officialUrl,
            otaReady = null,
            checkedAtMs = now,
          )
        }
    } else {
      _liveVersionState.value =
        LiveVersionCheckState.Idle(
          appPatch = appliedPatch,
          lastKnownOfficialPatch = knownOfficial,
        )
    }
  }

  fun refreshServerMetaStats() {
    viewModelScope.launch {
      val prefs = preferencesRepository.preferencesFlow.first()
      val server = MetaServerRegion.fromId(prefs.selectedMetaServerId)
      val lang = prefs.language
      val appPatch = prefs.activePatchLabel.ifBlank { homeUiState.value.activePatch?.version ?: "7.3f" }
      _metaRefreshState.value = MetaRefreshState.Refreshing(server)

      when (server) {
        MetaServerRegion.CN_DIAMOND_PLUS -> {
          val service = serverMetaSyncService
          if (service == null) {
            _metaRefreshState.value =
              MetaRefreshState.Failed(server, "Server sync service unavailable.")
            return@launch
          }
          val champs = repository.champions.first()
          val result = service.fetchCnRankedStats(champs)
          if (result.isSuccess) {
            val payload = result.getOrThrow()
            val updated = repository.updateChampionMetaMetrics(payload.updates, patchLabelOverride = appPatch)
            val now = System.currentTimeMillis()
            preferencesRepository.setMetaSyncTimestamp(now)
            _metaRefreshState.value =
              MetaRefreshState.Synced(
                server = server,
                patchVersion = appPatch,
                updatedCount = updated,
                syncedAtMs = now,
                note =
                  if (lang == AppLanguage.TH) {
                    "อัปเดตสถิติแรงก์ Diamond+ (${updated} แชมป์) แล้ว"
                  } else {
                    "Synced CN Diamond+ stats ($updated champions)"
                  },
              )
          } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Connection failed"
            _metaRefreshState.value =
              MetaRefreshState.Failed(
                server = server,
                message =
                  if (lang == AppLanguage.TH) "ดึงสถิติเซิร์ฟ CN ไม่สำเร็จ: $err"
                  else "CN server sync failed: $err",
              )
          }
        }

        MetaServerRegion.GLOBAL_SEA -> {
          val otaRepo = otaPatchRepository
          if (otaRepo != null && otaRepo.isValidRepoSlug(prefs.otaRepoSlug) && prefs.otaPublicKeyBase64.isNotBlank()) {
            when (val otaRes = otaRepo.checkAndVerifyLatestRelease(language = lang)) {
              is OtaCheckStatus.UpdateReady -> {
                _otaStatus.value = otaRes
                val applied = otaRepo.applyVerifiedOtaPack(otaRes.download, lang)
                if (applied is OtaCheckStatus.Applied) {
                  val now = System.currentTimeMillis()
                  preferencesRepository.setMetaSyncTimestamp(now)
                  _metaRefreshState.value =
                    MetaRefreshState.Synced(
                      server = server,
                      patchVersion = applied.patchVersion,
                      updatedCount = otaRes.download.validation.championsCount,
                      syncedAtMs = now,
                      note =
                        if (lang == AppLanguage.TH) "อัปเดต Global/SEA OTA Patch ${applied.patchVersion} แล้ว"
                        else "Applied Global/SEA OTA Patch ${applied.patchVersion}",
                    )
                } else if (applied is OtaCheckStatus.Error) {
                  _metaRefreshState.value = MetaRefreshState.Failed(server, applied.message)
                }
              }
              is OtaCheckStatus.UpToDate -> {
                val now = System.currentTimeMillis()
                preferencesRepository.setMetaSyncTimestamp(now)
                _metaRefreshState.value =
                  MetaRefreshState.Synced(
                    server = server,
                    patchVersion = otaRes.currentPatch,
                    updatedCount = repository.champions.first().size,
                    syncedAtMs = now,
                    note =
                      if (lang == AppLanguage.TH) "Global/SEA Meta ตรงกับ Patch ${otaRes.currentPatch} แล้ว"
                      else "Global/SEA Meta already on Patch ${otaRes.currentPatch}",
                  )
              }
              is OtaCheckStatus.Error -> {
                _metaRefreshState.value = MetaRefreshState.Failed(server, otaRes.message)
              }
              else -> Unit
            }
          } else {
            _metaRefreshState.value =
              MetaRefreshState.Failed(
                server = server,
                message =
                  if (lang == AppLanguage.TH) {
                    "กรุณาตั้งค่า OTA Repo ใน Patch Center หรือสลับเป็นเซิร์ฟจีน (CN) เพื่อดึงสถิติสด"
                  } else {
                    "Configure OTA Repo in Patch Center or switch to CN Official for live stats."
                  },
              )
          }
        }
      }
    }
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
      val lang = preferencesRepository.preferencesFlow.first().language
      _patchActionStatus.value = PatchActionStatus.Working
      when (val result = repository.importPatchFromUri(uri)) {
        is ImportResult.Success -> {
          _validationResult.value = result.validation
          syncLiveVersionStateToPatch(result.patchVersion)
          val syncedCount = autoSyncServerMetaAfterPatch(result.patchVersion, lang)
          val syncSuffix =
            if (syncedCount > 0) " • Synced $syncedCount champ Winrate/Tier." else ""
          _patchActionStatus.value =
            PatchActionStatus.Message(
              isError = false,
              text =
                "Imported Patch ${result.patchVersion}: +${result.validation.championsCount} champs, +${result.validation.itemsCount} items, +${result.validation.matchupsCount} matchups$syncSuffix",
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
        syncLiveVersionStateToPatch(outcome.patchVersion)
        val syncedCount = autoSyncServerMetaAfterPatch(outcome.patchVersion, lang)
        val syncNoteTh = if (syncedCount > 0) " และซิงค์ Winrate/Tier ($syncedCount แชมป์)" else ""
        val syncNoteEn = if (syncedCount > 0) " & synced $syncedCount champ stats" else ""
        _patchActionStatus.value =
          PatchActionStatus.Message(
            isError = false,
            text =
              if (lang == AppLanguage.TH) {
                "อัปเดต OTA Patch ${outcome.patchVersion} สำเร็จ$syncNoteTh (บันทึกจุด Rollback แล้ว)"
              } else {
                "Applied signed OTA Patch ${outcome.patchVersion}$syncNoteEn (Rollback snapshot saved)."
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
          syncLiveVersionStateToPatch(result.patchVersion)
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
    private val officialPatchChecker: OfficialPatchVersionChecker? = null,
    private val serverMetaSyncService: ServerMetaSyncService? = null,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      HomeAndPatchViewModel(
        repository = repository,
        preferencesRepository = preferencesRepository,
        gameDao = gameDao,
        geminiCoachService = geminiCoachService,
        otaPatchRepository = otaPatchRepository,
        officialPatchChecker = officialPatchChecker,
        serverMetaSyncService = serverMetaSyncService,
      ) as T
  }
}
