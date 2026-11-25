package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ItemData
import com.example.domain.engine.ItemRecommendationEntry
import com.example.domain.engine.SmartBuildRecommendation
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemIconSlot
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber

@Composable
fun FirstRushItemCard(
  entry: ItemRecommendationEntry,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = HextechGold, modifier = Modifier.testTag("first_rush_item_card")) {
    Text(
      text = strings.buyFirstLabel,
      style = MaterialTheme.typography.labelLarge,
      color = HextechGoldBright,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ItemIconSlot(
        item = entry.item,
        language = lang,
        onClick = { onInspectItem(entry.item) },
        size = 54.dp,
        accentColor = HextechGold,
        badgeText = "#1",
        badgeColor = HextechGold,
        showCost = false,
        showName = false,
        modifier = Modifier.testTag("analyzer_rush_item_icon"),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = entry.item.displayName(lang),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "${entry.item.cost}g",
            style = MaterialTheme.typography.titleSmall,
            color = HextechGold,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = entry.item.statsSummary(lang),
          style = MaterialTheme.typography.bodySmall,
          color = ArcaneCyan,
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = entry.reason(lang), style = MaterialTheme.typography.bodyMedium)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildDiffCard(
  build: SmartBuildRecommendation,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  val strings = LocalUiStrings.current
  val diff = build.buildDiff
  HextechCard(accentColor = ArcaneCyan) {
    Text(text = strings.buildDiffTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) diff.adaptationHeadlineTh else diff.adaptationHeadlineEn,
      style = MaterialTheme.typography.bodyMedium,
      color = ArcaneCyan,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "แตะที่รูปไอเทมเพื่อดูกล่องข้อมูลย่อและค่าพลัง"
        else "Tap any item icon for quick stats box",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = if (lang == AppLanguage.TH) "บิลด์มาตรฐาน:" else "Standard Core:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      diff.standardCoreItems.forEach { item ->
        val isSwappedOut = diff.swappedOutItems.any { it.id == item.id }
        ItemIconSlot(
          item = item,
          language = lang,
          onClick = { onInspectItem(item) },
          size = 46.dp,
          accentColor = if (isSwappedOut) ThreatCrimson else MaterialTheme.colorScheme.outline,
          badgeText = if (isSwappedOut) "−" else null,
          badgeColor = ThreatCrimson,
          reserveBadgeSpace = true,
          showCost = true,
          showName = true,
          modifier = Modifier.testTag("diff_std_item_${item.id}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = if (lang == AppLanguage.TH) "บิลด์ที่ปรับสำหรับไฟต์นี้:" else "Adapted Core:",
      style = MaterialTheme.typography.labelMedium,
      color = AdvantageEmerald,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      diff.adaptedCoreItems.forEach { item ->
        val isSwappedIn = diff.swappedInItems.any { it.id == item.id }
        ItemIconSlot(
          item = item,
          language = lang,
          onClick = { onInspectItem(item) },
          size = 46.dp,
          accentColor = if (isSwappedIn) AdvantageEmerald else HextechGold,
          badgeText = if (isSwappedIn) "+" else null,
          badgeColor = AdvantageEmerald,
          reserveBadgeSpace = true,
          showCost = true,
          showName = true,
          modifier = Modifier.testTag("diff_adapted_item_${item.id}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "รองเท้า (ขั้น 2 → อัปเกรด 10:00+) & ไอเทมกดใช้:"
        else "Boots (Tier 2 → 10:00+ Tier 3) & Active:",
      style = MaterialTheme.typography.labelMedium,
      color = HextechGoldBright,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      ItemIconSlot(
        item = build.recommendedBoots.item,
        language = lang,
        onClick = { onInspectItem(build.recommendedBoots.item) },
        size = 44.dp,
        accentColor = AdvantageEmerald,
        badgeText = if (lang == AppLanguage.TH) "ขั้น 2" else "Tier 2",
        badgeColor = AdvantageEmerald,
        showCost = true,
        showName = true,
        modifier = Modifier.testTag("diff_boots_${build.recommendedBoots.item.id}"),
      )
      build.recommendedTier3Boots?.item?.let { t3Boot ->
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = HextechGold,
          modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
        )
        ItemIconSlot(
          item = t3Boot,
          language = lang,
          onClick = { onInspectItem(t3Boot) },
          size = 44.dp,
          accentColor = HextechGoldBright,
          badgeText = if (lang == AppLanguage.TH) "10น.+ T3" else "10m+ T3",
          badgeColor = HextechGold,
          showCost = true,
          showName = true,
          modifier = Modifier.testTag("diff_boots_t3_${t3Boot.id}"),
        )
      }
      ItemIconSlot(
        item = build.recommendedEnchant.item,
        language = lang,
        onClick = { onInspectItem(build.recommendedEnchant.item) },
        size = 44.dp,
        accentColor = SkillViolet,
        badgeText = "Active",
        badgeColor = SkillViolet,
        showCost = true,
        showName = true,
        modifier = Modifier.testTag("diff_enchant_${build.recommendedEnchant.item.id}"),
      )
    }
  }
}

@Composable
fun SmartItemDetailCard(
  entry: ItemRecommendationEntry,
  lang: AppLanguage,
  onInspectItem: (ItemData) -> Unit,
) {
  var expanded by remember { mutableStateOf(entry.isAdapted) }
  val accent = if (entry.isAdapted) HextechGold else MaterialTheme.colorScheme.outline

  HextechCard(
    accentColor = if (entry.isAdapted) HextechGold else null,
    onClick = { expanded = !expanded },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ItemIconSlot(
        item = entry.item,
        language = lang,
        onClick = { onInspectItem(entry.item) },
        size = 46.dp,
        accentColor = if (entry.isAdapted) HextechGold else ArcaneCyan,
        showCost = false,
        showName = false,
        modifier = Modifier.testTag("analyzer_item_icon_${entry.item.id}"),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color =
              if (entry.isAdapted) HextechGold.copy(alpha = 0.22f)
              else ArcaneCyan.copy(alpha = 0.16f),
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = entry.priorityLabel(lang),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (entry.isAdapted) HextechGoldBright else ArcaneCyan,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${entry.item.cost}g",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = entry.item.displayName(lang),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = entry.item.statsSummary(lang),
          style = MaterialTheme.typography.bodySmall,
          color = accent,
        )
      }
      Icon(
        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = "Toggle item details",
      )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "${if (lang == AppLanguage.TH) "แก้ทาง:" else "Targets:"} ${entry.targetThreat(lang)}",
      style = MaterialTheme.typography.labelMedium,
      color = WarningAmber,
    )

    if (expanded) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "เหตุผล:" else "Why:"} ${entry.reason(lang)}",
        style = MaterialTheme.typography.bodySmall,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "จังหวะซื้อ:" else "Timing:"} ${entry.buyTiming(lang)}",
        style = MaterialTheme.typography.bodySmall,
        color = ArcaneCyan,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "• ${if (lang == AppLanguage.TH) "ข้อได้เปรียบ:" else "Advantage:"} ${entry.tacticalBenefit(lang)}",
        style = MaterialTheme.typography.bodySmall,
        color = AdvantageEmerald,
      )
    }
  }
}
