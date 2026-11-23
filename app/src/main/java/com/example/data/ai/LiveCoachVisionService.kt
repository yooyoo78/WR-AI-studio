package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.local.SecureApiKeyStore
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.domain.engine.ChampionScanMatcher
import com.example.domain.engine.RawChampionScanPayload
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "LiveCoachVisionService"
private const val LIVE_MAX_IMAGE_DIM_PX = 1280

enum class LiveScreenPhase(val labelTh: String, val labelEn: String) {
  CHAMP_SELECT("หน้าดราฟต์ / เลือกตัว", "Champ Select / Draft"),
  LOADING_SCREEN("หน้าโหลดก่อนเริ่มเกม", "Loading Screen"),
  IN_GAME("ระหว่างเกม / สกอร์บอร์ด", "In-Game / Scoreboard"),
  UNKNOWN("วิเคราะห์หน้าจอเกม", "Live Screen Scan"),
}

data class LivePickSuggestion(
  val championName: String,
  val resolvedChampion: ChampionData?,
  val reason: String,
)

data class LiveCoachAnalysisResult(
  val phase: LiveScreenPhase,
  val myChampion: ChampionData?,
  val detectedLane: Lane,
  val allies: List<ChampionData>,
  val enemies: List<ChampionData>,
  val skippedUnpickedCount: Int,
  val headline: String,
  val recommendedPicks: List<LivePickSuggestion>,
  val recommendedItems: List<String>,
  val earlyLanePlan: String,
  val threatAlert: String,
  val spokenBriefing: String,
  val usedModelLabel: String = "",
)

sealed interface LiveCoachOutcome {
  data class Success(val result: LiveCoachAnalysisResult) : LiveCoachOutcome

  data class Failure(val message: String) : LiveCoachOutcome
}

