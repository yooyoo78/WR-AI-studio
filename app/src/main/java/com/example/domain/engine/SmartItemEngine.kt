package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.ThreatTag

data class ItemRecommendationEntry(
  val item: ItemData,
  val priorityLabelEn: String,
  val priorityLabelTh: String,
  val isAdapted: Boolean,
  val replacedStandardItemName: String?,
  val reasonEn: String,
  val reasonTh: String,
  val targetThreatEn: String,
  val targetThreatTh: String,
  val buyTimingEn: String,
  val buyTimingTh: String,
  val tacticalBenefitEn: String,
  val tacticalBenefitTh: String,
) {
  fun priorityLabel(lang: AppLanguage) =
    if (lang == AppLanguage.TH) priorityLabelTh else priorityLabelEn

  fun reason(lang: AppLanguage) = if (lang == AppLanguage.TH) reasonTh else reasonEn

  fun targetThreat(lang: AppLanguage) =
    if (lang == AppLanguage.TH) targetThreatTh else targetThreatEn

  fun buyTiming(lang: AppLanguage) = if (lang == AppLanguage.TH) buyTimingTh else buyTimingEn

  fun tacticalBenefit(lang: AppLanguage) =
    if (lang == AppLanguage.TH) tacticalBenefitTh else tacticalBenefitEn
}

data class BuildDiffSummary(
  val standardCoreItems: List<ItemData>,
  val adaptedCoreItems: List<ItemData>,
  val swappedInItems: List<ItemData>,
  val swappedOutItems: List<ItemData>,
  val adaptationHeadlineEn: String,
  val adaptationHeadlineTh: String,
)

data class SmartBuildRecommendation(
  val starterItem: ItemData?,
  val firstRushRecommendation: ItemRecommendationEntry,
  val prioritizedItems: List<ItemRecommendationEntry>,
  val recommendedBoots: ItemRecommendationEntry,
  val recommendedTier3Boots: ItemRecommendationEntry? = null,
  val recommendedEnchant: ItemRecommendationEntry,
  val buildDiff: BuildDiffSummary,
  val keystone: RuneData?,
  val minorRunes: List<RuneData>,
  val spells: List<SpellData>,
  val runeSpellReasonEn: String,
  val runeSpellReasonTh: String,
  val earlyGamePlanEn: String,
  val earlyGamePlanTh: String,
  val midGamePlanEn: String,
  val midGamePlanTh: String,
  val lateGamePlanEn: String,
  val lateGamePlanTh: String,
  val skillOrder: String = "",
  val laneConditionNoteEn: String = "",
  val laneConditionNoteTh: String = "",
)

