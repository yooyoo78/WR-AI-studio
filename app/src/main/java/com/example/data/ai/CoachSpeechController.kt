package com.example.data.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AppLanguage
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "CoachSpeechController"

data class CoachSpeechState(
  val activeMessageId: String? = null,
  val isSpeaking: Boolean = false,
  val isEngineReady: Boolean = false,
  val errorMessage: String? = null,
)

class CoachSpeechController(private val context: Context) {
  private val appContext = context.applicationContext
  private var tts: TextToSpeech? = null
  private var isInitializing = false
  private var pendingSpeak: Triple<String, String, AppLanguage>? = null

  private val _state = MutableStateFlow(CoachSpeechState())
  val state: StateFlow<CoachSpeechState> = _state.asStateFlow()

  fun toggleSpeak(messageId: String, rawText: String, language: AppLanguage) {
    val current = _state.value
    if (current.isSpeaking && current.activeMessageId == messageId) {
      stop()
      return
    }
    speak(messageId, rawText, language)
  }

  fun speak(messageId: String, rawText: String, language: AppLanguage) {
    val cleaned = sanitizeForSpeech(rawText)
    if (cleaned.isBlank()) return

    val engine = tts
    if (engine == null) {
      pendingSpeak = Triple(messageId, cleaned, language)
      ensureInitialized(language)
      return
    }

    if (!_state.value.isEngineReady) {
      pendingSpeak = Triple(messageId, cleaned, language)
      return
    }

    speakNow(engine, messageId, cleaned, language)
  }

  fun stop() {
    pendingSpeak = null
    try {
      tts?.stop()
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping TTS", e)
    }
    _state.update { it.copy(activeMessageId = null, isSpeaking = false) }
  }

  fun clearError() {
    _state.update { it.copy(errorMessage = null) }
  }

  fun shutdown() {
    pendingSpeak = null
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (e: Exception) {
      Log.w(TAG, "Error shutting down TTS", e)
    }
    tts = null
    isInitializing = false
    _state.value = CoachSpeechState()
  }

  private fun ensureInitialized(initialLanguage: AppLanguage) {
    if (tts != null || isInitializing) return
    isInitializing = true
    try {
      tts =
        TextToSpeech(appContext) { status ->
          isInitializing = false
          if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return@TextToSpeech
            engine.setOnUtteranceProgressListener(
              object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                  _state.update {
                    it.copy(
                      activeMessageId = utteranceId,
                      isSpeaking = true,
                      errorMessage = null,
                    )
                  }
                }

                override fun onDone(utteranceId: String?) {
                  _state.update { curr ->
                    if (curr.activeMessageId == utteranceId) {
                      curr.copy(activeMessageId = null, isSpeaking = false)
                    } else curr
                  }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                  _state.update { curr ->
                    if (curr.activeMessageId == utteranceId) {
                      curr.copy(activeMessageId = null, isSpeaking = false)
                    } else curr
                  }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                  Log.w(TAG, "TTS utterance error code=$errorCode for $utteranceId")
                  _state.update { curr ->
                    if (curr.activeMessageId == utteranceId) {
                      curr.copy(
                        activeMessageId = null,
                        isSpeaking = false,
                        errorMessage =
                          if (initialLanguage == AppLanguage.TH) {
                            "ไม่สามารถเล่นเสียงอ่านบนอุปกรณ์ได้ (รหัส $errorCode)"
                          } else {
                            "Device speech playback error ($errorCode)"
                          },
                      )
                    } else curr
                  }
                }
              }
            )
            _state.update { it.copy(isEngineReady = true, errorMessage = null) }
            val pending = pendingSpeak
            pendingSpeak = null
            if (pending != null) {
              speakNow(engine, pending.first, pending.second, pending.third)
            }
          } else {
            pendingSpeak = null
            _state.update {
              it.copy(
                isEngineReady = false,
                isSpeaking = false,
                activeMessageId = null,
                errorMessage =
                  if (initialLanguage == AppLanguage.TH) {
                    "อุปกรณ์นี้ยังไม่พร้อมใช้งานเสียงอ่าน (Text-to-Speech)"
                  } else {
                    "Text-to-Speech engine is unavailable on this device."
                  },
              )
            }
          }
        }
    } catch (e: Exception) {
      isInitializing = false
      pendingSpeak = null
      Log.e(TAG, "Failed to create TextToSpeech", e)
      _state.update {
        it.copy(
          isEngineReady = false,
          isSpeaking = false,
          activeMessageId = null,
          errorMessage = e.localizedMessage ?: "TTS unavailable",
        )
      }
    }
  }

  private fun speakNow(
    engine: TextToSpeech,
    messageId: String,
    cleanedText: String,
    language: AppLanguage,
  ) {
    val targetLocale =
      if (language == AppLanguage.TH) Locale.Builder().setLanguage("th").setRegion("TH").build()
      else Locale.US
    val langResult = engine.setLanguage(targetLocale)
    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
      // Fall back to default locale or US English if Thai voice pack is absent on emulator
      engine.setLanguage(Locale.US)
    }
    engine.setSpeechRate(if (language == AppLanguage.TH) 1.05f else 1.0f)
    _state.update {
      it.copy(activeMessageId = messageId, isSpeaking = true, errorMessage = null)
    }
    engine.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, messageId)
  }

  companion object {
    internal fun sanitizeForSpeech(raw: String): String {
      return raw
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("[*_`#>]"), "")
        .replace(Regex("(?m)^\\s*[-•+]\\s*"), "")
        .replace("•", ", ")
        .replace(Regex("\\s*\\n+\\s*"), ". ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
    }
  }
}
