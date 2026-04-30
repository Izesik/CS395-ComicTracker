package com.moravian.comictracker

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.moravian.comictracker.ui.viewmodels.CollectionViewModel
import com.moravian.comictracker.ui.viewmodels.HomeViewModel
import com.moravian.comictracker.ui.viewmodels.SearchViewModel

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object MyCollection : Screen("my_collection", "My Collection")
    object Search : Screen("search", "Search")
}

private val hideBottomBarPrefixes = listOf("comic_detail", "issue_detail", "webview", "barcode_scan")

@Composable
fun App(database: ComicTrackerDatabase, prefsRepository: UserPreferencesRepository) {
    MaterialTheme {
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    NavigationBar {
                        navItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
                                label = { Text(screen.label) }
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
                        viewModel = viewModel { HomeViewModel() }
                    )
                }
                composable(Screen.MyCollection.route) {
                    CollectionScreen(
                        viewModel = viewModel(factory = CollectionViewModel.factory(database, prefsRepository))
                    )
                }
                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel { SearchViewModel() }
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
                        onIssueClick = { navController.navigate("issue_detail/$it") },
                        onViewOnMetron = { navController.navigate("webview/series/$seriesId") }
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
                        onViewOnMetron = { navController.navigate("webview/issue/$issueId") }
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
                    val type = backStackEntry.arguments?.getString("type") ?: return@composable
                    val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
                    val url = when (type) {
                        "series" -> "https://metron.cloud/series/$id/"
                        "issue" -> "https://metron.cloud/issue/$id/"
                        else -> return@composable
                    }
                    ComicWebViewScreen(
                        url = url,
                        title = "Metron",
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
