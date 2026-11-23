package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.Lane
import com.example.data.model.LaneDifficulty
import com.example.data.model.MatchupRuleData
import com.example.data.model.ThreatTag
import kotlin.math.roundToInt

data class EnemyMatchupAnalysis(
  val enemy: ChampionData,
  val advantageScore: Int, // -5 (hard counter) to +5 (strong advantage)
  val verdict: LaneDifficulty,
  val threatScore: Int, // 0..100 how dangerous this enemy is to myChampion
  val isLikelyLaneOpponent: Boolean,
  val dangerAbilitiesEn: String,
  val dangerAbilitiesTh: String,
  val tacticalTipEn: String,
  val tacticalTipTh: String,
  val keySpikeEn: String,
  val keySpikeTh: String,
) {
  fun dangerAbilities(lang: AppLanguage) =
    if (lang == AppLanguage.TH) dangerAbilitiesTh else dangerAbilitiesEn

  fun tacticalTip(lang: AppLanguage) = if (lang == AppLanguage.TH) tacticalTipTh else tacticalTipEn

  fun keySpike(lang: AppLanguage) = if (lang == AppLanguage.TH) keySpikeTh else keySpikeEn
}

data class EnemyTeamThreatProfile(
  val apBurstScore: Int,
  val adDpsScore: Int,
  val healingScore: Int,
  val tankinessScore: Int,
  val attackSpeedScore: Int,
  val ccScore: Int,
  val critScore: Int,
  val diveScore: Int,
  val apThreats: List<ChampionData>,
  val adThreats: List<ChampionData>,
  val healingThreats: List<ChampionData>,
  val tankThreats: List<ChampionData>,
  val attackSpeedThreats: List<ChampionData>,
  val ccThreats: List<ChampionData>,
  val critThreats: List<ChampionData>,
  val diveThreats: List<ChampionData>,
)

class MatchupCounterEngine {
  fun analyzeMatchups(
    myChampion: ChampionData,
    myLane: Lane,
    enemies: List<ChampionData>,
    explicitRules: List<MatchupRuleData>,
  ): Pair<List<EnemyMatchupAnalysis>, EnemyTeamThreatProfile> {
    val analyses =
      enemies
        .map { enemy -> analyzeSingleEnemy(myChampion, myLane, enemy, explicitRules) }
        .sortedWith(
          compareByDescending<EnemyMatchupAnalysis> { it.threatScore }
            .thenBy { it.advantageScore }
            .thenByDescending { it.enemy.burstRating }
        )

    val profile = buildTeamThreatProfile(enemies)
    return analyses to profile
  }

