package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AbilityInfo
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.DataSourceType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.LaneDifficulty
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchChangeEntry
import com.example.data.model.PatchNoteData
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
  RuneData(id, nameEn, nameTh, slotType, descEn, descTh, patchVersion, sourceType)

fun RuneData.toEntity() =
  RuneEntity(id, nameEn, nameTh, slotType, descEn, descTh, patchVersion, sourceType)

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
