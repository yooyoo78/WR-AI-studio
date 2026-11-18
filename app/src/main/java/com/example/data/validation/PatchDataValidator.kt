package com.example.data.validation

import com.example.data.model.ChampionData
import com.example.data.model.ItemData
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchPackPayload
import com.example.data.model.RuneData
import com.example.data.model.SpellData

data class ValidationResult(
  val isValid: Boolean,
  val errors: List<String>,
  val warnings: List<String>,
  val championsCount: Int,
  val itemsCount: Int,
  val runesCount: Int,
  val spellsCount: Int,
  val matchupsCount: Int,
  val patchNotesCount: Int,
)

class PatchDataValidator {
  fun validate(
    payload: PatchPackPayload,
    existingChampions: List<ChampionData> = emptyList(),
    existingItems: List<ItemData> = emptyList(),
    existingRunes: List<RuneData> = emptyList(),
    existingSpells: List<SpellData> = emptyList(),
    existingMatchups: List<MatchupRuleData> = emptyList(),
  ): ValidationResult {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    if (payload.patchVersion.isBlank()) {
      errors.add("patchVersion must not be blank.")
    }

    val allItemIds = (existingItems.map { it.id } + payload.items.map { it.id }).toSet()
    val allRuneIds = (existingRunes.map { it.id } + payload.runes.map { it.id }).toSet()
    val allSpellIds = (existingSpells.map { it.id } + payload.spells.map { it.id }).toSet()
    val allChampIds = (existingChampions.map { it.id } + payload.champions.map { it.id }).toSet()

    // Check duplicate IDs within payload
    findDuplicates(payload.champions.map { it.id }).forEach {
      errors.add("Duplicate champion id in pack: $it")
    }
    findDuplicates(payload.items.map { it.id }).forEach {
      errors.add("Duplicate item id in pack: $it")
    }
    findDuplicates(payload.runes.map { it.id }).forEach {
      errors.add("Duplicate rune id in pack: $it")
    }
    findDuplicates(payload.spells.map { it.id }).forEach {
      errors.add("Duplicate spell id in pack: $it")
    }

    payload.champions.forEach { champ ->
      if (champ.id.isBlank() || champ.nameEn.isBlank()) {
        errors.add("Champion missing id or nameEn: ${champ.id}")
      }
      if (champ.roles.isEmpty() || champ.lanes.isEmpty()) {
        errors.add("Champion ${champ.id} must declare at least one role and one lane.")
      }
      if (champ.difficulty !in 1..3) {
        errors.add("Champion ${champ.id} difficulty must be 1..3.")
      }
      if (champ.winRatePct !in 0.0..100.0 || champ.pickRatePct !in 0.0..100.0 || champ.banRatePct !in 0.0..100.0) {
        errors.add("Champion ${champ.id} win/pick/ban rates must be within 0..100.")
      }
      listOf(
          "earlyPower" to champ.earlyPower,
          "midPower" to champ.midPower,
          "latePower" to champ.latePower,
          "burstRating" to champ.burstRating,
          "dpsRating" to champ.dpsRating,
          "tankinessRating" to champ.tankinessRating,
          "ccRating" to champ.ccRating,
          "mobilityRating" to champ.mobilityRating,
          "sustainRating" to champ.sustainRating,
        )
        .forEach { (field, value) ->
          if (value !in 1..10) {
            errors.add("Champion ${champ.id} $field=$value is outside 1..10.")
          }
        }

      if (champ.starterItemId !in allItemIds) {
        errors.add("Champion ${champ.id} references unknown starterItemId '${champ.starterItemId}'.")
      }
      champ.coreItemIds.forEach { itemId ->
        if (itemId !in allItemIds) {
          errors.add("Champion ${champ.id} references unknown coreItemId '$itemId'.")
        }
      }
      if (champ.defaultBootsId !in allItemIds) {
        errors.add(
          "Champion ${champ.id} references unknown defaultBootsId '${champ.defaultBootsId}'."
        )
      }
      if (champ.defaultEnchantId !in allItemIds) {
        errors.add(
          "Champion ${champ.id} references unknown defaultEnchantId '${champ.defaultEnchantId}'."
        )
      }
      if (champ.defaultKeystoneId !in allRuneIds) {
        errors.add(
          "Champion ${champ.id} references unknown defaultKeystoneId '${champ.defaultKeystoneId}'."
        )
      }
      champ.defaultMinorRuneIds.forEach { runeId ->
        if (runeId !in allRuneIds) {
          errors.add("Champion ${champ.id} references unknown minorRuneId '$runeId'.")
        }
      }
      champ.defaultSpellIds.forEach { spellId ->
        if (spellId !in allSpellIds) {
          errors.add("Champion ${champ.id} references unknown spellId '$spellId'.")
        }
      }
      champ.counterChampionIds.forEach { cId ->
        if (cId !in allChampIds) {
          warnings.add("Champion ${champ.id} counterChampionId '$cId' not yet in roster.")
        }
      }
      champ.favorableAgainstIds.forEach { fId ->
        if (fId !in allChampIds) {
          warnings.add("Champion ${champ.id} favorableAgainstId '$fId' not yet in roster.")
        }
      }
    }

    payload.matchupRules.forEach { rule ->
      if (rule.champAId !in allChampIds) {
        errors.add("MatchupRule ${rule.id} references unknown champAId '${rule.champAId}'.")
      }
      if (rule.champBId !in allChampIds) {
        errors.add("MatchupRule ${rule.id} references unknown champBId '${rule.champBId}'.")
      }
      if (rule.advantageScore !in -5..5) {
        errors.add("MatchupRule ${rule.id} advantageScore=${rule.advantageScore} must be -5..5.")
      }
    }

    if (
      payload.champions.isEmpty() &&
        payload.items.isEmpty() &&
        payload.runes.isEmpty() &&
        payload.spells.isEmpty() &&
        payload.matchupRules.isEmpty() &&
        payload.patchNotes.isEmpty() &&
        existingMatchups.isEmpty()
    ) {
      warnings.add("Patch pack contains no records.")
    }

    return ValidationResult(
      isValid = errors.isEmpty(),
      errors = errors,
      warnings = warnings,
      championsCount = payload.champions.size,
      itemsCount = payload.items.size,
      runesCount = payload.runes.size,
      spellsCount = payload.spells.size,
      matchupsCount = payload.matchupRules.size,
      patchNotesCount = payload.patchNotes.size,
    )
  }

  private fun findDuplicates(ids: List<String>): List<String> =
    ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.toList()
}
