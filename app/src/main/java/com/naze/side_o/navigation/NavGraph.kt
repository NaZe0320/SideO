package com.naze.side_o.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naze.side_o.TodoApplication
import com.naze.side_o.ui.archive.ArchiveScreen
import com.naze.side_o.ui.archive.ArchiveViewModel
import com.naze.side_o.ui.archive.ArchiveViewModelFactory
import com.naze.side_o.ui.home.HomeScreen
import com.naze.side_o.ui.home.HomeViewModel
import com.naze.side_o.ui.home.HomeViewModelFactory
import com.naze.side_o.ui.settings.SettingsScreen

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
            val repository = (context.applicationContext as TodoApplication).repository
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(repository)
            )
            Column(modifier = Modifier.fillMaxSize()) {
                HomeScreen(viewModel = viewModel)
                Column(Modifier.padding(16.dp)) {
                    Button(onClick = { navController.navigate(Screen.Archive.route) }) {
                        Text("아카이브로")
                    }
                    Button(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Text("설정으로")
                    }
                }
            }
        }
        composable(Screen.Archive.route) {
            val context = LocalContext.current
            val repository = (context.applicationContext as TodoApplication).repository
            val viewModel: ArchiveViewModel = viewModel(
                factory = ArchiveViewModelFactory(repository)
            )
            Column(modifier = Modifier.fillMaxSize()) {
                ArchiveScreen(viewModel = viewModel)
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("뒤로")
                }
            }
        }
        composable(Screen.Settings.route) {
            Column(modifier = Modifier.fillMaxSize()) {
                SettingsScreen()
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("뒤로")
                }
            }
        }
    }
}
