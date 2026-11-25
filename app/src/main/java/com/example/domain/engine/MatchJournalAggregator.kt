package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import kotlin.math.roundToInt

data class ChampionJournalStats(
  val championId: String,
  val championName: String,
  val games: Int,
  val wins: Int,
  val winRatePct: Int,
  val avgKda: Double,
  val avgBuildAccuracyPct: Int,
)

data class LaneJournalStats(
  val lane: Lane,
  val games: Int,
  val wins: Int,
  val winRatePct: Int,
  val avgBuildAccuracyPct: Int,
)

data class RecurringMistakeStat(
  val tag: String,
  val count: Int,
  val labelEn: String,
  val labelTh: String,
  val fixTipEn: String,
  val fixTipTh: String,
) {
  fun label(lang: AppLanguage) = if (lang == AppLanguage.TH) labelTh else labelEn

  fun fixTip(lang: AppLanguage) = if (lang == AppLanguage.TH) fixTipTh else fixTipEn
}

data class MatchJournalSummary(
  val totalGames: Int = 0,
  val wins: Int = 0,
  val losses: Int = 0,
  val winRatePct: Int = 0,
  val avgKda: Double = 0.0,
  val avgBuildAccuracyPct: Int = 0,
  val perChampionStats: List<ChampionJournalStats> = emptyList(),
  val perLaneStats: List<LaneJournalStats> = emptyList(),
  val recurringMistakes: List<RecurringMistakeStat> = emptyList(),
)

object MatchJournalAggregator {
  fun summarize(
    entries: List<MatchJournalEntryData>,
    championsById: Map<String, ChampionData>,
    language: AppLanguage,
  ): MatchJournalSummary {
    if (entries.isEmpty()) return MatchJournalSummary()

    val total = entries.size
    val wins = entries.count { it.isWin }
    val losses = total - wins
    val wr = ((wins.toDouble() / total) * 100.0).roundToInt()
    val avgKda = entries.map { kdaRatio(it.kills, it.deaths, it.assists) }.average()
    val avgAcc = entries.map { it.buildAccuracyPct }.average().roundToInt()

    val champStats =
      entries
        .groupBy { it.championId }
        .map { (champId, list) ->
          val champWins = list.count { it.isWin }
          val champName = championsById[champId]?.displayName(language) ?: champId
          ChampionJournalStats(
            championId = champId,
            championName = champName,
            games = list.size,
            wins = champWins,
            winRatePct = ((champWins.toDouble() / list.size) * 100.0).roundToInt(),
            avgKda = list.map { kdaRatio(it.kills, it.deaths, it.assists) }.average(),
            avgBuildAccuracyPct = list.map { it.buildAccuracyPct }.average().roundToInt(),
          )
        }
        .sortedWith(compareByDescending<ChampionJournalStats> { it.games }.thenByDescending { it.winRatePct })

    val laneStats =
      entries
        .groupBy { it.lane }
        .map { (lane, list) ->
          val lWins = list.count { it.isWin }
          LaneJournalStats(
            lane = lane,
            games = list.size,
            wins = lWins,
            winRatePct = ((lWins.toDouble() / list.size) * 100.0).roundToInt(),
            avgBuildAccuracyPct = list.map { it.buildAccuracyPct }.average().roundToInt(),
          )
        }
        .sortedByDescending { it.games }

    val mistakeCounts =
      entries
        .flatMap { it.mistakeTags }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { (tag, count) -> mapMistakeStat(tag, count) }

    return MatchJournalSummary(
      totalGames = total,
      wins = wins,
      losses = losses,
      winRatePct = wr,
      avgKda = avgKda,
      avgBuildAccuracyPct = avgAcc,
      perChampionStats = champStats,
      perLaneStats = laneStats,
      recurringMistakes = mistakeCounts,
    )
  }

  /** Compact 2-line player memory block injected into AI Coach prompts for personalized advice. */
  fun buildCoachPlayerMemory(
    entries: List<MatchJournalEntryData>,
    championsById: Map<String, ChampionData>,
    championFilterId: String?,
  ): String {
    if (entries.isEmpty()) return ""
    val recent = entries.take(20)
    val summary = summarize(recent, championsById, AppLanguage.EN)
    val topChamps =
      summary.perChampionStats.take(3).joinToString(", ") {
        "${it.championName} (${it.wins}W-${it.games - it.wins}L, ${it.avgBuildAccuracyPct}% build)"
      }
    val topMistakes =
      summary.recurringMistakes.take(2).joinToString("; ") { "${it.labelEn} (${it.count}x)" }
    val champFocus =
      championFilterId?.let { id ->
        summary.perChampionStats.firstOrNull { it.championId == id }?.let {
          " On current champion ${it.championName}: ${it.games} games, ${it.winRatePct}% WR, ${"%.1f".format(it.avgKda)} KDA."
        }.orEmpty()
      }.orEmpty()

    return buildString {
      append(
        "Player Match Journal (${summary.totalGames} recent games: ${summary.winRatePct}% WR, avg ${"%.1f".format(summary.avgKda)} KDA, avg ${summary.avgBuildAccuracyPct}% Build Accuracy)."
      )
      if (topChamps.isNotBlank()) append(" Top Picks: $topChamps.")
      if (topMistakes.isNotBlank()) append(" Recurring Habits to Coach: $topMistakes.")
      append(champFocus)
    }.trim()
  }

