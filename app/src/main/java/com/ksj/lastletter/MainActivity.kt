package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LastLetterTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "yoursMain") {
                    composable("yoursMain") {
                        YoursMainScreen(navController = navController)
                    }
                    composable(
                        route = "yoursContext/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                        YoursContextScreen(contactId = contactId)
                    }
                    // 다른 화면 추가 가능
                }
            }
        }
    }
}
