package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiProviderId
import com.example.data.ai.CoachErrorKind
import com.example.data.ai.CoachSpeechState
import com.example.data.local.UserPreferences
import com.example.data.model.AppDarkThemePreset
import com.example.data.model.AppFontPreset
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.IconSizePreset
import com.example.data.model.Lane
import com.example.ui.components.AiEngineControlStrip
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.ChampionPickerBottomSheet
import com.example.ui.components.CoachAppearanceQuickSheet
import com.example.ui.components.CoachFormattedAdviceBody
import com.example.ui.components.FreeApiKeysBottomSheet
import com.example.ui.components.ModelPickerBottomSheet
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.LocalIconSizePreset
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalRiftPalette
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.RiftThemePalette
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.CoachChatMessage
import com.example.ui.viewmodel.CoachPromptCategory
import com.example.ui.viewmodel.CoachUiState
import com.example.ui.viewmodel.GeminiUiState
import com.example.ui.viewmodel.LiveCoachUiState
import com.example.ui.viewmodel.ProviderKeyTestState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(
  state: CoachUiState,
  geminiState: GeminiUiState,
  messages: List<CoachChatMessage>,
  speechState: CoachSpeechState,
  liveState: LiveCoachUiState,
  isQuickCaptureActive: Boolean,
  preferences: UserPreferences = UserPreferences(),
  keyTestStates: Map<AiProviderId, ProviderKeyTestState> = emptyMap(),
  openLiveTabRequest: Boolean = false,
  onConsumeOpenLiveTabRequest: () -> Unit = {},
  onSelectChampion: (ChampionData) -> Unit,
  onSelectLane: (Lane) -> Unit,
  onSelectThemePreset: (AppDarkThemePreset) -> Unit = {},
  onSelectFontPreset: (AppFontPreset) -> Unit = {},
  onChangeFontScale: (Float) -> Unit = {},
  onSelectIconSizePreset: (IconSizePreset) -> Unit = {},
  onToggleHighContrast: (Boolean) -> Unit = {},
  onSelectCoachModel: (String) -> Unit = {},
  onSelectLiveModel: (String) -> Unit = {},
  onSaveAndTestApiKey: (AiProviderId, String) -> Unit = { _, _ -> },
  onClearApiKey: (AiProviderId) -> Unit = {},
  onClearAiCache: () -> Unit = {},
  onAskGemini: (String) -> Unit,
  onRegenerateQuestion: (String) -> Unit = {},
  onSwitchModelAndRetry: (String) -> Unit = {},
  onRetryGemini: () -> Unit,
  onClearChat: () -> Unit,
  onToggleSpeak: (id: String, text: String) -> Unit,
  onClearSpeechError: () -> Unit,
  onLiveModeVisible: (Boolean) -> Unit,
  onStartLiveCountdown: (() -> Unit) -> Unit,
  onCancelLiveCountdown: () -> Unit,
  onPickLiveUri: (Uri) -> Unit,
  onToggleAutoSpeakLive: () -> Unit,
  onSpeakLiveBriefing: () -> Unit,
  onLivePermissionDeclined: (String) -> Unit,
  onOpenAnalyzer: () -> Unit,
  onOpenDraft: () -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
) {
  val strings = LocalUiStrings.current
  val palette = LocalRiftPalette.current
  val isMiniMode = LocalInGameMiniMode.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val champAvatarSize = if (compactIcons) 22.dp else 28.dp
  val topIconSize = if (compactIcons) 15.dp else 19.dp
  val headerIconSize = if (compactIcons) 18.dp else 22.dp
  val highContrast = preferences.highContrastCards
  val lang = state.language
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedPromptCategory by remember { mutableStateOf(CoachPromptCategory.ALL) }
  var showChampPicker by remember { mutableStateOf(false) }
  var showModelPicker by remember { mutableStateOf(false) }
  var showFreeKeysSheet by remember { mutableStateOf(false) }
  var showCacheInfoDialog by remember { mutableStateOf(false) }
  var showAppearanceSheet by remember { mutableStateOf(false) }
  var laneMenuExpanded by remember { mutableStateOf(false) }
  var questionInput by remember { mutableStateOf("") }
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(openLiveTabRequest) {
    if (openLiveTabRequest) {
      selectedTab = 2
      onConsumeOpenLiveTabRequest()
    }
  }

  LaunchedEffect(selectedTab) { onLiveModeVisible(selectedTab == 2) }
  DisposableEffect(Unit) { onDispose { onLiveModeVisible(false) } }

  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    val isCompactHeight = isMiniMode || maxHeight < 520.dp
    val showSegmentIcons = maxWidth >= 430.dp
    val outerPad = if (isCompactHeight) 6.dp else 10.dp

    Column(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 780.dp)
          .padding(horizontal = outerPad, vertical = if (isCompactHeight) 4.dp else 8.dp),
      verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 5.dp else 8.dp),
    ) {
      // Ultra-compact single-row secondary control strip + Quick Appearance Customizer
      Surface(
        shape = MaterialTheme.shapes.small,
        color = palette.surfaceNavy,
        border =
          BorderStroke(
            width = if (highContrast) 1.4.dp else 1.dp,
            color = if (highContrast) palette.outlineHighContrast else palette.outlineVariant,
          ),
        modifier = Modifier.fillMaxWidth().testTag("coach_compact_topbar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val selectedChamp = state.selectedChampion
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.8f)),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = selectedChamp != null) { showChampPicker = true }
                .testTag("coach_select_champion"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              if (selectedChamp != null) {
                ChampionCrestAvatar(champion = selectedChamp, size = champAvatarSize)
                Text(
                  text = selectedChamp.displayName(lang),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
              } else {
                Text(
                  text = strings.selectYourChampion,
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.textPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
              }
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = strings.selectYourChampion,
                tint = palette.textSecondary,
                modifier = Modifier.size(topIconSize),
              )
            }
          }

          Box {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = palette.surface,
              border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.8f)),
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { laneMenuExpanded = true }
                  .testTag("coach_select_lane_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (lang == AppLanguage.TH) state.selectedLane.labelTh else state.selectedLane.labelEn,
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.textPrimary,
                  maxLines = 1,
                )
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = strings.selectLane,
                  tint = palette.textSecondary,
                  modifier = Modifier.size(topIconSize),
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
                      color = if (isSelectedLane) palette.primaryBright else palette.textPrimary,
                      fontWeight = if (isSelectedLane) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                  trailingIcon =
                    if (isSelectedLane) {
                      {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = null,
                          tint = palette.primaryBright,
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

          // 3-Way Segmented Mode Toggle (Playbook | AI | Live)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.surfaceHighest,
            border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.75f)),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CompactModeSegment(
                selected = selectedTab == 0,
                label = if (lang == AppLanguage.TH) "คู่มือ" else "Guide",
                showIcon = showSegmentIcons,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconSize = topIconSize,
                accentColor = palette.primary,
                contentDescription = strings.offlinePlaybookTab,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("coach_offline_tab"),
              )
              CompactModeSegment(
                selected = selectedTab == 1,
                label = "AI",
                showIcon = showSegmentIcons,
                icon = Icons.Default.AutoAwesome,
                iconSize = topIconSize,
                accentColor = palette.secondary,
                contentDescription = strings.geminiCoachTab,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("coach_gemini_tab"),
              )
              CompactModeSegment(
                selected = selectedTab == 2,
                label = if (lang == AppLanguage.TH) "สด" else "Live",
                showIcon = showSegmentIcons,
                icon = Icons.Default.Visibility,
                iconSize = topIconSize,
                accentColor = AdvantageEmerald,
                contentDescription = if (lang == AppLanguage.TH) "โหมด Live Coach" else "Live Coach Mode",
                onClick = { selectedTab = 2 },
                modifier = Modifier.testTag("coach_live_tab"),
              )
            }
          }

          // In-Screen Quick Appearance Customizer Button (Synced with Settings)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.primaryContainer.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.75f)),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showAppearanceSheet = true }
                .testTag("coach_quick_appearance_button"),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Palette,
                contentDescription =
                  if (lang == AppLanguage.TH) "ปรับสี ฟอนต์ และขนาด AI Coach"
                  else "Customize AI Coach appearance",
                tint = palette.primaryBright,
                modifier = Modifier.size(topIconSize),
              )
            }
          }
        }
      }

      // Live Matchup Context Summary Bar (Active Champion + Lane + Enemy Threats + Journal Memory)
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.surface.copy(alpha = 0.88f),
        border =
          BorderStroke(
            width = if (highContrast) 1.35.dp else 1.dp,
            color =
              if (highContrast) palette.outlineHighContrast.copy(alpha = 0.85f)
              else palette.outlineVariant,
          ),
        modifier = Modifier.fillMaxWidth().testTag("coach_matchup_summary_bar"),
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = if (isCompactHeight) 4.dp else 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = if (lang == AppLanguage.TH) "บริบท:" else "Context:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.primaryBright,
            maxLines = 1,
          )
          if (state.enemyContext.isNotEmpty()) {
            Text(
              text = "vs",
              style = MaterialTheme.typography.labelSmall,
              color = ThreatCrimson,
              fontWeight = FontWeight.Bold,
            )
            LazyRow(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              itemsIndexed(state.enemyContext, key = { _, e -> e.id }) { _, enemy ->
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = ThreatCrimson.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.55f)),
                  modifier =
                    Modifier.clip(RoundedCornerShape(999.dp))
                      .clickable { onOpenChampionDetail(enemy.id) },
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    ChampionCrestAvatar(
                      champion = enemy,
                      size = if (compactIcons) 16.dp else 20.dp,
                    )
                    Text(
                      text = enemy.displayName(lang),
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textPrimary,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          } else {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = palette.secondary.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.45f)),
              modifier =
                Modifier.weight(1f, fill = false)
                  .clip(RoundedCornerShape(999.dp))
                  .clickable(onClick = onOpenAnalyzer)
                  .testTag("coach_context_add_enemies"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = palette.secondary,
                  modifier = Modifier.size(if (compactIcons) 12.dp else 15.dp),
                )
                Text(
                  text =
                    if (lang == AppLanguage.TH) "ยังไม่ระบุศัตรู • แตะเลือกใน 1v5"
                    else "No enemies set • Pick in 1v5",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.secondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Spacer(modifier = Modifier.weight(1f))
          }

          if (state.journalGameCount > 0) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = AdvantageEmerald.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.5f)),
              modifier = Modifier.testTag("coach_journal_memory_badge"),
            ) {
              Text(
                text =
                  if (lang == AppLanguage.TH) "จำ ${state.journalGameCount} เกม"
                  else "${state.journalGameCount} matches",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AdvantageEmerald,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      // AI Model Switcher, Cache Counter & Free API Keys Bar (visible on AI & Live tabs)
      if (selectedTab == 1 || selectedTab == 2) {
        AiEngineControlStrip(
          selectedModelId =
            if (selectedTab == 2) state.selectedLiveModelId else state.selectedCoachModelId,
          keyStatus = state.keyStatus,
          cacheStats = state.cacheStats,
          cooldownModelIds = state.cooldownModelIds,
          forVision = selectedTab == 2,
          language = lang,
          onOpenModelPicker = { showModelPicker = true },
          onOpenApiKeys = { showFreeKeysSheet = true },
          onOpenCacheInfo = { showCacheInfoDialog = true },
        )
      }

      // Optional TTS error banner (only shown if device TTS fails)
      speechState.errorMessage?.let { speechErr ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = WarningAmber.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth().testTag("coach_tts_error_banner"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = speechErr,
              style = MaterialTheme.typography.labelSmall,
              color = WarningAmber,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = onClearSpeechError,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss speech error",
                tint = WarningAmber,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      // HERO SPOTLIGHT ADVICE VIEWPORT — 100% synced with active dark theme, font & icon density
      val spotlightAccent =
        when (selectedTab) {
          0 -> palette.primary
          1 -> palette.secondary
          else -> AdvantageEmerald
        }
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = palette.background,
        tonalElevation = 4.dp,
        border =
          BorderStroke(
            width = if (highContrast) 1.8.dp else 1.4.dp,
            color =
              if (highContrast) palette.outlineHighContrast
              else spotlightAccent.copy(alpha = 0.72f),
          ),
        modifier = Modifier.weight(1f).fillMaxWidth().testTag("coach_advice_spotlight"),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    spotlightAccent.copy(alpha = 0.14f),
                    palette.heroTopTint.copy(alpha = 0.55f),
                    palette.background,
                  )
                )
              )
        ) {
          when (selectedTab) {
            0 -> {
              val playbookListState = rememberLazyListState()
              val playbookSections = state.offlinePlaybook

              LazyColumn(
                state = playbookListState,
                modifier = Modifier.fillMaxSize().testTag("coach_playbook_list"),
                contentPadding = PaddingValues(if (isCompactHeight) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp),
              ) {
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
                          tint = palette.primaryBright,
                          modifier = Modifier.size(headerIconSize),
                        )
                        Text(
                          text =
                            if (lang == AppLanguage.TH) "คู่มือและคำแนะนำการเล่น"
                            else "Tactical Playbook & Advice",
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = palette.primaryBright,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.5f)),
                      ) {
                        Text(
                          text = "Patch ${state.activePatchVersion}",
                          style = MaterialTheme.typography.labelSmall,
                          color = palette.primaryBright,
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
                          val chipColor = phaseAccentColor(idx, palette)
                          Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = chipColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, chipColor.copy(alpha = 0.5f)),
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
                  val cardId = "playbook_$index"
                  val phaseAccent = phaseAccentColor(index, palette)
                  val phaseBadge =
                    when (index) {
                      0 -> if (lang == AppLanguage.TH) "EARLY • ต้นเกม" else "EARLY PHASE"
                      1 -> if (lang == AppLanguage.TH) "MID • กลางเกม" else "MID PHASE"
                      else -> if (lang == AppLanguage.TH) "LATE • ท้ายเกม" else "LATE PHASE"
                    }
                  val titleText = section.title(lang)
                  val bodyText = section.body(lang)

                  AdviceSpotlightCard(
                    cardId = cardId,
                    accentColor = phaseAccent,
                    badgeText = phaseBadge,
                    title = titleText,
                    body = bodyText,
                    compact = isCompactHeight,
                    language = lang,
                    isSpeaking = speechState.isSpeaking && speechState.activeMessageId == cardId,
                    onToggleSpeak = { onToggleSpeak(cardId, "$titleText. $bodyText") },
                  )
                }
              }
            }
            1 -> {
              val chatListState = rememberLazyListState()
              val isBusy =
                geminiState is GeminiUiState.Loading || geminiState is GeminiUiState.Streaming
              val visiblePrompts =
                remember(state.quickPrompts, selectedPromptCategory) {
                  if (selectedPromptCategory == CoachPromptCategory.ALL) {
                    state.quickPrompts
                  } else {
                    state.quickPrompts.filter { it.category == selectedPromptCategory }
                  }
                }

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
                item(key = "prompt_categories_row") {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(CoachPromptCategory.entries) { _, cat ->
                      val selectedCat = selectedPromptCategory == cat
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color =
                          if (selectedCat) palette.primary.copy(alpha = 0.22f)
                          else palette.surfaceHigh,
                        border =
                          BorderStroke(
                            1.dp,
                            if (selectedCat) palette.primaryBright
                            else palette.outline.copy(alpha = 0.65f),
                          ),
                        modifier =
                          Modifier.clip(RoundedCornerShape(999.dp))
                            .clickable { selectedPromptCategory = cat }
                            .testTag("coach_prompt_cat_${cat.name}"),
                      ) {
                        Text(
                          text = if (lang == AppLanguage.TH) cat.labelTh else cat.labelEn,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = if (selectedCat) FontWeight.Bold else FontWeight.Medium,
                          color = if (selectedCat) palette.primaryBright else palette.textSecondary,
                          modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }

                item(key = "quick_prompts_row") {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(visiblePrompts, key = { _, p -> p.id }) { idx, prompt ->
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.secondary.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.55f)),
                        modifier =
                          Modifier.clip(RoundedCornerShape(999.dp))
                            .clickable(enabled = !isBusy) { onAskGemini(prompt.fullQuestion) }
                            .testTag("coach_quick_prompt_$idx"),
                      ) {
                        Text(
                          text = prompt.shortLabel,
                          style = MaterialTheme.typography.labelMedium,
                          color = if (isBusy) palette.textMuted else palette.secondary,
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
                      cardId = "gemini_welcome",
                      accentColor = palette.secondary,
                      badgeText = "WR-AI COACH • PATCH ${state.activePatchVersion}",
                      title =
                        if (lang == AppLanguage.TH) "พร้อมวิเคราะห์แมตช์อัปให้คุณทันที"
                        else "Ready for Live Matchup Coaching",
                      body =
                        if (lang == AppLanguage.TH) {
                          val champName = state.selectedChampion?.displayName(lang) ?: ""
                          val laneLabel = state.selectedLane.labelTh
                          "กดปุ่มคำถามกลยุทธ์ด้านบนเพื่อถามทันทีในคลิกเดียว หรือพิมพ์ถามแผนเล่น $champName ($laneLabel) ด้านล่าง"
                        } else {
                          val champName = state.selectedChampion?.displayName(lang) ?: ""
                          val laneLabel = state.selectedLane.labelEn
                          "Tap any categorized strategy chip above to ask in 1 tap, or ask a custom question for $champName ($laneLabel)."
                        },
                      compact = isCompactHeight,
                      language = lang,
                      isSpeaking = false,
                    )
                  }
                }

                itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                  if (msg.isUser) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primaryContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.65f)),
                        modifier = Modifier.widthIn(max = 300.dp),
                      ) {
                        val userMsgStyle = MaterialTheme.typography.bodyMedium
                        Text(
                          text = msg.text,
                          style = userMsgStyle.copy(lineHeight = userMsgStyle.fontSize * 1.42f),
                          color = palette.primaryBright,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                      }
                    }
                  } else {
                    val activeModelBadge =
                      msg.modelLabel.ifBlank { "WR-AI COACH" } +
                        if (msg.isFallbackUsed) " (Fallback)" else ""
                    AdviceSpotlightCard(
                      cardId = msg.id,
                      accentColor = palette.secondary,
                      badgeText = activeModelBadge,
                      cachedBadgeText =
                        if (msg.fromCache) {
                          if (lang == AppLanguage.TH) "แคช • 0 โทเคน" else "Cached • 0 Tokens"
                        } else null,
                      title = null,
                      body = msg.text,
                      compact = isCompactHeight,
                      language = lang,
                      isSpeaking = speechState.isSpeaking && speechState.activeMessageId == msg.id,
                      onToggleSpeak = { onToggleSpeak(msg.id, msg.text) },
                      onCopy = { clipboardManager.setText(AnnotatedString(msg.text)) },
                      onRefresh =
                        if (!isBusy && msg.questionText.isNotBlank()) {
                          { onRegenerateQuestion(msg.questionText) }
                        } else null,
                    )
                  }
                }

                when (geminiState) {
                  is GeminiUiState.Loading -> {
                    item(key = "gemini_loading") {
                      Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = palette.surfaceHigh,
                        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                          CircularProgressIndicator(
                            color = palette.secondary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                          )
                          val modelHint = geminiState.modelLabel.takeIf { it.isNotBlank() } ?: "WR-AI Coach"
                          Text(
                            text =
                              if (lang == AppLanguage.TH) "กำลังวิเคราะห์คำแนะนำ ($modelHint)..."
                              else "Analyzing matchup with $modelHint...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondary,
                          )
                        }
                      }
                    }
                  }
                  is GeminiUiState.Streaming -> {
                    item(key = "gemini_streaming") {
                      val modelHint = geminiState.modelLabel.takeIf { it.isNotBlank() } ?: "AI"
                      AdviceSpotlightCard(
                        cardId = "gemini_streaming",
                        accentColor = palette.secondary,
                        badgeText =
                          if (lang == AppLanguage.TH) "กำลังตอบ ($modelHint)..."
                          else "STREAMING ($modelHint)...",
                        title = null,
                        body = geminiState.text,
                        compact = isCompactHeight,
                        language = lang,
                        isSpeaking = false,
                      )
                    }
                  }
                  is GeminiUiState.Error -> {
                    item(key = "gemini_error") {
                      Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = ThreatCrimson.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth().testTag("coach_error_card"),
                      ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                          Text(
                            text = geminiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThreatCrimson,
                          )
                          Spacer(modifier = Modifier.height(8.dp))
                          FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            geminiState.suggestedFallbackModelId?.let { fallbackId ->
                              val fallbackSpec = AiModelCatalog.findById(fallbackId)
                              Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.secondary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, palette.secondary),
                                modifier =
                                  Modifier.minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSwitchModelAndRetry(fallbackId) }
                                    .testTag("coach_switch_fallback_button"),
                              ) {
                                Text(
                                  text =
                                    if (lang == AppLanguage.TH) "สลับไป ${fallbackSpec.shortBadge}"
                                    else "Switch to ${fallbackSpec.shortBadge}",
                                  style = MaterialTheme.typography.labelLarge,
                                  fontWeight = FontWeight.Bold,
                                  color = palette.secondary,
                                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                              }
                            }
                            Surface(
                              shape = RoundedCornerShape(8.dp),
                              color = palette.primary.copy(alpha = 0.16f),
                              border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.8f)),
                              modifier =
                                Modifier.minimumInteractiveComponentSize()
                                  .clip(RoundedCornerShape(8.dp))
                                  .clickable {
                                    if (geminiState.errorKind == CoachErrorKind.INVALID_KEY) {
                                      showFreeKeysSheet = true
                                    } else {
                                      showModelPicker = true
                                    }
                                  }
                                  .testTag("coach_error_open_keys_button"),
                            ) {
                              Text(
                                text =
                                  if (geminiState.errorKind == CoachErrorKind.INVALID_KEY) {
                                    if (lang == AppLanguage.TH) "แก้ Key ฟรี" else "Edit Key"
                                  } else {
                                    if (lang == AppLanguage.TH) "เปลี่ยนโมเดล / ใส่ Key" else "Models / Keys"
                                  },
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryBright,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                              )
                            }
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
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                              )
                            }
                            Surface(
                              shape = RoundedCornerShape(8.dp),
                              color = palette.primary.copy(alpha = 0.18f),
                              border = BorderStroke(1.dp, palette.primary),
                              modifier =
                                Modifier.minimumInteractiveComponentSize()
                                  .clip(RoundedCornerShape(8.dp))
                                  .clickable { selectedTab = 0 }
                                  .testTag("coach_fallback_offline_button"),
                            ) {
                              Text(
                                text = strings.offlinePlaybookTab,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primaryBright,
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
            else -> {
              CoachLivePanel(
                liveState = liveState,
                speechState = speechState,
                isQuickCaptureActive = isQuickCaptureActive,
                language = lang,
                isCompactHeight = isCompactHeight,
                onStartCountdownCapture = onStartLiveCountdown,
                onCancelCountdown = onCancelLiveCountdown,
                onPickLiveUri = onPickLiveUri,
                onToggleAutoSpeak = onToggleAutoSpeakLive,
                onSpeakBriefing = onSpeakLiveBriefing,
                onCapturePermissionDeclined = onLivePermissionDeclined,
                onOpenAnalyzer = onOpenAnalyzer,
                onOpenDraft = onOpenDraft,
                onOpenChampionDetail = onOpenChampionDetail,
              )
            }
          }
        }
      }

      // Slim Bottom Input Dock (only shown on AI Coach tab)
      if (selectedTab == 1) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = palette.surfaceNavy,
          border =
            BorderStroke(
              width = if (highContrast) 1.4.dp else 1.dp,
              color = if (highContrast) palette.outlineHighContrast else palette.secondary.copy(alpha = 0.45f),
            ),
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
                  tint = palette.textSecondary,
                  modifier = Modifier.size(topIconSize),
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
                  focusedBorderColor = palette.secondary,
                  unfocusedBorderColor = palette.outline.copy(alpha = 0.65f),
                  focusedTextColor = palette.textPrimary,
                  unfocusedTextColor = palette.textPrimary,
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
                tint = if (canSend) palette.secondary else palette.textMuted,
                modifier = Modifier.size(headerIconSize),
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

  if (showModelPicker) {
    val forVision = selectedTab == 2
    ModelPickerBottomSheet(
      selectedModelId =
        if (forVision) state.selectedLiveModelId else state.selectedCoachModelId,
      keyStatus = state.keyStatus,
      cooldownModelIds = state.cooldownModelIds,
      forVision = forVision,
      language = lang,
      onSelectModel = { modelId ->
        if (forVision) onSelectLiveModel(modelId) else onSelectCoachModel(modelId)
      },
      onOpenApiKeys = { showFreeKeysSheet = true },
      onDismiss = { showModelPicker = false },
    )
  }

  if (showFreeKeysSheet) {
    FreeApiKeysBottomSheet(
      keyStatus = state.keyStatus,
      keyTestStates = keyTestStates,
      language = lang,
      onSaveAndTestKey = onSaveAndTestApiKey,
      onClearKey = onClearApiKey,
      onDismiss = { showFreeKeysSheet = false },
    )
  }

  if (showAppearanceSheet) {
    CoachAppearanceQuickSheet(
      preferences = preferences,
      onSelectThemePreset = onSelectThemePreset,
      onSelectFontPreset = onSelectFontPreset,
      onChangeFontScale = onChangeFontScale,
      onSelectIconSizePreset = onSelectIconSizePreset,
      onToggleHighContrast = onToggleHighContrast,
      onDismiss = { showAppearanceSheet = false },
    )
  }

  if (showCacheInfoDialog) {
    AlertDialog(
      onDismissRequest = { showCacheInfoDialog = false },
      containerColor = palette.surfaceNavy,
      titleContentColor = palette.primaryBright,
      textContentColor = palette.textPrimary,
      title = {
        Text(
          text =
            if (lang == AppLanguage.TH) "แคชคำตอบประหยัดโทเคน"
            else "Token-Saving Response Cache",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text =
            if (lang == AppLanguage.TH) {
              "คำตอบที่บันทึกในเครื่อง: ${state.cacheStats.entryCount} รายการ\n" +
                "ช่วยประหยัดการเรียก AI ซ้ำ: ${state.cacheStats.totalHitsSaved} ครั้ง (0 โทเคน)\n\n" +
                "เมื่อถามคำถามเดิมในแชมเปี้ยน/เลน/ศัตรู/แพตช์เดิมซ้ำ ระบบจะตอบทันทีโดยไม่เปลืองโควตา"
            } else {
              "Cached answers on device: ${state.cacheStats.entryCount} entries\n" +
                "Repeated hits served free: ${state.cacheStats.totalHitsSaved}x (0 tokens)\n\n" +
                "Repeated questions for the same champion, lane, enemies, and patch load instantly without spending quota."
            },
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TextButton(onClick = { showCacheInfoDialog = false }) {
          Text(if (lang == AppLanguage.TH) "ปิด" else "Close", color = palette.secondary)
        }
      },
      dismissButton =
        if (state.cacheStats.entryCount > 0) {
          {
            TextButton(
              onClick = {
                onClearAiCache()
                showCacheInfoDialog = false
              },
              modifier = Modifier.testTag("coach_clear_cache_confirm_button"),
            ) {
              Text(if (lang == AppLanguage.TH) "ล้างแคชทั้งหมด" else "Clear Cache", color = ThreatCrimson)
            }
          }
        } else null,
    )
  }
}

