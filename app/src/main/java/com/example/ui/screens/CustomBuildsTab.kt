package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemData
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.resolveLegacyItemId
import com.example.data.model.resolveLegacyRuneId
import com.example.data.model.tier3BootUpgradeId
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.WildRiftRuneLoadoutStrip
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson

@Composable
fun CustomBuildsTab(
  customBuilds: List<CustomBuildData>,
  championsById: Map<String, ChampionData>,
  itemsById: Map<String, ItemData>,
  runesById: Map<String, RuneData>,
  spellsById: Map<String, SpellData>,
  language: AppLanguage,
  onCreateNew: () -> Unit,
  onEditBuild: (CustomBuildData) -> Unit,
  onDuplicateBuild: (CustomBuildData) -> Unit,
  onDeleteBuild: (Long) -> Unit,
  onInspectItem: (ItemData) -> Unit,
  onInspectRune: (RuneData) -> Unit,
  onInspectSpell: (SpellData) -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
) {
  val isMiniMode = LocalInGameMiniMode.current
  var query by remember { mutableStateOf("") }

  val filtered =
    remember(customBuilds, query, championsById) {
      val q = query.trim().lowercase()
      if (q.isBlank()) customBuilds
      else {
        customBuilds.filter { build ->
          val champ = championsById[build.championId]
          build.name.lowercase().contains(q) ||
            (champ?.nameEn?.lowercase()?.contains(q) == true) ||
            (champ?.nameTh?.lowercase()?.contains(q) == true)
        }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("custom_builds_lazy_column"),
    contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 10.dp),
  ) {
    item {
      Button(
        onClick = onCreateNew,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("create_custom_build_button"),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = HextechGold,
            contentColor = Color(0xFF0A0F1A),
          ),
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (language == AppLanguage.TH) "สร้างเซ็ตไอเทมเอง (+ New Custom Build)"
            else "Create Custom Build (+ New Set)",
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (customBuilds.isNotEmpty()) {
      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth().testTag("custom_builds_search_input"),
          placeholder = {
            Text(
              if (language == AppLanguage.TH) "ค้นหาเซ็ตหรือแชมเปี้ยน..."
              else "Search custom builds..."
            )
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
        )
      }
    }

    if (filtered.isEmpty()) {
      item {
        HextechCard(accentColor = ArcaneCyan) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Default.Build,
              contentDescription = null,
              tint = ArcaneCyan,
              modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text =
                if (language == AppLanguage.TH) "ยังไม่มีเซ็ตไอเทมที่คุณสร้างเอง"
                else "No Custom Builds Yet",
              style = MaterialTheme.typography.titleMedium,
              color = HextechGoldBright,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text =
                if (language == AppLanguage.TH)
                  "กดปุ่มด้านบนเพื่อเลือกแชมเปี้ยน จัดไอเทม รองเท้า ไอเทมกดใช้ และรูนด้วยตัวเอง หรือบันทึกมาจากผลวิเคราะห์ 1v5"
                else
                  "Tap above to pick a champion and customize items, boots, active item, runes, and spells—or save directly from 1v5 Analyzer.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(filtered, key = { it.id }) { build ->
        CustomBuildCard(
          build = build,
          champion = championsById[build.championId],
          itemsById = itemsById,
          runesById = runesById,
          spellsById = spellsById,
          language = language,
          onEdit = { onEditBuild(build) },
          onDuplicate = { onDuplicateBuild(build) },
          onDelete = { onDeleteBuild(build.id) },
          onInspectItem = onInspectItem,
          onInspectRune = onInspectRune,
          onInspectSpell = onInspectSpell,
          onOpenChampionDetail = onOpenChampionDetail,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomBuildCard(
  build: CustomBuildData,
  champion: ChampionData?,
  itemsById: Map<String, ItemData>,
  runesById: Map<String, RuneData>,
  spellsById: Map<String, SpellData>,
  language: AppLanguage,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
  onInspectItem: (ItemData) -> Unit,
  onInspectRune: (RuneData) -> Unit,
  onInspectSpell: (SpellData) -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
) {
  val starter = itemsById[resolveLegacyItemId(build.starterItemId)]
  val cores = build.coreItemIds.mapNotNull { itemsById[resolveLegacyItemId(it)] }.distinctBy { it.id }
  val boots = itemsById[resolveLegacyItemId(build.bootsId)]
  val tier3Boots = boots?.let { tier3BootUpgradeId(it.id) }?.let { itemsById[it] }
  val active = itemsById[resolveLegacyItemId(build.enchantId)]
  val keystone = runesById[resolveLegacyRuneId(build.keystoneId)]
  val minors = build.minorRuneIds.mapNotNull { runesById[resolveLegacyRuneId(it)] }
  val spells = build.spellIds.mapNotNull { spellsById[it] }

  val totalGold =
    (starter?.cost ?: 0) +
      cores.sumOf { it.cost } +
      (boots?.cost ?: 0) +
      (active?.cost ?: 0)

  HextechCard(
    accentColor = HextechGold,
    modifier = Modifier.fillMaxWidth().testTag("custom_build_card_${build.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (champion != null) {
        ChampionCrestAvatar(
          champion = champion,
          size = 40.dp,
          onClick = { onOpenChampionDetail(champion.id) },
        )
        Spacer(modifier = Modifier.width(10.dp))
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = build.name,
          style = MaterialTheme.typography.titleMedium,
          color = HextechGoldBright,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (champion != null) {
            Text(
              text = champion.displayName(language),
              style = MaterialTheme.typography.labelSmall,
              color = ArcaneCyan,
            )
          }
          Surface(
            shape = RoundedCornerShape(5.dp),
            color = ArcaneCyan.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.4f)),
          ) {
            Text(
              text = if (language == AppLanguage.TH) build.lane.labelTh else build.lane.labelEn,
              style = MaterialTheme.typography.labelSmall,
              color = ArcaneCyan,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
          Surface(
            shape = RoundedCornerShape(5.dp),
            color = HextechGold.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
          ) {
            Text(
              text = "${totalGold}g",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = HextechGoldBright,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
      }

      IconButton(
        onClick = onEdit,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_build_edit_${build.id}"),
      ) {
        Icon(Icons.Default.Edit, contentDescription = "Edit build", tint = HextechGold)
      }
      IconButton(
        onClick = onDuplicate,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_build_duplicate_${build.id}"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate build", tint = ArcaneCyan)
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_build_delete_${build.id}"),
      ) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete build", tint = ThreatCrimson)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      starter?.let { item ->
        item(key = "cb_start_${item.id}") {
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { onInspectItem(item) },
            size = 42.dp,
            accentColor = ArcaneCyan,
            badgeText = "Start",
            badgeColor = ArcaneCyan,
            showCost = true,
            showName = true,
          )
        }
      }
      items(cores.size, key = { idx -> "cb_core_${idx}_${cores[idx].id}" }) { idx ->
        val item = cores[idx]
        ItemIconSlot(
          item = item,
          language = language,
          onClick = { onInspectItem(item) },
          size = 42.dp,
          accentColor = HextechGold,
          badgeText = "#${idx + 1}",
          badgeColor = HextechGold,
          showCost = true,
          showName = true,
        )
      }
      boots?.let { item ->
        item(key = "cb_boots_${item.id}") {
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { onInspectItem(item) },
            size = 42.dp,
            accentColor = AdvantageEmerald,
            badgeText = if (language == AppLanguage.TH) "รองเท้า" else "Boots",
            badgeColor = AdvantageEmerald,
            showCost = true,
            showName = true,
          )
        }
      }
      active?.let { item ->
        item(key = "cb_act_${item.id}") {
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { onInspectItem(item) },
            size = 42.dp,
            accentColor = SkillViolet,
            badgeText = "Active",
            badgeColor = SkillViolet,
            showCost = true,
            showName = true,
          )
        }
      }
      tier3Boots?.let { t3Item ->
        item(key = "cb_t3_${t3Item.id}") {
          ItemIconSlot(
            item = t3Item,
            language = language,
            onClick = { onInspectItem(t3Item) },
            size = 42.dp,
            accentColor = HextechGoldBright,
            badgeText = if (language == AppLanguage.TH) "10น.+ T3" else "10m+ T3",
            badgeColor = HextechGold,
            showCost = true,
            showName = true,
          )
        }
      }
    }

    if (keystone != null || minors.isNotEmpty() || spells.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      WildRiftRuneLoadoutStrip(
        keystone = keystone,
        minorRunes = minors,
        language = language,
        onInspectRune = onInspectRune,
        tagPrefix = "cb_rune_${build.id}_",
        compact = true,
      )
      if (spells.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          spells.forEach { sp ->
            SpellIconSlot(
              spell = sp,
              language = language,
              onClick = { onInspectSpell(sp) },
              size = 30.dp,
              showLabel = false,
            )
          }
        }
      }
    }

    if (build.notes.isNotBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = build.notes,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
