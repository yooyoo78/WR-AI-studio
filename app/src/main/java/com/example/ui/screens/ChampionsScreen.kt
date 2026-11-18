package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.ChampionRole
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.SpellData
import com.example.data.remote.DDragonIconResolver
import com.example.ui.components.AbilityIconBadge
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemDetailBottomSheet
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.PatchBadgePill
import com.example.ui.components.SpellDetailBottomSheet
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.TacticalMeterBar
import com.example.ui.components.TierBadge
import com.example.ui.components.WinRatePillsRow
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.ChampionSortMode
import com.example.ui.viewmodel.ChampionsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChampionsScreen(
  state: ChampionsUiState,
  onSearchChange: (String) -> Unit,
  onSelectLane: (Lane?) -> Unit,
  onSelectRole: (ChampionRole?) -> Unit,
  onToggleOnlyFavorites: (Boolean) -> Unit,
  onSortModeChange: (ChampionSortMode) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onOpenDetail: (String) -> Unit,
) {
  val strings = LocalUiStrings.current
  val lang = LocalUserPrefs.current.language

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.fillMaxWidth().testTag("champions_search_input"),
          placeholder = { Text(strings.searchChampionHint) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
        )
      }

      item {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = state.sortMode == ChampionSortMode.TIER,
            onClick = { onSortModeChange(ChampionSortMode.TIER) },
            modifier = Modifier.testTag("sort_tier_chip"),
            label = { Text(if (lang == AppLanguage.TH) "เรียงตาม Tier" else "Sort: Tier") },
          )
          FilterChip(
            selected = state.sortMode == ChampionSortMode.WIN_RATE,
            onClick = { onSortModeChange(ChampionSortMode.WIN_RATE) },
            modifier = Modifier.testTag("sort_winrate_chip"),
            label = { Text(if (lang == AppLanguage.TH) "เรียงตาม Winrate" else "Sort: Win Rate") },
          )
          FilterChip(
            selected = state.onlyFavorites,
            onClick = { onToggleOnlyFavorites(!state.onlyFavorites) },
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
            label = { Text(if (lang == AppLanguage.TH) "ตัวโปรด" else "Favorites") },
          )
          Lane.entries.forEach { lane ->
            FilterChip(
              selected = state.selectedLane == lane,
              onClick = { onSelectLane(if (state.selectedLane == lane) null else lane) },
              label = { Text(if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn) },
            )
          }
          ChampionRole.entries.forEach { role ->
            FilterChip(
              selected = state.selectedRole == role,
              onClick = { onSelectRole(if (state.selectedRole == role) null else role) },
              label = { Text(if (lang == AppLanguage.TH) role.labelTh else role.labelEn) },
            )
          }
        }
      }

      if (state.filteredChampions.isEmpty()) {
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "ไม่พบแชมเปี้ยนที่ตรงกับเงื่อนไข"
                else "No champions match the selected filters.",
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      } else {
        items(state.filteredChampions, key = { it.id }) { champ ->
          val isFav = champ.id in state.favoriteIds
          HextechCard(
            modifier = Modifier.fillMaxWidth().testTag("champ_card_${champ.id}"),
            onClick = { onOpenDetail(champ.id) },
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ChampionCrestAvatar(champion = champ, size = 48.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = champ.displayName(lang),
                    style = MaterialTheme.typography.titleMedium,
                  )
                  TierBadge(champ.tierRank)
                }
                Text(
                  text = champ.displayTitle(lang),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  DamageTypeBadge(champ.damageType, lang)
                  Text(
                    text =
                      champ.lanes.joinToString(" • ") {
                        if (lang == AppLanguage.TH) it.labelTh else it.labelEn
                      },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                WinRatePillsRow(champion = champ, language = lang)
              }
              IconButton(
                onClick = { onToggleFavorite(champ.id) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("fav_btn_${champ.id}"),
              ) {
                Icon(
                  if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                  contentDescription = "Toggle favorite",
                  tint = if (isFav) HextechGold else MaterialTheme.colorScheme.onSurfaceVariant,
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
fun ChampionDetailScreen(
  champion: ChampionData,
  state: ChampionsUiState,
  onToggleFavorite: (String) -> Unit,
  onAnalyze1v5: (String) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val lang = LocalUserPrefs.current.language
  val isFav = champion.id in state.favoriteIds
  val champMap = state.allChampions.associateBy { it.id }

  var inspectedItem by remember { mutableStateOf<ItemData?>(null) }
  var inspectedSpell by remember { mutableStateOf<SpellData?>(null) }
  var resolvedAbilityIcons by remember(champion.id) {
    mutableStateOf<Map<String, String>>(emptyMap())
  }
  LaunchedEffect(champion.id) {
    resolvedAbilityIcons = DDragonIconResolver.resolveAbilityIcons(champion.id, champion.nameEn)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(champion.displayName(lang)) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { onToggleFavorite(champion.id) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Favorite",
              tint = if (isFav) HextechGold else MaterialTheme.colorScheme.onSurface,
            )
          }
        },
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("champion_detail_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Identity Header
        item {
          HextechCard(accentColor = HextechGold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ChampionCrestAvatar(champion = champion, size = 60.dp)
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = champion.displayName(lang),
                    style = MaterialTheme.typography.headlineSmall,
                  )
                  TierBadge(champion.tierRank)
                }
                Text(
                  text = champion.displayTitle(lang),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                DamageTypeBadge(champion.damageType, lang)
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            WinRatePillsRow(champion = champion, language = lang, showBanRate = true)
            Spacer(modifier = Modifier.height(8.dp))
            PatchBadgePill(
              patchVersion = champion.patchVersion,
              sourceLabel =
                if (lang == AppLanguage.TH) champion.sourceType.labelTh
                else champion.sourceType.labelEn,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = { onAnalyze1v5(champion.id) },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("detail_analyze_button"),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = HextechGold,
                  contentColor = Color(0xFF0A0F1A),
                ),
            ) {
              Icon(Icons.Default.Security, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) "วิเคราะห์ 1v5 & จัดบิลด์แก้ทางให้ ${champion.nameTh}"
                  else "Analyze 1v5 Matchup for ${champion.nameEn}",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        // Power Spike Curve & Kit Ratings
        item {
          HextechCard(accentColor = ArcaneCyan) {
            Text(
              text =
                if (lang == AppLanguage.TH) "พลังตามช่วงเกม & ค่าความสามารถ"
                else "Power Curve & Kit Ratings",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "ต้นเกม (Early)" else "Early Power",
                valuePercent = champion.earlyPower * 10,
                barColor = ArcaneCyan,
                trailingLabel = "${champion.earlyPower}/10",
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "กลางเกม (Mid)" else "Mid Power",
                valuePercent = champion.midPower * 10,
                barColor = HextechGold,
                trailingLabel = "${champion.midPower}/10",
              )
              TacticalMeterBar(
                label = if (lang == AppLanguage.TH) "ท้ายเกม (Late)" else "Late Power",
                valuePercent = champion.latePower * 10,
                barColor = AdvantageEmerald,
                trailingLabel = "${champion.latePower}/10",
              )
            }
          }
        }

        // Abilities (P, 1, 2, 3, 4)
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "สกิลทั้งหมด (P, 1, 2, 3, 4) • ลำดับอัป ${champion.skillOrder}"
                else "Full Kit (P, 1, 2, 3, 4) • Skill Order ${champion.skillOrder}",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              champion.abilities.forEach { ab ->
                val iconUrl = ab.iconUrl.ifBlank { resolvedAbilityIcons[ab.slot].orEmpty() }
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    AbilityIconBadge(
                      slot = ab.slot,
                      iconUrl = iconUrl,
                      contentDescription = if (lang == AppLanguage.TH) ab.nameTh else ab.nameEn,
                      size = 48.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (lang == AppLanguage.TH) ab.nameTh else ab.nameEn,
                        style = MaterialTheme.typography.titleSmall,
                        color = HextechGoldBright,
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        if (ab.cooldownNote.isNotBlank()) {
                          Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = ArcaneCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ArcaneCyan.copy(alpha = 0.45f)),
                          ) {
                            Text(
                              text = "CD ${ab.cooldownNote}",
                              style = MaterialTheme.typography.labelSmall,
                              color = ArcaneCyan,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                        }
                        if (ab.costNote.isNotBlank() && ab.costNote != "0") {
                          Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = HextechGold.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, HextechGold.copy(alpha = 0.45f)),
                          ) {
                            Text(
                              text = ab.costNote,
                              style = MaterialTheme.typography.labelSmall,
                              color = HextechGoldBright,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                        }
                      }
                      Spacer(modifier = Modifier.height(5.dp))
                      Text(
                        text = if (lang == AppLanguage.TH) ab.summaryTh else ab.summaryEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Strengths & Weaknesses
        item {
          HextechCard {
            Text(
              text = if (lang == AppLanguage.TH) "จุดแข็ง & จุดอ่อน" else "Strengths & Weaknesses",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            champion.strengths(lang).forEach { s ->
              Text(
                text = "+ $s",
                style = MaterialTheme.typography.bodySmall,
                color = AdvantageEmerald,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            champion.weaknesses(lang).forEach { w ->
              Text(
                text = "− $w",
                style = MaterialTheme.typography.bodySmall,
                color = ThreatCrimson,
                modifier = Modifier.padding(vertical = 2.dp),
              )
            }
          }
        }

        // Standard Build, Runes & Spells (Interactive Icon Slots)
        item {
          HextechCard {
            Text(
              text =
                if (lang == AppLanguage.TH) "บิลด์มาตรฐาน รูน และ Spell"
                else "Standard Build, Runes & Spells",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) "กดที่รูปไอเทมหรือ Spell เพื่อดูค่าพลัง ราคา และสกิลติดตัว"
                else "Tap any item or spell icon to view stats, cost & passive details",
              style = MaterialTheme.typography.labelSmall,
              color = ArcaneCyan,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val starterItem = state.itemsById[champion.starterItemId]
            val coreItems = champion.coreItemIds.mapNotNull { state.itemsById[it] }
            val bootsItem = state.itemsById[champion.defaultBootsId]
            val enchantItem = state.itemsById[champion.defaultEnchantId]
            val keystone = state.runesById[champion.defaultKeystoneId]
            val minorRunes = champion.defaultMinorRuneIds.mapNotNull { state.runesById[it] }
            val spells = champion.defaultSpellIds.mapNotNull { state.spellsById[it] }

            Text(
              text = if (lang == AppLanguage.TH) "ไอเทมเริ่มต้น & ไอเทมหลัก (Core):" else "Starter & Core Build:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              starterItem?.let { item ->
                item(key = "starter_${item.id}") {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { inspectedItem = item },
                    accentColor = ArcaneCyan,
                    badgeText = "Start",
                    badgeColor = ArcaneCyan,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${item.id}"),
                  )
                }
              }
              items(coreItems.size, key = { idx -> "core_${idx}_${coreItems[idx].id}" }) { idx ->
                val item = coreItems[idx]
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = HextechGold,
                  badgeText = "#${idx + 1}",
                  badgeColor = HextechGold,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (lang == AppLanguage.TH) "รองเท้า & Enchant:" else "Boots & Enchant:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              bootsItem?.let { item ->
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = HextechGold,
                  badgeText = if (lang == AppLanguage.TH) "รองเท้า" else "Boots",
                  badgeColor = HextechGold,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
              enchantItem?.let { item ->
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { inspectedItem = item },
                  accentColor = ArcaneCyan,
                  badgeText = "Ench",
                  badgeColor = ArcaneCyan,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            keystone?.let { ks ->
              Text(
                text = "Keystone: ${ks.displayName(lang)}",
                style = MaterialTheme.typography.titleSmall,
                color = HextechGoldBright,
              )
              Spacer(modifier = Modifier.height(4.dp))
            }
            if (minorRunes.isNotEmpty()) {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                minorRunes.forEach { rune ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, SkillViolet.copy(alpha = 0.5f)),
                  ) {
                    Text(
                      text = rune.displayName(lang),
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }

            if (spells.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Summoner Spells:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                spells.forEach { spell ->
                  SpellIconSlot(
                    spell = spell,
                    language = lang,
                    onClick = { inspectedSpell = spell },
                    modifier = Modifier.testTag("detail_spell_${spell.id}"),
                  )
                }
              }
            }
          }
        }

        // Counters & Favorable Matchups
        item {
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              champion.counterChampionIds.mapNotNull { champMap[it] }.forEach { c ->
                Surface(
                  color = ThreatCrimson.copy(alpha = 0.16f),
                  shape = MaterialTheme.shapes.small,
                  border = BorderStroke(1.dp, ThreatCrimson.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = c.displayName(lang),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = if (lang == AppLanguage.TH) "ชนะทาง:" else "Strong Against:",
              style = MaterialTheme.typography.labelMedium,
              color = AdvantageEmerald,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              champion.favorableAgainstIds.mapNotNull { champMap[it] }.forEach { c ->
                Surface(
                  color = AdvantageEmerald.copy(alpha = 0.16f),
                  shape = MaterialTheme.shapes.small,
                  border = BorderStroke(1.dp, AdvantageEmerald.copy(alpha = 0.45f)),
                ) {
                  Text(
                    text = c.displayName(lang),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  inspectedItem?.let { item ->
    ItemDetailBottomSheet(
      item = item,
      language = lang,
      onDismiss = { inspectedItem = null },
    )
  }

  inspectedSpell?.let { spell ->
    SpellDetailBottomSheet(
      spell = spell,
      language = lang,
      onDismiss = { inspectedSpell = null },
    )
  }
}
