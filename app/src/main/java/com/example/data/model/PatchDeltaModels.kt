package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson

@JsonClass(generateAdapter = true)
data class AbilityPatchDelta(
  val slot: String,
  val nameEn: String? = null,
  val nameTh: String? = null,
  val summaryEn: String? = null,
  val summaryTh: String? = null,
  val cooldownNote: String? = null,
  val costNote: String? = null,
  val iconUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChampionPatchDelta(
  val id: String,
  val nameEn: String? = null,
  val nameTh: String? = null,
  val titleEn: String? = null,
  val titleTh: String? = null,
  val roles: List<ChampionRole>? = null,
  val lanes: List<Lane>? = null,
  val damageType: DamageType? = null,
  val difficulty: Int? = null,
  val tierRank: TierRank? = null,
  val winRatePct: Double? = null,
  val pickRatePct: Double? = null,
  val banRatePct: Double? = null,
  val portraitUrl: String? = null,
  val earlyPower: Int? = null,
  val midPower: Int? = null,
  val latePower: Int? = null,
  val burstRating: Int? = null,
  val dpsRating: Int? = null,
  val tankinessRating: Int? = null,
  val ccRating: Int? = null,
  val mobilityRating: Int? = null,
  val sustainRating: Int? = null,
  val abilityUpdates: List<AbilityPatchDelta>? = null,
  val abilities: List<AbilityInfo>? = null,
  val strengthsEn: List<String>? = null,
  val strengthsTh: List<String>? = null,
  val weaknessesEn: List<String>? = null,
  val weaknessesTh: List<String>? = null,
  val threatTags: List<ThreatTag>? = null,
  val starterItemId: String? = null,
  val coreItemIds: List<String>? = null,
  val defaultBootsId: String? = null,
  val defaultEnchantId: String? = null,
  val defaultKeystoneId: String? = null,
  val defaultMinorRuneIds: List<String>? = null,
  val defaultSpellIds: List<String>? = null,
  val skillOrder: String? = null,
  val counterChampionIds: List<String>? = null,
  val favorableAgainstIds: List<String>? = null,
  val earlyPlanEn: String? = null,
  val earlyPlanTh: String? = null,
  val midPlanEn: String? = null,
  val midPlanTh: String? = null,
  val latePlanEn: String? = null,
  val latePlanTh: String? = null,
  val sourceType: DataSourceType? = null,
  val laneOverrides: List<LaneBuildOverride>? = null,
)

@JsonClass(generateAdapter = true)
data class ItemPatchDelta(
  val id: String,
  val nameEn: String? = null,
  val nameTh: String? = null,
  val category: ItemCategory? = null,
  val cost: Int? = null,
  val statsSummaryEn: String? = null,
  val statsSummaryTh: String? = null,
  val passiveNameEn: String? = null,
  val passiveNameTh: String? = null,
  val passiveDescEn: String? = null,
  val passiveDescTh: String? = null,
  val counterTags: List<ItemCounterTag>? = null,
  val damageAffinity: String? = null,
  val sourceType: DataSourceType? = null,
  val iconUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class RunePatchDelta(
  val id: String,
  val nameEn: String? = null,
  val nameTh: String? = null,
  val slotType: String? = null,
  val descEn: String? = null,
  val descTh: String? = null,
  val sourceType: DataSourceType? = null,
  val iconUrl: String? = null,
  val tree: String? = null,
)

@JsonClass(generateAdapter = true)
data class SpellPatchDelta(
  val id: String,
  val nameEn: String? = null,
  val nameTh: String? = null,
  val descEn: String? = null,
  val descTh: String? = null,
  val sourceType: DataSourceType? = null,
  val iconUrl: String? = null,
  val cooldownNote: String? = null,
)

@JsonClass(generateAdapter = true)
data class MatchupPatchDelta(
  val id: String,
  val advantageScore: Int? = null,
  val laneDifficulty: LaneDifficulty? = null,
  val dangerAbilitiesEn: String? = null,
  val dangerAbilitiesTh: String? = null,
  val tipEn: String? = null,
  val tipTh: String? = null,
  val keySpikeEn: String? = null,
  val keySpikeTh: String? = null,
  val sourceType: DataSourceType? = null,
)

/**
 * Tolerant Moshi adapter for [PatchChangeType] so external or hand-authored OTA packs using
 * aliases like "CHANGE", "MODIFIED", "UPDATE", or lowercase labels map cleanly without failing.
 */
class LenientPatchChangeTypeAdapter {
  @FromJson
  fun fromJson(raw: String): PatchChangeType =
    when (raw.trim().uppercase()) {
      "NEW",
      "ADD",
      "ADDED" -> PatchChangeType.NEW
      "BUFF",
      "BUFFED" -> PatchChangeType.BUFF
      "NERF",
      "NERFED" -> PatchChangeType.NERF
      "ADJUST",
      "ADJUSTED",
      "CHANGE",
      "CHANGED",
      "MODIFIED",
      "UPDATE",
      "UPDATED",
      "REWORK" -> PatchChangeType.ADJUST
      else -> PatchChangeType.ADJUST
    }

  @ToJson
  fun toJson(value: PatchChangeType): String = value.name
}
