package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.LaneDifficulty
import com.example.domain.engine.EnemyMatchupAnalysis
import com.example.domain.engine.EnemyTeamThreatProfile
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.HextechCard
import com.example.ui.components.TacticalMeterBar
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber

@Composable
fun TopThreatSpotlightCard(
  top: EnemyMatchupAnalysis,
  lang: AppLanguage,
  onOpenChampionProfile: (String) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = ThreatCrimson, modifier = Modifier.testTag("top_threat_card")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ThreatCrimson, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = strings.topThreatLabel,
          style = MaterialTheme.typography.labelLarge,
          color = ThreatCrimson,
        )
      }
      MatchupConfidenceBadge(isVerified = top.isVerifiedRule, lang = lang)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      ChampionCrestAvatar(
        champion = top.enemy,
        size = 46.dp,
        onClick = { onOpenChampionProfile(top.enemy.id) },
        modifier = Modifier.testTag("top_threat_avatar"),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = top.enemy.displayName(lang),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = top.dangerAbilities(lang),
          style = MaterialTheme.typography.bodySmall,
          color = WarningAmber,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Surface(
        color = ThreatCrimson.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
      ) {
        Text(
          text = "${top.threatScore}/100",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = ThreatCrimson,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = top.tacticalTip(lang),
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun ThreatProfileCard(profile: EnemyTeamThreatProfile, lang: AppLanguage) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = ArcaneCyan) {
    Text(text = strings.teamThreatProfileTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "เบิร์สเวท (AP)" else "AP Burst",
          valuePercent = profile.apBurstScore,
          barColor = ArcaneCyan,
          modifier = Modifier.weight(1f),
        )
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "กายภาพ (AD)" else "Physical AD",
          valuePercent = profile.adDpsScore,
          barColor = WarningAmber,
          modifier = Modifier.weight(1f),
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "ฮีล / ดูดเลือด" else "Healing",
          valuePercent = profile.healingScore,
          barColor = AdvantageEmerald,
          modifier = Modifier.weight(1f),
        )
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "แทงค์ถึก" else "Frontline",
          valuePercent = profile.tankinessScore,
          barColor = HextechGold,
          modifier = Modifier.weight(1f),
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "CC ล็อก" else "Hard CC",
          valuePercent = profile.ccScore,
          barColor = SkillViolet,
          modifier = Modifier.weight(1f),
        )
        TacticalMeterBar(
          label = if (lang == AppLanguage.TH) "ตีไว (AS)" else "Attack Speed",
          valuePercent = profile.attackSpeedScore,
          barColor = ThreatCrimson,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
fun EnemyMatchupCard(
  analysis: EnemyMatchupAnalysis,
  lang: AppLanguage,
  onOpenChampionProfile: (String) -> Unit,
  onOpenDetails: () -> Unit = {},
) {
  val (verdictColor, verdictLabel) =
    when (analysis.verdict) {
      LaneDifficulty.EASY ->
        AdvantageEmerald to (if (lang == AppLanguage.TH) "ได้เปรียบ" else "Advantage")
      LaneDifficulty.HARD ->
        ThreatCrimson to (if (lang == AppLanguage.TH) "เสียเปรียบ" else "Disadvantage")
      LaneDifficulty.SKILL ->
        WarningAmber to (if (lang == AppLanguage.TH) "วัดฝีมือ" else "Skill Matchup")
      LaneDifficulty.EVEN ->
        ArcaneCyan to (if (lang == AppLanguage.TH) "สูสี" else "Even")
    }

  HextechCard(
    accentColor = verdictColor,
    onClick = onOpenDetails,
    modifier = Modifier.fillMaxWidth().testTag("matchup_card_${analysis.enemy.id}"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ChampionCrestAvatar(
        champion = analysis.enemy,
        size = 38.dp,
        onClick = { onOpenChampionProfile(analysis.enemy.id) },
        modifier = Modifier.testTag("matchup_enemy_avatar_${analysis.enemy.id}"),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = analysis.enemy.displayName(lang),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (analysis.isLikelyLaneOpponent) {
            Surface(
              color = HextechGold.copy(alpha = 0.2f),
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = if (lang == AppLanguage.TH) "คู่เลน" else "Lane",
                style = MaterialTheme.typography.labelSmall,
                color = HextechGoldBright,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          MatchupConfidenceBadge(
            isVerified = analysis.isVerifiedRule,
            lang = lang,
            modifier = Modifier.testTag("matchup_badge_${analysis.enemy.id}"),
          )
        }
        Text(
          text =
            "${if (lang == AppLanguage.TH) "พีค:" else "Spike:"} ${analysis.keySpike(lang)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      Surface(
        color = verdictColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, verdictColor.copy(alpha = 0.45f)),
      ) {
        Text(
          text = verdictLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = verdictColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchupDetailBottomSheet(
  analysis: EnemyMatchupAnalysis,
  lang: AppLanguage,
  onOpenChampionProfile: (String) -> Unit,
  onDismiss: () -> Unit,
) {
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
          .testTag("analyzer_matchup_detail_sheet"),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ChampionCrestAvatar(champion = analysis.enemy, size = 46.dp)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = analysis.enemy.displayName(lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HextechGoldBright,
          )
          Text(
            text = "${if (lang == AppLanguage.TH) "จุดพีค:" else "Key Spike:"} ${analysis.keySpike(lang)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        MatchupConfidenceBadge(isVerified = analysis.isVerifiedRule, lang = lang)
      }

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = WarningAmber.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = analysis.dangerAbilities(lang),
          style = MaterialTheme.typography.bodyMedium,
          color = WarningAmber,
          modifier = Modifier.padding(12.dp),
        )
      }

      Text(
        text = analysis.tacticalTip(lang),
        style = MaterialTheme.typography.bodyMedium,
      )

      OutlinedButton(
        onClick = {
          onDismiss()
          onOpenChampionProfile(analysis.enemy.id)
        },
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
      ) {
        Text(if (lang == AppLanguage.TH) "ดูโปรไฟล์และสกิลทั้งหมด" else "Open Champion Profile")
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
      }

      Spacer(modifier = Modifier.height(10.dp))
    }
  }
}

@Composable
fun MatchupConfidenceBadge(
  isVerified: Boolean,
  lang: AppLanguage,
  modifier: Modifier = Modifier,
) {
  val color = if (isVerified) AdvantageEmerald else ArcaneCyan
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(0.6.dp, color.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      if (isVerified) {
        Icon(
          Icons.Default.Verified,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(11.dp),
        )
      }
      Text(
        text =
          if (isVerified) {
            if (lang == AppLanguage.TH) "Verified" else "Verified"
          } else {
            if (lang == AppLanguage.TH) "Estimate" else "Estimate"
          },
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
