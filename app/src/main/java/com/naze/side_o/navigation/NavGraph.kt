package com.naze.side_o.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naze.side_o.TodoApplication
import com.naze.side_o.ui.archive.ArchiveScreen
import com.naze.side_o.ui.archive.ArchiveViewModel
import com.naze.side_o.ui.archive.ArchiveViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.naze.side_o.ui.home.HomeScreen
import com.naze.side_o.ui.home.HomeViewModel
import com.naze.side_o.ui.home.HomeViewModelFactory
import com.naze.side_o.ui.settings.SettingsScreen
import com.naze.side_o.ui.settings.SettingsViewModel
import com.naze.side_o.ui.settings.SettingsViewModelFactory
import com.naze.side_o.ui.trash.TrashScreen
import com.naze.side_o.ui.trash.TrashViewModel
import com.naze.side_o.ui.trash.TrashViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
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
            val swipeReversed by app.settingsRepository.swipeReversed.collectAsState(initial = false)
            HomeScreen(
                viewModel = viewModel,
                swipeReversed = swipeReversed,
                onNavigateToArchive = { navController.navigate(Screen.Archive.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(Screen.Trash.route) {
            val context = LocalContext.current
            val app = context.applicationContext as TodoApplication
            val repository = app.repository
            val viewModel: TrashViewModel = viewModel(
                factory = TrashViewModelFactory(repository, app)
            )
            TrashScreen(
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
