package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.domain.engine.ChampionComboSynergyEngine
import com.example.domain.engine.ComboDifficulty
import com.example.domain.engine.ComboSituation
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.HextechCard
import com.example.ui.components.TacticalMeterBar
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.LocalRiftPalette
import com.example.ui.theme.RiftThemePalette
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionGamePlanCard(champion: ChampionData, lang: AppLanguage) {
  val palette = LocalRiftPalette.current
  var selectedGamePlanTab by remember { mutableIntStateOf(0) }
  HextechCard(accentColor = palette.secondary) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (lang == AppLanguage.TH) "Game Plan • แผนการเดินเกม" else "Game Plan & Strategy",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag("detail_game_plan_title"),
      )
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = palette.secondary.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.55f)),
      ) {
        Text(
          text = if (lang == AppLanguage.TH) "3 ช่วงเกม" else "3 Phases",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.secondary,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    SecondaryTabRow(
      selectedTabIndex = selectedGamePlanTab,
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = palette.primaryBright,
      modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    ) {
      Tab(
        selected = selectedGamePlanTab == 0,
        onClick = { selectedGamePlanTab = 0 },
        modifier = Modifier.testTag("game_plan_tab_0"),
        text = {
          Text(
            text = if (lang == AppLanguage.TH) "ต้นเกม (Early)" else "Early Game",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selectedGamePlanTab == 0) FontWeight.Bold else FontWeight.Normal,
          )
        },
      )
      Tab(
        selected = selectedGamePlanTab == 1,
        onClick = { selectedGamePlanTab = 1 },
        modifier = Modifier.testTag("game_plan_tab_1"),
        text = {
          Text(
            text = if (lang == AppLanguage.TH) "กลางเกม (Mid)" else "Mid Game",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selectedGamePlanTab == 1) FontWeight.Bold else FontWeight.Normal,
          )
        },
      )
      Tab(
        selected = selectedGamePlanTab == 2,
        onClick = { selectedGamePlanTab = 2 },
        modifier = Modifier.testTag("game_plan_tab_2"),
        text = {
          Text(
            text = if (lang == AppLanguage.TH) "ท้ายเกม (Late)" else "Late Game",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selectedGamePlanTab == 2) FontWeight.Bold else FontWeight.Normal,
          )
        },
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (selectedGamePlanTab) {
      0 -> {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "ระดับพลังต้นเกม (Early Power)" else "Early Game Power",
          valuePercent = champion.earlyPower * 10,
          barColor = palette.secondary,
          trailingLabel = "${champion.earlyPower}/10",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = champion.earlyPlan(lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("game_plan_content_0"),
        )
      }
      1 -> {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "ระดับพลังกลางเกม (Mid Power)" else "Mid Game Power",
          valuePercent = champion.midPower * 10,
          barColor = palette.primary,
          trailingLabel = "${champion.midPower}/10",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = champion.midPlan(lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("game_plan_content_1"),
        )
      }
      else -> {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "ระดับพลังท้ายเกม (Late Power)" else "Late Game Power",
          valuePercent = champion.latePower * 10,
          barColor = AdvantageEmerald,
          trailingLabel = "${champion.latePower}/10",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = champion.latePlan(lang),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("game_plan_content_2"),
        )
      }
    }
  }
}

private fun situationColor(situation: ComboSituation, palette: RiftThemePalette): Color =
  when (situation) {
    ComboSituation.WAVE_CLEAR -> palette.secondary
    ComboSituation.LANE_TRADE -> SkillViolet
    ComboSituation.SOLO_KILL -> ThreatCrimson
    ComboSituation.TEAMFIGHT -> palette.primaryBright
    ComboSituation.ESCAPE_SURVIVAL -> AdvantageEmerald
  }

