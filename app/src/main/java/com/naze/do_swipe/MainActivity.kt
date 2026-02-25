package com.naze.do_swipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.naze.do_swipe.navigation.NavGraph
import com.naze.do_swipe.ui.theme.DoSwipeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TodoApplication
        setContent {
            val themeMode by app.settingsRepository.themeMode.collectAsState(
                initial = com.naze.do_swipe.data.preferences.ThemeMode.SYSTEM
            )
            DoSwipeTheme(themeMode = themeMode) {
                NavGraph()
            }
        }
    }
}