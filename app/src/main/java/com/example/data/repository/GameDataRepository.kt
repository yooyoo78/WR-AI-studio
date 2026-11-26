package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.FavoriteChampionEntity
import com.example.data.local.GameDao
import com.example.data.local.PatchSnapshotEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.SavedMatchupEntity
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.local.toSummary
import com.example.data.model.ChampionData
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchNoteData
import com.example.data.model.PatchPackPayload
import com.example.data.model.PatchSnapshotSummary
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.remote.ChampionMetaStatPatch
import com.example.data.validation.PatchDataValidator
import com.example.data.validation.ValidationResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GameDataRepository"
private const val SEED_ASSET_FILE = "patch_7_3f_seed.json"
private const val CURRENT_SEED_REVISION = 11

sealed interface ImportResult {
  data class Success(val patchVersion: String, val validation: ValidationResult) : ImportResult

  data class Invalid(val message: String, val validation: ValidationResult? = null) : ImportResult
}

class GameDataRepository(
  private val context: Context,
  private val dao: GameDao,
  private val preferencesRepository: PreferencesRepository,
  private val validator: PatchDataValidator = PatchDataValidator(),
) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val payloadAdapter = moshi.adapter(PatchPackPayload::class.java).indent("  ")

  val champions: Flow<List<ChampionData>> =
    dao.observeChampions().map { list -> list.map { it.toDomain() } }

  val items: Flow<List<ItemData>> = dao.observeItems().map { list -> list.map { it.toDomain() } }

  val runes: Flow<List<RuneData>> = dao.observeRunes().map { list -> list.map { it.toDomain() } }

  val spells: Flow<List<SpellData>> = dao.observeSpells().map { list -> list.map { it.toDomain() } }

  val matchupRules: Flow<List<MatchupRuleData>> =
    dao.observeMatchupRules().map { list -> list.map { it.toDomain() } }

  val patchNotes: Flow<List<PatchNoteData>> =
    dao.observePatchNotes().map { list -> list.map { it.toDomain() } }

  val favoriteChampionIds: Flow<Set<String>> =
    dao.observeFavoriteIds().map { list -> list.map { it.championId }.toSet() }

  val savedMatchups: Flow<List<SavedMatchupEntity>> = dao.observeSavedMatchups()

  val customBuilds: Flow<List<CustomBuildData>> =
    dao.observeCustomBuilds().map { list -> list.map { it.toDomain() } }

  val matchJournal: Flow<List<MatchJournalEntryData>> =
    dao.observeMatchJournal().map { list -> list.map { it.toDomain() } }

  val patchSnapshots: Flow<List<PatchSnapshotSummary>> =
    dao.observePatchSnapshots().map { list -> list.map { it.toSummary() } }

  suspend fun ensureSeeded() {
    withContext(Dispatchers.IO) {
      val currentRev = preferencesRepository.preferencesFlow.first().seedRevision
      val isEmpty = dao.getChampionCount() == 0
      if (isEmpty || currentRev < CURRENT_SEED_REVISION) {
        val result = seedFromAssets(replaceExisting = true)
        if (result is ImportResult.Success) {
          preferencesRepository.setSeedRevision(CURRENT_SEED_REVISION)
        }
      }
    }
  }

  suspend fun resetToBundledSeed(): ImportResult =
    withContext(Dispatchers.IO) {
      dao.clearAllGameData()
      val result = seedFromAssets(replaceExisting = true)
      if (result is ImportResult.Success) {
        preferencesRepository.setSeedRevision(CURRENT_SEED_REVISION)
      }
      result
    }

  private suspend fun seedFromAssets(replaceExisting: Boolean = true): ImportResult {
    return try {
      val json = context.assets.open(SEED_ASSET_FILE).bufferedReader().use { it.readText() }
      importPatchJson(json, replaceExisting = replaceExisting)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to seed database from assets", e)
      ImportResult.Invalid(e.localizedMessage ?: "Failed to read seed data")
    }
  }

  suspend fun importPatchFromUri(uri: Uri): ImportResult =
    withContext(Dispatchers.IO) {
      try {
        val json =
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return@withContext ImportResult.Invalid("Unable to open selected file.")
        importPatchJson(json, replaceExisting = false)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to import JSON from URI", e)
        ImportResult.Invalid(e.localizedMessage ?: "JSON file read error")
      }
    }

  suspend fun importPatchJson(json: String, replaceExisting: Boolean = false): ImportResult =
    withContext(Dispatchers.IO) {
      val payload =
        try {
          payloadAdapter.fromJson(json)
        } catch (e: Exception) {
          Log.e(TAG, "JSON syntax error during patch import", e)
          return@withContext ImportResult.Invalid("Invalid JSON format: ${e.localizedMessage}")
        } ?: return@withContext ImportResult.Invalid("Empty patch JSON payload.")

      val existingChampions =
        if (replaceExisting) emptyList() else dao.getAllChampionsOnce().map { it.toDomain() }
      val existingItems =
        if (replaceExisting) emptyList() else dao.getAllItemsOnce().map { it.toDomain() }
      val existingRunes =
        if (replaceExisting) emptyList() else dao.getAllRunesOnce().map { it.toDomain() }
      val existingSpells =
        if (replaceExisting) emptyList() else dao.getAllSpellsOnce().map { it.toDomain() }
      val existingMatchups =
        if (replaceExisting) emptyList() else dao.getAllMatchupRulesOnce().map { it.toDomain() }

      val validation =
        validator.validate(
          payload = payload,
          existingChampions = existingChampions,
          existingItems = existingItems,
          existingRunes = existingRunes,
          existingSpells = existingSpells,
          existingMatchups = existingMatchups,
        )

      if (!validation.isValid) {
        return@withContext ImportResult.Invalid(
          message = validation.errors.joinToString("\n"),
          validation = validation,
        )
      }

      if (replaceExisting) {
        dao.clearAllGameData()
      }

      dao.applyPatchPack(
        champions = payload.champions.map { it.toEntity() },
        items = payload.items.map { it.toEntity() },
        runes = payload.runes.map { it.toEntity() },
        spells = payload.spells.map { it.toEntity() },
        matchupRules = payload.matchupRules.map { it.toEntity() },
        patchNotes = payload.patchNotes.map { it.toEntity() },
        deactivateOlderNotes = !replaceExisting,
      )

      preferencesRepository.setPatchMetadata(
        patchVersion = payload.patchVersion,
        importInfo = "${payload.sourceLabel} (${payload.lastUpdated})",
      )

      ImportResult.Success(payload.patchVersion, validation)
    }

  suspend fun validateCurrentDatabase(): ValidationResult =
    withContext(Dispatchers.IO) {
      val champions = dao.getAllChampionsOnce().map { it.toDomain() }
      val items = dao.getAllItemsOnce().map { it.toDomain() }
      val runes = dao.getAllRunesOnce().map { it.toDomain() }
      val spells = dao.getAllSpellsOnce().map { it.toDomain() }
      val rules = dao.getAllMatchupRulesOnce().map { it.toDomain() }
      validator.validate(
        payload =
          PatchPackPayload(
            packVersion = 1,
            patchVersion = champions.firstOrNull()?.patchVersion ?: "7.3f",
            lastUpdated = champions.firstOrNull()?.lastUpdated ?: "2026-11-15",
            sourceLabel = "Database Integrity Check",
            champions = champions,
            items = items,
            runes = runes,
            spells = spells,
            matchupRules = rules,
          )
      )
    }

  fun buildSamplePatchPackTemplateJson(): String {
    val sample =
      PatchPackPayload(
        packVersion = 1,
        patchVersion = "7.4a",
        lastUpdated = "2026-12-01",
        sourceLabel = "Custom Patch Extension Pack",
        champions = emptyList(),
        items = emptyList(),
        runes = emptyList(),
        spells = emptyList(),
        matchupRules = emptyList(),
        patchNotes = emptyList(),
      )
    return payloadAdapter.toJson(sample)
  }

  suspend fun toggleFavorite(championId: String, currentlyFavorite: Boolean) {
    withContext(Dispatchers.IO) {
      if (currentlyFavorite) {
        dao.removeFavorite(championId)
      } else {
        dao.addFavorite(FavoriteChampionEntity(championId = championId))
      }
    }
  }

  suspend fun saveMatchupPreset(
    myChampionId: String,
    lane: Lane,
    enemyIds: List<String>,
    label: String,
  ) {
    withContext(Dispatchers.IO) {
      dao.insertSavedMatchup(
        SavedMatchupEntity(
          myChampionId = myChampionId,
          lane = lane,
          enemyIds = enemyIds,
          label = label,
        )
      )
    }
  }

  suspend fun deleteSavedMatchup(id: Long) {
    withContext(Dispatchers.IO) { dao.deleteSavedMatchup(id) }
  }

  suspend fun saveCustomBuild(build: CustomBuildData): Long =
    withContext(Dispatchers.IO) {
      dao.upsertCustomBuild(build.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

  suspend fun deleteCustomBuild(id: Long) {
    withContext(Dispatchers.IO) { dao.deleteCustomBuild(id) }
  }

  suspend fun saveMatchJournalEntry(entry: MatchJournalEntryData): Long =
    withContext(Dispatchers.IO) { dao.insertMatchJournal(entry.toEntity()) }

  suspend fun deleteMatchJournalEntry(id: Long) {
    withContext(Dispatchers.IO) { dao.deleteMatchJournal(id) }
  }

  suspend fun getRecentMatchJournal(limit: Int = 20): List<MatchJournalEntryData> =
    withContext(Dispatchers.IO) { dao.getRecentMatchJournalOnce(limit).map { it.toDomain() } }

  suspend fun buildCurrentPatchSnapshotPayload(): PatchPackPayload =
    withContext(Dispatchers.IO) {
      val prefs = preferencesRepository.preferencesFlow.first()
      val champions = dao.getAllChampionsOnce().map { it.toDomain() }
      val items = dao.getAllItemsOnce().map { it.toDomain() }
      val runes = dao.getAllRunesOnce().map { it.toDomain() }
      val spells = dao.getAllSpellsOnce().map { it.toDomain() }
      val matchups = dao.getAllMatchupRulesOnce().map { it.toDomain() }
      val notes = dao.observePatchNotes().first().map { it.toDomain() }
      PatchPackPayload(
        packVersion = 1,
        patchVersion = prefs.activePatchLabel,
        lastUpdated = champions.firstOrNull()?.lastUpdated ?: "2026-11-15",
        sourceLabel = prefs.lastImportedPackInfo.ifBlank { "Patch ${prefs.activePatchLabel}" },
        champions = champions,
        items = items,
        runes = runes,
        spells = spells,
        matchupRules = matchups,
        patchNotes = notes,
      )
    }

  suspend fun capturePatchRollbackSnapshot() {
    withContext(Dispatchers.IO) {
      val current = buildCurrentPatchSnapshotPayload()
      val json = payloadAdapter.toJson(current)
      dao.insertPatchSnapshot(
        PatchSnapshotEntity(
          patchVersion = current.patchVersion,
          sourceLabel = current.sourceLabel,
          createdAt = System.currentTimeMillis(),
          payloadJson = json,
          isRolledBack = false,
        )
      )
      dao.trimPatchSnapshots(keepLimit = 5)
    }
  }

  suspend fun rollbackToLatestSnapshot(): ImportResult =
    withContext(Dispatchers.IO) {
      val snap =
        dao.getLatestRollbackSnapshot()
          ?: return@withContext ImportResult.Invalid("No saved patch snapshot available for rollback.")
      val res = importPatchJson(snap.payloadJson, replaceExisting = true)
      if (res is ImportResult.Success) {
        dao.markSnapshotRolledBack(snap.id)
      }
      res
    }

  suspend fun updateChampionMetaMetrics(
    updates: List<ChampionMetaStatPatch>,
    patchLabelOverride: String? = null,
  ): Int =
    withContext(Dispatchers.IO) {
      if (updates.isEmpty()) return@withContext 0
      val byId = updates.associateBy { it.championId }
      val current = dao.getAllChampionsOnce().map { it.toDomain() }
      val updatedEntities =
        current.mapNotNull { champ ->
          val stat = byId[champ.id] ?: return@mapNotNull null
          champ
            .copy(
              winRatePct = stat.winRatePct,
              pickRatePct = stat.pickRatePct,
              banRatePct = stat.banRatePct,
              tierRank = stat.tierRank,
              patchVersion = patchLabelOverride?.takeIf { it.isNotBlank() } ?: champ.patchVersion,
            )
            .toEntity()
        }
      if (updatedEntities.isNotEmpty()) {
        dao.upsertChampions(updatedEntities)
      }
      updatedEntities.size
    }
}

