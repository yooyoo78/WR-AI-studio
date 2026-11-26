package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.CoachSpeechState
import com.example.data.ai.PostGameScanDraft
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.MatchJournalEntryData
import com.example.domain.engine.MatchJournalAggregator
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PostGameReviewUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostGameReviewSheet(
  state: PostGameReviewUiState,
  allChampions: List<ChampionData>,
  allItems: List<ItemData>,
  language: AppLanguage,
  speechState: CoachSpeechState,
  onPickScoreboardUri: (Uri) -> Unit,
  onUpdateDraft: (Boolean, (PostGameScanDraft) -> PostGameScanDraft) -> Unit,
  onToggleSpeak: (String, String) -> Unit,
  onSaveToJournal: ((MatchJournalEntryData) -> Unit) -> Unit,
  onAskCoachAboutMatch: (MatchJournalEntryData) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showMyChampPicker by remember { mutableStateOf(false) }
  var showEnemyPicker by remember { mutableStateOf(false) }
  var showItemPicker by remember { mutableStateOf(false) }

  val scoreboardPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onPickScoreboardUri(uri)
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("post_game_review_sheet"),
  ) {
    ProvideSafeTextToolbar {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp)
          .padding(bottom = 28.dp),
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
              if (language == AppLanguage.TH) "วิเคราะห์หลังจบเกม (Post-Game Review)"
              else "Post-Game Scoreboard Review",
            style = MaterialTheme.typography.titleLarge,
            color = HextechGoldBright,
          )
          Text(
            text =
              if (language == AppLanguage.TH) {
                "สแกนสกอร์บอร์ดหรือปรับค่าด้านล่างเพื่อวัดความแม่นยำบิลด์และรับ 3 บทเรียนโค้ช"
              } else {
                "Scan scoreboard or edit stats below to score Build Accuracy & get 3 Coach lessons."
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("post_game_close_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Photo Picker Scan Button + Preview
      OutlinedButton(
        onClick = {
          scoreboardPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("post_game_pick_image_button"),
        border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.7f)),
      ) {
        Icon(
          Icons.Default.PhotoLibrary,
          contentDescription = null,
          tint = ArcaneCyan,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (language == AppLanguage.TH) "สแกนภาพสกอร์บอร์ดหลังจบเกม (Photo Picker)"
            else "Scan Post-Game Scoreboard Screenshot",
          color = ArcaneCyan,
        )
      }

      state.previewBitmap?.let { bmp ->
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = 120.dp)
              .clip(MaterialTheme.shapes.small)
              .background(HextechSurfaceHigh),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Scoreboard preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(110.dp),
          )
        }
      }

      if (state.isScanning) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CircularProgressIndicator(color = ArcaneCyan, modifier = Modifier.size(22.dp))
          Text(
            text =
              if (language == AppLanguage.TH) "AI กำลังอ่าน KDA ไอเทม ทอง และให้คะแนนบิลด์..."
              else "Reading KDA, items, gold & evaluating build accuracy...",
            style = MaterialTheme.typography.bodyMedium,
            color = ArcaneCyan,
          )
        }
      }

      state.errorMessage?.let { err ->
        Surface(
          shape = MaterialTheme.shapes.small,
          color = ThreatCrimson.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = err,
            style = MaterialTheme.typography.bodySmall,
            color = ThreatCrimson,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      val eval = state.evaluation
      val draft = eval?.draft
      val report = eval?.accuracyReport

      if (draft != null) {
        // Inline Editable Match Summary Card
        HextechCard(accentColor = if (draft.isWin) AdvantageEmerald else ThreatCrimson) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = draft.isWin,
              onClick = { onUpdateDraft(false) { it.copy(isWin = true) } },
              label = { Text(if (language == AppLanguage.TH) "ชนะ (WIN)" else "WIN") },
              modifier = Modifier.testTag("post_game_win_chip"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = AdvantageEmerald.copy(alpha = 0.22f),
                  selectedLabelColor = AdvantageEmerald,
                ),
            )
            FilterChip(
              selected = !draft.isWin,
              onClick = { onUpdateDraft(false) { it.copy(isWin = false) } },
              label = { Text(if (language == AppLanguage.TH) "แพ้ (LOSS)" else "LOSS") },
              modifier = Modifier.testTag("post_game_loss_chip"),
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ThreatCrimson.copy(alpha = 0.22f),
                  selectedLabelColor = ThreatCrimson,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (draft.usedModelLabel.isNotBlank()) {
              Text(
                text = draft.usedModelLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ArcaneCyan,
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Champion + Lane
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            draft.myChampion?.let { ChampionCrestAvatar(champion = it, size = 40.dp) }
            OutlinedButton(
              onClick = { showMyChampPicker = true },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Text(
                text =
                  draft.myChampion?.displayName(language)
                    ?: if (language == AppLanguage.TH) "เลือกแชมเปี้ยน" else "Pick Champion",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Lane.entries.forEach { lane ->
              FilterChip(
                selected = draft.detectedLane == lane,
                onClick = { onUpdateDraft(false) { it.copy(detectedLane = lane) } },
                label = { Text(if (language == AppLanguage.TH) lane.labelTh else lane.labelEn) },
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // K / D / A + Gold / Min quick numeric edit
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StatNumberField(
              label = "K",
              value = draft.kills,
              onValueChange = { k -> onUpdateDraft(false) { it.copy(kills = k.coerceIn(0, 60)) } },
              modifier = Modifier.weight(1f),
            )
            StatNumberField(
              label = "D",
              value = draft.deaths,
              onValueChange = { d -> onUpdateDraft(false) { it.copy(deaths = d.coerceIn(0, 60)) } },
              modifier = Modifier.weight(1f),
            )
            StatNumberField(
              label = "A",
              value = draft.assists,
              onValueChange = { a -> onUpdateDraft(false) { it.copy(assists = a.coerceIn(0, 80)) } },
              modifier = Modifier.weight(1f),
            )
            StatNumberField(
              label = if (language == AppLanguage.TH) "นาที" else "Min",
              value = draft.durationMinutes,
              onValueChange = { m ->
                onUpdateDraft(false) { it.copy(durationMinutes = m.coerceIn(5, 60)) }
              },
              modifier = Modifier.weight(1.1f),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Bought Items Strip
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                if (language == AppLanguage.TH) "ไอเทมที่ซื้อจริง (${draft.boughtItems.size})"
                else "Bought Items (${draft.boughtItems.size})",
              style = MaterialTheme.typography.labelLarge,
              color = HextechGold,
            )
            OutlinedButton(
              onClick = { showItemPicker = true },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.TH) "เพิ่มไอเทม" else "Add Item")
            }
          }
          if (draft.boughtItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              draft.boughtItems.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  ItemIconSlot(
                    item = item,
                    language = language,
                    onClick = {
                      onUpdateDraft(false) { cur ->
                        cur.copy(boughtItems = cur.boughtItems.filterNot { it.id == item.id })
                      }
                    },
                    size = 38.dp,
                  )
                  Text(
                    text = "×",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThreatCrimson,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Enemies Strip
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text =
                if (language == AppLanguage.TH) "ฝั่งตรงข้าม (${draft.enemyChampions.size}/5)"
                else "Enemy Team (${draft.enemyChampions.size}/5)",
              style = MaterialTheme.typography.labelLarge,
              color = ThreatCrimson,
            )
            OutlinedButton(
              onClick = { showEnemyPicker = true },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (language == AppLanguage.TH) "เลือกศัตรู" else "Add Enemy")
            }
          }
          if (draft.enemyChampions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              draft.enemyChampions.forEach { enemy ->
                FilterChip(
                  selected = true,
                  onClick = {
                    onUpdateDraft(false) { cur ->
                      cur.copy(enemyChampions = cur.enemyChampions.filterNot { it.id == enemy.id })
                    }
                  },
                  label = { Text(enemy.displayName(language)) },
                  trailingIcon = {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Remove",
                      modifier = Modifier.size(14.dp),
                    )
                  },
                )
              }
            }
          }
        }

        // Build Accuracy Card
        if (report != null) {
          val accColor =
            when {
              report.accuracyPct >= 80 -> AdvantageEmerald
              report.accuracyPct >= 60 -> HextechGold
              else -> ThreatCrimson
            }
          HextechCard(
            accentColor = accColor,
            modifier = Modifier.testTag("post_game_accuracy_card"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (language == AppLanguage.TH) "Build Accuracy Score" else "Build Accuracy",
                style = MaterialTheme.typography.titleMedium,
                color = accColor,
              )
              Text(
                text = "${report.accuracyPct}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accColor,
                modifier = Modifier.testTag("post_game_accuracy_pct"),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { (report.accuracyPct / 100f).coerceIn(0f, 1f) },
              color = accColor,
              trackColor = accColor.copy(alpha = 0.18f),
              modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            )
            if (report.missedCoreItems.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text =
                  if (language == AppLanguage.TH) "ไอเทมแนะนำที่พลาดไป:"
                  else "Missed Smart Build Items:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                report.missedCoreItems.take(4).forEach { missed ->
                  ItemIconSlot(
                    item = missed,
                    language = language,
                    onClick = {},
                    size = 34.dp,
                  )
                }
              }
            }
            if (report.mistakeTags.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                report.mistakeTags.forEach { tag ->
                  val stat = MatchJournalAggregator.mapMistakeStat(tag, 1)
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = WarningAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f)),
                  ) {
                    Text(
                      text = stat.label(language),
                      style = MaterialTheme.typography.labelSmall,
                      color = WarningAmber,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // 3 Coach Lessons + TTS Card
        HextechCard(
          accentColor = ArcaneCyan,
          modifier = Modifier.testTag("post_game_lessons_card"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArcaneCyan)
              Text(
                text =
                  if (language == AppLanguage.TH) "3 บทเรียนสำคัญจากโค้ช"
                  else "3 Post-Game Coach Lessons",
                style = MaterialTheme.typography.titleMedium,
                color = ArcaneCyan,
              )
            }
            val isSpeakingHere = speechState.isSpeaking && speechState.activeMessageId == "post_game_tts"
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              IconButton(
                onClick = { onUpdateDraft(true) { it } },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  Icons.Default.Refresh,
                  contentDescription = "Recalculate lessons",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(
                onClick = {
                  val speakText =
                    draft.spokenBriefing.ifBlank { draft.lessons.joinToString(". ") }
                  onToggleSpeak("post_game_tts", speakText)
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("post_game_tts_button"),
              ) {
                Icon(
                  imageVector = if (isSpeakingHere) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = "Speak Post-Game Lessons",
                  tint = if (isSpeakingHere) WarningAmber else HextechGoldBright,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          draft.lessons.take(3).forEachIndexed { idx, lesson ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
              Text(
                text = "${idx + 1}. $lesson",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(9.dp),
              )
            }
          }
        }

        // Save + Ask AI Coach Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onSaveToJournal {} },
            enabled = !state.isSaved,
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("post_game_save_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (state.isSaved) AdvantageEmerald else HextechGold,
                contentColor = Color(0xFF140F02),
              ),
          ) {
            Icon(
              imageVector = if (state.isSaved) Icons.Default.BookmarkAdded else Icons.Default.Save,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text =
                if (state.isSaved) {
                  if (language == AppLanguage.TH) "บันทึกแล้ว" else "Saved"
                } else {
                  if (language == AppLanguage.TH) "บันทึกลง Journal" else "Save Journal"
                },
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedButton(
            onClick = {
              val existing = state.lastSavedEntry
              if (existing != null) {
                onDismiss()
                onAskCoachAboutMatch(existing)
              } else {
                onSaveToJournal { saved ->
                  onDismiss()
                  onAskCoachAboutMatch(saved)
                }
              }
            },
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("post_game_ask_coach_button"),
          ) {
            Icon(
              Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = ArcaneCyan,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (language == AppLanguage.TH) "ถาม AI โค้ชต่อ" else "Ask Coach")
          }
        }
      }
    }
    }
  }

  if (showMyChampPicker) {
    ChampionPickerBottomSheet(
      title = if (language == AppLanguage.TH) "เลือกแชมเปี้ยนของคุณ" else "Select Your Champion",
      champions = allChampions,
      selectedIds = setOfNotNull(state.evaluation?.draft?.myChampion?.id),
      language = language,
      onSelectChampion = { champ ->
        showMyChampPicker = false
        onUpdateDraft(true) {
          it.copy(
            myChampion = champ,
            detectedLane = champ.lanes.firstOrNull() ?: it.detectedLane,
            enemyChampions = it.enemyChampions.filterNot { e -> e.id == champ.id },
          )
        }
      },
      onDismiss = { showMyChampPicker = false },
    )
  }

  if (showEnemyPicker) {
    val draft = state.evaluation?.draft
    ChampionPickerBottomSheet(
      title = if (language == AppLanguage.TH) "เลือกแชมเปี้ยนฝั่งตรงข้าม" else "Toggle Enemy Champion",
      champions = allChampions,
      selectedIds = draft?.enemyChampions?.map { it.id }?.toSet().orEmpty(),
      disabledIds = setOfNotNull(draft?.myChampion?.id),
      language = language,
      onSelectChampion = { champ ->
        onUpdateDraft(true) { cur ->
          val next =
            if (cur.enemyChampions.any { it.id == champ.id }) {
              cur.enemyChampions.filterNot { it.id == champ.id }
            } else if (cur.enemyChampions.size < 5) {
              cur.enemyChampions + champ
            } else cur.enemyChampions
          cur.copy(enemyChampions = next)
        }
      },
      onDismiss = { showEnemyPicker = false },
    )
  }

  if (showItemPicker) {
    ItemPickerBottomSheet(
      title = if (language == AppLanguage.TH) "เพิ่มไอเทมที่ซื้อในเกม" else "Add Bought Item",
      items = allItems,
      selectedIds = state.evaluation?.draft?.boughtItems?.map { it.id }?.toSet().orEmpty(),
      language = language,
      onSelectItem = { item ->
        onUpdateDraft(true) { cur ->
          val next =
            if (cur.boughtItems.any { it.id == item.id }) {
              cur.boughtItems.filterNot { it.id == item.id }
            } else if (cur.boughtItems.size < 7) {
              cur.boughtItems + item
            } else cur.boughtItems
          cur.copy(boughtItems = next)
        }
      },
      onDismiss = { showItemPicker = false },
    )
  }
}

@Composable
private fun StatNumberField(
  label: String,
  value: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = value.toString(),
    onValueChange = { raw ->
      val digits = raw.filter { it.isDigit() }
      onValueChange(digits.toIntOrNull() ?: 0)
    },
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = modifier,
  )
}
