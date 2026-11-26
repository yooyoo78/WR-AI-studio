package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppDarkThemePreset
import com.example.data.model.AppFontPreset
import com.example.data.model.AppLanguage
import com.example.data.model.IconSizePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
  preferencesDataStore(name = "rift_companion_prefs")

data class UserPreferences(
  val language: AppLanguage = AppLanguage.TH,
  val fontScaleMultiplier: Float = 1.0f,
  val highContrastCards: Boolean = false,
  val activePatchLabel: String = "7.3f",
  val lastImportedPackInfo: String = "",
  val seedRevision: Int = 0,
  val inGameMiniMode: Boolean = false,
  val selectedCoachModelId: String = "auto",
  val selectedLiveModelId: String = "auto",
  val otaRepoSlug: String = "",
  val otaPublicKeyBase64: String = "",
  val autoCheckOtaOnLaunch: Boolean = true,
  val lastOtaCheckMs: Long = 0L,
  val lastKnownOtaTag: String = "",
  val selectedMetaServerId: String = "cn_diamond",
  val lastMetaSyncMs: Long = 0L,
  val lastKnownOfficialPatch: String = "",
  val lastOfficialCheckMs: Long = 0L,
  val darkThemePreset: AppDarkThemePreset = AppDarkThemePreset.HEXTECH_OBSIDIAN,
  val fontPreset: AppFontPreset = AppFontPreset.NOTO_SANS_THAI,
  val iconSizePreset: IconSizePreset = IconSizePreset.COMPACT,
)

class PreferencesRepository(private val context: Context) {
  private object Keys {
    val LANGUAGE = stringPreferencesKey("app_language")
    val FONT_SCALE = floatPreferencesKey("font_scale_multiplier")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_cards")
    val ACTIVE_PATCH = stringPreferencesKey("active_patch_label")
    val LAST_IMPORT = stringPreferencesKey("last_imported_pack_info")
    val SEED_REVISION = intPreferencesKey("seed_revision")
    val IN_GAME_MINI_MODE = booleanPreferencesKey("in_game_mini_mode")
    val SELECTED_COACH_MODEL_ID = stringPreferencesKey("selected_coach_model_id")
    val SELECTED_LIVE_MODEL_ID = stringPreferencesKey("selected_live_model_id")
    val OTA_REPO_SLUG = stringPreferencesKey("ota_repo_slug")
    val OTA_PUBLIC_KEY_B64 = stringPreferencesKey("ota_public_key_b64")
    val AUTO_CHECK_OTA = booleanPreferencesKey("auto_check_ota_on_launch")
    val LAST_OTA_CHECK_MS = longPreferencesKey("last_ota_check_ms")
    val LAST_KNOWN_OTA_TAG = stringPreferencesKey("last_known_ota_tag")
    val SELECTED_META_SERVER = stringPreferencesKey("selected_meta_server_id")
    val LAST_META_SYNC_MS = longPreferencesKey("last_meta_sync_ms")
    val LAST_KNOWN_OFFICIAL_PATCH = stringPreferencesKey("last_known_official_patch")
    val LAST_OFFICIAL_CHECK_MS = longPreferencesKey("last_official_check_ms")
    val DARK_THEME_PRESET = stringPreferencesKey("dark_theme_preset")
    val FONT_PRESET = stringPreferencesKey("font_preset")
    val ICON_SIZE_PRESET = stringPreferencesKey("icon_size_preset")
  }

