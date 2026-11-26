package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.capture.CaptureTarget
import com.example.core.AppContainer
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiProviderId
import com.example.data.ai.ApiKeyTestResult
import com.example.data.ai.CoachErrorKind
import com.example.data.ai.CoachSpeechController
import com.example.data.ai.CoachSpeechState
import com.example.data.ai.CoachStreamEvent
import com.example.data.ai.ExternalApiKeyStatus
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.LiveCoachAnalysisResult
import com.example.data.ai.LiveCoachOutcome
import com.example.data.ai.LiveCoachVisionService
import com.example.data.local.CoachCacheStats
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.CoachPlaybookSection
import com.example.domain.engine.MatchJournalAggregator
import com.example.domain.engine.OfflineCoachEngine
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GeminiUiState {
  data object Idle : GeminiUiState

  data class Loading(val modelLabel: String = "") : GeminiUiState

  data class Streaming(val text: String, val modelLabel: String = "") : GeminiUiState

  data class Success(val text: String) : GeminiUiState

  data class Error(
    val message: String,
    val errorKind: CoachErrorKind = CoachErrorKind.OTHER,
    val failedModelId: String = "",
    val suggestedFallbackModelId: String? = null,
  ) : GeminiUiState
}

data class ProviderKeyTestState(
  val isTesting: Boolean = false,
  val lastResult: ApiKeyTestResult? = null,
)

data class CoachChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val modelId: String = "",
  val modelLabel: String = "",
  val fromCache: Boolean = false,
  val isFallbackUsed: Boolean = false,
  val questionText: String = "",
)

data class CoachQuickPrompt(
  val id: String,
  val shortLabel: String,
  val fullQuestion: String,
)

data class LiveCoachUiState(
  val isAnalyzing: Boolean = false,
  val countdownSeconds: Int? = null,
  val previewBitmap: Bitmap? = null,
  val analysis: LiveCoachAnalysisResult? = null,
  val autoSpeakLive: Boolean = false,
  val errorMessage: String? = null,
)

data class CoachUiState(
  val allChampions: List<ChampionData> = emptyList(),
  val selectedChampion: ChampionData? = null,
  val selectedLane: Lane = Lane.MID,
  val enemyContext: List<ChampionData> = emptyList(),
  val offlinePlaybook: List<CoachPlaybookSection> = emptyList(),
  val quickPrompts: List<CoachQuickPrompt> = emptyList(),
  val activePatchVersion: String = "7.3g",
  val language: AppLanguage = AppLanguage.TH,
  val selectedCoachModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  val selectedLiveModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  val keyStatus: ExternalApiKeyStatus = ExternalApiKeyStatus(),
  val cacheStats: CoachCacheStats = CoachCacheStats(),
  val cooldownModelIds: Set<String> = emptySet(),
  val playerMemorySummary: String = "",
  val journalGameCount: Int = 0,
)

