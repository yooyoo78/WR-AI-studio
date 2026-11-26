package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.local.AiCoachCacheEntity
import com.example.data.local.GameDao
import com.example.data.local.SecureApiKeyStore
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiCoachService"
private const val MAX_MEMORY_CACHE_ENTRIES = 25
private const val MAX_DB_CACHE_ENTRIES = 250
private const val CACHE_TTL_MS = 14L * 24L * 60L * 60L * 1000L
private const val MODEL_COOLDOWN_MS = 60_000L

sealed interface CoachStreamEvent {
  data class Chunk(
    val accumulatedText: String,
    val modelId: String = "",
    val modelLabel: String = "",
  ) : CoachStreamEvent

  data class Completed(
    val finalText: String,
    val modelId: String = "",
    val modelLabel: String = "WR-AI COACH",
    val fromCache: Boolean = false,
    val isFallbackUsed: Boolean = false,
  ) : CoachStreamEvent

  data class Error(
    val message: String,
    val kind: CoachErrorKind = CoachErrorKind.OTHER,
    val failedModelId: String = "",
    val suggestedFallbackModelId: String? = null,
  ) : CoachStreamEvent
}

enum class CoachErrorKind {
  APP_CHECK,
  INVALID_KEY,
  QUOTA_LIMIT,
  NETWORK,
  OTHER,
}

