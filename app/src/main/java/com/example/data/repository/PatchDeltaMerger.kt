package com.example.data.repository

import com.example.data.model.AbilityInfo
import com.example.data.model.AbilityPatchDelta
import com.example.data.model.ChampionData
import com.example.data.model.ChampionPatchDelta
import com.example.data.model.ItemData
import com.example.data.model.ItemPatchDelta
import com.example.data.model.MatchupPatchDelta
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchPackPayload
import com.example.data.model.RuneData
import com.example.data.model.RunePatchDelta
import com.example.data.model.SpellData
import com.example.data.model.SpellPatchDelta

data class MergedPatchResult(
  val payload: PatchPackPayload,
  val unknownDeltaErrors: List<String> = emptyList(),
)

/**
 * Resolves compact ID-only delta arrays (`championUpdates`, `itemUpdates`, `runeUpdates`,
 * `spellUpdates`, `matchupUpdates`) onto existing catalog records so OTA/JSON packs only need to
 * declare `"id"` and modified fields.
 */
object PatchDeltaMerger {
  fun merge(
    payload: PatchPackPayload,
    existingChampions: List<ChampionData>,
    existingItems: List<ItemData>,
    existingRunes: List<RuneData>,
    existingSpells: List<SpellData>,
    existingMatchups: List<MatchupRuleData>,
  ): MergedPatchResult {
    val errors = mutableListOf<String>()
    val patchVer = payload.patchVersion
    val lastUpdated = payload.lastUpdated

    // Items first (merged items can also be referenced by merged champions)
    val itemMap = LinkedHashMap<String, ItemData>()
    payload.items.forEach { itemMap[it.id] = it.copy(patchVersion = patchVer) }
    val existingItemById = existingItems.associateBy { it.id }
    payload.itemUpdates.forEach { delta ->
      val base = itemMap[delta.id] ?: existingItemById[delta.id]
      if (base == null) {
        errors.add("Unknown itemUpdates id '${delta.id}' (must match an existing item or full items entry).")
      } else {
        itemMap[delta.id] = applyItemDelta(base, delta, patchVer)
      }
    }

    val runeMap = LinkedHashMap<String, RuneData>()
    payload.runes.forEach { runeMap[it.id] = it.copy(patchVersion = patchVer) }
    val existingRuneById = existingRunes.associateBy { it.id }
    payload.runeUpdates.forEach { delta ->
      val base = runeMap[delta.id] ?: existingRuneById[delta.id]
      if (base == null) {
        errors.add("Unknown runeUpdates id '${delta.id}' (must match an existing rune or full runes entry).")
      } else {
        runeMap[delta.id] = applyRuneDelta(base, delta, patchVer)
      }
    }

    val spellMap = LinkedHashMap<String, SpellData>()
    payload.spells.forEach { spellMap[it.id] = it.copy(patchVersion = patchVer) }
    val existingSpellById = existingSpells.associateBy { it.id }
    payload.spellUpdates.forEach { delta ->
      val base = spellMap[delta.id] ?: existingSpellById[delta.id]
      if (base == null) {
        errors.add("Unknown spellUpdates id '${delta.id}' (must match an existing spell or full spells entry).")
      } else {
        spellMap[delta.id] = applySpellDelta(base, delta, patchVer)
      }
    }

    val champMap = LinkedHashMap<String, ChampionData>()
    payload.champions.forEach { champMap[it.id] = it.copy(patchVersion = patchVer, lastUpdated = lastUpdated) }
    val existingChampById = existingChampions.associateBy { it.id }
    payload.championUpdates.forEach { delta ->
      val base = champMap[delta.id] ?: existingChampById[delta.id]
      if (base == null) {
        errors.add(
          "Unknown championUpdates id '${delta.id}' (must match an existing champion or full champions entry)."
        )
      } else {
        champMap[delta.id] = applyChampionDelta(base, delta, patchVer, lastUpdated)
      }
    }

    val matchupMap = LinkedHashMap<String, MatchupRuleData>()
    payload.matchupRules.forEach { matchupMap[it.id] = it.copy(patchVersion = patchVer) }
    val existingMatchupById = existingMatchups.associateBy { it.id }
    payload.matchupUpdates.forEach { delta ->
      val base = matchupMap[delta.id] ?: existingMatchupById[delta.id]
      if (base == null) {
        errors.add(
          "Unknown matchupUpdates id '${delta.id}' (must match an existing matchup rule or full matchupRules entry)."
        )
      } else {
        matchupMap[delta.id] = applyMatchupDelta(base, delta, patchVer)
      }
    }

    val mergedPayload =
      payload.copy(
        champions = champMap.values.toList(),
        items = itemMap.values.toList(),
        runes = runeMap.values.toList(),
        spells = spellMap.values.toList(),
        matchupRules = matchupMap.values.toList(),
        championUpdates = emptyList(),
        itemUpdates = emptyList(),
        runeUpdates = emptyList(),
        spellUpdates = emptyList(),
        matchupUpdates = emptyList(),
      )

    return MergedPatchResult(payload = mergedPayload, unknownDeltaErrors = errors)
  }

