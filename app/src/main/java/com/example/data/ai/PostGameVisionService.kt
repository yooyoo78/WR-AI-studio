package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.local.SecureApiKeyStore
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.MatchupRuleData
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.domain.engine.BuildAccuracyEngine
import com.example.domain.engine.BuildAccuracyReport
import com.example.domain.engine.ChampionScanMatcher
import com.example.domain.engine.RawChampionScanPayload
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "PostGameVisionService"
private const val POST_GAME_MAX_IMAGE_DIM_PX = 1440

data class PostGameScanDraft(
  val isWin: Boolean,
  val myChampion: ChampionData?,
  val detectedLane: Lane,
  val kills: Int,
  val deaths: Int,
  val assists: Int,
  val goldEarned: Int,
  val durationMinutes: Int,
  val boughtItems: List<ItemData>,
  val enemyChampions: List<ChampionData>,
  val lessons: List<String>,
  val spokenBriefing: String,
  val usedModelLabel: String = "",
  val confidenceNote: String = "",
)

data class PostGameEvaluationResult(
  val draft: PostGameScanDraft,
  val accuracyReport: BuildAccuracyReport?,
)

sealed interface PostGameScanOutcome {
  data class Success(val result: PostGameEvaluationResult) : PostGameScanOutcome

  data class Failure(val message: String) : PostGameScanOutcome
}

