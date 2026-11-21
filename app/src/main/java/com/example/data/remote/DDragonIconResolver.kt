package com.example.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object DDragonIconResolver {
  private const val DDRAGON_VERSION = "15.10.1"
  private const val BASE_CDN = "https://ddragon.leagueoflegends.com/cdn/$DDRAGON_VERSION"

  private val abilityCache = ConcurrentHashMap<String, Map<String, String>>()

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
      "kogmaw", "kog_maw" -> "KogMaw"
      "velkoz", "vel_koz" -> "Velkoz"
      "leblanc" -> "Leblanc"
      else -> nameEn.replace("'", "").replace(".", "").replace(" ", "").replace("&", "")
    }

  fun championPortraitUrl(championId: String, nameEn: String): String {
    val key = championKey(championId, nameEn)
    return "$BASE_CDN/img/champion/$key.png"
  }

  /**
   * Resolves exact passive (P) and active spell (1,2,3,4) icon URLs from Riot Data Dragon's
   * authoritative champion JSON endpoint on demand, caching results per champion.
   * Returns emptyMap() safely when offline so UI falls back to bundled URLs or Hextech badges.
   */
  suspend fun resolveAbilityIcons(championId: String, nameEn: String): Map<String, String> {
    val key = championKey(championId, nameEn)
    abilityCache[key]?.let { return it }

    return withContext(Dispatchers.IO) {
      try {
        val url = URL("$BASE_CDN/data/en_US/champion/$key.json")
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
          map["P"] = "$BASE_CDN/img/passive/$passiveFile"
        }

        val spells = champObj.getJSONArray("spells")
        val slotNames = listOf("1", "2", "3", "4")
        for (i in 0 until minOf(spells.length(), 4)) {
          val spellFile =
            spells.getJSONObject(i).getJSONObject("image").getString("full")
          if (spellFile.isNotBlank()) {
            map[slotNames[i]] = "$BASE_CDN/img/spell/$spellFile"
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
