package com.example.domain.engine

import com.example.data.model.ChampionData
import com.example.data.model.Lane

data class RawChampionScanPayload(
  val screenType: String = "UNKNOWN",
  val myChampionName: String? = null,
  val detectedLane: String = "UNKNOWN",
  val allies: List<String> = emptyList(),
  val enemies: List<String> = emptyList(),
  val skippedEmptySlots: Int = 0,
  val confidenceNote: String = "",
)

data class ChampionScanMatchResult(
  val screenType: String,
  val myChampion: ChampionData?,
  val detectedLane: Lane,
  val allies: List<ChampionData>,
  val enemies: List<ChampionData>,
  val skippedUnpickedCount: Int,
  val unmatchedTokens: List<String>,
  val confidenceNote: String,
)

class ChampionScanMatcher {
  fun resolve(
    payload: RawChampionScanPayload,
    catalog: List<ChampionData>,
  ): ChampionScanMatchResult {
    if (catalog.isEmpty()) {
      return ChampionScanMatchResult(
        screenType = payload.screenType,
        myChampion = null,
        detectedLane = Lane.MID,
        allies = emptyList(),
        enemies = emptyList(),
        skippedUnpickedCount = payload.skippedEmptySlots.coerceAtLeast(0),
        unmatchedTokens = emptyList(),
        confidenceNote = payload.confidenceNote,
      )
    }

    val lookupIndex = buildLookupIndex(catalog)
    var skippedCounter = payload.skippedEmptySlots.coerceAtLeast(0)
    val unmatched = mutableListOf<String>()

    val resolvedAllies = mutableListOf<ChampionData>()
    val seenIds = mutableSetOf<String>()

    payload.allies.forEach { raw ->
      if (isPlaceholderToken(raw)) {
        skippedCounter++
      } else {
        val match = matchChampion(raw, lookupIndex, catalog)
        if (match != null) {
          if (resolvedAllies.size < 5 && seenIds.add(match.id)) {
            resolvedAllies.add(match)
          }
        } else {
          unmatched.add(raw.trim())
        }
      }
    }

    val resolvedEnemies = mutableListOf<ChampionData>()
    payload.enemies.forEach { raw ->
      if (isPlaceholderToken(raw)) {
        skippedCounter++
      } else {
        val match = matchChampion(raw, lookupIndex, catalog)
        if (match != null) {
          if (resolvedEnemies.size < 5 && seenIds.add(match.id)) {
            resolvedEnemies.add(match)
          }
        } else {
          unmatched.add(raw.trim())
        }
      }
    }

    val explicitMyChamp =
      payload.myChampionName
        ?.takeUnless { isPlaceholderToken(it) }
        ?.let { matchChampion(it, lookupIndex, catalog) }

    val myChampion =
      when {
        explicitMyChamp != null -> {
          if (explicitMyChamp.id !in resolvedEnemies.map { it.id } &&
            explicitMyChamp.id !in resolvedAllies.map { it.id } &&
            resolvedAllies.size < 5
          ) {
            resolvedAllies.add(0, explicitMyChamp)
          }
          explicitMyChamp
        }
        else -> resolvedAllies.firstOrNull()
      }

    val lane =
      parseLane(payload.detectedLane)
        ?: myChampion?.lanes?.firstOrNull()
        ?: Lane.MID

    // Ensure skipped count reflects visible open slots in a 5v5 draft if Gemini omitted it
    val inferredMissing =
      if (payload.skippedEmptySlots <= 0 &&
        payload.screenType in setOf("CHAMP_SELECT", "LOADING_SCREEN")
      ) {
        (10 - (resolvedAllies.size + resolvedEnemies.size)).coerceAtLeast(0)
      } else {
        skippedCounter
      }

    return ChampionScanMatchResult(
      screenType = payload.screenType,
      myChampion = myChampion,
      detectedLane = lane,
      allies = resolvedAllies,
      enemies = resolvedEnemies,
      skippedUnpickedCount = maxOf(skippedCounter, inferredMissing),
      unmatchedTokens = unmatched.distinct(),
      confidenceNote = payload.confidenceNote,
    )
  }

