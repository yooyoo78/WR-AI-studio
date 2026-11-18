package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.DamageType
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.SpellData
import com.example.data.model.TierRank
import com.example.data.remote.DDragonIconResolver
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.HextechSurface
import com.example.ui.theme.HextechSurfaceHigh
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun HextechCard(
  modifier: Modifier = Modifier,
  accentColor: Color? = null,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val highContrast = LocalUserPrefs.current.highContrastCards
  val borderColor =
    accentColor?.copy(alpha = if (highContrast) 0.85f else 0.45f)
      ?: MaterialTheme.colorScheme.outline

  Surface(
    modifier =
      modifier.then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
      ),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 2.dp,
    border = BorderStroke(if (highContrast) 1.5.dp else 1.dp, borderColor),
  ) {
    Column(modifier = Modifier.padding(12.dp), content = content)
  }
}

@Composable
fun ChampionCrestAvatar(
  champion: ChampionData,
  size: Dp = 40.dp,
  modifier: Modifier = Modifier,
) {
  val lang = LocalUserPrefs.current.language
  val ringColors =
    when (champion.damageType) {
      DamageType.AD -> listOf(HextechGold, Color(0xFFB45309))
      DamageType.AP -> listOf(ArcaneCyan, Color(0xFF1D4ED8))
      DamageType.TRUE -> listOf(Color(0xFFF8FAFC), SkillViolet)
      DamageType.HYBRID -> listOf(HextechGold, ArcaneCyan)
    }

  val initials =
    champion.nameEn
      .split(" ", "'", ".")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercaseChar().toString() }
      .take(2)

  val imageUrl =
    champion.portraitUrl.ifBlank {
      DDragonIconResolver.championPortraitUrl(champion.id, champion.nameEn)
    }

  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(HextechSurfaceHigh, HextechSurface)))
        .border(2.dp, Brush.linearGradient(ringColors), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    val fallbackContent: @Composable () -> Unit = {
      Text(
        text = initials,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ringColors.first(),
      )
    }
    SubcomposeAsyncImage(
      model =
        ImageRequest.Builder(LocalContext.current)
          .data(imageUrl)
          .crossfade(true)
          .build(),
      contentDescription = champion.displayName(lang),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().clip(CircleShape),
      loading = { fallbackContent() },
      error = { fallbackContent() },
    )
  }
}

@Composable
fun ItemIconSlot(
  item: ItemData,
  language: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  accentColor: Color = HextechGold,
  badgeText: String? = null,
  badgeColor: Color = accentColor,
  showCost: Boolean = true,
  showName: Boolean = false,
) {
  val initials =
    item.nameEn
      .split(" ", "'", "-")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercaseChar().toString() }
      .take(2)

  Column(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .padding(2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(modifier = Modifier.size(size)) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(HextechSurfaceHigh, HextechSurface)))
            .border(1.5.dp, accentColor.copy(alpha = 0.78f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
      ) {
        val fallback: @Composable () -> Unit = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = accentColor.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = initials,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
          }
        }
        if (item.iconUrl.isNotBlank()) {
          SubcomposeAsyncImage(
            model =
              ImageRequest.Builder(LocalContext.current)
                .data(item.iconUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName(language),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
            loading = { fallback() },
            error = { fallback() },
          )
        } else {
          fallback()
        }
      }

      if (!badgeText.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(5.dp),
          color = badgeColor,
          modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF070B14),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }
      }
    }

    if (showCost) {
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "${item.cost}g",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = HextechGoldBright,
        maxLines = 1,
      )
    }

    if (showName) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = item.displayName(language),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(size + 14.dp),
      )
    }
  }
}