class CoachViewModel(
  private val appContext: Context,
  private val repository: GameDataRepository,
  private val preferencesRepository: PreferencesRepository,
  private val offlineCoachEngine: OfflineCoachEngine,
  private val geminiCoachService: GeminiCoachService,
  private val liveCoachVisionService: LiveCoachVisionService,
  private val appContainer: AppContainer,
) : ViewModel() {

  private val speechController = CoachSpeechController(appContext)
  val speechState: StateFlow<CoachSpeechState> = speechController.state

  private val _selectedChampId = MutableStateFlow<String?>(null)
  private val _selectedLane = MutableStateFlow(Lane.MID)
  private val _enemyIds = MutableStateFlow<List<String>>(emptyList())

  private val _geminiState = MutableStateFlow<GeminiUiState>(GeminiUiState.Idle)
  val geminiState: StateFlow<GeminiUiState> = _geminiState.asStateFlow()

  private val _messages = MutableStateFlow<List<CoachChatMessage>>(emptyList())
  val messages: StateFlow<List<CoachChatMessage>> = _messages.asStateFlow()

  private val _liveState = MutableStateFlow(LiveCoachUiState())
  val liveState: StateFlow<LiveCoachUiState> = _liveState.asStateFlow()

  private val _keyTestStates = MutableStateFlow<Map<AiProviderId, ProviderKeyTestState>>(emptyMap())
  val keyTestStates: StateFlow<Map<AiProviderId, ProviderKeyTestState>> = _keyTestStates.asStateFlow()

  private var streamJob: Job? = null
  private var liveJob: Job? = null
  private var liveCountdownJob: Job? = null
  private var lastQuestion: String? = null
  private var messageSeq = 0L
  private var lastAppliedExternalSeq = 0L

  val uiState: StateFlow<CoachUiState> =
    combine(
        repository.champions,
        preferencesRepository.preferencesFlow,
        _selectedChampId,
        _selectedLane,
        _enemyIds,
        appContainer.secureApiKeyStore.keyStatusFlow,
        appContainer.gameDao.observeCoachCacheStats(),
        geminiCoachService.cooldownModelIds,
        repository.matchJournal,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val champions = values[0] as List<ChampionData>
        val prefs = values[1] as com.example.data.local.UserPreferences
        val champId = values[2] as String?
        val lane = values[3] as Lane
        @Suppress("UNCHECKED_CAST")
        val enemyIds = values[4] as List<String>
        val keyStatus = values[5] as ExternalApiKeyStatus
        val cacheStats = values[6] as CoachCacheStats
        @Suppress("UNCHECKED_CAST")
        val cooldowns = values[7] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val journalEntries = values[8] as List<MatchJournalEntryData>

        val map = champions.associateBy { it.id }
        val selected = map[champId] ?: champions.firstOrNull()
        val enemies = enemyIds.mapNotNull { map[it] }
        val playbook =
          if (selected != null) {
            offlineCoachEngine.buildPlaybook(selected, lane, enemies)
          } else emptyList()
        val prompts = buildQuickPrompts(selected, lane, enemies, prefs.language)
        val playerMemory =
          MatchJournalAggregator.buildCoachPlayerMemory(journalEntries, map, selected?.id)

        CoachUiState(
          allChampions = champions,
          selectedChampion = selected,
          selectedLane = lane,
          enemyContext = enemies,
          offlinePlaybook = playbook,
          quickPrompts = prompts,
          activePatchVersion = prefs.activePatchLabel,
          language = prefs.language,
          selectedCoachModelId = prefs.selectedCoachModelId,
          selectedLiveModelId = prefs.selectedLiveModelId,
          keyStatus = keyStatus,
          cacheStats = cacheStats,
          cooldownModelIds = cooldowns,
          playerMemorySummary = playerMemory,
          journalGameCount = journalEntries.size,
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
        if (ctx.externalUpdateSeq > lastAppliedExternalSeq || ctx.myChampionId != null) {
          if (ctx.externalUpdateSeq > lastAppliedExternalSeq) {
            lastAppliedExternalSeq = ctx.externalUpdateSeq
          }
          if (ctx.myChampionId != null) {
            _selectedChampId.value = ctx.myChampionId
          }
          _selectedLane.value = ctx.lane
          _enemyIds.value = ctx.enemyIds
        }
      }
    }

    viewModelScope.launch {
      appContainer.quickScanCoordinator.liveCoachFrames.collect { bitmap ->
        analyzeLiveBitmap(bitmap)
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

  fun selectCoachModel(modelId: String) {
    viewModelScope.launch { preferencesRepository.setSelectedCoachModelId(modelId) }
  }

  fun selectLiveModel(modelId: String) {
    viewModelScope.launch { preferencesRepository.setSelectedLiveModelId(modelId) }
  }

  fun saveAndTestApiKey(provider: AiProviderId, rawKey: String) {
    val lang = uiState.value.language
    viewModelScope.launch {
      _keyTestStates.update { it + (provider to ProviderKeyTestState(isTesting = true)) }
      val result = appContainer.externalAiClient.testProviderKey(provider, rawKey, lang)
      if (result.isValid) {
        appContainer.secureApiKeyStore.saveKey(provider, rawKey)
      }
      _keyTestStates.update {
        it + (provider to ProviderKeyTestState(isTesting = false, lastResult = result))
      }
    }
  }

  fun clearApiKey(provider: AiProviderId) {
    appContainer.secureApiKeyStore.clearKey(provider)
    _keyTestStates.update { it - provider }
    val currentTextSpec = AiModelCatalog.findById(uiState.value.selectedCoachModelId)
    if (currentTextSpec.provider == provider) {
      selectCoachModel(AiModelCatalog.AUTO_MODEL_ID)
    }
    val currentLiveSpec = AiModelCatalog.findById(uiState.value.selectedLiveModelId)
    if (currentLiveSpec.provider == provider) {
      selectLiveModel(AiModelCatalog.AUTO_MODEL_ID)
    }
  }

  fun clearKeyTestStatus(provider: AiProviderId) {
    _keyTestStates.update { it - provider }
  }

  fun clearAiCache() {
    viewModelScope.launch { geminiCoachService.clearPersistentCache() }
  }

  fun toggleSpeak(messageId: String, text: String) {
    speechController.toggleSpeak(messageId, text, uiState.value.language)
  }

  fun stopSpeaking() {
    speechController.stop()
  }

  fun clearSpeechError() {
    speechController.clearError()
  }

  fun setLiveModeActive(active: Boolean) {
    appContainer.quickScanCoordinator.setCaptureTarget(
      if (active) CaptureTarget.LIVE_COACH else CaptureTarget.QUICK_SCAN_SHEET
    )
  }

  fun reportLiveError(message: String) {
    _liveState.update { it.copy(isAnalyzing = false, countdownSeconds = null, errorMessage = message) }
  }

  fun clearLiveError() {
    _liveState.update { it.copy(errorMessage = null) }
  }

  fun toggleAutoSpeakLive() {
    _liveState.update { it.copy(autoSpeakLive = !it.autoSpeakLive) }
  }

  fun startLiveCaptureCountdown(seconds: Int = 3, onFireCapture: () -> Unit) {
    liveCountdownJob?.cancel()
    liveCountdownJob =
      viewModelScope.launch {
        for (sec in seconds downTo 1) {
          _liveState.update { it.copy(countdownSeconds = sec, errorMessage = null) }
          delay(1000L)
        }
        _liveState.update { it.copy(countdownSeconds = null) }
        onFireCapture()
      }
  }

  fun cancelLiveCountdown() {
    liveCountdownJob?.cancel()
    _liveState.update { it.copy(countdownSeconds = null) }
  }

  fun analyzeLiveUri(uri: Uri) {
    liveJob?.cancel()
    liveJob =
      viewModelScope.launch {
        val thumb = withContext(Dispatchers.IO) { decodePreviewThumbnail(uri) }
        _liveState.update {
          it.copy(
            isAnalyzing = true,
            countdownSeconds = null,
            previewBitmap = thumb ?: it.previewBitmap,
            errorMessage = null,
          )
        }
        repository.ensureSeeded()
        val current = uiState.value
        val catalog = current.allChampions.ifEmpty { repository.champions.first() }
        val outcome =
          liveCoachVisionService.analyzeUri(
            uri = uri,
            catalog = catalog,
            fallbackChampion = current.selectedChampion,
            fallbackLane = current.selectedLane,
            patchVersion = current.activePatchVersion,
            language = current.language,
            preferredModelId = current.selectedLiveModelId,
          )
        handleLiveOutcome(outcome, current.language)
      }
  }

  fun analyzeLiveBitmap(bitmap: Bitmap) {
    liveJob?.cancel()
    liveJob =
      viewModelScope.launch {
        val thumb = withContext(Dispatchers.Default) { createThumbnail(bitmap) }
        _liveState.update {
          it.copy(
            isAnalyzing = true,
            countdownSeconds = null,
            previewBitmap = thumb,
            errorMessage = null,
          )
        }
        repository.ensureSeeded()
        val current = uiState.value
        val catalog = current.allChampions.ifEmpty { repository.champions.first() }
        val outcome =
          liveCoachVisionService.analyzeBitmap(
            sourceBitmap = bitmap,
            catalog = catalog,
            fallbackChampion = current.selectedChampion,
            fallbackLane = current.selectedLane,
            patchVersion = current.activePatchVersion,
            language = current.language,
            preferredModelId = current.selectedLiveModelId,
          )
        handleLiveOutcome(outcome, current.language)
      }
  }

  fun speakLiveBriefing() {
    val analysis = _liveState.value.analysis ?: return
    val text = buildLiveSpeechText(analysis)
    if (text.isNotBlank()) {
      speechController.toggleSpeak("live_briefing", text, uiState.value.language)
    }
  }

  private fun handleLiveOutcome(outcome: LiveCoachOutcome, language: AppLanguage) {
    when (outcome) {
      is LiveCoachOutcome.Success -> {
        val res = outcome.result
        if (res.allies.isNotEmpty() || res.enemies.isNotEmpty()) {
          appContainer.applyDetectedRoster(
            myChampionId = res.myChampion?.id,
            lane = res.detectedLane,
            allyIds = res.allies.map { it.id },
            enemyIds = res.enemies.map { it.id },
          )
        }
        _liveState.update {
          it.copy(
            isAnalyzing = false,
            analysis = res,
            errorMessage = null,
          )
        }
        if (_liveState.value.autoSpeakLive) {
          val text = buildLiveSpeechText(res)
          if (text.isNotBlank()) {
            speechController.speak("live_briefing", text, language)
          }
        }
      }
      is LiveCoachOutcome.Failure -> {
        _liveState.update {
          it.copy(
            isAnalyzing = false,
            errorMessage = outcome.message,
          )
        }
      }
    }
  }

  private fun buildLiveSpeechText(analysis: LiveCoachAnalysisResult): String {
    if (analysis.spokenBriefing.isNotBlank()) return analysis.spokenBriefing
    return buildString {
      if (analysis.headline.isNotBlank()) append(analysis.headline).append(". ")
      if (analysis.recommendedPicks.isNotEmpty()) {
        append(analysis.recommendedPicks.joinToString(", ") { "${it.championName}: ${it.reason}" })
          .append(". ")
      }
      if (analysis.recommendedItems.isNotEmpty()) {
        append(analysis.recommendedItems.joinToString(", ")).append(". ")
      }
      if (analysis.earlyLanePlan.isNotBlank()) append(analysis.earlyLanePlan)
    }.trim()
  }

  fun askGeminiCoach(question: String, bypassCache: Boolean = false) {
    val trimmed = question.trim()
    if (trimmed.isBlank()) return
    lastQuestion = trimmed
    val userMsgId = "msg_${++messageSeq}"
    _messages.value = _messages.value + CoachChatMessage(id = userMsgId, isUser = true, text = trimmed)
    launchCoachRequest(trimmed, bypassCache = bypassCache)
  }

  fun regenerateQuestion(question: String) {
    val trimmed = question.trim()
    if (trimmed.isBlank()) return
    lastQuestion = trimmed
    launchCoachRequest(trimmed, bypassCache = true)
  }

  fun retryLastQuestion() {
    val q = lastQuestion ?: _messages.value.lastOrNull { it.isUser }?.text ?: return
    launchCoachRequest(q, bypassCache = false)
  }

  fun switchModelAndRetry(modelId: String) {
    viewModelScope.launch {
      preferencesRepository.setSelectedCoachModelId(modelId)
      val q = lastQuestion ?: _messages.value.lastOrNull { it.isUser }?.text
      if (q != null) {
        launchCoachRequest(q, bypassCache = false, overrideModelId = modelId)
      }
    }
  }

  fun askMatchFollowUp(entry: MatchJournalEntryData) {
    _selectedChampId.value = entry.championId
    _selectedLane.value = entry.lane
    _enemyIds.value = entry.enemyChampionIds
    appContainer.updateSharedMatchupContext(
      myChampionId = entry.championId,
      lane = entry.lane,
      enemyIds = entry.enemyChampionIds,
      isExternal = false,
    )
    val state = uiState.value
    val champName =
      state.allChampions.firstOrNull { it.id == entry.championId }?.displayName(state.language)
        ?: entry.championId
    val prompt =
      if (state.language == AppLanguage.TH) {
        val resTag = if (entry.isWin) "ชนะ" else "แพ้"
        "เพิ่งจบเกม $champName ($resTag ${entry.kills}/${entry.deaths}/${entry.assists}, Build Accuracy ${entry.buildAccuracyPct}%) " +
          "อยากให้ช่วยวางแผนแก้จุดพลาดเกมหน้า โดยเฉพาะช่วงต้นเกมและลำดับไอเทมครับ"
      } else {
        val resTag = if (entry.isWin) "WIN" else "LOSS"
        "Review my recent $champName match ($resTag ${entry.kills}/${entry.deaths}/${entry.assists}, Build Accuracy ${entry.buildAccuracyPct}%). " +
          "Give me concrete fixes for laning and itemization next game."
      }
    askGeminiCoach(prompt)
  }

  private fun launchCoachRequest(
    question: String,
    bypassCache: Boolean,
    overrideModelId: String? = null,
  ) {
    val current = uiState.value
    val targetModelId = overrideModelId ?: current.selectedCoachModelId
    val loadingLabel = AiModelCatalog.findById(targetModelId).displayName
    streamJob?.cancel()
    _geminiState.value = GeminiUiState.Loading(modelLabel = loadingLabel)

    streamJob =
      viewModelScope.launch {
        geminiCoachService
          .streamCoachingAdvice(
            question = question,
            champion = current.selectedChampion,
            lane = current.selectedLane,
            enemies = current.enemyContext,
            patchVersion = current.activePatchVersion,
            language = current.language,
            preferredModelId = targetModelId,
            bypassCache = bypassCache,
            playerMemorySummary = current.playerMemorySummary,
          )
          .collect { event ->
            when (event) {
              is CoachStreamEvent.Chunk -> {
                _geminiState.value =
                  GeminiUiState.Streaming(
                    text = event.accumulatedText,
                    modelLabel = event.modelLabel,
                  )
              }
              is CoachStreamEvent.Completed -> {
                _geminiState.value = GeminiUiState.Success(event.finalText)
                val botMsgId = "msg_${++messageSeq}"
                _messages.value =
                  _messages.value +
                    CoachChatMessage(
                      id = botMsgId,
                      isUser = false,
                      text = event.finalText,
                      modelId = event.modelId,
                      modelLabel = event.modelLabel,
                      fromCache = event.fromCache,
                      isFallbackUsed = event.isFallbackUsed,
                      questionText = question,
                    )
              }
              is CoachStreamEvent.Error -> {
                _geminiState.value =
                  GeminiUiState.Error(
                    message = event.message,
                    errorKind = event.kind,
                    failedModelId = event.failedModelId,
                    suggestedFallbackModelId = event.suggestedFallbackModelId,
                  )
              }
            }
          }
      }
  }

  fun clearChat() {
    streamJob?.cancel()
    speechController.stop()
    lastQuestion = null
    _messages.value = emptyList()
    _geminiState.value = GeminiUiState.Idle
  }

  override fun onCleared() {
    speechController.shutdown()
    super.onCleared()
  }

  private fun decodePreviewThumbnail(uri: Uri): Bitmap? {
    return try {
      val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
      appContext.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
      }?.let { createThumbnail(it) }
    } catch (_: Exception) {
      null
    }
  }

  private fun createThumbnail(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    if (maxSide <= 640 || maxSide <= 0) return source
    val ratio = 640f / maxSide.toFloat()
    val w = (source.width * ratio).roundToInt().coerceAtLeast(1)
    val h = (source.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
  }

  companion object {
    internal fun buildQuickPrompts(
      champion: ChampionData?,
      lane: Lane,
      enemies: List<ChampionData>,
      language: AppLanguage,
    ): List<CoachQuickPrompt> {
      val champName = champion?.displayName(language)
      val topEnemy = enemies.firstOrNull()?.displayName(language)

      return if (language == AppLanguage.TH) {
        val champTag = champName ?: "ตัวเรา"
        val laneTag = lane.labelTh
        listOf(
          CoachQuickPrompt(
            id = "trade_lv1_3",
            shortLabel = "เทรด Lv 1–3 & คุมเวฟ",
            fullQuestion =
              "เล่น $champTag เลน$laneTag ช่วง Lv 1–3 ควรอัพสกิล คุมเวฟ และหาจังหวะเทรดยังไงให้ได้เปรียบ?",
          ),
          CoachQuickPrompt(
            id = "rush_item_enchant",
            shortLabel = "ไอเทมแรก & Enchant",
            fullQuestion =
              "เกมนี้ $champTag ควรออกชิ้นแรก รองเท้า และ Boot Enchant (เช่น Stasis/QSS) ชิ้นไหนก่อนเพราะอะไร?",
          ),
          CoachQuickPrompt(
            id = "counter_threat",
            shortLabel = if (topEnemy != null) "แก้ทาง $topEnemy" else "แก้ทางศัตรูตัวหลัก",
            fullQuestion =
              if (topEnemy != null) {
                "วิธีรับมือ $topEnemy และทีมศัตรูเมื่อเล่น $champTag ต้องระวังสกิลไหนและออกไอเทมแก้ทางอะไร?"
              } else {
                "เวลาเจอตัวเบิร์สหรือฮีลแรงในเลน$laneTag $champTag ควรปรับบิลด์และจังหวะเข้าไฟต์ยังไง?"
              },
          ),
          CoachQuickPrompt(
            id = "objective_setup",
            shortLabel = "ไฟต์มังกร / บารอน",
            fullQuestion =
              "จังหวะชิงมังกร/Herald นาที 5:00 และบารอน $champTag ควรยืนตำแหน่งตรงไหนและเปิดไฟต์เมื่อไหร่?",
          ),
          CoachQuickPrompt(
            id = "play_behind",
            shortLabel = "คัมแบ็กตอนเสียเปรียบ",
            fullQuestion =
              "ถ้าช่วงต้นเกม $champTag เสียเปรียบหรือโดนกดเลน ต้องปรับวิธีฟาร์ม ออกของ และเดินเกมยังไงให้กลับมาชนะไฟต์ได้?",
          ),
        )
      } else {
        val champTag = champName ?: "my champion"
        val laneTag = lane.labelEn
        listOf(
          CoachQuickPrompt(
            id = "trade_lv1_3",
            shortLabel = "Lv 1–3 Trade & Wave",
            fullQuestion =
              "Playing $champTag in $laneTag, how should I manage the wave and trade levels 1–3?",
          ),
          CoachQuickPrompt(
            id = "rush_item_enchant",
            shortLabel = "First Item & Enchant",
            fullQuestion =
              "For $champTag in this match, what first core item, boots, and boot enchant (Stasis/QSS/etc.) should I rush?",
          ),
          CoachQuickPrompt(
            id = "counter_threat",
            shortLabel = if (topEnemy != null) "Counter $topEnemy" else "Counter Top Threat",
            fullQuestion =
              if (topEnemy != null) {
                "How do I counter $topEnemy and the enemy comp with $champTag, including key cooldowns and counter items?"
              } else {
                "Against heavy burst or healing in $laneTag, how should $champTag adapt items and positioning?"
              },
          ),
          CoachQuickPrompt(
            id = "objective_setup",
            shortLabel = "Dragon / Baron Setup",
            fullQuestion =
              "How should $champTag position and play around 5:00 Dragon/Herald and late Baron fights?",
          ),
          CoachQuickPrompt(
            id = "play_behind",
            shortLabel = "Play From Behind",
            fullQuestion =
              "If $champTag falls behind early, how do I farm safely, build budget defense, and look for comeback picks?",
          ),
        )
      }
    }
  }

  class Factory(
    private val appContext: Context,
    private val repository: GameDataRepository,
    private val preferencesRepository: PreferencesRepository,
    private val offlineCoachEngine: OfflineCoachEngine,
    private val geminiCoachService: GeminiCoachService,
    private val liveCoachVisionService: LiveCoachVisionService,
    private val appContainer: AppContainer,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      CoachViewModel(
        appContext,
        repository,
        preferencesRepository,
        offlineCoachEngine,
        geminiCoachService,
        liveCoachVisionService,
        appContainer,
      ) as T
  }
}