class PostGameVisionService(
  private val context: Context,
  private val matcher: ChampionScanMatcher = ChampionScanMatcher(),
  private val accuracyEngine: BuildAccuracyEngine = BuildAccuracyEngine(),
  private val secureKeyStore: SecureApiKeyStore? = null,
  private val externalClient: ExternalAiProvidersClient = ExternalAiProvidersClient(),
) {
  private val moshi: Moshi = Moshi.Builder().build()
  private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
  private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

  private val postGameSchema =
    Schema.obj(
      properties =
        mapOf(
          "result" to Schema.enumeration(values = listOf("WIN", "LOSS"), description = "Match outcome"),
          "myChampionName" to Schema.string(description = "Player champion name in English"),
          "detectedLane" to
            Schema.enumeration(
              values = listOf("BARON", "MID", "DRAGON", "SUPPORT", "JUNGLE", "UNKNOWN"),
              description = "Player lane",
            ),
          "kills" to Schema.integer(description = "Player kills"),
          "deaths" to Schema.integer(description = "Player deaths"),
          "assists" to Schema.integer(description = "Player assists"),
          "goldEarned" to Schema.integer(description = "Player total gold earned, 0 if unreadable"),
          "durationMinutes" to Schema.integer(description = "Match duration in whole minutes, 18 if unreadable"),
          "boughtItems" to
            Schema.array(
              items = Schema.string(),
              description = "Exact English Wild Rift item, boot, and enchant names bought by the player",
            ),
          "enemyChampions" to
            Schema.array(
              items = Schema.string(),
              description = "Enemy team champion names visible on scoreboard",
            ),
          "lessons" to
            Schema.array(
              items = Schema.string(),
              description = "Exactly 3 concise actionable post-game lessons",
            ),
          "spokenBriefing" to
            Schema.string(description = "2-3 sentence spoken summary of the 3 post-game lessons"),
          "confidenceNote" to Schema.string(description = "Short extraction note"),
        ),
      optionalProperties = listOf("goldEarned", "durationMinutes", "confidenceNote"),
    )

  private val postGameConfig by lazy {
    generationConfig {
      temperature = 0.25f
      maxOutputTokens = 1200
      responseMimeType = "application/json"
      responseSchema = postGameSchema
    }
  }

  private val primaryVisionModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH.remoteModelId,
      generationConfig = postGameConfig,
    )
  }

  private val fallbackVisionModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH_LITE.remoteModelId,
      generationConfig = postGameConfig,
    )
  }

  suspend fun scanScoreboardUri(
    uri: Uri,
    champions: List<ChampionData>,
    items: List<ItemData>,
    runes: List<RuneData>,
    spells: List<SpellData>,
    matchupRules: List<MatchupRuleData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
    preferredModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  ): PostGameScanOutcome {
    val bitmap =
      withContext(Dispatchers.IO) { decodeScaledBitmapFromUri(uri) }
        ?: return PostGameScanOutcome.Failure(
          if (language == AppLanguage.TH) "ไม่สามารถเปิดไฟล์ภาพสกอร์บอร์ดได้" else "Unable to decode scoreboard screenshot."
        )
    return scanScoreboardBitmap(
      sourceBitmap = bitmap,
      champions = champions,
      items = items,
      runes = runes,
      spells = spells,
      matchupRules = matchupRules,
      fallbackChampion = fallbackChampion,
      fallbackLane = fallbackLane,
      patchVersion = patchVersion,
      language = language,
      preferredModelId = preferredModelId,
    )
  }

  suspend fun scanScoreboardBitmap(
    sourceBitmap: Bitmap,
    champions: List<ChampionData>,
    items: List<ItemData>,
    runes: List<RuneData>,
    spells: List<SpellData>,
    matchupRules: List<MatchupRuleData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
    preferredModelId: String = AiModelCatalog.AUTO_MODEL_ID,
  ): PostGameScanOutcome {
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
      return PostGameScanOutcome.Failure(
        if (language == AppLanguage.TH) {
          "ไม่พบโมเดล Vision ที่พร้อมใช้งาน กรุณาใส่ Key (AI Studio / Groq) หรือสลับกลับ Auto"
        } else {
          "No vision-capable model available. Add an AI Studio / Groq key or switch to Auto."
        }
      )
    }

    val scaledBitmap = withContext(Dispatchers.Default) { prepareScaledBitmap(sourceBitmap) }
    val prompt = buildScoreboardPrompt(fallbackChampion, fallbackLane, patchVersion, language)
    val isManualLock = preferredModelId != AiModelCatalog.AUTO_MODEL_ID
    var lastError: Exception? = null

    for (spec in candidates) {
      try {
        val rawJson =
          if (spec.provider == AiProviderId.FIREBASE_AI) {
            val fbModel =
              if (spec.id == AiModelCatalog.FIREBASE_FLASH_LITE.id) fallbackVisionModel
              else primaryVisionModel
            val contentInput = content {
              image(scaledBitmap)
              text(prompt)
            }
            withTimeout(40_000L) { fbModel.generateContent(contentInput) }.text?.trim().orEmpty()
          } else {
            val apiKey = secureKeyStore?.getKey(spec.provider).orEmpty()
            if (apiKey.isBlank()) throw ExternalAiHttpException(401, spec.provider, "Missing API key")
            withTimeout(40_000L) {
              externalClient.generateVisionJson(spec, apiKey, scaledBitmap, prompt)
            }
          }

        if (rawJson.isNotEmpty()) {
          val draft =
            parseScoreboardDraft(
              jsonString = rawJson,
              champions = champions,
              items = items,
              fallbackChampion = fallbackChampion,
              fallbackLane = fallbackLane,
              usedModelLabel = spec.displayName,
              language = language,
            )
          val report =
            draft.myChampion?.let { champ ->
              accuracyEngine.evaluate(
                myChampion = champ,
                lane = draft.detectedLane,
                enemies = draft.enemyChampions,
                boughtItems = draft.boughtItems,
                kills = draft.kills,
                deaths = draft.deaths,
                assists = draft.assists,
                goldEarned = draft.goldEarned,
                durationMinutes = draft.durationMinutes,
                allItems = items,
                allRunes = runes,
                allSpells = spells,
                explicitMatchupRules = matchupRules,
              )
            }
          val enrichedDraft =
            if (draft.lessons.isEmpty() && report != null) {
              draft.copy(
                lessons = buildOfflineThreeLessons(draft, report, language),
                spokenBriefing = buildOfflineThreeLessons(draft, report, language).joinToString(" "),
              )
            } else {
              draft
            }
          return PostGameScanOutcome.Success(PostGameEvaluationResult(enrichedDraft, report))
        }
      } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
          lastError = e
          if (isManualLock) break
        } else {
          throw e
        }
      } catch (e: Exception) {
        Log.w(TAG, "Post-game scan failed on ${spec.displayName}: ${e.message}", e)
        lastError = e
        if (isManualLock) break
      }
    }

    val msg =
      lastError?.let { GeminiCoachService.buildDetailedErrorMessage(it, language) }
        ?: if (language == AppLanguage.TH) {
          "ไม่สามารถอ่านผลสกอร์บอร์ดได้ ลองเลือกภาพที่เห็นไอเทมและ KDA ชัดเจน"
        } else {
          "Could not extract scoreboard details. Try a clearer post-game screenshot."
        }
    return PostGameScanOutcome.Failure(msg)
  }

  /** Re-evaluates build accuracy & generates 3 personalized lessons after manual edits or entry. */
  fun evaluateManualOrEdited(
    draft: PostGameScanDraft,
    items: List<ItemData>,
    runes: List<RuneData>,
    spells: List<SpellData>,
    matchupRules: List<MatchupRuleData>,
    language: AppLanguage,
    regenerateLessonsFromEngine: Boolean = false,
  ): PostGameEvaluationResult {
    val report =
      draft.myChampion?.let { champ ->
        accuracyEngine.evaluate(
          myChampion = champ,
          lane = draft.detectedLane,
          enemies = draft.enemyChampions,
          boughtItems = draft.boughtItems,
          kills = draft.kills,
          deaths = draft.deaths,
          assists = draft.assists,
          goldEarned = draft.goldEarned,
          durationMinutes = draft.durationMinutes,
          allItems = items,
          allRunes = runes,
          allSpells = spells,
          explicitMatchupRules = matchupRules,
        )
      }
    val finalLessons =
      if (regenerateLessonsFromEngine || draft.lessons.isEmpty()) {
        buildOfflineThreeLessons(draft, report, language)
      } else {
        draft.lessons.take(3)
      }
    val spoken =
      if (regenerateLessonsFromEngine || draft.spokenBriefing.isBlank()) {
        finalLessons.joinToString(". ")
      } else {
        draft.spokenBriefing
      }
    return PostGameEvaluationResult(
      draft = draft.copy(lessons = finalLessons, spokenBriefing = spoken),
      accuracyReport = report,
    )
  }

  @Suppress("UNCHECKED_CAST")
  internal fun parseScoreboardDraft(
    jsonString: String,
    champions: List<ChampionData>,
    items: List<ItemData>,
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    usedModelLabel: String,
    language: AppLanguage,
  ): PostGameScanDraft {
    val cleaned = ExternalAiProvidersClient.stripCodeFence(jsonString)
    val root = mapAdapter.fromJson(cleaned).orEmpty()

    val isWin = (root["result"] as? String)?.equals("WIN", ignoreCase = true) == true
    val rawMyChamp = (root["myChampionName"] as? String).orEmpty()
    val rawLane = (root["detectedLane"] as? String).orEmpty()
    val enemiesRaw = (root["enemyChampions"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    val itemsRaw = (root["boughtItems"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    val lessonsRaw =
      (root["lessons"] as? List<*>)?.mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotBlank) }.orEmpty()

    val matchRes =
      matcher.resolve(
        RawChampionScanPayload(
          screenType = "IN_GAME",
          myChampionName = rawMyChamp,
          detectedLane = rawLane,
          allies = listOfNotNull(rawMyChamp.takeIf { it.isNotBlank() }),
          enemies = enemiesRaw,
          skippedEmptySlots = 0,
          confidenceNote = (root["confidenceNote"] as? String).orEmpty(),
        ),
        champions,
      )

    val resolvedChamp = matchRes.myChampion ?: fallbackChampion
    val resolvedLane =
      if (rawLane.isBlank() || rawLane.equals("UNKNOWN", ignoreCase = true)) {
        resolvedChamp?.lanes?.firstOrNull() ?: fallbackLane
      } else {
        matchRes.detectedLane
      }
    val resolvedItems = accuracyEngine.resolveItemsByNames(itemsRaw, items)

    val kills = (root["kills"] as? Number)?.toInt()?.coerceIn(0, 60) ?: 0
    val deaths = (root["deaths"] as? Number)?.toInt()?.coerceIn(0, 60) ?: 0
    val assists = (root["assists"] as? Number)?.toInt()?.coerceIn(0, 80) ?: 0
    val gold = (root["goldEarned"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    val duration = (root["durationMinutes"] as? Number)?.toInt()?.coerceIn(5, 60) ?: 18
    val spoken = (root["spokenBriefing"] as? String).orEmpty().trim()
    val note = (root["confidenceNote"] as? String).orEmpty().trim()

    return PostGameScanDraft(
      isWin = isWin,
      myChampion = resolvedChamp,
      detectedLane = resolvedLane,
      kills = kills,
      deaths = deaths,
      assists = assists,
      goldEarned = gold,
      durationMinutes = duration,
      boughtItems = resolvedItems,
      enemyChampions = matchRes.enemies,
      lessons = lessonsRaw.take(3),
      spokenBriefing =
        spoken.ifBlank {
          lessonsRaw.take(3).joinToString(". ").ifBlank {
            if (language == AppLanguage.TH) "สรุปบทเรียนหลังจบเกมพร้อมแล้ว" else "Post-game review is ready."
          }
        },
      usedModelLabel = usedModelLabel,
      confidenceNote = note,
    )
  }

  private fun buildOfflineThreeLessons(
    draft: PostGameScanDraft,
    report: BuildAccuracyReport?,
    language: AppLanguage,
  ): List<String> {
    val champName = draft.myChampion?.displayName(language) ?: "Champion"
    val acc = report?.accuracyPct ?: 0
    val firstFeedback = report?.feedback(language)?.firstOrNull()
    val missedCore = report?.missedCoreItems?.firstOrNull()?.displayName(language)

    return if (language == AppLanguage.TH) {
      val l1 =
        firstFeedback
          ?: if (missedCore != null) {
            "ไอเทม ($acc% ตรงแผน): ควรเสริม $missedCore เพื่อให้สไปก์ไวและแก้ทางทีมศัตรูได้คมขึ้น"
          } else {
            "ไอเทม ($acc% ตรงแผน): การออกของหลักของ $champName สอดคล้องกับรูปเกมแล้ว"
          }
      val l2 =
        if (draft.deaths >= 6) {
          "การยืนตำแหน่ง & การตาย: ตาย ${draft.deaths} ครั้งมากเกินไป ควรเช็คจุดป่าศัตรูก่อนดันเวฟและเก็บรองเท้า Stasis/QSS ให้ทันไฟต์"
        } else {
          "จังหวะเกม & ทรัพยากร: สถิติ ${draft.kills}/${draft.deaths}/${draft.assists} ค่อนข้างนิ่ง เน้นคุมเวฟข้างก่อนเกิดมังกร/บารอน 30 วิ"
        }
      val l3 =
        if (draft.enemyChampions.isNotEmpty()) {
          "รับมือศัตรู: รักษาสกิลหนี/CC ไว้ตอบโต้ ${draft.enemyChampions.first().nameTh} และรอคูลดาวน์หลักก่อนเข้าซ้ำ"
        } else {
          "สรุปไฟต์ทีม: เปิด-ปิดไฟต์ตามจังหวะพาวเวอร์สไปก์ 2 ชิ้นของ $champName ในเลน ${draft.detectedLane.labelTh}"
        }
      listOf(l1, l2, l3)
    } else {
      val l1 =
        firstFeedback
          ?: if (missedCore != null) {
            "Itemization ($acc% match): Prioritize $missedCore earlier to sharpen your counter-spike."
          } else {
            "Itemization ($acc% match): Your core build on $champName aligned well with enemy threats."
          }
      val l2 =
        if (draft.deaths >= 6) {
          "Positioning & Tempo: ${draft.deaths} deaths delayed your item spikes; respect fog-of-war before pushing past river."
        } else {
          "Macro Tempo: Clean ${draft.kills}/${draft.deaths}/${draft.assists} baseline — shove side waves 30s before Dragon/Baron spawns."
        }
      val l3 =
        if (draft.enemyChampions.isNotEmpty()) {
          "Matchup Discipline: Track ${draft.enemyChampions.first().nameEn}'s key cooldown before committing into teamfights."
        } else {
          "Teamfight Focus: Play around your 2-item spike window in ${draft.detectedLane.labelEn}."
        }
      listOf(l1, l2, l3)
    }
  }

  private fun buildVisionCandidates(
    preferredModelId: String,
    firebaseReady: Boolean,
    keyStatus: ExternalApiKeyStatus,
  ): List<AiModelSpec> {
    if (preferredModelId != AiModelCatalog.AUTO_MODEL_ID) {
      val spec = AiModelCatalog.findById(preferredModelId)
      if (spec.supportsVision && spec.id != AiModelCatalog.AUTO_MODEL_ID) {
        if (spec.provider == AiProviderId.FIREBASE_AI && firebaseReady) return listOf(spec)
        if (spec.provider.requiresApiKey && keyStatus.hasKeyFor(spec.provider)) return listOf(spec)
        return emptyList()
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

  private fun decodeScaledBitmapFromUri(uri: Uri): Bitmap? =
    try {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
      val maxDim = max(bounds.outWidth, bounds.outHeight)
      var sample = 1
      while (maxDim / sample > POST_GAME_MAX_IMAGE_DIM_PX * 2) sample *= 2
      val opts = BitmapFactory.Options().apply { inSampleSize = sample }
      context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to decode post-game URI: $uri", e)
      null
    }

  private fun prepareScaledBitmap(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    val resized =
      if (maxSide > POST_GAME_MAX_IMAGE_DIM_PX && maxSide > 0) {
        val ratio = POST_GAME_MAX_IMAGE_DIM_PX.toFloat() / maxSide.toFloat()
        Bitmap.createScaledBitmap(
          source,
          (source.width * ratio).roundToInt().coerceAtLeast(1),
          (source.height * ratio).roundToInt().coerceAtLeast(1),
          true,
        )
      } else {
        source
      }
    val out = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, 86, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: resized
  }

  private fun buildScoreboardPrompt(
    fallbackChampion: ChampionData?,
    fallbackLane: Lane,
    patchVersion: String,
    language: AppLanguage,
  ): String {
    val langRule =
      if (language == AppLanguage.TH) {
        "Write the 3 lessons and spokenBriefing in concise, practical Thai (keep Wild Rift item & champion names in English like Stasis, QSS, Anti-Heal)."
      } else {
        "Write the 3 lessons and spokenBriefing in concise, practical English."
      }
    val hint =
      fallbackChampion?.let {
        "Context hint if highlighted player row is ambiguous: ${it.nameEn} (${fallbackLane.labelEn})."
      } ?: "Fallback lane hint: ${fallbackLane.labelEn}."

    return """
      You are an elite League of Legends: Wild Rift Post-Game Analyst (Patch $patchVersion).
      Inspect this Wild Rift post-game / match summary / scoreboard screenshot and extract:
      1. `result`: "WIN" (Victory) or "LOSS" (Defeat).
      2. `myChampionName`: The player's highlighted champion row in English.
      3. `detectedLane`: BARON, MID, DRAGON, SUPPORT, JUNGLE, or UNKNOWN.
      4. `kills`, `deaths`, `assists`, `goldEarned`, and `durationMinutes`.
      5. `boughtItems`: Exact Wild Rift English item, boots, and active enchant names visible in the player's build slots.
      6. `enemyChampions`: Enemy champion names visible on the scoreboard (up to 5).
      7. `lessons`: Exactly 3 specific, actionable post-game lessons covering (1) Itemization/Counter-buy, (2) Deaths/Gold/Wave tempo, (3) Matchup/Teamfight positioning.
      8. `spokenBriefing`: A natural 2-3 sentence summary suitable for TTS playback.
      $langRule
      $hint
    """.trimIndent()
  }
}
