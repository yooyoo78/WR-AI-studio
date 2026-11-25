package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.domain.engine.SmartBuildRecommendation
import com.example.ui.components.HextechCard
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.WildRiftRuneLoadoutStrip
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.SkillViolet

@Composable
fun RunesRecommendationCard(
  build: SmartBuildRecommendation,
  lang: AppLanguage,
  onInspectRune: (RuneData) -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = SkillViolet) {
    Text(text = strings.runesAndSpellsTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (lang == AppLanguage.TH) build.runeSpellReasonTh else build.runeSpellReasonEn,
      style = MaterialTheme.typography.bodySmall,
      color = ArcaneCyan,
    )
    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text =
        if (lang == AppLanguage.TH) "แตะที่รูปรูนเพื่อดูกล่องข้อมูลย่อ"
        else "Tap any rune icon for quick info",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))

    WildRiftRuneLoadoutStrip(
      keystone = build.keystone,
      minorRunes = build.minorRunes,
      language = lang,
      onInspectRune = onInspectRune,
      tagPrefix = "analyzer_rune_",
    )

    build.keystone?.let { ks ->
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "${ks.displayName(lang)}: ${ks.desc(lang)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SpellsAndSkillOrderCard(
  build: SmartBuildRecommendation,
  skillOrder: String,
  lang: AppLanguage,
  onInspectSpell: (SpellData) -> Unit,
) {
  HextechCard(accentColor = ArcaneCyan) {
    Text(
      text =
        if (lang == AppLanguage.TH) "Summoner Spells & อัปสกิล ($skillOrder):"
        else "Summoner Spells & Skill Order ($skillOrder):",
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text =
        if (lang == AppLanguage.TH) "แตะที่รูป Spell เพื่อดูกล่องข้อมูลย่อ"
        else "Tap a spell icon for quick info",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val laneNote = if (lang == AppLanguage.TH) build.laneConditionNoteTh else build.laneConditionNoteEn
    if (laneNote.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = laneNote,
        style = MaterialTheme.typography.bodySmall,
        color = ArcaneCyan,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      build.spells.forEach { spell ->
        SpellIconSlot(
          spell = spell,
          language = lang,
          onClick = { onInspectSpell(spell) },
          modifier = Modifier.testTag("analyzer_spell_${spell.id}"),
        )
      }
    }
  }
}

@Composable
fun GamePlanPhaseCard(
  build: SmartBuildRecommendation,
  lang: AppLanguage,
  onAskCoach: () -> Unit,
) {
  val strings = LocalUiStrings.current
  HextechCard(accentColor = AdvantageEmerald) {
    Text(text = strings.gamePlanTitle, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    PhaseBlock(
      label = strings.earlyPhase,
      color = ArcaneCyan,
      body = if (lang == AppLanguage.TH) build.earlyGamePlanTh else build.earlyGamePlanEn,
    )
    Spacer(modifier = Modifier.height(8.dp))
    PhaseBlock(
      label = strings.midPhase,
      color = HextechGold,
      body = if (lang == AppLanguage.TH) build.midGamePlanTh else build.midGamePlanEn,
    )
    Spacer(modifier = Modifier.height(8.dp))
    PhaseBlock(
      label = strings.latePhase,
      color = AdvantageEmerald,
      body = if (lang == AppLanguage.TH) build.lateGamePlanTh else build.lateGamePlanEn,
    )

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
      onClick = onAskCoach,
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .testTag("analyzer_ask_coach_button"),
    ) {
      Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ArcaneCyan)
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.askCoachAboutMatchup)
    }
  }
}

@Composable
private fun PhaseBlock(label: String, color: Color, body: String) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(0.75.dp, color.copy(alpha = 0.3f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(11.dp)) {
      Text(text = label, style = MaterialTheme.typography.titleSmall, color = color)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