  val preferencesFlow: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.TH.code
      val language = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.TH
      UserPreferences(
        language = language,
        fontScaleMultiplier = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
        highContrastCards = prefs[Keys.HIGH_CONTRAST] ?: false,
        activePatchLabel = prefs[Keys.ACTIVE_PATCH] ?: "7.3f",
        lastImportedPackInfo = prefs[Keys.LAST_IMPORT] ?: "",
        seedRevision = prefs[Keys.SEED_REVISION] ?: 0,
        inGameMiniMode = prefs[Keys.IN_GAME_MINI_MODE] ?: false,
        selectedCoachModelId = prefs[Keys.SELECTED_COACH_MODEL_ID] ?: "auto",
        selectedLiveModelId = prefs[Keys.SELECTED_LIVE_MODEL_ID] ?: "auto",
        otaRepoSlug = prefs[Keys.OTA_REPO_SLUG] ?: "",
        otaPublicKeyBase64 = prefs[Keys.OTA_PUBLIC_KEY_B64] ?: "",
        autoCheckOtaOnLaunch = prefs[Keys.AUTO_CHECK_OTA] ?: true,
        lastOtaCheckMs = prefs[Keys.LAST_OTA_CHECK_MS] ?: 0L,
        lastKnownOtaTag = prefs[Keys.LAST_KNOWN_OTA_TAG] ?: "",
        selectedMetaServerId = prefs[Keys.SELECTED_META_SERVER] ?: "cn_diamond",
        lastMetaSyncMs = prefs[Keys.LAST_META_SYNC_MS] ?: 0L,
        lastKnownOfficialPatch = prefs[Keys.LAST_KNOWN_OFFICIAL_PATCH] ?: "",
        lastOfficialCheckMs = prefs[Keys.LAST_OFFICIAL_CHECK_MS] ?: 0L,
        darkThemePreset = AppDarkThemePreset.fromId(prefs[Keys.DARK_THEME_PRESET]),
        fontPreset = AppFontPreset.fromId(prefs[Keys.FONT_PRESET]),
        iconSizePreset = IconSizePreset.fromId(prefs[Keys.ICON_SIZE_PRESET]),
      )
    }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setFontScaleMultiplier(multiplier: Float) {
    context.dataStore.edit { it[Keys.FONT_SCALE] = multiplier.coerceIn(0.85f, 1.35f) }
  }

  suspend fun setHighContrastCards(enabled: Boolean) {
    context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
  }

  suspend fun setPatchMetadata(patchVersion: String, importInfo: String) {
    context.dataStore.edit {
      it[Keys.ACTIVE_PATCH] = patchVersion
      it[Keys.LAST_IMPORT] = importInfo
    }
  }

  suspend fun setSeedRevision(revision: Int) {
    context.dataStore.edit { it[Keys.SEED_REVISION] = revision }
  }

  suspend fun setInGameMiniMode(enabled: Boolean) {
    context.dataStore.edit { it[Keys.IN_GAME_MINI_MODE] = enabled }
  }

  suspend fun setSelectedCoachModelId(modelId: String) {
    context.dataStore.edit { it[Keys.SELECTED_COACH_MODEL_ID] = modelId }
  }

  suspend fun setSelectedLiveModelId(modelId: String) {
    context.dataStore.edit { it[Keys.SELECTED_LIVE_MODEL_ID] = modelId }
  }

  suspend fun setOtaConfig(repoSlug: String, publicKeyBase64: String, autoCheckOnLaunch: Boolean) {
    context.dataStore.edit {
      it[Keys.OTA_REPO_SLUG] = repoSlug.trim()
      it[Keys.OTA_PUBLIC_KEY_B64] = publicKeyBase64.trim()
      it[Keys.AUTO_CHECK_OTA] = autoCheckOnLaunch
    }
  }

  suspend fun setLastOtaCheck(timestampMs: Long, latestTag: String) {
    context.dataStore.edit {
      it[Keys.LAST_OTA_CHECK_MS] = timestampMs
      if (latestTag.isNotBlank()) it[Keys.LAST_KNOWN_OTA_TAG] = latestTag
    }
  }

  suspend fun setSelectedMetaServerId(serverId: String) {
    context.dataStore.edit { it[Keys.SELECTED_META_SERVER] = serverId }
  }

  suspend fun setMetaSyncTimestamp(timestampMs: Long) {
    context.dataStore.edit { it[Keys.LAST_META_SYNC_MS] = timestampMs }
  }

  suspend fun setOfficialPatchCheck(officialPatch: String, timestampMs: Long) {
    context.dataStore.edit {
      it[Keys.LAST_KNOWN_OFFICIAL_PATCH] = officialPatch
      it[Keys.LAST_OFFICIAL_CHECK_MS] = timestampMs
    }
  }

  suspend fun setDarkThemePreset(preset: AppDarkThemePreset) {
    context.dataStore.edit { it[Keys.DARK_THEME_PRESET] = preset.id }
  }

  suspend fun setFontPreset(preset: AppFontPreset) {
    context.dataStore.edit { it[Keys.FONT_PRESET] = preset.id }
  }

  suspend fun setIconSizePreset(preset: IconSizePreset) {
    context.dataStore.edit { it[Keys.ICON_SIZE_PRESET] = preset.id }
  }
}
