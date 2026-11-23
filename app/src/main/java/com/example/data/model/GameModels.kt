package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AbilityInfo(
  val slot: String,
  val nameEn: String,
  val nameTh: String,
  val summaryEn: String,
  val summaryTh: String,
  val cooldownNote: String,
  val costNote: String = "",
  val iconUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class LaneBuildOverride(
  val lane: Lane,
  val starterItemId: String? = null,
  val coreItemIds: List<String> = emptyList(),
  val bootsId: String? = null,
  val enchantId: String? = null,
  val keystoneId: String? = null,
  val minorRuneIds: List<String> = emptyList(),
  val spellIds: List<String> = emptyList(),
  val skillOrder: String? = null,
  val laneNoteEn: String = "",
  val laneNoteTh: String = "",
)

data class ResolvedLaneLoadout(
  val lane: Lane,
  val isPrimaryOrListedLane: Boolean,
  val isOffMetaLane: Boolean,
  val starterItemId: String,
  val coreItemIds: List<String>,
  val bootsId: String,
  val enchantId: String,
  val keystoneId: String,
  val minorRuneIds: List<String>,
  val spellIds: List<String>,
  val skillOrder: String,
  val conditionNoteEn: String,
  val conditionNoteTh: String,
)

@JsonClass(generateAdapter = true)
data class ChampionData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val titleEn: String,
  val titleTh: String,
  val roles: List<ChampionRole>,
  val lanes: List<Lane>,
  val damageType: DamageType,
  val difficulty: Int,
  val tierRank: TierRank,
  val winRatePct: Double = 50.0,
  val pickRatePct: Double = 5.0,
  val banRatePct: Double = 2.0,
  val portraitUrl: String = "",
  val earlyPower: Int,
  val midPower: Int,
  val latePower: Int,
  val burstRating: Int,
  val dpsRating: Int,
  val tankinessRating: Int,
  val ccRating: Int,
  val mobilityRating: Int,
  val sustainRating: Int,
  val abilities: List<AbilityInfo>,
  val strengthsEn: List<String>,
  val strengthsTh: List<String>,
  val weaknessesEn: List<String>,
  val weaknessesTh: List<String>,
  val threatTags: List<ThreatTag>,
  val starterItemId: String,
  val coreItemIds: List<String>,
  val defaultBootsId: String,
  val defaultEnchantId: String,
  val defaultKeystoneId: String,
  val defaultMinorRuneIds: List<String>,
  val defaultSpellIds: List<String>,
  val skillOrder: String,
  val counterChampionIds: List<String>,
  val favorableAgainstIds: List<String>,
  val earlyPlanEn: String,
  val earlyPlanTh: String,
  val midPlanEn: String,
  val midPlanTh: String,
  val latePlanEn: String,
  val latePlanTh: String,
  val patchVersion: String,
  val lastUpdated: String,
  val sourceType: DataSourceType,
  val laneOverrides: List<LaneBuildOverride> = emptyList(),
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun displayTitle(lang: AppLanguage): String = if (lang == AppLanguage.TH) titleTh else titleEn

  fun strengths(lang: AppLanguage): List<String> =
    if (lang == AppLanguage.TH) strengthsTh else strengthsEn

  fun weaknesses(lang: AppLanguage): List<String> =
    if (lang == AppLanguage.TH) weaknessesTh else weaknessesEn

  fun earlyPlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) earlyPlanTh else earlyPlanEn
  fun midPlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) midPlanTh else midPlanEn
  fun latePlan(lang: AppLanguage): String = if (lang == AppLanguage.TH) latePlanTh else latePlanEn

  fun resolveForLane(lane: Lane): ResolvedLaneLoadout {
    val isListed = lane in lanes
    val isPrimary = lanes.firstOrNull() == lane
    val explicit = laneOverrides.firstOrNull { it.lane == lane }
    val supportStarters = setOf("relic_shield", "spectral_sickle")
    val isTankLike = ChampionRole.TANK in roles || tankinessRating >= 8
    val isAp = damageType == DamageType.AP || ChampionRole.MAGE in roles

    fun ensureJungleSpells(spells: List<String>): List<String> {
      if ("smite" in spells) return spells
      val primary = spells.firstOrNull { it != "smite" } ?: "flash"
      return listOf(primary, "smite")
    }

    fun nonSupportStarter(): String =
      if (starterItemId !in supportStarters) starterItemId
      else when {
        isTankLike -> "ruby_crystal"
        isAp -> "amplifying_tome"
        ChampionRole.MARKSMAN in roles -> "dagger"
        else -> "long_sword"
      }

    fun supportStarter(): String =
      if (isTankLike || ChampionRole.FIGHTER in roles) "relic_shield"
      else "spectral_sickle"

    if (explicit != null) {
      val starter =
        explicit.starterItemId
          ?: if (lane == Lane.SUPPORT && starterItemId !in supportStarters) supportStarter()
          else if (lane != Lane.SUPPORT && starterItemId in supportStarters) nonSupportStarter()
          else starterItemId
      val rawSpells = explicit.spellIds.ifEmpty { defaultSpellIds }
      val spells =
        when (lane) {
          Lane.JUNGLE -> ensureJungleSpells(rawSpells)
          Lane.SUPPORT ->
            if ("smite" in rawSpells) listOf("flash", "exhaust") else rawSpells
          else ->
            if ("smite" in rawSpells) listOf("flash", if (isAp || ChampionRole.MARKSMAN in roles) "barrier" else "ignite")
            else rawSpells
        }
      return ResolvedLaneLoadout(
        lane = lane,
        isPrimaryOrListedLane = isListed,
        isOffMetaLane = !isListed,
        starterItemId = starter,
        coreItemIds = explicit.coreItemIds.ifEmpty { coreItemIds },
        bootsId = explicit.bootsId ?: defaultBootsId,
        enchantId = explicit.enchantId ?: defaultEnchantId,
        keystoneId = explicit.keystoneId ?: defaultKeystoneId,
        minorRuneIds = explicit.minorRuneIds.ifEmpty { defaultMinorRuneIds },
        spellIds = spells,
        skillOrder = explicit.skillOrder?.takeIf { it.isNotBlank() } ?: skillOrder,
        conditionNoteEn =
          explicit.laneNoteEn.ifBlank {
            if (isPrimary) "Primary ${lane.labelEn} setup for ${nameEn}."
            else "Off-role ${lane.labelEn} adaptation for ${nameEn}."
          },
        conditionNoteTh =
          explicit.laneNoteTh.ifBlank {
            if (isPrimary) "เซ็ตมาตรฐานประจำ${lane.labelTh}ของ ${nameTh}"
            else "ปรับเซ็ตสำหรับเล่น${lane.labelTh}ของ ${nameTh}"
          },
      )
    }

    if (isPrimary) {
      val spells = if (lane == Lane.JUNGLE) ensureJungleSpells(defaultSpellIds) else defaultSpellIds
      return ResolvedLaneLoadout(
        lane = lane,
        isPrimaryOrListedLane = true,
        isOffMetaLane = false,
        starterItemId = starterItemId,
        coreItemIds = coreItemIds,
        bootsId = defaultBootsId,
        enchantId = defaultEnchantId,
        keystoneId = defaultKeystoneId,
        minorRuneIds = defaultMinorRuneIds,
        spellIds = spells,
        skillOrder = skillOrder,
        conditionNoteEn = "Primary ${lane.labelEn} setup for ${nameEn}.",
        conditionNoteTh = "เซ็ตมาตรฐานประจำ${lane.labelTh}ของ ${nameTh}",
      )
    }

    // Dynamic Wild Rift 7.3f off-lane condition synthesis when no explicit override exists
    val pureSupportItems =
      setOf(
        "harmonic_echo",
        "ardent_censer",
        "staff_of_flowing_water",
        "echoes_of_helia",
        "diadem_of_songs",
        "redemption",
        "imperial_mandate",
        "yordle_trap",
      )
    val hasPureSupportCore = coreItemIds.any { it in pureSupportItems }

    return when (lane) {
      Lane.JUNGLE -> {
        val jungleCore =
          when {
            hasPureSupportCore && isAp -> listOf("ludens_echo", "infinity_orb", "rabadons_deathcap", "banshees_veil")
            hasPureSupportCore -> listOf("sunfire_aegis", "heartsteel", "dead_mans_plate", "amaranths_twinguard")
            isTankLike && "sunfire_aegis" !in coreItemIds && "titanic_hydra" !in coreItemIds ->
              (listOf("sunfire_aegis") + coreItemIds).distinct().take(4)
            else -> coreItemIds
          }
        val normalizedMinors = defaultMinorRuneIds.map(::resolveLegacyRuneId)
        val jungleMinors =
          when {
            normalizedMinors.size >= 4 && normalizedMinors.first() in setOf("cheap_shot", "empowered_attack", "sudden_impact") ->
              listOf(normalizedMinors[0], normalizedMinors[1], "relentless_hunter", normalizedMinors[3])
            normalizedMinors.size >= 4 ->
              normalizedMinors.take(3) + "relentless_hunter"
            else ->
              listOf("triumph", "coup_de_grace", "legend_alacrity", "relentless_hunter")
          }
        val jungleKeystone =
          if (defaultKeystoneId in listOf("grasp_undying", "summon_aery", "arcane_comet")) {
            when {
              isTankLike -> "ice_overlord"
              isAp -> "first_strike"
              else -> "conqueror"
            }
          } else resolveLegacyRuneId(defaultKeystoneId)
        ResolvedLaneLoadout(
          lane = lane,
          isPrimaryOrListedLane = isListed,
          isOffMetaLane = !isListed,
          starterItemId = nonSupportStarter(),
          coreItemIds = jungleCore,
          bootsId = defaultBootsId,
          enchantId =
            if (defaultEnchantId in listOf("locket_enchant", "mikaels_blessing")) {
              if (isTankLike) "gargoyle_enchant" else "stasis_enchant"
            } else defaultEnchantId,
          keystoneId = jungleKeystone,
          minorRuneIds = jungleMinors.distinct(),
          spellIds = ensureJungleSpells(defaultSpellIds),
          skillOrder = skillOrder,
          conditionNoteEn =
            if (isListed) "Jungle condition: Bring Smite, Relentless Hunter for map tempo, and clear toward CC lanes."
            else "Off-meta Jungle condition: Smite + clear speed itemization and Relentless Hunter for gank tempo.",
          conditionNoteTh =
            if (isListed) "เงื่อนไขเลนป่า: พก Smite + รูน Relentless Hunter เร่งความเร็วเดินแก๊งและคุมมังกร/ปูแม่น้ำ"
            else "เงื่อนไขปรับเล่นป่า: บังคับพก Smite + เสริมไอเทมเคลียร์ป่าและรูน Relentless Hunter สำหรับเดินแก๊ง",
        )
      }

      Lane.SUPPORT -> {
        val supCore =
          when {
            isTankLike -> listOf("yordle_trap", "dawnshroud", "zekes_convergence", "amaranths_twinguard")
            ChampionRole.SUPPORT in roles -> listOf("harmonic_echo", "echoes_of_helia", "ardent_censer", "imperial_mandate")
            isAp -> listOf("imperial_mandate", "rylais_scepter", "liandrys_torment", "morellonomicon")
            else -> listOf("yordle_trap", "black_cleaver", "chempunk_chainsword", "dead_mans_plate")
          }
        val supBoots =
          when {
            isTankLike -> "plated_steelcaps"
            ChampionRole.SUPPORT in roles -> "ionian_boots"
            isAp -> "boots_of_mana"
            else -> "ionian_boots"
          }
        val supEnchant =
          when {
            isTankLike -> "locket_enchant"
            ChampionRole.SUPPORT in roles -> "mikaels_blessing"
            isAp -> "stasis_enchant"
            else -> "locket_enchant"
          }
        val supKeystone =
          when {
            isTankLike -> "guardian"
            ChampionRole.SUPPORT in roles -> "summon_aery"
            isAp -> "arcane_comet"
            else -> "ice_overlord"
          }
        val supMinors =
          if (isTankLike || (ChampionRole.SUPPORT !in roles && !isAp)) {
            listOf("font_of_life", "bone_plating", "perseverance", "transcendence")
          } else {
            listOf("manaflow_band", "transcendence", "scorch", "revitalize")
          }
        val supSpells = listOf("flash", if (isTankLike && ccRating >= 7) "ignite" else "exhaust")
        ResolvedLaneLoadout(
          lane = lane,
          isPrimaryOrListedLane = isListed,
          isOffMetaLane = !isListed,
          starterItemId = supportStarter(),
          coreItemIds = supCore,
          bootsId = supBoots,
          enchantId = supEnchant,
          keystoneId = supKeystone,
          minorRuneIds = supMinors,
          spellIds = supSpells,
          skillOrder = skillOrder,
          conditionNoteEn =
            if (isListed) "Support condition: Start with a Support Quest item, Exhaust/Ignite, and Locket/Mikael's utility."
            else "Off-meta Support condition: Switch to Support Quest gold item, budget utility items, and Exhaust to peel ADC.",
          conditionNoteTh =
            if (isListed) "เงื่อนไขซัพพอร์ต: ออกไอเทมซัพพอร์ต (500g) พก Exhaust/Ignite และเน้นไอเทมช่วยทีม/Locket"
            else "เงื่อนไขปรับเล่นซัพพอร์ต: เปลี่ยนไปออกไอเทมซัพพอร์ต 500g พก Exhaust และเน้นไอเทมราคาประหยัดช่วยแครี่",
        )
      }

      Lane.MID -> {
        val midCore =
          if (hasPureSupportCore || (isTankLike && isAp)) {
            listOf("ludens_echo", "infinity_orb", "rabadons_deathcap", "banshees_veil")
          } else if (hasPureSupportCore) {
            listOf("eclipse", "black_cleaver", "deaths_dance", "steraks_gage")
          } else {
            coreItemIds
          }
        val midBoots =
          when {
            isAp -> "boots_of_mana"
            ChampionRole.ASSASSIN in roles -> "boots_of_dynamism"
            else -> defaultBootsId
          }
        val midKeystone =
          if (defaultKeystoneId in listOf("grasp_undying", "ice_overlord", "guardian", "summon_aery")) {
            when {
              isAp && burstRating >= 7 -> "electrocute"
              isAp -> "arcane_comet"
              else -> "conqueror"
            }
          } else resolveLegacyRuneId(defaultKeystoneId)
        val midMinors =
          if (isAp) listOf("manaflow_band", "transcendence", "scorch", "cheap_shot")
          else defaultMinorRuneIds.map(::resolveLegacyRuneId)
        ResolvedLaneLoadout(
          lane = lane,
          isPrimaryOrListedLane = isListed,
          isOffMetaLane = !isListed,
          starterItemId = nonSupportStarter(),
          coreItemIds = midCore,
          bootsId = midBoots,
          enchantId =
            if (defaultEnchantId in listOf("locket_enchant", "mikaels_blessing")) "stasis_enchant"
            else defaultEnchantId,
          keystoneId = midKeystone,
          minorRuneIds = midMinors,
          spellIds = listOf("flash", if (ChampionRole.MARKSMAN in roles || mobilityRating <= 4) "barrier" else "ignite"),
          skillOrder = skillOrder,
          conditionNoteEn =
            if (isListed) "Mid condition: Prioritize waveclear + burst boots and roam to side lanes after cannon waves."
            else "Off-meta Mid condition: Swap to solo-lane burst/waveclear items and Flash + Ignite/Barrier.",
          conditionNoteTh =
            if (isListed) "เงื่อนไขเลนกลาง: เน้นเคลียร์เวฟไว ออกไอเทมเบิร์ส/Mana Boots และเดินแก๊งหลังเคลียร์เวฟรถถัง"
            else "เงื่อนไขปรับเล่นเลนกลาง: ปรับเป็นไอเทมเคลียร์เวฟ/เบิร์สเดี่ยว พก Ignite หรือ Barrier สู้ 1v1",
        )
      }

      Lane.BARON -> {
        val baronCore =
          when {
            hasPureSupportCore && isAp -> listOf("riftmaker", "liandrys_torment", "cosmic_drive", "amaranths_twinguard")
            hasPureSupportCore -> listOf("heartsteel", "sunfire_aegis", "iceborn_gauntlet", "amaranths_twinguard")
            isAp && ChampionRole.TANK !in roles && "riftmaker" !in coreItemIds ->
              (listOf("riftmaker") + coreItemIds).distinct().take(4)
            else -> coreItemIds
          }
        val baronKeystone =
          if (defaultKeystoneId in listOf("summon_aery", "guardian", "ice_overlord")) {
            if (isTankLike) "grasp_undying" else "conqueror"
          } else resolveLegacyRuneId(defaultKeystoneId)
        ResolvedLaneLoadout(
          lane = lane,
          isPrimaryOrListedLane = isListed,
          isOffMetaLane = !isListed,
          starterItemId = nonSupportStarter(),
          coreItemIds = baronCore,
          bootsId = if (isTankLike || ChampionRole.FIGHTER in roles) "plated_steelcaps" else defaultBootsId,
          enchantId =
            if (defaultEnchantId in listOf("locket_enchant", "mikaels_blessing")) {
              if (isTankLike) "gargoyle_enchant" else "stasis_enchant"
            } else defaultEnchantId,
          keystoneId = baronKeystone,
          minorRuneIds = listOf("demolish", "bone_plating", "overgrowth", "last_stand"),
          spellIds = listOf("flash", if (mobilityRating <= 4 && isTankLike) "ghost" else "ignite"),
          skillOrder = skillOrder,
          conditionNoteEn =
            if (isListed) "Baron Lane condition: Build 1v1 sustain/durability with Grasp/Conqueror and Bone Plating."
            else "Off-meta Baron condition: Focus on sustain, Bone Plating for short trades, and side-lane durability.",
          conditionNoteTh =
            if (isListed) "เงื่อนไขเลนบารอน: เสริมความถึก/ดูดเลือด 1v1 ด้วย Grasp หรือ Conqueror + Bone Plating"
            else "เงื่อนไขปรับเล่นบารอน: ปรับรูน/ของให้ยืน 1v1 ไหว ใส่ Bone Plating ลดดาเมจเทรดในเลนยาว",
        )
      }

      Lane.DRAGON -> {
        val dragonCore =
          when {
            ChampionRole.MARKSMAN in roles -> coreItemIds
            isAp -> listOf("ludens_echo", "liandrys_torment", "infinity_orb", "rabadons_deathcap")
            else -> listOf("eclipse", "sundered_sky", "deaths_dance", "guardian_angel")
          }
        val dragonBoots =
          when {
            ChampionRole.MARKSMAN in roles -> "berserkers_greaves"
            isAp -> "boots_of_mana"
            else -> defaultBootsId
          }
        ResolvedLaneLoadout(
          lane = lane,
          isPrimaryOrListedLane = isListed,
          isOffMetaLane = !isListed,
          starterItemId = nonSupportStarter(),
          coreItemIds = dragonCore,
          bootsId = dragonBoots,
          enchantId =
            if (ChampionRole.MARKSMAN in roles) "quicksilver_enchant"
            else if (isAp) "stasis_enchant"
            else defaultEnchantId,
          keystoneId =
            if (ChampionRole.MARKSMAN in roles && defaultKeystoneId !in listOf("lethal_tempo", "fleet_footwork", "empowerment", "conqueror")) {
              "lethal_tempo"
            } else resolveLegacyRuneId(defaultKeystoneId),
          minorRuneIds =
            if (ChampionRole.MARKSMAN in roles) listOf("brutal", "cut_down", "legend_bloodline", "bone_plating")
            else defaultMinorRuneIds.map(::resolveLegacyRuneId),
          spellIds = listOf("flash", "barrier"),
          skillOrder = skillOrder,
          conditionNoteEn =
            if (isListed) "Dragon Lane condition: Take Flash + Barrier/Exhaust and scale around 2v2 skirmishes & QSS/Stasis."
            else "Off-meta Dragon carry condition: Prioritize sustained DPS/waveclear with Flash + Barrier for 2v2 survival.",
          conditionNoteTh =
            if (isListed) "เงื่อนไขเลนดราก้อน: พก Flash + Barrier เล่น 2v2 คู่ซัพพอร์ตและเร่ง DPS ช่วงมังกร"
            else "เงื่อนไขปรับเล่นเลนดราก้อน: ปรับของเป็นสาย DPS/ฟาร์มเวฟ พก Flash + Barrier เอาตัวรอดในเลน 2v2",
        )
      }
    }
  }
}

