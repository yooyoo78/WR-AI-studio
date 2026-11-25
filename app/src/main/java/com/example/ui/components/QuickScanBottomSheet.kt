package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.capture.QuickCaptureService
import com.example.capture.QuickScanCoordinator
import com.example.capture.QuickScanUiState
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickScanBottomSheet(
  state: QuickScanUiState,
  language: AppLanguage,
  coordinator: QuickScanCoordinator,
  onNavigateToAnalyzer: () -> Unit,
  onNavigateToDraft: () -> Unit,
  onNavigateToCoach: () -> Unit,
  onAnalyzePostGame: (android.net.Uri?, android.graphics.Bitmap?) -> Unit = { _, _ -> },
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        coordinator.scanUriAsync(uri, openSheet = true)
      }
    }

  val projectionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data
      if (result.resultCode == Activity.RESULT_OK && data != null) {
        QuickCaptureService.startProjection(context, result.resultCode, data)
      } else {
        coordinator.reportCaptureError(
          if (language == AppLanguage.TH) {
            "ไม่ได้รับอนุญาตให้จับภาพหน้าจอ คุณยังสามารถใช้ปุ่ม 'เลือกรูปที่แคปไว้' หรือแชร์ภาพเข้าแอปได้"
          } else {
            "Screen capture permission declined. You can still pick or share a screenshot."
          }
        )
      }
    }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      // Proceed to launch MediaProjection consent regardless (notification still works or falls back)
      launchScreenCaptureConsent(context) { intent -> projectionLauncher.launch(intent) }
    }

  fun requestQuickCaptureStart() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      launchScreenCaptureConsent(context) { intent -> projectionLauncher.launch(intent) }
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("quick_scan_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp)
          .padding(bottom = 26.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (language == AppLanguage.TH) "แคปเร็ว & สแกนแชมเปี้ยนอัตโนมัติ"
              else "Quick Capture & Auto Champion Scan",
            style = MaterialTheme.typography.titleLarge,
            color = HextechGoldBright,
          )
          Text(
            text =
              if (language == AppLanguage.TH) {
                "ตรวจจับฝั่งเรา/ฝั่งตรงข้าม ข้ามช่องที่ยังไม่เลือก และส่งเข้า 1v5, Draft, AI Coach ทันที"
              } else {
                "Detects allies & enemies, skips unpicked slots, and syncs to 1v5, Draft & AI Coach."
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_scan_close_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Quick Capture Mode Card (MediaProjection + Notification/Quick Settings Tile)
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border =
          BorderStroke(
            1.2.dp,
            if (state.isQuickCaptureActive) AdvantageEmerald.copy(alpha = 0.7f)
            else ArcaneCyan.copy(alpha = 0.55f),
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = null,
              tint = if (state.isQuickCaptureActive) AdvantageEmerald else ArcaneCyan,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (state.isQuickCaptureActive) {
                    if (language == AppLanguage.TH) "โหมดแคปเร็วกำลังเปิดใช้งานอยู่"
                    else "Quick Capture Service Active"
                  } else {
                    if (language == AppLanguage.TH) "แคปหน้าจอแบบเร็ว (1-Tap Notification / Tile)"
                    else "Quick Screen Capture (Notification & QS Tile)"
                  },
                style = MaterialTheme.typography.titleSmall,
                color = if (state.isQuickCaptureActive) AdvantageEmerald else ArcaneCyan,
              )
              Text(
                text =
                  if (state.isQuickCaptureActive) {
                    if (language == AppLanguage.TH) {
                      "สลับเข้าเกม Wild Rift แล้วดึงแถบแจ้งเตือนกด 'แคป & สแกน' หรือกดปุ่มนับถอยหลัง 3 วิ"
                    } else {
                      "Switch to Wild Rift, pull notification shade & tap 'Capture & Scan', or use 3s timer."
                    }
                  } else {
                    if (language == AppLanguage.TH) {
                      "เปิดทิ้งไว้ข้างเกม เพื่อกดแคปจากแถบแจ้งเตือนหรือ Quick Settings ส่งเข้าแอปอัตโนมัติ"
                    } else {
                      "Keep active while in Wild Rift to capture from Notification/Quick Settings automatically."
                    }
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (!state.isQuickCaptureActive) {
            Button(
              onClick = { requestQuickCaptureStart() },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("quick_scan_start_capture_button"),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = ArcaneCyan,
                  contentColor = Color(0xFF031520),
                ),
            ) {
              Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (language == AppLanguage.TH) "เปิดโหมดแคปเร็วบนแถบแจ้งเตือน"
                  else "Enable Quick Screen Capture",
                fontWeight = FontWeight.Bold,
              )
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  if (state.countdownSeconds != null) {
                    coordinator.cancelCountdown()
                  } else {
                    coordinator.startCountdownAndCapture(seconds = 3) {
                      QuickCaptureService.triggerCapture(context, delayMs = 0L)
                    }
                  }
                },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("quick_scan_countdown_button"),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = HextechGold,
                    contentColor = Color(0xFF140F02),
                  ),
              ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text =
                    state.countdownSeconds?.let { sec ->
                      if (language == AppLanguage.TH) "กำลังแคปใน ${sec}s... (แตะยกเลิก)"
                      else "Capturing in ${sec}s... (Cancel)"
                    }
                      ?: if (language == AppLanguage.TH) "แคป & สแกนใน 3 วิ"
                      else "Capture in 3s",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              OutlinedButton(
                onClick = { QuickCaptureService.stopProjection(context) },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("quick_scan_stop_capture_button"),
                border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.7f)),
              ) {
                Icon(
                  Icons.Default.StopCircle,
                  contentDescription = "Stop capture",
                  tint = ThreatCrimson,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }

      // Zero-permission Photo Picker + Share tip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("quick_scan_pick_image_button"),
          border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.65f)),
        ) {
          Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = HextechGoldBright,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (language == AppLanguage.TH) "เลือกรูปสแกน"
              else "Pick Image",
            color = HextechGoldBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = {
            onDismiss()
            onAnalyzePostGame(state.sourceUri, coordinator.lastPostGameBitmap ?: state.previewBitmap)
          },
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("quick_scan_post_game_button"),
          border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.7f)),
        ) {
          Icon(
            Icons.Default.Analytics,
            contentDescription = null,
            tint = AdvantageEmerald,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (language == AppLanguage.TH) "วิเคราะห์จบเกม"
              else "Post-Game",
            color = AdvantageEmerald,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text =
          if (language == AppLanguage.TH) {
            "เคล็ดลับ: กดแคปหน้าจอมือถือตามปกติแล้วกด 'แชร์ -> WR-AI' ระบบจะสแกนเข้า 1v5, Draft และ AI Coach ให้อัตโนมัติทันที"
          } else {
            "Tip: Take a system screenshot and tap Share -> WR-AI to auto-scan directly into 1v5, Draft, and AI Coach."
          },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Thumbnail preview + Scanning indicator
      state.previewBitmap?.let { bmp ->
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = 140.dp)
              .clip(MaterialTheme.shapes.small)
              .background(HextechSurfaceHigh),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Captured screenshot preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(130.dp),
          )
          if (state.isScanning) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(130.dp)
                  .background(Color.Black.copy(alpha = 0.62f)),
              contentAlignment = Alignment.Center,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CircularProgressIndicator(
                  color = ArcaneCyan,
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.5.dp,
                )
                Text(
                  text =
                    if (language == AppLanguage.TH) "กำลังตรวจจับแชมเปี้ยนในภาพ..."
                    else "Detecting champions from screenshot...",
                  style = MaterialTheme.typography.labelLarge,
                  color = TextPrimaryLight,
                )
              }
            }
          }
        }
      }

      if (state.isScanning && state.previewBitmap == null) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CircularProgressIndicator(
            color = ArcaneCyan,
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
          )
          Text(
            text =
              if (language == AppLanguage.TH) "กำลังสแกนแชมเปี้ยนฝั่งเราและฝั่งตรงข้าม..."
              else "Scanning allied and enemy champions...",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcaneCyan,
          )
        }
      }

      // Error message if any
      state.errorMessage?.let { err ->
        Surface(
          shape = MaterialTheme.shapes.small,
          color = ThreatCrimson.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth().testTag("quick_scan_error_card"),
        ) {
          Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = ThreatCrimson,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      // Detected Roster + Auto-Apply + Routing to 1v5 / Draft / AI Coach
      state.scanResult?.takeIf { it.allies.isNotEmpty() || it.enemies.isNotEmpty() }?.let { res ->
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.2.dp, HextechGold.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth().testTag("quick_scan_result_card"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = AdvantageEmerald,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    if (language == AppLanguage.TH) {
                      "ซิงก์เข้า 1v5 • Draft • AI Coach แล้ว"
                    } else {
                      "Synced to 1v5 • Draft • AI Coach"
                    },
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = AdvantageEmerald,
                )
              }
              if (res.skippedUnpickedCount > 0) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = WarningAmber.copy(alpha = 0.18f),
                  border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f)),
                ) {
                  Text(
                    text =
                      if (language == AppLanguage.TH) "ข้ามยังไม่เลือก ${res.skippedUnpickedCount}"
                      else "Skipped ${res.skippedUnpickedCount} empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
              }
            }

            if (res.confidenceNote.isNotBlank()) {
              Text(
                text = res.confidenceNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            // Lane selector
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Lane.entries.forEach { lane ->
                FilterChip(
                  selected = res.detectedLane == lane,
                  onClick = { coordinator.setDetectedLane(lane) },
                  label = { Text(if (language == AppLanguage.TH) lane.labelTh else lane.labelEn) },
                )
              }
            }

            // Allies (tap to set as My Champion, tap X to remove)
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "ฝั่งเรา (${res.allies.size}/5) — แตะเลือกเป็นตัวของคุณ"
                } else {
                  "Allied Team (${res.allies.size}/5) — Tap to set as You"
                },
              style = MaterialTheme.typography.labelMedium,
              color = ArcaneCyan,
            )
            if (res.allies.isEmpty()) {
              Text(
                text =
                  if (language == AppLanguage.TH) "ยังไม่มีฝั่งเราที่เลือกแล้ว (ข้ามช่องว่าง)"
                  else "No picked allies detected yet (empty slots skipped)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                res.allies.forEach { ally ->
                  val isMe = res.myChampion?.id == ally.id
                  DetectedChampionChip(
                    champion = ally,
                    language = language,
                    isHighlightedMe = isMe,
                    accentColor = if (isMe) HextechGold else ArcaneCyan,
                    onClick = { coordinator.setDetectedMyChampion(ally) },
                    onRemove = { coordinator.removeDetectedAlly(ally.id) },
                  )
                }
              }
            }

            // Enemies
            Text(
              text =
                if (language == AppLanguage.TH) "ฝั่งตรงข้าม (${res.enemies.size}/5)"
                else "Enemy Team (${res.enemies.size}/5)",
              style = MaterialTheme.typography.labelMedium,
              color = ThreatCrimson,
            )
            if (res.enemies.isEmpty()) {
              Text(
                text =
                  if (language == AppLanguage.TH) "ยังไม่มีฝั่งตรงข้ามที่เลือกแล้ว (ข้ามช่องว่าง)"
                  else "No picked enemies detected yet (empty slots skipped)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                res.enemies.forEach { enemy ->
                  DetectedChampionChip(
                    champion = enemy,
                    language = language,
                    isHighlightedMe = false,
                    accentColor = ThreatCrimson,
                    onClick = {},
                    onRemove = { coordinator.removeDetectedEnemy(enemy.id) },
                  )
                }
              }
            }

            // Direct mode action buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  onDismiss()
                  onNavigateToAnalyzer()
                },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("quick_scan_go_analyzer_button"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = HextechGold,
                    contentColor = Color(0xFF140F02),
                  ),
              ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (language == AppLanguage.TH) "1v5" else "1v5",
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                )
              }
              OutlinedButton(
                onClick = {
                  onDismiss()
                  onNavigateToDraft()
                },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("quick_scan_go_draft_button"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
              ) {
                Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (language == AppLanguage.TH) "Draft" else "Draft",
                  maxLines = 1,
                )
              }
              OutlinedButton(
                onClick = {
                  onDismiss()
                  onNavigateToCoach()
                },
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("quick_scan_go_coach_button"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
              ) {
                Icon(
                  Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = ArcaneCyan,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (language == AppLanguage.TH) "AI โค้ช" else "AI Coach",
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetectedChampionChip(
  champion: ChampionData,
  language: AppLanguage,
  isHighlightedMe: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  onRemove: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = accentColor.copy(alpha = if (isHighlightedMe) 0.24f else 0.12f),
    border = BorderStroke(if (isHighlightedMe) 1.5.dp else 1.dp, accentColor.copy(alpha = 0.75f)),
    modifier = Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ChampionCrestAvatar(champion = champion, size = 26.dp)
      Column {
        Text(
          text = champion.displayName(language),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (isHighlightedMe) FontWeight.Bold else FontWeight.Medium,
          color = if (isHighlightedMe) HextechGoldBright else MaterialTheme.colorScheme.onSurface,
        )
        if (isHighlightedMe) {
          Text(
            text = if (language == AppLanguage.TH) "ตัวเรา" else "You",
            style = MaterialTheme.typography.labelSmall,
            color = HextechGold,
          )
        }
      }
      IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Remove ${champion.nameEn}",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

private fun launchScreenCaptureConsent(
  context: Context,
  onLaunch: (android.content.Intent) -> Unit,
) {
  val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  val intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      mgr.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
      mgr.createScreenCaptureIntent()
    }
  onLaunch(intent)
}