  private fun applyChampionDelta(
    base: ChampionData,
    d: ChampionPatchDelta,
    patchVersion: String,
    lastUpdated: String,
  ): ChampionData {
    val updatedAbilities =
      when {
        d.abilities != null -> d.abilities
        !d.abilityUpdates.isNullOrEmpty() -> mergeAbilities(base.abilities, d.abilityUpdates)
        else -> base.abilities
      }
    return base.copy(
      nameEn = d.nameEn ?: base.nameEn,
      nameTh = d.nameTh ?: base.nameTh,
      titleEn = d.titleEn ?: base.titleEn,
      titleTh = d.titleTh ?: base.titleTh,
      roles = d.roles ?: base.roles,
      lanes = d.lanes ?: base.lanes,
      damageType = d.damageType ?: base.damageType,
      difficulty = d.difficulty ?: base.difficulty,
      tierRank = d.tierRank ?: base.tierRank,
      winRatePct = d.winRatePct ?: base.winRatePct,
      pickRatePct = d.pickRatePct ?: base.pickRatePct,
      banRatePct = d.banRatePct ?: base.banRatePct,
      portraitUrl = d.portraitUrl ?: base.portraitUrl,
      earlyPower = d.earlyPower ?: base.earlyPower,
      midPower = d.midPower ?: base.midPower,
      latePower = d.latePower ?: base.latePower,
      burstRating = d.burstRating ?: base.burstRating,
      dpsRating = d.dpsRating ?: base.dpsRating,
      tankinessRating = d.tankinessRating ?: base.tankinessRating,
      ccRating = d.ccRating ?: base.ccRating,
      mobilityRating = d.mobilityRating ?: base.mobilityRating,
      sustainRating = d.sustainRating ?: base.sustainRating,
      abilities = updatedAbilities,
      strengthsEn = d.strengthsEn ?: base.strengthsEn,
      strengthsTh = d.strengthsTh ?: base.strengthsTh,
      weaknessesEn = d.weaknessesEn ?: base.weaknessesEn,
      weaknessesTh = d.weaknessesTh ?: base.weaknessesTh,
      threatTags = d.threatTags ?: base.threatTags,
      starterItemId = d.starterItemId ?: base.starterItemId,
      coreItemIds = d.coreItemIds ?: base.coreItemIds,
      defaultBootsId = d.defaultBootsId ?: base.defaultBootsId,
      defaultEnchantId = d.defaultEnchantId ?: base.defaultEnchantId,
      defaultKeystoneId = d.defaultKeystoneId ?: base.defaultKeystoneId,
      defaultMinorRuneIds = d.defaultMinorRuneIds ?: base.defaultMinorRuneIds,
      defaultSpellIds = d.defaultSpellIds ?: base.defaultSpellIds,
      skillOrder = d.skillOrder ?: base.skillOrder,
      counterChampionIds = d.counterChampionIds ?: base.counterChampionIds,
      favorableAgainstIds = d.favorableAgainstIds ?: base.favorableAgainstIds,
      earlyPlanEn = d.earlyPlanEn ?: base.earlyPlanEn,
      earlyPlanTh = d.earlyPlanTh ?: base.earlyPlanTh,
      midPlanEn = d.midPlanEn ?: base.midPlanEn,
      midPlanTh = d.midPlanTh ?: base.midPlanTh,
      latePlanEn = d.latePlanEn ?: base.latePlanEn,
      latePlanTh = d.latePlanTh ?: base.latePlanTh,
      patchVersion = patchVersion,
      lastUpdated = lastUpdated.ifBlank { base.lastUpdated },
      sourceType = d.sourceType ?: base.sourceType,
      laneOverrides = d.laneOverrides ?: base.laneOverrides,
    )
  }

