// Screen.kt
package com.steadywj.wjfakelocation.manager.navigation

sealed class Screen(val route: String) {
    object Map : Screen("map")

    object Favorites : Screen("favorites")

    object Settings : Screen("settings")

    object About : Screen("about")

    object ApiKeySettings : Screen("api_key_settings")
}