@Composable
private fun CompactModeSegment(
  selected: Boolean,
  label: String,
  showIcon: Boolean,
  icon: ImageVector,
  iconSize: androidx.compose.ui.unit.Dp,
  accentColor: Color,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalRiftPalette.current
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
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      if (showIcon) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          tint = if (selected) accentColor else palette.textSecondary,
          modifier = Modifier.size(iconSize),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) accentColor else palette.textSecondary,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun AdviceSpotlightCard(
  cardId: String,
  accentColor: Color,
  badgeText: String,
  cachedBadgeText: String? = null,
  title: String?,
  body: String,
  compact: Boolean,
  language: AppLanguage,
  isSpeaking: Boolean,
  onToggleSpeak: (() -> Unit)? = null,
  onCopy: (() -> Unit)? = null,
  onRefresh: (() -> Unit)? = null,
) {
  val palette = LocalRiftPalette.current
  val prefs = LocalUserPrefs.current
  val compactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
  val actionIconSize = if (compactIcons) 16.dp else 21.dp
  val speakIconSize = if (compactIcons) 18.dp else 22.dp
  val effectiveBorder =
    when {
      isSpeaking -> palette.primaryBright.copy(alpha = 0.95f)
      prefs.highContrastCards -> palette.outlineHighContrast
      else -> accentColor.copy(alpha = 0.68f)
    }
  val borderWidth =
    when {
      isSpeaking || prefs.highContrastCards -> 1.7.dp
      else -> 1.2.dp
    }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = palette.surfaceHigh,
    tonalElevation = 3.dp,
    border = BorderStroke(borderWidth, effectiveBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      Box(
        modifier =
          Modifier.width(4.dp)
            .fillMaxHeight()
            .background(if (isSpeaking) palette.primaryBright else accentColor)
      )
      Column(
        modifier = Modifier.weight(1f).padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = accentColor.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
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
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (!cachedBadgeText.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = AdvantageEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("coach_cached_badge_$cardId"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AdvantageEmerald,
                    modifier = Modifier.size(if (compactIcons) 11.dp else 14.dp),
                  )
                  Text(
                    text = cachedBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdvantageEmerald,
                    maxLines = 1,
                  )
                }
              }
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRefresh != null) {
              IconButton(
                onClick = onRefresh,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("coach_refresh_button_$cardId"),
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription =
                    if (language == AppLanguage.TH) "ถาม AI สดใหม่ (ข้ามแคช)"
                    else "Ask fresh (bypass cache)",
                  tint = palette.textSecondary,
                  modifier = Modifier.size(actionIconSize),
                )
              }
            }
            if (onToggleSpeak != null) {
              IconButton(
                onClick = onToggleSpeak,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("coach_speak_button_$cardId"),
              ) {
                Icon(
                  imageVector =
                    if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription =
                    if (isSpeaking) {
                      if (language == AppLanguage.TH) "หยุดอ่านเสียง" else "Stop speaking"
                    } else {
                      if (language == AppLanguage.TH) "ฟังเสียงอ่านคำตอบ" else "Listen to advice"
                    },
                  tint = if (isSpeaking) palette.primaryBright else accentColor,
                  modifier = Modifier.size(speakIconSize),
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
                  tint = palette.textSecondary,
                  modifier = Modifier.size(actionIconSize),
                )
              }
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

        CoachFormattedAdviceBody(
          rawText = body,
          accentColor = accentColor,
          compact = compact,
        )
      }
    }
  }
}

private fun phaseAccentColor(index: Int, palette: RiftThemePalette): Color =
  when (index) {
    0 -> palette.primary
    1 -> palette.secondary
    else -> AdvantageEmerald
  }
