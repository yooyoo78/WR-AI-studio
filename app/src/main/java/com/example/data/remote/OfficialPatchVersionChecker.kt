package com.example.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

enum class MetaServerRegion(
  val id: String,
  val shortLabel: String,
  val labelEn: String,
  val labelTh: String,
) {
  CN_DIAMOND_PLUS(
    id = "cn_diamond",
    shortLabel = "CN",
    labelEn = "CN Official (Diamond+)",
    labelTh = "เซิร์ฟจีน (CN Diamond+)",
  ),
  GLOBAL_SEA(
    id = "global_sea",
    shortLabel = "Global/SEA",
    labelEn = "Global / SEA (OTA)",
    labelTh = "Global / SEA (OTA)",
  );

  companion object {
    fun fromId(id: String?): MetaServerRegion =
      entries.firstOrNull { it.id == id } ?: CN_DIAMOND_PLUS
  }
}

data class OfficialPatchInfo(
  val patchVersion: String,
  val headline: String,
  val articleUrl: String,
)

sealed interface LiveVersionCheckState {
  val appPatch: String

  data class Idle(
    override val appPatch: String,
    val lastKnownOfficialPatch: String? = null,
  ) : LiveVersionCheckState

  data class Checking(override val appPatch: String) : LiveVersionCheckState

  data class Matched(
    override val appPatch: String,
    val officialPatch: String,
    val officialUrl: String,
    val checkedAtMs: Long,
  ) : LiveVersionCheckState

  data class Outdated(
    override val appPatch: String,
    val officialPatch: String,
    val officialUrl: String,
    val otaReady: VerifiedOtaDownload? = null,
    val otaMessage: String? = null,
    val checkedAtMs: Long,
  ) : LiveVersionCheckState

  data class Error(
    override val appPatch: String,
    val reason: String,
  ) : LiveVersionCheckState
}

sealed interface MetaRefreshState {
  data object Idle : MetaRefreshState

  data class Refreshing(val server: MetaServerRegion) : MetaRefreshState

  data class Synced(
    val server: MetaServerRegion,
    val patchVersion: String,
    val updatedCount: Int,
    val syncedAtMs: Long,
    val note: String,
  ) : MetaRefreshState

  data class Failed(
    val server: MetaServerRegion,
    val message: String,
  ) : MetaRefreshState
}

object OfficialPatchParser {
  private val PATCH_SLUG_REGEX =
    Regex("""href=["']([^"']*wild-rift-patch-notes-(\d+)-(\d+)([a-z]?)(?:-|\b)[^"']*)["']""", RegexOption.IGNORE_CASE)
  private val PATCH_TEXT_REGEX =
    Regex("""(?:Wild Rift\s+)?Patch\s+(?:Notes\s+)?(\d+\.\d+[a-z]?)""", RegexOption.IGNORE_CASE)

  fun normalizePatchVersion(raw: String): String {
    val clean = raw.trim().lowercase().removePrefix("v").removePrefix("patch").trim()
    val m = Regex("""(\d+)\.(\d+)([a-z]?)""").find(clean) ?: return clean
    return "${m.groupValues[1]}.${m.groupValues[2]}${m.groupValues[3]}"
  }

  /**
   * Semantic comparison for Wild Rift patches like "7.3f" < "7.3g" < "7.4" < "7.4a".
   */
  fun comparePatchVersions(aRaw: String, bRaw: String): Int {
    val pa = parseTokens(aRaw)
    val pb = parseTokens(bRaw)
    if (pa == null || pb == null) {
      return normalizePatchVersion(aRaw).compareTo(normalizePatchVersion(bRaw))
    }
    if (pa.major != pb.major) return pa.major.compareTo(pb.major)
    if (pa.minor != pb.minor) return pa.minor.compareTo(pb.minor)
    return pa.letterRank.compareTo(pb.letterRank)
  }

  fun extractLatestPatchFromHtml(html: String): OfficialPatchInfo? {
    if (html.isBlank()) return null

    // Prefer explicit Wild Rift patch notes links on game-updates feed
    val slugMatches =
      PATCH_SLUG_REGEX.findAll(html).mapNotNull { match ->
        val rawUrl = match.groupValues[1]
        val major = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        val minor = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
        val letter = match.groupValues[4].lowercase()
        val version = "$major.$minor$letter"
        val fullUrl =
          if (rawUrl.startsWith("http", ignoreCase = true)) rawUrl
          else "https://wildrift.leagueoflegends.com/${rawUrl.trimStart('/')}"
        OfficialPatchInfo(
          patchVersion = version,
          headline = "Wild Rift Patch Notes $version",
          articleUrl = fullUrl,
        )
      }.toList()

    if (slugMatches.isNotEmpty()) {
      return slugMatches.maxWithOrNull { x, y -> comparePatchVersions(x.patchVersion, y.patchVersion) }
    }

    // Fallback to text headings matching "Patch X.Y[a-z]"
    val textMatches =
      PATCH_TEXT_REGEX.findAll(html)
        .map { normalizePatchVersion(it.groupValues[1]) }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
    val latest = textMatches.maxWithOrNull { x, y -> comparePatchVersions(x, y) } ?: return null
    return OfficialPatchInfo(
      patchVersion = latest,
      headline = "Wild Rift Patch $latest",
      articleUrl = OfficialPatchVersionChecker.OFFICIAL_GAME_UPDATES_URL,
    )
  }

  private data class PatchTokens(val major: Int, val minor: Int, val letterRank: Int)

  private fun parseTokens(raw: String): PatchTokens? {
    val m = Regex("""(\d+)\.(\d+)([a-z]?)""").find(normalizePatchVersion(raw)) ?: return null
    val major = m.groupValues[1].toIntOrNull() ?: return null
    val minor = m.groupValues[2].toIntOrNull() ?: return null
    val letter = m.groupValues[3].firstOrNull()
    val rank = if (letter == null) 0 else (letter - 'a' + 1)
    return PatchTokens(major, minor, rank)
  }
}

class OfficialPatchVersionChecker(
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build(),
) {
  suspend fun fetchLatestOfficialPatch(): Result<OfficialPatchInfo> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request =
          Request.Builder()
            .url(OFFICIAL_GAME_UPDATES_URL)
            .header("User-Agent", "WR-AI-Companion/1.1")
            .header("Accept", "text/html,application/xhtml+xml")
            .build()
        httpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IOException("Official Wild Rift updates HTTP ${response.code}")
          }
          val html = response.body?.string().orEmpty()
          OfficialPatchParser.extractLatestPatchFromHtml(html)
            ?: throw IOException("Could not find patch version in official updates page")
        }
      }
    }

  companion object {
    const val OFFICIAL_GAME_UPDATES_URL = "https://wildrift.leagueoflegends.com/en-us/news/game-updates/"
  }
}
