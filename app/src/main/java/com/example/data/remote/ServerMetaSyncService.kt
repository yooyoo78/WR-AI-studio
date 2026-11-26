package com.example.data.remote

import com.example.data.model.ChampionData
import com.example.data.model.TierRank
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ChampionMetaStatPatch(
  val championId: String,
  val winRatePct: Double,
  val pickRatePct: Double,
  val banRatePct: Double,
  val tierRank: TierRank,
)

data class CnMetaSyncOutput(
  val patchVersionHint: String?,
  val updates: List<ChampionMetaStatPatch>,
)

object ServerMetaSyncParser {
  private val moshi = Moshi.Builder().build()
  private val anyAdapter = moshi.adapter(Any::class.java)

  private val ALIAS_TO_LOCAL_ID =
    mapOf(
      "aurelionsol" to "aurelion_sol",
      "drmundo" to "dr_mundo",
      "jarvaniv" to "jarvan_iv",
      "kaisa" to "kaisa",
      "khazix" to "kha_zix",
      "leesin" to "lee_sin",
      "masteryi" to "master_yi",
      "missfortune" to "miss_fortune",
      "monkeyking" to "wukong",
      "nunu" to "nunu",
      "reksai" to "reksai",
      "twistedfate" to "twisted_fate",
      "velkoz" to "velkoz",
      "xinzhao" to "xin_zhao",
      "chogath" to "chogath",
      "ksante" to "ksante",
    )

  /**
   * Parses Tencent Wild Rift public heroList (`hero_list.js` JSON object with `heroList` map/array)
   * + `hero_rank_list_v2` JSON response into normalized [ChampionMetaStatPatch] records matched
   * against the local champion catalog. Uses Moshi (pure JVM) so it runs identically on Android
   * devices and JVM unit tests.
   */
  fun parseCnFeed(
    heroListRaw: String,
    rankListRaw: String,
    localChampions: List<ChampionData>,
  ): CnMetaSyncOutput {
    val cleanHeroText = stripJsWrapper(heroListRaw)
    val heroRoot = (anyAdapter.fromJson(cleanHeroText) as? Map<*, *>) ?: emptyMap<Any?, Any?>()
    val patchHint =
      (heroRoot["version"] as? String)?.takeIf { it.isNotBlank() }
        ?: (heroRoot["fileTime"] as? String)?.takeIf { it.isNotBlank() }

    val heroIdToLocalId = mutableMapOf<String, String>()
    val localByNorm =
      localChampions.associateBy { normalizeNameKey(it.nameEn) } +
        localChampions.associateBy { normalizeNameKey(it.id) }

    when (val heroListNode = heroRoot["heroList"]) {
      is Map<*, *> -> {
        heroListNode.forEach { (key, value) ->
          val obj = value as? Map<*, *> ?: return@forEach
          val heroId = strField(obj, "heroId").ifBlank { key?.toString().orEmpty() }
          val alias = strField(obj, "alias").ifBlank { strField(obj, "name") }
          resolveLocalId(alias, localByNorm)?.let { localId ->
            if (heroId.isNotBlank()) heroIdToLocalId[heroId] = localId
          }
        }
      }
      is List<*> -> {
        heroListNode.forEach { value ->
          val obj = value as? Map<*, *> ?: return@forEach
          val heroId = strField(obj, "heroId")
          val alias = strField(obj, "alias").ifBlank { strField(obj, "name") }
          resolveLocalId(alias, localByNorm)?.let { localId ->
            if (heroId.isNotBlank()) heroIdToLocalId[heroId] = localId
          }
        }
      }
    }

    val rankRoot = (anyAdapter.fromJson(rankListRaw) as? Map<*, *>) ?: emptyMap<Any?, Any?>()
    val dataNode = rankRoot["data"] ?: rankRoot

    val records = collectStatEntries(dataNode)
    val grouped = mutableMapOf<String, MutableList<RawStatEntry>>()
    records.forEach { rec ->
      val localId =
        heroIdToLocalId[rec.heroId]
          ?: rec.alias?.let { resolveLocalId(it, localByNorm) }
          ?: return@forEach
      grouped.getOrPut(localId) { mutableListOf() }.add(rec)
    }

    val patches =
      grouped.entries.mapNotNull { (localId, stats) ->
        if (stats.isEmpty()) return@mapNotNull null
        val primary = stats.maxByOrNull { it.pickRatePct } ?: stats.first()
        val wr = round1(primary.winRatePct.coerceIn(38.0, 62.0))
        val pr = round1(primary.pickRatePct.coerceIn(0.1, 60.0))
        val br = round1(primary.banRatePct.coerceIn(0.0, 85.0))
        ChampionMetaStatPatch(
          championId = localId,
          winRatePct = wr,
          pickRatePct = pr,
          banRatePct = br,
          tierRank = deriveTierRank(wr, pr, br),
        )
      }

    return CnMetaSyncOutput(patchVersionHint = patchHint, updates = patches)
  }