  private fun mergeAbilities(
    existing: List<AbilityInfo>,
    deltas: List<AbilityPatchDelta>,
  ): List<AbilityInfo> {
    val bySlot = deltas.associateBy { it.slot.trim().uppercase() }
    return existing.map { ability ->
      val delta = bySlot[ability.slot.trim().uppercase()] ?: return@map ability
      ability.copy(
        nameEn = delta.nameEn ?: ability.nameEn,
        nameTh = delta.nameTh ?: ability.nameTh,
        summaryEn = delta.summaryEn ?: ability.summaryEn,
        summaryTh = delta.summaryTh ?: ability.summaryTh,
        cooldownNote = delta.cooldownNote ?: ability.cooldownNote,
        costNote = delta.costNote ?: ability.costNote,
        iconUrl = delta.iconUrl ?: ability.iconUrl,
      )
    }
  }

  private fun applyItemDelta(base: ItemData, d: ItemPatchDelta, patchVersion: String): ItemData =
    base.copy(
      nameEn = d.nameEn ?: base.nameEn,
      nameTh = d.nameTh ?: base.nameTh,
      category = d.category ?: base.category,
      cost = d.cost ?: base.cost,
      statsSummaryEn = d.statsSummaryEn ?: base.statsSummaryEn,
      statsSummaryTh = d.statsSummaryTh ?: base.statsSummaryTh,
      passiveNameEn = d.passiveNameEn ?: base.passiveNameEn,
      passiveNameTh = d.passiveNameTh ?: base.passiveNameTh,
      passiveDescEn = d.passiveDescEn ?: base.passiveDescEn,
      passiveDescTh = d.passiveDescTh ?: base.passiveDescTh,
      counterTags = d.counterTags ?: base.counterTags,
      damageAffinity = d.damageAffinity ?: base.damageAffinity,
      patchVersion = patchVersion,
      sourceType = d.sourceType ?: base.sourceType,
      iconUrl = d.iconUrl ?: base.iconUrl,
    )

  private fun applyRuneDelta(base: RuneData, d: RunePatchDelta, patchVersion: String): RuneData =
    base.copy(
      nameEn = d.nameEn ?: base.nameEn,
      nameTh = d.nameTh ?: base.nameTh,
      slotType = d.slotType ?: base.slotType,
      descEn = d.descEn ?: base.descEn,
      descTh = d.descTh ?: base.descTh,
      patchVersion = patchVersion,
      sourceType = d.sourceType ?: base.sourceType,
      iconUrl = d.iconUrl ?: base.iconUrl,
      tree = d.tree ?: base.tree,
    )

  private fun applySpellDelta(base: SpellData, d: SpellPatchDelta, patchVersion: String): SpellData =
    base.copy(
      nameEn = d.nameEn ?: base.nameEn,
      nameTh = d.nameTh ?: base.nameTh,
      descEn = d.descEn ?: base.descEn,
      descTh = d.descTh ?: base.descTh,
      patchVersion = patchVersion,
      sourceType = d.sourceType ?: base.sourceType,
      iconUrl = d.iconUrl ?: base.iconUrl,
      cooldownNote = d.cooldownNote ?: base.cooldownNote,
    )

  private fun applyMatchupDelta(
    base: MatchupPatchDeltaBase,
    d: MatchupPatchDelta,
    patchVersion: String,
  ): MatchupRuleData =
    base.copy(
      advantageScore = d.advantageScore ?: base.advantageScore,
      laneDifficulty = d.laneDifficulty ?: base.laneDifficulty,
      dangerAbilitiesEn = d.dangerAbilitiesEn ?: base.dangerAbilitiesEn,
      dangerAbilitiesTh = d.dangerAbilitiesTh ?: base.dangerAbilitiesTh,
      tipEn = d.tipEn ?: base.tipEn,
      tipTh = d.tipTh ?: base.tipTh,
      keySpikeEn = d.keySpikeEn ?: base.keySpikeEn,
      keySpikeTh = d.keySpikeTh ?: base.keySpikeTh,
      patchVersion = patchVersion,
      sourceType = d.sourceType ?: base.sourceType,
    )
}

private typealias MatchupPatchDeltaBase = MatchupRuleData
