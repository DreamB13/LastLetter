package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LastLetterTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "yoursMain") {
                    composable("yoursMain") { YoursMainScreen() }
                    // 다른 화면 추가 가능
                }
            }
        }
    }
}
