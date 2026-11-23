package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.capture.QuickCaptureService
import com.example.data.ai.CoachSpeechState
import com.example.data.ai.LiveScreenPhase
import com.example.data.model.AppLanguage
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.LiveCoachUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachLivePanel(
  liveState: LiveCoachUiState,
  speechState: CoachSpeechState,
  isQuickCaptureActive: Boolean,
  language: AppLanguage,
  isCompactHeight: Boolean,
  onStartCountdownCapture: (() -> Unit) -> Unit,
  onCancelCountdown: () -> Unit,
  onPickLiveUri: (Uri) -> Unit,
  onToggleAutoSpeak: () -> Unit,
  onSpeakBriefing: () -> Unit,
  onCapturePermissionDeclined: (String) -> Unit,
  onOpenAnalyzer: () -> Unit,
  onOpenDraft: () -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
) {
  val context = LocalContext.current

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onPickLiveUri(uri)
    }

  val projectionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data
      if (result.resultCode == Activity.RESULT_OK && data != null) {
        QuickCaptureService.startProjection(context, result.resultCode, data)
      } else {
        onCapturePermissionDeclined(
          if (language == AppLanguage.TH) {
            "ไม่ได้รับอนุญาตจับภาพหน้าจอ สามารถใช้ปุ่ม 'เลือกรูป' แทนได้ทันที"
          } else {
            "Screen capture permission declined. You can pick a screenshot instead."
          }
        )
      }
    }

  val notificationLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      launchLiveProjectionConsent(context) { intent -> projectionLauncher.launch(intent) }
    }

  fun onLiveCaptureClick() {
    if (!isQuickCaptureActive) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        launchLiveProjectionConsent(context) { intent -> projectionLauncher.launch(intent) }
      }
    } else if (liveState.countdownSeconds != null) {
      onCancelCountdown()
    } else {
      onStartCountdownCapture { QuickCaptureService.triggerCapture(context, delayMs = 0L) }
    }
  }

  val isSpeakingLive = speechState.isSpeaking && speechState.activeMessageId == "live_briefing"

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("coach_live_list"),
    contentPadding = PaddingValues(if (isCompactHeight) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp),
  ) {
    // Manual Live Trigger Controls (quota-friendly)
    item(key = "live_controls") {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = { onLiveCaptureClick() },
            enabled = !liveState.isAnalyzing,
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("coach_live_capture_button"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isQuickCaptureActive) AdvantageEmerald else ArcaneCyan,
                contentColor = Color(0xFF031520),
              ),
          ) {
            Icon(
              imageVector =
                if (liveState.countdownSeconds != null) Icons.Default.Timer
                else if (isQuickCaptureActive) Icons.Default.Visibility
                else Icons.Default.CameraAlt,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                liveState.countdownSeconds?.let { sec ->
                  if (language == AppLanguage.TH) "แคปใน ${sec}s (แตะยกเลิก)"
                  else "Capturing ${sec}s (Cancel)"
                }
                  ?: if (isQuickCaptureActive) {
                    if (language == AppLanguage.TH) "แคปวิเคราะห์สด (3s)" else "Live Scan (3s)"
                  } else {
                    if (language == AppLanguage.TH) "เปิดดูหน้าจอสด" else "Enable Live Screen"
                  },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            enabled = !liveState.isAnalyzing,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("coach_live_pick_button"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.7f)),
          ) {
            Icon(
              Icons.Default.PhotoLibrary,
              contentDescription = if (language == AppLanguage.TH) "เลือกรูป" else "Pick Image",
              tint = HextechGoldBright,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (language == AppLanguage.TH) "รูปภาพ" else "Image",
              style = MaterialTheme.typography.labelMedium,
              color = HextechGoldBright,
              maxLines = 1,
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color =
              if (liveState.autoSpeakLive) AdvantageEmerald.copy(alpha = 0.2f)
              else MaterialTheme.colorScheme.surfaceContainerHigh,
            border =
              BorderStroke(
                1.dp,
                if (liveState.autoSpeakLive) AdvantageEmerald
                else MaterialTheme.colorScheme.outline,
              ),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleAutoSpeak)
                .testTag("coach_live_autospeak_toggle"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription =
                  if (language == AppLanguage.TH) "อ่านผล Live อัตโนมัติ" else "Auto-speak live advice",
                tint =
                  if (liveState.autoSpeakLive) AdvantageEmerald
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (language == AppLanguage.TH) "พูดสด" else "Voice",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (liveState.autoSpeakLive) FontWeight.Bold else FontWeight.Medium,
                color =
                  if (liveState.autoSpeakLive) AdvantageEmerald
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    // Thumbnail + Loading overlay
    if (liveState.isAnalyzing) {
      item(key = "live_analyzing") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = HextechSurfaceHigh,
          border = BorderStroke(1.2.dp, ArcaneCyan.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            liveState.previewBitmap?.let { bmp ->
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Live screen frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(110.dp),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(
                color = ArcaneCyan,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
              )
              Text(
                text =
                  if (language == AppLanguage.TH) {
                    "Live Coach กำลังตรวจจับหน้าจอและวิเคราะห์คำแนะนำสด..."
                  } else {
                    "Live Coach is analyzing your game screen..."
                  },
                style = MaterialTheme.typography.bodyMedium,
                color = ArcaneCyan,
              )
            }
          }
        }
      }
    }

    // Error card
    liveState.errorMessage?.let { err ->
      item(key = "live_error") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = ThreatCrimson.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth().testTag("coach_live_error_card"),
        ) {
          Text(
            text = err,
            style = MaterialTheme.typography.bodyMedium,
            color = ThreatCrimson,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }

    // Empty instructions before first capture
    val analysis = liveState.analysis
    if (analysis == null && !liveState.isAnalyzing) {
      item(key = "live_empty_guide") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = HextechSurfaceHigh,
          border = BorderStroke(1.2.dp, ArcaneCyan.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth().testTag("coach_live_empty_card"),
        ) {
          Column(
            modifier = Modifier.padding(if (isCompactHeight) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text =
                if (language == AppLanguage.TH) "โหมด Live Coach (ประหยัดโควตา • กดวิเคราะห์เมื่อต้องการ)"
                else "Live Coach Mode (On-Demand Screen Analysis)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = ArcaneCyan,
            )
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "• หน้าดราฟต์ (Champ Select): กด 'เปิดดูหน้าจอสด' หรือดึงแถบแจ้งเตือนกด 'แคป & สแกน' เพื่อรับคำแนะนำตัวที่ควรหยิบและไอเทมแก้ทางแบบกระชับ\n" +
                    "• หน้าโหลดก่อนเริ่มเกม: แนะนำไอเทมแรก, Enchant และแผนยืนเลนต้นเกมกับคู่ต่อสู้ที่เจอทันที (เปิด 'พูดสด' เพื่อฟังเสียงอ่านอัตโนมัติได้)"
                } else {
                  "• Champ Select: Tap Live Screen or notification 'Capture & Scan' for concise pick suggestions and counter items.\n" +
                    "• Loading Screen: Get instant starter/first-item advice and Lv 1–5 laning tips against detected opponents (enable Voice for auto read-aloud)."
                },
              style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
              color = TextPrimaryLight,
            )
          }
        }
      }
    }

    // Live Coach Analysis Cards
    if (analysis != null) {
      item(key = "live_header_card") {
        val phaseColor =
          when (analysis.phase) {
            LiveScreenPhase.CHAMP_SELECT -> HextechGold
            LiveScreenPhase.LOADING_SCREEN -> ArcaneCyan
            LiveScreenPhase.IN_GAME -> AdvantageEmerald
            LiveScreenPhase.UNKNOWN -> ArcaneCyan
          }
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = HextechSurfaceHigh,
          border = BorderStroke(1.5.dp, phaseColor.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth().testTag("coach_live_result_card"),
        ) {
          Column(
            modifier = Modifier.padding(if (isCompactHeight) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = phaseColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.55f)),
              ) {
                Text(
                  text = if (language == AppLanguage.TH) analysis.phase.labelTh else analysis.phase.labelEn,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = phaseColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (analysis.usedModelLabel.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ArcaneCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = analysis.usedModelLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = ArcaneCyan,
                      maxLines = 1,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
                if (analysis.skippedUnpickedCount > 0) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WarningAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text =
                        if (language == AppLanguage.TH) "ข้ามว่าง ${analysis.skippedUnpickedCount}"
                        else "Skip ${analysis.skippedUnpickedCount}",
                      style = MaterialTheme.typography.labelSmall,
                      color = WarningAmber,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
                IconButton(
                  onClick = onSpeakBriefing,
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("coach_live_speak_button"),
                ) {
                  Icon(
                    imageVector =
                      if (isSpeakingLive) Icons.Default.Stop
                      else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription =
                      if (isSpeakingLive) {
                        if (language == AppLanguage.TH) "หยุดอ่าน" else "Stop speaking"
                      } else {
                        if (language == AppLanguage.TH) "ฟังคำแนะนำสด" else "Speak live briefing"
                      },
                    tint = if (isSpeakingLive) HextechGoldBright else ArcaneCyan,
                  )
                }
              }
            }

            if (analysis.headline.isNotBlank()) {
              Text(
                text = analysis.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = phaseColor,
              )
            }

            // Detected Allies & Enemies mini-roster
            if (analysis.allies.isNotEmpty() || analysis.enemies.isNotEmpty()) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                analysis.allies.forEach { ally ->
                  val isMe = analysis.myChampion?.id == ally.id
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color =
                      if (isMe) HextechGold.copy(alpha = 0.2f)
                      else ArcaneCyan.copy(alpha = 0.12f),
                    border =
                      BorderStroke(
                        1.dp,
                        if (isMe) HextechGold else ArcaneCyan.copy(alpha = 0.5f),
                      ),
                    modifier =
                      Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenChampionDetail(ally.id) },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      ChampionCrestAvatar(champion = ally, size = 20.dp)
                      Text(
                        text = ally.displayName(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) HextechGoldBright else TextPrimaryLight,
                      )
                    }
                  }
                }
                analysis.enemies.forEach { enemy ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThreatCrimson.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.5f)),
                    modifier =
                      Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenChampionDetail(enemy.id) },
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      ChampionCrestAvatar(champion = enemy, size = 20.dp)
                      Text(
                        text = enemy.displayName(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThreatCrimson,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Recommended Picks during Champ Select
      if (analysis.recommendedPicks.isNotEmpty()) {
        item(key = "live_picks_card") {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = HextechSurfaceHigh,
            border = BorderStroke(1.2.dp, HextechGold.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().testTag("coach_live_picks_card"),
          ) {
            Column(
              modifier = Modifier.padding(if (isCompactHeight) 10.dp else 14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text =
                  if (language == AppLanguage.TH) "ตัวละครแนะนำให้เลือก (Draft Picks)"
                  else "Recommended Draft Picks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = HextechGoldBright,
              )
              analysis.recommendedPicks.forEach { pick ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  pick.resolvedChampion?.let { champ ->
                    ChampionCrestAvatar(
                      champion = champ,
                      size = 32.dp,
                      onClick = { onOpenChampionDetail(champ.id) },
                    )
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = pick.resolvedChampion?.displayName(language) ?: pick.championName,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = HextechGoldBright,
                    )
                    if (pick.reason.isNotBlank()) {
                      Text(
                        text = pick.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimaryLight,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Recommended Items & Boot Enchants
      if (analysis.recommendedItems.isNotEmpty()) {
        item(key = "live_items_card") {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = HextechSurfaceHigh,
            border = BorderStroke(1.2.dp, ArcaneCyan.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().testTag("coach_live_items_card"),
          ) {
            Column(
              modifier = Modifier.padding(if (isCompactHeight) 10.dp else 14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text =
                  if (language == AppLanguage.TH) "ไอเทม & Enchant ที่ควรออก"
                  else "Recommended Items & Enchants",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ArcaneCyan,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                analysis.recommendedItems.forEach { itemText ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ArcaneCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = itemText,
                      style = MaterialTheme.typography.labelMedium,
                      color = TextPrimaryLight,
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Early Game Plan & Threat Alert
      if (analysis.earlyLanePlan.isNotBlank() || analysis.threatAlert.isNotBlank()) {
        item(key = "live_early_plan_card") {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = HextechSurfaceHigh,
            border = BorderStroke(1.2.dp, AdvantageEmerald.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().testTag("coach_live_early_card"),
          ) {
            Column(
              modifier = Modifier.padding(if (isCompactHeight) 10.dp else 14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (analysis.earlyLanePlan.isNotBlank()) {
                Text(
                  text =
                    if (language == AppLanguage.TH) "แผนยืนเลนต้นเกมกับคู่ต่อสู้"
                    else "Early Laning Plan",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = AdvantageEmerald,
                )
                Text(
                  text = analysis.earlyLanePlan,
                  style =
                    MaterialTheme.typography.bodyMedium.copy(
                      lineHeight = if (isCompactHeight) 20.sp else 22.sp
                    ),
                  color = TextPrimaryLight,
                )
              }

              if (analysis.threatAlert.isNotBlank()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = ThreatCrimson.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.55f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      Icons.Default.Warning,
                      contentDescription = null,
                      tint = ThreatCrimson,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = analysis.threatAlert,
                      style = MaterialTheme.typography.bodySmall,
                      color = TextPrimaryLight,
                    )
                  }
                }
              }

              // Quick jump to synced 1v5 or Draft
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onOpenAnalyzer,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (language == AppLanguage.TH) "เปิด 1v5" else "Open 1v5",
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
                OutlinedButton(
                  onClick = onOpenDraft,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (language == AppLanguage.TH) "เปิด Draft" else "Open Draft",
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun launchLiveProjectionConsent(
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
