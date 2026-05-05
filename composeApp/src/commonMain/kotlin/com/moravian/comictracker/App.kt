package com.moravian.comictracker

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.data.UserPreferencesRepository
import com.moravian.comictracker.ui.screens.BarcodeScanRoute
import com.moravian.comictracker.ui.screens.CollectionScreen
import com.moravian.comictracker.ui.screens.ComicDetailScreen
import com.moravian.comictracker.ui.screens.ComicWebViewScreen
import com.moravian.comictracker.ui.screens.HomeScreen
import com.moravian.comictracker.ui.screens.IssueDetailScreen
import com.moravian.comictracker.ui.screens.SearchScreen
import com.moravian.comictracker.ui.theme.Amber
import com.moravian.comictracker.ui.theme.AmberSubtle
import com.moravian.comictracker.ui.theme.ColorNavBar
import com.moravian.comictracker.ui.theme.ColorOnSurfaceMuted
import com.moravian.comictracker.ui.theme.ComicTrackerTheme
import com.moravian.comictracker.ui.viewmodels.CollectionViewModel
import com.moravian.comictracker.ui.viewmodels.HomeViewModel
import com.moravian.comictracker.ui.viewmodels.SearchViewModel
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.comicvine_label
import org.jetbrains.compose.resources.stringResource

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object MyCollection : Screen("my_collection", "My Collection")
    object Search : Screen("search", "Search")
}

private val hideBottomBarPrefixes = listOf("comic_detail", "issue_detail", "webview", "barcode_scan")

@Composable
fun App(database: ComicTrackerDatabase, prefsRepository: UserPreferencesRepository) {
    ComicTrackerTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val navItems = listOf(Screen.Home, Screen.MyCollection, Screen.Search)
        val showBottomBar = hideBottomBarPrefixes.none { currentDestination?.route?.startsWith(it) == true }

        val showFab = currentDestination?.route?.startsWith("barcode_scan") != true

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = { navController.navigate("barcode_scan") },
                        shape = CircleShape,
                        containerColor = Amber,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan barcode"
                        )
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = ColorNavBar,
                        contentColor = ColorOnSurfaceMuted,
                    ) {
                        navItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = when (screen) {
                                            Screen.Home -> Icons.Filled.Home
                                            Screen.MyCollection -> Icons.Filled.Star
                                            Screen.Search -> Icons.Filled.Search
                                        },
                                        contentDescription = screen.label
                                    )
                                },
                                label = { Text(screen.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Amber,
                                    selectedTextColor = Amber,
                                    indicatorColor = AmberSubtle,
                                    unselectedIconColor = ColorOnSurfaceMuted,
                                    unselectedTextColor = ColorOnSurfaceMuted,
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onVolumeClick = { navController.navigate("comic_detail/$it") },
                        onIssueClick = { navController.navigate("issue_detail/$it") },
                        viewModel = viewModel { HomeViewModel(prefsRepository) }
                    )
                }
                composable(Screen.MyCollection.route) {
                    CollectionScreen(
                        viewModel = viewModel(factory = CollectionViewModel.factory(database, prefsRepository)),
                        onSeriesClick = { navController.navigate("comic_detail/$it") }
                    )
                }
                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel { SearchViewModel(prefsRepository) }
                    SearchScreen(
                        viewModel = searchViewModel,
                        onComicClick = { seriesId ->
                            navController.navigate("comic_detail/$seriesId")
                        }
                    )
                }
                composable(
                    route = "comic_detail/{seriesId}",
                    arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val seriesId = backStackEntry.savedStateHandle.get<String>("seriesId")?.toIntOrNull() ?: return@composable
                    ComicDetailScreen(
                        seriesId = seriesId,
                        onBack = { navController.popBackStack() },
                        database = database,
                        prefsRepository = prefsRepository,
                        onIssueClick = { navController.navigate("issue_detail/$it") },
                        onViewOnComicVine = { navController.navigate("webview/series/$seriesId") }
                    )
                }
                composable(
                    route = "issue_detail/{issueId}",
                    arguments = listOf(navArgument("issueId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val issueId = backStackEntry.savedStateHandle.get<String>("issueId")?.toIntOrNull() ?: return@composable
                    IssueDetailScreen(
                        issueId = issueId,
                        onBack = { navController.popBackStack() },
                        database = database,
                        onViewOnComicVine = { navController.navigate("webview/issue/$issueId") }
                    )
                }
                composable("barcode_scan") {
                    BarcodeScanRoute(
                        onIssueFound = { issueId ->
                            navController.popBackStack()
                            navController.navigate("issue_detail/$issueId")
                        },
                        onDismiss = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "webview/{type}/{id}",
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val type = backStackEntry.savedStateHandle.get<String>("type") ?: return@composable
                    val id = backStackEntry.savedStateHandle.get<String>("id") ?: return@composable
                    val url = when (type) {
                        "series" -> "https://comicvine.gamespot.com/volume/4050-$id/"
                        "issue" -> "https://comicvine.gamespot.com/issue/4000-$id/"
                        else -> return@composable
                    }
                    ComicWebViewScreen(
                        url = url,
                        title = stringResource(Res.string.comicvine_label),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