class SmartItemEngine {
  fun buildRecommendation(
    myChampion: ChampionData,
    myLane: Lane,
    enemies: List<ChampionData>,
    matchups: List<EnemyMatchupAnalysis>,
    profile: EnemyTeamThreatProfile,
    allItems: List<ItemData>,
    allRunes: List<RuneData>,
    allSpells: List<SpellData>,
  ): SmartBuildRecommendation {
    val itemMap = allItems.associateBy { it.id }
    val runeMap = allRunes.associateBy { it.id }
    val spellMap = allSpells.associateBy { it.id }

    val laneLoadout = myChampion.resolveForLane(myLane)
    val starterItem = itemMap[laneLoadout.starterItemId] ?: itemMap[myChampion.starterItemId]
    val standardCore = laneLoadout.coreItemIds.mapNotNull { itemMap[it] }
    val isTankOrFrontline =
      ChampionRole.TANK in myChampion.roles && myChampion.damageType != DamageType.AD
    val isApChampion = myChampion.damageType == DamageType.AP && !isTankOrFrontline
    val isMarksman = ChampionRole.MARKSMAN in myChampion.roles
    val isSupportEnchanter =
      myLane == Lane.SUPPORT && ChampionRole.TANK !in myChampion.roles

    // 1. Evaluate Boots adaptation (Tier 2 + 10:00+ Tier 3 Upgrade)
    val recommendedBoots =
      selectAdaptiveBoots(myChampion, laneLoadout.bootsId, profile, itemMap)
    val recommendedTier3Boots =
      com.example.data.model.tier3BootUpgradeId(recommendedBoots.item.id)?.let { t3Id ->
        itemMap[t3Id]?.let { t3Item ->
          ItemRecommendationEntry(
            item = t3Item,
            priorityLabelEn = "10:00+ Tier 3 Boot",
            priorityLabelTh = "อัปเกรดรองเท้า 10:00+",
            isAdapted = recommendedBoots.isAdapted,
            replacedStandardItemName = null,
            reasonEn =
              "Unlocks at 10:00+ (+1000g from ${recommendedBoots.item.nameEn}). Upgrades stats and adds ${t3Item.passiveNameEn}.",
            reasonTh =
              "ปลดล็อกนาทีที่ 10:00+ (อัปจาก ${recommendedBoots.item.nameTh} +1000g) เพิ่มค่าพลังและได้ความสามารถ ${t3Item.passiveNameTh}",
            targetThreatEn = recommendedBoots.targetThreatEn,
            targetThreatTh = recommendedBoots.targetThreatTh,
            buyTimingEn = "After 10:00 in-game (+1000g, typically after 2–3 Core items)",
            buyTimingTh = "หลังนาทีที่ 10:00+ ในเกม (+1000g มักอัปหลังครบ 2–3 ชิ้นหลัก)",
            tacticalBenefitEn = t3Item.passiveDescEn,
            tacticalBenefitTh = t3Item.passiveDescTh,
          )
        }
      }

    // 2. Evaluate Enchantment adaptation
    val recommendedEnchant =
      selectAdaptiveEnchant(myChampion, laneLoadout.enchantId, myLane, enemies, profile, itemMap)

    // 3. Evaluate Situational Counter Injections (multi-threat aware!)
    val counterInjections = mutableListOf<ItemRecommendationEntry>()
    val alreadyChosenIds = mutableSetOf<String>()

    // Anti-Heal Rule
    if (profile.healingScore >= 34) {
      val names = profile.healingThreats.joinToString(", ") { it.nameEn }
      val antiHealItem =
        when {
          isTankOrFrontline -> itemMap["thornmail"]
          isApChampion || isSupportEnchanter -> itemMap["morellonomicon"]
          isMarksman || myChampion.damageType == DamageType.TRUE -> itemMap["mortal_reminder"]
          else -> itemMap["chempunk_chainsword"]
        }
      if (antiHealItem != null && alreadyChosenIds.add(antiHealItem.id)) {
        val urgent = profile.healingScore >= 60 ||
          profile.healingThreats.any { it.id in enemies.filter { e -> myLane in e.lanes }.map { e -> e.id } }
        counterInjections.add(
          ItemRecommendationEntry(
            item = antiHealItem,
            priorityLabelEn = if (urgent) "Rush Counter" else "Situational Counter",
            priorityLabelTh = if (urgent) "ออกแก้ทางด่วน" else "ไอเทมแก้ทาง",
            isAdapted = true,
            replacedStandardItemName = null,
            reasonEn = "Enemy composition packs heavy sustain/healing ($names). Applying Grievous Wounds cuts their effective health drastically.",
            reasonTh = "ศัตรูมีการฮีลและดูดเลือดสูง ($names) การติดตัดฮีลจะลดความอึดและโอกาสพลิกไฟต์ของศัตรูลงอย่างมาก",
            targetThreatEn = "Healing Threat: $names",
            targetThreatTh = "แก้ทางตัวฮีล: $names",
            buyTimingEn = if (urgent) "Buy component after Boots / 1st Core" else "2nd or 3rd Full Item",
            buyTimingTh = if (urgent) "ขึ้นชิ้นส่วนตัดฮีลหลังรองเท้าหรือชิ้น 1" else "ออกเป็นชิ้นที่ 2-3",
            tacticalBenefitEn = "Reduces enemy healing by 40–60% in lane duels and Dragon skirmishes.",
            tacticalBenefitTh = "ลดพลังฮีลศัตรู 40–60% ทำให้ปิดคิลในเลนและทีมไฟต์ง่ายขึ้นมาก",
          )
        )
      }
    }

    // High-HP Tank / Armor-Stacking Rule
    if (profile.tankinessScore >= 40) {
      val names = profile.tankThreats.joinToString(", ") { it.nameEn }
      val antiTankItem =
        when {
          isApChampion -> itemMap["liandrys_torment"] ?: itemMap["riftmaker"] ?: itemMap["oceanids_trident"]
          isMarksman -> itemMap["terminus"] ?: itemMap["botrk"] ?: itemMap["mortal_reminder"]
          isTankOrFrontline -> null
          myChampion.damageType == DamageType.AD ->
            itemMap["divine_sunderer"] ?: itemMap["black_cleaver"] ?: itemMap["sundered_sky"] ?: itemMap["seryldas_grudge"]
          else -> itemMap["riftmaker"]
        }
      if (antiTankItem != null && alreadyChosenIds.add(antiTankItem.id)) {
        counterInjections.add(
          ItemRecommendationEntry(
            item = antiTankItem,
            priorityLabelEn = "Anti-Tank Core",
            priorityLabelTh = "เจาะเกราะ/ละลายแทงค์",
            isAdapted = antiTankItem.id !in laneLoadout.coreItemIds,
            replacedStandardItemName = null,
            reasonEn = "Enemy frontline ($names) stacks high HP and resistances. Percentage-HP or penetration prevents your damage from stalling.",
            reasonTh = "แนวหน้าศัตรู ($names) เลือดและเกราะหนา ต้องใช้ดาเมจ % เลือดหรือเจาะเกราะเพื่อไม่ให้ดาเมจตันกลางเกม",
            targetThreatEn = "Tank Frontline: $names",
            targetThreatTh = "ละลายแทงค์: $names",
            buyTimingEn = "2nd or 3rd Item before major Dragon/Baron fights",
            buyTimingTh = "ออกเป็นชิ้นที่ 2-3 ก่อนไฟต์ชิงมังกร/บารอนใหญ่",
            tacticalBenefitEn = "Breaks through Heartsteel/Amaranth stacking so you win front-to-back fights.",
            tacticalBenefitTh = "เจาะทะลุ Heartsteel และ Amaranth ช่วยให้ทีมชนะไฟต์ปะทะตรงหน้า",
          )
        )
      }
    }

    // High AP Burst Rule
    if (profile.apBurstScore >= 45) {
      val names = profile.apThreats.joinToString(", ") { it.nameEn }
      val antiApItem =
        when {
          isTankOrFrontline ->
            if (profile.apBurstScore >= 60) {
              itemMap["kaenic_rookern"] ?: itemMap["force_of_nature"] ?: itemMap["amaranths_twinguard"]
            } else {
              itemMap["force_of_nature"] ?: itemMap["kaenic_rookern"] ?: itemMap["abyssal_mask"]
            }
          isApChampion || isSupportEnchanter -> itemMap["banshees_veil"]
          isMarksman -> itemMap["wits_end"] ?: itemMap["maw_of_malmortius"]
          else -> itemMap["maw_of_malmortius"]
        }
      if (antiApItem != null && alreadyChosenIds.add(antiApItem.id)) {
        counterInjections.add(
          ItemRecommendationEntry(
            item = antiApItem,
            priorityLabelEn = "Anti-AP Defense",
            priorityLabelTh = "กันเบิร์สเวท",
            isAdapted = antiApItem.id !in laneLoadout.coreItemIds,
            replacedStandardItemName = null,
            reasonEn = "High enemy AP burst ($names) threatens one-shot combos without Magic Resist or spell-shield protection.",
            reasonTh = "ฝั่งศัตรูมีเบิร์สเวทแรง ($names) เสี่ยงโดนคอมโบชุดเดียวหายหากไม่มีต้านเวทหรือโล่กันสกิล",
            targetThreatEn = "AP Burst: $names",
            targetThreatTh = "กันเบิร์สเวท: $names",
            buyTimingEn = "2nd or 3rd Item as AP carries hit 2-item spike",
            buyTimingTh = "ออกช่วงชิ้นที่ 2-3 เมื่อเมจศัตรูเริ่มมีดาเมจ",
            tacticalBenefitEn = "Absorbs magic burst and preserves uptime to counter-attack.",
            tacticalBenefitTh = "รอดจากชุดเบิร์สเวทและมีโอกาสรอดกลับมาทำดาเมจต่อ",
          )
        )
      }
    }

    // High AD / Crit / Attack Speed Rule
    if (profile.adDpsScore >= 55 || profile.critScore >= 38 || profile.attackSpeedScore >= 60) {
      val names =
        (profile.critThreats + profile.attackSpeedThreats + profile.adThreats)
          .distinctBy { it.id }
          .take(3)
          .joinToString(", ") { it.nameEn }
      val antiAdItem =
        when {
          isTankOrFrontline && profile.critScore >= 38 -> itemMap["randuins_omen"]
          isTankOrFrontline && profile.attackSpeedScore >= 32 -> itemMap["frozen_heart"]
          isTankOrFrontline -> itemMap["thornmail"]
          isMarksman -> itemMap["guardian_angel"]
          !isApChampion -> itemMap["deaths_dance"]
          else -> itemMap["stasis_enchant"] ?: itemMap["banshees_veil"]
        }
      if (antiAdItem != null && alreadyChosenIds.add(antiAdItem.id)) {
        counterInjections.add(
          ItemRecommendationEntry(
            item = antiAdItem,
            priorityLabelEn = "Anti-AD / AS",
            priorityLabelTh = "กัน AD / ตีไว / คริ",
            isAdapted = antiAdItem.id !in laneLoadout.coreItemIds,
            replacedStandardItemName = null,
            reasonEn = "Enemy physical DPS/Crit/AS ($names) will shred unarmored targets in sustained fights.",
            reasonTh = "ศัตรูมีดาเมจกายภาพ/ตีไว/คริสูง ($names) ต้องลดความเร็วโจมตีหรือหน่วงดาเมจกายภาพ",
            targetThreatEn = "Physical DPS: $names",
            targetThreatTh = "แก้ทางกายภาพ/คริ: $names",
            buyTimingEn = "3rd or 4th Item before late-game teamfights",
            buyTimingTh = "ออกเป็นชิ้นที่ 3-4 ก่อนเข้าสู่ทีมไฟต์ท้ายเกม",
            tacticalBenefitEn = "Cuts enemy DPS output and prevents instant physical resets.",
            tacticalBenefitTh = "ลดดาเมจต่อเนื่องของแครี่และไฟต์เตอร์ฝั่งศัตรูอย่างชัดเจน",
          )
        )
      }
    }

    // Merge Standard Core + Counter Injections into a crisp 4-item Full Core + Boots + Enchant
    val adaptedCoreList = mutableListOf<ItemData>()
    val entries = mutableListOf<ItemRecommendationEntry>()

    // Choose 1st Rush item: either an urgent lane counter (like Divine Sunderer vs Mundo/Malphite or Frozen Heart vs full AD) or standard core[0]
    val firstStandard = standardCore.firstOrNull()
    val urgentFirst =
      counterInjections.firstOrNull {
        it.item.category == ItemCategory.CORE ||
          (isTankOrFrontline && it.item.id in listOf("frozen_heart", "thornmail", "kaenic_rookern", "force_of_nature"))
      }

    val rushItem =
      if (urgentFirst != null && urgentFirst.item.id != firstStandard?.id) {
        urgentFirst.item
      } else {
        firstStandard ?: counterInjections.firstOrNull()?.item ?: allItems.first()
      }

    val rushIsAdapted = rushItem.id != firstStandard?.id
    val rushEntry =
      if (rushIsAdapted && urgentFirst != null) {
        urgentFirst.copy(
          priorityLabelEn = "Buy 1st (Adapted)",
          priorityLabelTh = "ออกชิ้นแรก (ปรับแก้ทาง)",
          replacedStandardItemName = firstStandard?.nameEn,
          buyTimingEn = "First Full Item (Minute 3–6)",
          buyTimingTh = "ออกเป็นไอเทมใหญ่ชิ้นแรก (นาที 3–6)",
        )
      } else {
        ItemRecommendationEntry(
          item = rushItem,
          priorityLabelEn = "Buy 1st (Power Spike)",
          priorityLabelTh = "ออกชิ้นแรก (Core Spike)",
          isAdapted = false,
          replacedStandardItemName = null,
          reasonEn = "${rushItem.nameEn} unlocks ${myChampion.nameEn}'s primary laning and skirmish spike.",
          reasonTh = "${rushItem.nameTh} คือแกนหลักที่ปลดล็อกเพาเวอร์สไปก์ช่วงต้น-กลางเกมของ ${myChampion.nameTh}",
          targetThreatEn = "General Lane & Mid-Game Tempo",
          targetThreatTh = "คุมจังหวะเลนและกลางเกม",
          buyTimingEn = "First Full Item (Minute 3–6)",
          buyTimingTh = "ออกเป็นไอเทมใหญ่ชิ้นแรก (นาที 3–6)",
          tacticalBenefitEn = rushItem.passiveDescEn,
          tacticalBenefitTh = rushItem.passiveDescTh,
        )
      }

    adaptedCoreList.add(rushItem)
    entries.add(rushEntry)

    // Add Boots right after or alongside 1st item
    entries.add(recommendedBoots)

    // Fill slots 2, 3, 4 by prioritizing counterInjections (not yet added) then remaining standardCore
    val remainingCounters = counterInjections.filter { it.item.id != rushItem.id }
    val remainingStandard = standardCore.filter { it.id != rushItem.id }

    var slotNum = 2
    remainingStandard.take(1).forEach { stdItem ->
      if (adaptedCoreList.none { it.id == stdItem.id }) {
        adaptedCoreList.add(stdItem)
        entries.add(
          ItemRecommendationEntry(
            item = stdItem,
            priorityLabelEn = "Core #$slotNum",
            priorityLabelTh = "ไอเทมหลัก #$slotNum",
            isAdapted = false,
            replacedStandardItemName = null,
            reasonEn = "Synergizes directly with ${myChampion.nameEn}'s kit and 1st item spike.",
            reasonTh = "เสริมคอมโบสกิลและพลังต่อเนื่องจากไอเทมชิ้นแรกของ ${myChampion.nameTh}",
            targetThreatEn = "Core Kit Synergy",
            targetThreatTh = "เสริมพลังหลักของแชมเปี้ยน",
            buyTimingEn = "2nd Full Item (Minute 7–10)",
            buyTimingTh = "ไอเทมชิ้นที่ 2 (นาที 7–10)",
            tacticalBenefitEn = stdItem.passiveDescEn,
            tacticalBenefitTh = stdItem.passiveDescTh,
          )
        )
        slotNum++
      }
    }

    // Add Enchantment timing
    entries.add(recommendedEnchant)

    // Inject adapted counter items
    remainingCounters.forEach { counterEntry ->
      if (adaptedCoreList.size < 4 && adaptedCoreList.none { it.id == counterEntry.item.id }) {
        val replaced = remainingStandard.getOrNull(slotNum - 1)
        adaptedCoreList.add(counterEntry.item)
        entries.add(
          counterEntry.copy(
            priorityLabelEn = "Adapted #$slotNum",
            priorityLabelTh = "ปรับแก้ทาง #$slotNum",
            replacedStandardItemName = replaced?.nameEn,
          )
        )
        slotNum++
      }
    }

    // Fill remaining up to 4 core/situational items from standardCore
    remainingStandard.forEach { stdItem ->
      if (adaptedCoreList.size < 4 && adaptedCoreList.none { it.id == stdItem.id }) {
        adaptedCoreList.add(stdItem)
        entries.add(
          ItemRecommendationEntry(
            item = stdItem,
            priorityLabelEn = "Core #$slotNum",
            priorityLabelTh = "ไอเทมหลัก #$slotNum",
            isAdapted = false,
            replacedStandardItemName = null,
            reasonEn = "Completes your late-game scaling profile.",
            reasonTh = "เติมเต็มพลังช่วงกลาง-ท้ายเกมตามจุดเด่นของแชมเปี้ยน",
            targetThreatEn = "Late-Game Scaling",
            targetThreatTh = "เสริมพลังท้ายเกม",
            buyTimingEn = "Late-Game Slot #$slotNum",
            buyTimingTh = "ช่วงท้ายเกม (ชิ้นที่ $slotNum)",
            tacticalBenefitEn = stdItem.passiveDescEn,
            tacticalBenefitTh = stdItem.passiveDescTh,
          )
        )
        slotNum++
      }
    }

    recommendedTier3Boots?.let { entries.add(it) }

    val swappedIn = adaptedCoreList.filter { item -> standardCore.none { it.id == item.id } }
    val swappedOut = standardCore.filter { item -> adaptedCoreList.none { it.id == item.id } }

    val headlineEn =
      if (swappedIn.isEmpty()) {
        "Standard build remains optimal — focus on ${recommendedBoots.item.nameEn} + ${recommendedEnchant.item.nameEn} timing."
      } else {
        "Swapped in ${swappedIn.joinToString { it.nameEn }} to counter enemy threats."
      }
    val headlineTh =
      if (swappedIn.isEmpty()) {
        "บิลด์มาตรฐานเหมาะสมแล้ว เน้นปรับรองเท้าเป็น ${recommendedBoots.item.nameTh} และ ${recommendedEnchant.item.nameTh}"
      } else {
        "ปรับเพิ่ม ${swappedIn.joinToString { it.nameTh }} จากบิลด์มาตรฐานเพื่อแก้ทางภัยคุกคามศัตรู"
      }

    // 4. Adaptive Runes & Summoner Spells
    val (keystone, minors, spells, runeReasonEn, runeReasonTh) =
      selectAdaptiveRunesAndSpells(myChampion, myLane, laneLoadout, profile, runeMap, spellMap)

    // 5. Early / Mid / Late Game Plan tailored to enemies
    val topThreat = matchups.firstOrNull()?.enemy
    val earlyExtraEn =
      if (enemies.any { it.earlyPower >= 9 }) {
        " Respect early spikes from ${enemies.filter { it.earlyPower >= 9 }.joinToString { it.nameEn }} before level 5."
      } else ""
    val earlyExtraTh =
      if (enemies.any { it.earlyPower >= 9 }) {
        " ระวังแรงกดดันต้นเกมจาก ${enemies.filter { it.earlyPower >= 9 }.joinToString { it.nameTh }} ช่วงเลเวล 1–5"
      } else ""

    val midExtraEn =
      if (profile.healingScore >= 34) {
        " Prioritize Anti-Heal before contesting 2nd Dragon."
      } else if (profile.diveScore >= 35) {
        " Group around vision and complete ${recommendedEnchant.item.nameEn} before mid-game skirmishes."
      } else ""
    val midExtraTh =
      if (profile.healingScore >= 34) {
        " รีบทำไอเทมตัดฮีลให้พร้อมก่อนไฟต์มังกรตัวที่ 2"
      } else if (profile.diveScore >= 35) {
        " เกาะกลุ่มคุมวิชั่นและขึ้น ${recommendedEnchant.item.nameTh} ก่อนไฟต์กลางเกม"
      } else ""

    val lateExtraEn =
      if (topThreat != null) {
        " Track #1 threat ${topThreat.nameEn}'s cooldowns before committing."
      } else ""
    val lateExtraTh =
      if (topThreat != null) {
        " จับตาคูลดาวน์ของ ${topThreat.nameTh} (ภัยคุกคามอันดับ 1) ก่อนตัดสินใจเปิดไฟต์"
      } else ""

    return SmartBuildRecommendation(
      starterItem = starterItem,
      firstRushRecommendation = rushEntry,
      prioritizedItems = entries,
      recommendedBoots = recommendedBoots,
      recommendedTier3Boots = recommendedTier3Boots,
      recommendedEnchant = recommendedEnchant,
      buildDiff =
        BuildDiffSummary(
          standardCoreItems = standardCore,
          adaptedCoreItems = adaptedCoreList,
          swappedInItems = swappedIn,
          swappedOutItems = swappedOut,
          adaptationHeadlineEn = headlineEn,
          adaptationHeadlineTh = headlineTh,
        ),
      keystone = keystone,
      minorRunes = minors,
      spells = spells,
      runeSpellReasonEn = runeReasonEn,
      runeSpellReasonTh = runeReasonTh,
      earlyGamePlanEn = myChampion.earlyPlanEn + earlyExtraEn,
      earlyGamePlanTh = myChampion.earlyPlanTh + earlyExtraTh,
      midGamePlanEn = myChampion.midPlanEn + midExtraEn,
      midGamePlanTh = myChampion.midPlanTh + midExtraTh,
      lateGamePlanEn = myChampion.latePlanEn + lateExtraEn,
      lateGamePlanTh = myChampion.latePlanTh + lateExtraTh,
      skillOrder = laneLoadout.skillOrder,
      laneConditionNoteEn = laneLoadout.conditionNoteEn,
      laneConditionNoteTh = laneLoadout.conditionNoteTh,
    )
  }

