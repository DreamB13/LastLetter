package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ksj.lastletter.login.LoginScreen
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Firebase나 KakaoSdk 초기화는 보통 Application 클래스에서 진행
        setContent {
            LastLetterTheme {
                val navController = rememberNavController()
                // 로그인 화면을 시작 화면으로 설정하고,
                // 로그인 성공 시 "yoursMain" 화면으로 이동하게 구성
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            navController = navController,
                            loginAction = {},
                            context = LocalContext.current
                        )
                    }
                    composable("yoursMain") {
                        YoursMainScreen(navController = navController)
                    }
                    composable(
                        route = "yoursContext/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                        YoursContextScreen(contactId = contactId, navController = navController)
                    }
                    composable(
                        route = "recording/{contactId}/{contactName}",
                        arguments = listOf(
                            navArgument("contactId") { type = NavType.StringType },
                            navArgument("contactName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                        RecordingScreen(navController = navController, contactName = contactName)
                    }
                    composable("dayquestion") {
                        DayquestionScreen(navController = navController)
                    }
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }
}