  private fun analyzeSingleEnemy(
    myChamp: ChampionData,
    myLane: Lane,
    enemy: ChampionData,
    rules: List<MatchupRuleData>,
  ): EnemyMatchupAnalysis {
    val directRule =
      rules.firstOrNull { it.champAId == myChamp.id && it.champBId == enemy.id }
    val reverseRule =
      rules.firstOrNull { it.champAId == enemy.id && it.champBId == myChamp.id }

    var score = 0
    val reasonsEn = mutableListOf<String>()
    val reasonsTh = mutableListOf<String>()

    when {
      directRule != null -> {
        score = directRule.advantageScore
        reasonsEn.add(directRule.tipEn)
        reasonsTh.add(directRule.tipTh)
      }
      reverseRule != null -> {
        score = -reverseRule.advantageScore
      }
    }

    // Explicit roster counter/favorable relationships
    if (enemy.id in myChamp.counterChampionIds && directRule == null && reverseRule == null) {
      score -= 3
    }
    if (enemy.id in myChamp.favorableAgainstIds && directRule == null && reverseRule == null) {
      score += 3
    }

    // Kit & stat compatibility heuristics
    if (
      ThreatTag.ASSASSIN_DIVE in enemy.threatTags &&
        myChamp.mobilityRating <= 4 &&
        myChamp.tankinessRating <= 4
    ) {
      score -= 2
      reasonsEn.add("${enemy.nameEn} can dive your immobile kit quickly.")
      reasonsTh.add("${enemy.nameTh} สามารถพุ่งล้วงตัวคุณที่ไม่มีสกิลหนีได้ง่าย")
    }
    if (
      ThreatTag.HEAVY_CC in enemy.threatTags &&
        ChampionRole.ASSASSIN in myChamp.roles &&
        myChamp.tankinessRating <= 5
    ) {
      score -= 1
      reasonsEn.add("${enemy.nameEn}'s hard CC disrupts your dive windows.")
      reasonsTh.add("CC ของ ${enemy.nameTh} ขัดจังหวะการพุ่งเข้าล้วงของคุณได้ดี")
    }
    if (
      myChamp.damageType == DamageType.TRUE &&
        ThreatTag.HIGH_HP_TANK in enemy.threatTags
    ) {
      score += 2
      reasonsEn.add("Your True Damage cuts directly through ${enemy.nameEn}'s resistances.")
      reasonsTh.add("True Damage ของคุณเจาะเกราะและเลือดของ ${enemy.nameTh} ได้โดยตรง")
    }
    if (
      enemy.id == "rammus" &&
        (ThreatTag.HIGH_ATTACK_SPEED in myChamp.threatTags ||
          ThreatTag.CRIT_HYPERCARRY in myChamp.threatTags)
    ) {
      score -= 3
      reasonsEn.add("Rammus W + Thornmail reflects your rapid auto-attacks.")
      reasonsTh.add("สกิล W + Thornmail ของ Rammus สะท้อนดาเมจใส่การโจมตีต่อเนื่องของคุณ")
    }
    if (
      enemy.id == "sylas" &&
        myChamp.id in listOf("malphite", "leona", "nautilus", "orianna")
    ) {
      score -= 2
      reasonsEn.add("Sylas gains massive value stealing your high-impact teamfight Ultimate.")
      reasonsTh.add("Sylas ได้เปรียบมากจากการขโมยอัลติเปิดไฟต์ของคุณไปใช้สเกล AP")
    }
    if (myChamp.earlyPower - enemy.earlyPower >= 3) {
      score += 1
      reasonsEn.add("You hold stronger early-game priority (${myChamp.earlyPower} vs ${enemy.earlyPower}).")
      reasonsTh.add("คุณได้เปรียบจังหวะต้นเกมมากกว่า (${myChamp.earlyPower} ต่อ ${enemy.earlyPower})")
    } else if (enemy.earlyPower - myChamp.earlyPower >= 3) {
      score -= 1
      reasonsEn.add("${enemy.nameEn} spikes earlier; respect levels 1-5.")
      reasonsTh.add("${enemy.nameTh} เก่งต้นเกมกว่า ควรระวังช่วงเลเวล 1-5")
    }

    val clampedScore = score.coerceIn(-5, 5)
    val verdict =
      when {
        clampedScore >= 2 -> LaneDifficulty.EASY
        clampedScore <= -2 -> LaneDifficulty.HARD
        directRule?.laneDifficulty == LaneDifficulty.SKILL -> LaneDifficulty.SKILL
        else -> LaneDifficulty.EVEN
      }

    val isLaneMatch = myLane in enemy.lanes

    // Threat score: combines disadvantage, lane presence, burst/assassin dive/CC danger
    var threat = 42 - (clampedScore * 7)
    if (isLaneMatch) threat += 12
    if (ThreatTag.ASSASSIN_DIVE in enemy.threatTags && myChamp.tankinessRating <= 5) threat += 10
    if (ChampionRole.ASSASSIN in enemy.roles && myChamp.tankinessRating <= 4) threat += 8
    if (enemy.burstRating >= 9 && myChamp.tankinessRating <= 4) threat += 5
    if (ThreatTag.HEAVY_CC in enemy.threatTags) threat += 5
    if (ThreatTag.HEAVY_HEALING in enemy.threatTags) threat += 4
    if (enemy.tierRank.sortOrder <= 1) threat += 4
    val clampedThreat = threat.coerceIn(15, 99)

    val dangerEn =
      directRule?.dangerAbilitiesEn
        ?: buildDefaultDangerEn(enemy)
    val dangerTh =
      directRule?.dangerAbilitiesTh
        ?: buildDefaultDangerTh(enemy)

    val tipEn =
      reasonsEn.firstOrNull()
        ?: if (clampedScore >= 0) {
          "Press your mid-wave priority and punish ${enemy.nameEn}'s cooldown windows."
        } else {
          "Play around key defensive cooldowns and avoid extended trades when ${enemy.nameEn} spikes."
        }
    val tipTh =
      reasonsTh.firstOrNull()
        ?: if (clampedScore >= 0) {
          "คุมจังหวะเวฟและกดดัน ${enemy.nameTh} ช่วงที่สกิลหลักติดคูลดาวน์"
        } else {
          "รักษาระยะและรอสกิลป้องกัน/ไอเทมแก้ทางก่อนเข้าปะทะกับ ${enemy.nameTh}"
        }

    val spikeEn =
      directRule?.keySpikeEn
        ?: when {
          enemy.earlyPower >= 8 -> "Early (Lvl 1-5)"
          enemy.midPower >= 9 -> "Mid (1-2 Items)"
          else -> "Late (3+ Items)"
        }
    val spikeTh =
      directRule?.keySpikeTh
        ?: when {
          enemy.earlyPower >= 8 -> "ต้นเกม (เลเวล 1-5)"
          enemy.midPower >= 9 -> "กลางเกม (1-2 ชิ้น)"
          else -> "ท้ายเกม (3+ ชิ้น)"
        }

    return EnemyMatchupAnalysis(
      enemy = enemy,
      advantageScore = clampedScore,
      verdict = verdict,
      threatScore = clampedThreat,
      isLikelyLaneOpponent = isLaneMatch,
      dangerAbilitiesEn = dangerEn,
      dangerAbilitiesTh = dangerTh,
      tacticalTipEn = tipEn,
      tacticalTipTh = tipTh,
      keySpikeEn = spikeEn,
      keySpikeTh = spikeTh,
    )
  }

