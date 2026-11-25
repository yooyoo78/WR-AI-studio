package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AbilityInfo
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.CustomBuildData
import com.example.data.model.DamageType
import com.example.data.model.DataSourceType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.LaneBuildOverride
import com.example.data.model.LaneDifficulty
import com.example.data.model.MatchJournalEntryData
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchChangeEntry
import com.example.data.model.PatchNoteData
import com.example.data.model.PatchSnapshotSummary
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.ThreatTag
import com.example.data.model.TierRank

@Entity(tableName = "champions")
data class ChampionEntity(
  @PrimaryKey val id: String,
  val nameEn: String,
  val nameTh: String,
  val titleEn: String,
  val titleTh: String,
  val roles: List<ChampionRole>,
  val lanes: List<Lane>,
  val damageType: DamageType,
  val difficulty: Int,
  val tierRank: TierRank,
  val winRatePct: Double = 50.0,
  val pickRatePct: Double = 5.0,
  val banRatePct: Double = 2.0,
  val portraitUrl: String = "",
  val earlyPower: Int,
  val midPower: Int,
  val latePower: Int,
  val burstRating: Int,
  val dpsRating: Int,
  val tankinessRating: Int,
  val ccRating: Int,
  val mobilityRating: Int,
  val sustainRating: Int,
  val abilities: List<AbilityInfo>,
  val strengthsEn: List<String>,
  val strengthsTh: List<String>,
  val weaknessesEn: List<String>,
  val weaknessesTh: List<String>,
  val threatTags: List<ThreatTag>,
  val starterItemId: String,
  val coreItemIds: List<String>,
  val defaultBootsId: String,
  val defaultEnchantId: String,
  val defaultKeystoneId: String,
  val defaultMinorRuneIds: List<String>,
  val defaultSpellIds: List<String>,
  val skillOrder: String,
  val counterChampionIds: List<String>,
  val favorableAgainstIds: List<String>,
  val earlyPlanEn: String,
  val earlyPlanTh: String,
  val midPlanEn: String,
  val midPlanTh: String,
  val latePlanEn: String,
  val latePlanTh: String,
  val patchVersion: String,
  val lastUpdated: String,
  val sourceType: DataSourceType,
  val laneOverrides: List<LaneBuildOverride> = emptyList(),
)

@Entity(tableName = "items")
data class ItemEntity(
  @PrimaryKey val id: String,
  val nameEn: String,
  val nameTh: String,
  val category: ItemCategory,
  val cost: Int,
  val statsSummaryEn: String,
  val statsSummaryTh: String,
  val passiveNameEn: String,
  val passiveNameTh: String,
  val passiveDescEn: String,
  val passiveDescTh: String,
  val counterTags: List<ItemCounterTag>,
  val damageAffinity: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
)

@Entity(tableName = "runes")
data class RuneEntity(
  @PrimaryKey val id: String,
  val nameEn: String,
  val nameTh: String,
  val slotType: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
  val tree: String = "",
)

@Entity(tableName = "spells")
data class SpellEntity(
  @PrimaryKey val id: String,
  val nameEn: String,
  val nameTh: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
  val cooldownNote: String = "",
)

@Entity(tableName = "matchup_rules")
data class MatchupRuleEntity(
  @PrimaryKey val id: String,
  val champAId: String,
  val champBId: String,
  val advantageScore: Int,
  val laneDifficulty: LaneDifficulty,
  val dangerAbilitiesEn: String,
  val dangerAbilitiesTh: String,
  val tipEn: String,
  val tipTh: String,
  val keySpikeEn: String,
  val keySpikeTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
)

@Entity(tableName = "patch_notes")
data class PatchNoteEntity(
  @PrimaryKey val version: String,
  val releaseDate: String,
  val headlineEn: String,
  val headlineTh: String,
  val summaryEn: String,
  val summaryTh: String,
  val sourceLabel: String,
  val isActive: Boolean,
  val changes: List<PatchChangeEntry>,
)

@Entity(tableName = "favorite_champions")
data class FavoriteChampionEntity(
  @PrimaryKey val championId: String,
  val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "saved_matchups")
data class SavedMatchupEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val myChampionId: String,
  val lane: Lane,
  val enemyIds: List<String>,
  val label: String,
  val savedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_builds")
data class CustomBuildEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val championId: String,
  val lane: Lane,
  val starterItemId: String,
  val coreItemIds: List<String>,
  val bootsId: String,
  val enchantId: String,
  val keystoneId: String,
  val minorRuneIds: List<String>,
  val spellIds: List<String>,
  val notes: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "ai_coach_cache")
data class AiCoachCacheEntity(
  @PrimaryKey val cacheKey: String,
  val normalizedQuestion: String,
  val championId: String,
  val lane: String,
  val patchVersion: String,
  val languageCode: String,
  val responseText: String,
  val modelId: String,
  val modelLabel: String,
  val createdAt: Long,
  val lastAccessedAt: Long,
  val hitCount: Int = 0,
)

