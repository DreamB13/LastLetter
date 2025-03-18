package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.ksj.lastletter.dailyquestion.DailyQuestionDetail
import com.ksj.lastletter.dailyquestion.DailyQuestionListScreen
import com.ksj.lastletter.dailyquestion.DailyQuestionScreen
import com.ksj.lastletter.login.LoginScreen
import com.ksj.lastletter.setting.SettingsScreen
import com.ksj.lastletter.setting.TextSizeOption
import com.ksj.lastletter.setting.TextSizeSettingScreen
import com.ksj.lastletter.setting.getTextSizeOption
import com.ksj.lastletter.ui.MyPageScreen
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
                            route = "yoursContext/{contactId}",
                            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val contactId = backStackEntry.arguments?.getString("contactId") ?: ""
                            YoursContextScreen(
                                navController = navController,
                                contactId = contactId
                            )
                        }
                        composable(
                            route = "recording/{contactId}/{contactName}",
                            arguments = listOf(
                                navArgument("contactId") { type = NavType.StringType },
                                navArgument("contactName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
                            RecordingScreen(
                                navController = navController,
                                contactName = contactName
                            )
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
                        composable("dailyQuestionList") {
                            DailyQuestionListScreen(navController = navController)
                        }
                        composable(
                            route = "dailyQuestionDetail/{docId}",
                            arguments = listOf(navArgument("docId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("docId") ?: ""
                            DailyQuestionDetail(navController = navController, docId = docId)
                        }
                        composable("myPage") {
                            MyPageScreen(navController = navController)
                        }
                        composable("inputtextscreen") {
                            InputTextScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
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
            selected = currentRoute == "dailyQuestionList",
            onClick = { navController.navigate("dailyQuestionList") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountBox,
                    contentDescription = "일일질문목록"
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "myPage",
            onClick = { navController.navigate("myPage") },
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
