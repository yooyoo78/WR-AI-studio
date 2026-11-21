package com.example.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.ai.ScreenChampionScanner
import com.example.data.ai.ScreenScanOutcome
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.domain.engine.ChampionScanMatchResult
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuickScanUiState(
  val isSheetOpen: Boolean = false,
  val isQuickCaptureActive: Boolean = false,
  val isScanning: Boolean = false,
  val countdownSeconds: Int? = null,
  val previewBitmap: Bitmap? = null,
  val scanResult: ChampionScanMatchResult? = null,
  val errorMessage: String? = null,
  val autoAppliedToAllModes: Boolean = false,
  val lastCaptureSummary: String? = null,
)

class QuickScanCoordinator(
  private val context: Context,
  private val repository: GameDataRepository,
  private val preferencesRepository: PreferencesRepository,
  private val scanner: ScreenChampionScanner,
  private val onApplyDetectedRoster:
    (myChampionId: String?, lane: Lane, allyIds: List<String>, enemyIds: List<String>) -> Unit,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var scanJob: Job? = null
  private var countdownJob: Job? = null

  private val _uiState = MutableStateFlow(QuickScanUiState())
  val uiState: StateFlow<QuickScanUiState> = _uiState.asStateFlow()

  fun openSheet() {
    _uiState.update { it.copy(isSheetOpen = true) }
  }

  fun dismissSheet() {
    _uiState.update { it.copy(isSheetOpen = false) }
  }

  fun setQuickCaptureActive(active: Boolean) {
    _uiState.update { it.copy(isQuickCaptureActive = active) }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun scanUriAsync(uri: Uri, openSheet: Boolean = true) {
    scanJob?.cancel()
    scanJob =
      scope.launch {
        val preview = withContext(Dispatchers.IO) { decodePreviewThumbnail(uri) }
        _uiState.update {
          it.copy(
            isSheetOpen = openSheet || it.isSheetOpen,
            isScanning = true,
            previewBitmap = preview ?: it.previewBitmap,
            errorMessage = null,
            autoAppliedToAllModes = false,
          )
        }
        repository.ensureSeeded()
        val catalog = repository.champions.first()
        val lang = preferencesRepository.preferencesFlow.first().language
        val outcome = scanner.scanUri(uri, catalog, lang)
        handleOutcome(outcome, lang)
      }
  }

  suspend fun scanCapturedBitmap(
    bitmap: Bitmap,
    openSheet: Boolean = true,
  ): ScreenScanOutcome {
    val thumb = withContext(Dispatchers.Default) { createThumbnail(bitmap) }
    _uiState.update {
      it.copy(
        isSheetOpen = openSheet || it.isSheetOpen,
        isScanning = true,
        countdownSeconds = null,
        previewBitmap = thumb,
        errorMessage = null,
        autoAppliedToAllModes = false,
      )
    }
    repository.ensureSeeded()
    val catalog = repository.champions.first()
    val lang = preferencesRepository.preferencesFlow.first().language
    val outcome = scanner.scanBitmap(bitmap, catalog, lang)
    handleOutcome(outcome, lang)
    return outcome
  }

  fun startCountdownAndCapture(seconds: Int = 3, onFireCapture: () -> Unit) {
    countdownJob?.cancel()
    countdownJob =
      scope.launch {
        for (sec in seconds downTo 1) {
          _uiState.update { it.copy(countdownSeconds = sec, errorMessage = null) }
          delay(1000L)
        }
        _uiState.update { it.copy(countdownSeconds = null) }
        onFireCapture()
      }
  }

  fun cancelCountdown() {
    countdownJob?.cancel()
    _uiState.update { it.copy(countdownSeconds = null) }
  }

  fun setDetectedMyChampion(champion: ChampionData) {
    val current = _uiState.value.scanResult ?: return
    val updatedAllies =
      if (champion.id in current.allies.map { it.id }) {
        current.allies
      } else {
        (listOf(champion) + current.allies).distinctBy { it.id }.take(5)
      }
    val updatedEnemies = current.enemies.filter { it.id != champion.id }
    val updated =
      current.copy(
        myChampion = champion,
        allies = updatedAllies,
        enemies = updatedEnemies,
      )
    applyAndStore(updated)
  }

  fun setDetectedLane(lane: Lane) {
    val current = _uiState.value.scanResult ?: return
    applyAndStore(current.copy(detectedLane = lane))
  }

  fun removeDetectedAlly(champId: String) {
    val current = _uiState.value.scanResult ?: return
    val updatedAllies = current.allies.filterNot { it.id == champId }
    val updatedMyChamp =
      if (current.myChampion?.id == champId) updatedAllies.firstOrNull() else current.myChampion
    applyAndStore(
      current.copy(
        allies = updatedAllies,
        myChampion = updatedMyChamp,
        skippedUnpickedCount = current.skippedUnpickedCount + 1,
      )
    )
  }

  fun removeDetectedEnemy(champId: String) {
    val current = _uiState.value.scanResult ?: return
    val updatedEnemies = current.enemies.filterNot { it.id == champId }
    applyAndStore(
      current.copy(
        enemies = updatedEnemies,
        skippedUnpickedCount = current.skippedUnpickedCount + 1,
      )
    )
  }

  fun reportCaptureError(message: String) {
    _uiState.update {
      it.copy(
        isScanning = false,
        countdownSeconds = null,
        errorMessage = message,
      )
    }
  }

  private fun handleOutcome(outcome: ScreenScanOutcome, language: AppLanguage) {
    when (outcome) {
      is ScreenScanOutcome.Success -> {
        val res = outcome.result
        if (res.allies.isEmpty() && res.enemies.isEmpty()) {
          _uiState.update {
            it.copy(
              isScanning = false,
              scanResult = res,
              autoAppliedToAllModes = false,
              errorMessage =
                if (language == AppLanguage.TH) {
                  "ยังไม่พบแชมเปี้ยนที่เลือกแล้วในภาพนี้ (ข้ามช่องที่ยังไม่เลือก ${res.skippedUnpickedCount} ช่อง) ลองแคปใหม่เมื่อมีคนเลือกตัวแล้ว"
                } else {
                  "No locked/picked champions detected yet (skipped ${res.skippedUnpickedCount} empty slots). Capture again once champions are picked."
                },
            )
          }
        } else {
          applyAndStore(res, language)
        }
      }
      is ScreenScanOutcome.Failure -> {
        _uiState.update {
          it.copy(
            isScanning = false,
            autoAppliedToAllModes = false,
            errorMessage = outcome.message,
          )
        }
      }
    }
  }

  private fun applyAndStore(
    result: ChampionScanMatchResult,
    language: AppLanguage = AppLanguage.TH,
  ) {
    val allyIds = result.allies.map { it.id }
    val enemyIds = result.enemies.map { it.id }
    onApplyDetectedRoster(
      result.myChampion?.id,
      result.detectedLane,
      allyIds,
      enemyIds,
    )
    val summary =
      if (language == AppLanguage.TH) {
        "ฝั่งเรา ${result.allies.size} • ฝั่งตรงข้าม ${result.enemies.size}" +
          if (result.skippedUnpickedCount > 0) " (ข้ามช่องว่าง ${result.skippedUnpickedCount})" else ""
      } else {
        "Allies ${result.allies.size} • Enemies ${result.enemies.size}" +
          if (result.skippedUnpickedCount > 0) " (skipped ${result.skippedUnpickedCount} empty)" else ""
      }
    _uiState.update {
      it.copy(
        isScanning = false,
        scanResult = result,
        errorMessage = null,
        autoAppliedToAllModes = true,
        lastCaptureSummary = summary,
      )
    }
  }

  private fun decodePreviewThumbnail(uri: Uri): Bitmap? {
    return try {
      val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, opts)
      }?.let { createThumbnail(it) }
    } catch (_: Exception) {
      null
    }
  }

  private fun createThumbnail(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    if (maxSide <= 640 || maxSide <= 0) return source
    val ratio = 640f / maxSide.toFloat()
    val w = (source.width * ratio).roundToInt().coerceAtLeast(1)
    val h = (source.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
  }
}
