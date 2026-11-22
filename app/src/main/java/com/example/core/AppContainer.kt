package com.example.core

import android.content.Context
import com.example.capture.QuickScanCoordinator
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.LiveCoachVisionService
import com.example.data.ai.ScreenChampionScanner
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.ChampionRole
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.ChampionScanMatcher
import com.example.domain.engine.DraftAssistantEngine
import com.example.domain.engine.MatchupCounterEngine
import com.example.domain.engine.OfflineCoachEngine
import com.example.domain.engine.SmartItemEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveMatchupContext(
  val myChampionId: String? = null,
  val lane: Lane = Lane.BARON,
  val enemyIds: List<String> = emptyList(),
  val externalUpdateSeq: Long = 0L,
)

data class ActiveDraftContext(
  val allyIds: List<String> = emptyList(),
  val enemyIds: List<String> = emptyList(),
  val intendedLane: Lane = Lane.MID,
  val preferredRole: ChampionRole? = null,
  val externalUpdateSeq: Long = 0L,
)

class AppContainer(context: Context) {
  val appContext: Context = context.applicationContext
  private val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

  val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(appContext) }

  val gameDataRepository: GameDataRepository by lazy {
    GameDataRepository(
      context = appContext,
      dao = database.gameDao(),
      preferencesRepository = preferencesRepository,
    )
  }

  val matchupCounterEngine: MatchupCounterEngine by lazy { MatchupCounterEngine() }
  val smartItemEngine: SmartItemEngine by lazy { SmartItemEngine() }
  val draftAssistantEngine: DraftAssistantEngine by lazy { DraftAssistantEngine() }
  val offlineCoachEngine: OfflineCoachEngine by lazy { OfflineCoachEngine() }
  val geminiCoachService: GeminiCoachService by lazy { GeminiCoachService(appContext) }
  val championScanMatcher: ChampionScanMatcher by lazy { ChampionScanMatcher() }
  val screenChampionScanner: ScreenChampionScanner by lazy {
    ScreenChampionScanner(appContext, championScanMatcher)
  }
  val liveCoachVisionService: LiveCoachVisionService by lazy {
    LiveCoachVisionService(appContext, championScanMatcher)
  }
  val quickScanCoordinator: QuickScanCoordinator by lazy {
    QuickScanCoordinator(
      context = appContext,
      repository = gameDataRepository,
      preferencesRepository = preferencesRepository,
      scanner = screenChampionScanner,
      onApplyDetectedRoster = { myChampId, lane, allyIds, enemyIds ->
        applyDetectedRoster(
          myChampionId = myChampId,
          lane = lane,
          allyIds = allyIds,
          enemyIds = enemyIds,
        )
      },
    )
  }

  private val _sharedMatchupContext = MutableStateFlow(ActiveMatchupContext())
  val sharedMatchupContext: StateFlow<ActiveMatchupContext> = _sharedMatchupContext.asStateFlow()

  private val _sharedDraftContext = MutableStateFlow(ActiveDraftContext())
  val sharedDraftContext: StateFlow<ActiveDraftContext> = _sharedDraftContext.asStateFlow()

  private var seqCounter = 0L

  @Synchronized
  fun updateSharedMatchupContext(
    myChampionId: String?,
    lane: Lane,
    enemyIds: List<String>,
    isExternal: Boolean = true,
  ) {
    val nextSeq = if (isExternal) ++seqCounter else _sharedMatchupContext.value.externalUpdateSeq
    _sharedMatchupContext.value =
      ActiveMatchupContext(
        myChampionId = myChampionId,
        lane = lane,
        enemyIds = enemyIds.distinct().take(5),
        externalUpdateSeq = nextSeq,
      )
  }

  @Synchronized
  fun applyDetectedRoster(
    myChampionId: String?,
    lane: Lane,
    allyIds: List<String>,
    enemyIds: List<String>,
  ) {
    val nextSeq = ++seqCounter
    val cleanAllies = allyIds.distinct().take(5)
    val cleanEnemies = enemyIds.filter { it !in cleanAllies }.distinct().take(5)
    val effectiveMyChamp =
      myChampionId?.takeIf { it !in cleanEnemies } ?: cleanAllies.firstOrNull()

    _sharedMatchupContext.value =
      ActiveMatchupContext(
        myChampionId = effectiveMyChamp,
        lane = lane,
        enemyIds = cleanEnemies,
        externalUpdateSeq = nextSeq,
      )
    _sharedDraftContext.value =
      ActiveDraftContext(
        allyIds = cleanAllies,
        enemyIds = cleanEnemies,
        intendedLane = lane,
        preferredRole = null,
        externalUpdateSeq = nextSeq,
      )
  }

  companion object {
    @Volatile private var instance: AppContainer? = null

    fun getInstance(context: Context): AppContainer =
      instance
        ?: synchronized(this) {
          instance ?: AppContainer(context.applicationContext).also { instance = it }
        }
  }
}
