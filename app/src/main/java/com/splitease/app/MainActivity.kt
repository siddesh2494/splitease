package com.splitease.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.splitease.app.ui.navigation.AppNavHost
import com.splitease.app.ui.theme.SplitEaseTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge is mandatory when targeting SDK 35+; enableEdgeToEdge() also gives
        // transparent system bars with correct icon contrast on all API levels.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val joinCode = intent?.data?.let { it.lastPathSegment }   // deep link code, if any
        setContent {
            SplitEaseTheme { AppNavHost(initialJoinCode = joinCode) }
        }
    }
}