data class CoachCacheStats(
  val entryCount: Int = 0,
  val totalHitsSaved: Int = 0,
)

@Entity(tableName = "match_journal")
data class MatchJournalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playedAt: Long,
  val championId: String,
  val lane: Lane,
  val isWin: Boolean,
  val kills: Int,
  val deaths: Int,
  val assists: Int,
  val goldEarned: Int,
  val durationMinutes: Int,
  val boughtItemIds: List<String>,
  val recommendedItemIds: List<String>,
  val enemyChampionIds: List<String>,
  val buildAccuracyPct: Int,
  val mistakeTags: List<String>,
  val lessons: List<String>,
  val coachSummaryText: String,
  val patchVersion: String,
  val notes: String = "",
)

@Entity(tableName = "patch_snapshots")
data class PatchSnapshotEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val patchVersion: String,
  val sourceLabel: String,
  val createdAt: Long,
  val payloadJson: String,
  val isRolledBack: Boolean = false,
)

fun ChampionEntity.toDomain() =
  ChampionData(
    id = id,
    nameEn = nameEn,
    nameTh = nameTh,
    titleEn = titleEn,
    titleTh = titleTh,
    roles = roles,
    lanes = lanes,
    damageType = damageType,
    difficulty = difficulty,
    tierRank = tierRank,
    winRatePct = winRatePct,
    pickRatePct = pickRatePct,
    banRatePct = banRatePct,
    portraitUrl = portraitUrl,
    earlyPower = earlyPower,
    midPower = midPower,
    latePower = latePower,
    burstRating = burstRating,
    dpsRating = dpsRating,
    tankinessRating = tankinessRating,
    ccRating = ccRating,
    mobilityRating = mobilityRating,
    sustainRating = sustainRating,
    abilities = abilities,
    strengthsEn = strengthsEn,
    strengthsTh = strengthsTh,
    weaknessesEn = weaknessesEn,
    weaknessesTh = weaknessesTh,
    threatTags = threatTags,
    starterItemId = starterItemId,
    coreItemIds = coreItemIds,
    defaultBootsId = defaultBootsId,
    defaultEnchantId = defaultEnchantId,
    defaultKeystoneId = defaultKeystoneId,
    defaultMinorRuneIds = defaultMinorRuneIds,
    defaultSpellIds = defaultSpellIds,
    skillOrder = skillOrder,
    counterChampionIds = counterChampionIds,
    favorableAgainstIds = favorableAgainstIds,
    earlyPlanEn = earlyPlanEn,
    earlyPlanTh = earlyPlanTh,
    midPlanEn = midPlanEn,
    midPlanTh = midPlanTh,
    latePlanEn = latePlanEn,
    latePlanTh = latePlanTh,
    patchVersion = patchVersion,
    lastUpdated = lastUpdated,
    sourceType = sourceType,
    laneOverrides = laneOverrides,
  )

fun ChampionData.toEntity() =
  ChampionEntity(
    id = id,
    nameEn = nameEn,
    nameTh = nameTh,
    titleEn = titleEn,
    titleTh = titleTh,
    roles = roles,
    lanes = lanes,
    damageType = damageType,
    difficulty = difficulty,
    tierRank = tierRank,
    winRatePct = winRatePct,
    pickRatePct = pickRatePct,
    banRatePct = banRatePct,
    portraitUrl = portraitUrl,
    earlyPower = earlyPower,
    midPower = midPower,
    latePower = latePower,
    burstRating = burstRating,
    dpsRating = dpsRating,
    tankinessRating = tankinessRating,
    ccRating = ccRating,
    mobilityRating = mobilityRating,
    sustainRating = sustainRating,
    abilities = abilities,
    strengthsEn = strengthsEn,
    strengthsTh = strengthsTh,
    weaknessesEn = weaknessesEn,
    weaknessesTh = weaknessesTh,
    threatTags = threatTags,
    starterItemId = starterItemId,
    coreItemIds = coreItemIds,
    defaultBootsId = defaultBootsId,
    defaultEnchantId = defaultEnchantId,
    defaultKeystoneId = defaultKeystoneId,
    defaultMinorRuneIds = defaultMinorRuneIds,
    defaultSpellIds = defaultSpellIds,
    skillOrder = skillOrder,
    counterChampionIds = counterChampionIds,
    favorableAgainstIds = favorableAgainstIds,
    earlyPlanEn = earlyPlanEn,
    earlyPlanTh = earlyPlanTh,
    midPlanEn = midPlanEn,
    midPlanTh = midPlanTh,
    latePlanEn = latePlanEn,
    latePlanTh = latePlanTh,
    patchVersion = patchVersion,
    lastUpdated = lastUpdated,
    sourceType = sourceType,
    laneOverrides = laneOverrides,
  )

fun ItemEntity.toDomain() =
  ItemData(
    id,
    nameEn,
    nameTh,
    category,
    cost,
    statsSummaryEn,
    statsSummaryTh,
    passiveNameEn,
    passiveNameTh,
    passiveDescEn,
    passiveDescTh,
    counterTags,
    damageAffinity,
    patchVersion,
    sourceType,
    iconUrl,
  )

