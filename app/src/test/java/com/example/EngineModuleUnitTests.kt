package com.example

import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiProviderId
import com.example.data.ai.CoachErrorKind
import com.example.data.ai.ExternalAiHttpException
import com.example.data.ai.ExternalAiProvidersClient
import com.example.data.ai.ExternalApiKeyStatus
import com.example.data.ai.GeminiCoachService
import com.example.data.model.AbilityInfo
import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.DataSourceType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.ItemData
import com.example.data.model.Lane
import com.example.data.model.LaneDifficulty
import com.example.data.model.MatchupRuleData
import com.example.data.model.PatchPackPayload
import com.example.data.model.RuneData
import com.example.data.model.SpellData
import com.example.data.model.TIER3_BOOT_UPGRADE_MAP
import com.example.data.model.ThreatTag
import com.example.data.model.TierRank
import com.example.data.model.tier2BootBaseId
import com.example.data.model.tier3BootUpgradeId
import com.example.data.validation.PatchDataValidator
import com.example.domain.engine.DraftAssistantEngine
import com.example.domain.engine.MatchupCounterEngine
import com.example.domain.engine.OfflineCoachEngine
import com.example.domain.engine.SmartItemEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineModuleUnitTests {

  private fun loadSeedPayload(): PatchPackPayload {
    val paths =
      listOf(
        "src/main/assets/patch_7_3f_seed.json",
        "app/src/main/assets/patch_7_3f_seed.json",
      )
    val file = paths.map { File(it) }.first { it.exists() }
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(PatchPackPayload::class.java)
    return adapter.fromJson(file.readText())!!
  }

  @Test
  fun `bundled Patch 7_3f seed passes strict referential integrity validation`() {
    val seed = loadSeedPayload()
    val validator = PatchDataValidator()
    val result = validator.validate(seed)

    assertTrue(
      "Expected seed validation to pass, errors=${result.errors}",
      result.isValid,
    )
    assertTrue("Expected at least 144 champions, found ${result.championsCount}", result.championsCount >= 144)
    assertTrue("Expected at least 100 items, found ${result.itemsCount}", result.itemsCount >= 100)
    assertTrue("Expected at least 52 runes, found ${result.runesCount}", result.runesCount >= 52)
    assertTrue("Expected no warnings, got ${result.warnings}", result.warnings.isEmpty())

    seed.champions.forEach { champ ->
      assertEquals(
        "Champion ${champ.id} must have 5 abilities (P, Q, W, E, R)",
        setOf("P", "Q", "W", "E", "R"),
        champ.abilities.map { it.slot }.toSet(),
      )
      assertTrue(
        "Champion ${champ.id} winRatePct=${champ.winRatePct} out of expected bounds",
        champ.winRatePct in 40.0..60.0,
      )
      assertTrue(
        "Champion ${champ.id} pickRatePct=${champ.pickRatePct} should be positive",
        champ.pickRatePct > 0.0,
      )
      assertTrue(
        "Champion ${champ.id} missing portraitUrl",
        champ.portraitUrl.startsWith("https://"),
      )
    }
    seed.items.forEach { item ->
      assertTrue("Item ${item.id} missing iconUrl", item.iconUrl.startsWith("https://"))
      assertFalse(
        "Item ${item.id} must use Wild Rift icon instead of PC DDragon item icon",
        item.iconUrl.contains("ddragon.leagueoflegends.com"),
      )
      assertTrue(
        "Item ${item.id} should come from wildriftguides.com",
        item.iconUrl.contains("wildriftguides.com"),
      )
    }
    val validTrees = setOf("PRECISION", "DOMINATION", "RESOLVE", "SORCERY")
    seed.runes.forEach { rune ->
      assertTrue("Rune ${rune.id} missing iconUrl", rune.iconUrl.startsWith("https://"))
      assertFalse(
        "Rune ${rune.id} must use Wild Rift icon instead of PC perk-images",
        rune.iconUrl.contains("perk-images"),
      )
      assertTrue("Rune ${rune.id} invalid tree '${rune.tree}'", rune.tree in validTrees)
    }

    val runeById = seed.runes.associateBy { it.id }
    fun assertValidFiveRuneSetup(label: String, keystoneId: String, minorIds: List<String>) {
      val ks = runeById.getValue(keystoneId)
      assertEquals("$label keystone $keystoneId must have slotType=KEYSTONE", "KEYSTONE", ks.slotType)
      assertEquals("$label must have 4 minor runes (3 Primary + 1 Secondary)", 4, minorIds.size)
      val m0 = runeById.getValue(minorIds[0])
      val m1 = runeById.getValue(minorIds[1])
      val m2 = runeById.getValue(minorIds[2])
      val sec = runeById.getValue(minorIds[3])
      assertEquals("$label minor[0] ${m0.id} must be MINOR_1", "MINOR_1", m0.slotType)
      assertEquals("$label minor[1] ${m1.id} must be MINOR_2", "MINOR_2", m1.slotType)
      assertEquals("$label minor[2] ${m2.id} must be MINOR_3", "MINOR_3", m2.slotType)
      assertTrue("$label primary minors must share one tree", m0.tree == m1.tree && m1.tree == m2.tree)
      assertTrue("$label secondary minor ${sec.id} must be a MINOR rune", sec.slotType.startsWith("MINOR_"))
      assertNotEquals("$label secondary minor ${sec.id} must be from a different tree than ${m0.tree}", m0.tree, sec.tree)
    }

    seed.champions.forEach { champ ->
      assertValidFiveRuneSetup("Champion ${champ.id}", champ.defaultKeystoneId, champ.defaultMinorRuneIds)
      champ.laneOverrides.forEach { ov ->
        if (ov.minorRuneIds.isNotEmpty()) {
          assertValidFiveRuneSetup(
            "Champion ${champ.id} override(${ov.lane})",
            ov.keystoneId ?: champ.defaultKeystoneId,
            ov.minorRuneIds,
          )
        }
      }
    }
    seed.spells.forEach { spell ->
      assertTrue("Spell ${spell.id} missing iconUrl", spell.iconUrl.startsWith("https://"))
      assertFalse(
        "Spell ${spell.id} must use Wild Rift summoners icon instead of PC DDragon Summoner*.png",
        spell.iconUrl.contains("/img/spell/Summoner"),
      )
      assertTrue("Spell ${spell.id} missing cooldownNote", spell.cooldownNote.isNotBlank())
    }
    assertEquals(
      "All runes must have distinct icon URLs",
      seed.runes.size,
      seed.runes.map { it.iconUrl }.distinct().size,
    )
    assertEquals(
      "All spells must have distinct icon URLs",
      seed.spells.size,
      seed.spells.map { it.iconUrl }.distinct().size,
    )

    val itemById = seed.items.associateBy { it.id }
    assertNotEquals(
      "gluttonous_greaves and boots_of_dynamism should have distinct icons",
      itemById.getValue("gluttonous_greaves").iconUrl,
      itemById.getValue("boots_of_dynamism").iconUrl,
    )
    assertNotEquals(
      "harmonic_echo and echoes_of_helia should have distinct icons",
      itemById.getValue("harmonic_echo").iconUrl,
      itemById.getValue("echoes_of_helia").iconUrl,
    )
    assertNotEquals(
      "amaranths_twinguard and kaenic_rookern should have distinct icons",
      itemById.getValue("amaranths_twinguard").iconUrl,
      itemById.getValue("kaenic_rookern").iconUrl,
    )
    assertNotEquals(
      "divine_sunderer and sundered_sky should have distinct icons",
      itemById.getValue("divine_sunderer").iconUrl,
      itemById.getValue("sundered_sky").iconUrl,
    )

    val duplicateIcons = seed.items.groupBy { it.iconUrl }.filter { it.value.size > 1 }
    assertTrue("All items must have distinct icons, duplicates=$duplicateIcons", duplicateIcons.isEmpty())

    val itemIds = seed.items.map { it.id }.toSet()
    val retiredOrPcOnlyItems = listOf(
      "ravenous_hydra",
      "cryptbloom",
      "redemption_enchant",
      "crystalline_reflector",
      "protectors_vow",
      "repulsor_enchant",
      "teleport_enchant",
      "searing_crown",
      "magnetic_blaster",
      "psychic_projector",
      "crown_shattered_queen",
      "awakened_soulstealer",
      "solari_chargeblade",
      "spirit_visage",
      "ancient_coin",
    )
    retiredOrPcOnlyItems.forEach { oldId ->
      assertFalse("Retired/PC-only item $oldId should not be in latest WR 7.3f seed", oldId in itemIds)
    }
    listOf(
        "yun_tal_wildarrows",
        "hexoptics_c44",
        "rapid_firecannon",
        "statikk_shiv",
        "fiendhunter_bolts",
        "guinsoos_rageblade",
        "kaenic_rookern",
        "echoes_of_helia",
        "diadem_of_songs",
        "yordle_trap",
        "mikaels_blessing",
        "stridebreaker",
        "goredrinker",
        "galeforce",
        "titanic_hydra",
        "sundered_sky",
        "oceanids_trident",
        "terminus",
        "staff_of_flowing_water",
        "redemption",
        "banshees_veil",
        "malignance",
      )
      .forEach { wrId ->
        assertTrue("Wild Rift 7.3f item $wrId should exist in seed", wrId in itemIds)
      }

    // Verify Wild Rift 7.3f item stats & pricing (no PC LoL stat leaks)
    assertEquals(2900, itemById.getValue("statikk_shiv").cost)
    assertFalse(
      "Wild Rift Statikk Shiv must not carry PC Crit chance",
      itemById.getValue("statikk_shiv").statsSummaryEn.contains("Crit", ignoreCase = true),
    )
    assertEquals(3300, itemById.getValue("divine_sunderer").cost)
    assertEquals(2600, itemById.getValue("rod_of_ages").cost)

    // Verify Wild Rift 7.3f Tier 2 Boots stats & 10:00+ Tier 3 Boot Upgrades
    assertEquals(7, TIER3_BOOT_UPGRADE_MAP.size)
    TIER3_BOOT_UPGRADE_MAP.forEach { (tier2Id, tier3Id) ->
      val t2 = itemById.getValue(tier2Id)
      val t3 = itemById.getValue(tier3Id)
      assertEquals(ItemCategory.BOOTS, t2.category)
      assertEquals(ItemCategory.BOOTS, t3.category)
      assertEquals(tier3Id, tier3BootUpgradeId(tier2Id))
      assertEquals(tier2Id, tier2BootBaseId(tier3Id))
      assertEquals("${tier3Id} should cost +1000g over ${tier2Id}", t2.cost + 1000, t3.cost)
      assertTrue("${tier3Id} must mention 10:00+", t3.statsSummaryEn.contains("10:00+") || t3.passiveDescEn.contains("10:00+"))
    }
    assertTrue(itemById.getValue("plated_steelcaps").statsSummaryEn.contains("+150 HP, +25 Armor"))
    assertTrue(itemById.getValue("mercurys_treads").statsSummaryEn.contains("+30% Tenacity"))
    assertFalse(itemById.getValue("berserkers_greaves").statsSummaryEn.contains("AD"))
    assertTrue(itemById.getValue("berserkers_greaves").statsSummaryEn.contains("+35% Attack Speed"))
    assertTrue(itemById.getValue("ionian_boots").statsSummaryEn.contains("+15 Ability Haste"))

    val champIds = seed.champions.map { it.id }.toSet()
    listOf(
        "talon",
        "lissandra",
        "poppy",
        "rumble",
        "gnar",
        "ryze",
        "nocturne",
        "zilean",
        "aurora",
        "velkoz",
        "kogmaw",
        "nidalee",
        "rell",
        "mel",
        "chogath",
        "norra",
        "ksante",
        "taliyah",
        "skarner",
        "yunara",
        "reksai",
      )
      .forEach { wrChamp ->
        assertTrue("Wild Rift champion $wrChamp should exist in seed", wrChamp in champIds)
      }

    val numericKeyRegex = Regex("\\([1-4]\\)")
    seed.champions.forEach { champ ->
      assertTrue(
        "Champion ${champ.id} skillOrder '${champ.skillOrder}' should use Q/W/E/R notation",
        champ.skillOrder.any { it in setOf('Q', 'W', 'E', 'R') } &&
          champ.skillOrder.none { it in setOf('1', '2', '3', '4') },
      )
      champ.abilities.forEach { ab ->
        assertFalse(
          "Champion ${champ.id} ability '${ab.nameEn}' has numeric (1)-(4) suffix",
          numericKeyRegex.containsMatchIn(ab.nameEn) || numericKeyRegex.containsMatchIn(ab.nameTh),
        )
      }
    }
  }

  @Test
  fun `PatchDataValidator catches dangling item reference`() {
    val seed = loadSeedPayload()
    val validator = PatchDataValidator()
    val brokenChamp = seed.champions.first().copy(id = "broken_champ", starterItemId = "missing_item_id")
    val result =
      validator.validate(
        payload = seed.copy(champions = listOf(brokenChamp)),
        existingChampions = seed.champions,
        existingItems = seed.items,
        existingRunes = seed.runes,
        existingSpells = seed.spells,
      )
    assertFalse(result.isValid)
    assertTrue(result.errors.any { it.contains("missing_item_id") })
  }

  @Test
  fun `MatchupCounterEngine ranks Zed as top threat to immobile Jinx`() {
    val seed = loadSeedPayload()
    val champMap = seed.champions.associateBy { it.id }
    val jinx = champMap.getValue("jinx")
    val enemies =
      listOf("garen", "vi", "zed", "caitlyn", "braum").map { champMap.getValue(it) }

    val engine = MatchupCounterEngine()
    val (matchups, profile) = engine.analyzeMatchups(jinx, Lane.DRAGON, enemies, seed.matchupRules)

    assertEquals(5, matchups.size)
    assertEquals("zed", matchups.first().enemy.id)
    assertEquals(LaneDifficulty.HARD, matchups.first().verdict)
    assertTrue(profile.diveScore >= 35)
  }

  @Test
  fun `SmartItemEngine adapts builds differently for Heavy Healing vs Heavy AP Burst`() {
    val seed = loadSeedPayload()
    val champMap = seed.champions.associateBy { it.id }
    val jinx = champMap.getValue("jinx")
    val healEnemies =
      listOf("aatrox", "dr_mundo", "sylas", "kaisa", "soraka").map { champMap.getValue(it) }
    val apBurstEnemies =
      listOf("malphite", "evelynn", "syndra", "ezreal", "lux").map { champMap.getValue(it) }

    val matchupEngine = MatchupCounterEngine()
    val itemEngine = SmartItemEngine()

    val (healMatchups, healProfile) =
      matchupEngine.analyzeMatchups(jinx, Lane.DRAGON, healEnemies, seed.matchupRules)
    val healBuild =
      itemEngine.buildRecommendation(
        jinx,
        Lane.DRAGON,
        healEnemies,
        healMatchups,
        healProfile,
        seed.items,
        seed.runes,
        seed.spells,
      )

    val (apMatchups, apProfile) =
      matchupEngine.analyzeMatchups(jinx, Lane.DRAGON, apBurstEnemies, seed.matchupRules)
    val apBuild =
      itemEngine.buildRecommendation(
        jinx,
        Lane.DRAGON,
        apBurstEnemies,
        apMatchups,
        apProfile,
        seed.items,
        seed.runes,
        seed.spells,
      )

    assertTrue(
      "Expected Grievous Wounds (Mortal Reminder) vs heavy healing team",
      healBuild.prioritizedItems.any { ItemCounterTag.ANTI_HEAL in it.item.counterTags },
    )
    assertEquals("stasis_enchant", apBuild.recommendedEnchant.item.id)
    assertNotEquals(
      healBuild.prioritizedItems.map { it.item.id },
      apBuild.prioritizedItems.map { it.item.id },
    )
    assertTrue(healBuild.prioritizedItems.all { it.reasonEn.isNotBlank() && it.targetThreatEn.isNotBlank() })
    assertEquals(
      tier3BootUpgradeId(healBuild.recommendedBoots.item.id),
      healBuild.recommendedTier3Boots?.item?.id,
    )
    assertEquals(
      tier3BootUpgradeId(apBuild.recommendedBoots.item.id),
      apBuild.recommendedTier3Boots?.item?.id,
    )
    assertTrue(
      "Expected 10:00+ Tier 3 Boot entry in prioritizedItems",
      apBuild.prioritizedItems.any { it.item.id == apBuild.recommendedTier3Boots?.item?.id },
    )
  }

  @Test
  fun `DraftAssistantEngine detects All-AD risk and missing frontline and suggests AP or Tank picks`() {
    val seed = loadSeedPayload()
    val champMap = seed.champions.associateBy { it.id }
    val allies = listOf("fiora", "kha_zix", "zed", "jinx").map { champMap.getValue(it) }
    val enemies = listOf("rammus", "malphite", "yasuo", "lucian", "leona").map { champMap.getValue(it) }

    val draftEngine = DraftAssistantEngine()
    val report =
      draftEngine.analyzeDraft(
        allies = allies,
        enemies = enemies,
        intendedLane = Lane.SUPPORT,
        preferredRole = null,
        allChampions = seed.champions,
        rules = seed.matchupRules,
      )

    assertTrue(report.warnings.any { it.titleEn.contains("All-AD") })
    assertTrue(report.warnings.any { it.titleEn.contains("Frontline") })
    assertTrue(report.recommendedPicks.isNotEmpty())
  }

  @Test
  fun `OfflineCoachEngine builds 3-phase playbook`() {
    val seed = loadSeedPayload()
    val sylas = seed.champions.first { it.id == "sylas" }
    val sections = OfflineCoachEngine().buildPlaybook(sylas, Lane.MID)
    assertEquals(3, sections.size)
    assertTrue(sections.all { it.bodyTh.isNotBlank() && it.bodyEn.isNotBlank() })
  }

  @Test
  fun `Extensible custom PatchPackPayload with new champion passes validation`() {
    val seed = loadSeedPayload()
    val newChamp =
      ChampionData(
        id = "ambessa",
        nameEn = "Ambessa",
        nameTh = "Ambessa",
        titleEn = "Matriarch of War",
        titleTh = "แม่ทัพแห่งสงคราม",
        roles = listOf(ChampionRole.FIGHTER),
        lanes = listOf(Lane.BARON),
        damageType = DamageType.AD,
        difficulty = 3,
        tierRank = TierRank.S,
        earlyPower = 8,
        midPower = 9,
        latePower = 8,
        burstRating = 8,
        dpsRating = 8,
        tankinessRating = 6,
        ccRating = 6,
        mobilityRating = 9,
        sustainRating = 6,
        abilities = listOf(AbilityInfo("R", "Public Execution", "Public Execution", "Dive slam", "พุ่งทุ่ม", "75s")),
        strengthsEn = listOf("High dash mobility"),
        strengthsTh = listOf("พุ่งต่อเนื่อง"),
        weaknessesEn = listOf("Weak to hard CC"),
        weaknessesTh = listOf("แพ้ CC ล็อก"),
        threatTags = listOf(ThreatTag.ASSASSIN_DIVE, ThreatTag.HIGH_AD_DPS),
        starterItemId = "long_sword",
        coreItemIds = listOf("eclipse", "black_cleaver", "deaths_dance"),
        defaultBootsId = "plated_steelcaps",
        defaultEnchantId = "stasis_enchant",
        defaultKeystoneId = "conqueror",
        defaultMinorRuneIds = listOf("brutal", "last_stand", "legend_alacrity", "bone_plating"),
        defaultSpellIds = listOf("flash", "ignite"),
        skillOrder = "Q > E > W",
        counterChampionIds = listOf("rammus"),
        favorableAgainstIds = listOf("lux"),
        earlyPlanEn = "Dash in on cooldowns.",
        earlyPlanTh = "พุ่งเทรดช่วงสกิลพร้อม",
        midPlanEn = "Flank carries.",
        midPlanTh = "อ้อมหลังล้วงแครี่",
        latePlanEn = "Wait for CC.",
        latePlanTh = "รอศัตรูใช้ CC",
        patchVersion = "7.4a",
        lastUpdated = "2026-12-01",
        sourceType = DataSourceType.CUSTOM_USER_PACK,
      )

    val extensionPack =
      PatchPackPayload(
        packVersion = 1,
        patchVersion = "7.4a",
        lastUpdated = "2026-12-01",
        sourceLabel = "Test Pack",
        champions = listOf(newChamp),
      )

    val result =
      PatchDataValidator()
        .validate(
          payload = extensionPack,
          existingChampions = seed.champions,
          existingItems = seed.items,
          existingRunes = seed.runes,
          existingSpells = seed.spells,
        )
    assertTrue("Errors: ${result.errors}", result.isValid)
  }

  @Test
  fun `GeminiCoachService classifies quota limits for model fallback and Thai guidance`() {
    val quotaEx = RuntimeException("429 RESOURCE_EXHAUSTED: Quota exceeded for metric limit")
    assertEquals(CoachErrorKind.QUOTA_LIMIT, GeminiCoachService.classifyError(quotaEx))
    assertTrue(GeminiCoachService.shouldFallbackToSecondaryModel(quotaEx))

    val thMsg = GeminiCoachService.buildDetailedErrorMessage(quotaEx, AppLanguage.TH)
    assertTrue("Expected Thai quota message, got: $thMsg", thMsg.contains("โควตา") && thMsg.contains("คู่มือโค้ชออฟไลน์"))

    val appCheckEx = RuntimeException("403 App Check token rejected")
    assertEquals(CoachErrorKind.APP_CHECK, GeminiCoachService.classifyError(appCheckEx))
    assertFalse(GeminiCoachService.shouldFallbackToSecondaryModel(appCheckEx))
  }

  @Test
  fun `ChampionData resolveForLane adapts explicit overrides and off-meta lane conditions`() {
    val seed = loadSeedPayload()
    val champMap = seed.champions.associateBy { it.id }

    // Explicit multi-lane overrides on Cho'Gath (Baron Tank vs Jungle Smite vs Mid AP RoA)
    val chogath = champMap.getValue("chogath")
    val baronLoadout = chogath.resolveForLane(Lane.BARON)
    assertFalse(baronLoadout.isOffMetaLane)
    assertEquals("E > Q > W", baronLoadout.skillOrder)

    val jungleLoadout = chogath.resolveForLane(Lane.JUNGLE)
    assertTrue("smite" in jungleLoadout.spellIds)
    assertTrue(jungleLoadout.conditionNoteEn.isNotBlank() && jungleLoadout.conditionNoteTh.isNotBlank())

    val midLoadout = chogath.resolveForLane(Lane.MID)
    assertTrue("rod_of_ages" in midLoadout.coreItemIds)
    assertEquals("Q > W > E", midLoadout.skillOrder)

    // Explicit Support override on Norra
    val norra = champMap.getValue("norra")
    val norraSupport = norra.resolveForLane(Lane.SUPPORT)
    assertEquals("spectral_sickle", norraSupport.starterItemId)
    assertTrue("exhaust" in norraSupport.spellIds)

    // Dynamic off-meta synthesis on non-override lane (e.g., Darius in Support / Jungle)
    val darius = champMap.getValue("darius")
    val dariusSupport = darius.resolveForLane(Lane.SUPPORT)
    assertTrue(dariusSupport.isOffMetaLane)
    assertEquals("relic_shield", dariusSupport.starterItemId)
    assertTrue("exhaust" in dariusSupport.spellIds || "ignite" in dariusSupport.spellIds)

    val dariusJungle = darius.resolveForLane(Lane.JUNGLE)
    assertTrue(dariusJungle.isOffMetaLane)
    assertTrue("smite" in dariusJungle.spellIds)
    assertEquals(4, dariusJungle.minorRuneIds.size)
    assertTrue("relentless_hunter" in dariusJungle.minorRuneIds)
  }

  @Test
  fun `SmartItemEngine applies lane-specific skillOrder spells and core items`() {
    val seed = loadSeedPayload()
    val champMap = seed.champions.associateBy { it.id }
    val chogath = champMap.getValue("chogath")
    val enemies = listOf("darius", "lee_sin", "ahri", "jinx", "lulu").map { champMap.getValue(it) }

    val matchupEngine = MatchupCounterEngine()
    val itemEngine = SmartItemEngine()

    val (jngMatchups, jngProfile) = matchupEngine.analyzeMatchups(chogath, Lane.JUNGLE, enemies, seed.matchupRules)
    val jngBuild =
      itemEngine.buildRecommendation(
        myChampion = chogath,
        myLane = Lane.JUNGLE,
        enemies = enemies,
        matchups = jngMatchups,
        profile = jngProfile,
        allItems = seed.items,
        allRunes = seed.runes,
        allSpells = seed.spells,
      )
    assertTrue("Jungle SmartBuild must bring Smite", jngBuild.spells.any { it.id == "smite" })

    val (midMatchups, midProfile) = matchupEngine.analyzeMatchups(chogath, Lane.MID, enemies, seed.matchupRules)
    val midBuild =
      itemEngine.buildRecommendation(
        myChampion = chogath,
        myLane = Lane.MID,
        enemies = enemies,
        matchups = midMatchups,
        profile = midProfile,
        allItems = seed.items,
        allRunes = seed.runes,
        allSpells = seed.spells,
      )
    assertEquals("Q > W > E", midBuild.skillOrder)
    assertTrue(
      "Mid AP Cho'Gath SmartBuild should include Rod of Ages",
      midBuild.prioritizedItems.any { it.item.id == "rod_of_ages" },
    )
    assertTrue(midBuild.laneConditionNoteEn.isNotBlank() && midBuild.laneConditionNoteTh.isNotBlank())
  }

  @Test
  fun `GeminiCoachService routes Auto pool across Firebase and configured external keys`() {
    val service = GeminiCoachService()
    val keys = ExternalApiKeyStatus(hasGroqKey = true, hasOpenRouterKey = true, hasAiStudioKey = true)

    val autoPool = service.buildCandidateModels(AiModelCatalog.AUTO_MODEL_ID, firebaseReady = true, keyStatus = keys)
    assertTrue(autoPool.any { it.id == AiModelCatalog.FIREBASE_FLASH.id })
    assertTrue(autoPool.any { it.id == AiModelCatalog.GROQ_LLAMA_70B.id })
    assertTrue(autoPool.any { it.id == AiModelCatalog.OPENROUTER_FREE_ROUTER.id })
    assertTrue(autoPool.any { it.id == AiModelCatalog.AI_STUDIO_FLASH.id })

    val manualLocked =
      service.buildCandidateModels(AiModelCatalog.GROQ_LLAMA_70B.id, firebaseReady = true, keyStatus = keys)
    assertEquals(listOf(AiModelCatalog.GROQ_LLAMA_70B), manualLocked)

    val nextSuggestion =
      service.suggestNextModelId(
        excludingModelId = AiModelCatalog.FIREBASE_FLASH.id,
        firebaseReady = true,
        keyStatus = keys,
      )
    assertNotEquals(AiModelCatalog.FIREBASE_FLASH.id, nextSuggestion)

    val unauthorized = ExternalAiHttpException(401, AiProviderId.GROQ, "Invalid API Key")
    assertEquals(CoachErrorKind.INVALID_KEY, GeminiCoachService.classifyError(unauthorized))

    val groqLimit = ExternalAiHttpException(429, AiProviderId.GROQ, "Rate limit reached")
    assertEquals(CoachErrorKind.QUOTA_LIMIT, GeminiCoachService.classifyError(groqLimit))
    assertTrue(GeminiCoachService.shouldFallbackToSecondaryModel(groqLimit))
  }

  @Test
  fun `ExternalAiProvidersClient parses OpenAI SSE deltas AI Studio JSON and code fences`() {
    val openAiChunk = """{"choices":[{"delta":{"content":"ออก Stasis ก่อน"}}]}"""
    assertEquals(
      "ออก Stasis ก่อน",
      ExternalAiProvidersClient.extractOpenAiDeltaText(openAiChunk, AiProviderId.GROQ, 200),
    )

    val aiStudioChunk = """{"candidates":[{"content":{"parts":[{"text":"คุมเวฟ Lv 1–3"}]}}]}"""
    assertEquals("คุมเวฟ Lv 1–3", ExternalAiProvidersClient.extractAiStudioText(aiStudioChunk))

    val fenced = "```json\n{\"screenPhase\":\"CHAMP_SELECT\"}\n```"
    assertEquals("{\"screenPhase\":\"CHAMP_SELECT\"}", ExternalAiProvidersClient.stripCodeFence(fenced))
  }
}
