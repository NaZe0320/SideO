package com.naze.side_o.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naze.side_o.ui.archive.ArchiveScreen
import com.naze.side_o.ui.home.HomeScreen
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
            Column(modifier = Modifier.fillMaxSize()) {
                HomeScreen()
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
            Column(modifier = Modifier.fillMaxSize()) {
                ArchiveScreen()
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
