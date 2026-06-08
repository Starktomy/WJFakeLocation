// AppNavGraph.kt
package com.steadywj.wjfakelocation.manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.steadywj.wjfakelocation.manager.about.AboutScreen
import com.steadywj.wjfakelocation.manager.favorites.FavoritesScreen
import com.steadywj.wjfakelocation.manager.map.MapScreen
import com.steadywj.wjfakelocation.manager.settings.ApiKeySettingsScreen
import com.steadywj.wjfakelocation.manager.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToApiKeySettings = {
                    navController.navigate(Screen.ApiKeySettings.route)
                },
            )
        }

        composable(Screen.ApiKeySettings.route) {
            ApiKeySettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
