package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.CustomBuildData
import com.example.data.model.ItemCategory
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.resolveLegacyItemId
import com.example.data.model.resolveLegacyRuneId
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson

private sealed interface EditorSlotTarget {
  data object Starter : EditorSlotTarget
  data class CoreSlot(val index: Int?) : EditorSlotTarget
  data object Boots : EditorSlotTarget
  data object Enchant : EditorSlotTarget
  data object Keystone : EditorSlotTarget
  data class MinorRune(val index: Int) : EditorSlotTarget
  data class SpellSlot(val index: Int) : EditorSlotTarget
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomBuildEditorSheet(
  initialBuild: CustomBuildData,
  allChampions: List<ChampionData>,
  itemsById: Map<String, ItemData>,
  runesById: Map<String, RuneData>,
  spellsById: Map<String, SpellData>,
  language: AppLanguage,
  onSave: (CustomBuildData) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  var selectedChampionId by remember(initialBuild.id, initialBuild.championId) {
    mutableStateOf(initialBuild.championId)
  }
  var buildName by remember(initialBuild.id, initialBuild.name) {
    mutableStateOf(initialBuild.name)
  }
  var selectedLane by remember(initialBuild.id, initialBuild.lane) {
    mutableStateOf(initialBuild.lane)
  }
  var starterId by remember(initialBuild.id, initialBuild.starterItemId) {
    mutableStateOf(resolveLegacyItemId(initialBuild.starterItemId))
  }
  var coreIds by remember(initialBuild.id, initialBuild.coreItemIds) {
    mutableStateOf(initialBuild.coreItemIds.map(::resolveLegacyItemId).distinct().take(5))
  }
  var bootsId by remember(initialBuild.id, initialBuild.bootsId) {
    mutableStateOf(resolveLegacyItemId(initialBuild.bootsId))
  }
  var enchantId by remember(initialBuild.id, initialBuild.enchantId) {
    mutableStateOf(resolveLegacyItemId(initialBuild.enchantId))
  }
  var keystoneId by remember(initialBuild.id, initialBuild.keystoneId) {
    mutableStateOf(resolveLegacyRuneId(initialBuild.keystoneId))
  }
  var minorRuneIds by remember(initialBuild.id, initialBuild.minorRuneIds) {
    val resolved = initialBuild.minorRuneIds.map(::resolveLegacyRuneId).take(4).toMutableList()
    val fallbackDefaults = listOf("triumph", "coup_de_grace", "legend_alacrity", "bone_plating")
    while (resolved.size < 4) {
      val nextFallback = fallbackDefaults.firstOrNull { it !in resolved } ?: "bone_plating"
      resolved.add(nextFallback)
    }
    mutableStateOf(resolved.toList())
  }
  var spellIds by remember(initialBuild.id, initialBuild.spellIds) {
    mutableStateOf(initialBuild.spellIds.take(2))
  }
  var notes by remember(initialBuild.id, initialBuild.notes) {
    mutableStateOf(initialBuild.notes)
  }

  var showChampPicker by remember { mutableStateOf(false) }
  var activeSlotTarget by remember { mutableStateOf<EditorSlotTarget?>(null) }

  val champMap = remember(allChampions) { allChampions.associateBy { it.id } }
  val selectedChamp = champMap[selectedChampionId] ?: allChampions.firstOrNull()
  val allItemsList = remember(itemsById) { itemsById.values.toList() }
  val allRunesList = remember(runesById) { runesById.values.toList() }
  val allSpellsList = remember(spellsById) { spellsById.values.toList() }

  val totalGoldCost =
    remember(starterId, coreIds, bootsId, enchantId, itemsById) {
      val starterCost = itemsById[starterId]?.cost ?: 0
      val coreCost = coreIds.sumOf { itemsById[it]?.cost ?: 0 }
      val bootsCost = itemsById[bootsId]?.cost ?: 0
      val enchantCost = itemsById[enchantId]?.cost ?: 0
      starterCost + coreCost + bootsCost + enchantCost
    }

  val counterCoverage =
    remember(coreIds, bootsId, enchantId, itemsById) {
      (coreIds + bootsId + enchantId)
        .mapNotNull { itemsById[it] }
        .flatMap { it.counterTags }
        .distinct()
    }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("custom_build_editor_sheet"),
  ) {
    ProvideSafeTextToolbar {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 680.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            if (initialBuild.id == 0L) {
              if (language == AppLanguage.TH) "สร้างเซ็ตไอเทมเอง" else "Create Custom Build"
            } else {
              if (language == AppLanguage.TH) "แก้ไขเซ็ตไอเทม" else "Edit Custom Build"
            },
          style = MaterialTheme.typography.titleLarge,
          color = HextechGoldBright,
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("custom_build_close_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Champion selector card
      selectedChamp?.let { champ ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.55f)),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { showChampPicker = true }
              .testTag("custom_build_pick_champion"),
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ChampionCrestAvatar(champion = champ, size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = champ.displayName(language),
                style = MaterialTheme.typography.titleSmall,
                color = HextechGoldBright,
              )
              Text(
                text = if (language == AppLanguage.TH) "แตะเพื่อเปลี่ยนแชมเปี้ยน" else "Tap to change champion",
                style = MaterialTheme.typography.labelSmall,
                color = ArcaneCyan,
              )
            }
            Icon(Icons.Default.SwapHoriz, contentDescription = "Change champion", tint = HextechGold)
          }
        }
      }

      // Build Name & Notes
      OutlinedTextField(
        value = buildName,
        onValueChange = { buildName = it },
        modifier = Modifier.fillMaxWidth().testTag("custom_build_name_input"),
        label = {
          Text(if (language == AppLanguage.TH) "ชื่อเซ็ต (เช่น Jinx เจาะแทงค์)" else "Build Name")
        },
        singleLine = true,
      )

      // Lane Selector
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Lane.entries.forEach { lane ->
          FilterChip(
            selected = selectedLane == lane,
            onClick = { selectedLane = lane },
            modifier = Modifier.testTag("custom_build_lane_${lane.name}"),
            label = { Text(if (language == AppLanguage.TH) lane.labelTh else lane.labelEn) },
          )
        }
      }

      // Summary bar: Gold & Tags
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = HextechGold.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (language == AppLanguage.TH) "ราคารวมทั้งเซ็ต" else "Total Build Gold",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${totalGoldCost}g",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = HextechGoldBright,
              modifier = Modifier.testTag("custom_build_total_gold"),
            )
          }
          if (counterCoverage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              counterCoverage.take(6).forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = AdvantageEmerald.copy(alpha = 0.16f),
                  border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = tag.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = AdvantageEmerald,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Starter, Boots, Active Row
      Text(
        text =
          if (language == AppLanguage.TH) "เริ่มต้น • รองเท้า • ไอเทมกดใช้ (แตะเพื่อเปลี่ยน)"
          else "Starter • Boots • Active Item (Tap to change)",
        style = MaterialTheme.typography.labelMedium,
        color = ArcaneCyan,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        itemsById[starterId]?.let { item ->
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { activeSlotTarget = EditorSlotTarget.Starter },
            accentColor = ArcaneCyan,
            badgeText = "Start",
            badgeColor = ArcaneCyan,
            showCost = true,
            showName = true,
            modifier = Modifier.testTag("editor_slot_starter"),
          )
        }
        itemsById[bootsId]?.let { item ->
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { activeSlotTarget = EditorSlotTarget.Boots },
            accentColor = HextechGold,
            badgeText = if (language == AppLanguage.TH) "รองเท้า" else "Boots",
            badgeColor = HextechGold,
            showCost = true,
            showName = true,
            modifier = Modifier.testTag("editor_slot_boots"),
          )
        }
        itemsById[enchantId]?.let { item ->
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { activeSlotTarget = EditorSlotTarget.Enchant },
            accentColor = SkillViolet,
            badgeText = "Active",
            badgeColor = SkillViolet,
            showCost = true,
            showName = true,
            modifier = Modifier.testTag("editor_slot_enchant"),
          )
        }
      }

      // Core Items Section (1..5 items with reorder & swap/remove)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            if (language == AppLanguage.TH) "ไอเทมหลัก (${coreIds.size}/5 ชิ้น)"
            else "Core Items (${coreIds.size}/5)",
          style = MaterialTheme.typography.labelMedium,
          color = HextechGoldBright,
        )
        if (coreIds.size < 5) {
          OutlinedButton(
            onClick = { activeSlotTarget = EditorSlotTarget.CoreSlot(null) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_add_core_item"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (language == AppLanguage.TH) "เพิ่มไอเทม" else "Add Item")
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        coreIds.forEachIndexed { idx, itemId ->
          val item = itemsById[itemId]
          if (item != null) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth().testTag("editor_core_row_$idx"),
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ItemIconSlot(
                  item = item,
                  language = language,
                  onClick = { activeSlotTarget = EditorSlotTarget.CoreSlot(idx) },
                  size = 44.dp,
                  badgeText = "#${idx + 1}",
                  showCost = false,
                  showName = false,
                  modifier = Modifier.testTag("editor_core_slot_$idx"),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                  modifier =
                    Modifier.weight(1f).clickable { activeSlotTarget = EditorSlotTarget.CoreSlot(idx) }
                ) {
                  Text(
                    text = item.displayName(language),
                    style = MaterialTheme.typography.titleSmall,
                    color = HextechGoldBright,
                  )
                  Text(
                    text = "${item.cost}g • ${item.statsSummary(language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ArcaneCyan,
                    maxLines = 1,
                  )
                }
                if (idx > 0) {
                  IconButton(
                    onClick = {
                      val list = coreIds.toMutableList()
                      val tmp = list[idx - 1]
                      list[idx - 1] = list[idx]
                      list[idx] = tmp
                      coreIds = list
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = "Move earlier",
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                if (idx < coreIds.lastIndex) {
                  IconButton(
                    onClick = {
                      val list = coreIds.toMutableList()
                      val tmp = list[idx + 1]
                      list[idx + 1] = list[idx]
                      list[idx] = tmp
                      coreIds = list
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      Icons.AutoMirrored.Filled.ArrowForward,
                      contentDescription = "Move later",
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                if (coreIds.size > 1) {
                  IconButton(
                    onClick = { coreIds = coreIds.filterIndexed { i, _ -> i != idx } },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("editor_remove_core_$idx"),
                  ) {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Remove item",
                      tint = ThreatCrimson,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Runes (1 Keystone + 3 Primary Minor + 1 Secondary Minor) & Spells
      Text(
        text =
          if (language == AppLanguage.TH) "รูน: หลัก (1) • รอง 1 (3 ช่อง) • รอง 2 (1 ช่อง) และ Spells"
          else "Runes: Keystone (1) • Primary (3) • Secondary (1) & Spells",
        style = MaterialTheme.typography.labelMedium,
        color = SkillViolet,
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        runesById[keystoneId]?.let { ks ->
          RuneIconSlot(
            rune = ks,
            language = language,
            onClick = { activeSlotTarget = EditorSlotTarget.Keystone },
            size = 44.dp,
            badgeOverride = "★",
            modifier = Modifier.testTag("editor_slot_keystone"),
          )
        }
        minorRuneIds.take(3).forEachIndexed { idx, rId ->
          runesById[rId]?.let { mr ->
            RuneIconSlot(
              rune = mr,
              language = language,
              onClick = { activeSlotTarget = EditorSlotTarget.MinorRune(idx) },
              size = 38.dp,
              badgeOverride = "${idx + 1}",
              modifier = Modifier.testTag("editor_slot_minor_$idx"),
            )
          }
        }
        minorRuneIds.getOrNull(3)?.let { rId ->
          runesById[rId]?.let { mr ->
            RuneIconSlot(
              rune = mr,
              language = language,
              onClick = { activeSlotTarget = EditorSlotTarget.MinorRune(3) },
              size = 38.dp,
              badgeOverride = if (language == AppLanguage.TH) "รอง2" else "2nd",
              modifier = Modifier.testTag("editor_slot_minor_3"),
            )
          }
        }
        spellIds.forEachIndexed { idx, sId ->
          spellsById[sId]?.let { sp ->
            SpellIconSlot(
              spell = sp,
              language = language,
              onClick = { activeSlotTarget = EditorSlotTarget.SpellSlot(idx) },
              size = 38.dp,
              modifier = Modifier.testTag("editor_slot_spell_$idx"),
            )
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        modifier = Modifier.fillMaxWidth().testTag("custom_build_notes_input"),
        label = {
          Text(if (language == AppLanguage.TH) "โน้ตแทคติคส่วนตัว (ไม่บังคับ)" else "Tactical Notes (Optional)")
        },
        maxLines = 2,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier =
            Modifier.weight(1f).minimumInteractiveComponentSize().testTag("custom_build_cancel_button"),
        ) {
          Text(if (language == AppLanguage.TH) "ยกเลิก" else "Cancel")
        }
        Button(
          onClick = {
            val finalName =
              buildName.trim().ifBlank {
                "${selectedChamp?.nameEn ?: "Custom"} Build"
              }
            onSave(
              initialBuild.copy(
                name = finalName,
                championId = selectedChamp?.id ?: selectedChampionId,
                lane = selectedLane,
                starterItemId = starterId,
                coreItemIds = coreIds,
                bootsId = bootsId,
                enchantId = enchantId,
                keystoneId = keystoneId,
                minorRuneIds = minorRuneIds,
                spellIds = spellIds,
                notes = notes.trim(),
              )
            )
          },
          modifier =
            Modifier.weight(1f).minimumInteractiveComponentSize().testTag("custom_build_save_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = HextechGold,
              contentColor = Color(0xFF0A0F1A),
            ),
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (language == AppLanguage.TH) "บันทึกเซ็ต" else "Save Build",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
    }
  }

  if (showChampPicker) {
    ChampionPickerBottomSheet(
      title = if (language == AppLanguage.TH) "เลือกแชมเปี้ยนสำหรับเซ็ตนี้" else "Select Champion",
      champions = allChampions,
      selectedIds = setOf(selectedChampionId),
      language = language,
      onSelectChampion = { champ ->
        selectedChampionId = champ.id
        selectedLane = champ.lanes.firstOrNull() ?: selectedLane
        showChampPicker = false
      },
      onDismiss = { showChampPicker = false },
    )
  }

  when (val target = activeSlotTarget) {
    EditorSlotTarget.Starter -> {
      ItemPickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือกไอเทมเริ่มต้น" else "Select Starter Item",
        items = allItemsList,
        initialCategory = ItemCategory.STARTER,
        selectedIds = setOf(starterId),
        language = language,
        onSelectItem = { item ->
          starterId = item.id
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    is EditorSlotTarget.CoreSlot -> {
      ItemPickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือกไอเทมหลัก (Core / Counter)" else "Select Core Item",
        items = allItemsList.filter { it.category != ItemCategory.BOOTS && it.category != ItemCategory.STARTER },
        initialCategory = null,
        selectedIds = coreIds.toSet(),
        language = language,
        onSelectItem = { item ->
          coreIds =
            if (target.index == null) {
              if (item.id !in coreIds) (coreIds + item.id).take(5) else coreIds
            } else {
              coreIds.toMutableList().apply { set(target.index, item.id) }
            }
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    EditorSlotTarget.Boots -> {
      ItemPickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือกรองเท้า" else "Select Boots",
        items = allItemsList.filter { it.category == ItemCategory.BOOTS },
        initialCategory = ItemCategory.BOOTS,
        selectedIds = setOf(bootsId),
        language = language,
        onSelectItem = { item ->
          bootsId = item.id
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    EditorSlotTarget.Enchant -> {
      ItemPickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือกไอเทมกดใช้ (Active)" else "Select Active Item",
        items = allItemsList.filter { it.category == ItemCategory.ENCHANTMENT },
        initialCategory = ItemCategory.ENCHANTMENT,
        selectedIds = setOf(enchantId),
        language = language,
        onSelectItem = { item ->
          enchantId = item.id
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    EditorSlotTarget.Keystone -> {
      RunePickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือก Keystone รูนหลัก" else "Select Keystone",
        runes = allRunesList,
        keystonesOnly = true,
        selectedIds = setOf(keystoneId),
        language = language,
        onSelectRune = { rune ->
          keystoneId = rune.id
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    is EditorSlotTarget.MinorRune -> {
      val isSecondarySlot = target.index >= 3
      val primaryTree =
        minorRuneIds.take(3).firstNotNullOfOrNull { runesById[it]?.tree?.takeIf { t -> t.isNotBlank() } }
      val requiredSlot =
        if (!isSecondarySlot) "MINOR_${(target.index + 1).coerceIn(1, 3)}" else null
      val pickerTitle =
        if (isSecondarySlot) {
          if (language == AppLanguage.TH) "เลือกรูนรอง 2 (1 ช่อง ต่างสาย)" else "Select Secondary Rune (1 Slot)"
        } else {
          if (language == AppLanguage.TH) "เลือกรูนรอง 1 ช่อง ${target.index + 1}" else "Select Primary Minor Slot ${target.index + 1}"
        }
      RunePickerBottomSheet(
        title = pickerTitle,
        runes = allRunesList,
        keystonesOnly = false,
        selectedIds = setOfNotNull(minorRuneIds.getOrNull(target.index)),
        requiredSlotType = requiredSlot,
        initialTree = if (!isSecondarySlot) primaryTree else null,
        excludedTree = if (isSecondarySlot) primaryTree else null,
        language = language,
        onSelectRune = { rune ->
          val mutable = minorRuneIds.toMutableList()
          while (mutable.size <= target.index) mutable.add(rune.id)
          mutable[target.index] = rune.id
          if (!isSecondarySlot) {
            // Keep Primary Slots 1-3 coherent with the newly chosen rune's tree when switching trees
            val chosenTree = rune.tree
            if (chosenTree.isNotBlank()) {
              for (slotIdx in 0..2) {
                val cur = runesById[mutable[slotIdx]]
                if (cur == null || !cur.tree.equals(chosenTree, ignoreCase = true)) {
                  val slotName = "MINOR_${slotIdx + 1}"
                  val replacement =
                    allRunesList.firstOrNull {
                      it.tree.equals(chosenTree, ignoreCase = true) &&
                        it.slotType.equals(slotName, ignoreCase = true)
                    }
                  if (replacement != null) mutable[slotIdx] = replacement.id
                }
              }
              val secRune = runesById[mutable.getOrNull(3)]
              if (secRune != null && secRune.tree.equals(chosenTree, ignoreCase = true)) {
                val altSec =
                  allRunesList.firstOrNull {
                    !it.slotType.equals("KEYSTONE", ignoreCase = true) &&
                      !it.tree.equals(chosenTree, ignoreCase = true)
                  }
                if (altSec != null && mutable.size > 3) mutable[3] = altSec.id
              }
            }
          }
          minorRuneIds = mutable.take(4)
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    is EditorSlotTarget.SpellSlot -> {
      SpellPickerBottomSheet(
        title = if (language == AppLanguage.TH) "เลือก Summoner Spell" else "Select Summoner Spell",
        spells = allSpellsList,
        selectedIds = spellIds.toSet(),
        language = language,
        onSelectSpell = { spell ->
          val mutable = spellIds.toMutableList()
          if (target.index < mutable.size) mutable[target.index] = spell.id else mutable.add(spell.id)
          spellIds = mutable.distinct().take(2)
          activeSlotTarget = null
        },
        onDismiss = { activeSlotTarget = null },
      )
    }

    null -> Unit
  }
}

fun buildDefaultCustomDraft(champion: ChampionData, lane: Lane? = null): CustomBuildData {
  val targetLane = lane ?: champion.lanes.firstOrNull() ?: Lane.MID
  val loadout = champion.resolveForLane(targetLane)
  val suffix = if (targetLane != champion.lanes.firstOrNull()) " ${targetLane.labelEn}" else " Custom"
  return CustomBuildData(
    id = 0L,
    name = "${champion.nameEn}$suffix",
    championId = champion.id,
    lane = targetLane,
    starterItemId = loadout.starterItemId,
    coreItemIds = loadout.coreItemIds,
    bootsId = loadout.bootsId,
    enchantId = loadout.enchantId,
    keystoneId = loadout.keystoneId,
    minorRuneIds = loadout.minorRuneIds,
    spellIds = loadout.spellIds,
    notes = if (loadout.lane != champion.lanes.firstOrNull()) loadout.conditionNoteEn else "",
  )
}
