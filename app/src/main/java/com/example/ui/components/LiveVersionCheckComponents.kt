package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.remote.LiveVersionCheckState
import com.example.data.remote.OfficialPatchParser
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechOutline
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.TextMutedLight
import com.example.ui.theme.ThreatCrimson

/**
 * Minimal Version Check control requested by user: ONLY a Check button ("ตรวจสอบ") + Red/Green status LED.
 */
@Composable
fun TopBarVersionCheckPill(
  state: LiveVersionCheckState,
  language: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isOutdated =
    when (state) {
      is LiveVersionCheckState.Outdated,
      is LiveVersionCheckState.Error -> true
      is LiveVersionCheckState.Idle -> {
        val known = state.lastKnownOfficialPatch
        !known.isNullOrBlank() &&
          OfficialPatchParser.comparePatchVersions(known, state.appPatch) > 0
      }
      else -> false
    }
  val isMatched =
    when (state) {
      is LiveVersionCheckState.Matched -> true
      is LiveVersionCheckState.Idle -> {
        val known = state.lastKnownOfficialPatch
        !known.isNullOrBlank() &&
          OfficialPatchParser.comparePatchVersions(known, state.appPatch) <= 0
      }
      else -> false
    }

  val targetLedColor =
    when {
      state is LiveVersionCheckState.Checking -> ArcaneCyan
      isOutdated -> ThreatCrimson
      isMatched -> AdvantageEmerald
      else -> TextMutedLight
    }
  val ledColor by animateColorAsState(targetValue = targetLedColor, label = "version_led")
  val borderColor =
    when {
      isOutdated -> ThreatCrimson.copy(alpha = 0.7f)
      isMatched -> AdvantageEmerald.copy(alpha = 0.6f)
      else -> HextechOutline
    }

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(999.dp),
    color = HextechSurfaceHigh,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier.minimumInteractiveComponentSize().testTag("topbar_version_check_button"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (state is LiveVersionCheckState.Checking) {
        CircularProgressIndicator(
          modifier = Modifier.size(11.dp).testTag("topbar_version_led"),
          strokeWidth = 1.8.dp,
          color = ArcaneCyan,
        )
      } else {
        Box(
          modifier =
            Modifier.size(10.dp)
              .clip(CircleShape)
              .background(ledColor)
              .border(1.5.dp, ledColor.copy(alpha = 0.45f), CircleShape)
              .testTag("topbar_version_led")
        )
      }
      Text(
        text = if (language == AppLanguage.TH) "ตรวจสอบ" else "Check",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color =
          when {
            isOutdated -> ThreatCrimson
            isMatched -> AdvantageEmerald
            else -> MaterialTheme.colorScheme.onSurface
          },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVersionCheckSheet(
  state: LiveVersionCheckState,
  language: AppLanguage,
  onRecheck: () -> Unit,
  onApplyOtaUpdate: () -> Unit,
  onOpenPatchCenter: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = HextechSurface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 10.dp)
          .testTag("live_version_check_sheet"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            if (language == AppLanguage.TH) "ตรวจสอบเวอร์ชั่นเกมล่าสุด"
            else "Live Game Version Check",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = HextechGoldBright,
        )
        TopBarVersionCheckPill(
          state = state,
          language = language,
          onClick = onRecheck,
        )
      }

      when (state) {
        is LiveVersionCheckState.Checking -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = HextechSurfaceHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Text(
                text =
                  if (language == AppLanguage.TH) "กำลังเช็ค Patch Notes ทางการ และ OTA Pack..."
                  else "Checking Official Patch Notes & OTA Pack...",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }

        is LiveVersionCheckState.Matched -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = AdvantageEmerald.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text =
                  if (language == AppLanguage.TH) {
                    "เวอร์ชั่นตรงกับเกมล่าสุดแล้ว (Patch ${state.appPatch})"
                  } else {
                    "Up to date with Live Game (Patch ${state.appPatch})"
                  },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AdvantageEmerald,
              )
              Text(
                text =
                  if (language == AppLanguage.TH) {
                    "แอป: ${state.appPatch} • ทางการล่าสุด: ${state.officialPatch}"
                  } else {
                    "App: ${state.appPatch} • Official Live: ${state.officialPatch}"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        is LiveVersionCheckState.Outdated -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = ThreatCrimson.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text =
                  if (language == AppLanguage.TH) {
                    "พบแพทช์เกมใหม่: ${state.officialPatch} (แอปปัจจุบัน ${state.appPatch})"
                  } else {
                    "Newer Live Patch ${state.officialPatch} detected (App: ${state.appPatch})"
                  },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ThreatCrimson,
              )
              state.otaMessage?.let { hint ->
                Text(
                  text = hint,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (state.otaReady != null) {
                Button(
                  onClick = onApplyOtaUpdate,
                  modifier =
                    Modifier.fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .testTag("version_sheet_apply_ota_button"),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = AdvantageEmerald,
                      contentColor = Color(0xFF041A11),
                    ),
                ) {
                  Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text =
                      if (language == AppLanguage.TH) {
                        "อัปเดตเป็น Patch ${state.otaReady.diff.incomingPatchVersion} ทันที"
                      } else {
                        "Update to Patch ${state.otaReady.diff.incomingPatchVersion} Now"
                      },
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
          TextButton(
            onClick = {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.officialUrl)))
              }
            },
            contentPadding = PaddingValues(horizontal = 4.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (language == AppLanguage.TH) "เปิดหน้า Patch Notes ทางการ" else "View Official Patch Notes")
          }
        }

        is LiveVersionCheckState.Error -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = ThreatCrimson.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.reason,
              style = MaterialTheme.typography.bodySmall,
              color = ThreatCrimson,
              modifier = Modifier.padding(14.dp),
            )
          }
        }

        is LiveVersionCheckState.Idle -> {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = HextechSurfaceHigh,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text =
                if (language == AppLanguage.TH) {
                  "เวอร์ชั่นแอปปัจจุบัน: Patch ${state.appPatch}" +
                    (state.lastKnownOfficialPatch?.let { " • ล่าสุดที่เคยตรวจ: $it" } ?: "")
                } else {
                  "App Patch: ${state.appPatch}" +
                    (state.lastKnownOfficialPatch?.let { " • Last official check: $it" } ?: "")
                },
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onRecheck,
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("version_sheet_recheck_button"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (language == AppLanguage.TH) "ตรวจสอบอีกครั้ง" else "Re-check")
        }
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenPatchCenter()
          },
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("version_sheet_open_patch_button"),
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = HextechGold)
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (language == AppLanguage.TH) "ไป Patch Center" else "Patch Center")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}
