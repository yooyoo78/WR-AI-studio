package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
  @Query("SELECT * FROM champions ORDER BY tierRank ASC, nameEn ASC")
  fun observeChampions(): Flow<List<ChampionEntity>>

  @Query("SELECT * FROM champions") suspend fun getAllChampionsOnce(): List<ChampionEntity>

  @Query("SELECT * FROM items ORDER BY cost ASC") fun observeItems(): Flow<List<ItemEntity>>

  @Query("SELECT * FROM items") suspend fun getAllItemsOnce(): List<ItemEntity>

  @Query("SELECT * FROM runes") fun observeRunes(): Flow<List<RuneEntity>>

  @Query("SELECT * FROM runes") suspend fun getAllRunesOnce(): List<RuneEntity>

  @Query("SELECT * FROM spells") fun observeSpells(): Flow<List<SpellEntity>>

  @Query("SELECT * FROM spells") suspend fun getAllSpellsOnce(): List<SpellEntity>

  @Query("SELECT * FROM matchup_rules")
  fun observeMatchupRules(): Flow<List<MatchupRuleEntity>>

  @Query("SELECT * FROM matchup_rules")
  suspend fun getAllMatchupRulesOnce(): List<MatchupRuleEntity>

  @Query("SELECT * FROM patch_notes ORDER BY isActive DESC, releaseDate DESC")
  fun observePatchNotes(): Flow<List<PatchNoteEntity>>

  @Query("SELECT * FROM favorite_champions ORDER BY addedAt DESC")
  fun observeFavoriteIds(): Flow<List<FavoriteChampionEntity>>

  @Query("SELECT * FROM saved_matchups ORDER BY savedAt DESC LIMIT 20")
  fun observeSavedMatchups(): Flow<List<SavedMatchupEntity>>

  @Query("SELECT * FROM custom_builds ORDER BY updatedAt DESC")
  fun observeCustomBuilds(): Flow<List<CustomBuildEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertChampions(champions: List<ChampionEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItems(items: List<ItemEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRunes(runes: List<RuneEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSpells(spells: List<SpellEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMatchupRules(rules: List<MatchupRuleEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPatchNotes(notes: List<PatchNoteEntity>)

  @Query("UPDATE patch_notes SET isActive = 0") suspend fun deactivateAllPatchNotes()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addFavorite(favorite: FavoriteChampionEntity)

  @Query("DELETE FROM favorite_champions WHERE championId = :championId")
  suspend fun removeFavorite(championId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSavedMatchup(matchup: SavedMatchupEntity)

  @Query("DELETE FROM saved_matchups WHERE id = :id")
  suspend fun deleteSavedMatchup(id: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCustomBuild(build: CustomBuildEntity): Long

  @Query("DELETE FROM custom_builds WHERE id = :id")
  suspend fun deleteCustomBuild(id: Long)

  @Query("SELECT COUNT(*) FROM champions") suspend fun getChampionCount(): Int

  @Transaction
  suspend fun applyPatchPack(
    champions: List<ChampionEntity>,
    items: List<ItemEntity>,
    runes: List<RuneEntity>,
    spells: List<SpellEntity>,
    matchupRules: List<MatchupRuleEntity>,
    patchNotes: List<PatchNoteEntity>,
    deactivateOlderNotes: Boolean,
  ) {
    if (items.isNotEmpty()) upsertItems(items)
    if (runes.isNotEmpty()) upsertRunes(runes)
    if (spells.isNotEmpty()) upsertSpells(spells)
    if (champions.isNotEmpty()) upsertChampions(champions)
    if (matchupRules.isNotEmpty()) upsertMatchupRules(matchupRules)
    if (patchNotes.isNotEmpty()) {
      if (deactivateOlderNotes && patchNotes.any { it.isActive }) {
        deactivateAllPatchNotes()
      }
      upsertPatchNotes(patchNotes)
    }
  }

  @Transaction
  suspend fun clearAllGameData() {
    clearChampions()
    clearItems()
    clearRunes()
    clearSpells()
    clearMatchupRules()
    clearPatchNotes()
  }

  @Query("DELETE FROM champions") suspend fun clearChampions()

  @Query("DELETE FROM items") suspend fun clearItems()

  @Query("DELETE FROM runes") suspend fun clearRunes()

  @Query("DELETE FROM spells") suspend fun clearSpells()

  @Query("DELETE FROM matchup_rules") suspend fun clearMatchupRules()

  @Query("DELETE FROM patch_notes") suspend fun clearPatchNotes()

  @Query("SELECT * FROM ai_coach_cache WHERE cacheKey = :cacheKey LIMIT 1")
  suspend fun getCoachCacheEntry(cacheKey: String): AiCoachCacheEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCoachCacheEntry(entry: AiCoachCacheEntity)

  @Query(
    "UPDATE ai_coach_cache SET hitCount = hitCount + 1, lastAccessedAt = :accessedAt WHERE cacheKey = :cacheKey"
  )
  suspend fun incrementCoachCacheHit(cacheKey: String, accessedAt: Long)

  @Query(
    "SELECT COUNT(*) AS entryCount, COALESCE(SUM(hitCount), 0) AS totalHitsSaved FROM ai_coach_cache"
  )
  fun observeCoachCacheStats(): Flow<CoachCacheStats>

  @Query("DELETE FROM ai_coach_cache WHERE createdAt < :cutoffMillis OR patchVersion != :activePatch")
  suspend fun pruneExpiredCoachCache(cutoffMillis: Long, activePatch: String)

  @Query(
    """
    DELETE FROM ai_coach_cache WHERE cacheKey NOT IN (
      SELECT cacheKey FROM ai_coach_cache ORDER BY lastAccessedAt DESC LIMIT :keepLimit
    )
    """
  )
  suspend fun trimCoachCacheToLimit(keepLimit: Int)

  @Query("DELETE FROM ai_coach_cache") suspend fun clearAllCoachCache()

  @Query("SELECT * FROM match_journal ORDER BY playedAt DESC")
  fun observeMatchJournal(): Flow<List<MatchJournalEntity>>

  @Query("SELECT * FROM match_journal ORDER BY playedAt DESC LIMIT :limit")
  suspend fun getRecentMatchJournalOnce(limit: Int): List<MatchJournalEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatchJournal(entry: MatchJournalEntity): Long

  @Query("DELETE FROM match_journal WHERE id = :id")
  suspend fun deleteMatchJournal(id: Long)

  @Query("SELECT * FROM patch_snapshots ORDER BY createdAt DESC LIMIT 10")
  fun observePatchSnapshots(): Flow<List<PatchSnapshotEntity>>

  @Query("SELECT * FROM patch_snapshots WHERE isRolledBack = 0 ORDER BY createdAt DESC LIMIT 1")
  suspend fun getLatestRollbackSnapshot(): PatchSnapshotEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPatchSnapshot(snapshot: PatchSnapshotEntity): Long

  @Query("UPDATE patch_snapshots SET isRolledBack = 1 WHERE id = :id")
  suspend fun markSnapshotRolledBack(id: Long)

  @Query(
    """
    DELETE FROM patch_snapshots WHERE id NOT IN (
      SELECT id FROM patch_snapshots ORDER BY createdAt DESC LIMIT :keepLimit
    )
    """
  )
  suspend fun trimPatchSnapshots(keepLimit: Int)
}