fun ItemData.toEntity() =
  ItemEntity(
    id,
    nameEn,
    nameTh,
    category,
    cost,
    statsSummaryEn,
    statsSummaryTh,
    passiveNameEn,
    passiveNameTh,
    passiveDescEn,
    passiveDescTh,
    counterTags,
    damageAffinity,
    patchVersion,
    sourceType,
    iconUrl,
  )

fun RuneEntity.toDomain() =
  RuneData(id, nameEn, nameTh, slotType, descEn, descTh, patchVersion, sourceType, iconUrl, tree)

fun RuneData.toEntity() =
  RuneEntity(id, nameEn, nameTh, slotType, descEn, descTh, patchVersion, sourceType, iconUrl, tree)

fun SpellEntity.toDomain() =
  SpellData(id, nameEn, nameTh, descEn, descTh, patchVersion, sourceType, iconUrl, cooldownNote)

fun SpellData.toEntity() =
  SpellEntity(id, nameEn, nameTh, descEn, descTh, patchVersion, sourceType, iconUrl, cooldownNote)

fun MatchupRuleEntity.toDomain() =
  MatchupRuleData(
    id,
    champAId,
    champBId,
    advantageScore,
    laneDifficulty,
    dangerAbilitiesEn,
    dangerAbilitiesTh,
    tipEn,
    tipTh,
    keySpikeEn,
    keySpikeTh,
    patchVersion,
    sourceType,
  )

fun MatchupRuleData.toEntity() =
  MatchupRuleEntity(
    id,
    champAId,
    champBId,
    advantageScore,
    laneDifficulty,
    dangerAbilitiesEn,
    dangerAbilitiesTh,
    tipEn,
    tipTh,
    keySpikeEn,
    keySpikeTh,
    patchVersion,
    sourceType,
  )

fun PatchNoteEntity.toDomain() =
  PatchNoteData(
    version,
    releaseDate,
    headlineEn,
    headlineTh,
    summaryEn,
    summaryTh,
    sourceLabel,
    isActive,
    changes,
  )

fun PatchNoteData.toEntity() =
  PatchNoteEntity(
    version,
    releaseDate,
    headlineEn,
    headlineTh,
    summaryEn,
    summaryTh,
    sourceLabel,
    isActive,
    changes,
  )

fun CustomBuildEntity.toDomain() =
  CustomBuildData(
    id = id,
    name = name,
    championId = championId,
    lane = lane,
    starterItemId = starterItemId,
    coreItemIds = coreItemIds,
    bootsId = bootsId,
    enchantId = enchantId,
    keystoneId = keystoneId,
    minorRuneIds = minorRuneIds,
    spellIds = spellIds,
    notes = notes,
    updatedAt = updatedAt,
  )

fun CustomBuildData.toEntity() =
  CustomBuildEntity(
    id = id,
    name = name,
    championId = championId,
    lane = lane,
    starterItemId = starterItemId,
    coreItemIds = coreItemIds,
    bootsId = bootsId,
    enchantId = enchantId,
    keystoneId = keystoneId,
    minorRuneIds = minorRuneIds,
    spellIds = spellIds,
    notes = notes,
    updatedAt = updatedAt,
  )

fun MatchJournalEntity.toDomain() =
  MatchJournalEntryData(
    id = id,
    playedAt = playedAt,
    championId = championId,
    lane = lane,
    isWin = isWin,
    kills = kills,
    deaths = deaths,
    assists = assists,
    goldEarned = goldEarned,
    durationMinutes = durationMinutes,
    boughtItemIds = boughtItemIds,
    recommendedItemIds = recommendedItemIds,
    enemyChampionIds = enemyChampionIds,
    buildAccuracyPct = buildAccuracyPct,
    mistakeTags = mistakeTags,
    lessons = lessons,
    coachSummaryText = coachSummaryText,
    patchVersion = patchVersion,
    notes = notes,
  )

fun MatchJournalEntryData.toEntity() =
  MatchJournalEntity(
    id = id,
    playedAt = playedAt,
    championId = championId,
    lane = lane,
    isWin = isWin,
    kills = kills,
    deaths = deaths,
    assists = assists,
    goldEarned = goldEarned,
    durationMinutes = durationMinutes,
    boughtItemIds = boughtItemIds,
    recommendedItemIds = recommendedItemIds,
    enemyChampionIds = enemyChampionIds,
    buildAccuracyPct = buildAccuracyPct,
    mistakeTags = mistakeTags,
    lessons = lessons,
    coachSummaryText = coachSummaryText,
    patchVersion = patchVersion,
    notes = notes,
  )

fun PatchSnapshotEntity.toSummary() =
  PatchSnapshotSummary(
    id = id,
    patchVersion = patchVersion,
    sourceLabel = sourceLabel,
    createdAt = createdAt,
    isRolledBack = isRolledBack,
  )
