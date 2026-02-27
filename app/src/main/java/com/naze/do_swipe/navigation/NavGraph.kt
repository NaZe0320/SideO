package com.naze.do_swipe.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naze.do_swipe.TodoApplication
import com.naze.do_swipe.ui.archive.ArchiveScreen
import com.naze.do_swipe.ui.archive.ArchiveViewModel
import com.naze.do_swipe.ui.archive.ArchiveViewModelFactory
import com.naze.do_swipe.ui.home.HomeScreen
import com.naze.do_swipe.ui.home.HomeViewModel
import com.naze.do_swipe.ui.home.HomeViewModelFactory
import com.naze.do_swipe.ui.settings.SettingsScreen
import com.naze.do_swipe.ui.settings.SettingsViewModel
import com.naze.do_swipe.ui.settings.SettingsViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    openAddFromWidget: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            val context = LocalContext.current
            val app = context.applicationContext as TodoApplication
            val repository = app.repository
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(repository, app)
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                openAddOnStart = openAddFromWidget
            )
        }
        composable(Screen.Archive.route) {
            val context = LocalContext.current
            val app = context.applicationContext as TodoApplication
            val repository = app.repository
            val viewModel: ArchiveViewModel = viewModel(
                factory = ArchiveViewModelFactory(repository, app)
            )
            ArchiveScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(Screen.Settings.route) {
            val context = LocalContext.current
            val app = context.applicationContext as TodoApplication
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(app)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
