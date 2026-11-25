package com.example.domain.engine

import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import kotlin.math.roundToInt

enum class TopMetaSortMode(val labelEn: String, val labelTh: String) {
  MOST_PLAYED("Top Played", "ฮิต+เมต้า"),
  HIGHEST_WINRATE("Win Rate", "ชนะสูงสุด"),
  BAN_PRIORITY("Ban Rate", "แบนสูงสุด"),
}

data class TopMetaChampionEntry(
  val champion: ChampionData,
  val rank: Int,
  val metaCompositeScore: Double,
  val personalGames: Int = 0,
  val personalWinRatePct: Int? = null,
)

data class TopMetaSliceResult(
  val entries: List<TopMetaChampionEntry> = emptyList(),
  val avgWinRatePct: Double = 0.0,
  val avgPickRatePct: Double = 0.0,
)

object TopMetaRankingEngine {

  fun computeMetaCompositeScore(champion: ChampionData): Double {
    val tierBonus = (4 - champion.tierRank.sortOrder).coerceAtLeast(0) * 2.4
    val winDelta = (champion.winRatePct - 46.0).coerceAtLeast(0.0) * 1.35
    val pickWeight = champion.pickRatePct * 1.45
    return ((pickWeight + winDelta + tierBonus) * 10.0).roundToInt() / 10.0
  }

  fun computeTopChampions(
    champions: List<ChampionData>,
    journalEntries: List<MatchJournalEntryData> = emptyList(),
    laneFilter: Lane? = null,
    sortMode: TopMetaSortMode = TopMetaSortMode.MOST_PLAYED,
    limit: Int = 5,
  ): TopMetaSliceResult {
    if (champions.isEmpty() || limit <= 0) return TopMetaSliceResult()

    val filtered =
      champions.filter { laneFilter == null || laneFilter in it.lanes }
    if (filtered.isEmpty()) return TopMetaSliceResult()

    val personalByChamp =
      journalEntries
        .filter { laneFilter == null || it.lane == laneFilter }
        .groupBy { it.championId }
        .mapValues { (_, list) ->
          val wins = list.count { it.isWin }
          val wr = ((wins.toDouble() / list.size) * 100.0).roundToInt()
          list.size to wr
        }

    val comparator =
      when (sortMode) {
        TopMetaSortMode.MOST_PLAYED ->
          compareByDescending<ChampionData> { computeMetaCompositeScore(it) }
            .thenByDescending { it.pickRatePct }
            .thenByDescending { it.winRatePct }
            .thenBy { it.tierRank.sortOrder }
        TopMetaSortMode.HIGHEST_WINRATE ->
          compareByDescending<ChampionData> { it.winRatePct }
            .thenByDescending { it.pickRatePct }
            .thenBy { it.tierRank.sortOrder }
        TopMetaSortMode.BAN_PRIORITY ->
          compareByDescending<ChampionData> { it.banRatePct }
            .thenByDescending { it.pickRatePct }
            .thenByDescending { it.winRatePct }
      }

    val topSlice = filtered.sortedWith(comparator).take(limit)
    val entries =
      topSlice.mapIndexed { idx, champ ->
        val personal = personalByChamp[champ.id]
        TopMetaChampionEntry(
          champion = champ,
          rank = idx + 1,
          metaCompositeScore = computeMetaCompositeScore(champ),
          personalGames = personal?.first ?: 0,
          personalWinRatePct = personal?.second,
        )
      }

    val avgWr =
      ((entries.map { it.champion.winRatePct }.average()) * 10.0).roundToInt() / 10.0
    val avgPr =
      ((entries.map { it.champion.pickRatePct }.average()) * 10.0).roundToInt() / 10.0

    return TopMetaSliceResult(
      entries = entries,
      avgWinRatePct = avgWr,
      avgPickRatePct = avgPr,
    )
  }
}
