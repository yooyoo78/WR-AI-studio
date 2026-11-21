package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiCoachService"
private const val MAX_CACHE_ENTRIES = 25

sealed interface CoachStreamEvent {
  data class Chunk(val accumulatedText: String) : CoachStreamEvent

  data class Completed(val finalText: String) : CoachStreamEvent

  data class Error(val message: String) : CoachStreamEvent
}

internal enum class CoachErrorKind {
  APP_CHECK,
  QUOTA_LIMIT,
  NETWORK,
  OTHER,
}

class GeminiCoachService(private val context: Context? = null) {
  private val coachConfig by lazy {
    generationConfig {
      temperature = 0.6f
      maxOutputTokens = 1024
    }
  }

  // Lazily initialized per firebase-ai-logic-android guidelines
  private val primaryModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = coachConfig,
    )
  }
  private val fallbackModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig = coachConfig,
    )
  }

  private val responseCache =
    object : LinkedHashMap<String, String>(MAX_CACHE_ENTRIES + 1, 0.75f, true) {
      override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
        size > MAX_CACHE_ENTRIES
    }

  fun clearCache() {
    synchronized(responseCache) { responseCache.clear() }
  }

  fun streamCoachingAdvice(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
  ): Flow<CoachStreamEvent> = flow {
    val cacheKey = buildCacheKey(question, champion, lane, enemies, patchVersion, language)
    val cached = synchronized(responseCache) { responseCache[cacheKey] }
    if (!cached.isNullOrBlank()) {
      emit(CoachStreamEvent.Completed(cached))
      return@flow
    }

    val appCtx = context?.applicationContext
    if (appCtx != null && FirebaseApp.getApps(appCtx).isEmpty()) {
      FirebaseApp.initializeApp(appCtx)
    }
    if (appCtx != null && FirebaseApp.getApps(appCtx).isEmpty()) {
      emit(
        CoachStreamEvent.Error(
          if (language == AppLanguage.TH) {
            "ยังไม่ได้ตั้งค่า Firebase ในไฟล์ APK นี้ (ไม่พบ app/google-services.json ตอน Build) กรุณาใช้แท็บ 'คู่มือโค้ชออฟไลน์' หรือเพิ่ม google-services.json ก่อน Build APK ใหม่"
          } else {
            "Firebase is not configured in this APK build (missing app/google-services.json at compile time). Use the 'Offline Playbook' tab or rebuild with google-services.json."
          }
        )
      )
      return@flow
    }

    val prompt = buildTacticalPrompt(question, champion, lane, enemies, patchVersion, language)
    val accumulated = StringBuilder()
    var shouldTryFallback = false
    var primaryFailure: Exception? = null

    try {
      withTimeout(35_000L) {
        primaryModel.generateContentStream(prompt).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          if (chunkText.isNotEmpty()) {
            accumulated.append(chunkText)
            emit(CoachStreamEvent.Chunk(accumulated.toString()))
          }
        }
      }
      if (accumulated.isNotBlank()) {
        val result = accumulated.toString().trim()
        synchronized(responseCache) { responseCache[cacheKey] = result }
        emit(CoachStreamEvent.Completed(result))
        return@flow
      } else {
        shouldTryFallback = true
      }
    } catch (e: TimeoutCancellationException) {
      Log.w(
        TAG,
        "Primary gemini-3.8-flash stream timed out at 35s (partial=${accumulated.isNotEmpty()})",
        e,
      )
      if (accumulated.isNotBlank()) {
        emit(CoachStreamEvent.Completed(accumulated.toString().trim()))
        return@flow
      }
      shouldTryFallback = true
      primaryFailure = e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (shouldFallbackToSecondaryModel(e)) {
        Log.w(
          TAG,
          "Primary gemini-3.8-flash failed (${e::class.simpleName}); falling back to gemini-3.5-flash-lite",
          e,
        )
        shouldTryFallback = true
        primaryFailure = e
      } else {
        Log.e(TAG, "Gemini AI Coach failed on primary model", e)
        emit(CoachStreamEvent.Error(buildDetailedErrorMessage(e, language)))
        return@flow
      }
    }

    // Fallback: if primary timed out empty or hit model quota/rate-limit/transient error, try gemini-3.5-flash-lite
    if (shouldTryFallback) {
      try {
        val unaryResponse = withTimeout(25_000L) { fallbackModel.generateContent(prompt) }
        val text = unaryResponse.text?.trim().orEmpty()
        if (text.isNotEmpty()) {
          synchronized(responseCache) { responseCache[cacheKey] = text }
          emit(CoachStreamEvent.Completed(text))
        } else {
          emit(
            CoachStreamEvent.Error(
              if (language == AppLanguage.TH) {
                "โมเดลไม่ส่งคำตอบกลับมา กรุณาลองใหม่อีกครั้ง หรือดูคู่มือโค้ชออฟไลน์"
              } else {
                "Gemini returned an empty response. Please try again or use the Offline Playbook."
              }
            )
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini AI Coach fallback request also timed out", e)
        val prior = primaryFailure
        if (prior != null && classifyError(prior) == CoachErrorKind.QUOTA_LIMIT) {
          emit(CoachStreamEvent.Error(buildDetailedErrorMessage(prior, language)))
        } else {
          emit(
            CoachStreamEvent.Error(
              if (language == AppLanguage.TH) {
                "หมดเวลาเชื่อมต่อ AI Coach (60 วินาที) กรุณาตรวจสอบอินเทอร์เน็ตแล้วกดลองใหม่ หรือดูคู่มือโค้ชออฟไลน์"
              } else {
                "AI Coach timed out after 60 seconds. Check your connection and retry, or use the Offline Playbook."
              }
            )
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Gemini AI Coach fallback failed", e)
        emit(CoachStreamEvent.Error(buildDetailedErrorMessage(e, language)))
      }
    }
  }

  private fun buildCacheKey(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
  ): String {
    val enemyKey = enemies.map { it.id }.sorted().joinToString(",")
    return listOf(
        champion?.id.orEmpty(),
        lane.name,
        enemyKey,
        patchVersion,
        language.code,
        question.trim().lowercase(),
      )
      .joinToString("|")
  }

  internal companion object {
    fun classifyError(e: Throwable): CoachErrorKind {
      val combined =
        buildString {
            append(e::class.simpleName.orEmpty())
            append(' ')
            append(e.localizedMessage.orEmpty())
            append(' ')
            append(e.message.orEmpty())
            e.cause?.let { c ->
              append(' ')
              append(c::class.simpleName.orEmpty())
              append(' ')
              append(c.message.orEmpty())
            }
          }
          .lowercase()

      return when {
        "403" in combined || "app check" in combined || "attestation" in combined ->
          CoachErrorKind.APP_CHECK
        "quota" in combined ||
          "429" in combined ||
          "resource_exhausted" in combined ||
          "resource exhausted" in combined ||
          "rate limit" in combined ||
          "ratelimit" in combined ||
          "too many requests" in combined ||
          ("limit" in combined && ("exceeded" in combined || "reached" in combined || "exhausted" in combined)) ->
          CoachErrorKind.QUOTA_LIMIT
        "unable to resolve host" in combined ||
          "network" in combined ||
          "connect" in combined ||
          "unknownhost" in combined -> CoachErrorKind.NETWORK
        else -> CoachErrorKind.OTHER
      }
    }

    fun shouldFallbackToSecondaryModel(e: Throwable): Boolean {
      val kind = classifyError(e)
      if (kind == CoachErrorKind.QUOTA_LIMIT) return true
      val combined = "${e::class.simpleName.orEmpty()} ${e.message.orEmpty()}".lowercase()
      return "503" in combined ||
        "500" in combined ||
        "unavailable" in combined ||
        "overloaded" in combined
    }

    fun buildDetailedErrorMessage(e: Throwable, language: AppLanguage): String {
      val raw = e.localizedMessage ?: e.message ?: e::class.simpleName.orEmpty()
      return when (classifyError(e)) {
        CoachErrorKind.APP_CHECK ->
          if (language == AppLanguage.TH) {
            "การยืนยันสิทธิ์ Firebase App Check ไม่ผ่าน ($raw) หากทดสอบผ่าน Debug APK ให้รันผ่าน AI Studio Emulator หรือตรวจสอบการตั้งค่า Release App Check"
          } else {
            "Firebase App Check attestation failed ($raw). Run via AI Studio Emulator for debug builds or verify Release App Check config."
          }
        CoachErrorKind.QUOTA_LIMIT ->
          if (language == AppLanguage.TH) {
            "โควตา Gemini AI Coach เต็มชั่วคราว (ถึงลิมิตคำขอต่อนาที/ต่อวันของ Firebase ฟรีแพลน) กรุณารอ ~1 นาทีแล้วกด 'ลองใหม่' หรือใช้แท็บ 'คู่มือโค้ชออฟไลน์' ได้ทันทีโดยไม่จำกัดจำนวนครั้ง (รายละเอียด: $raw)"
          } else {
            "Gemini AI Coach quota/rate limit reached on Firebase free tier. Wait ~1 minute and tap 'Retry', use the unlimited 'Offline Playbook' tab, or upgrade your Firebase project to Blaze plan for higher limits. ($raw)"
          }
        CoachErrorKind.NETWORK ->
          if (language == AppLanguage.TH) {
            "ไม่สามารถเชื่อมต่อเครือข่ายได้ ($raw) กรุณาตรวจสอบอินเทอร์เน็ต"
          } else {
            "Network connection error ($raw). Please check your internet connection."
          }
        CoachErrorKind.OTHER ->
          if (language == AppLanguage.TH) {
            "เชื่อมต่อ Gemini AI Coach ไม่สำเร็จ: $raw"
          } else {
            "Gemini AI Coach error: $raw"
          }
      }
    }
  }

  private fun buildTacticalPrompt(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
  ): String {
    val langInstruction =
      if (language == AppLanguage.TH) {
        "Respond strictly in clear, concise, practical Thai gaming language (use English champion/item names like Stasis, QSS, Anti-Heal where Thai players naturally use them)."
      } else {
        "Respond strictly in clear, concise, practical English."
      }
    val champContext =
      champion?.let {
        "Player Champion: ${it.nameEn} (${it.roles.joinToString("/")}, Damage: ${it.damageType.name}, Lane: ${lane.labelEn}, Skill Order: ${it.skillOrder}, Keystone: ${it.defaultKeystoneId}, Core Items: ${it.coreItemIds.joinToString(", ")}, Boots/Enchant: ${it.defaultBootsId} + ${it.defaultEnchantId})."
      } ?: "No specific champion locked yet (Lane: ${lane.labelEn})."
    val enemyContext =
      if (enemies.isEmpty()) {
        "Enemy Team: Not specified."
      } else {
        "Enemy Team (up to 5): ${enemies.joinToString(", ") { "${it.nameEn} (${it.damageType.name})" }}."
      }

    return """
      You are an elite League of Legends: Wild Rift tactical coach (Patch $patchVersion).
      IMPORTANT: Use strictly Wild Rift mechanics (Skill 1/2/3/Ultimate instead of PC Q/W/E/R), Wild Rift items (boot enchants like Stasis/Quicksilver/Gargoyle/Glory/Repulsor/Veil/Meteor, Amaranth's Twinguard, Heartsteel, Oceanid's Trident, Sundered Sky, Terminus, Magnetic Blaster, Gluttonous Greaves, Boots of Mana/Dynamism, Crystalline Reflector), and Wild Rift runes (no PC LoL-only Mythics, Doran's items, or ward-quest support items).
      $langInstruction
      $champContext
      $enemyContext
      
      Give actionable, match-winning advice covering laning, itemization/enchant timing, wave control, objective setup, and teamfight positioning as relevant to the player's question. Keep formatting crisp with 4-6 short bullet points so it is fast and easy to read before or during champion select.
      
      Player Question: $question
    """
      .trimIndent()
  }
}
