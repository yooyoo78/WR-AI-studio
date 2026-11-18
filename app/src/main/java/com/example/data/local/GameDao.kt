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
}
