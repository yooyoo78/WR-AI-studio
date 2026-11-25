package com.example.data.remote

import com.example.data.model.PatchPackPayload
import com.example.data.validation.ValidationResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubReleaseAssetDto(
  val name: String,
  @param:Json(name = "browser_download_url") val browserDownloadUrl: String,
  val size: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
  @param:Json(name = "tag_name") val tagName: String,
  val name: String = "",
  @param:Json(name = "html_url") val htmlUrl: String = "",
  val body: String = "",
  val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OtaReleaseManifest(
  val schemaVersion: Int = 1,
  val patchVersion: String,
  val packVersion: Int = 1,
  val releaseDate: String = "",
  val notesEn: String = "",
  val notesTh: String = "",
  val packFileName: String = "patch_pack.json",
  val packSha256: String,
  val packSignatureBase64: String,
)

data class PatchDiffSummary(
  val incomingPatchVersion: String,
  val currentPatchVersion: String,
  val addedChampions: List<String>,
  val updatedChampions: List<String>,
  val addedItems: List<String>,
  val updatedItems: List<String>,
  val addedRunesCount: Int,
  val addedMatchupsCount: Int,
  val patchNotesCount: Int,
) {
  val totalChangedRecords: Int
    get() =
      addedChampions.size +
        updatedChampions.size +
        addedItems.size +
        updatedItems.size +
        addedRunesCount +
        addedMatchupsCount
}

data class VerifiedOtaDownload(
  val releaseTag: String,
  val releaseUrl: String,
  val manifest: OtaReleaseManifest,
  val rawPackJson: String,
  val payload: PatchPackPayload,
  val validation: ValidationResult,
  val diff: PatchDiffSummary,
  val signatureVerified: Boolean,
)

sealed interface OtaCheckStatus {
  data object Idle : OtaCheckStatus

  data object Checking : OtaCheckStatus

  data class UpToDate(val currentPatch: String, val remoteTag: String) : OtaCheckStatus

  data class UpdateReady(val download: VerifiedOtaDownload) : OtaCheckStatus

  data class Applied(val patchVersion: String, val snapshotCaptured: Boolean) : OtaCheckStatus

  data class Error(val message: String) : OtaCheckStatus
}