class LiveCoachVisionService(
  private val context: Context,
  private val matcher: ChampionScanMatcher = ChampionScanMatcher(),
  private val secureKeyStore: SecureApiKeyStore? = null,
  private val externalClient: ExternalAiProvidersClient = ExternalAiProvidersClient(),
) {
  private val liveSchema =
    Schema.obj(
      properties =
        mapOf(
          "screenPhase" to
            Schema.enumeration(
              values = listOf("CHAMP_SELECT", "LOADING_SCREEN", "IN_GAME", "UNKNOWN"),
              description = "Current Wild Rift screen phase",
            ),
          "myChampionName" to
            Schema.string(
              description =
                "Player's highlighted champion name if picked/visible, else empty string",
              nullable = true,
            ),
          "detectedLane" to
            Schema.enumeration(
              values = listOf("BARON", "MID", "DRAGON", "SUPPORT", "JUNGLE", "UNKNOWN"),
              description = "Player's lane if visible, else UNKNOWN",
            ),
          "allies" to
            Schema.array(
              items = Schema.string(),
              description = "Picked allied champions only; skip empty/unpicked slots",
            ),
          "enemies" to
            Schema.array(
              items = Schema.string(),
              description = "Picked enemy champions only; skip empty slots and small ban icons",
            ),
          "skippedEmptySlots" to
            Schema.integer(description = "Number of empty/unpicked player slots skipped"),
          "headline" to
            Schema.string(description = "Ultra-concise 1-line tactical headline"),
          "recommendedPicks" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "championName" to Schema.string(description = "Recommended Wild Rift champion name"),
                      "reason" to Schema.string(description = "Short 1-sentence counter/synergy reason"),
                    )
                ),
              description = "Top 2-3 recommended picks/counters if in CHAMP_SELECT; empty otherwise",
            ),
          "recommendedItems" to
            Schema.array(
              items = Schema.string(),
              description =
                "2-4 concise Wild Rift item/boot enchant recommendations tailored to the visible matchup",
            ),
          "earlyLanePlan" to
            Schema.string(
              description =
                "Concise 2-3 bullet/sentence early game wave, trading, and positioning advice against visible opponents"
            ),
          "threatAlert" to
            Schema.string(
              description = "1 concise sentence identifying the biggest visible enemy threat or spike"
            ),
          "spokenBriefing" to
            Schema.string(
              description =
                "Natural 2-3 sentence spoken briefing summarizing the top pick or item + early lane plan"
            ),
        ),
      optionalProperties = listOf("myChampionName", "recommendedPicks"),
    )

  private val liveConfig by lazy {
    generationConfig {
      temperature = 0.25f
      maxOutputTokens = 1024
      responseMimeType = "application/json"
      responseSchema = liveSchema
    }
  }

  private val primaryModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH.remoteModelId,
      generationConfig = liveConfig,
    )
  }

  private val fallbackModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH_LITE.remoteModelId,
      generationConfig = liveConfig,
    )
  }

  suspend fun analyzeUri(
    uri: Uri,
    catalog: List<ChampionData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
    preferredModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  ): LiveCoachOutcome {
    val bitmap =
      withContext(Dispatchers.IO) { decodeScaledBitmapFromUri(uri) }
        ?: return LiveCoachOutcome.Failure(
          if (language == AppLanguage.TH) {
            "ไม่สามารถอ่านภาพหน้าจอได้ กรุณาลองแคปหรือเลือกภาพใหม่อีกครั้ง"
          } else {
            "Could not decode screenshot image. Please try capturing or picking again."
          }
        )
    return analyzeBitmap(
      sourceBitmap = bitmap,
      catalog = catalog,
      fallbackChampion = fallbackChampion,
      fallbackLane = fallbackLane,
      patchVersion = patchVersion,
      language = language,
      preferredModelId = preferredModelId,
    )
  }

  suspend fun analyzeBitmap(
    sourceBitmap: Bitmap,
    catalog: List<ChampionData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
    preferredModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  ): LiveCoachOutcome {
    val appCtx = context.applicationContext
    if (FirebaseApp.getApps(appCtx).isEmpty()) {
      try {
        FirebaseApp.initializeApp(appCtx)
      } catch (_: Exception) {
        // Handled cleanly below
      }
    }
    val firebaseReady = FirebaseApp.getApps(appCtx).isNotEmpty()
    val keyStatus = secureKeyStore?.keyStatusFlow?.value ?: ExternalApiKeyStatus()

    val candidates = buildVisionCandidates(preferredModelId, firebaseReady, keyStatus)
    if (candidates.isEmpty()) {
      return LiveCoachOutcome.Failure(
        if (language == AppLanguage.TH) {
          "โมเดลที่เลือกไม่รองรับการสแกนภาพ หรือยังไม่ได้ตั้งค่า Key ฟรี (AI Studio / Groq Vision)"
        } else {
          "No vision-capable AI model available. Switch to Auto/Firebase or add an AI Studio / Groq free key."
        }
      )
    }

    val scaledBitmap = withContext(Dispatchers.Default) { prepareScaledBitmap(sourceBitmap) }
    val prompt = buildLiveCoachPrompt(fallbackChampion, fallbackLane, patchVersion, language)
    val isManualLock = preferredModelId != AiModelCatalog.AUTO_MODEL_ID
    var lastError: Exception? = null

    for (spec in candidates) {
      try {
        val rawJson =
          if (spec.provider == AiProviderId.FIREBASE_AI) {
            val fbModel =
              if (spec.id == AiModelCatalog.FIREBASE_FLASH_LITE.id) fallbackModel else primaryModel
            val inputContent = content {
              image(scaledBitmap)
              text(prompt)
            }
            val response = withTimeout(35_000L) { fbModel.generateContent(inputContent) }
            response.text?.trim().orEmpty()
          } else {
            val apiKey = secureKeyStore?.getKey(spec.provider).orEmpty()
            if (apiKey.isBlank()) {
              throw ExternalAiHttpException(401, spec.provider, "Missing saved API key")
            }
            withTimeout(40_000L) {
              externalClient.generateVisionJson(spec, apiKey, scaledBitmap, prompt)
            }
          }

        if (rawJson.isNotEmpty()) {
          return LiveCoachOutcome.Success(
            parseAndResolve(
              jsonString = rawJson,
              catalog = catalog,
              fallbackChampion = fallbackChampion,
              fallbackLane = fallbackLane,
              usedModelLabel = spec.displayName,
            )
          )
        }
      } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
          Log.w(TAG, "Live Coach ${spec.displayName} timed out", e)
          lastError = e
          if (isManualLock) break
        } else {
          throw e
        }
      } catch (e: Exception) {
        Log.w(TAG, "Live Coach ${spec.displayName} failed (${e::class.simpleName}): ${e.message}", e)
        lastError = e
        if (isManualLock) break
        if (!GeminiCoachService.shouldFallbackToSecondaryModel(e) &&
          GeminiCoachService.classifyError(e) == CoachErrorKind.NETWORK
        ) {
          break
        }
      }
    }

    val err = lastError
    return if (err is TimeoutCancellationException) {
      LiveCoachOutcome.Failure(
        if (language == AppLanguage.TH) {
          "หมดเวลาเชื่อมต่อ Live Coach กรุณาลองเปลี่ยนโมเดลหรือสแกนใหม่อีกครั้ง"
        } else {
          "Live Coach analysis timed out. Try switching models or retry."
        }
      )
    } else if (err != null) {
      LiveCoachOutcome.Failure(GeminiCoachService.buildDetailedErrorMessage(err, language))
    } else {
      LiveCoachOutcome.Failure(
        if (language == AppLanguage.TH) {
          "Live Coach ไม่ได้รับข้อมูลจากภาพนี้ ลองแคปหน้าเลือกตัวหรือหน้าโหลดใหม่อีกครั้ง"
        } else {
          "Live Coach returned empty analysis. Capture a clearer Draft or Loading screen."
        }
      )
    }
  }

  private fun buildVisionCandidates(
    preferredModelId: String,
    firebaseReady: Boolean,
    keyStatus: ExternalApiKeyStatus,
  ): List<AiModelSpec> {
    if (preferredModelId != AiModelCatalog.AUTO_MODEL_ID) {
      val spec = AiModelCatalog.findById(preferredModelId)
      if (!spec.supportsVision || spec.id == AiModelCatalog.AUTO_MODEL_ID) {
        // Fallback smoothly to Auto Vision pool if user picked a text-only model for chat
      } else {
        if (spec.provider == AiProviderId.FIREBASE_AI && !firebaseReady) return emptyList()
        if (spec.provider.requiresApiKey && !keyStatus.hasKeyFor(spec.provider)) return emptyList()
        return listOf(spec)
      }
    }

    return buildList {
      if (firebaseReady) {
        add(AiModelCatalog.FIREBASE_FLASH)
        add(AiModelCatalog.FIREBASE_FLASH_LITE)
      }
      if (keyStatus.hasAiStudioKey) {
        add(AiModelCatalog.AI_STUDIO_FLASH)
        add(AiModelCatalog.AI_STUDIO_FLASH_LITE)
      }
      if (keyStatus.hasGroqKey) {
        add(AiModelCatalog.GROQ_LLAMA4_SCOUT)
      }
    }
  }

  internal fun parseAndResolve(
    jsonString: String,
    catalog: List<ChampionData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    usedModelLabel: String = "",
  ): LiveCoachAnalysisResult {
    val json = JSONObject(ExternalAiProvidersClient.stripCodeFence(jsonString))
    val rawPhase = json.optString("screenPhase", "UNKNOWN")
    val phase =
      when (rawPhase.uppercase()) {
        "CHAMP_SELECT" -> LiveScreenPhase.CHAMP_SELECT
        "LOADING_SCREEN" -> LiveScreenPhase.LOADING_SCREEN
        "IN_GAME" -> LiveScreenPhase.IN_GAME
        else -> LiveScreenPhase.UNKNOWN
      }

    val myChampName =
      if (json.isNull("myChampionName")) null
      else json.optString("myChampionName").takeIf { it.isNotBlank() }
    val detectedLaneRaw = json.optString("detectedLane", "UNKNOWN")
    val alliesArr = json.optJSONArray("allies")
    val alliesRaw =
      if (alliesArr != null) {
        (0 until alliesArr.length()).mapNotNull { alliesArr.optString(it)?.takeIf { s -> s.isNotBlank() } }
      } else emptyList()
    val enemiesArr = json.optJSONArray("enemies")
    val enemiesRaw =
      if (enemiesArr != null) {
        (0 until enemiesArr.length()).mapNotNull { enemiesArr.optString(it)?.takeIf { s -> s.isNotBlank() } }
      } else emptyList()

    val skippedEmpty = json.optInt("skippedEmptySlots", 0)

    val matchResult =
      matcher.resolve(
        RawChampionScanPayload(
          screenType = rawPhase,
          myChampionName = myChampName,
          detectedLane = detectedLaneRaw,
          allies = alliesRaw,
          enemies = enemiesRaw,
          skippedEmptySlots = skippedEmpty,
          confidenceNote = "",
        ),
        catalog,
      )

    val index = buildSimpleLookup(catalog)
    val picksArr = json.optJSONArray("recommendedPicks")
    val picks =
      if (picksArr != null) {
        (0 until picksArr.length()).mapNotNull { idx ->
          val obj = picksArr.optJSONObject(idx) ?: return@mapNotNull null
          val name = obj.optString("championName").trim()
          val reason = obj.optString("reason").trim()
          if (name.isBlank()) null
          else {
            val resolved = matcher.matchChampion(name, index, catalog)
            LivePickSuggestion(
              championName = resolved?.nameEn ?: name,
              resolvedChampion = resolved,
              reason = reason,
            )
          }
        }
      } else emptyList()

    val itemsArr = json.optJSONArray("recommendedItems")
    val items =
      if (itemsArr != null) {
        (0 until itemsArr.length()).mapNotNull { itemsArr.optString(it)?.trim()?.takeIf { s -> s.isNotBlank() } }
      } else emptyList()

    val resolvedChamp = matchResult.myChampion ?: fallbackChampion
    val resolvedLane =
      if (detectedLaneRaw.equals("UNKNOWN", ignoreCase = true)) {
        resolvedChamp?.lanes?.firstOrNull() ?: fallbackLane
      } else {
        matchResult.detectedLane
      }

    return LiveCoachAnalysisResult(
      phase = phase,
      myChampion = resolvedChamp,
      detectedLane = resolvedLane,
      allies = matchResult.allies,
      enemies = matchResult.enemies,
      skippedUnpickedCount = matchResult.skippedUnpickedCount,
      headline = json.optString("headline", "").trim(),
      recommendedPicks = picks.take(3),
      recommendedItems = items.take(4),
      earlyLanePlan = json.optString("earlyLanePlan", "").trim(),
      threatAlert = json.optString("threatAlert", "").trim(),
      spokenBriefing = json.optString("spokenBriefing", "").trim(),
      usedModelLabel = usedModelLabel,
    )
  }

  private fun buildSimpleLookup(catalog: List<ChampionData>): Map<String, ChampionData> {
    val map = LinkedHashMap<String, ChampionData>(catalog.size * 3)
    catalog.forEach { c ->
      map[normalize(c.id)] = c
      map[normalize(c.nameEn)] = c
      if (c.nameTh.isNotBlank()) map[normalize(c.nameTh)] = c
    }
    return map
  }

  private fun normalize(v: String): String =
    v.lowercase().replace("&", "and").replace(Regex("[^\\p{L}\\p{N}]"), "")

  private fun decodeScaledBitmapFromUri(uri: Uri): Bitmap? {
    return try {
      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOpts)
      }
      val maxOrig = max(boundsOpts.outWidth, boundsOpts.outHeight)
      var sampleSize = 1
      while (maxOrig / sampleSize > LIVE_MAX_IMAGE_DIM_PX * 2) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed decoding Live Coach URI: $uri", e)
      null
    }
  }

  private fun prepareScaledBitmap(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    val resized =
      if (maxSide > LIVE_MAX_IMAGE_DIM_PX && maxSide > 0) {
        val ratio = LIVE_MAX_IMAGE_DIM_PX.toFloat() / maxSide.toFloat()
        val w = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (source.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(source, w, h, true)
      } else {
        source
      }
    val out = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: resized
  }

  private fun buildLiveCoachPrompt(
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
  ): String {
    val langRule =
      if (language == AppLanguage.TH) {
        "Write headline, reasons, earlyLanePlan, threatAlert, and spokenBriefing in concise, practical Thai (keep Wild Rift champion & item names in English such as Stasis, QSS, Anti-Heal, Steelcaps)."
      } else {
        "Write headline, reasons, earlyLanePlan, threatAlert, and spokenBriefing in concise, practical English."
      }
    val contextHint =
      fallbackChampion?.let {
        "Current app context (use only if player's champion is not clearly visible): ${it.nameEn} (${fallbackLane.labelEn})."
      } ?: "Fallback lane context: ${fallbackLane.labelEn}."

    return """
      You are Live Coach for League of Legends: Wild Rift (Patch $patchVersion).
      Inspect this live game screenshot and provide ultra-concise, immediate tactical coaching:
      1. Identify `screenPhase`:
         - `CHAMP_SELECT` (drafting / picking champions),
         - `LOADING_SCREEN` (match loading splash cards before spawn),
         - `IN_GAME` (scoreboard or live lane gameplay),
         - `UNKNOWN`.
      2. Extract picked `allies` and `enemies` using exact official Wild Rift English champion names. SKIP any empty/unpicked slots ("Picking...", blank silhouette) and IGNORE small ban icons. Count skipped empty player slots in `skippedEmptySlots`.
      3. Tailored Live Coaching:
         - If `CHAMP_SELECT`: Give 2–3 top counter/synergy `recommendedPicks` for the player's lane/team with 1-sentence `reason`, plus 2–3 core/counter `recommendedItems` to prepare, and a crisp early game plan.
         - If `LOADING_SCREEN` or `IN_GAME`: Leave `recommendedPicks` empty, give 3–4 exact Wild Rift items & boot enchants (`recommendedItems` e.g. Plated Steelcaps + Stasis, Oblivion Orb early, Serpent's Fang), give a punchy `earlyLanePlan` (Lv 1–5 wave control, trading, level-up spikes against the visible enemy laner/jungler), and `threatAlert`.
      4. Use strictly Wild Rift Patch 7.3f items/mechanics (no PC LoL Mythics or Doran's items; use P/Q/W/E/R ability labels and Wild Rift Active slot items like Stasis/Quicksilver/Gargoyle/Mikael's/Stridebreaker).
      5. Keep `spokenBriefing` to 2–3 natural spoken sentences suitable for reading aloud before the match starts.
      6. $langRule
      7. $contextHint
    """.trimIndent()
  }
}