private fun keycapColor(token: String, palette: RiftThemePalette): Color {
  val norm = token.uppercase()
  return when {
    norm.startsWith("R") -> palette.primaryBright
    norm.contains("FLASH") || norm.contains("GHOST") -> WarningAmber
    norm == "AA" || norm == "P" -> palette.textSecondary
    norm.startsWith("W") -> palette.secondary
    norm.startsWith("E") -> AdvantageEmerald
    else -> palette.tertiary
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChampionCombosCard(champion: ChampionData, lang: AppLanguage) {
  val palette = LocalRiftPalette.current
  val allCombos =
    remember(champion.id) { ChampionComboSynergyEngine.getCombosForChampion(champion) }
  var selectedDifficulty by remember(champion.id) { mutableStateOf(ComboDifficulty.BASIC) }
  val visibleCombos =
    remember(allCombos, selectedDifficulty) {
      allCombos.filter { it.difficulty == selectedDifficulty }
    }

  HextechCard(
    accentColor = palette.primary,
    modifier = Modifier.testTag("detail_combos_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Default.Bolt,
          contentDescription = null,
          tint = palette.primaryBright,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text =
            if (lang == AppLanguage.TH) "คอมโบใช้งานจริง (Basic – Hard)"
            else "Combo Playbook (Basic – Hard)",
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
      }
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = palette.primary.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.55f)),
      ) {
        Text(
          text = if (lang == AppLanguage.TH) "5 สถานการณ์" else "5 Situations",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.primaryBright,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    SecondaryTabRow(
      selectedTabIndex = selectedDifficulty.ordinal,
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = palette.primaryBright,
      modifier = Modifier.clip(RoundedCornerShape(8.dp)),
    ) {
      ComboDifficulty.entries.forEach { diff ->
        val selected = selectedDifficulty == diff
        Tab(
          selected = selected,
          onClick = { selectedDifficulty = diff },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("combo_tab_${diff.name}"),
          text = {
            Text(
              text = if (lang == AppLanguage.TH) diff.labelTh else diff.labelEn,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      visibleCombos.forEach { entry ->
        val sitColor = situationColor(entry.situation, palette)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.surfaceNavy,
          border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.85f)),
          modifier = Modifier.fillMaxWidth().testTag("combo_entry_${entry.id}"),
        ) {
          Column(modifier = Modifier.padding(11.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (lang == AppLanguage.TH) entry.titleTh else entry.titleEn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = sitColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, sitColor.copy(alpha = 0.6f)),
              ) {
                Text(
                  text =
                    if (lang == AppLanguage.TH) entry.situation.labelTh
                    else entry.situation.labelEn,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = sitColor,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Skill Keycaps Sequence (Q-W-E-R-Flash-AA)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              entry.sequence.forEachIndexed { idx, token ->
                val badgeColor = keycapColor(token, palette)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = badgeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.1.dp, badgeColor.copy(alpha = 0.78f)),
                  ) {
                    Text(
                      text = token,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = badgeColor,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                  if (idx < entry.sequence.lastIndex) {
                    Text(
                      text = " → ",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = palette.textMuted,
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = if (lang == AppLanguage.TH) entry.summaryTh else entry.summaryEn,
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = palette.secondaryContainer.copy(alpha = 0.45f),
              border = BorderStroke(0.9.dp, palette.secondary.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Timer,
                  contentDescription = null,
                  tint = palette.secondary,
                  modifier = Modifier.size(15.dp).padding(top = 1.dp),
                )
                Text(
                  text =
                    if (lang == AppLanguage.TH) "ทริคจังหวะกด: ${entry.timingTipTh}"
                    else "Timing & Practice Tip: ${entry.timingTipEn}",
                  style = MaterialTheme.typography.labelSmall,
                  color = palette.onSecondaryContainer,
                  modifier = Modifier.weight(1f),
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
fun ChampionTeamSynergyCard(
  champion: ChampionData,
  allChampions: List<ChampionData>,
  lang: AppLanguage,
  onOpenChampionDetail: (String) -> Unit,
) {
  val palette = LocalRiftPalette.current
  val partners =
    remember(champion.id, allChampions) {
      ChampionComboSynergyEngine.getTeamSynergies(champion, allChampions, limit = 5)
    }

  if (partners.isEmpty()) return

  HextechCard(
    accentColor = palette.secondary,
    modifier = Modifier.testTag("detail_synergy_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Default.Groups,
          contentDescription = null,
          tint = palette.secondary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text =
            if (lang == AppLanguage.TH) "คู่แชมเปี้ยนเข้าขา & คอมโบร่วมทีม"
            else "Best Teammates & Team Combos",
          style = MaterialTheme.typography.titleMedium,
          color = palette.textPrimary,
        )
      }
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = palette.secondary.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.55f)),
      ) {
        Text(
          text = "${partners.size} Duo",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.secondary,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "แตะที่ตัวละครเพื่อเปิดหน้าโปรไฟล์และดูคอมโบคู่กัน"
        else "Tap any partner to inspect their full profile & combos.",
      style = MaterialTheme.typography.bodySmall,
      color = palette.textMuted,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      partners.forEach { partner ->
        val c = partner.champion
        val laneText =
          c.lanes.firstOrNull()?.let { if (lang == AppLanguage.TH) it.labelTh else it.labelEn } ?: ""
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = palette.surfaceNavy,
          border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.85f)),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onOpenChampionDetail(c.id) }
              .testTag("detail_synergy_${c.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ChampionCrestAvatar(champion = c, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = c.displayName(lang),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.textPrimary,
                )
                if (laneText.isNotBlank()) {
                  Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = palette.surfaceHigh,
                    border = BorderStroke(0.75.dp, palette.outline),
                  ) {
                    Text(
                      text = laneText,
                      style = MaterialTheme.typography.labelSmall,
                      color = palette.textSecondary,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                  }
                }
                Surface(
                  shape = RoundedCornerShape(5.dp),
                  color = AdvantageEmerald.copy(alpha = 0.16f),
                  border = BorderStroke(0.75.dp, AdvantageEmerald.copy(alpha = 0.55f)),
                ) {
                  Text(
                    text = "${partner.synergyScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AdvantageEmerald,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) "${partner.tagTh} • ${partner.comboSequenceLabel}"
                  else "${partner.tagEn} • ${partner.comboSequenceLabel}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryBright,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (lang == AppLanguage.TH) partner.reasonTh else partner.reasonEn,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = c.displayName(lang),
              tint = palette.textMuted,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChampionCountersCard(
  champion: ChampionData,
  champMap: Map<String, ChampionData>,
  lang: AppLanguage,
  onOpenChampionDetail: (String) -> Unit,
) {
  HextechCard {
    Text(
      text =
        if (lang == AppLanguage.TH) "ตัวแก้ทางและตัวที่ชนะทาง"
        else "Counters & Favorable Matchups",
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (lang == AppLanguage.TH) "แพ้ทาง:" else "Countered By:",
      style = MaterialTheme.typography.labelMedium,
      color = ThreatCrimson,
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      champion.counterChampionIds.mapNotNull { champMap[it] }.forEach { c ->
        Surface(
          color = ThreatCrimson.copy(alpha = 0.14f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(0.9.dp, ThreatCrimson.copy(alpha = 0.5f)),
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(MaterialTheme.shapes.small)
              .clickable { onOpenChampionDetail(c.id) }
              .testTag("detail_counter_${c.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            ChampionCrestAvatar(champion = c, size = 20.dp)
            Text(
              text = c.displayName(lang),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = if (lang == AppLanguage.TH) "ชนะทาง:" else "Strong Against:",
      style = MaterialTheme.typography.labelMedium,
      color = AdvantageEmerald,
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      champion.favorableAgainstIds.mapNotNull { champMap[it] }.forEach { c ->
        Surface(
          color = AdvantageEmerald.copy(alpha = 0.14f),
          shape = MaterialTheme.shapes.small,
          border = BorderStroke(0.9.dp, AdvantageEmerald.copy(alpha = 0.5f)),
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(MaterialTheme.shapes.small)
              .clickable { onOpenChampionDetail(c.id) }
              .testTag("detail_favorable_${c.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            ChampionCrestAvatar(champion = c, size = 20.dp)
            Text(
              text = c.displayName(lang),
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }
    }
  }
}

