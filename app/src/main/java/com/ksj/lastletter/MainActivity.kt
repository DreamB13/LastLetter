package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ksj.lastletter.login.LoginScreen
import com.ksj.lastletter.setting.SettingsScreen
import com.ksj.lastletter.setting.TextSizeOption
import com.ksj.lastletter.setting.TextSizeSettingScreen
import com.ksj.lastletter.setting.getTextSizeOption
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            var selectedTextSize by remember { mutableStateOf(TextSizeOption.MEDIUM) }

            LaunchedEffect(Unit) {
                selectedTextSize = getTextSizeOption(context)
            }
            LastLetterTheme(textSizeOption = selectedTextSize) {
                val navController = rememberNavController()
                // 전역 Scaffold에 하단 네비게이션 바 추가
                Scaffold(
                    bottomBar = { BottomNavigationBar(navController = navController) }
                ) { innerPadding ->
                    NavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                loginAction = {},
                                context = androidx.compose.ui.platform.LocalContext.current
                            )
                        }
                        composable("dailyQuestion") {
                            DailyQuestionScreen(navController = navController)
                        }
                        composable("yoursMain") {
                            YoursMainScreen(navController = navController)
                        }
                        composable(
                            route = "yoursContext/ {contactId}",
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
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                        composable("textSizeSetting") {
                            TextSizeSettingScreen(
                                navController = navController,
                                selectedTextSize = selectedTextSize,
                                onTextSizeChange = { newSize -> selectedTextSize = newSize }
                            )
                        }
                        // 더미 화면: 마이페이지와 우리의 돈줄 (추후 실제 화면으로 대체 가능)
                        composable("profile") {
                            DummyScreen(title = "마이페이지")
                        }
                        composable("shopping") {
                            DummyScreen(title = "우리의 돈줄")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    // 현재 선택된 경로 확인
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "dailyQuestion",
            onClick = { navController.navigate("dailyQuestion") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "홈"
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "yoursMain",
            onClick = { navController.navigate("yoursMain") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "편지쓰기"
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountBox,
                    contentDescription = "마이페이지"
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "shopping",
            onClick = { navController.navigate("shopping") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "우리의 돈줄"
                )
            }
        )
    }
}

@Composable
fun DummyScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = title)
        }
    }
}