  private fun selectAdaptiveBoots(
    myChamp: ChampionData,
    laneBootsId: String,
    profile: EnemyTeamThreatProfile,
    itemMap: Map<String, ItemData>,
  ): ItemRecommendationEntry {
    val defaultBoots = itemMap[laneBootsId] ?: itemMap[myChamp.defaultBootsId] ?: itemMap["plated_steelcaps"]!!
    val isTankOrFighter =
      ChampionRole.TANK in myChamp.roles || ChampionRole.FIGHTER in myChamp.roles

    val chosen: ItemData
    val reasonEn: String
    val reasonTh: String
    val targetEn: String
    val targetTh: String

    when {
      profile.ccScore >= 60 || (profile.apBurstScore >= 65 && isTankOrFighter) -> {
        chosen = itemMap["mercurys_treads"] ?: defaultBoots
        reasonEn = "Enemy team packs heavy Crowd Control and/or Magic Burst—Mercury's Treads grants +150 HP, +25 MR, and +30% Tenacity."
        reasonTh = "ฝั่งศัตรูมี CC ล็อกและดาเมจเวทสูง Mercury's Treads เพิ่ม +150 เลือด, +25 ต้านเวท และลดเวลาติด CC 30%"
        targetEn = (profile.ccThreats + profile.apThreats).distinctBy { it.id }.take(3).joinToString { it.nameEn }.ifBlank { "Heavy CC & AP" }
        targetTh = (profile.ccThreats + profile.apThreats).distinctBy { it.id }.take(3).joinToString { it.nameTh }.ifBlank { "CC และเวท" }
      }
      profile.adDpsScore >= 65 && (isTankOrFighter || profile.diveScore >= 35) -> {
        chosen = itemMap["plated_steelcaps"] ?: defaultBoots
        reasonEn = "Strong physical DPS & auto-attackers on enemy team—Plated Steelcaps grants +150 HP, +25 Armor, and cuts champion basic attacks by 10%."
        reasonTh = "ศัตรูเน้นการโจมตีปกติและดาเมจกายภาพสูง Plated Steelcaps เพิ่ม +150 เลือด, +25 เกราะ และลดดาเมจตีปกติจากแชมเปี้ยน 10%"
        targetEn = profile.adThreats.take(3).joinToString { it.nameEn }.ifBlank { "Heavy AD" }
        targetTh = profile.adThreats.take(3).joinToString { it.nameTh }.ifBlank { "AD หนัก" }
      }
      else -> {
        chosen = defaultBoots
        reasonEn = "Optimal offensive/tempo boots matching ${myChamp.nameEn}'s power curve."
        reasonTh = "รองเท้ามาตรฐานที่เข้ากับจังหวะทำดาเมจและการเดินเกมของ ${myChamp.nameTh}"
        targetEn = "Lane Tempo & Kit Synergy"
        targetTh = "เร่งสไปก์ตามชุดสกิล"
      }
    }

    return ItemRecommendationEntry(
      item = chosen,
      priorityLabelEn = if (chosen.id != defaultBoots.id) "Boots (Adapted)" else "Boots",
      priorityLabelTh = if (chosen.id != defaultBoots.id) "รองเท้า (ปรับแก้ทาง)" else "รองเท้าแนะนำ",
      isAdapted = chosen.id != defaultBoots.id,
      replacedStandardItemName = if (chosen.id != defaultBoots.id) defaultBoots.nameEn else null,
      reasonEn = reasonEn,
      reasonTh = reasonTh,
      targetThreatEn = targetEn,
      targetThreatTh = targetTh,
      buyTimingEn = "Complete Tier-2 Boots right after or before 1st Core",
      buyTimingTh = "อัปรองเท้าขั้น 2 ช่วงก่อนหรือหลังไอเทมชิ้นแรก",
      tacticalBenefitEn = chosen.passiveDescEn,
      tacticalBenefitTh = chosen.passiveDescTh,
    )
  }

