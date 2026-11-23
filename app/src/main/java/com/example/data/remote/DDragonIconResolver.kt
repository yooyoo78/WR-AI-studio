package com.example.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object DDragonIconResolver {
  private const val FALLBACK_DDRAGON_VERSION = "16.20.1"
  private const val VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json"
  private const val NORRA_PORTRAIT_URL =
    "https://leagueoflegends.fandom.com/wiki/Special:FilePath/NorraSquare_WR.png"

  @Volatile private var cachedLatestVersion: String? = null
  private val abilityCache = ConcurrentHashMap<String, Map<String, String>>()

  private fun baseCdn(version: String): String =
    "https://ddragon.leagueoflegends.com/cdn/$version"

  private fun resolveLatestVersionSync(): String {
    cachedLatestVersion?.let { return it }
    return try {
      val conn =
        (URL(VERSIONS_URL).openConnection() as HttpURLConnection).apply {
          connectTimeout = 3000
          readTimeout = 3000
          requestMethod = "GET"
        }
      if (conn.responseCode in 200..299) {
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val arr = JSONArray(body)
        val latest = arr.optString(0).takeIf { it.isNotBlank() } ?: FALLBACK_DDRAGON_VERSION
        cachedLatestVersion = latest
        latest
      } else {
        conn.disconnect()
        FALLBACK_DDRAGON_VERSION
      }
    } catch (_: Exception) {
      FALLBACK_DDRAGON_VERSION
    }
  }

  fun championKey(championId: String, nameEn: String): String =
    when (championId.lowercase()) {
      "dr_mundo" -> "DrMundo"
      "lee_sin" -> "LeeSin"
      "kha_zix" -> "Khazix"
      "kaisa" -> "Kaisa"
      "aurelion_sol" -> "AurelionSol"
      "jarvan_iv" -> "JarvanIV"
      "master_yi" -> "MasterYi"
      "miss_fortune" -> "MissFortune"
      "tahm_kench" -> "TahmKench"
      "twisted_fate" -> "TwistedFate"
      "xin_zhao" -> "XinZhao"
      "wukong" -> "MonkeyKing"
      "nunu", "nunu_willump" -> "Nunu"
      "ksante", "k_sante" -> "KSante"
      "chogath", "cho_gath" -> "Chogath"
      "reksai", "rek_sai" -> "Reksai"
      "kogmaw", "kog_maw" -> "KogMaw"
      "velkoz", "vel_koz" -> "Velkoz"
      "leblanc" -> "Leblanc"
      "renata_glasc" -> "Renata"
      "belveth" -> "Belveth"
      else -> nameEn.replace("'", "").replace(".", "").replace(" ", "").replace("&", "")
    }

  fun championPortraitUrl(championId: String, nameEn: String): String {
    if (championId.equals("norra", ignoreCase = true)) {
      return NORRA_PORTRAIT_URL
    }
    val key = championKey(championId, nameEn)
    val ver = cachedLatestVersion ?: FALLBACK_DDRAGON_VERSION
    return "${baseCdn(ver)}/img/champion/$key.png"
  }

  /**
   * Resolves exact passive (P) and active spell (Q, W, E, R) icon URLs from Riot Data Dragon's
   * authoritative champion JSON endpoint on demand, caching results per champion.
   * Returns emptyMap() safely for Wild Rift-exclusive champions (e.g. Norra) or when offline.
   */
  suspend fun resolveAbilityIcons(championId: String, nameEn: String): Map<String, String> {
    if (championId.equals("norra", ignoreCase = true)) {
      return emptyMap()
    }
    val key = championKey(championId, nameEn)
    abilityCache[key]?.let { return it }

    return withContext(Dispatchers.IO) {
      try {
        val version = resolveLatestVersionSync()
        val cdn = baseCdn(version)
        val url = URL("$cdn/data/en_US/champion/$key.json")
        val conn =
          (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            requestMethod = "GET"
          }
        if (conn.responseCode !in 200..299) {
          conn.disconnect()
          return@withContext emptyMap()
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val root = JSONObject(body)
        val champObj = root.getJSONObject("data").getJSONObject(key)
        val map = mutableMapOf<String, String>()

        val passiveFile = champObj.getJSONObject("passive").getJSONObject("image").getString("full")
        if (passiveFile.isNotBlank()) {
          map["P"] = "$cdn/img/passive/$passiveFile"
        }

        val spells = champObj.getJSONArray("spells")
        val slotNames = listOf("Q", "W", "E", "R")
        for (i in 0 until minOf(spells.length(), 4)) {
          val spellFile =
            spells.getJSONObject(i).getJSONObject("image").getString("full")
          if (spellFile.isNotBlank()) {
            map[slotNames[i]] = "$cdn/img/spell/$spellFile"
          }
        }
        abilityCache[key] = map
        map
      } catch (_: Exception) {
        emptyMap()
      }
    }
  }
}
