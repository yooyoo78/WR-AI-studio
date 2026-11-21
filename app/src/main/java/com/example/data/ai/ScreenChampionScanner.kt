package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.domain.engine.ChampionScanMatchResult
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

private const val TAG = "ScreenChampionScanner"
private const val MAX_IMAGE_DIM_PX = 1280

sealed interface ScreenScanOutcome {
  data class Success(val result: ChampionScanMatchResult) : ScreenScanOutcome

  data class Failure(val message: String) : ScreenScanOutcome
}

class ScreenChampionScanner(
  private val context: Context,
  private val matcher: ChampionScanMatcher = ChampionScanMatcher(),
) {
  private val scanSchema =
    Schema.obj(
      properties =
        mapOf(
          "screenType" to
            Schema.enumeration(
              values = listOf("CHAMP_SELECT", "LOADING_SCREEN", "SCOREBOARD", "UNKNOWN"),
              description = "Type of Wild Rift screen shown in the screenshot",
            ),
          "myChampionName" to
            Schema.string(
              description =
                "Exact English name of the player's own highlighted champion if clearly identifiable, else empty string",
              nullable = true,
            ),
          "detectedLane" to
            Schema.enumeration(
              values = listOf("BARON", "MID", "DRAGON", "SUPPORT", "JUNGLE", "UNKNOWN"),
              description = "Detected lane of the player if visible, else UNKNOWN",
            ),
          "allies" to
            Schema.array(
              items =
                Schema.string(
                  description =
                    "Exact English Wild Rift champion name for each picked allied slot only"
                ),
              description =
                "Allied team picked champions only. Skip any empty, picking, or unpicked slots.",
            ),
          "enemies" to
            Schema.array(
              items =
                Schema.string(
                  description =
                    "Exact English Wild Rift champion name for each picked enemy slot only"
                ),
              description =
                "Enemy team picked champions only. Skip any empty, picking, unpicked slots, and small ban icons.",
            ),
          "skippedEmptySlots" to
            Schema.integer(
              description = "Count of player slots that have not picked a champion yet and were skipped"
            ),
          "confidenceNote" to
            Schema.string(
              description = "Short 1-sentence note describing what was detected or skipped"
            ),
        ),
      optionalProperties = listOf("myChampionName", "confidenceNote"),
    )

  private val scanConfig by lazy {
    generationConfig {
      temperature = 0.1f
      maxOutputTokens = 768
      responseMimeType = "application/json"
      responseSchema = scanSchema
    }
  }

  private val primaryModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = scanConfig,
    )
  }

  private val fallbackModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig = scanConfig,
    )
  }

  suspend fun scanUri(
    uri: Uri,
    catalog: List<ChampionData>,
    language: AppLanguage,
  ): ScreenScanOutcome {
    val bitmap =
      withContext(Dispatchers.IO) { decodeScaledBitmapFromUri(uri) }
        ?: return ScreenScanOutcome.Failure(
          if (language == AppLanguage.TH) {
            "ไม่สามารถอ่านรูปภาพหน้าจอได้ กรุณาลองแคปหรือเลือกภาพใหม่อีกครั้ง"
          } else {
            "Could not decode screenshot image. Please capture or pick the image again."
          }
        )
    return scanBitmap(bitmap, catalog, language)
  }

  suspend fun scanBitmap(
    sourceBitmap: Bitmap,
    catalog: List<ChampionData>,
    language: AppLanguage,
  ): ScreenScanOutcome {
    val appCtx = context.applicationContext
    if (FirebaseApp.getApps(appCtx).isEmpty()) {
      FirebaseApp.initializeApp(appCtx)
    }
    if (FirebaseApp.getApps(appCtx).isEmpty()) {
      return ScreenScanOutcome.Failure(
        if (language == AppLanguage.TH) {
          "ยังไม่ได้ตั้งค่า Firebase ในบิลด์นี้ (ไม่พบ google-services.json) จึงยังไม่สามารถสแกนภาพด้วย AI ได้"
        } else {
          "Firebase is not configured in this build (missing google-services.json), so AI vision scan is unavailable."
        }
      )
    }

    val scaledBitmap =
      withContext(Dispatchers.Default) { prepareScaledBitmap(sourceBitmap) }

    val prompt = buildVisionPrompt(language)
    val inputContent = content {
      image(scaledBitmap)
      text(prompt)
    }

    var primaryError: Exception? = null
    try {
      val response = withTimeout(35_000L) { primaryModel.generateContent(inputContent) }
      val rawJson = response.text?.trim().orEmpty()
      if (rawJson.isNotEmpty()) {
        val parsed = parseRawPayload(rawJson)
        val matched = matcher.resolve(parsed, catalog)
        return ScreenScanOutcome.Success(matched)
      }
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Primary vision scan timed out; trying fallback lite model", e)
      primaryError = e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (GeminiCoachService.shouldFallbackToSecondaryModel(e)) {
        Log.w(TAG, "Primary vision scan failed (${e::class.simpleName}); trying fallback", e)
        primaryError = e
      } else {
        Log.e(TAG, "Vision scan failed on primary model", e)
        return ScreenScanOutcome.Failure(GeminiCoachService.buildDetailedErrorMessage(e, language))
      }
    }

    // Fallback model attempt
    return try {
      val response = withTimeout(25_000L) { fallbackModel.generateContent(inputContent) }
      val rawJson = response.text?.trim().orEmpty()
      if (rawJson.isEmpty()) {
        ScreenScanOutcome.Failure(
          if (language == AppLanguage.TH) {
            "ไม่พบข้อมูลแชมเปี้ยนในภาพหน้าจอ กรุณาลองแคปให้เห็นหน้าต่างเลือกตัวชัดเจนขึ้น"
          } else {
            "No champion data returned from screenshot. Capture a clearer view of champion select or loading screen."
          }
        )
      } else {
        val parsed = parseRawPayload(rawJson)
        val matched = matcher.resolve(parsed, catalog)
        ScreenScanOutcome.Success(matched)
      }
    } catch (e: TimeoutCancellationException) {
      val prior = primaryError
      if (prior != null && GeminiCoachService.classifyError(prior) == CoachErrorKind.QUOTA_LIMIT) {
        ScreenScanOutcome.Failure(GeminiCoachService.buildDetailedErrorMessage(prior, language))
      } else {
        ScreenScanOutcome.Failure(
          if (language == AppLanguage.TH) {
            "หมดเวลาสแกนภาพหน้าจอ (60 วินาที) กรุณาลองใหม่อีกครั้ง"
          } else {
            "Screenshot scan timed out after 60 seconds. Please retry."
          }
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Vision scan fallback failed", e)
      ScreenScanOutcome.Failure(GeminiCoachService.buildDetailedErrorMessage(e, language))
    }
  }

  private fun parseRawPayload(jsonString: String): RawChampionScanPayload {
    val json = JSONObject(jsonString)
    val screenType = json.optString("screenType", "UNKNOWN")
    val myChamp =
      if (json.isNull("myChampionName")) null else json.optString("myChampionName").takeIf { it.isNotBlank() }
    val detectedLane = json.optString("detectedLane", "UNKNOWN")
    val alliesArr = json.optJSONArray("allies")
    val allies =
      if (alliesArr != null) {
        (0 until alliesArr.length()).mapNotNull { idx -> alliesArr.optString(idx)?.takeIf { it.isNotBlank() } }
      } else emptyList()
    val enemiesArr = json.optJSONArray("enemies")
    val enemies =
      if (enemiesArr != null) {
        (0 until enemiesArr.length()).mapNotNull { idx -> enemiesArr.optString(idx)?.takeIf { it.isNotBlank() } }
      } else emptyList()
    val skippedEmptySlots = json.optInt("skippedEmptySlots", 0)
    val note = json.optString("confidenceNote", "")
    return RawChampionScanPayload(
      screenType = screenType,
      myChampionName = myChamp,
      detectedLane = detectedLane,
      allies = allies,
      enemies = enemies,
      skippedEmptySlots = skippedEmptySlots,
      confidenceNote = note,
    )
  }

  private fun decodeScaledBitmapFromUri(uri: Uri): Bitmap? {
    return try {
      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOpts)
      }
      val maxOrig = max(boundsOpts.outWidth, boundsOpts.outHeight)
      var sampleSize = 1
      while (maxOrig / sampleSize > MAX_IMAGE_DIM_PX * 2) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed decoding screenshot URI: $uri", e)
      null
    }
  }

  private fun prepareScaledBitmap(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    val resized =
      if (maxSide > MAX_IMAGE_DIM_PX && maxSide > 0) {
        val ratio = MAX_IMAGE_DIM_PX.toFloat() / maxSide.toFloat()
        val w = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (source.height * ratio).roundToInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(source, w, h, true)
      } else {
        source
      }
    // Re-encode JPEG briefly if bitmap byte count is huge so inline payload stays well under 20MB
    val out = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: resized
  }

  private fun buildVisionPrompt(language: AppLanguage): String {
    val noteLang = if (language == AppLanguage.TH) "Thai" else "English"
    return """
      Analyze this League of Legends: Wild Rift screenshot (Champion Select, Loading Screen, or Scoreboard).
      Rules:
      1. Identify ALL currently picked/locked Allied Team champions (usually left side in Champ Select, top/blue team in Loading/Scoreboard) and Enemy Team champions (usually right side in Champ Select, bottom/red team in Loading/Scoreboard).
      2. IMPORTANT: Some player slots may NOT have picked/locked a champion yet (empty slot, silhouette, "Picking...", "Selecting...", or blank). SKIP those unpicked slots completely — do NOT guess or invent champions for empty slots, and count how many player slots look unpicked in `skippedEmptySlots`.
      3. IGNORE small greyed-out/crossed-out Ban champion icons at the top/corners of Champion Select. Only include actual picked player slots.
      4. Use exact official English Wild Rift champion names (e.g., "Lee Sin", "Kai'Sa", "Dr. Mundo", "Miss Fortune", "Nunu & Willump", "Aurelion Sol", "Wukong", "Twisted Fate").
      5. If the player's own slot is highlighted (yellow/gold frame, "You"/"คุณ", or player highlight), set `myChampionName` to that champion and `detectedLane` to their lane badge if visible (BARON/MID/DRAGON/SUPPORT/JUNGLE).
      6. Write `confidenceNote` concisely in $noteLang.
    """.trimIndent()
  }
}
