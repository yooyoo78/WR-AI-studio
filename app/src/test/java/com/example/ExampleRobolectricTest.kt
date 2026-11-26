package com.example

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppContainer
import com.example.data.ai.AiModelCatalog
import com.example.data.ai.AiProviderId
import com.example.data.ai.CoachSpeechController
import com.example.data.ai.CoachStreamEvent
import com.example.data.ai.GeminiCoachService
import com.example.data.ai.LiveCoachVisionService
import com.example.data.ai.LiveScreenPhase
import com.example.data.local.AiCoachCacheEntity
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SecureApiKeyStore
import com.example.data.model.AppLanguage
import com.example.data.model.CustomBuildData
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
import com.example.domain.engine.ChampionScanMatcher
import com.example.domain.engine.RawChampionScanPayload
import com.example.ui.viewmodel.CoachViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WR-AI", appName)
    val googleAppIdRes =
      context.resources.getIdentifier("google_app_id", "string", context.packageName)
    if (googleAppIdRes != 0) {
      assertTrue(
        "FirebaseApp should initialize from google-services.json resources",
        com.google.firebase.FirebaseApp.getApps(context).isNotEmpty(),
      )
    }
  }

  @Test
  fun `room repository seeds Patch 7_3f and validates integrity`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()

      val champs = repo.champions.first()
      val items = repo.items.first()
      assertTrue(champs.size >= 120)
      assertTrue(items.size >= 65)

      val validation = repo.validateCurrentDatabase()
      assertTrue(validation.isValid)

      // Toggle favorite persistence
      repo.toggleFavorite("sylas", currentlyFavorite = false)
      assertTrue("sylas" in repo.favoriteChampionIds.first())
    } finally {
      db.close()
    }
  }

  @Test
  fun `repository imports incremental Patch JSON dynamically`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()

      val customPatch =
        """
        {
          "packVersion": 1,
          "patchVersion": "7.4a",
          "lastUpdated": "2026-12-01",
          "sourceLabel": "Custom Patch 7.4a",
          "items": [
            {
              "id": "horizon_focus",
              "nameEn": "Horizon Focus",
              "nameTh": "Horizon Focus",
              "category": "OFFENSIVE",
              "cost": 2900,
              "statsSummaryEn": "+80 AP, +15 AH",
              "statsSummaryTh": "+80 AP, +15 เร่งสกิล",
              "passiveNameEn": "Hypershot",
              "passiveNameTh": "เปิดวิชั่นและเพิ่มแรงสกิลระยะไกล",
              "passiveDescEn": "Amplifies long-range ability damage.",
              "passiveDescTh": "เพิ่มดาเมจสกิลระยะไกลและเปิดเผยตำแหน่งศัตรู",
              "counterTags": ["MAGIC_PEN"],
              "damageAffinity": "AP",
              "patchVersion": "7.4a",
              "sourceType": "CUSTOM_USER_PACK"
            }
          ]
        }
        """.trimIndent()

      val result = repo.importPatchJson(customPatch)
      assertTrue(result is ImportResult.Success)
      assertTrue(repo.items.first().any { it.id == "horizon_focus" })
    } finally {
      db.close()
    }
  }

  @Test
  fun `cuj home to 1v5 analyzer preset produces threat and smart build`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_launch_analyzer_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Scroll to Step 2 and apply 5-enemy preset
    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Threat Spotlight is visible on Tab 0
    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("top_threat_card"))
    composeTestRule.onNodeWithTag("top_threat_card").assertIsDisplayed()

    // Switch to Smart Build tab
    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("analyzer_report_tab_1"))
    composeTestRule.onNodeWithTag("analyzer_report_tab_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("first_rush_item_card"))
    composeTestRule.onNodeWithTag("first_rush_item_card").assertIsDisplayed()
  }

  @Test
  fun `cuj settings switches Thai default to English cleanly`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("topbar_settings_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("settings_lang_en").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_title_text").assertIsDisplayed()

    // Switch back to Thai
    composeTestRule.onNodeWithTag("settings_lang_th").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_title_text").assertIsDisplayed()

    // Toggle In-Game Mini-Window Mode in Settings
    composeTestRule
      .onNodeWithTag("settings_lazy_column")
      .performScrollToNode(hasTestTag("settings_mini_mode_switch"))
    composeTestRule.onNodeWithTag("settings_mini_mode_switch").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    // TopBar mini-mode toggle remains accessible
    composeTestRule.onNodeWithTag("topbar_mini_mode_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj analyzer item and spell icon taps open mini box then detail sheets`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_launch_analyzer_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Switch to Smart Build tab and tap Rush Item Icon -> Stage 1 Mini Info Box -> Expand -> Stage 2 Sheet
    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("analyzer_report_tab_1"))
    composeTestRule.onNodeWithTag("analyzer_report_tab_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("analyzer_rush_item_icon"))
    composeTestRule.onNodeWithTag("analyzer_rush_item_icon").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()

    // Switch to Runes & Plan tab and tap Flash Spell Icon -> Stage 1 Mini Info Box -> Expand -> Stage 2 Sheet
    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("analyzer_report_tab_2"))
    composeTestRule.onNodeWithTag("analyzer_report_tab_2").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("analyzer_spell_flash"))
    composeTestRule.onNodeWithTag("analyzer_spell_flash").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("spell_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_spell_sheet_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj champions sort by winrate and inspect smolder kit and item`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("sort_winrate_chip").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("champions_lazy_column")
      .performScrollToNode(hasTestTag("champ_card_smolder"))
    composeTestRule.onNodeWithTag("champ_card_smolder").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Verify 3-Phase Game Plan card
    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_game_plan_title"))
    composeTestRule.onNodeWithTag("detail_game_plan_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("game_plan_tab_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("game_plan_content_1").assertIsDisplayed()

    // Verify Skill Strip + Mini Box -> Skill Detail Sheet
    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_skill_title"))
    composeTestRule.onNodeWithTag("detail_skill_title").assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasText("Super Scorcher Breath", substring = true))
    composeTestRule.onNode(hasText("Super Scorcher Breath", substring = true)).assertIsDisplayed()

    composeTestRule.onNodeWithTag("detail_skill_Q").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("skill_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_skill_sheet_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_item_essence_reaver"))
    composeTestRule.onNodeWithTag("detail_item_essence_reaver").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj catalog tabs browse items and runes with mini box and detail sheets`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    // Items Catalog Tab -> Mini Box -> Expand -> Item Detail Sheet
    composeTestRule.onNodeWithTag("catalog_tab_items").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("items_catalog_lazy_column")
      .performScrollToNode(hasTestTag("catalog_item_kaenic_rookern"))
    composeTestRule.onNodeWithTag("catalog_item_kaenic_rookern").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()

    // Runes Catalog Tab -> Mini Box -> Expand -> Rune Detail Sheet
    composeTestRule.onNodeWithTag("catalog_tab_runes").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("runes_catalog_lazy_column")
      .performScrollToNode(hasTestTag("catalog_rune_dark_harvest"))
    composeTestRule.onNodeWithTag("catalog_rune_dark_harvest").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_info_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_box_expand_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("rune_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_rune_sheet_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `repository persists and deletes custom builds cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()

      val buildId =
        repo.saveCustomBuild(
          CustomBuildData(
            name = "Jinx Crit Melt",
            championId = "jinx",
            lane = Lane.DRAGON,
            starterItemId = "long_sword",
            coreItemIds = listOf("yun_tal_wildarrows", "runaans_hurricane", "infinity_edge", "mortal_reminder"),
            bootsId = "berserkers_greaves",
            enchantId = "quicksilver_enchant",
            keystoneId = "lethal_tempo",
            minorRuneIds = listOf("brutal", "cut_down", "legend_bloodline", "bone_plating"),
            spellIds = listOf("flash", "barrier"),
            notes = "Anti-tank hypercarry build",
          )
        )
      val savedList = repo.customBuilds.first()
      assertEquals(1, savedList.size)
      assertEquals("Jinx Crit Melt", savedList.first().name)
      assertEquals("yun_tal_wildarrows", savedList.first().coreItemIds.first())

      repo.deleteCustomBuild(buildId)
      assertTrue(repo.customBuilds.first().isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun `cuj custom builds tab creates edits and displays custom item set`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("catalog_tab_custom_builds").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("create_custom_build_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("custom_build_editor_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("custom_build_name_input").performTextClearance()
    composeTestRule.onNodeWithTag("custom_build_name_input").performTextInput("My WR 7.3f Test Build")
    composeTestRule.onNodeWithTag("custom_build_save_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("My WR 7.3f Test Build").assertIsDisplayed()
  }

  @Test
  fun `cuj coach screen keeps advice spotlight prominent in normal and mini mode`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_coach").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("coach_compact_topbar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_advice_spotlight").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_playbook_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_speak_button_playbook_0").assertIsDisplayed()

    // Change lane via compact dropdown pill
    composeTestRule.onNodeWithTag("coach_select_lane_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_lane_option_JUNGLE").performClick()
    composeTestRule.waitForIdle()

    // Toggle Mini-Window mode and ensure spotlight advice stays visible
    composeTestRule.onNodeWithTag("topbar_mini_mode_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_advice_spotlight").assertIsDisplayed()

    // Switch to Gemini AI Coach tab and verify curated quick prompts + in-screen Model Switcher bar
    composeTestRule.onNodeWithTag("coach_gemini_tab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_model_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_model_selector_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_api_keys_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_cache_stats_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_chat_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_quick_prompt_0").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_question_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_send_button").assertIsDisplayed()

    // Open in-screen Model Picker sheet & close
    composeTestRule.onNodeWithTag("coach_model_selector_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("model_picker_sheet").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("model_picker_lazy_column")
      .performScrollToNode(hasTestTag("model_option_firebase:gemini-3.5-flash-lite"))
    composeTestRule.onNodeWithTag("model_option_firebase:gemini-3.5-flash-lite").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_model_picker_button").performClick()
    composeTestRule.waitForIdle()

    // Open Free API Keys sheet from AI Coach & verify Groq / OpenRouter / AI Studio inputs
    composeTestRule.onNodeWithTag("coach_api_keys_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("free_api_keys_sheet").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("free_keys_lazy_column")
      .performScrollToNode(hasTestTag("api_key_input_GROQ"))
    composeTestRule.onNodeWithTag("api_key_input_GROQ").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("free_keys_lazy_column")
      .performScrollToNode(hasTestTag("api_key_input_OPENROUTER"))
    composeTestRule.onNodeWithTag("api_key_input_OPENROUTER").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("free_keys_lazy_column")
      .performScrollToNode(hasTestTag("api_key_input_GOOGLE_AI_STUDIO"))
    composeTestRule.onNodeWithTag("api_key_input_GOOGLE_AI_STUDIO").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_free_keys_button").performClick()
    composeTestRule.waitForIdle()

    // Switch to Live Coach tab and verify on-demand manual capture controls & voice toggle
    composeTestRule.onNodeWithTag("coach_live_tab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_model_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_live_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_live_capture_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_live_pick_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_live_autospeak_toggle").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_live_empty_card").assertIsDisplayed()
  }

  @Test
  fun `champion scan matcher skips unpicked slots and resolves wild rift roster`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()
      val catalog = repo.champions.first()

      val matcher = ChampionScanMatcher()
      val result =
        matcher.resolve(
          RawChampionScanPayload(
            screenType = "CHAMP_SELECT",
            myChampionName = "Kai'Sa",
            detectedLane = "DRAGON",
            allies = listOf("Kai'Sa", "Picking...", "Lee Sin", ""),
            enemies = listOf("Dr. Mundo", "empty", "Ahri", "กำลังเลือก..."),
            skippedEmptySlots = 4,
            confidenceNote = "Partial draft captured",
          ),
          catalog,
        )

      assertEquals("kaisa", result.myChampion?.id)
      assertEquals(Lane.DRAGON, result.detectedLane)
      assertEquals(listOf("kaisa", "lee_sin"), result.allies.map { it.id })
      assertEquals(listOf("dr_mundo", "ahri"), result.enemies.map { it.id })
      assertTrue(result.skippedUnpickedCount >= 6)
    } finally {
      db.close()
    }
  }

  @Test
  fun `app container applyDetectedRoster syncs 1v5 draft and ai coach contexts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    container.applyDetectedRoster(
      myChampionId = "jinx",
      lane = Lane.DRAGON,
      allyIds = listOf("jinx", "lulu"),
      enemyIds = listOf("draven", "thresh", "zed"),
    )

    val matchup = container.sharedMatchupContext.value
    val draft = container.sharedDraftContext.value
    assertEquals("jinx", matchup.myChampionId)
    assertEquals(Lane.DRAGON, matchup.lane)
    assertEquals(listOf("draven", "thresh", "zed"), matchup.enemyIds)
    assertEquals(listOf("jinx", "lulu"), draft.allyIds)
    assertEquals(listOf("draven", "thresh", "zed"), draft.enemyIds)
    assertTrue(matchup.externalUpdateSeq > 0L)
    assertEquals(matchup.externalUpdateSeq, draft.externalUpdateSeq)
  }

  @Test
  fun `cuj topbar quick scan opens capture and picker sheet`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("topbar_quick_scan_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("quick_scan_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_scan_start_capture_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_scan_pick_image_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_scan_close_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `coach speech sanitizer strips markdown and cleans bullets`() {
    val raw =
      """
      ### แผนต้นเกม **Ahri**
      - เก็บสกิล 1 เคลียร์เวฟ
      • ระวัง *Zed* เลเวล 5 `Shadow`
      """.trimIndent()
    val cleaned = CoachSpeechController.sanitizeForSpeech(raw)
    assertTrue(!cleaned.contains("**"))
    assertTrue(!cleaned.contains("###"))
    assertTrue(!cleaned.contains("`"))
    assertTrue(cleaned.contains("Ahri"))
    assertTrue(cleaned.contains("Zed"))
  }

  @Test
  fun `coach quick prompts adapt to champion lane and enemy context`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()
      val champs = repo.champions.first()
      val ahri = champs.first { it.id == "ahri" }
      val zed = champs.first { it.id == "zed" }

      val thPrompts = CoachViewModel.buildQuickPrompts(ahri, Lane.MID, listOf(zed), AppLanguage.TH)
      assertEquals(5, thPrompts.size)
      assertTrue(thPrompts.any { it.shortLabel.contains("Zed", ignoreCase = true) })
      assertTrue(thPrompts.all { it.fullQuestion.contains("Ahri", ignoreCase = true) })

      val enPrompts = CoachViewModel.buildQuickPrompts(ahri, Lane.MID, listOf(zed), AppLanguage.EN)
      assertEquals(5, enPrompts.size)
      assertTrue(enPrompts.any { it.shortLabel.contains("Zed", ignoreCase = true) })
    } finally {
      db.close()
    }
  }

  @Test
  fun `live coach vision service parses champ select and loading JSON cleanly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, db.gameDao(), prefs)
      repo.ensureSeeded()
      val catalog = repo.champions.first()
      val service = LiveCoachVisionService(context)

      val draftJson =
        """
        {
          "screenPhase": "CHAMP_SELECT",
          "myChampionName": "",
          "detectedLane": "MID",
          "allies": ["Lee Sin", "Picking..."],
          "enemies": ["Zed", "Dr. Mundo", "empty"],
          "skippedEmptySlots": 2,
          "headline": "ควรหยิบเมจมีกันเบิร์สและแก้ทางฮีล",
          "recommendedPicks": [
            {"championName": "Lissandra", "reason": "ล็อก Zed ในไฟต์และคุมโซนง่าย"},
            {"championName": "Ahri", "reason": "หนีคอมโบ Zed ด้วยอัลติ"}
          ],
          "recommendedItems": ["Plated Steelcaps + Stasis", "Oblivion Orb"],
          "earlyLanePlan": "คุมเวฟใกล้ป้อมก่อน Lv 5 แล้วเก็บ Stasis",
          "threatAlert": "ระวัง Zed Lv 5 all-in",
          "spokenBriefing": "แนะนำ Lissandra หรือ Ahri เพื่อคุม Zed และออก Stasis กับตัดฮีล"
        }
        """.trimIndent()

      val draftParsed = service.parseAndResolve(draftJson, catalog, null, Lane.MID)
      assertEquals(LiveScreenPhase.CHAMP_SELECT, draftParsed.phase)
      assertEquals(Lane.MID, draftParsed.detectedLane)
      assertEquals(listOf("lee_sin"), draftParsed.allies.map { it.id })
      assertEquals(listOf("zed", "dr_mundo"), draftParsed.enemies.map { it.id })
      assertEquals(2, draftParsed.recommendedPicks.size)
      assertEquals("ahri", draftParsed.recommendedPicks[1].resolvedChampion?.id)
      assertEquals(2, draftParsed.recommendedItems.size)
      assertTrue(draftParsed.skippedUnpickedCount >= 2)
    } finally {
      db.close()
    }
  }

  @Test
  fun `cuj tapping champion portrait in 1v5 analyzer opens profile while swap opens picker`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_launch_analyzer_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Swap button still opens picker
    composeTestRule.onNodeWithTag("select_my_champion_card").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("close_picker_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Apply preset enemies, then tap enemy slot portrait to open Champion Profile
    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("enemy_slot_profile_0").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("champion_detail_lazy_column").assertIsDisplayed()
    composeTestRule.onNodeWithTag("detail_back_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj search chogath verify pqwer skills and switch jungle vs mid lane builds`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("champions_search_input").performTextInput("Cho")
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("champions_lazy_column")
      .performScrollToNode(hasTestTag("champ_card_chogath"))
    composeTestRule.onNodeWithTag("champ_card_chogath").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Verify P/Q/W/E/R Skill Strip
    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_skill_title"))
    composeTestRule.onNodeWithTag("detail_skill_P").assertIsDisplayed()
    composeTestRule.onNodeWithTag("detail_skill_Q").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("detail_skill_row")
      .performScrollToNode(hasTestTag("detail_skill_R"))
    composeTestRule.onNodeWithTag("detail_skill_R").assertIsDisplayed()

    // Switch lane to JUNGLE and verify Jungle Smite + condition callout appear
    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_lane_JUNGLE"))
    composeTestRule.onNodeWithTag("detail_lane_JUNGLE").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("detail_lane_condition").assertIsDisplayed()
    composeTestRule.onNodeWithTag("detail_spell_smite").performScrollTo().assertIsDisplayed()

    // Switch lane to MID and verify AP Rod of Ages core appears
    composeTestRule.onNodeWithTag("detail_lane_row").performScrollTo()
    composeTestRule
      .onNodeWithTag("detail_lane_row")
      .performScrollToNode(hasTestTag("detail_lane_MID"))
    composeTestRule.onNodeWithTag("detail_lane_MID").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("detail_lane_condition").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("detail_build_items_row").performScrollTo()
    composeTestRule
      .onNodeWithTag("detail_build_items_row")
      .performScrollToNode(hasTestTag("detail_item_rod_of_ages"))
    composeTestRule.onNodeWithTag("detail_item_rod_of_ages").assertIsDisplayed()
  }

  @Test
  fun `migration 4 to 5 and seed refresh preserve user custom builds favorites and saved matchups`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "migration_4_5_preservation_test.db"
    context.deleteDatabase(dbName)

    val v4OpenHelper =
      FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbName)
          .callback(
            object : SupportSQLiteOpenHelper.Callback(4) {
              override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS champions (
                    id TEXT NOT NULL PRIMARY KEY,
                    nameEn TEXT NOT NULL,
                    nameTh TEXT NOT NULL,
                    titleEn TEXT NOT NULL,
                    titleTh TEXT NOT NULL,
                    roles TEXT NOT NULL,
                    lanes TEXT NOT NULL,
                    damageType TEXT NOT NULL,
                    difficulty INTEGER NOT NULL,
                    tierRank TEXT NOT NULL,
                    winRatePct REAL NOT NULL,
                    pickRatePct REAL NOT NULL,
                    banRatePct REAL NOT NULL,
                    portraitUrl TEXT NOT NULL,
                    earlyPower INTEGER NOT NULL,
                    midPower INTEGER NOT NULL,
                    latePower INTEGER NOT NULL,
                    burstRating INTEGER NOT NULL,
                    dpsRating INTEGER NOT NULL,
                    tankinessRating INTEGER NOT NULL,
                    ccRating INTEGER NOT NULL,
                    mobilityRating INTEGER NOT NULL,
                    sustainRating INTEGER NOT NULL,
                    abilities TEXT NOT NULL,
                    strengthsEn TEXT NOT NULL,
                    strengthsTh TEXT NOT NULL,
                    weaknessesEn TEXT NOT NULL,
                    weaknessesTh TEXT NOT NULL,
                    threatTags TEXT NOT NULL,
                    starterItemId TEXT NOT NULL,
                    coreItemIds TEXT NOT NULL,
                    defaultBootsId TEXT NOT NULL,
                    defaultEnchantId TEXT NOT NULL,
                    defaultKeystoneId TEXT NOT NULL,
                    defaultMinorRuneIds TEXT NOT NULL,
                    defaultSpellIds TEXT NOT NULL,
                    skillOrder TEXT NOT NULL,
                    counterChampionIds TEXT NOT NULL,
                    favorableAgainstIds TEXT NOT NULL,
                    earlyPlanEn TEXT NOT NULL,
                    earlyPlanTh TEXT NOT NULL,
                    midPlanEn TEXT NOT NULL,
                    midPlanTh TEXT NOT NULL,
                    latePlanEn TEXT NOT NULL,
                    latePlanTh TEXT NOT NULL,
                    patchVersion TEXT NOT NULL,
                    lastUpdated TEXT NOT NULL,
                    sourceType TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS items (
                    id TEXT NOT NULL PRIMARY KEY,
                    nameEn TEXT NOT NULL,
                    nameTh TEXT NOT NULL,
                    category TEXT NOT NULL,
                    cost INTEGER NOT NULL,
                    statsSummaryEn TEXT NOT NULL,
                    statsSummaryTh TEXT NOT NULL,
                    passiveNameEn TEXT NOT NULL,
                    passiveNameTh TEXT NOT NULL,
                    passiveDescEn TEXT NOT NULL,
                    passiveDescTh TEXT NOT NULL,
                    counterTags TEXT NOT NULL,
                    damageAffinity TEXT NOT NULL,
                    patchVersion TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    iconUrl TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS runes (
                    id TEXT NOT NULL PRIMARY KEY,
                    nameEn TEXT NOT NULL,
                    nameTh TEXT NOT NULL,
                    slotType TEXT NOT NULL,
                    descEn TEXT NOT NULL,
                    descTh TEXT NOT NULL,
                    patchVersion TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    iconUrl TEXT NOT NULL,
                    tree TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS spells (
                    id TEXT NOT NULL PRIMARY KEY,
                    nameEn TEXT NOT NULL,
                    nameTh TEXT NOT NULL,
                    descEn TEXT NOT NULL,
                    descTh TEXT NOT NULL,
                    patchVersion TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    iconUrl TEXT NOT NULL,
                    cooldownNote TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS matchup_rules (
                    id TEXT NOT NULL PRIMARY KEY,
                    champAId TEXT NOT NULL,
                    champBId TEXT NOT NULL,
                    advantageScore INTEGER NOT NULL,
                    laneDifficulty TEXT NOT NULL,
                    dangerAbilitiesEn TEXT NOT NULL,
                    dangerAbilitiesTh TEXT NOT NULL,
                    tipEn TEXT NOT NULL,
                    tipTh TEXT NOT NULL,
                    keySpikeEn TEXT NOT NULL,
                    keySpikeTh TEXT NOT NULL,
                    patchVersion TEXT NOT NULL,
                    sourceType TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS patch_notes (
                    version TEXT NOT NULL PRIMARY KEY,
                    releaseDate TEXT NOT NULL,
                    headlineEn TEXT NOT NULL,
                    headlineTh TEXT NOT NULL,
                    summaryEn TEXT NOT NULL,
                    summaryTh TEXT NOT NULL,
                    sourceLabel TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    changes TEXT NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS favorite_champions (
                    championId TEXT NOT NULL PRIMARY KEY,
                    addedAt INTEGER NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS saved_matchups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    myChampionId TEXT NOT NULL,
                    lane TEXT NOT NULL,
                    enemyIds TEXT NOT NULL,
                    label TEXT NOT NULL,
                    savedAt INTEGER NOT NULL
                  )
                  """.trimIndent()
                )
                db.execSQL(
                  """
                  CREATE TABLE IF NOT EXISTS custom_builds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    championId TEXT NOT NULL,
                    lane TEXT NOT NULL,
                    starterItemId TEXT NOT NULL,
                    coreItemIds TEXT NOT NULL,
                    bootsId TEXT NOT NULL,
                    enchantId TEXT NOT NULL,
                    keystoneId TEXT NOT NULL,
                    minorRuneIds TEXT NOT NULL,
                    spellIds TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                  )
                  """.trimIndent()
                )
              }

              override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }
          )
          .build()
      )

    v4OpenHelper.writableDatabase.use { rawDb ->
      rawDb.execSQL("INSERT INTO favorite_champions (championId, addedAt) VALUES ('sylas', 1700000000000)")
      rawDb.execSQL(
        "INSERT INTO saved_matchups (myChampionId, lane, enemyIds, label, savedAt) VALUES ('darius', 'BARON', '[\"malphite\"]', 'Top vs Malphite', 1700000000000)"
      )
      rawDb.execSQL(
        """
        INSERT INTO custom_builds (name, championId, lane, starterItemId, coreItemIds, bootsId, enchantId, keystoneId, minorRuneIds, spellIds, notes, updatedAt)
        VALUES ('My Saved Akshan Set', 'akshan', 'MID', 'long_sword', '["botrk","collector","infinity_edge","mortal_reminder"]', 'berserkers_greaves', 'stasis_enchant', 'press_the_attack', '["brutal","coup_de_grace","alacrity"]', '["flash","ignite"]', 'Keep user notes', 1700000000000)
        """.trimIndent()
      )
    }
    v4OpenHelper.close()

    val migratedDb =
      Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
        .allowMainThreadQueries()
        .build()

    try {
      val prefs = PreferencesRepository(context)
      val repo = GameDataRepository(context, migratedDb.gameDao(), prefs)
      repo.ensureSeeded()

      val champs = repo.champions.first()
      assertTrue("Expected 144 champions after v4->v6 seed refresh, got ${champs.size}", champs.size >= 144)
      assertTrue(champs.any { it.id == "chogath" })
      assertTrue(champs.any { it.id == "norra" })

      val savedBuilds = repo.customBuilds.first()
      assertEquals(1, savedBuilds.size)
      assertEquals("My Saved Akshan Set", savedBuilds.first().name)
      assertEquals("Keep user notes", savedBuilds.first().notes)

      val favs = repo.favoriteChampionIds.first()
      assertTrue("sylas" in favs)

      val matchups = repo.savedMatchups.first()
      assertEquals(1, matchups.size)
      assertEquals("Top vs Malphite", matchups.first().label)
    } finally {
      migratedDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `secure api key store saves masks and clears external provider keys`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = SecureApiKeyStore(context)
    AiProviderId.entries.filter { it.requiresApiKey }.forEach { store.clearKey(it) }

    store.saveKey(AiProviderId.GROQ, "gsk_test_1234567890abcd")
    store.saveKey(AiProviderId.OPENROUTER, "sk-or-v1-abcdef123456")

    assertEquals("gsk_test_1234567890abcd", store.getKey(AiProviderId.GROQ))
    assertEquals("sk-or-v1-abcdef123456", store.getKey(AiProviderId.OPENROUTER))
    assertTrue(store.keyStatusFlow.value.hasGroqKey)
    assertTrue(store.keyStatusFlow.value.hasOpenRouterKey)
    assertTrue(!store.keyStatusFlow.value.hasAiStudioKey)
    assertEquals(2, store.keyStatusFlow.value.configuredExternalCount)
    assertTrue(store.keyStatusFlow.value.groqPreview.contains("abcd"))

    store.clearKey(AiProviderId.GROQ)
    assertEquals(null, store.getKey(AiProviderId.GROQ))
    assertEquals(1, store.keyStatusFlow.value.configuredExternalCount)
    store.clearKey(AiProviderId.OPENROUTER)
  }

  @Test
  fun `persistent room ai coach cache serves repeated questions at 0 tokens across service instances`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
      try {
        val prefs = PreferencesRepository(context)
        val repo = GameDataRepository(context, db.gameDao(), prefs)
        repo.ensureSeeded()
        val ahri = repo.champions.first().first { it.id == "ahri" }
        val zed = repo.champions.first().first { it.id == "zed" }
        val question = "ช่วง Lv 1-3 ควรเทรด Zed ยังไง?"
        val cacheKey =
          GeminiCoachService.buildCacheKey(
            question = question,
            champion = ahri,
            lane = Lane.MID,
            enemies = listOf(zed),
            patchVersion = "7.3f",
            language = AppLanguage.TH,
          )

        val now = System.currentTimeMillis()
        db.gameDao()
          .upsertCoachCacheEntry(
            AiCoachCacheEntity(
              cacheKey = cacheKey,
              normalizedQuestion = GeminiCoachService.normalizeQuestion(question),
              championId = ahri.id,
              lane = Lane.MID.name,
              patchVersion = "7.3f",
              languageCode = AppLanguage.TH.code,
              responseText = "• เก็บ Charm ไว้สวนตอน Zed พุ่ง R\n• ออก Stasis Enchant ชิ้นแรก",
              modelId = AiModelCatalog.GROQ_LLAMA_70B.id,
              modelLabel = AiModelCatalog.GROQ_LLAMA_70B.displayName,
              createdAt = now,
              lastAccessedAt = now,
              hitCount = 0,
            )
          )

        // Fresh GeminiCoachService instance (simulating cold app restart with empty memory cache)
        val freshService = GeminiCoachService(context = null, gameDao = db.gameDao())
        val events = mutableListOf<CoachStreamEvent>()
        freshService
          .streamCoachingAdvice(
            question = "  ช่วง   Lv 1-3 ควรเทรด Zed ยังไง? ",
            champion = ahri,
            lane = Lane.MID,
            enemies = listOf(zed),
            patchVersion = "7.3f",
            language = AppLanguage.TH,
          )
          .collect { events += it }

        assertEquals(1, events.size)
        val completed = events.single() as CoachStreamEvent.Completed
        assertTrue(completed.fromCache)
        assertTrue(completed.finalText.contains("Stasis Enchant"))
        assertEquals(AiModelCatalog.GROQ_LLAMA_70B.displayName, completed.modelLabel)

        val stats = db.gameDao().observeCoachCacheStats().first()
        assertEquals(1, stats.entryCount)
        assertEquals(1, stats.totalHitsSaved)

        freshService.clearPersistentCache()
        assertEquals(0, db.gameDao().observeCoachCacheStats().first().entryCount)
      } finally {
        db.close()
      }
    }

  @Test
  fun `cuj journal tab opens post game review saves match and displays stats`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("catalog_tab_journal").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("journal_scan_post_game_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("post_game_review_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("post_game_accuracy_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("post_game_save_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("post_game_close_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("journal_summary_card").assertIsDisplayed()
  }

  @Test
  fun `cuj analyzer shows matchup confidence badge for analyzed enemy`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_launch_analyzer_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("matchup_card_soraka"))
    composeTestRule.onNodeWithTag("matchup_badge_soraka", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `cuj patch center displays signed ota config and check controls`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_patch_card").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("patch_overview_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("patch_open_notes_sheet_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("patch_open_ota_sheet_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("patch_ota_config_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ota_config_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ota_repo_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ota_pubkey_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ota_check_button").assertIsDisplayed()
  }

  @Test
  fun `cuj topbar version check pill displays red green led and opens version sheet`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("topbar_version_check_button").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("topbar_version_led", useUnmergedTree = true)
      .assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_version_check_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("live_version_check_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("version_sheet_recheck_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("version_sheet_open_patch_button").assertIsDisplayed()
  }

  @Test
  fun `cuj home server selector switches server and triggers refresh cleanly`() {
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("top_played_meta_widget"))
    composeTestRule.onNodeWithTag("meta_server_selector_chip").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("meta_server_option_GLOBAL_SEA").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("meta_refresh_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("meta_refresh_status").assertIsDisplayed()
  }

  @Test
  fun `cuj decluttered champions filter sheet and analyzer matchup detail sheet`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("champions_filter_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("champions_filter_sheet").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_analyzer").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("analyzer_lazy_column")
      .performScrollToNode(hasTestTag("matchup_card_soraka"))
    composeTestRule.onNodeWithTag("matchup_card_soraka").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("analyzer_matchup_detail_sheet").assertIsDisplayed()
  }

  @Test
  fun `cuj home top 5 ranked meta widget switches metric and filters by lane`() {
    fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("TestTag starts with '$prefix'") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
      }

    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("top_played_meta_widget"))
    composeTestRule.onNodeWithTag("top_played_meta_widget").assertIsDisplayed()
    composeTestRule.onAllNodes(hasTestTagPrefix("top_meta_row_")).assertCountEquals(5)

    // Switch metric mode to Win Rate and filter by Mid lane
    composeTestRule.onNodeWithTag("top_meta_sort_HIGHEST_WINRATE").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("top_meta_lane_MID").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodes(hasTestTagPrefix("top_meta_row_")).assertCountEquals(5)

    // Expand the #1 row to verify inline power curve breakdown & View Kit button
    composeTestRule.onAllNodes(hasTestTagPrefix("top_meta_row_"))[0].performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onAllNodes(hasTestTagPrefix("top_meta_expand_"), useUnmergedTree = true)
      .assertCountEquals(1)
    composeTestRule.onAllNodes(hasTestTagPrefix("top_meta_kit_"))[0].performScrollTo().assertIsDisplayed()

    // Launch 1v5 Analyzer directly from Top 5 widget row
    composeTestRule.onAllNodes(hasTestTagPrefix("top_meta_analyze_"))[0].performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("analyzer_lazy_column").assertIsDisplayed()
  }

  @Test
  fun `cuj champions screen shows active patch badge and delta import syncs stats items skills and all patch labels`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("champions_patch_badge").assertIsDisplayed()

    val appContext = ApplicationProvider.getApplicationContext<Context>()
    val container = MainActivity::class.java
    val prefsRepo = PreferencesRepository(appContext)
    val db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).allowMainThreadQueries().build()
    val repo = GameDataRepository(appContext, db.gameDao(), prefsRepo)

    runBlocking {
      repo.ensureSeeded()
      val deltaJson =
        """
        {
          "packVersion": 3,
          "patchVersion": "7.4a",
          "lastUpdated": "2026-11-26",
          "sourceLabel": "OTA Delta Test",
          "championUpdates": [
            {
              "id": "ahri",
              "winRatePct": 55.1,
              "abilityUpdates": [
                {
                  "slot": "Q",
                  "cooldownNote": "5s"
                }
              ]
            }
          ],
          "itemUpdates": [
            {
              "id": "statikk_shiv",
              "cost": 2800
            }
          ],
          "patchNotes": [
            {
              "version": "7.4a",
              "releaseDate": "2026-11-26",
              "headlineEn": "7.4a",
              "headlineTh": "7.4a",
              "summaryEn": "Delta",
              "summaryTh": "เดลตา",
              "sourceLabel": "OTA",
              "isActive": true,
              "changes": [
                {
                  "targetName": "Ahri",
                  "changeType": "CHANGE",
                  "detailEn": "Q cooldown reduced",
                  "detailTh": "ลดคูลดาวน์ Q"
                }
              ]
            }
          ]
        }
        """.trimIndent()
      val res = repo.importPatchJson(deltaJson, replaceExisting = false)
      assertTrue(res is ImportResult.Success)
      val champs = repo.champions.first()
      val ahri = champs.first { it.id == "ahri" }
      val garen = champs.first { it.id == "garen" }
      assertEquals(55.1, ahri.winRatePct, 0.01)
      assertEquals("5s", ahri.abilities.first { it.slot.equals("Q", true) }.cooldownNote)
      assertEquals("7.4a", ahri.patchVersion)
      assertEquals("7.4a", garen.patchVersion)
      val shiv = repo.items.first().first { it.id == "statikk_shiv" }
      assertEquals(2800, shiv.cost)
      assertEquals("7.4a", shiv.patchVersion)
    }
    db.close()
  }
}
