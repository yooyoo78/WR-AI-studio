package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.ai.AiProviderId
import com.example.data.ai.ExternalApiKeyStatus
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SecureApiKeyStore"
private const val PREFS_NAME = "wr_ai_secure_keys_prefs"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEYSTORE_ALIAS = "wr_ai_coach_ext_key_aes_v1"
private const val GCM_TAG_BITS = 128

class SecureApiKeyStore(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _keyStatusFlow = MutableStateFlow(readStatus())
  val keyStatusFlow: StateFlow<ExternalApiKeyStatus> = _keyStatusFlow.asStateFlow()

  @Synchronized
  fun saveKey(provider: AiProviderId, rawKey: String) {
    if (!provider.requiresApiKey) return
    val cleaned = rawKey.trim()
    val prefKey = prefKeyFor(provider)
    if (cleaned.isEmpty()) {
      prefs.edit().remove(prefKey).apply()
    } else {
      val encrypted = encryptValue(cleaned)
      prefs.edit().putString(prefKey, encrypted).apply()
    }
    _keyStatusFlow.value = readStatus()
  }

  @Synchronized
  fun clearKey(provider: AiProviderId) {
    if (!provider.requiresApiKey) return
    prefs.edit().remove(prefKeyFor(provider)).apply()
    _keyStatusFlow.value = readStatus()
  }

  @Synchronized
  fun getKey(provider: AiProviderId): String? {
    if (!provider.requiresApiKey) return null
    val stored = prefs.getString(prefKeyFor(provider), null)?.takeIf { it.isNotBlank() } ?: return null
    return decryptValue(stored)?.takeIf { it.isNotBlank() }
  }

  fun hasKey(provider: AiProviderId): Boolean = getKey(provider) != null

  private fun readStatus(): ExternalApiKeyStatus {
    val groq = getKey(AiProviderId.GROQ)
    val openRouter = getKey(AiProviderId.OPENROUTER)
    val aiStudio = getKey(AiProviderId.GOOGLE_AI_STUDIO)
    return ExternalApiKeyStatus(
      hasGroqKey = !groq.isNullOrBlank(),
      hasOpenRouterKey = !openRouter.isNullOrBlank(),
      hasAiStudioKey = !aiStudio.isNullOrBlank(),
      groqPreview = maskPreview(groq),
      openRouterPreview = maskPreview(openRouter),
      aiStudioPreview = maskPreview(aiStudio),
    )
  }

  private fun encryptValue(plainText: String): String {
    val secretKey = getOrCreateAesKey()
    if (secretKey != null) {
      try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "ks1:$ivB64:$dataB64"
      } catch (e: Exception) {
        Log.w(TAG, "AndroidKeyStore AES encrypt fallback triggered", e)
      }
    }
    // Fallback only if AndroidKeyStore is unavailable (e.g., Robolectric JVM test harness)
    val rawB64 = Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    return "b64:$rawB64"
  }

  private fun decryptValue(encoded: String): String? {
    return try {
      when {
        encoded.startsWith("ks1:") -> {
          val parts = encoded.split(":", limit = 3)
          if (parts.size != 3) return null
          val iv = Base64.decode(parts[1], Base64.NO_WRAP)
          val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
          val secretKey = getOrCreateAesKey() ?: return null
          val cipher = Cipher.getInstance("AES/GCM/NoPadding")
          cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
          String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        }
        encoded.startsWith("b64:") -> {
          val bytes = Base64.decode(encoded.removePrefix("b64:"), Base64.NO_WRAP)
          String(bytes, StandardCharsets.UTF_8)
        }
        else -> null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to decrypt stored external API key", e)
      null
    }
  }

  private fun getOrCreateAesKey(): SecretKey? {
    return try {
      val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
      val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
      if (existing != null) return existing

      val keyGenerator =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
      val spec =
        KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .build()
      keyGenerator.init(spec)
      keyGenerator.generateKey()
    } catch (e: Exception) {
      Log.w(TAG, "AndroidKeyStore unavailable in current environment", e)
      null
    }
  }

  private fun prefKeyFor(provider: AiProviderId): String = "ext_key_${provider.name}"

  companion object {
    internal fun maskPreview(raw: String?): String {
      val trimmed = raw?.trim().orEmpty()
      if (trimmed.isEmpty()) return ""
      if (trimmed.length <= 8) return "••••${trimmed.takeLast(2)}"
      return "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
    }
  }
}