@Composable
fun SpellIconSlot(
  spell: SpellData,
  language: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  showLabel: Boolean = true,
) {
  val initials = spell.nameEn.take(2).uppercase()
  Column(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)
        .padding(2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier.size(size)
          .clip(RoundedCornerShape(10.dp))
          .background(Brush.linearGradient(listOf(HextechSurfaceHigh, HextechSurface)))
          .border(1.5.dp, ArcaneCyan.copy(alpha = 0.78f), RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center,
    ) {
      val fallback: @Composable () -> Unit = {
        Text(
          text = initials,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = ArcaneCyan,
        )
      }
      if (spell.iconUrl.isNotBlank()) {
        SubcomposeAsyncImage(
          model =
            ImageRequest.Builder(LocalContext.current)
              .data(spell.iconUrl)
              .crossfade(true)
              .build(),
          contentDescription = spell.displayName(language),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
          loading = { fallback() },
          error = { fallback() },
        )
      } else {
        fallback()
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = spell.displayName(language),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (spell.cooldownNote.isNotBlank()) {
        Text(
          text = spell.cooldownNote,
          style = MaterialTheme.typography.labelSmall,
          color = ArcaneCyan,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun AbilityIconBadge(
  slot: String,
  iconUrl: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  val slotColor =
    when (slot.uppercase()) {
      "P" -> ArcaneCyan
      "4", "R" -> HextechGold
      else -> SkillViolet
    }

  Box(modifier = modifier.size(size)) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(RoundedCornerShape(10.dp))
          .background(Brush.linearGradient(listOf(HextechSurfaceHigh, HextechSurface)))
          .border(1.5.dp, slotColor.copy(alpha = 0.75f), RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center,
    ) {
      val fallback: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = slotColor.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = slot,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = slotColor,
          )
        }
      }
      if (iconUrl.isNotBlank()) {
        SubcomposeAsyncImage(
          model =
            ImageRequest.Builder(LocalContext.current)
              .data(iconUrl)
              .crossfade(true)
              .build(),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
          loading = { fallback() },
          error = { fallback() },
        )
      } else {
        fallback()
      }
    }

    Surface(
      shape = RoundedCornerShape(5.dp),
      color = Color(0xFF070B14).copy(alpha = 0.9f),
      border = BorderStroke(1.dp, slotColor.copy(alpha = 0.8f)),
      modifier = Modifier.align(Alignment.BottomEnd),
    ) {
      Text(
        text = slot,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = slotColor,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
      )
    }
  }
}

@Composable
fun WinRatePillsRow(
  champion: ChampionData,
  language: AppLanguage,
  showBanRate: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val wrColor =
    when {
      champion.winRatePct >= 51.5 -> AdvantageEmerald
      champion.winRatePct >= 50.0 -> ArcaneCyan
      else -> WarningAmber
    }
  val wrText = String.format(Locale.US, "%.1f%%", champion.winRatePct)
  val prText = String.format(Locale.US, "%.1f%%", champion.pickRatePct)
  val brText = String.format(Locale.US, "%.1f%%", champion.banRatePct)

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = wrColor.copy(alpha = 0.16f),
      border = BorderStroke(1.dp, wrColor.copy(alpha = 0.5f)),
    ) {
      Text(
        text = if (language == AppLanguage.TH) "ชนะ $wrText" else "WR $wrText",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = wrColor,
        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
      )
    }
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Text(
        text = if (language == AppLanguage.TH) "หยิบ $prText" else "PR $prText",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
    if (showBanRate) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = ThreatCrimson.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.45f)),
      ) {
        Text(
          text = if (language == AppLanguage.TH) "แบน $brText" else "BR $brText",
          style = MaterialTheme.typography.labelSmall,
          color = ThreatCrimson,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailBottomSheet(
  item: ItemData,
  language: AppLanguage,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("item_detail_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ItemIconSlot(
          item = item,
          language = language,
          onClick = {},
          size = 64.dp,
          showCost = false,
          showName = false,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.displayName(language),
            style = MaterialTheme.typography.titleLarge,
            color = HextechGoldBright,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = HextechGold.copy(alpha = 0.18f),
              border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.6f)),
            ) {
              Text(
                text = "${item.cost} Gold",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HextechGoldBright,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = ArcaneCyan.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
            ) {
              Text(
                text = item.category.name,
                style = MaterialTheme.typography.labelSmall,
                color = ArcaneCyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_item_sheet_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = if (language == AppLanguage.TH) "ค่าพลังพื้นฐาน (Stats)" else "Item Stats",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = item.statsSummary(language),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ArcaneCyan,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          val passiveTitle =
            if (language == AppLanguage.TH) item.passiveNameTh else item.passiveNameEn
          Text(
            text =
              if (language == AppLanguage.TH) "สกิลติดตัว / กดใช้: $passiveTitle"
              else "Passive / Active: $passiveTitle",
            style = MaterialTheme.typography.titleSmall,
            color = HextechGoldBright,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = item.passiveDesc(language),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      if (item.counterTags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = if (language == AppLanguage.TH) "จุดเด่นแก้ทาง:" else "Tactical Counter Tags:",
          style = MaterialTheme.typography.labelMedium,
          color = AdvantageEmerald,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          item.counterTags.forEach { tag ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = AdvantageEmerald.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.5f)),
            ) {
              Text(
                text = tag.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AdvantageEmerald,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellDetailBottomSheet(
  spell: SpellData,
  language: AppLanguage,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("spell_detail_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SpellIconSlot(
          spell = spell,
          language = language,
          onClick = {},
          size = 60.dp,
          showLabel = false,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = spell.displayName(language),
            style = MaterialTheme.typography.titleLarge,
            color = ArcaneCyan,
          )
          if (spell.cooldownNote.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = ArcaneCyan.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.5f)),
            ) {
              Text(
                text =
                  if (language == AppLanguage.TH) "คูลดาวน์ ${spell.cooldownNote}"
                  else "Cooldown ${spell.cooldownNote}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ArcaneCyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_spell_sheet_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = if (language == AppLanguage.TH) "ความสามารถและการใช้งาน" else "Effect & Usage",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = spell.desc(language),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
fun TierBadge(tierRank: TierRank, modifier: Modifier = Modifier) {
  val bg =
    when (tierRank) {
      TierRank.S_PLUS -> HextechGold
      TierRank.S -> ArcaneCyan
      TierRank.A -> AdvantageEmerald
      TierRank.B -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
  val fg =
    when (tierRank) {
      TierRank.S_PLUS,
      TierRank.S,
      TierRank.A -> Color(0xFF070B14)
      TierRank.B -> MaterialTheme.colorScheme.onSurface
    }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = bg,
  ) {
    Text(
      text = tierRank.label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = fg,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
    )
  }
}

@Composable
fun DamageTypeBadge(damageType: DamageType, language: AppLanguage) {
  val color =
    when (damageType) {
      DamageType.AD -> WarningAmber
      DamageType.AP -> ArcaneCyan
      DamageType.HYBRID -> HextechGold
      DamageType.TRUE -> SkillViolet
    }
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
  ) {
    Text(
      text = if (language == AppLanguage.TH) damageType.labelTh else damageType.labelEn,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun TacticalMeterBar(
  label: String,
  valuePercent: Int,
  barColor: Color,
  trailingLabel: String = "$valuePercent%",
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = trailingLabel,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = barColor,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { (valuePercent.coerceIn(0, 100)) / 100f },
      modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
      color = barColor,
      trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
  }
}

@Composable
fun PatchBadgePill(patchVersion: String, sourceLabel: String) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = HextechGold.copy(alpha = 0.15f),
    border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.6f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier.size(7.dp).clip(CircleShape).background(AdvantageEmerald)
      )
      Text(
        text = "Patch $patchVersion • $sourceLabel",
        style = MaterialTheme.typography.labelSmall,
        color = HextechGoldBright,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChampionPickerBottomSheet(
  title: String,
  champions: List<ChampionData>,
  selectedIds: Set<String>,
  disabledIds: Set<String> = emptySet(),
  language: AppLanguage,
  onSelectChampion: (ChampionData) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalUiStrings.current
  var query by remember { mutableStateOf("") }
  var selectedLane by remember { mutableStateOf<Lane?>(null) }

  val filtered =
    remember(champions, query, selectedLane) {
      champions.filter { champ ->
        val matchesQ =
          query.isBlank() ||
            champ.nameEn.contains(query.trim(), ignoreCase = true) ||
            champ.nameTh.contains(query.trim(), ignoreCase = true)
        val matchesLane = selectedLane == null || selectedLane in champ.lanes
        matchesQ && matchesLane
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_picker_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().testTag("picker_search_input"),
        placeholder = { Text(strings.searchChampionHint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
      )

      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilterChip(
          selected = selectedLane == null,
          onClick = { selectedLane = null },
          label = { Text(if (language == AppLanguage.TH) "ทุกเลน" else "All Lanes") },
        )
        Lane.entries.forEach { lane ->
          FilterChip(
            selected = selectedLane == lane,
            onClick = { selectedLane = if (selectedLane == lane) null else lane },
            label = { Text(if (language == AppLanguage.TH) lane.labelTh else lane.labelEn) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = HextechGold.copy(alpha = 0.25f),
                selectedLabelColor = HextechGoldBright,
              ),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filtered, key = { it.id }) { champ ->
          val isSelected = champ.id in selectedIds
          val isDisabled = champ.id in disabledIds
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = !isDisabled) { onSelectChampion(champ) }
                .testTag("picker_champ_${champ.id}"),
            color =
              when {
                isSelected -> HextechGold.copy(alpha = 0.18f)
                isDisabled -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceContainer
              },
            border =
              BorderStroke(
                1.dp,
                if (isSelected) HextechGold else MaterialTheme.colorScheme.outline,
              ),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ChampionCrestAvatar(champion = champ, size = 42.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = champ.displayName(language),
                    style = MaterialTheme.typography.titleSmall,
                    color =
                      if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant
                      else MaterialTheme.colorScheme.onSurface,
                  )
                  TierBadge(champ.tierRank)
                }
                Text(
                  text =
                    champ.lanes.joinToString(" • ") {
                      if (language == AppLanguage.TH) it.labelTh else it.labelEn
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (isSelected) {
                Text(
                  text = if (language == AppLanguage.TH) "เลือกแล้ว" else "Selected",
                  style = MaterialTheme.typography.labelMedium,
                  color = HextechGoldBright,
                )
              } else if (isDisabled) {
                Text(
                  text = if (language == AppLanguage.TH) "ฝั่งตรงข้าม" else "Taken",
                  style = MaterialTheme.typography.labelSmall,
                  color = ThreatCrimson,
                )
              }
            }
          }
        }
      }
    }
  }
}
