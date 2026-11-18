package com.example.data.ai

import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiCoachService"

sealed interface CoachStreamEvent {
  data class Chunk(val accumulatedText: String) : CoachStreamEvent

  data class Completed(val finalText: String) : CoachStreamEvent

  data class Error(val message: String) : CoachStreamEvent
}

class GeminiCoachService {
  // Lazily initialized per firebase-ai-logic-android guidelines
  private val model by lazy { Firebase.ai.generativeModel("gemini-3.8-flash") }

  fun streamCoachingAdvice(
    question: String,
    champion: ChampionData?,
    lane: Lane,
    enemies: List<ChampionData>,
    patchVersion: String,
    language: AppLanguage,
  ): Flow<CoachStreamEvent> = flow {
    val prompt = buildTacticalPrompt(question, champion, lane, enemies, patchVersion, language)
    val accumulated = StringBuilder()
    try {
      withTimeout(60_000L) {
        model.generateContentStream(prompt).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          if (chunkText.isNotEmpty()) {
            accumulated.append(chunkText)
            emit(CoachStreamEvent.Chunk(accumulated.toString()))
          }
        }
      }
      if (accumulated.isNotBlank()) {
        emit(CoachStreamEvent.Completed(accumulated.toString()))
      } else {
        emit(
          CoachStreamEvent.Error(
            if (language == AppLanguage.TH) {
              "โมเดลไม่ส่งคำตอบกลับมา กรุณาลองใหม่อีกครั้ง"
            } else {
              "Gemini returned an empty response. Please try again."
            }
          )
        )
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini AI Coach request timed out", e)
      emit(
        CoachStreamEvent.Error(
          if (language == AppLanguage.TH) {
            "หมดเวลาเชื่อมต่อ AI Coach (60 วินาที) กรุณาลองใหม่ หรือดูแผนออฟไลน์ด้านล่าง"
          } else {
            "AI Coach timed out after 60 seconds. Check connection or use the offline playbook."
          }
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Gemini AI Coach failed", e)
      emit(
        CoachStreamEvent.Error(
          e.localizedMessage
            ?: if (language == AppLanguage.TH) {
              "ไม่สามารถเชื่อมต่อ Gemini AI Coach ได้ในขณะนี้"
            } else {
              "Unable to reach Gemini AI Coach right now."
            }
        )
      )
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
        "Player Champion: ${it.nameEn} (${it.roles.joinToString("/")}, Damage: ${it.damageType.name}, Lane: ${lane.labelEn})."
      } ?: "No specific champion locked yet (Lane: ${lane.labelEn})."
    val enemyContext =
      if (enemies.isEmpty()) {
        "Enemy Team: Not specified."
      } else {
        "Enemy Team (up to 5): ${enemies.joinToString(", ") { "${it.nameEn} (${it.damageType.name})" }}."
      }

    return """
      You are an elite League of Legends: Wild Rift tactical coach (Patch $patchVersion).
      $langInstruction
      $champContext
      $enemyContext
      
      Give actionable, match-winning advice covering laning, itemization/enchant timing, wave control, objective setup, and teamfight positioning as relevant to the player's question. Keep formatting crisp with short bullet points so it is easy to read before or during champion select.
      
      Player Question: $question
    """
      .trimIndent()
  }
}
