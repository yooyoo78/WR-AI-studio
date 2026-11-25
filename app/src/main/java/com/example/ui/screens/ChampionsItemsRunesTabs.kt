package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ItemCategory
import com.example.data.model.ItemData
import com.example.data.model.RuneData
import com.example.data.model.isTier3Boot
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.RuneIconSlot
import com.example.ui.components.runeSlotLabel
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.SkillViolet

@Composable
fun ChampionsItemsCatalogTab(
  filteredItems: List<ItemData>,
  searchQuery: String,
  selectedCategory: ItemCategory?,
  language: AppLanguage,
  onSearchChange: (String) -> Unit,
  onSelectCategory: (ItemCategory?) -> Unit,
  onInspectItem: (ItemData) -> Unit,
) {
  val isMiniMode = LocalInGameMiniMode.current

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("items_catalog_lazy_column"),
    contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 8.dp),
  ) {
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().testTag("items_search_input"),
        placeholder = {
          Text(if (language == AppLanguage.TH) "ค้นหาไอเทม Wild Rift..." else "Search Wild Rift items...")
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
      )
    }
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text(if (language == AppLanguage.TH) "ทั้งหมด" else "All") },
          )
        }
        items(ItemCategory.entries, key = { it.name }) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
            label = { Text(if (language == AppLanguage.TH) cat.labelTh else cat.labelEn) },
          )
        }
      }
    }
    items(filteredItems, key = { it.id }) { item ->
      HextechCard(
        modifier = Modifier.fillMaxWidth().testTag("catalog_item_${item.id}"),
        onClick = { onInspectItem(item) },
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          ItemIconSlot(
            item = item,
            language = language,
            onClick = { onInspectItem(item) },
            size = 44.dp,
            showCost = false,
            showName = false,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = item.displayName(language),
                style = MaterialTheme.typography.titleSmall,
                color = HextechGoldBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (isTier3Boot(item.id)) {
                Surface(
                  shape = RoundedCornerShape(5.dp),
                  color = HextechGold.copy(alpha = 0.2f),
                  modifier = Modifier.padding(end = 6.dp),
                ) {
                  Text(
                    text = "10:00+ T3",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HextechGoldBright,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                  )
                }
              }
              Text(
                text = "${item.cost}g",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HextechGold,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.statsSummary(language),
              style = MaterialTheme.typography.labelSmall,
              color = ArcaneCyan,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = item.passiveDesc(language),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChampionsRunesCatalogTab(
  filteredRunes: List<RuneData>,
  searchQuery: String,
  selectedTree: String?,
  language: AppLanguage,
  onSearchChange: (String) -> Unit,
  onSelectTree: (String?) -> Unit,
  onInspectRune: (RuneData) -> Unit,
) {
  val isMiniMode = LocalInGameMiniMode.current

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("runes_catalog_lazy_column"),
    contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
    verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 6.dp else 8.dp),
  ) {
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().testTag("runes_search_input"),
        placeholder = {
          Text(if (language == AppLanguage.TH) "ค้นหารูน Wild Rift..." else "Search Wild Rift runes...")
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
      )
    }
    item {
      val trees = listOf("PRECISION", "DOMINATION", "RESOLVE", "SORCERY")
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedTree == null,
            onClick = { onSelectTree(null) },
            label = { Text(if (language == AppLanguage.TH) "ทุกสาย" else "All Trees") },
          )
        }
        items(trees, key = { it }) { tree ->
          FilterChip(
            selected = selectedTree == tree,
            onClick = { onSelectTree(if (selectedTree == tree) null else tree) },
            label = { Text(tree.lowercase().replaceFirstChar { it.uppercase() }) },
          )
        }
      }
    }
    items(filteredRunes, key = { it.id }) { rune ->
      val isKeystone = rune.slotType.equals("KEYSTONE", ignoreCase = true)
      HextechCard(
        modifier = Modifier.fillMaxWidth().testTag("catalog_rune_${rune.id}"),
        accentColor = if (isKeystone) HextechGold else null,
        onClick = { onInspectRune(rune) },
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          RuneIconSlot(
            rune = rune,
            language = language,
            onClick = { onInspectRune(rune) },
            size = 44.dp,
            showLabel = false,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = rune.displayName(language),
                style = MaterialTheme.typography.titleSmall,
                color = if (isKeystone) HextechGoldBright else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              Surface(
                shape = RoundedCornerShape(5.dp),
                color =
                  if (isKeystone) HextechGold.copy(alpha = 0.2f)
                  else SkillViolet.copy(alpha = 0.16f),
              ) {
                Text(
                  text =
                    if (rune.tree.isNotBlank()) {
                      "${rune.tree.lowercase().replaceFirstChar { it.uppercase() }} • ${runeSlotLabel(rune.slotType, language)}"
                    } else {
                      runeSlotLabel(rune.slotType, language)
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isKeystone) HextechGoldBright else SkillViolet,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = rune.desc(language),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
