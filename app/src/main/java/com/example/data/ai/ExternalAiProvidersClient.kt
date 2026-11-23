package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.model.AppLanguage
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ExternalAiHttpException(
  val statusCode: Int,
  val provider: AiProviderId,
  override val message: String,
) : IOException("HTTP $statusCode from ${provider.shortLabel}: $message")

data class ApiKeyTestResult(
  val isValid: Boolean,
  val message: String,
)

class ExternalAiProvidersClient(
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .build()
) {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  fun streamTextCompletion(
    spec: AiModelSpec,
    apiKey: String,
    prompt: String,
  ): Flow<String> =
    flow {
      when (spec.provider) {
        AiProviderId.GROQ, AiProviderId.OPENROUTER -> {
          val endpoint =
            if (spec.provider == AiProviderId.GROQ) {
              "https://api.groq.com/openai/v1/chat/completions"
            } else {
              "https://openrouter.ai/api/v1/chat/completions"
            }
          val payloadJson =
            toJson(
              mapOf(
                "model" to spec.remoteModelId,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "temperature" to 0.6,
                "max_tokens" to 1024,
                "stream" to true,
              )
            )

          val reqBuilder =
            Request.Builder()
              .url(endpoint)
              .addHeader("Authorization", "Bearer ${apiKey.trim()}")
              .addHeader("Content-Type", "application/json")
              .post(payloadJson.toRequestBody(jsonMediaType))

          if (spec.provider == AiProviderId.OPENROUTER) {
            reqBuilder.addHeader("HTTP-Referer", "https://wr-ai.companion.local")
            reqBuilder.addHeader("X-Title", "WR-AI Tactical Coach")
          }

          httpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
              val errBody = response.body?.string().orEmpty()
              throw ExternalAiHttpException(
                statusCode = response.code,
                provider = spec.provider,
                message = extractErrorMessage(errBody, response.code, response.message),
              )
            }
            val body = response.body ?: throw IOException("Empty response body from ${spec.provider.shortLabel}")
            val contentType = response.header("Content-Type").orEmpty()
            val acc = StringBuilder()

            if ("text/event-stream" in contentType.lowercase()) {
              body.charStream().buffered().use { reader ->
                while (currentCoroutineContext().isActive) {
                  val line = reader.readLine() ?: break
                  if (!line.startsWith("data:")) continue
                  val data = line.removePrefix("data:").trim()
                  if (data == "[DONE]") break
                  if (data.isEmpty()) continue
                  val chunkText = extractOpenAiDeltaText(data, spec.provider, response.code)
                  if (chunkText.isNotEmpty()) {
                    acc.append(chunkText)
                    emit(acc.toString())
                  }
                }
              }
            } else {
              val raw = body.string()
              val fullText = extractOpenAiFullText(raw)
              if (fullText.isNotEmpty()) emit(fullText)
            }
          }
        }
        AiProviderId.GOOGLE_AI_STUDIO -> {
          val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${spec.remoteModelId}:streamGenerateContent?alt=sse"
          val payloadJson =
            toJson(
              mapOf(
                "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
                "generationConfig" to mapOf("temperature" to 0.6, "maxOutputTokens" to 1024),
              )
            )

          val request =
            Request.Builder()
              .url(url)
              .addHeader("x-goog-api-key", apiKey.trim())
              .addHeader("Content-Type", "application/json")
              .post(payloadJson.toRequestBody(jsonMediaType))
              .build()

          httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              val errBody = response.body?.string().orEmpty()
              throw ExternalAiHttpException(
                statusCode = response.code,
                provider = spec.provider,
                message = extractErrorMessage(errBody, response.code, response.message),
              )
            }
            val body = response.body ?: throw IOException("Empty response body from AI Studio")
            val acc = StringBuilder()
            body.charStream().buffered().use { reader ->
              while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                val chunkText = extractAiStudioText(data)
                if (chunkText.isNotEmpty()) {
                  acc.append(chunkText)
                  emit(acc.toString())
                }
              }
            }
          }
        }
        AiProviderId.AUTO_POOL, AiProviderId.FIREBASE_AI ->
          throw IllegalArgumentException("External client only handles external providers")
      }
    }
      .flowOn(Dispatchers.IO)

  suspend fun generateVisionJson(
    spec: AiModelSpec,
    apiKey: String,
    bitmap: Bitmap,
    prompt: String,
  ): String =
    withContext(Dispatchers.IO) {
      val jpegB64 = bitmapToJpegBase64(bitmap)
      when (spec.provider) {
        AiProviderId.GOOGLE_AI_STUDIO -> {
          val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${spec.remoteModelId}:generateContent"
          val payloadJson =
            toJson(
              mapOf(
                "contents" to
                  listOf(
                    mapOf(
                      "parts" to
                        listOf(
                          mapOf("inline_data" to mapOf("mime_type" to "image/jpeg", "data" to jpegB64)),
                          mapOf("text" to buildStrictJsonVisionPrompt(prompt)),
                        )
                    )
                  ),
                "generationConfig" to
                  mapOf(
                    "temperature" to 0.25,
                    "maxOutputTokens" to 1024,
                    "responseMimeType" to "application/json",
                  ),
              )
            )
          val request =
            Request.Builder()
              .url(url)
              .addHeader("x-goog-api-key", apiKey.trim())
              .addHeader("Content-Type", "application/json")
              .post(payloadJson.toRequestBody(jsonMediaType))
              .build()
          httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
              throw ExternalAiHttpException(
                statusCode = response.code,
                provider = spec.provider,
                message = extractErrorMessage(raw, response.code, response.message),
              )
            }
            stripCodeFence(extractAiStudioText(raw))
          }
        }
        AiProviderId.GROQ -> {
          val url = "https://api.groq.com/openai/v1/chat/completions"
          val payloadJson =
            toJson(
              mapOf(
                "model" to spec.remoteModelId,
                "messages" to
                  listOf(
                    mapOf(
                      "role" to "user",
                      "content" to
                        listOf(
                          mapOf("type" to "text", "text" to buildStrictJsonVisionPrompt(prompt)),
                          mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$jpegB64"),
                          ),
                        ),
                    )
                  ),
                "temperature" to 0.25,
                "max_tokens" to 1024,
                "response_format" to mapOf("type" to "json_object"),
              )
            )

          val request =
            Request.Builder()
              .url(url)
              .addHeader("Authorization", "Bearer ${apiKey.trim()}")
              .addHeader("Content-Type", "application/json")
              .post(payloadJson.toRequestBody(jsonMediaType))
              .build()
          httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
              throw ExternalAiHttpException(
                statusCode = response.code,
                provider = spec.provider,
                message = extractErrorMessage(raw, response.code, response.message),
              )
            }
            stripCodeFence(extractOpenAiFullText(raw))
          }
        }
        else -> throw UnsupportedOperationException("${spec.displayName} does not support Vision JSON")
      }
    }

  /** Lightweight 0-token key verification call for Groq, OpenRouter, and Google AI Studio. */
  suspend fun testProviderKey(
    provider: AiProviderId,
    rawKey: String,
    language: AppLanguage,
  ): ApiKeyTestResult =
    withContext(Dispatchers.IO) {
      val cleaned = rawKey.trim()
      if (cleaned.isEmpty()) {
        return@withContext ApiKeyTestResult(
          isValid = false,
          message =
            if (language == AppLanguage.TH) "กรุณาวาง API Key ก่อนกดทดสอบ" else "Please enter an API key first.",
        )
      }

      val request =
        when (provider) {
          AiProviderId.GROQ ->
            Request.Builder()
              .url("https://api.groq.com/openai/v1/models")
              .addHeader("Authorization", "Bearer $cleaned")
              .get()
              .build()
          AiProviderId.OPENROUTER ->
            Request.Builder()
              .url("https://openrouter.ai/api/v1/auth/key")
              .addHeader("Authorization", "Bearer $cleaned")
              .get()
              .build()
          AiProviderId.GOOGLE_AI_STUDIO ->
            Request.Builder()
              .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1")
              .addHeader("x-goog-api-key", cleaned)
              .get()
              .build()
          else ->
            return@withContext ApiKeyTestResult(
              isValid = true,
              message = if (language == AppLanguage.TH) "พร้อมใช้งาน" else "Ready",
            )
        }

      try {
        httpClient.newCall(request).execute().use { response ->
          val bodyText = response.body?.string().orEmpty()
          if (response.isSuccessful) {
            ApiKeyTestResult(
              isValid = true,
              message =
                if (language == AppLanguage.TH) {
                  "เชื่อมต่อ ${provider.shortLabel} สำเร็จ (0 โทเคน)"
                } else {
                  "Connected to ${provider.shortLabel} successfully (0 tokens used)."
                },
            )
          } else {
            val detail = extractErrorMessage(bodyText, response.code, response.message)
            ApiKeyTestResult(
              isValid = false,
              message =
                if (response.code == 401 || response.code == 403) {
                  if (language == AppLanguage.TH) {
                    "Key ไม่ถูกต้องหรือถูกปฏิเสธ (HTTP ${response.code}): $detail"
                  } else {
                    "Invalid or unauthorized key (HTTP ${response.code}): $detail"
                  }
                } else {
                  if (language == AppLanguage.TH) {
                    "ทดสอบ ${provider.shortLabel} ไม่ผ่าน (HTTP ${response.code}): $detail"
                  } else {
                    "${provider.shortLabel} check failed (HTTP ${response.code}): $detail"
                  }
                },
            )
          }
        }
      } catch (e: Exception) {
        ApiKeyTestResult(
          isValid = false,
          message =
            if (language == AppLanguage.TH) {
              "เชื่อมต่อ ${provider.shortLabel} ไม่สำเร็จ: ${e.localizedMessage ?: e::class.simpleName}"
            } else {
              "Network error testing ${provider.shortLabel}: ${e.localizedMessage ?: e::class.simpleName}"
            },
        )
      }
    }

  private fun bitmapToJpegBase64(bitmap: Bitmap): String {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
  }

  private fun buildStrictJsonVisionPrompt(basePrompt: String): String =
    """
      $basePrompt

      IMPORTANT OUTPUT FORMAT:
      Return ONLY raw valid JSON (no markdown code fences) with these exact keys:
      {
        "screenPhase": "CHAMP_SELECT" | "LOADING_SCREEN" | "IN_GAME" | "UNKNOWN",
        "myChampionName": "",
        "detectedLane": "BARON" | "MID" | "DRAGON" | "SUPPORT" | "JUNGLE" | "UNKNOWN",
        "allies": ["ChampionName"],
        "enemies": ["ChampionName"],
        "skippedEmptySlots": 0,
        "headline": "",
        "recommendedPicks": [{"championName": "", "reason": ""}],
        "recommendedItems": [""],
        "earlyLanePlan": "",
        "threatAlert": "",
        "spokenBriefing": ""
      }
    """
      .trimIndent()

  internal companion object {
    private val moshi: Moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)

    fun toJson(payload: Map<String, Any?>): String = mapAdapter.toJson(payload)

    @Suppress("UNCHECKED_CAST")
    private fun parseMap(json: String): Map<String, Any?>? =
      try {
        mapAdapter.fromJson(json)
      } catch (_: Exception) {
        null
      }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.obj(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.list(key: String): List<Map<String, Any?>>? =
      (this[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }

    private fun Map<String, Any?>.str(key: String): String = (this[key] as? String).orEmpty()

    fun extractOpenAiDeltaText(
      jsonLine: String,
      provider: AiProviderId,
      httpCode: Int,
    ): String {
      val root = parseMap(jsonLine) ?: return ""
      root.obj("error")?.let { err ->
        val msg = err.str("message").ifBlank { "Provider error" }
        val code = (err["code"] as? Number)?.toInt() ?: httpCode
        throw ExternalAiHttpException(code, provider, msg)
      }
      val choices = root.list("choices") ?: return ""
      val first = choices.firstOrNull() ?: return ""
      val delta = first.obj("delta")
      val deltaContent = delta?.str("content").orEmpty()
      if (deltaContent.isNotEmpty()) return deltaContent
      val msg = first.obj("message")
      return msg?.str("content").orEmpty()
    }

    fun extractOpenAiFullText(jsonString: String): String {
      val root = parseMap(jsonString) ?: return ""
      val first = root.list("choices")?.firstOrNull() ?: return ""
      return first.obj("message")?.str("content").orEmpty()
    }

    fun extractAiStudioText(jsonString: String): String {
      val root = parseMap(jsonString) ?: return ""
      val firstCandidate = root.list("candidates")?.firstOrNull() ?: return ""
      val parts = firstCandidate.obj("content")?.list("parts") ?: return ""
      return buildString {
        parts.forEach { part ->
          val text = part.str("text")
          if (text.isNotEmpty()) append(text)
        }
      }
    }

    fun extractErrorMessage(rawBody: String, code: Int, httpMessage: String): String {
      val trimmed = rawBody.trim()
      if (trimmed.startsWith("{")) {
        parseMap(trimmed)?.let { obj ->
          val errMsg = obj.obj("error")?.str("message").orEmpty()
          if (errMsg.isNotBlank()) return errMsg
          val msg = obj.str("message")
          if (msg.isNotBlank()) return msg
        }
      }
      return trimmed.take(180).ifBlank { httpMessage.ifBlank { "HTTP $code" } }
    }

    fun stripCodeFence(raw: String): String {
      val trimmed = raw.trim()
      if (!trimmed.startsWith("```")) return trimmed
      return trimmed
        .removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    }
  }
}
