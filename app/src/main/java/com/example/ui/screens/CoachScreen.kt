package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechObsidian
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.CoachChatMessage
import com.example.ui.viewmodel.CoachUiState
import com.example.ui.viewmodel.GeminiUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(
  state: CoachUiState,
  geminiState: GeminiUiState,
  messages: List<CoachChatMessage>,
  onSelectChampion: (ChampionData) -> Unit,
  onSelectLane: (Lane) -> Unit,
  onAskGemini: (String) -> Unit,
  onRetryGemini: () -> Unit,
  onClearChat: () -> Unit,
) {
  val strings = LocalUiStrings.current
  val isMiniMode = LocalInGameMiniMode.current
  val lang = state.language
  var selectedTab by remember { mutableIntStateOf(0) }
  var showChampPicker by remember { mutableStateOf(false) }
  var laneMenuExpanded by remember { mutableStateOf(false) }
  var questionInput by remember { mutableStateOf("") }
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  val quickPrompts =
    if (lang == AppLanguage.TH) {
      listOf(
        "ช่วงเลนเลเวล 1–5 ควรคุมเวฟและเทรดยังไง?",
        "ไฟต์มังกร/บารอนควรยืนตำแหน่งและเปิดยังไง?",
        "ถ้าแพ้เลนต้องปรับไอเทมและเดินเกมอย่างไร?",
      )
    } else {
      listOf(
        "How should I manage waves and trade levels 1-5?",
        "What is my exact teamfight positioning around Dragon?",
        "How do I play from behind and adjust my build?",
      )
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    val isCompactHeight = isMiniMode || maxHeight < 520.dp
    val isNarrowWidth = maxWidth < 340.dp
    val outerPad = if (isCompactHeight) 6.dp else 10.dp

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 780.dp)
          .padding(horizontal = outerPad, vertical = if (isCompactHeight) 4.dp else 8.dp),
      verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 5.dp else 8.dp),
    ) {
      // Ultra-compact single-row secondary control strip so the advice viewport dominates the screen
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("coach_compact_topbar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          // Compact Champion Picker Pill
          val selectedChamp = state.selectedChampion
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = selectedChamp != null) { showChampPicker = true }
                .testTag("coach_select_champion"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (selectedChamp != null) {
                ChampionCrestAvatar(champion = selectedChamp, size = 24.dp)
                Text(
                  text = selectedChamp.displayName(lang),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
              } else {
                Text(
                  text = strings.selectYourChampion,
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
              }
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = strings.selectYourChampion,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }

          // Compact Lane Dropdown Pill (replaces multi-row FilterChips)
          Box {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { laneMenuExpanded = true }
                  .testTag("coach_select_lane_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (lang == AppLanguage.TH) state.selectedLane.labelTh else state.selectedLane.labelEn,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = strings.selectLane,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
            DropdownMenu(
              expanded = laneMenuExpanded,
              onDismissRequest = { laneMenuExpanded = false },
            ) {
              Lane.entries.forEach { lane ->
                val isSelectedLane = lane == state.selectedLane
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn,
                      color = if (isSelectedLane) HextechGoldBright else MaterialTheme.colorScheme.onSurface,
                      fontWeight = if (isSelectedLane) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                  trailingIcon =
                    if (isSelectedLane) {
                      {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = null,
                          tint = HextechGoldBright,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  onClick = {
                    onSelectLane(lane)
                    laneMenuExpanded = false
                  },
                  modifier = Modifier.testTag("coach_lane_option_${lane.name}"),
                )
              }
            }
          }

          // Compact Segmented Mode Toggle (Playbook vs Gemini AI)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CompactModeSegment(
                selected = selectedTab == 0,
                label = if (lang == AppLanguage.TH) "คู่มือ" else "Guide",
                showIcon = !isNarrowWidth,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = HextechGold,
                contentDescription = strings.offlinePlaybookTab,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("coach_offline_tab"),
              )
              CompactModeSegment(
                selected = selectedTab == 1,
                label = "AI",
                showIcon = !isNarrowWidth,
                icon = Icons.Default.AutoAwesome,
                accentColor = ArcaneCyan,
                contentDescription = strings.geminiCoachTab,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("coach_gemini_tab"),
              )
            }
          }
        }
      }

      // HERO SPOTLIGHT ADVICE VIEWPORT — dominates screen space & visual prominence
      val spotlightAccent = if (selectedTab == 0) HextechGold else ArcaneCyan
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = HextechObsidian,
        tonalElevation = 4.dp,
        border = BorderStroke(1.5.dp, spotlightAccent.copy(alpha = 0.65f)),
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("coach_advice_spotlight"),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    spotlightAccent.copy(alpha = 0.11f),
                    HextechSurface.copy(alpha = 0.55f),
                    HextechObsidian,
                  )
                )
              )
        ) {
          if (selectedTab == 0) {
            val playbookListState = rememberLazyListState()
            val playbookSections = state.offlinePlaybook

            LazyColumn(
              state = playbookListState,
              modifier = Modifier.fillMaxSize().testTag("coach_playbook_list"),
              contentPadding = PaddingValues(if (isCompactHeight) 8.dp else 12.dp),
              verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp),
            ) {
              // Scrollable Spotlight Header + Phase Jump Pills (scrolls out of view while reading)
              item(key = "playbook_header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = HextechGoldBright,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text =
                          if (lang == AppLanguage.TH) "คู่มือและคำแนะนำการเล่น"
                          else "Tactical Playbook & Advice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HextechGoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = HextechGold.copy(alpha = 0.16f),
                      border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
                    ) {
                      Text(
                        text = "Patch ${state.activePatchVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HextechGoldBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                      )
                    }
                  }

                  if (state.enemyContext.isNotEmpty()) {
                    Text(
                      text =
                        (if (lang == AppLanguage.TH) "รับมือ: " else "Matchup vs ") +
                          state.enemyContext.joinToString(", ") { it.displayName(lang) },
                      style = MaterialTheme.typography.labelMedium,
                      color = ThreatCrimson,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }

                  if (playbookSections.isNotEmpty()) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      val jumpLabels =
                        if (lang == AppLanguage.TH) listOf("ต้นเกม", "กลางเกม", "ท้ายเกม")
                        else listOf("Early", "Mid", "Late")
                      playbookSections.forEachIndexed { idx, _ ->
                        val chipColor = phaseAccentColor(idx)
                        Surface(
                          shape = RoundedCornerShape(999.dp),
                          color = chipColor.copy(alpha = 0.14f),
                          border = BorderStroke(1.dp, chipColor.copy(alpha = 0.45f)),
                          modifier =
                            Modifier.clip(RoundedCornerShape(999.dp)).clickable {
                              coroutineScope.launch {
                                playbookListState.animateScrollToItem(idx + 1)
                              }
                            },
                        ) {
                          Text(
                            text = jumpLabels.getOrElse(idx) { "${idx + 1}" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = chipColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }

              itemsIndexed(playbookSections, key = { idx, sec -> "${idx}_${sec.titleEn}" }) { index, section ->
                val phaseAccent = phaseAccentColor(index)
                val phaseBadge =
                  when (index) {
                    0 -> if (lang == AppLanguage.TH) "EARLY • ต้นเกม" else "EARLY PHASE"
                    1 -> if (lang == AppLanguage.TH) "MID • กลางเกม" else "MID PHASE"
                    else -> if (lang == AppLanguage.TH) "LATE • ท้ายเกม" else "LATE PHASE"
                  }

                AdviceSpotlightCard(
                  accentColor = phaseAccent,
                  badgeText = phaseBadge,
                  title = section.title(lang),
                  body = section.body(lang),
                  compact = isCompactHeight,
                )
              }
            }
          } else {
            val chatListState = rememberLazyListState()
            val isBusy =
              geminiState is GeminiUiState.Loading || geminiState is GeminiUiState.Streaming

            LaunchedEffect(messages.size, geminiState) {
              if (messages.isNotEmpty() || geminiState !is GeminiUiState.Idle) {
                val targetIdx = (chatListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                chatListState.animateScrollToItem(targetIdx)
              }
            }

            LazyColumn(
              state = chatListState,
              modifier = Modifier.fillMaxSize().testTag("coach_chat_list"),
              contentPadding = PaddingValues(if (isCompactHeight) 8.dp else 12.dp),
              verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp),
            ) {
              item(key = "quick_prompts_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  itemsIndexed(quickPrompts) { idx, prompt ->
                    Surface(
                      shape = RoundedCornerShape(999.dp),
                      color = ArcaneCyan.copy(alpha = 0.12f),
                      border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.4f)),
                      modifier =
                        Modifier.clip(RoundedCornerShape(999.dp))
                          .clickable(enabled = !isBusy) { onAskGemini(prompt) }
                          .testTag("coach_quick_prompt_$idx"),
                    ) {
                      Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBusy) MaterialTheme.colorScheme.onSurfaceVariant else ArcaneCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                  }
                }
              }

              if (messages.isEmpty() && geminiState is GeminiUiState.Idle) {
                item(key = "gemini_empty_spotlight") {
                  AdviceSpotlightCard(
                    accentColor = ArcaneCyan,
                    badgeText = "GEMINI AI COACH • PATCH ${state.activePatchVersion}",
                    title =
                      if (lang == AppLanguage.TH) "พร้อมวิเคราะห์แมตช์อัปให้คุณทันที"
                      else "Ready for Live Matchup Coaching",
                    body =
                      if (lang == AppLanguage.TH) {
                        val champName = state.selectedChampion?.displayName(lang) ?: ""
                        val laneLabel = state.selectedLane.labelTh
                        "พิมพ์คำถามด้านล่างหรือแตะหัวข้อด่วนด้านบน เพื่อรับแผนเล่น $champName ($laneLabel) แบบละเอียด"
                      } else {
                        val champName = state.selectedChampion?.displayName(lang) ?: ""
                        val laneLabel = state.selectedLane.labelEn
                        "Type a question below or tap a quick prompt above for tailored $champName ($laneLabel) advice."
                      },
                    compact = isCompactHeight,
                  )
                }
              }

              itemsIndexed(messages, key = { idx, msg -> "${idx}_${msg.isUser}_${msg.text.hashCode()}" }) { _, msg ->
                if (msg.isUser) {
                  // Slim user prompt pill so AI coaching advice remains visually dominant
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = HextechGold.copy(alpha = 0.16f),
                      border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
                      modifier = Modifier.widthIn(max = 300.dp),
                    ) {
                      Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = HextechGoldBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                  }
                } else {
                  AdviceSpotlightCard(
                    accentColor = ArcaneCyan,
                    badgeText = "GEMINI AI COACH",
                    title = null,
                    body = msg.text,
                    compact = isCompactHeight,
                    onCopy = { clipboardManager.setText(AnnotatedString(msg.text)) },
                  )
                }
              }

              when (geminiState) {
                GeminiUiState.Loading -> {
                  item(key = "gemini_loading") {
                    Surface(
                      shape = MaterialTheme.shapes.medium,
                      color = HextechSurfaceHigh,
                      border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.55f)),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
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
                            if (lang == AppLanguage.TH) "กำลังวิเคราะห์คำแนะนำกับ Gemini AI Coach..."
                            else "Analyzing matchup with Gemini AI Coach...",
                          style = MaterialTheme.typography.bodyMedium,
                          color = ArcaneCyan,
                        )
                      }
                    }
                  }
                }
                is GeminiUiState.Streaming -> {
                  item(key = "gemini_streaming") {
                    AdviceSpotlightCard(
                      accentColor = ArcaneCyan,
                      badgeText = if (lang == AppLanguage.TH) "กำลังพิมพ์คำแนะนำ..." else "STREAMING ADVICE...",
                      title = null,
                      body = geminiState.text,
                      compact = isCompactHeight,
                    )
                  }
                }
                is GeminiUiState.Error -> {
                  item(key = "gemini_error") {
                    Surface(
                      shape = MaterialTheme.shapes.medium,
                      color = ThreatCrimson.copy(alpha = 0.12f),
                      border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.65f)),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                          text = geminiState.message,
                          style = MaterialTheme.typography.bodyMedium,
                          color = ThreatCrimson,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ThreatCrimson.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ThreatCrimson),
                            modifier =
                              Modifier.minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onRetryGemini)
                                .testTag("coach_retry_gemini_button"),
                          ) {
                            Text(
                              text = if (lang == AppLanguage.TH) "ลองใหม่" else "Retry",
                              style = MaterialTheme.typography.labelLarge,
                              color = TextPrimaryLight,
                              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                          }
                          Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HextechGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, HextechGold),
                            modifier =
                              Modifier.minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 }
                                .testTag("coach_fallback_offline_button"),
                          ) {
                            Text(
                              text = strings.offlinePlaybookTab,
                              style = MaterialTheme.typography.labelLarge,
                              color = HextechGoldBright,
                              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                          }
                        }
                      }
                    }
                  }
                }
                else -> Unit
              }
            }
          }
        }
      }

      // Slim Bottom Input Dock (only shown on AI Coach tab so Playbook gets 100% of remaining space)
      if (selectedTab == 1) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = if (isCompactHeight) 3.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (messages.isNotEmpty()) {
              IconButton(
                onClick = onClearChat,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("coach_clear_chat_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = "Clear chat",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            OutlinedTextField(
              value = questionInput,
              onValueChange = { questionInput = it },
              modifier = Modifier.weight(1f).testTag("coach_question_input"),
              placeholder = {
                Text(
                  text =
                    if (isCompactHeight) {
                      if (lang == AppLanguage.TH) "ถามโค้ช AI..." else "Ask AI Coach..."
                    } else {
                      strings.askGeminiHint
                    },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              maxLines = if (isCompactHeight) 2 else 3,
              textStyle = MaterialTheme.typography.bodyMedium,
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = ArcaneCyan,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )
            val canSend =
              questionInput.isNotBlank() &&
                geminiState !is GeminiUiState.Loading &&
                geminiState !is GeminiUiState.Streaming
            IconButton(
              onClick = {
                val q = questionInput
                questionInput = ""
                onAskGemini(q)
              },
              enabled = canSend,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("coach_send_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = strings.sendPrompt,
                tint = if (canSend) ArcaneCyan else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showChampPicker) {
    ChampionPickerBottomSheet(
      title = strings.selectYourChampion,
      champions = state.allChampions,
      selectedIds = setOfNotNull(state.selectedChampion?.id),
      language = lang,
      onSelectChampion = {
        onSelectChampion(it)
        showChampPicker = false
      },
      onDismiss = { showChampPicker = false },
    )
  }
}

@Composable
private fun CompactModeSegment(
  selected: Boolean,
  label: String,
  showIcon: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(7.dp),
    color = if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent,
    border = if (selected) BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)) else null,
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(7.dp))
        .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (showIcon) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun AdviceSpotlightCard(
  accentColor: Color,
  badgeText: String,
  title: String?,
  body: String,
  compact: Boolean,
  onCopy: (() -> Unit)? = null,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = HextechSurfaceHigh,
    tonalElevation = 3.dp,
    border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      Box(
        modifier =
          Modifier.width(4.dp)
            .fillMaxHeight()
            .background(accentColor)
      )
      Column(
        modifier = Modifier.weight(1f).padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
              )
            }
          }
          if (onCopy != null) {
            IconButton(
              onClick = onCopy,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy advice",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        if (!title.isNullOrBlank()) {
          Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
        }

        Text(
          text = body,
          style =
            if (compact) {
              MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
            } else {
              MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp)
            },
          color = TextPrimaryLight,
        )
      }
    }
  }
}

private fun phaseAccentColor(index: Int): Color =
  when (index) {
    0 -> HextechGold
    1 -> ArcaneCyan
    else -> AdvantageEmerald
  }
