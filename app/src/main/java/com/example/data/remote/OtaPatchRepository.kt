package com.example.data.remote

import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.LenientPatchChangeTypeAdapter
import com.example.data.model.PatchPackPayload
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
import com.example.data.repository.PatchDeltaMerger
import com.example.data.validation.PatchDataValidator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val OTA_MANIFEST_FILE_DEFAULT = "patch-manifest.json"
private const val OTA_LAUNCH_THROTTLE_MS = 6L * 60L * 60L * 1000L

class OtaPatchRepository(
  private val gameDataRepository: GameDataRepository,
  private val preferencesRepository: PreferencesRepository,
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(25, TimeUnit.SECONDS)
      .build(),
  private val validator: PatchDataValidator = PatchDataValidator(),
) {
  private val moshi: Moshi =
    Moshi.Builder()
      .add(LenientPatchChangeTypeAdapter())
      .addLast(KotlinJsonAdapterFactory())
      .build()
  private val releaseAdapter = moshi.adapter(GitHubReleaseDto::class.java)
  private val manifestAdapter = moshi.adapter(OtaReleaseManifest::class.java)
  private val payloadAdapter = moshi.adapter(PatchPackPayload::class.java)

  fun isValidRepoSlug(slug: String): Boolean =
    Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(slug.trim())

  suspend fun checkForUpdatesOnLaunchIfDue(language: AppLanguage): OtaCheckStatus? {
    val prefs = preferencesRepository.preferencesFlow.first()
    if (!prefs.autoCheckOtaOnLaunch || !isValidRepoSlug(prefs.otaRepoSlug)) return null
    val now = System.currentTimeMillis()
    if ((now - prefs.lastOtaCheckMs) < OTA_LAUNCH_THROTTLE_MS) return null
    val res = checkAndVerifyLatestRelease(language = language)
    return if (res is OtaCheckStatus.Error) null else res
  }

  suspend fun checkAndVerifyLatestRelease(
    repoSlugOverride: String? = null,
    publicKeyOverride: String? = null,
    language: AppLanguage = AppLanguage.TH,
  ): OtaCheckStatus =
    withContext(Dispatchers.IO) {
      val prefs = preferencesRepository.preferencesFlow.first()
      val slug = (repoSlugOverride ?: prefs.otaRepoSlug).trim()
      val pubKey = (publicKeyOverride ?: prefs.otaPublicKeyBase64).trim()

      if (!isValidRepoSlug(slug)) {
        return@withContext OtaCheckStatus.Error(
          if (language == AppLanguage.TH) {
            "กรุณาระบุ GitHub Repo รูปแบบ owner/repo ก่อนตรวจ OTA"
          } else {
            "Enter a GitHub repository in owner/repo format first."
          }
        )
      }
      if (pubKey.isBlank()) {
        return@withContext OtaCheckStatus.Error(
          if (language == AppLanguage.TH) {
            "กรุณาระบุ ECDSA Public Key (Base64 / PEM) เพื่อตรวจลายเซ็นดิจิทัลก่อนอัปเดต"
          } else {
            "Configure an ECDSA Public Key (Base64 / PEM) to verify OTA signatures."
          }
        )
      }

      try {
        val releaseUrl = "https://api.github.com/repos/$slug/releases/latest"
        val releaseJson = fetchString(releaseUrl, acceptJson = true)
        val release =
          releaseAdapter.fromJson(releaseJson)
            ?: return@withContext OtaCheckStatus.Error("Empty GitHub release metadata.")

        preferencesRepository.setLastOtaCheck(System.currentTimeMillis(), release.tagName)

        val manifestAsset =
          release.assets.firstOrNull {
            it.name.equals(OTA_MANIFEST_FILE_DEFAULT, ignoreCase = true) ||
              it.name.endsWith("manifest.json", ignoreCase = true)
          }
            ?: return@withContext OtaCheckStatus.Error(
              if (language == AppLanguage.TH) {
                "ไม่พบไฟล์ $OTA_MANIFEST_FILE_DEFAULT ใน Release ${release.tagName}"
              } else {
                "Missing $OTA_MANIFEST_FILE_DEFAULT asset in release ${release.tagName}."
              }
            )

        val manifestJson = fetchString(manifestAsset.browserDownloadUrl, acceptJson = false)
        val manifest =
          manifestAdapter.fromJson(manifestJson)
            ?: return@withContext OtaCheckStatus.Error("Invalid OTA manifest JSON.")

        val currentPatch = prefs.activePatchLabel
        if (manifest.patchVersion.equals(currentPatch, ignoreCase = true)) {
          return@withContext OtaCheckStatus.UpToDate(currentPatch, release.tagName)
        }

        val packAsset =
          release.assets.firstOrNull { it.name.equals(manifest.packFileName, ignoreCase = true) }
            ?: return@withContext OtaCheckStatus.Error(
              "Missing pack asset '${manifest.packFileName}' in release ${release.tagName}."
            )

        val packBytes = fetchBytes(packAsset.browserDownloadUrl)
        val sigCheck =
          EcdsaSignatureVerifier.verifyPack(
            packBytes = packBytes,
            expectedSha256Hex = manifest.packSha256,
            signatureBase64 = manifest.packSignatureBase64,
            publicKeyPemOrBase64 = pubKey,
          )
        if (sigCheck.isFailure) {
          val reason = sigCheck.exceptionOrNull()?.message ?: "Invalid signature"
          return@withContext OtaCheckStatus.Error(
            if (language == AppLanguage.TH) {
              "ปฏิเสธแพ็ก OTA: การตรวจลายเซ็นดิจิทัลไม่ผ่าน ($reason)"
            } else {
              "Rejected OTA pack: digital signature verification failed ($reason)."
            }
          )
        }

        val rawPackJson = String(packBytes, StandardCharsets.UTF_8)
        val rawPayload =
          payloadAdapter.fromJson(rawPackJson)
            ?: return@withContext OtaCheckStatus.Error("Failed to parse OTA PatchPack JSON.")

        val currentChamps = gameDataRepository.champions.first()
        val currentItems = gameDataRepository.items.first()
        val currentRunes = gameDataRepository.runes.first()
        val currentSpells = gameDataRepository.spells.first()
        val currentMatchups = gameDataRepository.matchupRules.first()

        val merged =
          PatchDeltaMerger.merge(
            payload = rawPayload,
            existingChampions = currentChamps,
            existingItems = currentItems,
            existingRunes = currentRunes,
            existingSpells = currentSpells,
            existingMatchups = currentMatchups,
          )
        if (merged.unknownDeltaErrors.isNotEmpty()) {
          return@withContext OtaCheckStatus.Error(
            if (language == AppLanguage.TH) {
              "ข้อมูล OTA ระบุ ID ไม่ถูกต้อง:\n${merged.unknownDeltaErrors.joinToString("\n")}"
            } else {
              "OTA pack references unknown IDs:\n${merged.unknownDeltaErrors.joinToString("\n")}"
            }
          )
        }
        val payload = merged.payload

        val validation =
          validator.validate(
            payload = payload,
            existingChampions = currentChamps,
            existingItems = currentItems,
            existingRunes = currentRunes,
            existingSpells = currentSpells,
            existingMatchups = currentMatchups,
          )
        if (!validation.isValid) {
          return@withContext OtaCheckStatus.Error(
            if (language == AppLanguage.TH) {
              "ข้อมูล OTA ไม่ผ่าน PatchDataValidator:\n${validation.errors.joinToString("\n")}"
            } else {
              "OTA pack failed PatchDataValidator:\n${validation.errors.joinToString("\n")}"
            }
          )
        }

        val diff =
          PatchDiffBuilder.compute(
            currentPatchVersion = currentPatch,
            existingChampionIds = currentChamps.map { it.id }.toSet(),
            existingItemIds = currentItems.map { it.id }.toSet(),
            existingRuneIds = currentRunes.map { it.id }.toSet(),
            existingMatchupIds = currentMatchups.map { it.id }.toSet(),
            incoming = payload,
          )

        OtaCheckStatus.UpdateReady(
          VerifiedOtaDownload(
            releaseTag = release.tagName,
            releaseUrl = release.htmlUrl,
            manifest = manifest,
            rawPackJson = rawPackJson,
            payload = payload,
            validation = validation,
            diff = diff,
            signatureVerified = true,
          )
        )
      } catch (e: Exception) {
        OtaCheckStatus.Error(
          if (language == AppLanguage.TH) {
            "เช็ค OTA ไม่สำเร็จ: ${e.localizedMessage ?: e::class.simpleName}"
          } else {
            "OTA check failed: ${e.localizedMessage ?: e::class.simpleName}"
          }
        )
      }
    }

  suspend fun applyVerifiedOtaPack(
    download: VerifiedOtaDownload,
    language: AppLanguage,
  ): OtaCheckStatus =
    withContext(Dispatchers.IO) {
      gameDataRepository.capturePatchRollbackSnapshot()
      when (val res = gameDataRepository.importPatchJson(download.rawPackJson, replaceExisting = false)) {
        is ImportResult.Success ->
          OtaCheckStatus.Applied(patchVersion = res.patchVersion, snapshotCaptured = true)
        is ImportResult.Invalid ->
          OtaCheckStatus.Error(
            if (language == AppLanguage.TH) "นำเข้า OTA ไม่สำเร็จ: ${res.message}"
            else "OTA apply failed: ${res.message}"
          )
      }
    }

  private fun fetchString(url: String, acceptJson: Boolean): String {
    val builder = Request.Builder().url(url).get()
    if (acceptJson) {
      builder.addHeader("Accept", "application/vnd.github+json")
      builder.addHeader("User-Agent", "WR-AI-Companion/1.1")
    }
    httpClient.newCall(builder.build()).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IOException("HTTP ${resp.code}: ${resp.message}")
      }
      return resp.body?.string().orEmpty()
    }
  }

  private fun fetchBytes(url: String): ByteArray {
    val req = Request.Builder().url(url).addHeader("User-Agent", "WR-AI-Companion/1.1").get().build()
    httpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IOException("HTTP ${resp.code}: ${resp.message}")
      }
      return resp.body?.bytes() ?: ByteArray(0)
    }
  }
}