  private fun buildDefaultDangerEn(enemy: ChampionData): String {
    val firstTag = enemy.threatTags.firstOrNull()?.labelEn ?: enemy.damageType.labelEn
    val ult = enemy.abilities.lastOrNull()?.nameEn ?: "Ultimate"
    return "$firstTag • Watch out for $ult"
  }

  private fun buildDefaultDangerTh(enemy: ChampionData): String {
    val firstTag = enemy.threatTags.firstOrNull()?.labelTh ?: enemy.damageType.labelTh
    val ult = enemy.abilities.lastOrNull()?.nameTh ?: "อัลติเมท"
    return "$firstTag • ระวังสกิล $ult"
  }

  private fun buildTeamThreatProfile(enemies: List<ChampionData>): EnemyTeamThreatProfile {
    if (enemies.isEmpty()) {
      return EnemyTeamThreatProfile(
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
      )
    }

    val apThreats =
      enemies.filter {
        it.damageType == DamageType.AP ||
          it.damageType == DamageType.HYBRID ||
          ThreatTag.HIGH_AP_BURST in it.threatTags
      }
    val adThreats =
      enemies.filter {
        it.damageType == DamageType.AD ||
          it.damageType == DamageType.TRUE ||
          ThreatTag.HIGH_AD_DPS in it.threatTags
      }
    val healingThreats =
      enemies.filter { ThreatTag.HEAVY_HEALING in it.threatTags || it.sustainRating >= 8 }
    val tankThreats =
      enemies.filter { ThreatTag.HIGH_HP_TANK in it.threatTags || it.tankinessRating >= 8 }
    val attackSpeedThreats =
      enemies.filter { ThreatTag.HIGH_ATTACK_SPEED in it.threatTags || it.dpsRating >= 8 }
    val ccThreats = enemies.filter { ThreatTag.HEAVY_CC in it.threatTags || it.ccRating >= 8 }
    val critThreats = enemies.filter { ThreatTag.CRIT_HYPERCARRY in it.threatTags }
    val diveThreats = enemies.filter { ThreatTag.ASSASSIN_DIVE in it.threatTags }

    val apScore =
      ((apThreats.sumOf { it.burstRating } / 32.0) * 100.0).roundToInt().coerceIn(0, 100)
    val adScore =
      ((adThreats.sumOf { maxOf(it.dpsRating, it.burstRating) } / 35.0) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
    val healScore = (healingThreats.size * 34).coerceIn(0, 100)
    val tankScore =
      ((tankThreats.sumOf { it.tankinessRating } / 24.0) * 100.0).roundToInt().coerceIn(0, 100)
    val asScore = (attackSpeedThreats.size * 32).coerceIn(0, 100)
    val ccScore =
      ((enemies.sumOf { it.ccRating } / 34.0) * 100.0).roundToInt().coerceIn(0, 100)
    val critScore = (critThreats.size * 38).coerceIn(0, 100)
    val diveScore = (diveThreats.size * 35).coerceIn(0, 100)

    return EnemyTeamThreatProfile(
      apBurstScore = apScore,
      adDpsScore = adScore,
      healingScore = healScore,
      tankinessScore = tankScore,
      attackSpeedScore = asScore,
      ccScore = ccScore,
      critScore = critScore,
      diveScore = diveScore,
      apThreats = apThreats,
      adThreats = adThreats,
      healingThreats = healingThreats,
      tankThreats = tankThreats,
      attackSpeedThreats = attackSpeedThreats,
      ccThreats = ccThreats,
      critThreats = critThreats,
      diveThreats = diveThreats,
    )
  }
}