  fun kdaRatio(kills: Int, deaths: Int, assists: Int): Double =
    (kills + assists).toDouble() / deaths.coerceAtLeast(1).toDouble()

  fun mapMistakeStat(tag: String, count: Int): RecurringMistakeStat =
    when (tag) {
      BuildAccuracyEngine.MISTAKE_MISSED_ANTI_HEAL ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Missed Anti-Heal",
          labelTh = "ลืมออกไอเทมตัดฮีล",
          fixTipEn = "Grab Oblivion Orb / Executioner's / Bramble Vest (800g) before Dragon 2.",
          fixTipTh = "แวะซื้อชิ้นส่วนตัดฮีล 800g ก่อนไฟต์มังกร 2 เมื่อเจอตัวดูดเลือด/ฮีล",
        )
      BuildAccuracyEngine.MISTAKE_MISSED_ANTI_SHIELD ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Missed Anti-Shield",
          labelTh = "ลืมออกไอเทมเจาะเกราะขาว",
          fixTipEn = "Build Serpent's Fang or Oceanid's Trident vs Lulu/Janna/Lux comps.",
          fixTipTh = "ออก Serpent's Fang หรือ Oceanid's Trident ตัดโล่ซัพพอร์ต/แชมป์เกราะขาว",
        )
      BuildAccuracyEngine.MISTAKE_MISSED_STASIS_QSS ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Delayed Stasis / QSS",
          labelTh = "ออก Stasis / QSS ช้าหรือผิดชิ้น",
          fixTipEn = "Prioritize Stasis vs burst/dive or Quicksilver vs unavoidable lockdown.",
          fixTipTh = "อัป Boot Enchant เป็น Stasis กันเบิร์สต์ หรือ Quicksilver แก้สตัน",
        )
      BuildAccuracyEngine.MISTAKE_NO_ENCHANT ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Missing Boot Enchant",
          labelTh = "ไม่ได้อัปเกรด Boot Enchant",
          fixTipEn = "Finish your active Boot Enchant by 2nd item / objective fight.",
          fixTipTh = "อย่าปล่อยรองเท้าว่าง รีบอัป Active Enchant ช่วงไอเทมชิ้นที่ 2",
        )
      BuildAccuracyEngine.MISTAKE_MISSED_PEN ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Lacked Tank Penetration",
          labelTh = "ขาดเจาะเกราะ / %HP ตีแทงก์",
          fixTipEn = "Add Mortal Reminder, Serylda's, Liandry's, or Infinity Orb vs tanks.",
          fixTipTh = "เจอแนวหน้าหนาต้องเสริมเจาะเกราะ/เจาะเวทเป็นชิ้นที่ 3–4",
        )
      BuildAccuracyEngine.MISTAKE_HIGH_DEATHS ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Over-Trading / High Deaths",
          labelTh = "ตายบ่อยเกิน (เสียจังหวะสไปก์)",
          fixTipEn = "Track enemy jungler on minimap before pushing past river.",
          fixTipTh = "เช็คป่าฝั่งตรงข้ามก่อนดันเวฟเกินกลางเลน และรอคูลดาวน์ก่อนเข้าซ้ำ",
        )
      BuildAccuracyEngine.MISTAKE_LOW_GPM ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Low Gold Per Minute",
          labelTh = "GPM ต่ำกว่ามาตรฐาน",
          fixTipEn = "Catch side waves between skirmishes instead of mid-lane ARAMing.",
          fixTipTh = "สลับเก็บครีปเลนข้างระหว่างรอไฟต์แทนการยืนรุมกลางเลนเปล่าๆ",
        )
      else ->
        RecurringMistakeStat(
          tag = tag,
          count = count,
          labelEn = "Off-Plan Core Build",
          labelTh = "บิลด์หลักหลุดแผนแก้ทาง",
          fixTipEn = "Review 1v5 Smart Build right in Loading Screen.",
          fixTipTh = "เช็คบิลด์แก้ทางใน 1v5 Analyzer ช่วงหน้าโหลดก่อนซื้อของ",
        )
    }
}