private val LEGACY_RUNE_ALIASES = mapOf(
  "aftershock" to "ice_overlord",
  "glacial_augment" to "ice_overlord",
  "giant_slayer" to "cut_down",
  "bloodline" to "legend_bloodline",
  "alacrity" to "legend_alacrity",
  "legend_tenacity" to "legend_haste",
  "sweet_tooth" to "botanist",
  "pathfinder" to "relentless_hunter",
  "loyalty" to "unshakeable",
  "psychic_wave" to "empowered_attack",
  "ingenious_hunter" to "relentless_hunter",
  "press_the_attack" to "empowerment",
  "kraken_slayer" to "empowerment",
)

fun resolveLegacyRuneId(id: String): String = LEGACY_RUNE_ALIASES[id] ?: id

private val LEGACY_ITEM_ALIASES = mapOf(
  "crown_shattered_queen" to "banshees_veil",
  "awakened_soulstealer" to "malignance",
  "solari_chargeblade" to "navori_quickblades",
  "spirit_visage" to "warmogs_armor",
  "ancient_coin" to "spectral_sickle",
)

fun resolveLegacyItemId(id: String): String = LEGACY_ITEM_ALIASES[id] ?: id

val TIER3_BOOT_UPGRADE_MAP = mapOf(
  "plated_steelcaps" to "armored_advance",
  "mercurys_treads" to "chainlaced_crushers",
  "berserkers_greaves" to "gunmetal_greaves",
  "boots_of_mana" to "spellslingers_shoes",
  "boots_of_dynamism" to "armorcrusher_boots",
  "ionian_boots" to "crimson_lucidity",
  "gluttonous_greaves" to "immortal_treads",
)

