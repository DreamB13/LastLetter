package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ksj.lastletter.login.LoginScreen
import com.ksj.lastletter.setting.SettingsScreen
import com.ksj.lastletter.setting.TextSizeOption
import com.ksj.lastletter.setting.TextSizeSettingScreen
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 전역 텍스트 크기 상태 (필요에 따라 ViewModel로 관리 가능)
            var selectedTextSize by remember { mutableStateOf(TextSizeOption.MEDIUM) }

            // 선택된 텍스트 크기에 따라 테마 적용
            LastLetterTheme(textSizeOption = selectedTextSize) {
                val navController = rememberNavController()
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
                    composable("dailyQuestion") {
                        DailyQuestionScreen(navController = navController)
                    }
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                    composable("textSizeSetting") {
                        // 텍스트 크기 설정 화면: 선택 값과 변경 콜백 전달
                        TextSizeSettingScreen(
                            navController = navController,
                            selectedTextSize = selectedTextSize,
                            onTextSizeChange = { newSize -> selectedTextSize = newSize }
                        )
                    }
                }
            }
        }
    }
}
