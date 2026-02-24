package com.naze.do_swipe.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Archive : Screen("archive")
    data object Settings : Screen("settings")
}
