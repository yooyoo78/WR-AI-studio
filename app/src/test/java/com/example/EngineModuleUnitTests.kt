package com.example

import com.example.data.ai.CoachErrorKind
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
import com.example.data.model.ThreatTag
import com.example.data.model.TierRank
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
    assertTrue("Expected at least 135 champions, found ${result.championsCount}", result.championsCount >= 135)
    assertTrue("Expected at least 75 items, found ${result.itemsCount}", result.itemsCount >= 75)
    assertTrue("Expected at least 44 runes, found ${result.runesCount}", result.runesCount >= 44)
    assertTrue("Expected no warnings, got ${result.warnings}", result.warnings.isEmpty())

    seed.champions.forEach { champ ->
      assertEquals(
        "Champion ${champ.id} must have 5 abilities (P, 1, 2, 3, 4)",
        setOf("P", "1", "2", "3", "4"),
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
    }
    val validTrees = setOf("PRECISION", "DOMINATION", "RESOLVE", "INSPIRATION")
    seed.runes.forEach { rune ->
      assertTrue("Rune ${rune.id} missing iconUrl", rune.iconUrl.startsWith("https://"))
      assertTrue("Rune ${rune.id} invalid tree '${rune.tree}'", rune.tree in validTrees)
    }
    seed.spells.forEach { spell ->
      assertTrue("Spell ${spell.id} missing iconUrl", spell.iconUrl.startsWith("https://"))
      assertTrue("Spell ${spell.id} missing cooldownNote", spell.cooldownNote.isNotBlank())
    }

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
        "solari_chargeblade",
        "malignance",
      )
      .forEach { wrId ->
        assertTrue("Wild Rift 7.3f item $wrId should exist in seed", wrId in itemIds)
      }

    val champIds = seed.champions.map { it.id }.toSet()
    listOf("talon", "lissandra", "poppy", "rumble", "gnar", "ryze", "nocturne", "zilean", "aurora", "velkoz", "kogmaw", "nidalee", "rell", "mel")
      .forEach { wrChamp ->
        assertTrue("Wild Rift champion $wrChamp should exist in seed", wrChamp in champIds)
      }

    val pcKeyRegex = Regex("\\([QWER]\\)")
    seed.champions.forEach { champ ->
      assertFalse(
        "Champion ${champ.id} skillOrder '${champ.skillOrder}' uses PC Q/W/E/R notation",
        champ.skillOrder.any { it in setOf('Q', 'W', 'E', 'R') },
      )
      champ.abilities.forEach { ab ->
        assertFalse(
          "Champion ${champ.id} ability '${ab.nameEn}' has PC (Q)/(W)/(E)/(R) tag",
          pcKeyRegex.containsMatchIn(ab.nameEn) || pcKeyRegex.containsMatchIn(ab.nameTh),
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
        abilities = listOf(AbilityInfo("4", "Public Execution", "Public Execution", "Dive slam", "พุ่งทุ่ม", "75s")),
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
        defaultMinorRuneIds = listOf("brutal", "last_stand", "bone_plating"),
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
}