val TIER2_BOOT_BASE_MAP = TIER3_BOOT_UPGRADE_MAP.entries.associate { (tier2, tier3) -> tier3 to tier2 }

fun tier3BootUpgradeId(tier2BootId: String): String? = TIER3_BOOT_UPGRADE_MAP[tier2BootId]

fun tier2BootBaseId(tier3BootId: String): String? = TIER2_BOOT_BASE_MAP[tier3BootId]

fun isTier3Boot(itemId: String): Boolean = itemId in TIER2_BOOT_BASE_MAP

@JsonClass(generateAdapter = true)
data class ItemData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val category: ItemCategory,
  val cost: Int,
  val statsSummaryEn: String,
  val statsSummaryTh: String,
  val passiveNameEn: String,
  val passiveNameTh: String,
  val passiveDescEn: String,
  val passiveDescTh: String,
  val counterTags: List<ItemCounterTag>,
  val damageAffinity: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun statsSummary(lang: AppLanguage): String =
    if (lang == AppLanguage.TH) statsSummaryTh else statsSummaryEn

  fun passiveDesc(lang: AppLanguage): String =
    if (lang == AppLanguage.TH) passiveDescTh else passiveDescEn
}

@JsonClass(generateAdapter = true)
data class RuneData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val slotType: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
  val tree: String = "",
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun desc(lang: AppLanguage): String = if (lang == AppLanguage.TH) descTh else descEn
}