  private fun selectAdaptiveEnchant(
    myChamp: ChampionData,
    laneEnchantId: String,
    myLane: Lane,
    enemies: List<ChampionData>,
    profile: EnemyTeamThreatProfile,
    itemMap: Map<String, ItemData>,
  ): ItemRecommendationEntry {
    val defaultEnchant = itemMap[laneEnchantId] ?: itemMap[myChamp.defaultEnchantId] ?: itemMap["stasis_enchant"]!!
    val isSquishyCarry =
      ChampionRole.MARKSMAN in myChamp.roles ||
        ChampionRole.MAGE in myChamp.roles ||
        myChamp.tankinessRating <= 4

    val isEnchanterSupport =
      myLane == Lane.SUPPORT && ChampionRole.TANK !in myChamp.roles
    val hasPointClickBurst = enemies.any { it.id in listOf("zed", "evelynn", "syndra", "vi", "camille") }
    val hasCriticalLockdown =
      enemies.any { it.id in listOf("rammus", "leona", "nautilus", "thresh", "lux", "ahri") }

    val chosen: ItemData
    val reasonEn: String
    val reasonTh: String
    val targetEn: String
    val targetTh: String

    when {
      isSquishyCarry && hasPointClickBurst -> {
        chosen = itemMap["stasis_enchant"] ?: defaultEnchant
        reasonEn = "Zhonya's Stasis nullifies point-and-click assassin/burst ultimates (Zed (R), Syndra (R), Evelynn (R))."
        reasonTh = "Zhonya's (Stasis) กดเป็นอมตะ 2.5 วิ หลบอัลติล็อกเป้าสายเบิร์สอย่าง Zed, Syndra, Evelynn ได้ทันที"
        targetEn = enemies.filter { it.id in listOf("zed", "evelynn", "syndra", "vi", "camille") }.joinToString { it.nameEn }
        targetTh = enemies.filter { it.id in listOf("zed", "evelynn", "syndra", "vi", "camille") }.joinToString { it.nameTh }
      }
      isEnchanterSupport && profile.ccScore >= 55 && hasCriticalLockdown -> {
        chosen = itemMap["mikaels_blessing"] ?: itemMap["locket_enchant"] ?: defaultEnchant
        reasonEn = "Enemy team stacks lethal lockdown CC—Mikael's Blessing cleanses and heals your carry immediately."
        reasonTh = "ศัตรูมี CC จับตายหลายสกิล Mikael's Blessing ช่วยล้างสถานะและฮีลแครี่ให้รอดในทีมไฟต์"
        targetEn = profile.ccThreats.take(3).joinToString { it.nameEn }.ifBlank { "Hard CC" }
        targetTh = profile.ccThreats.take(3).joinToString { it.nameTh }.ifBlank { "CC หนัก" }
      }
      profile.ccScore >= 55 && hasCriticalLockdown && ChampionRole.TANK !in myChamp.roles -> {
        chosen = itemMap["quicksilver_enchant"] ?: defaultEnchant
        reasonEn = "Enemy team relies on lethal CC chains—Quicksilver Sash cleanses stuns/taunts immediately so you keep dealing DPS."
        reasonTh = "ศัตรูมี CC จับตายหลายสกิล Quicksilver Sash ล้างสตั๊น/ยั่วยุทันทีเพื่อไม่ให้โดนจับตาย"
        targetEn = profile.ccThreats.take(3).joinToString { it.nameEn }.ifBlank { "Hard CC" }
        targetTh = profile.ccThreats.take(3).joinToString { it.nameTh }.ifBlank { "CC หนัก" }
      }
      myChamp.id in listOf("darius", "sett", "rammus") && profile.diveScore == 0 -> {
        chosen = itemMap["glory_enchant"] ?: defaultEnchant
        reasonEn = "Shurelya's Battlesong closes the gap onto backline carries who want to kite you."
        reasonTh = "Shurelya's Battlesong เร่งความเร็วทีมเข้าประชิดตัวตีไกลที่พยายามไคท์หนี"
        targetEn = "Kiting Backline"
        targetTh = "ไล่จับตัวตีไกล"
      }
      else -> {
        chosen = defaultEnchant
        reasonEn = "Matches ${myChamp.nameEn}'s primary teamfight survivability or engage role."
        reasonTh = "เหมาะที่สุดกับบทบาทการเอาตัวรอดหรือเปิดไฟต์ของ ${myChamp.nameTh}"
        targetEn = "Teamfight Utility"
        targetTh = "จังหวะทีมไฟต์"
      }
    }

    return ItemRecommendationEntry(
      item = chosen,
      priorityLabelEn = if (chosen.id != defaultEnchant.id) "Active Item (Adapted)" else "Active Item",
      priorityLabelTh = if (chosen.id != defaultEnchant.id) "ไอเทมกดใช้ (ปรับแก้ทาง)" else "ไอเทมกดใช้แนะนำ",
      isAdapted = chosen.id != defaultEnchant.id,
      replacedStandardItemName = if (chosen.id != defaultEnchant.id) defaultEnchant.nameEn else null,
      reasonEn = reasonEn,
      reasonTh = reasonTh,
      targetThreatEn = targetEn,
      targetThreatTh = targetTh,
      buyTimingEn = "Dedicated Active Slot before 2nd Dragon / Teamfight phase",
      buyTimingTh = "ออกในช่อง Active ก่อนเข้าสู่ช่วงทีมไฟต์มังกรตัวที่ 2",
      tacticalBenefitEn = chosen.passiveDescEn,
      tacticalBenefitTh = chosen.passiveDescTh,
    )
  }

