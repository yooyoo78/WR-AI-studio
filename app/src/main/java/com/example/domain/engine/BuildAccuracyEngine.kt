package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.isTier3Boot
import com.example.data.model.tier2BootBaseId
import kotlin.math.roundToInt

data class BuildAccuracyReport(
  val accuracyPct: Int,
  val matchedItems: List<ItemData>,
  val recommendedItems: List<ItemData>,
  val missedCoreItems: List<ItemData>,
  val mistakeTags: List<String>,
  val feedbackEn: List<String>,
  val feedbackTh: List<String>,
) {
  fun feedback(lang: AppLanguage): List<String> = if (lang == AppLanguage.TH) feedbackTh else feedbackEn
}

class BuildAccuracyEngine(
  private val matchupEngine: MatchupCounterEngine = MatchupCounterEngine(),
  private val smartItemEngine: SmartItemEngine = SmartItemEngine(),
) {
  fun evaluate(
    myChampion: ChampionData,
    lane: Lane,
    enemies: List<ChampionData>,
    boughtItems: List<ItemData>,
    kills: Int,
    deaths: Int,
    assists: Int,
    goldEarned: Int,
    durationMinutes: Int,
    allItems: List<ItemData>,
    allRunes: List<RuneData>,
    allSpells: List<SpellData>,
    explicitMatchupRules: List<com.example.data.model.MatchupRuleData>,
  ): BuildAccuracyReport {
    val (matchups, profile) =
      matchupEngine.analyzeMatchups(myChampion, lane, enemies, explicitMatchupRules)
    val smartBuild =
      smartItemEngine.buildRecommendation(
        myChampion = myChampion,
        myLane = lane,
        enemies = enemies,
        matchups = matchups,
        profile = profile,
        allItems = allItems,
        allRunes = allRunes,
        allSpells = allSpells,
      )

    val recommendedCore = smartBuild.prioritizedItems.map { it.item }.take(5)
    val recBoots = smartBuild.recommendedBoots.item
    val recTier3Boots = smartBuild.recommendedTier3Boots?.item
    val recEnchant = smartBuild.recommendedEnchant.item

    val fullRecommendedList =
      (recommendedCore + listOfNotNull(recBoots, recEnchant)).distinctBy { it.id }

    val boughtIds = boughtItems.map { it.id }.toSet()
    val boughtBaseBoots =
      boughtItems.mapNotNull { tier2BootBaseId(it.id) ?: it.id.takeIf { id -> it.category == ItemCategory.BOOTS } }.toSet()
    val recBootsBase = tier2BootBaseId(recBoots.id) ?: recBoots.id

    val boughtTags = boughtItems.flatMap { it.counterTags }.toSet()
    val recommendedTags = fullRecommendedList.flatMap { it.counterTags }.toSet()

    // Weighted scoring (Total 100 pts)
    var earnedPts = 0.0
    var maxPts = 0.0

    // 1) Core items (each 16 pts, max ~64-80)
    val matchedCore = mutableListOf<ItemData>()
    val missedCore = mutableListOf<ItemData>()
    recommendedCore.take(4).forEach { rec ->
      maxPts += 16.0
      if (rec.id in boughtIds) {
        earnedPts += 16.0
        matchedCore += rec
      } else {
        missedCore += rec
      }
    }

    // 2) Boots match (18 pts; full for exact/Tier3 match, partial 8 pts if wrong boot type)
    maxPts += 18.0
    val bootsMatched =
      recBoots.id in boughtIds ||
        (recTier3Boots != null && recTier3Boots.id in boughtIds) ||
        recBootsBase in boughtBaseBoots
    if (bootsMatched) {
      earnedPts += 18.0
      matchedCore += boughtItems.firstOrNull { it.id == recBoots.id || it.id == recTier3Boots?.id } ?: recBoots
    } else if (boughtItems.any { it.category == ItemCategory.BOOTS || isTier3Boot(it.id) }) {
      earnedPts += 7.0
    }

    // 3) Enchant / Active match (18 pts)
    maxPts += 18.0
    val hasAnyEnchant = boughtItems.any { it.category == ItemCategory.ENCHANTMENT }
    if (recEnchant.id in boughtIds) {
      earnedPts += 18.0
      matchedCore += recEnchant
    } else if (hasAnyEnchant) {
      earnedPts += 10.0
    }

    // Situational counter tag bonus/penalty checks
    val mistakeTags = mutableListOf<String>()
    val feedbackEn = mutableListOf<String>()
    val feedbackTh = mutableListOf<String>()

    val needsAntiHeal =
      profile.healingScore >= 34 || ItemCounterTag.ANTI_HEAL in recommendedTags
    if (needsAntiHeal && ItemCounterTag.ANTI_HEAL !in boughtTags) {
      mistakeTags += MISTAKE_MISSED_ANTI_HEAL
      feedbackEn += "Missing Anti-Heal against enemy sustain (${profile.healingThreats.joinToString { it.nameEn }.ifBlank { "enemy healing" }})."
      feedbackTh += "ขาดไอเทมตัดฮีล (Anti-Heal) ทั้งที่ฝั่งตรงข้ามมีตัวฟื้นฟูเลือด (${profile.healingThreats.joinToString { it.nameTh }.ifBlank { "ศัตรูฮีลแรง" }})"
      earnedPts -= 6.0
    }

    val antiShieldIds = setOf("serpents_fang", "oceanids_trident")
    val needsAntiShield = fullRecommendedList.any { it.id in antiShieldIds }
    if (needsAntiShield && boughtIds.none { it in antiShieldIds }) {
      mistakeTags += MISTAKE_MISSED_ANTI_SHIELD
      feedbackEn += "Enemy shields went unpunished — consider Serpent's Fang or Oceanid's Trident."
      feedbackTh += "ขาดไอเทมเจาะโล่ (Serpent's Fang / Oceanid's Trident) เพื่อลดเกราะฝั่งตรงข้าม"
      earnedPts -= 4.0
    }

    val needsSurvivabilityActive =
      (profile.diveScore >= 35 || profile.ccScore >= 45) && !hasAnyEnchant
    if (needsSurvivabilityActive) {
      mistakeTags += MISTAKE_NO_ENCHANT
      feedbackEn += "No Boot Enchant detected (${recEnchant.nameEn} strongly helps survive dive/CC)."
      feedbackTh += "ยังไม่พบ Boot Enchant (${recEnchant.nameTh} ช่วยกันเบิร์สต์/CC ได้มาก)"
    } else if (
      (recEnchant.id == "stasis_enchant" || recEnchant.id == "quicksilver_enchant") &&
        recEnchant.id !in boughtIds &&
        (profile.diveScore >= 35 || profile.ccScore >= 55)
    ) {
      mistakeTags += MISTAKE_MISSED_STASIS_QSS
      feedbackEn += "Consider switching Boot Enchant to ${recEnchant.nameEn} against heavy burst/CC."
      feedbackTh += "ควรปรับ Boot Enchant เป็น ${recEnchant.nameTh} เพื่อรับมือเบิร์สต์/CC หนัก"
    }

    val needsTankShred =
      profile.tankinessScore >= 45 &&
        ItemCounterTag.ARMOR_PEN !in boughtTags &&
        ItemCounterTag.MAGIC_PEN !in boughtTags &&
        ItemCounterTag.ANTI_TANK_PCT_HP !in boughtTags
    if (needsTankShred) {
      mistakeTags += MISTAKE_MISSED_PEN
      feedbackEn += "Lacked Armor/MR penetration or %HP damage vs enemy frontline tanks."
      feedbackTh += "ขาดไอเทมเจาะเกราะ/เจาะเวท หรือดาเมจ %HP รับมือแทงก์ฝั่งตรงข้าม"
      earnedPts -= 5.0
    }

    val safeDuration = durationMinutes.coerceAtLeast(8)
    val gpm = if (goldEarned > 0) goldEarned / safeDuration else 0
    if (deaths >= 7 || (deaths >= 5 && kills + assists < deaths * 2)) {
      mistakeTags += MISTAKE_HIGH_DEATHS
      feedbackEn += "High death count ($deaths deaths) slowed down core power spikes."
      feedbackTh += "จำนวนตายสูง ($deaths ครั้ง) ทำให้จังหวะพาวเวอร์สไปก์ช้าลง"
    }
    if (gpm in 1..619) {
      mistakeTags += MISTAKE_LOW_GPM
      feedbackEn += "Gold tempo was ~${gpm} GPM; tighten side-wave catches between objectives."
      feedbackTh += "อัตราฟาร์มทองเฉลี่ย ~${gpm} GPM ควรเก็บครีปเลนข้างเพิ่มก่อนเดินไฟต์"
    }

    val rawPct =
      if (boughtItems.isEmpty()) 0
      else if (maxPts <= 0.0) 70
      else ((earnedPts / maxPts) * 100.0).roundToInt().coerceIn(10, 100)

    if (rawPct < 55 && boughtItems.isNotEmpty() && MISTAKE_LOW_CORE_MATCH !in mistakeTags) {
      mistakeTags += MISTAKE_LOW_CORE_MATCH
    }

    if (feedbackEn.isEmpty() && boughtItems.isNotEmpty()) {
      feedbackEn += "Solid itemization alignment ($rawPct% match) with situational counter coverage."
      feedbackTh += "ออกไอเทมได้สอดคล้องกับแผนรับมือ ($rawPct%) ครอบคลุมตัวแก้ทางหลักได้ดี"
    }

    return BuildAccuracyReport(
      accuracyPct = rawPct,
      matchedItems = matchedCore.distinctBy { it.id },
      recommendedItems = fullRecommendedList,
      missedCoreItems = missedCore,
      mistakeTags = mistakeTags.distinct(),
      feedbackEn = feedbackEn,
      feedbackTh = feedbackTh,
    )
  }

  fun resolveItemsByNames(rawNames: List<String>, catalog: List<ItemData>): List<ItemData> {
    if (rawNames.isEmpty() || catalog.isEmpty()) return emptyList()
    val byId = catalog.associateBy { normalize(it.id) }
    val byEn = catalog.associateBy { normalize(it.nameEn) }
    val byTh = catalog.filter { it.nameTh.isNotBlank() }.associateBy { normalize(it.nameTh) }

    return rawNames
      .mapNotNull { raw ->
        val norm = normalize(raw)
        if (norm.isBlank()) return@mapNotNull null
        val aliasId = ITEM_ALIASES[norm]
        (aliasId?.let { byId[normalize(it)] })
          ?: byId[norm]
          ?: byEn[norm]
          ?: byTh[norm]
          ?: catalog.firstOrNull {
            val en = normalize(it.nameEn)
            val th = normalize(it.nameTh)
            (en.length >= 4 && (norm in en || en in norm)) ||
              (th.length >= 4 && (norm in th || th in norm))
          }
      }
      .distinctBy { it.id }
      .take(7)
  }

  private fun normalize(v: String): String =
    v.lowercase()
      .replace("'", "")
      .replace("’", "")
      .replace("&", "and")
      .replace(Regex("[^\\p{L}\\p{N}]"), "")

  companion object {
    const val MISTAKE_MISSED_ANTI_HEAL = "MISSED_ANTI_HEAL"
    const val MISTAKE_MISSED_ANTI_SHIELD = "MISSED_ANTI_SHIELD"
    const val MISTAKE_MISSED_STASIS_QSS = "MISSED_STASIS_QSS"
    const val MISTAKE_MISSED_PEN = "MISSED_ARMOR_MR_PEN"
    const val MISTAKE_NO_ENCHANT = "NO_ENCHANT"
    const val MISTAKE_HIGH_DEATHS = "HIGH_DEATHS"
    const val MISTAKE_LOW_GPM = "LOW_GPM"
    const val MISTAKE_LOW_CORE_MATCH = "LOW_CORE_MATCH"

    private val ITEM_ALIASES =
      mapOf(
        "stasis" to "stasis_enchant",
        "zhonya" to "stasis_enchant",
        "zhonyas" to "stasis_enchant",
        "qss" to "quicksilver_enchant",
        "quicksilver" to "quicksilver_enchant",
        "steelcaps" to "plated_steelcaps",
        "ninjatabi" to "plated_steelcaps",
        "mercs" to "mercurys_treads",
        "mercurytreads" to "mercurys_treads",
        "deathcap" to "rabadons_deathcap",
        "rabadon" to "rabadons_deathcap",
        "bork" to "blade_of_the_ruined_king",
        "botrk" to "blade_of_the_ruined_king",
        "ie" to "infinity_edge",
        "rfc" to "rapid_firecannon",
        "pd" to "phantom_dancer",
        "ga" to "guardian_angel",
        "luden" to "ludens_echo",
        "ludensecho" to "ludens_echo",
        "triforce" to "trinity_force",
        "bc" to "black_cleaver",
        "roa" to "rod_of_ages",
        "morello" to "morellonomicon",
        "mortal" to "mortal_reminder",
        "serpents" to "serpents_fang",
        "trident" to "oceanids_trident",
        "twinguard" to "amaranths_twinguard",
        "crown" to "banshees_veil",
        "banshees" to "banshees_veil",
        "yuntal" to "yun_tal_wildarrows",
        "kaenic" to "kaenic_rookern",
      )
  }
}
