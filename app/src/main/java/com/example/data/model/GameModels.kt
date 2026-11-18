package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AbilityInfo(
  val slot: String,
  val nameEn: String,
  val nameTh: String,
  val summaryEn: String,
  val summaryTh: String,
  val cooldownNote: String,
  val costNote: String = "",
  val iconUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class ChampionData(
  val id: String,
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
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun displayTitle(lang: AppLanguage): String = if (lang == AppLanguage.TH) titleTh else titleEn

  fun strengths(lang: AppLanguage): List<String> =
    if (lang == AppLanguage.TH) strengthsTh else strengthsEn

  fun weaknesses(lang: AppLanguage): List<String> =
    if (lang == AppLanguage.TH) weaknessesTh else weaknessesEn

  fun earlyPlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) earlyPlanTh else earlyPlanEn
  fun midPlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) midPlanTh else midPlanEn
  fun latePlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) latePlanTh else latePlanEn
}

@JsonClass(generateAdapter = true)
data class ItemData(
  val id: String,
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
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun statsSummary(lang: AppLanguage): String =
    if (lang == AppLanguage.TH) statsSummaryTh else statsSummaryEn

  fun passiveDesc(lang: AppLanguage): String =
    if (lang == AppLanguage.TH) passiveDescTh else passiveDescEn
}

@JsonClass(generateAdapter = true)
data class RuneData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val slotType: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun desc(lang: AppLanguage): String = if (lang == AppLanguage.TH) descTh else descEn
}

@JsonClass(generateAdapter = true)
data class SpellData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
  val cooldownNote: String = "",
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun desc(lang: AppLanguage): String = if (lang == AppLanguage.TH) descTh else descEn
}

@JsonClass(generateAdapter = true)
data class MatchupRuleData(
  val id: String,
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

@JsonClass(generateAdapter = true)
data class PatchChangeEntry(
  val targetName: String,
  val changeType: PatchChangeType,
  val detailEn: String,
  val detailTh: String,
)

@JsonClass(generateAdapter = true)
data class PatchNoteData(
  val version: String,
  val releaseDate: String,
  val headlineEn: String,
  val headlineTh: String,
  val summaryEn: String,
  val summaryTh: String,
  val sourceLabel: String,
  val isActive: Boolean,
  val changes: List<PatchChangeEntry>,
)

@JsonClass(generateAdapter = true)
data class PatchPackPayload(
  val packVersion: Int,
  val patchVersion: String,
  val lastUpdated: String,
  val sourceLabel: String,
  val champions: List<ChampionData> = emptyList(),
  val items: List<ItemData> = emptyList(),
  val runes: List<RuneData> = emptyList(),
  val spells: List<SpellData> = emptyList(),
  val matchupRules: List<MatchupRuleData> = emptyList(),
  val patchNotes: List<PatchNoteData> = emptyList(),
)
