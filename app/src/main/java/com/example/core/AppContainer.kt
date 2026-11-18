package com.example.core

import android.content.Context
import com.example.data.ai.GeminiCoachService
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
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
)

class AppContainer(context: Context) {
  private val appContext = context.applicationContext
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
  val geminiCoachService: GeminiCoachService by lazy { GeminiCoachService() }

  private val _sharedMatchupContext = MutableStateFlow(ActiveMatchupContext())
  val sharedMatchupContext: StateFlow<ActiveMatchupContext> = _sharedMatchupContext.asStateFlow()

  fun updateSharedMatchupContext(
    myChampionId: String?,
    lane: Lane,
    enemyIds: List<String>,
  ) {
    _sharedMatchupContext.value = ActiveMatchupContext(myChampionId, lane, enemyIds)
  }
}