  private fun deriveTierRank(wr: Double, pr: Double, br: Double): TierRank {
    val score = (wr - 48.0) * 1.7 + (pr * 0.45) + (br * 0.22)
    return when {
      score >= 12.0 || (wr >= 52.5 && pr >= 8.0) -> TierRank.S_PLUS
      score >= 7.0 || wr >= 51.2 -> TierRank.S
      score >= 2.5 -> TierRank.A
      else -> TierRank.B
    }
  }

  private data class RawStatEntry(
    val heroId: String,
    val alias: String?,
    val winRatePct: Double,
    val pickRatePct: Double,
    val banRatePct: Double,
  )

  private fun collectStatEntries(node: Any?): List<RawStatEntry> {
    val out = mutableListOf<RawStatEntry>()
    when (node) {
      is List<*> -> {
        node.forEach { out += collectStatEntries(it) }
      }
      is Map<*, *> -> {
        val heroId = strField(node, "hero_id").ifBlank { strField(node, "heroId") }
        val winRaw = node["win_rate"] ?: node["winRate"] ?: node["win_rate_percent"]
        val pickRaw = node["appear_rate"] ?: node["pick_rate"] ?: node["appearRate"]
        val banRaw = node["forbid_rate"] ?: node["ban_rate"] ?: node["banRate"]
        if (heroId.isNotBlank() && winRaw != null && pickRaw != null) {
          val wr = parsePct(winRaw)
          val pr = parsePct(pickRaw)
          val br = parsePct(banRaw)
          if (wr > 0.0) {
            val alias =
              strField(node, "hero_name").ifBlank { strField(node, "alias") }.takeIf { it.isNotBlank() }
            out +=
              RawStatEntry(
                heroId = heroId,
                alias = alias,
                winRatePct = wr,
                pickRatePct = pr,
                banRatePct = br,
              )
          }
        } else {
          node.values.forEach { out += collectStatEntries(it) }
        }
      }
    }
    return out
  }

  private fun strField(map: Map<*, *>, key: String): String =
    when (val v = map[key]) {
      null -> ""
      is Number -> v.toLong().toString()
      else -> v.toString().trim()
    }

  private fun parsePct(raw: Any?): Double {
    if (raw == null) return 0.0
    val num =
      when (raw) {
        is Number -> raw.toDouble()
        else -> raw.toString().trim().removeSuffix("%").toDoubleOrNull() ?: return 0.0
      }
    return if (num in 0.001..1.0) num * 100.0 else num
  }

  private fun stripJsWrapper(raw: String): String {
    val trimmed = raw.trim()
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
  }

  private fun normalizeNameKey(raw: String): String =
    raw.lowercase().replace(Regex("[^a-z0-9]"), "")

  private fun resolveLocalId(aliasOrName: String, localByNorm: Map<String, ChampionData>): String? {
    val norm = normalizeNameKey(aliasOrName)
    if (norm.isBlank()) return null
    ALIAS_TO_LOCAL_ID[norm]?.let { mapped ->
      if (localByNorm.values.any { it.id == mapped }) return mapped
    }
    return localByNorm[norm]?.id
  }

  private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}

class ServerMetaSyncService(
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(22, TimeUnit.SECONDS)
      .build(),
) {
  suspend fun fetchCnRankedStats(localChampions: List<ChampionData>): Result<CnMetaSyncOutput> =
    withContext(Dispatchers.IO) {
      runCatching {
        val heroListRaw = fetchText(CN_HERO_LIST_URL)
        val rankListRaw = fetchText(CN_HERO_RANK_URL)
        val parsed = ServerMetaSyncParser.parseCnFeed(heroListRaw, rankListRaw, localChampions)
        if (parsed.updates.isEmpty()) {
          throw IOException("CN ranked feed returned 0 matched champion entries.")
        }
        parsed
      }
    }

  private fun fetchText(url: String): String {
    val req =
      Request.Builder()
        .url(url)
        .header("User-Agent", "WR-AI-Companion/1.1")
        .header("Accept", "application/json,text/javascript,*/*")
        .build()
    httpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IOException("Server stats HTTP ${resp.code} from ${url.substringBefore('?')}")
      }
      return resp.body?.string() ?: throw IOException("Empty response body")
    }
  }

  companion object {
    const val CN_HERO_LIST_URL = "https://game.gtimg.cn/images/lgamem/act/lrlib/js/heroList/hero_list.js"
    const val CN_HERO_RANK_URL = "https://mlol.qt.qq.com/go/lgame_battle_info/hero_rank_list_v2"
  }
}