  private data class RuneSpellBundle(
    val keystone: RuneData?,
    val minors: List<RuneData>,
    val spells: List<SpellData>,
    val reasonEn: String,
    val reasonTh: String,
  )

  private fun selectAdaptiveRunesAndSpells(
    myChamp: ChampionData,
    myLane: Lane,
    laneLoadout: com.example.data.model.ResolvedLaneLoadout,
    profile: EnemyTeamThreatProfile,
    runeMap: Map<String, RuneData>,
    spellMap: Map<String, SpellData>,
  ): RuneSpellBundle {
    var keystone =
      runeMap[com.example.data.model.resolveLegacyRuneId(laneLoadout.keystoneId)]
        ?: runeMap[com.example.data.model.resolveLegacyRuneId(myChamp.defaultKeystoneId)]
    val minors =
      laneLoadout.minorRuneIds
        .mapNotNull { runeMap[com.example.data.model.resolveLegacyRuneId(it)] }
        .toMutableList()
    val spells = laneLoadout.spellIds.mapNotNull { spellMap[it] }.toMutableList()
    val notesEn = mutableListOf<String>()
    val notesTh = mutableListOf<String>()
    if (!laneLoadout.isPrimaryOrListedLane || laneLoadout.lane != myChamp.lanes.firstOrNull()) {
      notesEn.add(laneLoadout.conditionNoteEn)
      notesTh.add(laneLoadout.conditionNoteTh)
    }

    val primaryTree = minors.firstOrNull()?.tree?.uppercase().orEmpty()

    fun applyAdaptiveMinor(rune: RuneData) {
      if (minors.any { it.id == rune.id } || minors.isEmpty()) return
      if (rune.tree.equals(primaryTree, ignoreCase = true) && minors.size >= 3) {
        val targetIdx =
          when (rune.slotType.uppercase()) {
            "MINOR_1" -> 0
            "MINOR_2" -> 1
            "MINOR_3" -> 2
            else -> 1
          }
        if (targetIdx < minors.size) minors[targetIdx] = rune
      } else if (minors.size >= 4) {
        minors[3] = rune
      } else {
        minors[minors.lastIndex] = rune
      }
    }

    // Adjust Keystone for tough poke lanes
    if (
      laneLoadout.keystoneId == "conqueror" &&
        profile.apBurstScore >= 70 &&
        myChamp.mobilityRating <= 5
    ) {
      runeMap["fleet_footwork"]?.let {
        keystone = it
        notesEn.add("Fleet Footwork helps sustain heavy enemy poke.")
        notesTh.add("เลือก Fleet Footwork เพื่อฮีลยืนเลนสู้สกิลตอด")
      }
    }

    // Adjust Minor Runes: Bone Plating vs Dive/Burst, Perseverance vs Heavy CC, Cut Down vs Tanks
    if (profile.diveScore >= 35 || profile.apBurstScore >= 60) {
      runeMap["bone_plating"]?.let { bp ->
        if (minors.none { it.id == "bone_plating" } && minors.isNotEmpty()) {
          applyAdaptiveMinor(bp)
          notesEn.add("Bone Plating reduces 3-hit assassin burst.")
          notesTh.add("ใส่ Bone Plating ลดเบิร์ส 3 ฮิตของตัวล้วง")
        }
      }
    }
    if (profile.ccScore >= 60) {
      runeMap["perseverance"]?.let { pers ->
        if (minors.none { it.id == "perseverance" } && minors.isNotEmpty()) {
          applyAdaptiveMinor(pers)
          notesEn.add("Perseverance adds Tenacity vs heavy CC.")
          notesTh.add("เสริม Perseverance เพิ่ม Tenacity ลดเวลาติด CC")
        }
      }
    }
    if (profile.tankinessScore >= 50 && myChamp.damageType != DamageType.AP) {
      runeMap["cut_down"]?.let { cd ->
        if (minors.none { it.id == "cut_down" } && minors.isNotEmpty()) {
          applyAdaptiveMinor(cd)
          notesEn.add("Cut Down deals bonus damage against high-HP tanks.")
          notesTh.add("เลือก Cut Down เพิ่มดาเมจใส่แชมเปี้ยนเลือดเยอะและแทงค์")
        }
      }
    }

    // Adjust Secondary Summoner Spell (keep Smite for Jungle!)
    if (myLane != Lane.JUNGLE && spells.size >= 2) {
      val secondary = spells[1].id
      if (profile.diveScore >= 35 && secondary in listOf("ignite", "heal") &&
        (ChampionRole.MARKSMAN in myChamp.roles || ChampionRole.SUPPORT in myChamp.roles || ChampionRole.MAGE in myChamp.roles)
      ) {
        spellMap["exhaust"]?.let { ex ->
          spells[1] = ex
          notesEn.add("Swapped to Exhaust to slash diver burst by 40%.")
          notesTh.add("เปลี่ยนเป็น Exhaust เพื่อลดดาเมจตัวล้วงลง 40%")
        }
      } else if (profile.healingScore >= 60 && secondary in listOf("barrier", "ghost")) {
        spellMap["ignite"]?.let { ign ->
          spells[1] = ign
          notesEn.add("Swapped to Ignite for extra Grievous Wounds anti-heal.")
          notesTh.add("เปลี่ยนเป็น Ignite เพื่อช่วยตัดฮีลศัตรูในจังหวะปะทะ")
        }
      }
    }

    val finalReasonEn =
      notesEn.joinToString(" ").ifBlank {
        "Standard ${myChamp.nameEn} rune & spell setup fits this matchup cleanly."
      }
    val finalReasonTh =
      notesTh.joinToString(" ").ifBlank {
        "รูนและ Spell มาตรฐานของ ${myChamp.nameTh} เหมาะสมกับไฟต์นี้อยู่แล้ว"
      }

    return RuneSpellBundle(keystone, minors.distinctBy { it.id }, spells, finalReasonEn, finalReasonTh)
  }
}
