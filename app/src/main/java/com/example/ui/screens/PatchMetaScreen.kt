package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.PatchChangeType
import com.example.data.model.PatchSnapshotSummary
import com.example.data.remote.LiveVersionCheckState
import com.example.data.remote.OtaCheckStatus
import com.example.data.validation.ValidationResult
import com.example.ui.components.HextechCard
import com.example.ui.components.TopBarVersionCheckPill
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.PatchActionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PatchDetailSheetType {
  NOTES,
  OTA,
  TOOLS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchMetaScreen(
  homeState: HomeUiState,
  actionStatus: PatchActionStatus,
  validationResult: ValidationResult?,
  sampleSchemaJson: String,
  otaStatus: OtaCheckStatus = OtaCheckStatus.Idle,
  liveVersionState: LiveVersionCheckState = LiveVersionCheckState.Idle("7.3f"),
  patchSnapshots: List<PatchSnapshotSummary> = emptyList(),
  onCheckLiveVersion: () -> Unit = {},
  onApplyLiveVersionUpdate: () -> Unit = {},
  onSaveOtaConfig: (repoSlug: String, publicKeyBase64: String, autoCheck: Boolean) -> Unit = { _, _, _ -> },
  onCheckOtaUpdates: (repoSlug: String?, publicKeyBase64: String?) -> Unit = { _, _ -> },
  onApplyVerifiedOta: () -> Unit = {},
  onRollbackToPreviousPatch: () -> Unit = {},
  onImportJsonUri: (android.net.Uri) -> Unit,
  onVerifyIntegrity: () -> Unit,
  onResetSeed: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = homeState.preferences.language
  var activeSheet by remember { mutableStateOf<PatchDetailSheetType?>(null) }
  var showSampleSchema by remember { mutableStateOf(false) }

  val openJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        onImportJsonUri(uri)
      }
    }

  val activePatchVersion =
    homeState.preferences.activePatchLabel.ifBlank { homeState.activePatch?.version ?: "7.3f" }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(strings.patchCenterTitle) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("patch_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("patch_lazy_column"),
        contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 12.dp),
      ) {
        // 1. Clean Overview Card: ONLY Current Active Patch + Live Version Check Mode
        item {
          HextechCard(accentColor = HextechGold, modifier = Modifier.testTag("patch_overview_card")) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${strings.activePatchLabel}: $activePatchVersion",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = HextechGoldBright,
                )
                Text(
                  text =
                    homeState.activePatch?.let {
                      "${it.releaseDate} • ${homeState.totalChampions} Champs • ${homeState.totalItems} Items"
                    } ?: "${homeState.totalChampions} Champs • ${homeState.totalItems} Items",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              TopBarVersionCheckPill(
                state = liveVersionState,
                language = lang,
                onClick = onCheckLiveVersion,
              )
            }

            if (liveVersionState is LiveVersionCheckState.Outdated) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) {
                    "แพทช์เกมล่าสุดคือ ${liveVersionState.officialPatch} (แอปปัจจุบัน $activePatchVersion)"
                  } else {
                    "Live Wild Rift is on Patch ${liveVersionState.officialPatch} (App: $activePatchVersion)"
                  },
                style = MaterialTheme.typography.bodySmall,
                color = ThreatCrimson,
              )
              if (liveVersionState.otaReady != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                  onClick = onApplyLiveVersionUpdate,
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
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
                      if (lang == AppLanguage.TH) {
                        "อัปเดตเป็น Patch ${liveVersionState.otaReady.diff.incomingPatchVersion}"
                      } else {
                        "Apply Patch ${liveVersionState.otaReady.diff.incomingPatchVersion}"
                      },
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }

        // 2. Detail Launcher: Patch Notes & Changelogs
        item {
          HextechCard(
            accentColor = ArcaneCyan,
            onClick = { activeSheet = PatchDetailSheetType.NOTES },
            modifier = Modifier.fillMaxWidth().testTag("patch_open_notes_sheet_button"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = ArcaneCyan)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (lang == AppLanguage.TH) "รายละเอียดแพทช์และประวัติการปรับสมดุล" else "Patch Notes & Balance History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (lang == AppLanguage.TH) "${homeState.allPatchNotes.size} เวอร์ชั่น • แตะเพื่อดูรายการบัฟ/เนิร์ฟ"
                      else "${homeState.allPatchNotes.size} versions • Tap for buff/nerf details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArcaneCyan)
            }
          }
        }

        // 3. Detail Launcher: Signed OTA Patch Pack & Rollback
        item {
          HextechCard(
            accentColor = AdvantageEmerald,
            onClick = { activeSheet = PatchDetailSheetType.OTA },
            modifier = Modifier.fillMaxWidth().testTag("patch_open_ota_sheet_button"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AdvantageEmerald)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (lang == AppLanguage.TH) "ตั้งค่า OTA อัตโนมัติ & Rollback" else "Signed OTA Sync & Rollback",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  val repoSummary =
                    homeState.preferences.otaRepoSlug.ifBlank {
                      if (lang == AppLanguage.TH) "ยังไม่ได้ระบุ GitHub Repo" else "No GitHub Repo configured"
                    }
                  Text(
                    text = repoSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AdvantageEmerald)
            }
          }
        }

        // 4. Detail Launcher: Manual JSON Import & Integrity Tools
        item {
          HextechCard(
            accentColor = HextechGold,
            onClick = { activeSheet = PatchDetailSheetType.TOOLS },
            modifier = Modifier.fillMaxWidth().testTag("patch_open_tools_sheet_button"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = HextechGold)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = strings.patchImportTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text =
                      if (lang == AppLanguage.TH) "นำเข้าไฟล์ JSON • ตรวจ Integrity • รีเซ็ตข้อมูลตั้งต้น"
                      else "Import .json • Verify Integrity • Restore Seed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = HextechGold)
            }
          }
        }
      }
    }
  }

  when (activeSheet) {
    PatchDetailSheetType.NOTES -> {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { activeSheet = null },
        sheetState = sheetState,
        containerColor = HextechSurface,
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).testTag("patch_notes_detail_sheet"),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            Text(
              text = if (lang == AppLanguage.TH) "รายละเอียดแพทช์ทั้งหมด" else "Patch Notes History",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = HextechGoldBright,
            )
          }
          items(homeState.allPatchNotes, key = { it.version }) { patch ->
            HextechCard {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Patch ${patch.version}",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (patch.isActive) HextechGold else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${patch.releaseDate} • ${patch.sourceLabel}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (lang == AppLanguage.TH) patch.headlineTh else patch.headlineEn,
                style = MaterialTheme.typography.titleSmall,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (lang == AppLanguage.TH) patch.summaryTh else patch.summaryEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              patch.changes.forEach { entry ->
                val color =
                  when (entry.changeType) {
                    PatchChangeType.NEW -> HextechGold
                    PatchChangeType.BUFF -> AdvantageEmerald
                    PatchChangeType.NERF -> ThreatCrimson
                    PatchChangeType.ADJUST -> WarningAmber
                  }
                Text(
                  text =
                    "• [${if (lang == AppLanguage.TH) entry.changeType.labelTh else entry.changeType.labelEn}] ${entry.targetName}: ${if (lang == AppLanguage.TH) entry.detailTh else entry.detailEn}",
                  style = MaterialTheme.typography.bodySmall,
                  color = color,
                  modifier = Modifier.padding(vertical = 2.dp),
                )
              }
            }
          }
        }
      }
    }

    PatchDetailSheetType.OTA -> {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { activeSheet = null },
        sheetState = sheetState,
        containerColor = HextechSurface,
      ) {
        var repoSlugInput by remember(homeState.preferences.otaRepoSlug) {
          mutableStateOf(homeState.preferences.otaRepoSlug)
        }
        var pubKeyInput by remember(homeState.preferences.otaPublicKeyBase64) {
          mutableStateOf(homeState.preferences.otaPublicKeyBase64)
        }
        var autoCheckEnabled by remember(homeState.preferences.autoCheckOtaOnLaunch) {
          mutableStateOf(homeState.preferences.autoCheckOtaOnLaunch)
        }
        LaunchedEffect(homeState.preferences.otaRepoSlug, homeState.preferences.otaPublicKeyBase64) {
          if (repoSlugInput.isBlank() && homeState.preferences.otaRepoSlug.isNotBlank()) {
            repoSlugInput = homeState.preferences.otaRepoSlug
          }
          if (pubKeyInput.isBlank() && homeState.preferences.otaPublicKeyBase64.isNotBlank()) {
            pubKeyInput = homeState.preferences.otaPublicKeyBase64
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).testTag("patch_ota_config_sheet"),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            HextechCard(accentColor = AdvantageEmerald, modifier = Modifier.testTag("ota_config_card")) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AdvantageEmerald)
                Text(
                  text =
                    if (lang == AppLanguage.TH) "OTA Patch Pack (GitHub Releases + ECDSA)"
                    else "Signed OTA Patch Pack (GitHub Releases)",
                  style = MaterialTheme.typography.titleMedium,
                  color = AdvantageEmerald,
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = repoSlugInput,
                onValueChange = { repoSlugInput = it },
                label = { Text("GitHub Repo (owner/repo)") },
                placeholder = { Text("your-org/wild-rift-patches") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("ota_repo_input"),
              )
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedTextField(
                value = pubKeyInput,
                onValueChange = { pubKeyInput = it },
                label = { Text("ECDSA P-256 Public Key (Base64 / PEM)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("ota_pubkey_input"),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text =
                    if (lang == AppLanguage.TH) "เช็คอัปเดตอัตโนมัติตอนเปิดแอป"
                    else "Auto-check OTA on app launch",
                  style = MaterialTheme.typography.bodySmall,
                )
                Switch(
                  checked = autoCheckEnabled,
                  onCheckedChange = { checked ->
                    autoCheckEnabled = checked
                    onSaveOtaConfig(repoSlugInput, pubKeyInput, checked)
                  },
                  modifier = Modifier.testTag("ota_auto_check_switch"),
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { onSaveOtaConfig(repoSlugInput, pubKeyInput, autoCheckEnabled) },
                  modifier = Modifier.weight(0.9f).minimumInteractiveComponentSize().testTag("ota_save_config_button"),
                ) {
                  Text(if (lang == AppLanguage.TH) "บันทึกค่า" else "Save")
                }
                Button(
                  onClick = {
                    onSaveOtaConfig(repoSlugInput, pubKeyInput, autoCheckEnabled)
                    onCheckOtaUpdates(repoSlugInput, pubKeyInput)
                  },
                  modifier = Modifier.weight(1.3f).minimumInteractiveComponentSize().testTag("ota_check_button"),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = AdvantageEmerald,
                      contentColor = Color(0xFF031810),
                    ),
                ) {
                  Text(
                    text = if (lang == AppLanguage.TH) "เช็คอัปเดต OTA" else "Check OTA",
                    fontWeight = FontWeight.Bold,
                  )
                }
              }

              when (otaStatus) {
                OtaCheckStatus.Idle -> Unit
                OtaCheckStatus.Checking -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                      text =
                        if (lang == AppLanguage.TH) "กำลังโหลดและตรวจลายเซ็น SHA256withECDSA..."
                        else "Downloading & verifying ECDSA signature...",
                      style = MaterialTheme.typography.bodySmall,
                      color = ArcaneCyan,
                    )
                  }
                }
                is OtaCheckStatus.UpToDate -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(color = AdvantageEmerald.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
                    Text(
                      text = "Patch ${otaStatus.currentPatch} (${otaStatus.remoteTag}) ✓",
                      style = MaterialTheme.typography.bodySmall,
                      color = AdvantageEmerald,
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                    )
                  }
                }
                is OtaCheckStatus.UpdateReady -> {
                  val dl = otaStatus.download
                  val diff = dl.diff
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(
                    color = HextechGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().testTag("ota_diff_card"),
                  ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AdvantageEmerald, modifier = Modifier.size(16.dp))
                        Text(
                          text = "Signature Verified ✓ • Patch ${diff.incomingPatchVersion} (${dl.releaseTag})",
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = HextechGoldBright,
                        )
                      }
                      Text(
                        text =
                          "+${diff.addedChampions.size} / ~${diff.updatedChampions.size} Champs • " +
                            "+${diff.addedItems.size} / ~${diff.updatedItems.size} Items • +${diff.addedMatchupsCount} Matchups",
                        style = MaterialTheme.typography.bodySmall,
                      )
                      Button(
                        onClick = onApplyVerifiedOta,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("ota_apply_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = HextechGold, contentColor = Color(0xFF140F02)),
                      ) {
                        Text(
                          text = if (lang == AppLanguage.TH) "ติดตั้งแพ็กที่ยืนยันแล้ว" else "Apply Verified Patch Pack",
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }
                  }
                }
                is OtaCheckStatus.Applied -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(color = AdvantageEmerald.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
                    Text(
                      text = "Applied Patch ${otaStatus.patchVersion} ✓",
                      style = MaterialTheme.typography.bodySmall,
                      color = AdvantageEmerald,
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                    )
                  }
                }
                is OtaCheckStatus.Error -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(color = ThreatCrimson.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
                    Text(
                      text = otaStatus.message,
                      style = MaterialTheme.typography.bodySmall,
                      color = ThreatCrimson,
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                    )
                  }
                }
              }

              if (patchSnapshots.isNotEmpty()) {
                val latest = patchSnapshots.first()
                val timeStr =
                  SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(latest.createdAt))
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth().testTag("ota_rollback_card"),
                ) {
                  Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Text(
                          text = "Rollback: Patch ${latest.patchVersion}",
                          style = MaterialTheme.typography.labelLarge,
                          color = WarningAmber,
                        )
                      }
                      Text(
                        text = "$timeStr • ${latest.sourceLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    OutlinedButton(
                      onClick = onRollbackToPreviousPatch,
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("ota_rollback_button"),
                    ) {
                      Text(if (lang == AppLanguage.TH) "ย้อนกลับ" else "Rollback")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    PatchDetailSheetType.TOOLS -> {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { activeSheet = null },
        sheetState = sheetState,
        containerColor = HextechSurface,
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).testTag("patch_tools_sheet"),
          contentPadding = PaddingValues(bottom = 24.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            HextechCard(accentColor = ArcaneCyan) {
              Text(text = strings.patchImportTitle, style = MaterialTheme.typography.titleMedium)
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("import_patch_json_button"),
              ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.importJsonButton)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onVerifyIntegrity,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("verify_db_integrity_button"),
                ) {
                  Icon(Icons.Default.Verified, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(strings.validateDbButton)
                }
                OutlinedButton(
                  onClick = onResetSeed,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("reset_seed_button"),
                ) {
                  Icon(Icons.Default.Restore, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(strings.resetSeedButton)
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              OutlinedButton(
                onClick = { showSampleSchema = !showSampleSchema },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
              ) {
                Text(strings.sampleJsonButton)
              }
              if (showSampleSchema) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small) {
                  Text(
                    text = sampleSchemaJson,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp),
                  )
                }
              }
              when (actionStatus) {
                PatchActionStatus.Idle -> Unit
                PatchActionStatus.Working -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  CircularProgressIndicator()
                }
                is PatchActionStatus.Message -> {
                  Spacer(modifier = Modifier.height(10.dp))
                  Surface(
                    color = if (actionStatus.isError) ThreatCrimson.copy(alpha = 0.16f) else AdvantageEmerald.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small,
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (actionStatus.isError) ThreatCrimson else AdvantageEmerald,
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(text = actionStatus.text, style = MaterialTheme.typography.bodySmall)
                    }
                  }
                }
              }
              validationResult?.warnings?.takeIf { it.isNotEmpty() }?.let { warnings ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Warnings: ${warnings.joinToString("; ")}",
                  style = MaterialTheme.typography.bodySmall,
                  color = WarningAmber,
                )
              }
            }
          }
        }
      }
    }

    null -> Unit
  }
}
