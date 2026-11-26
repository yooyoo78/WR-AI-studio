package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.CustomBuildData
import com.example.data.model.IconSizePreset
import com.example.data.model.Lane
import com.example.data.model.tier3BootUpgradeId
import com.example.data.remote.DDragonIconResolver
import com.example.ui.components.AbilityIconBadge
import com.example.ui.components.ChampionCrestAvatar
import com.example.ui.components.CustomBuildEditorSheet
import com.example.ui.components.DamageTypeBadge
import com.example.ui.components.HextechCard
import com.example.ui.components.ItemIconSlot
import com.example.ui.components.MiniInspectHost
import com.example.ui.components.MiniInspectTarget
import com.example.ui.components.PatchBadgePill
import com.example.ui.components.SpellIconSlot
import com.example.ui.components.TierBadge
import com.example.ui.components.WildRiftRuneLoadoutStrip
import com.example.ui.components.WinRatePillsRow
import com.example.ui.components.buildDefaultCustomDraft
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.HextechGoldBright
import com.example.ui.theme.LocalIconSizePreset
import com.example.ui.theme.LocalInGameMiniMode
import com.example.ui.theme.LocalUserPrefs
import com.example.ui.theme.SkillViolet
import com.example.ui.theme.ThreatCrimson
import com.example.ui.viewmodel.ChampionsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionDetailScreen(
  champion: ChampionData,
  state: ChampionsUiState,
  onToggleFavorite: (String) -> Unit,
  onSaveCustomBuild: (CustomBuildData) -> Unit,
  onDeleteCustomBuild: (Long) -> Unit,
  onAnalyze1v5: (String, Lane) -> Unit,
  onOpenChampionDetail: (String) -> Unit = {},
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val isMiniMode = LocalInGameMiniMode.current
  val lang = LocalUserPrefs.current.language
  val isFav = champion.id in state.favoriteIds
  val champMap = state.allChampions.associateBy { it.id }
  val champCustomBuilds =
    remember(state.customBuilds, champion.id) {
      state.customBuilds.filter { it.championId == champion.id }
    }

  var selectedLane by remember(champion.id) {
    mutableStateOf(champion.lanes.firstOrNull() ?: Lane.BARON)
  }
  val laneLoadout = remember(champion, selectedLane) { champion.resolveForLane(selectedLane) }

  var miniInspectTarget by remember { mutableStateOf<MiniInspectTarget?>(null) }
  var editingCustomBuild by remember { mutableStateOf<CustomBuildData?>(null) }
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
        modifier =
          Modifier.fillMaxSize().widthIn(max = 760.dp).testTag("champion_detail_lazy_column"),
        contentPadding = PaddingValues(if (isMiniMode) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isMiniMode) 7.dp else 10.dp),
      ) {
        // Identity Header
        item {
          HextechCard(accentColor = HextechGold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              ChampionCrestAvatar(champion = champion, size = 50.dp)
              Spacer(modifier = Modifier.width(12.dp))
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
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  DamageTypeBadge(champion.damageType, lang)
                  Text(
                    text =
                      champion.lanes.joinToString("/") {
                        if (lang == AppLanguage.TH) it.labelTh else it.labelEn
                      },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            WinRatePillsRow(champion = champion, language = lang, showBanRate = true)
            Spacer(modifier = Modifier.height(6.dp))
            PatchBadgePill(
              patchVersion = champion.patchVersion,
              sourceLabel =
                if (lang == AppLanguage.TH) champion.sourceType.labelTh
                else champion.sourceType.labelEn,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onAnalyze1v5(champion.id, selectedLane) },
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
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) {
                    "วิเคราะห์ 1v5 (${selectedLane.labelTh}) • ${champion.nameTh}"
                  } else {
                    "Analyze 1v5 (${selectedLane.labelEn}) • ${champion.nameEn}"
                  },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // Skill Strip
        item {
          HextechCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (lang == AppLanguage.TH) "Skill • ลำดับอัป ${laneLoadout.skillOrder}"
                  else "Skill • Order ${laneLoadout.skillOrder}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("detail_skill_title"),
              )
              Text(
                text = if (lang == AppLanguage.TH) "กดไอคอนดูข้อมูล" else "Tap icon",
                style = MaterialTheme.typography.labelSmall,
                color = ArcaneCyan,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              modifier = Modifier.testTag("detail_skill_row"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(champion.abilities, key = { it.slot }) { ab ->
                val iconUrl = ab.iconUrl.ifBlank { resolvedAbilityIcons[ab.slot].orEmpty() }
                val abilityName = if (lang == AppLanguage.TH) ab.nameTh else ab.nameEn
                val isCompactIcons = LocalIconSizePreset.current == IconSizePreset.COMPACT
                Column(
                  modifier =
                    Modifier.width(if (isCompactIcons) 66.dp else 76.dp)
                      .minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { miniInspectTarget = MiniInspectTarget.Skill(ab, iconUrl) }
                      .padding(vertical = 4.dp, horizontal = 2.dp)
                      .testTag("detail_skill_${ab.slot}"),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  AbilityIconBadge(
                    slot = ab.slot,
                    iconUrl = iconUrl,
                    contentDescription = abilityName,
                    size = 44.dp,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = abilityName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = HextechGoldBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                  )
                  if (ab.cooldownNote.isNotBlank()) {
                    Text(
                      text = ab.cooldownNote,
                      style = MaterialTheme.typography.labelSmall,
                      color = ArcaneCyan,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }

        // Lane-Adaptive Build, Runes & Spells
        item {
          HextechCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text =
                  if (lang == AppLanguage.TH) "ไอเทม รูน และ Spell ตามเลน"
                  else "Lane Build, Runes & Spells",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = if (lang == AppLanguage.TH) "แตะไอคอนดูย่อ" else "Tap for quick box",
                style = MaterialTheme.typography.labelSmall,
                color = ArcaneCyan,
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              modifier = Modifier.testTag("detail_lane_row"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(Lane.entries, key = { it.name }) { lane ->
                val isListed = lane in champion.lanes
                val isPrimary = champion.lanes.firstOrNull() == lane
                val laneTitle = if (lang == AppLanguage.TH) lane.labelTh else lane.labelEn
                val chipLabel =
                  when {
                    isPrimary -> "$laneTitle ★"
                    isListed -> laneTitle
                    else -> if (lang == AppLanguage.TH) "$laneTitle (ปรับ)" else "$laneTitle (Alt)"
                  }
                FilterChip(
                  selected = selectedLane == lane,
                  onClick = { selectedLane = lane },
                  modifier = Modifier.testTag("detail_lane_${lane.name}"),
                  label = {
                    Text(
                      text = chipLabel,
                      style = MaterialTheme.typography.labelMedium,
                      maxLines = 1,
                    )
                  },
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color =
                if (laneLoadout.isOffMetaLane) ArcaneCyan.copy(alpha = 0.1f)
                else HextechGold.copy(alpha = 0.1f),
              border =
                BorderStroke(
                  0.75.dp,
                  if (laneLoadout.isOffMetaLane) ArcaneCyan.copy(alpha = 0.45f)
                  else HextechGold.copy(alpha = 0.45f),
                ),
              modifier = Modifier.fillMaxWidth().testTag("detail_lane_condition"),
            ) {
              Text(
                text =
                  if (lang == AppLanguage.TH) laneLoadout.conditionNoteTh
                  else laneLoadout.conditionNoteEn,
                style = MaterialTheme.typography.bodySmall,
                color = if (laneLoadout.isOffMetaLane) ArcaneCyan else HextechGoldBright,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val starterItem = state.itemsById[laneLoadout.starterItemId]
            val coreItems = laneLoadout.coreItemIds.mapNotNull { state.itemsById[it] }
            val bootsItem = state.itemsById[laneLoadout.bootsId]
            val tier3BootsItem =
              bootsItem?.let { tier3BootUpgradeId(it.id) }?.let { state.itemsById[it] }
            val enchantItem = state.itemsById[laneLoadout.enchantId]
            val keystone = state.runesById[laneLoadout.keystoneId]
            val minorRunes = laneLoadout.minorRuneIds.mapNotNull { state.runesById[it] }
            val spells = laneLoadout.spellIds.mapNotNull { state.spellsById[it] }

            Text(
              text =
                if (lang == AppLanguage.TH) "เริ่มต้น & ไอเทมหลัก (${selectedLane.labelTh}):"
                else "Starter & Core Build (${selectedLane.labelEn}):",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
              modifier = Modifier.testTag("detail_build_items_row"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              starterItem?.let { item ->
                item(key = "starter_${selectedLane.name}_${item.id}") {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { miniInspectTarget = MiniInspectTarget.Item(item) },
                    accentColor = ArcaneCyan,
                    badgeText = "Start",
                    badgeColor = ArcaneCyan,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${item.id}"),
                  )
                }
              }
              items(
                coreItems.size,
                key = { idx -> "core_${selectedLane.name}_${idx}_${coreItems[idx].id}" },
              ) { idx ->
                val item = coreItems[idx]
                ItemIconSlot(
                  item = item,
                  language = lang,
                  onClick = { miniInspectTarget = MiniInspectTarget.Item(item) },
                  accentColor = HextechGold,
                  badgeText = "#${idx + 1}",
                  badgeColor = HextechGold,
                  showCost = true,
                  showName = true,
                  modifier = Modifier.testTag("detail_item_${item.id}"),
                )
              }
              bootsItem?.let { item ->
                item(key = "boots_${selectedLane.name}_${item.id}") {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { miniInspectTarget = MiniInspectTarget.Item(item) },
                    accentColor = AdvantageEmerald,
                    badgeText = if (lang == AppLanguage.TH) "รองเท้า" else "Boots",
                    badgeColor = AdvantageEmerald,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${item.id}"),
                  )
                }
              }
              enchantItem?.let { item ->
                item(key = "enchant_${selectedLane.name}_${item.id}") {
                  ItemIconSlot(
                    item = item,
                    language = lang,
                    onClick = { miniInspectTarget = MiniInspectTarget.Item(item) },
                    accentColor = SkillViolet,
                    badgeText = "Active",
                    badgeColor = SkillViolet,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${item.id}"),
                  )
                }
              }
              tier3BootsItem?.let { t3Item ->
                item(key = "boots_t3_${selectedLane.name}_${t3Item.id}") {
                  ItemIconSlot(
                    item = t3Item,
                    language = lang,
                    onClick = { miniInspectTarget = MiniInspectTarget.Item(t3Item) },
                    accentColor = HextechGoldBright,
                    badgeText = if (lang == AppLanguage.TH) "10น.+ T3" else "10m+ T3",
                    badgeColor = HextechGold,
                    showCost = true,
                    showName = true,
                    modifier = Modifier.testTag("detail_item_${t3Item.id}"),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text =
                if (lang == AppLanguage.TH) "รูน (รูนหลัก 1 + รูนรอง1 3 + รูนรอง2 1):"
                else "Runes (1 Keystone + 3 Primary + 1 Secondary):",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            WildRiftRuneLoadoutStrip(
              keystone = keystone,
              minorRunes = minorRunes,
              language = lang,
              onInspectRune = { rune -> miniInspectTarget = MiniInspectTarget.Rune(rune) },
              tagPrefix = "detail_rune_",
            )

            if (spells.isNotEmpty()) {
              Spacer(modifier = Modifier.height(10.dp))
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
                    onClick = { miniInspectTarget = MiniInspectTarget.Spell(spell) },
                    modifier = Modifier.testTag("detail_spell_${spell.id}"),
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = { editingCustomBuild = buildDefaultCustomDraft(champion, selectedLane) },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("detail_customize_build_button"),
            ) {
              Icon(Icons.Default.Build, contentDescription = null, tint = HextechGold)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (lang == AppLanguage.TH) "จัดเซ็ตไอเทมเอง (${selectedLane.labelTh})"
                  else "Customize ${selectedLane.labelEn} Build",
                color = HextechGoldBright,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        // 3-Phase Game Plan Card
        item { ChampionGamePlanCard(champion = champion, lang = lang) }

        // Strengths & Weaknesses
        item {
          HextechCard {
            Text(
              text = if (lang == AppLanguage.TH) "จุดแข็ง & จุดอ่อน" else "Strengths & Weaknesses",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
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

        if (champCustomBuilds.isNotEmpty()) {
          item {
            Text(
              text =
                if (lang == AppLanguage.TH) "เซ็ตไอเทมของฉัน (${champCustomBuilds.size})"
                else "My Custom Builds (${champCustomBuilds.size})",
              style = MaterialTheme.typography.titleMedium,
              color = HextechGoldBright,
            )
          }
          items(champCustomBuilds, key = { "detail_cb_${it.id}" }) { cb ->
            CustomBuildCard(
              build = cb,
              champion = champion,
              itemsById = state.itemsById,
              runesById = state.runesById,
              spellsById = state.spellsById,
              language = lang,
              onEdit = { editingCustomBuild = cb },
              onDuplicate = {
                onSaveCustomBuild(cb.copy(id = 0L, name = "${cb.name} (Copy)"))
              },
              onDelete = { onDeleteCustomBuild(cb.id) },
              onInspectItem = { miniInspectTarget = MiniInspectTarget.Item(it) },
              onInspectRune = { miniInspectTarget = MiniInspectTarget.Rune(it) },
              onInspectSpell = { miniInspectTarget = MiniInspectTarget.Spell(it) },
              onOpenChampionDetail = onOpenChampionDetail,
            )
          }
        }

        // Counters & Favorable Matchups
        item {
          ChampionCountersCard(
            champion = champion,
            champMap = champMap,
            lang = lang,
            onOpenChampionDetail = onOpenChampionDetail,
          )
        }
      }
    }
  }

  editingCustomBuild?.let { draft ->
    CustomBuildEditorSheet(
      initialBuild = draft,
      allChampions = state.allChampions,
      itemsById = state.itemsById,
      runesById = state.runesById,
      spellsById = state.spellsById,
      language = lang,
      onSave = { saved ->
        onSaveCustomBuild(saved)
        editingCustomBuild = null
      },
      onDismiss = { editingCustomBuild = null },
    )
  }

  MiniInspectHost(
    target = miniInspectTarget,
    language = lang,
    onDismiss = { miniInspectTarget = null },
    itemsById = state.itemsById,
  )
}
