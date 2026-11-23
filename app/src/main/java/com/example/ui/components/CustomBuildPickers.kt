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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ItemCategory
import com.example.data.model.ItemData
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemPickerBottomSheet(
  title: String,
  items: List<ItemData>,
  initialCategory: ItemCategory? = null,
  selectedIds: Set<String> = emptySet(),
  language: AppLanguage,
  onSelectItem: (ItemData) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var query by remember { mutableStateOf("") }
  var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }

  val filtered =
    remember(items, query, selectedCategory) {
      val q = query.trim().lowercase()
      items
        .filter { item ->
          val matchesCat = selectedCategory == null || item.category == selectedCategory
          val matchesQ =
            q.isBlank() ||
              item.nameEn.lowercase().contains(q) ||
              item.nameTh.lowercase().contains(q) ||
              item.statsSummaryEn.lowercase().contains(q)
          matchesCat && matchesQ
        }
        .sortedWith(compareBy<ItemData> { it.category.ordinal }.thenBy { it.cost }.thenBy { it.nameEn })
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("item_picker_sheet"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_item_picker_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth().testTag("item_picker_search"),
        placeholder = {
          Text(if (language == AppLanguage.TH) "ค้นหาไอเทม..." else "Search item...")
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilterChip(
          selected = selectedCategory == null,
          onClick = { selectedCategory = null },
          label = { Text(if (language == AppLanguage.TH) "ทั้งหมด" else "All") },
        )
        ItemCategory.entries.forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
            label = { Text(if (language == AppLanguage.TH) cat.labelTh else cat.labelEn) },
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filtered, key = { it.id }) { item ->
          val isSelected = item.id in selectedIds
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectItem(item) }
                .testTag("pick_item_${item.id}"),
            color =
              if (isSelected) HextechGold.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceContainer,
            border =
              BorderStroke(
                1.dp,
                if (isSelected) HextechGold else MaterialTheme.colorScheme.outline,
              ),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              ItemIconSlot(
                item = item,
                language = language,
                onClick = { onSelectItem(item) },
                size = 44.dp,
                showCost = false,
                showName = false,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = item.displayName(language),
                    style = MaterialTheme.typography.titleSmall,
                    color = HextechGoldBright,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "${item.cost}g",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HextechGold,
                  )
                }
                Text(
                  text = item.statsSummary(language),
                  style = MaterialTheme.typography.labelSmall,
                  color = ArcaneCyan,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RunePickerBottomSheet(
  title: String,
  runes: List<RuneData>,
  keystonesOnly: Boolean,
  selectedIds: Set<String> = emptySet(),
  requiredSlotType: String? = null,
  initialTree: String? = null,
  excludedTree: String? = null,
  language: AppLanguage,
  onSelectRune: (RuneData) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  var selectedTree by remember(initialTree, excludedTree) {
    mutableStateOf(
      if (initialTree != null && !initialTree.equals(excludedTree, ignoreCase = true)) initialTree
      else null
    )
  }

  val availableTrees =
    remember(excludedTree) {
      listOf("PRECISION", "DOMINATION", "RESOLVE", "SORCERY")
        .filter { excludedTree == null || !it.equals(excludedTree, ignoreCase = true) }
    }

  val filtered =
    remember(runes, keystonesOnly, selectedTree, requiredSlotType, excludedTree) {
      runes
        .filter { rune ->
          val isKs = rune.slotType.equals("KEYSTONE", ignoreCase = true)
          val matchesType =
            if (keystonesOnly) isKs
            else if (requiredSlotType != null) rune.slotType.equals(requiredSlotType, ignoreCase = true)
            else !isKs
          val notExcluded =
            excludedTree == null || !rune.tree.equals(excludedTree, ignoreCase = true)
          val matchesTree = selectedTree == null || rune.tree.equals(selectedTree, ignoreCase = true)
          matchesType && notExcluded && matchesTree
        }
        .sortedWith(compareBy<RuneData> { it.tree }.thenBy { it.slotType }.thenBy { it.nameEn })
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("rune_picker_sheet"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        FilterChip(
          selected = selectedTree == null,
          onClick = { selectedTree = null },
          label = { Text(if (language == AppLanguage.TH) "ทุกสาย" else "All Trees") },
        )
        availableTrees.forEach { tree ->
          FilterChip(
            selected = selectedTree == tree,
            onClick = { selectedTree = if (selectedTree == tree) null else tree },
            label = { Text(tree.lowercase().replaceFirstChar { it.uppercase() }) },
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filtered, key = { it.id }) { rune ->
          val isSelected = rune.id in selectedIds
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectRune(rune) }
                .testTag("pick_rune_${rune.id}"),
            color =
              if (isSelected) HextechGold.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceContainer,
            border =
              BorderStroke(
                1.dp,
                if (isSelected) HextechGold else MaterialTheme.colorScheme.outline,
              ),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RuneIconSlot(
                rune = rune,
                language = language,
                onClick = { onSelectRune(rune) },
                size = 42.dp,
                showLabel = false,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${rune.displayName(language)} • ${runeSlotLabel(rune.slotType, language)}",
                  style = MaterialTheme.typography.titleSmall,
                  color = HextechGoldBright,
                )
                Text(
                  text = rune.desc(language),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellPickerBottomSheet(
  title: String,
  spells: List<SpellData>,
  selectedIds: Set<String> = emptySet(),
  language: AppLanguage,
  onSelectSpell: (SpellData) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("spell_picker_sheet"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(spells, key = { it.id }) { spell ->
          val isSelected = spell.id in selectedIds
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectSpell(spell) }
                .testTag("pick_spell_${spell.id}"),
            color =
              if (isSelected) ArcaneCyan.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.surfaceContainer,
            border =
              BorderStroke(
                1.dp,
                if (isSelected) ArcaneCyan else MaterialTheme.colorScheme.outline,
              ),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SpellIconSlot(
                spell = spell,
                language = language,
                onClick = { onSelectSpell(spell) },
                size = 42.dp,
                showLabel = false,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = spell.displayName(language),
                  style = MaterialTheme.typography.titleSmall,
                  color = ArcaneCyan,
                )
                Text(
                  text = spell.desc(language),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                )
              }
            }
          }
        }
      }
    }
  }
}
