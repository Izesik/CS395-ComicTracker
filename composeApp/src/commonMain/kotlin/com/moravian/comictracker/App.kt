package com.moravian.comictracker

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.moravian.comictracker.ui.screens.CollectionScreen
import com.moravian.comictracker.data.ComicTrackerDatabase
import com.moravian.comictracker.ui.screens.ComicDetailScreen
import com.moravian.comictracker.ui.screens.HomeScreen
import com.moravian.comictracker.ui.screens.IssueDetailScreen
import com.moravian.comictracker.ui.viewmodels.HomeViewModel

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object MyCollection : Screen("my_collection", "My Collection")
    object Search : Screen("search", "Search")
}

private val detailRoutes = listOf("comic_detail", "issue_detail")

@Composable
fun App(comicTrackerDatabase: ComicTrackerDatabase) {
    MaterialTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val navItems = listOf(Screen.Home, Screen.MyCollection, Screen.Search)
        val showBottomBar = detailRoutes.none { currentDestination?.route?.startsWith(it) == true }

        Scaffold(
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
                composable(Screen.MyCollection.route) { CollectionScreen() }
                composable(Screen.Search.route) { /* TODO */ }
                composable(
                    route = "comic_detail/{volumeId}",
                    arguments = listOf(navArgument("volumeId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val volumeId = backStackEntry.savedStateHandle.get<String>("volumeId")?.toIntOrNull() ?: return@composable
                    ComicDetailScreen(
                        volumeId = volumeId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "issue_detail/{issueId}",
                    arguments = listOf(navArgument("issueId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val issueId = backStackEntry.savedStateHandle.get<String>("issueId")?.toIntOrNull() ?: return@composable
                    IssueDetailScreen(
                        issueId = issueId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
