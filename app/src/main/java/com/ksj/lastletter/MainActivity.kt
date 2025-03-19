package com.ksj.lastletter

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.ksj.lastletter.keyfunction.InputTextScreen
import com.ksj.lastletter.keyfunction.RecordingScreen
import com.ksj.lastletter.keyfunction.YoursContextScreen
import com.ksj.lastletter.keyfunction.YoursMainScreen
import com.ksj.lastletter.login.LoginScreen
import com.ksj.lastletter.login.PhoneTermScreen
import com.ksj.lastletter.setting.SettingsScreen
import com.ksj.lastletter.setting.TextSizeOption
import com.ksj.lastletter.setting.TextSizeSettingScreen
import com.ksj.lastletter.setting.getTextSizeOption
import com.ksj.lastletter.ui.MyPageScreen
import com.ksj.lastletter.ui.theme.LastLetterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            var selectedTextSize by remember { mutableStateOf(TextSizeOption.MEDIUM) }

            LaunchedEffect(Unit) {
                val option = withContext(Dispatchers.IO) {
                    getTextSizeOption(context)
                }
                selectedTextSize = option
            }

            LastLetterTheme(textSizeOption = selectedTextSize) {
                val navController = rememberNavController()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute != "login" && currentRoute != "phoneTerm/{preFilledPhone}"

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController)
                        }
                    }
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
                                context = LocalContext.current
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
                            RecordingScreen(navController, contactName)
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                        composable("textSizeSetting") {
                            TextSizeSettingScreen(
                                navController = navController,
                                selectedTextSize = selectedTextSize,
                                onTextSizeChange = { newSize ->
                                    selectedTextSize = newSize
                                }
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
                        composable(
                            route = "phoneTerm/{preFilledPhone}",
                            arguments = listOf(
                                navArgument("preFilledPhone") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val preFilledPhone = backStackEntry.arguments?.getString("preFilledPhone") ?: ""
                            PhoneTermScreen(navController = navController, preFilledPhone = preFilledPhone)
                        }
                        composable("inputtextscreen/{recognizedText}/{customDateText}/{selectedEmotion}") { backStackEntry ->
                            val recognizedText = backStackEntry.arguments?.getString("recognizedText")?.let { Uri.decode(it) } ?: ""
                            val customDateText = backStackEntry.arguments?.getString("customDateText")?.let { Uri.decode(it) } ?: ""
                            val selectedEmotion = backStackEntry.arguments?.getString("selectedEmotion")?.let { Uri.decode(it) } ?: ""
                            InputTextScreen(navController, recognizedText, customDateText, selectedEmotion)
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
        containerColor = Color(0xFFFDFBF4),
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "dailyQuestion",
            onClick = { navController.navigate("dailyQuestion") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icon_write),
                    contentDescription = "홈",
                    tint = if (currentRoute == "dailyQuestion") Color(0xFF111111) else Color(0xFF797878)
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = currentRoute == "yoursMain",
            onClick = { navController.navigate("yoursMain") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icon_letter),
                    contentDescription = "편지쓰기",
                    tint = if (currentRoute == "yoursMain") Color(0xFF111111) else Color(0xFF797878)
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = currentRoute == "dailyQuestionList",
            onClick = { navController.navigate("dailyQuestionList") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icon_list),
                    contentDescription = "일일질문목록",
                    tint = if (currentRoute == "dailyQuestionList") Color(0xFF111111) else Color(0xFF797878)
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = currentRoute == "myPage",
            onClick = { navController.navigate("myPage") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.icon_cart),
                    contentDescription = "마이페이지",
                    tint = if (currentRoute == "myPage") Color(0xFF111111) else Color(0xFF797878)
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}
