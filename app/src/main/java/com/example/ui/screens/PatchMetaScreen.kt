package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.PatchChangeType
import com.example.data.validation.ValidationResult
import com.example.ui.components.HextechCard
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.PatchActionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchMetaScreen(
  homeState: HomeUiState,
  actionStatus: PatchActionStatus,
  validationResult: ValidationResult?,
  sampleSchemaJson: String,
  onImportJsonUri: (android.net.Uri) -> Unit,
  onVerifyIntegrity: () -> Unit,
  onResetSeed: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val strings = LocalUiStrings.current
  val lang = homeState.preferences.language
  var showSampleSchema by remember { mutableStateOf(false) }

  val openJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        onImportJsonUri(uri)
      }
    }

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
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Active Patch & Data Source Attribution Card
        item {
          HextechCard(accentColor = HextechGold) {
            Text(
              text =
                "${strings.activePatchLabel}: ${homeState.activePatch?.version ?: homeState.preferences.activePatchLabel}",
              style = MaterialTheme.typography.titleLarge,
              color = HextechGold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.sourceAttributionHint,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text =
                homeState.preferences.lastImportedPackInfo.ifBlank {
                  "Official Patch 7.3f + Curated Tactical Matrix (${homeState.activePatch?.releaseDate ?: "2026-11-15"})"
                },
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        // Extensible JSON Patch Pack Importer & Validator
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Text(text = strings.patchImportTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) {
                  "สามารถนำเข้าไฟล์ .json เพื่อเพิ่มแชมเปี้ยน ไอเทม รูน หรือปรับปรุง Patch ใหม่ได้ทันทีโดยไม่ต้องอัปเดตตัวแอป ระบบจะตรวจสอบความถูกต้องของ ID ก่อนบันทึกลงฐานข้อมูล Room"
                } else {
                  "Import a .json Patch Pack via Android Storage Access Framework (zero storage permission needed) to add new Champions, Items, Runes, or Matchups without rebuilding the app."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = { openJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("import_patch_json_button"),
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
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("verify_db_integrity_button"),
              ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.validateDbButton)
              }
              OutlinedButton(
                onClick = onResetSeed,
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("reset_seed_button"),
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
              Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.small,
              ) {
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
                  color =
                    if (actionStatus.isError) ThreatCrimson.copy(alpha = 0.16f)
                    else AdvantageEmerald.copy(alpha = 0.16f),
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
                    Text(
                      text = actionStatus.text,
                      style = MaterialTheme.typography.bodySmall,
                    )
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

        // Patch Notes History
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
}