class GeminiCoachService(
  private val context: Context? = null,
  private val gameDao: GameDao? = null,
  private val secureKeyStore: SecureApiKeyStore? = null,
  private val externalClient: ExternalAiProvidersClient = ExternalAiProvidersClient(),
) {
  private val coachConfig by lazy {
    generationConfig {
      temperature = 0.6f
      maxOutputTokens = 1024
    }
  }

  // Lazily initialized per firebase-ai-logic-android guidelines
  private val firebaseFlashModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH.remoteModelId,
      generationConfig = coachConfig,
    )
  }
  private val firebaseFlashLiteModel by lazy {
    Firebase.ai.generativeModel(
      modelName = AiModelCatalog.FIREBASE_FLASH_LITE.remoteModelId,
      generationConfig = coachConfig,
    )
  }

  private val cooldownUntilByModel = ConcurrentHashMap<String, Long>()
  private val _cooldownModelIds = MutableStateFlow<Set<String>>(emptySet())
  val cooldownModelIds: StateFlow<Set<String>> = _cooldownModelIds.asStateFlow()

  private val memoryCache =
    object : LinkedHashMap<String, AiCoachCacheEntity>(MAX_MEMORY_CACHE_ENTRIES + 1, 0.75f, true) {
      override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<String, AiCoachCacheEntity>?
      ): Boolean = size > MAX_MEMORY_CACHE_ENTRIES
    }

  suspend fun clearPersistentCache() {
    synchronized(memoryCache) { memoryCache.clear() }
    gameDao?.clearAllCoachCache()
  }

  fun clearCache() {
    synchronized(memoryCache) { memoryCache.clear() }
  }

  fun activeCooldownModelIds(nowMs: Long = System.currentTimeMillis()): Set<String> {
    pruneExpiredCooldowns(nowMs)
    return _cooldownModelIds.value
  }

  fun streamCoachingAdvice(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
    preferredModelId: String = AiModelCatalog.AUTO_MODEL_ID,
    bypassCache: Boolean = false,
    playerMemorySummary: String = "",
  ): Flow<CoachStreamEvent> = flow {
    val now = System.currentTimeMillis()
    val cacheKey =
      buildCacheKey(
        question = question,
        champion = champion,
        lane = lane,
        enemies = enemies,
        patchVersion = patchVersion,
        language = language,
        playerMemorySummary = playerMemorySummary,
      )

    if (!bypassCache) {
      val cachedEntry = readCachedEntry(cacheKey, patchVersion, now)
      if (cachedEntry != null && cachedEntry.responseText.isNotBlank()) {
        emit(
          CoachStreamEvent.Completed(
            finalText = cachedEntry.responseText,
            modelId = cachedEntry.modelId,
            modelLabel = cachedEntry.modelLabel.ifBlank { "WR-AI COACH" },
            fromCache = true,
            isFallbackUsed = false,
          )
        )
        return@flow
      }
    }

    val keyStatus = secureKeyStore?.keyStatusFlow?.value ?: ExternalApiKeyStatus()
    val firebaseAvailable = isFirebaseReady()
    val candidates =
      buildCandidateModels(
        preferredModelId = preferredModelId,
        firebaseReady = firebaseAvailable,
        keyStatus = keyStatus,
        nowMs = now,
      )

    if (candidates.isEmpty()) {
      val requestedSpec = AiModelCatalog.findById(preferredModelId)
      val errMsg =
        if (requestedSpec.provider.requiresApiKey && !keyStatus.hasKeyFor(requestedSpec.provider)) {
          if (language == AppLanguage.TH) {
            "ยังไม่ได้ใส่ API Key ฟรีสำหรับ ${requestedSpec.provider.shortLabel} กรุณากดปุ่มกุญแจเพื่อใส่ Key หรือเลือกโมเดลอื่น (เช่น Firebase / Auto)"
          } else {
            "No free API key saved for ${requestedSpec.provider.shortLabel}. Tap the key icon to add one or switch to Firebase/Auto."
          }
        } else {
          if (language == AppLanguage.TH) {
            "ยังไม่ได้ตั้งค่า Firebase ในไฟล์ APK นี้ และยังไม่มี Key ฟรีภายนอก กรุณาเพิ่ม Key (Groq / OpenRouter / AI Studio) หรือใช้ 'คู่มือโค้ชออฟไลน์'"
          } else {
            "Firebase is not configured in this APK and no external free key is saved. Add a free key (Groq/OpenRouter/AI Studio) or use Offline Playbook."
          }
        }
      emit(
        CoachStreamEvent.Error(
          message = errMsg,
          kind = if (requestedSpec.provider.requiresApiKey) CoachErrorKind.INVALID_KEY else CoachErrorKind.APP_CHECK,
          failedModelId = preferredModelId,
          suggestedFallbackModelId = suggestNextModelId(preferredModelId, firebaseAvailable, keyStatus, now),
        )
      )
      return@flow
    }

    val prompt =
      buildTacticalPrompt(
        question = question,
        champion = champion,
        lane = lane,
        enemies = enemies,
        patchVersion = patchVersion,
        language = language,
        playerMemorySummary = playerMemorySummary,
      )
    val isManualLock = preferredModelId != AiModelCatalog.AUTO_MODEL_ID
    var lastError: Throwable? = null
    var lastFailedModelId = candidates.first().id

    for ((index, modelSpec) in candidates.withIndex()) {
      val isFallbackAttempt = index > 0
      val accumulated = StringBuilder()

      try {
        if (modelSpec.provider == AiProviderId.FIREBASE_AI) {
          val fbModel =
            if (modelSpec.id == AiModelCatalog.FIREBASE_FLASH_LITE.id) firebaseFlashLiteModel
            else firebaseFlashModel

          // Use streaming on primary attempt; unary fallback avoids silent stream hang on overloaded nodes
          if (!isFallbackAttempt) {
            withTimeout(35_000L) {
              fbModel.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                  accumulated.append(chunkText)
                  emit(
                    CoachStreamEvent.Chunk(
                      accumulatedText = accumulated.toString(),
                      modelId = modelSpec.id,
                      modelLabel = modelSpec.displayName,
                    )
                  )
                }
              }
            }
          } else {
            val unary = withTimeout(25_000L) { fbModel.generateContent(prompt) }
            val text = unary.text.orEmpty()
            if (text.isNotEmpty()) {
              accumulated.append(text)
            }
          }
        } else {
          val apiKey = secureKeyStore?.getKey(modelSpec.provider).orEmpty()
          if (apiKey.isBlank()) {
            throw ExternalAiHttpException(401, modelSpec.provider, "Missing saved API key")
          }
          withTimeout(40_000L) {
            externalClient.streamTextCompletion(modelSpec, apiKey, prompt).collect { partial ->
              accumulated.clear()
              accumulated.append(partial)
              emit(
                CoachStreamEvent.Chunk(
                  accumulatedText = partial,
                  modelId = modelSpec.id,
                  modelLabel = modelSpec.displayName,
                )
              )
            }
          }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isNotEmpty()) {
          clearCooldown(modelSpec.id)
          saveToCache(
            cacheKey = cacheKey,
            question = question,
            champion = champion,
            lane = lane,
            patchVersion = patchVersion,
            language = language,
            responseText = finalText,
            modelSpec = modelSpec,
          )
          emit(
            CoachStreamEvent.Completed(
              finalText = finalText,
              modelId = modelSpec.id,
              modelLabel = modelSpec.displayName,
              fromCache = false,
              isFallbackUsed = isFallbackAttempt,
            )
          )
          return@flow
        } else {
          lastError = IllegalStateException("Empty response from ${modelSpec.displayName}")
          lastFailedModelId = modelSpec.id
        }
      } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
          Log.w(TAG, "${modelSpec.displayName} timed out (partial=${accumulated.isNotEmpty()})", e)
          val partial = accumulated.toString().trim()
          if (partial.isNotEmpty()) {
            saveToCache(
              cacheKey = cacheKey,
              question = question,
              champion = champion,
              lane = lane,
              patchVersion = patchVersion,
              language = language,
              responseText = partial,
              modelSpec = modelSpec,
            )
            emit(
              CoachStreamEvent.Completed(
                finalText = partial,
                modelId = modelSpec.id,
                modelLabel = modelSpec.displayName,
                fromCache = false,
                isFallbackUsed = isFallbackAttempt,
              )
            )
            return@flow
          }
          markCooldown(modelSpec.id)
          lastError = e
          lastFailedModelId = modelSpec.id
          if (isManualLock) break
        } else {
          throw e
        }
      } catch (e: Exception) {
        Log.w(TAG, "${modelSpec.displayName} failed (${e::class.simpleName}): ${e.message}", e)
        lastError = e
        lastFailedModelId = modelSpec.id
        val kind = classifyError(e)
        if (kind == CoachErrorKind.QUOTA_LIMIT || shouldFallbackToSecondaryModel(e)) {
          markCooldown(modelSpec.id)
        }
        // In manual lock mode, surface immediately without silently switching away from user's explicit pick
        if (isManualLock) break
        // In Auto mode, stop early only if it's a fatal non-recoverable network offline error across all providers
        if (kind == CoachErrorKind.NETWORK && !shouldFallbackToSecondaryModel(e)) {
          // Still try next provider only if it wasn't pure DNS/offline, otherwise break fast
          break
        }
      }
    }

    val finalThrowable = lastError ?: IllegalStateException("No AI models succeeded")
    val kind = classifyError(finalThrowable)
    val suggested =
      suggestNextModelId(
        excludingModelId = lastFailedModelId,
        firebaseReady = firebaseAvailable,
        keyStatus = keyStatus,
        nowMs = System.currentTimeMillis(),
      )
    val msg =
      if (finalThrowable is TimeoutCancellationException) {
        if (language == AppLanguage.TH) {
          "หมดเวลาเชื่อมต่อโมเดล ${AiModelCatalog.findById(lastFailedModelId).displayName} กรุณาลองเปลี่ยนโมเดลหรือลองใหม่อีกครั้ง"
        } else {
          "Timed out waiting for ${AiModelCatalog.findById(lastFailedModelId).displayName}. Try switching models or retry."
        }
      } else {
        buildDetailedErrorMessage(finalThrowable, language)
      }

    emit(
      CoachStreamEvent.Error(
        message = msg,
        kind = kind,
        failedModelId = lastFailedModelId,
        suggestedFallbackModelId = suggested,
      )
    )
  }

  private fun isFirebaseReady(): Boolean {
    val appCtx = context?.applicationContext ?: return true
    if (FirebaseApp.getApps(appCtx).isEmpty()) {
      try {
        FirebaseApp.initializeApp(appCtx)
      } catch (_: Exception) {
        return false
      }
    }
    return FirebaseApp.getApps(appCtx).isNotEmpty()
  }

  private fun markCooldown(modelId: String, durationMs: Long = MODEL_COOLDOWN_MS) {
    val now = System.currentTimeMillis()
    cooldownUntilByModel[modelId] = now + durationMs
    pruneExpiredCooldowns(now)
  }

  private fun clearCooldown(modelId: String) {
    cooldownUntilByModel.remove(modelId)
    pruneExpiredCooldowns(System.currentTimeMillis())
  }

  private fun pruneExpiredCooldowns(nowMs: Long) {
    val it = cooldownUntilByModel.entries.iterator()
    while (it.hasNext()) {
      if (it.next().value <= nowMs) it.remove()
    }
    _cooldownModelIds.value = cooldownUntilByModel.keys.toSet()
  }

  private suspend fun readCachedEntry(
    cacheKey: String,
    patchVersion: String,
    nowMs: Long,
  ): AiCoachCacheEntity? {
    synchronized(memoryCache) {
      val mem = memoryCache[cacheKey]
      if (mem != null && mem.patchVersion == patchVersion && (nowMs - mem.createdAt) <= CACHE_TTL_MS) {
        return mem
      }
    }
    val dao = gameDao ?: return null
    val dbEntry = dao.getCoachCacheEntry(cacheKey) ?: return null
    val expired = dbEntry.patchVersion != patchVersion || (nowMs - dbEntry.createdAt) > CACHE_TTL_MS
    if (expired) return null
    dao.incrementCoachCacheHit(cacheKey, nowMs)
    val updated = dbEntry.copy(hitCount = dbEntry.hitCount + 1, lastAccessedAt = nowMs)
    synchronized(memoryCache) { memoryCache[cacheKey] = updated }
    return updated
  }

  private suspend fun saveToCache(
    cacheKey: String,
    question: String,
    champion: ChampionData?,
    lane: Lane,
    patchVersion: String,
    language: AppLanguage,
    responseText: String,
    modelSpec: AiModelSpec,
  ) {
    val now = System.currentTimeMillis()
    val entry =
      AiCoachCacheEntity(
        cacheKey = cacheKey,
        normalizedQuestion = normalizeQuestion(question),
        championId = champion?.id.orEmpty(),
        lane = lane.name,
        patchVersion = patchVersion,
        languageCode = language.code,
        responseText = responseText,
        modelId = modelSpec.id,
        modelLabel = modelSpec.displayName,
        createdAt = now,
        lastAccessedAt = now,
        hitCount = 0,
      )
    synchronized(memoryCache) { memoryCache[cacheKey] = entry }
    gameDao?.let { dao ->
      dao.upsertCoachCacheEntry(entry)
      dao.pruneExpiredCoachCache(now - CACHE_TTL_MS, patchVersion)
      dao.trimCoachCacheToLimit(MAX_DB_CACHE_ENTRIES)
    }
  }

  internal fun buildCandidateModels(
    preferredModelId: String,
    firebaseReady: Boolean,
    keyStatus: ExternalApiKeyStatus,
    nowMs: Long = System.currentTimeMillis(),
  ): List<AiModelSpec> {
    pruneExpiredCooldowns(nowMs)
    if (preferredModelId != AiModelCatalog.AUTO_MODEL_ID) {
      val spec = AiModelCatalog.findById(preferredModelId)
      if (spec.id == AiModelCatalog.AUTO_MODEL_ID) return emptyList()
      if (spec.provider == AiProviderId.FIREBASE_AI && !firebaseReady) return emptyList()
      if (spec.provider.requiresApiKey && !keyStatus.hasKeyFor(spec.provider)) return emptyList()
      return listOf(spec)
    }

    val pool = mutableListOf<AiModelSpec>()
    if (firebaseReady) {
      pool += AiModelCatalog.FIREBASE_FLASH
      pool += AiModelCatalog.FIREBASE_FLASH_LITE
    }
    if (keyStatus.hasGroqKey) {
      pool += AiModelCatalog.GROQ_LLAMA_70B
      pool += AiModelCatalog.GROQ_LLAMA_8B
    }
    if (keyStatus.hasOpenRouterKey) {
      pool += AiModelCatalog.OPENROUTER_FREE_ROUTER
    }
    if (keyStatus.hasAiStudioKey) {
      pool += AiModelCatalog.AI_STUDIO_FLASH
      pool += AiModelCatalog.AI_STUDIO_FLASH_LITE
    }

    // Prioritize models not currently on 429 cooldown
    val healthy = pool.filter { (cooldownUntilByModel[it.id] ?: 0L) <= nowMs }
    val cooling = pool.filter { (cooldownUntilByModel[it.id] ?: 0L) > nowMs }
    return healthy + cooling
  }

  internal fun suggestNextModelId(
    excludingModelId: String,
    firebaseReady: Boolean,
    keyStatus: ExternalApiKeyStatus,
    nowMs: Long = System.currentTimeMillis(),
  ): String? {
    val autoPool =
      buildCandidateModels(
        preferredModelId = AiModelCatalog.AUTO_MODEL_ID,
        firebaseReady = firebaseReady,
        keyStatus = keyStatus,
        nowMs = nowMs,
      )
    return autoPool
      .firstOrNull { it.id != excludingModelId && (cooldownUntilByModel[it.id] ?: 0L) <= nowMs }
      ?.id ?: autoPool.firstOrNull { it.id != excludingModelId }?.id
  }

  internal companion object {
    fun normalizeQuestion(question: String): String =
      question.trim().lowercase().replace(Regex("\\s+"), " ")

    fun buildCacheKey(
      question: String,
      champion: ChampionData?,
      lane: Lane,
      enemies: List<ChampionData>,
      patchVersion: String,
      language: AppLanguage,
      playerMemorySummary: String = "",
    ): String {
      val enemyKey = enemies.map { it.id }.sorted().joinToString(",")
      val canonical =
        listOf(
            champion?.id.orEmpty(),
            lane.name,
            enemyKey,
            patchVersion.trim(),
            language.code,
            normalizeQuestion(question),
            playerMemorySummary.trim(),
          )
          .joinToString("|")
      val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
      return digest.joinToString("") { "%02x".format(it) }
    }

    fun classifyError(e: Throwable): CoachErrorKind {
      if (e is ExternalAiHttpException) {
        return when (e.statusCode) {
          401, 403 -> CoachErrorKind.INVALID_KEY
          429 -> CoachErrorKind.QUOTA_LIMIT
          else -> CoachErrorKind.OTHER
        }
      }
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
        "401" in combined || "invalid api key" in combined || "incorrect api key" in combined ->
          CoachErrorKind.INVALID_KEY
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
      if (e is ExternalAiHttpException) {
        return e.statusCode == 429 || e.statusCode >= 500 || e.statusCode == 408
      }
      val kind = classifyError(e)
      if (kind == CoachErrorKind.QUOTA_LIMIT) return true
      val combined = "${e::class.simpleName.orEmpty()} ${e.message.orEmpty()}".lowercase()
      return "503" in combined ||
        "500" in combined ||
        "502" in combined ||
        "504" in combined ||
        "unavailable" in combined ||
        "overloaded" in combined
    }

    fun buildDetailedErrorMessage(e: Throwable, language: AppLanguage): String {
      val raw = e.localizedMessage ?: e.message ?: e::class.simpleName.orEmpty()
      return when (classifyError(e)) {
        CoachErrorKind.APP_CHECK ->
          if (language == AppLanguage.TH) {
            "การยืนยันสิทธิ์ Firebase App Check ไม่ผ่าน ($raw) คุณสามารถใส่ Key ฟรี (Groq / OpenRouter / AI Studio) หรือใช้ 'คู่มือโค้ชออฟไลน์' ได้ทันที"
          } else {
            "Firebase App Check attestation failed ($raw). You can add a free external API key (Groq/OpenRouter/AI Studio) or use Offline Playbook."
          }
        CoachErrorKind.INVALID_KEY ->
          if (language == AppLanguage.TH) {
            "API Key ที่บันทึกไว้ไม่ถูกต้องหรือหมดสิทธิ์ใช้งาน ($raw) กรุณาตรวจสอบ Key ในหน้าตั้งค่า Key ฟรี"
          } else {
            "The saved external API key was rejected ($raw). Please verify or replace your key."
          }
        CoachErrorKind.QUOTA_LIMIT ->
          if (language == AppLanguage.TH) {
            "โควตาโมเดลนี้เต็มชั่วคราว (ช่วงพีค / Rate Limit) กดสลับไปโมเดลอื่นด้านล่าง, ใส่ Key ฟรีเพิ่มเติม, หรือใช้ 'คู่มือโค้ชออฟไลน์' ได้ทันที ($raw)"
          } else {
            "This model hit a temporary rate/quota limit. Switch to another free model below, add a free API key, or open the Offline Playbook. ($raw)"
          }
        CoachErrorKind.NETWORK ->
          if (language == AppLanguage.TH) {
            "ไม่สามารถเชื่อมต่อเครือข่ายได้ ($raw) กรุณาตรวจสอบอินเทอร์เน็ต หรือใช้ 'คู่มือโค้ชออฟไลน์'"
          } else {
            "Network connection error ($raw). Check your internet connection or use Offline Playbook."
          }
        CoachErrorKind.OTHER ->
          if (language == AppLanguage.TH) {
            "เชื่อมต่อ AI Coach ไม่สำเร็จ: $raw"
          } else {
            "AI Coach error: $raw"
          }
      }
    }
  }

  internal fun buildTacticalPrompt(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
    playerMemorySummary: String = "",
  ): String {
    val langInstruction =
      if (language == AppLanguage.TH) {
        "Respond strictly in clear, concise, practical Thai gaming language (use English champion/item names like Stasis, QSS, Anti-Heal where Thai players naturally use them)."
      } else {
        "Respond strictly in clear, concise, practical English."
      }
    val champContext =
      champion?.let {
        val loadout = it.resolveForLane(lane)
        "Player Champion: ${it.nameEn} (${it.roles.joinToString("/")}, Damage: ${it.damageType.name}, Lane: ${lane.labelEn} [${if (loadout.isPrimaryOrListedLane) "Listed Role" else "Off-Meta Role"}], Skill Order: ${loadout.skillOrder}, Keystone: ${loadout.keystoneId}, Spells: ${loadout.spellIds.joinToString(" + ")}, Starter: ${loadout.starterItemId}, Core Items: ${loadout.coreItemIds.joinToString(", ")}, Boots/Active: ${loadout.bootsId} + ${loadout.enchantId}, Lane Condition: ${loadout.conditionNoteEn})."
      } ?: "No specific champion locked yet (Lane: ${lane.labelEn})."
    val enemyContext =
      if (enemies.isEmpty()) {
        "Enemy Team: Not specified."
      } else {
        "Enemy Team (up to 5): ${enemies.joinToString(", ") { "${it.nameEn} (${it.damageType.name})" }}."
      }
    val memoryBlock =
      if (playerMemorySummary.isNotBlank()) {
        "\nPlayer History & Habits (tailor advice to fix recurring gaps): $playerMemorySummary"
      } else ""

    return """
      You are an elite League of Legends: Wild Rift tactical coach (Patch $patchVersion).
      IMPORTANT: Use strictly Wild Rift Patch 7.3g mechanics and P/Q/W/E/R ability labels for clarity (and respect Wild Rift cooldowns/map tempo), Wild Rift items (Active boot-slot items like Stasis/Quicksilver/Gargoyle/Glory/Locket/Mikael's/Stridebreaker/Goredrinker/Galeforce, Yun Tal Wildarrows, Hexoptics C44, Kaenic Rookern, Statikk Shiv [hybrid on-hit, NO crit], Amaranth's Twinguard, Heartsteel, Oceanid's Trident, Sundered Sky, Terminus, Echoes of Helia, Yordle Trap, Gluttonous Greaves, Boots of Mana/Dynamism), and Wild Rift runes (no PC LoL Mythics, Doran's items, or retired items like Magnetic Blaster or Crystalline Reflector).
      $langInstruction
      $champContext
      $enemyContext$memoryBlock
      
      Give actionable, match-winning advice covering laning, lane-specific starter/spells (e.g. Smite for Jungle, Support Quest item + Exhaust for Support), itemization/Active timing, wave control, objective setup, and teamfight positioning as relevant to the player's question. Keep formatting crisp with 4-6 short bullet points so it is fast and easy to read before or during champion select.
      
      Player Question: $question
    """
      .trimIndent()
  }
}
