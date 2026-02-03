package com.naze.side_o

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.naze.side_o.navigation.NavGraph
import com.naze.side_o.ui.theme.SideOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideOTheme {
                NavGraph()
            }
        }
    }
}