@JsonClass(generateAdapter = true)
data class SpellData(
  val id: String,
  val nameEn: String,
  val nameTh: String,
  val descEn: String,
  val descTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
  val iconUrl: String = "",
  val cooldownNote: String = "",
) {
  fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.TH) nameTh else nameEn

  fun desc(lang: AppLanguage): String = if (lang == AppLanguage.TH) descTh else descEn
}

@JsonClass(generateAdapter = true)
data class MatchupRuleData(
  val id: String,
  val champAId: String,
  val champBId: String,
  val advantageScore: Int,
  val laneDifficulty: LaneDifficulty,
  val dangerAbilitiesEn: String,
  val dangerAbilitiesTh: String,
  val tipEn: String,
  val tipTh: String,
  val keySpikeEn: String,
  val keySpikeTh: String,
  val patchVersion: String,
  val sourceType: DataSourceType,
)

@JsonClass(generateAdapter = true)
data class PatchChangeEntry(
  val targetName: String,
  val changeType: PatchChangeType,
  val detailEn: String,
  val detailTh: String,
)

@JsonClass(generateAdapter = true)
data class PatchNoteData(
  val version: String,
  val releaseDate: String,
  val headlineEn: String,
  val headlineTh: String,
  val summaryEn: String,
  val summaryTh: String,
  val sourceLabel: String,
  val isActive: Boolean,
  val changes: List<PatchChangeEntry>,
)

@JsonClass(generateAdapter = true)
data class PatchPackPayload(
  val packVersion: Int,
  val patchVersion: String,
  val lastUpdated: String,
  val sourceLabel: String,
  val champions: List<ChampionData> = emptyList(),
  val items: List<ItemData> = emptyList(),
  val runes: List<RuneData> = emptyList(),
  val spells: List<SpellData> = emptyList(),
  val matchupRules: List<MatchupRuleData> = emptyList(),
  val patchNotes: List<PatchNoteData> = emptyList(),
)

data class CustomBuildData(
  val id: Long = 0,
  val name: String,
  val championId: String,
  val lane: Lane,
  val starterItemId: String,
  val coreItemIds: List<String>,
  val bootsId: String,
  val enchantId: String,
  val keystoneId: String,
  val minorRuneIds: List<String>,
  val spellIds: List<String>,
  val notes: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
)

