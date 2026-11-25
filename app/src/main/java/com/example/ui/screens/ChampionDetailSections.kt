package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.HextechCard
import com.example.ui.components.TacticalMeterBar
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.ThreatCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionGamePlanCard(champion: ChampionData, lang: AppLanguage) {
  var selectedGamePlanTab by remember { mutableIntStateOf(0) }
  HextechCard(accentColor = ArcaneCyan) {
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
        color = ArcaneCyan.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, ArcaneCyan.copy(alpha = 0.4f)),
      ) {
        Text(
          text = if (lang == AppLanguage.TH) "3 ช่วงเกม" else "3 Phases",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = ArcaneCyan,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    SecondaryTabRow(
      selectedTabIndex = selectedGamePlanTab,
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = HextechGoldBright,
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
          barColor = ArcaneCyan,
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
          barColor = HextechGold,
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
          border = BorderStroke(0.75.dp, ThreatCrimson.copy(alpha = 0.4f)),
          modifier =
            Modifier.clip(MaterialTheme.shapes.small)
              .clickable { onOpenChampionDetail(c.id) }
              .testTag("detail_counter_${c.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
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
          border = BorderStroke(0.75.dp, AdvantageEmerald.copy(alpha = 0.4f)),
          modifier =
            Modifier.clip(MaterialTheme.shapes.small)
              .clickable { onOpenChampionDetail(c.id) }
              .testTag("detail_favorable_${c.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
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
