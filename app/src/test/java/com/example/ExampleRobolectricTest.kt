package com.example

import android.content.Context
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
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.AppLanguage
import com.example.data.model.CustomBuildData
import com.example.data.model.Lane
import com.example.data.repository.GameDataRepository
import com.example.data.repository.ImportResult
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
    assertEquals("Rift Companion", appName)
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
    composeTestRule.onNodeWithTag("settings_mini_mode_switch").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    // TopBar mini-mode toggle remains accessible
    composeTestRule.onNodeWithTag("topbar_mini_mode_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj analyzer item and spell icon taps open detail bottom sheets`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_launch_analyzer_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("analyzer_lazy_column").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("preset_heal_burst").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Switch to Smart Build tab and tap Rush Item Icon
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

    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()

    // Switch to Runes & Plan tab and tap Flash Spell Icon
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

    // Verify Skill title
    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_skill_title"))
    composeTestRule.onNodeWithTag("detail_skill_title").assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasText("Super Scorcher Breath", substring = true))
    composeTestRule.onNode(hasText("Super Scorcher Breath", substring = true)).assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("champion_detail_lazy_column")
      .performScrollToNode(hasTestTag("detail_item_essence_reaver"))
    composeTestRule.onNodeWithTag("detail_item_essence_reaver").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `cuj catalog tabs browse items and runes with detail sheets`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_champions").performClick()
    composeTestRule.waitForIdle()

    // Items Catalog Tab
    composeTestRule.onNodeWithTag("catalog_tab_items").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("items_catalog_lazy_column")
      .performScrollToNode(hasTestTag("catalog_item_kaenic_rookern"))
    composeTestRule.onNodeWithTag("catalog_item_kaenic_rookern").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("item_detail_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_item_sheet_button").performClick()
    composeTestRule.waitForIdle()

    // Runes Catalog Tab
    composeTestRule.onNodeWithTag("catalog_tab_runes").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("runes_catalog_lazy_column")
      .performScrollToNode(hasTestTag("catalog_rune_dark_harvest"))
    composeTestRule.onNodeWithTag("catalog_rune_dark_harvest").performClick()
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
            minorRuneIds = listOf("brutal", "giant_slayer", "bloodline"),
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

    // Change lane via compact dropdown pill
    composeTestRule.onNodeWithTag("coach_select_lane_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_lane_option_JUNGLE").performClick()
    composeTestRule.waitForIdle()

    // Toggle Mini-Window mode and ensure spotlight advice stays visible
    composeTestRule.onNodeWithTag("topbar_mini_mode_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_advice_spotlight").assertIsDisplayed()

    // Switch to Gemini AI Coach tab
    composeTestRule.onNodeWithTag("coach_gemini_tab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("coach_chat_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_question_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("coach_send_button").assertIsDisplayed()
  }
}
