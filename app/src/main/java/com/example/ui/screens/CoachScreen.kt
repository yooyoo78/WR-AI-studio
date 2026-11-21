package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.Lane
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.components.HextechCard
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.CoachChatMessage
import com.example.ui.viewmodel.CoachUiState
import com.example.ui.viewmodel.GeminiUiState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
  var questionInput by remember { mutableStateOf("") }

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

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)) {
      // Champion & Lane Context Header
      Column(
        modifier =
          Modifier.padding(
            horizontal = if (isMiniMode) 8.dp else 12.dp,
            vertical = if (isMiniMode) 6.dp else 10.dp,
          )
      ) {
        state.selectedChampion?.let { champ ->
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { showChampPicker = true }
                .testTag("coach_select_champion"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.6f)),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ChampionCrestAvatar(champion = champ, size = 40.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${champ.displayName(lang)} • ${if (lang == AppLanguage.TH) state.selectedLane.labelTh else state.selectedLane.labelEn}",
                  style = MaterialTheme.typography.titleSmall,
                )
                if (state.enemyContext.isNotEmpty()) {
                  Text(
                    text = "vs ${state.enemyContext.joinToString(", ") { it.nameEn }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThreatCrimson,
                  )
                }
              }
              Icon(Icons.Default.SwapHoriz, contentDescription = "Change Champion", tint = ArcaneCyan)
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Lane.entries.forEach { lane ->
            FilterChip(
              selected = state.selectedLane == lane,
              onClick = { onSelectLane(lane) },
              label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
            )
          }
        }
      }

      SecondaryTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          modifier = Modifier.testTag("coach_offline_tab"),
          text = { Text(strings.offlinePlaybookTab) },
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          modifier = Modifier.testTag("coach_gemini_tab"),
          text = { Text(strings.geminiCoachTab) },
        )
      }

      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
          verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
        ) {
          items(state.offlinePlaybook) { section ->
            HextechCard(accentColor = HextechGold) {
              Text(
                text = section.title(lang),
                style = MaterialTheme.typography.titleMedium,
                color = HextechGold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(text = section.body(lang), style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      } else {
        val chatListState = rememberLazyListState()
        LaunchedEffect(messages.size, geminiState) {
          if (messages.isNotEmpty() || geminiState !is GeminiUiState.Idle) {
            val targetIdx = (chatListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            chatListState.animateScrollToItem(targetIdx)
          }
        }
        Column(modifier = Modifier.fillMaxSize()) {
          LazyColumn(
            state = chatListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 8.dp else 10.dp),
          ) {
            item {
              val isBusy =
                geminiState is GeminiUiState.Loading || geminiState is GeminiUiState.Streaming
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { prompt ->
                  FilterChip(
                    selected = false,
                    enabled = !isBusy,
                    onClick = { onAskGemini(prompt) },
                    label = { Text(prompt) },
                  )
                }
              }
            }

            if (messages.isEmpty() && geminiState is GeminiUiState.Idle) {
              item {
                HextechCard(accentColor = ArcaneCyan) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArcaneCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text =
                        if (lang == AppLanguage.TH) {
                          "ถามโค้ช Gemini AI (Patch ${state.activePatchVersion}) เกี่ยวกับจังหวะเทรด การคุมเวฟ หรือแก้ทางคู่ต่อสู้ได้เลย"
                        } else {
                          "Ask Gemini AI Coach (Patch ${state.activePatchVersion}) about trading windows, wave control, or matchup adjustments."
                        },
                      style = MaterialTheme.typography.bodyMedium,
                    )
                  }
                }
              }
            }

            items(messages) { msg ->
              HextechCard(
                accentColor = if (msg.isUser) HextechGold else ArcaneCyan,
              ) {
                Text(
                  text =
                    if (msg.isUser) (if (lang == AppLanguage.TH) "คุณ" else "You")
                    else "Gemini AI Coach",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (msg.isUser) HextechGold else ArcaneCyan,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = msg.text, style = MaterialTheme.typography.bodyMedium)
              }
            }

            when (geminiState) {
              GeminiUiState.Loading -> {
                item {
                  HextechCard(accentColor = ArcaneCyan) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
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
                          if (lang == AppLanguage.TH) {
                            "กำลังวิเคราะห์แมตช์อัปกับ Gemini AI Coach..."
                          } else {
                            "Analyzing matchup with Gemini AI Coach..."
                          },
                        style = MaterialTheme.typography.bodySmall,
                        color = ArcaneCyan,
                      )
                    }
                  }
                }
              }
              is GeminiUiState.Streaming -> {
                item {
                  HextechCard(accentColor = ArcaneCyan) {
                    Text(
                      text = "Gemini AI Coach...",
                      style = MaterialTheme.typography.labelMedium,
                      color = ArcaneCyan,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = geminiState.text, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
              is GeminiUiState.Error -> {
                item {
                  HextechCard(accentColor = ThreatCrimson) {
                    Text(
                      text = geminiState.message,
                      style = MaterialTheme.typography.bodySmall,
                      color = ThreatCrimson,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      FilterChip(
                        selected = false,
                        onClick = onRetryGemini,
                        modifier = Modifier.testTag("coach_retry_gemini_button"),
                        label = { Text(if (lang == AppLanguage.TH) "ลองใหม่" else "Retry") },
                      )
                      FilterChip(
                        selected = false,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("coach_fallback_offline_button"),
                        label = { Text(strings.offlinePlaybookTab) },
                      )
                    }
                  }
                }
              }
              else -> Unit
            }
          }

          // Input Bar
          Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (messages.isNotEmpty()) {
                IconButton(
                  onClick = onClearChat,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Default.DeleteSweep, contentDescription = "Clear chat")
                }
              }
              OutlinedTextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                modifier = Modifier.weight(1f).testTag("coach_question_input"),
                placeholder = { Text(strings.askGeminiHint) },
                maxLines = 3,
              )
              Spacer(modifier = Modifier.width(8.dp))
              IconButton(
                onClick = {
                  val q = questionInput
                  questionInput = ""
                  onAskGemini(q)
                },
                enabled =
                  questionInput.isNotBlank() &&
                    geminiState !is GeminiUiState.Loading &&
                    geminiState !is GeminiUiState.Streaming,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("coach_send_button"),
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.Send,
                  contentDescription = strings.sendPrompt,
                  tint = ArcaneCyan,
                )
              }
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
