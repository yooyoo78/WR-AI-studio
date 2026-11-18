package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.Lane
import com.example.data.model.MatchupRuleData
import kotlin.math.roundToInt

data class DraftWarning(
  val severityCritical: Boolean,
  val titleEn: String,
  val titleTh: String,
  val detailEn: String,
  val detailTh: String,
) {
  fun title(lang: AppLanguage) = if (lang == AppLanguage.TH) titleTh else titleEn

  fun detail(lang: AppLanguage) = if (lang == AppLanguage.TH) detailTh else detailEn
}

data class DraftPickSuggestion(
  val champion: ChampionData,
  val totalScore: Int,
  val reasonEn: String,
  val reasonTh: String,
) {
  fun reason(lang: AppLanguage) = if (lang == AppLanguage.TH) reasonTh else reasonEn
}

data class DraftAnalysisReport(
  val physicalSharePct: Int,
  val magicSharePct: Int,
  val trueHybridSharePct: Int,
  val frontlineScore: Int,
  val ccScore: Int,
  val engageScore: Int,
  val disengageScore: Int,
  val teamfightScore: Int,
  val splitPushScore: Int,
  val warnings: List<DraftWarning>,
  val recommendedPicks: List<DraftPickSuggestion>,
)

class DraftAssistantEngine {
  fun analyzeDraft(
    allies: List<ChampionData>,
    enemies: List<ChampionData>,
    intendedLane: Lane,
    preferredRole: ChampionRole?,
    allChampions: List<ChampionData>,
    rules: List<MatchupRuleData>,
  ): DraftAnalysisReport {
    val adCount = allies.count { it.damageType == DamageType.AD }
    val apCount = allies.count { it.damageType == DamageType.AP }
    val hybridTrueCount =
      allies.count { it.damageType == DamageType.HYBRID || it.damageType == DamageType.TRUE }
    val total = allies.size.coerceAtLeast(1)

    val physicalPct = ((adCount * 100.0) / total).roundToInt()
    val magicPct = ((apCount * 100.0) / total).roundToInt()
    val hybridPct = (100 - physicalPct - magicPct).coerceAtLeast(0)

    val frontlineScore =
      if (allies.isEmpty()) 0
      else ((allies.sumOf { it.tankinessRating } / (allies.size * 8.0)) * 100.0).roundToInt().coerceIn(0, 100)
    val ccScore =
      if (allies.isEmpty()) 0
      else ((allies.sumOf { it.ccRating } / (allies.size * 7.5)) * 100.0).roundToInt().coerceIn(0, 100)
    val engageScore =
      if (allies.isEmpty()) 0
      else
        ((allies.count { it.ccRating >= 7 && (it.tankinessRating >= 6 || it.mobilityRating >= 7) } *
            38))
          .coerceIn(0, 100)
    val disengageScore =
      if (allies.isEmpty()) 0
      else
        ((allies.count {
            it.id in listOf("lulu", "braum", "thresh", "ahri", "lux", "orianna", "jinx")
          } * 34))
          .coerceIn(0, 100)
    val teamfightScore =
      if (allies.isEmpty()) 0
      else
        (((allies.sumOf { it.midPower + it.latePower } / (allies.size * 18.0)) * 70 + ccScore * 0.3))
          .roundToInt()
          .coerceIn(0, 100)
    val splitPushScore =
      if (allies.isEmpty()) 0
      else
        ((allies.count {
            it.id in listOf("fiora", "camille", "zed", "yasuo", "darius", "garen")
          } * 35))
          .coerceIn(0, 100)

    val warnings = mutableListOf<DraftWarning>()
    if (
      allies.size >= 3 &&
        apCount == 0 &&
        allies.none { it.damageType == DamageType.HYBRID } &&
        adCount >= allies.size - 1
    ) {
      warnings.add(
        DraftWarning(
          severityCritical = true,
          titleEn = "All-AD Composition Risk",
          titleTh = "ทีมขาดดาเมจเวท (All-AD Risk)",
          detailEn = "Enemy tanks can rush Plated Steelcaps, Thornmail, and Frozen Heart freely.",
          detailTh = "แทงค์ศัตรูจะออกเกราะล้วน (Steelcaps/Thornmail/Frozen Heart) แก้ทางทั้งทีมได้ง่าย",
        )
      )
    }
    if (allies.size >= 3 && adCount == 0 && hybridTrueCount == 0) {
      warnings.add(
        DraftWarning(
          severityCritical = true,
          titleEn = "Heavy AP Overload",
          titleTh = "ทีมมีแต่ดาเมจเวท (Heavy AP)",
          detailEn = "Force of Nature + Mercury's Treads will shut down your team's damage.",
          detailTh = "ศัตรูออก Force of Nature + Mercury's Treads แล้วทีมจะตีไม่เข้า",
        )
      )
    }
    if (allies.size >= 2 && frontlineScore < 45) {
      warnings.add(
        DraftWarning(
          severityCritical = true,
          titleEn = "Missing Tank Frontline",
          titleTh = "ขาดแทงค์ยืนแนวหน้า",
          detailEn = "Your carries lack a frontline sponge for Dragon/Baron face-checks.",
          detailTh = "ทีมตัวบางเกินไป ไม่มีคนเปิดทางและรับสกิลหน้ามังกร/บารอน",
        )
      )
    }
    if (allies.size >= 2 && ccScore < 45) {
      warnings.add(
        DraftWarning(
          severityCritical = false,
          titleEn = "Low Hard Crowd Control",
          titleTh = "ขาดสกิล CC หยุดศัตรู",
          detailEn = "Mobile carries or assassins on the enemy team can freely dive.",
          detailTh = "ขาดสกิลสตั๊น/จับขัง ทำให้ตัวคล่องและแอสซาซินศัตรูเล่นง่าย",
        )
      )
    }

    val pickedIds = (allies.map { it.id } + enemies.map { it.id }).toSet()
    val candidates =
      allChampions.filter { champ ->
        champ.id !in pickedIds &&
          intendedLane in champ.lanes &&
          (preferredRole == null || preferredRole in champ.roles)
      }

    val scoredSuggestions =
      candidates
        .map { cand ->
          var score = 50 + (4 - cand.tierRank.sortOrder) * 6
          val reasonsEn = mutableListOf<String>()
          val reasonsTh = mutableListOf<String>()

          // Fix AD/AP imbalance
          if (apCount == 0 && cand.damageType == DamageType.AP) {
            score += 22
            reasonsEn.add("Balances team damage with strong AP.")
            reasonsTh.add("ช่วยเติมดาเมจเวทให้ทีมไม่โดนแก้ทางด้วยเกราะล้วน")
          } else if (adCount == 0 && cand.damageType == DamageType.AD) {
            score += 20
            reasonsEn.add("Adds needed physical DPS.")
            reasonsTh.add("ช่วยเติมดาเมจกายภาพให้ทีมสมดุล")
          }

          // Fix frontline gap
          if (frontlineScore < 50 && cand.tankinessRating >= 8) {
            score += 20
            reasonsEn.add("Supplies crucial frontline durability.")
            reasonsTh.add("เติมแนวหน้าและตัวรับดาเมจให้ทีม")
          }

          // Fix CC gap
          if (ccScore < 50 && cand.ccRating >= 7) {
            score += 15
            reasonsEn.add("Brings reliable engage/peel CC.")
            reasonsTh.add("เติมสกิล CC สำหรับเปิดไฟต์และกันแครี่")
          }

          // Enemy counters
          val counteredEnemies =
            enemies.filter { enemy ->
              enemy.id in cand.favorableAgainstIds ||
                rules.any {
                  it.champAId == cand.id && it.champBId == enemy.id && it.advantageScore >= 2
                }
            }
          if (counteredEnemies.isNotEmpty()) {
            score += counteredEnemies.size * 12
            val namesEn = counteredEnemies.joinToString(", ") { it.nameEn }
            val namesTh = counteredEnemies.joinToString(", ") { it.nameTh }
            reasonsEn.add("Counters enemy $namesEn.")
            reasonsTh.add("แก้ทางฝั่งศัตรู ($namesTh)")
          }

          // Penalize if countered by enemies
          val dangerEnemies = enemies.filter { enemy -> enemy.id in cand.counterChampionIds }
          score -= dangerEnemies.size * 10

          DraftPickSuggestion(
            champion = cand,
            totalScore = score.coerceIn(10, 99),
            reasonEn =
              reasonsEn.joinToString(" ").ifBlank {
                "Strong ${cand.tierRank.label}-tier ${intendedLane.labelEn} pick."
              },
            reasonTh =
              reasonsTh.joinToString(" ").ifBlank {
                "ตัวเลือก Tier ${cand.tierRank.label} ที่แข็งแกร่งใน ${intendedLane.labelTh}"
              },
          )
        }
        .sortedByDescending { it.totalScore }
        .take(5)

    return DraftAnalysisReport(
      physicalSharePct = physicalPct,
      magicSharePct = magicPct,
      trueHybridSharePct = hybridPct,
      frontlineScore = frontlineScore,
      ccScore = ccScore,
      engageScore = engageScore,
      disengageScore = disengageScore,
      teamfightScore = teamfightScore,
      splitPushScore = splitPushScore,
      warnings = warnings,
      recommendedPicks = scoredSuggestions,
    )
  }
}
