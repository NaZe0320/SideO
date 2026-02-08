package com.naze.side_o

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.naze.side_o.navigation.NavGraph
import com.naze.side_o.ui.theme.SideOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TodoApplication
        setContent {
            val themeMode by app.settingsRepository.themeMode.collectAsState(
                initial = com.naze.side_o.data.preferences.ThemeMode.SYSTEM
            )
            SideOTheme(themeMode = themeMode) {
                NavGraph()
            }
        }
    }
}