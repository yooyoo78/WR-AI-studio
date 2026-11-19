package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.AppContainer
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.ChampionDetailScreen
import com.example.ui.screens.ChampionsScreen
import com.example.ui.screens.CoachScreen
import com.example.ui.screens.DraftScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PatchMetaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ArcaneCyan
import com.example.ui.theme.HextechGold
import com.example.ui.theme.LocalUiStrings
import com.example.ui.theme.RiftCompanionTheme
import com.example.ui.viewmodel.AnalyzerViewModel
import com.example.ui.viewmodel.ChampionsViewModel
import com.example.ui.viewmodel.CoachViewModel
import com.example.ui.viewmodel.DraftViewModel
import com.example.ui.viewmodel.HomeAndPatchViewModel

object NavRoutes {
  const val HOME = "home"
  const val ANALYZER = "analyzer"
  const val CHAMPIONS = "champions"
  const val DRAFT = "draft"
  const val COACH = "coach"
  const val CHAMPION_DETAIL = "champion_detail/{championId}"
  const val PATCH_CENTER = "patch_center"
  const val SETTINGS = "settings"

  fun championDetail(id: String) = "champion_detail/$id"
}

private data class TopLevelNavItem(
  val route: String,
  val labelProvider: @Composable () -> String,
  val icon: ImageVector,
  val testTag: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiftCompanionApp(appContainer: AppContainer) {
  val homePatchVm: HomeAndPatchViewModel =
    viewModel(
      factory =
        HomeAndPatchViewModel.Factory(
          appContainer.gameDataRepository,
          appContainer.preferencesRepository,
        )
    )
  val analyzerVm: AnalyzerViewModel =
    viewModel(
      factory =
        AnalyzerViewModel.Factory(
          appContainer.gameDataRepository,
          appContainer.matchupCounterEngine,
          appContainer.smartItemEngine,
          appContainer,
        )
    )
  val championsVm: ChampionsViewModel =
    viewModel(factory = ChampionsViewModel.Factory(appContainer.gameDataRepository))
  val draftVm: DraftViewModel =
    viewModel(
      factory =
        DraftViewModel.Factory(
          appContainer.gameDataRepository,
          appContainer.draftAssistantEngine,
        )
    )
  val coachVm: CoachViewModel =
    viewModel(
      factory =
        CoachViewModel.Factory(
          appContainer.gameDataRepository,
          appContainer.preferencesRepository,
          appContainer.offlineCoachEngine,
          appContainer.geminiCoachService,
          appContainer,
        )
    )

  val prefs by homePatchVm.preferences.collectAsStateWithLifecycle()
  val homeState by homePatchVm.homeUiState.collectAsStateWithLifecycle()
  val patchActionStatus by homePatchVm.patchActionStatus.collectAsStateWithLifecycle()
  val validationResult by homePatchVm.validationResult.collectAsStateWithLifecycle()
  val analyzerState by analyzerVm.uiState.collectAsStateWithLifecycle()
  val championsState by championsVm.uiState.collectAsStateWithLifecycle()
  val draftState by draftVm.uiState.collectAsStateWithLifecycle()
  val coachState by coachVm.uiState.collectAsStateWithLifecycle()
  val geminiState by coachVm.geminiState.collectAsStateWithLifecycle()
  val coachMessages by coachVm.messages.collectAsStateWithLifecycle()

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 680.dp
    val autoCompactWindow = maxWidth < 380.dp || maxHeight < 560.dp
    val effectiveMiniMode = prefs.inGameMiniMode || autoCompactWindow

    RiftCompanionTheme(preferences = prefs, inGameMiniMode = effectiveMiniMode) {
      val navController = rememberNavController()
      val backStackEntry by navController.currentBackStackEntryAsState()
      val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.HOME
      val strings = LocalUiStrings.current

      val navItems =
        listOf(
          TopLevelNavItem(NavRoutes.HOME, { strings.navHome }, Icons.Default.Home, "nav_home"),
          TopLevelNavItem(
            NavRoutes.ANALYZER,
            { strings.navAnalyzer },
            Icons.Default.Security,
            "nav_analyzer",
          ),
          TopLevelNavItem(
            NavRoutes.CHAMPIONS,
            { strings.navChampions },
            Icons.Default.Shield,
            "nav_champions",
          ),
          TopLevelNavItem(NavRoutes.DRAFT, { strings.navDraft }, Icons.Default.Groups, "nav_draft"),
          TopLevelNavItem(
            NavRoutes.COACH,
            { strings.navCoach },
            Icons.Default.AutoAwesome,
            "nav_coach",
          ),
        )

      val showChrome = currentRoute in navItems.map { it.route }

      fun navigateTopLevel(route: String) {
        navController.navigate(route) {
          popUpTo(NavRoutes.HOME) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
          if (showChrome) {
            TopAppBar(
              title = {
                Text(
                  text =
                    when (currentRoute) {
                      NavRoutes.HOME -> strings.appTitle
                      NavRoutes.ANALYZER -> strings.navAnalyzer
                      NavRoutes.CHAMPIONS -> strings.navChampions
                      NavRoutes.DRAFT -> strings.navDraft
                      NavRoutes.COACH -> strings.navCoach
                      else -> strings.appTitle
                    },
                  style =
                    if (effectiveMiniMode) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  titleContentColor = HextechGold,
                ),
              actions = {
                IconButton(
                  onClick = { homePatchVm.setInGameMiniMode(!prefs.inGameMiniMode) },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("topbar_mini_mode_button"),
                ) {
                  Icon(
                    if (prefs.inGameMiniMode) Icons.Default.CloseFullscreen
                    else Icons.Default.PictureInPictureAlt,
                    contentDescription = strings.miniModeTitle,
                    tint =
                      if (effectiveMiniMode) HextechGold
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(
                  onClick = { navController.navigate(NavRoutes.PATCH_CENTER) },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("topbar_patch_button"),
                ) {
                  Icon(
                    Icons.Default.Update,
                    contentDescription = strings.patchCenterTitle,
                    tint = ArcaneCyan,
                  )
                }
                IconButton(
                  onClick = { navController.navigate(NavRoutes.SETTINGS) },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("topbar_settings_button"),
                ) {
                  Icon(
                    Icons.Default.Settings,
                    contentDescription = strings.settingsTitle,
                    tint = HextechGold,
                  )
                }
              },
            )
          }
        },
        bottomBar = {
          if (showChrome && !useNavRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = if (effectiveMiniMode) Modifier.height(60.dp) else Modifier,
            ) {
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(item.route) },
                  alwaysShowLabel = !effectiveMiniMode,
                  modifier = Modifier.testTag(item.testTag),
                  icon = { Icon(item.icon, contentDescription = item.labelProvider()) },
                  label = {
                    Text(
                      text = item.labelProvider(),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (showChrome && useNavRail) {
            NavigationRail(
              modifier = Modifier.fillMaxHeight(),
              containerColor = MaterialTheme.colorScheme.surface,
            ) {
              navItems.forEach { item ->
                NavigationRailItem(
                  selected = currentRoute == item.route,
                  onClick = { navigateTopLevel(item.route) },
                  modifier = Modifier.testTag(item.testTag),
                  icon = { Icon(item.icon, contentDescription = item.labelProvider()) },
                  label = { Text(item.labelProvider(), maxLines = 1) },
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(navController = navController, startDestination = NavRoutes.HOME) {
              composable(NavRoutes.HOME) {
                HomeDashboardScreen(
                  state = homeState,
                  onSelectTierLane = homePatchVm::selectTierLane,
                  onOpenAnalyzer = { champId ->
                    if (champId != null) {
                      analyzerVm.selectMyChampionById(champId)
                    }
                    navigateTopLevel(NavRoutes.ANALYZER)
                  },
                  onOpenChampionDetail = { champId ->
                    navController.navigate(NavRoutes.championDetail(champId))
                  },
                  onOpenDraft = { navigateTopLevel(NavRoutes.DRAFT) },
                  onOpenCoach = { navigateTopLevel(NavRoutes.COACH) },
                  onOpenPatchCenter = { navController.navigate(NavRoutes.PATCH_CENTER) },
                  onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                )
              }

              composable(NavRoutes.ANALYZER) {
                AnalyzerScreen(
                  state = analyzerState,
                  onSelectMyChampion = analyzerVm::selectMyChampion,
                  onSelectLane = analyzerVm::selectLane,
                  onToggleEnemy = analyzerVm::toggleEnemyChampion,
                  onApplyPresetEnemies = analyzerVm::applyQuickEnemyPreset,
                  onClearEnemies = analyzerVm::clearEnemies,
                  onSaveMatchup = analyzerVm::saveCurrentMatchup,
                  onLoadSavedMatchup = analyzerVm::loadSavedMatchup,
                  onDeleteSavedMatchup = analyzerVm::deleteSavedMatchup,
                  onAskCoachForMatchup = {
                    analyzerVm.syncSharedContext()
                    navigateTopLevel(NavRoutes.COACH)
                  },
                )
              }

              composable(NavRoutes.CHAMPIONS) {
                ChampionsScreen(
                  state = championsState,
                  onSearchChange = championsVm::setSearchQuery,
                  onSelectLane = championsVm::setSelectedLane,
                  onSelectRole = championsVm::setSelectedRole,
                  onToggleOnlyFavorites = championsVm::setOnlyFavorites,
                  onSortModeChange = championsVm::setSortMode,
                  onToggleFavorite = championsVm::toggleFavorite,
                  onOpenDetail = { champId ->
                    navController.navigate(NavRoutes.championDetail(champId))
                  },
                )
              }

              composable(
                route = NavRoutes.CHAMPION_DETAIL,
                arguments = listOf(navArgument("championId") { type = NavType.StringType }),
              ) { entry ->
                val champId = entry.arguments?.getString("championId")
                val champion =
                  championsState.allChampions.firstOrNull { it.id == champId }
                if (champion != null) {
                  ChampionDetailScreen(
                    champion = champion,
                    state = championsState,
                    onToggleFavorite = championsVm::toggleFavorite,
                    onAnalyze1v5 = { id ->
                      analyzerVm.selectMyChampionById(id)
                      navController.popBackStack()
                      navigateTopLevel(NavRoutes.ANALYZER)
                    },
                    onBack = { navController.popBackStack() },
                  )
                }
              }

              composable(NavRoutes.DRAFT) {
                DraftScreen(
                  state = draftState,
                  onToggleAlly = draftVm::addOrRemoveAlly,
                  onToggleEnemy = draftVm::addOrRemoveEnemy,
                  onSelectLane = draftVm::setIntendedLane,
                  onSelectRole = draftVm::setPreferredRole,
                  onClearDraft = draftVm::clearDraft,
                )
              }

              composable(NavRoutes.COACH) {
                CoachScreen(
                  state = coachState,
                  geminiState = geminiState,
                  messages = coachMessages,
                  onSelectChampion = coachVm::selectChampion,
                  onSelectLane = coachVm::selectLane,
                  onAskGemini = coachVm::askGeminiCoach,
                  onClearChat = coachVm::clearChat,
                )
              }

              composable(NavRoutes.PATCH_CENTER) {
                PatchMetaScreen(
                  homeState = homeState,
                  actionStatus = patchActionStatus,
                  validationResult = validationResult,
                  sampleSchemaJson = homePatchVm.getSampleSchemaJson(),
                  onImportJsonUri = homePatchVm::importPatchJsonUri,
                  onVerifyIntegrity = homePatchVm::verifyDatabaseIntegrity,
                  onResetSeed = homePatchVm::resetToBundledSeed,
                  onBack = {
                    homePatchVm.clearPatchStatus()
                    navController.popBackStack()
                  },
                )
              }

              composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                  preferences = prefs,
                  onSelectLanguage = homePatchVm::setLanguage,
                  onChangeFontScale = homePatchVm::setFontScale,
                  onToggleHighContrast = homePatchVm::setHighContrastCards,
                  onToggleMiniMode = homePatchVm::setInGameMiniMode,
                  onBack = { navController.popBackStack() },
                )
              }
            }
          }
        }
      }
    }
  }
}
