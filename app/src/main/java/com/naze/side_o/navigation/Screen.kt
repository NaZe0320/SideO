package com.naze.side_o.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Archive : Screen("archive")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
}