  internal fun matchChampion(
    rawName: String,
    lookupIndex: Map<String, ChampionData>,
    catalog: List<ChampionData>,
  ): ChampionData? {
    val norm = normalize(rawName)
    if (norm.isBlank() || isPlaceholderToken(rawName)) return null

    lookupIndex[norm]?.let { return it }
    ALIAS_TO_ID[norm]?.let { aliasId ->
      lookupIndex[normalize(aliasId)]?.let { return it }
    }

    if (norm.length >= 4) {
      val candidates =
        catalog.filter { champ ->
          val normEn = normalize(champ.nameEn)
          val normId = normalize(champ.id)
          normEn.startsWith(norm) ||
            norm.startsWith(normEn) ||
            normId == norm ||
            (norm.length >= 5 && normEn.contains(norm))
        }
      if (candidates.size == 1) return candidates.first()
    }
    return null
  }

  private fun buildLookupIndex(catalog: List<ChampionData>): Map<String, ChampionData> {
    val map = LinkedHashMap<String, ChampionData>(catalog.size * 3)
    catalog.forEach { champ ->
      map[normalize(champ.id)] = champ
      map[normalize(champ.nameEn)] = champ
      if (champ.nameTh.isNotBlank()) {
        map[normalize(champ.nameTh)] = champ
      }
    }
    return map
  }

  private fun parseLane(raw: String): Lane? =
    when (raw.trim().uppercase()) {
      "BARON", "TOP", "SOLO" -> Lane.BARON
      "MID", "MIDDLE" -> Lane.MID
      "DRAGON", "BOT", "ADC", "DUO" -> Lane.DRAGON
      "SUPPORT", "SUP" -> Lane.SUPPORT
      "JUNGLE", "JUG", "JG" -> Lane.JUNGLE
      else -> null
    }

  private fun isPlaceholderToken(raw: String): Boolean {
    val cleaned = raw.trim().lowercase()
    if (cleaned.isBlank()) return true
    return cleaned in PLACEHOLDER_TOKENS ||
      cleaned.startsWith("picking") ||
      cleaned.startsWith("selecting") ||
      cleaned.startsWith("empty") ||
      cleaned.startsWith("unknown") ||
      cleaned.startsWith("กำลังเลือก") ||
      cleaned.startsWith("ว่าง")
  }

  private fun normalize(value: String): String =
    value
      .lowercase()
      .replace("&", "and")
      .replace(Regex("[^\\p{L}\\p{N}]"), "")

  private companion object {
    val PLACEHOLDER_TOKENS =
      setOf(
        "none",
        "null",
        "empty",
        "blank",
        "picking",
        "unpicked",
        "unknown",
        "ban",
        "banned",
        "locked",
        "open",
        "slot",
        "n/a",
        "na",
        "-",
        "?",
        "ว่าง",
        "ยังไม่เลือก",
        "รอเลือก",
      )

    val ALIAS_TO_ID =
      mapOf(
        "monkeyking" to "wukong",
        "nunu" to "nunu_willump",
        "nunuandwillump" to "nunu_willump",
        "willump" to "nunu_willump",
        "drmundo" to "dr_mundo",
        "mundo" to "dr_mundo",
        "j4" to "jarvan_iv",
        "jarvan" to "jarvan_iv",
        "jarvan4" to "jarvan_iv",
        "tf" to "twisted_fate",
        "mf" to "miss_fortune",
        "asol" to "aurelion_sol",
        "aurelion" to "aurelion_sol",
        "yi" to "master_yi",
        "xin" to "xin_zhao",
        "leesin" to "lee_sin",
        "kaisa" to "kaisa",
        "khazix" to "khazix",
        "velkoz" to "velkoz",
        "chogath" to "chogath",
        "reksai" to "reksai",
        "kogmaw" to "kogmaw",
        "belveth" to "belveth",
        "ksante" to "ksante",
        "tahmkench" to "tahm_kench",
        "tk" to "tahm_kench",
        "renataglasc" to "renata_glasc",
        "renata" to "renata_glasc",
        "heime" to "heimerdinger",
        "heimer" to "heimerdinger",
        "fiddle" to "fiddlesticks",
        "vlad" to "vladimir",
        "morde" to "mordekaiser",
        "kass" to "kassadin",
        "cait" to "caitlyn",
        "ez" to "ezreal",
        "luxanna" to "lux",
        "blitz" to "blitzcrank",
        "thresh" to "thresh",
        "ali" to "alistar",
        "sera" to "seraphine",
        "morg" to "morgana",
      )
  }
}
