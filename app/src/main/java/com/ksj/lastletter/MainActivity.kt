package com.ksj.lastletter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.ksj.lastletter.ui.theme.LastLetterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LastLetterTheme {
                val navController = rememberNavController()
                // 시작 시 LoginScreen부터 시작
                NavGraph(navController = navController)
            }
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.Login.route, // 무조건 로그인 화면으로 시작
        modifier = modifier
    ) {
        // 로그인 화면
        composable(AuthScreen.Login.route) {
            LoginScreen(
                navController = navController,
                loginAction = {
                    // 로그인 성공 시 MainScreen으로 이동하며 LoginScreen 제거
                    navController.navigate(MainScreen.Home.route) {
                        popUpTo(AuthScreen.Login.route) { inclusive = true }
                    }
                },
                context = LocalContext.current
            )
        }
        // 메인 화면
        composable(MainScreen.Home.route) {
            MainScreen(navController = navController)
        }

    }
}


object AuthScreen {
    object Login {
        const val route = "login"
    }
    // 필요 시 다른 인증 관련 화면 추가 가능
}

object MainScreen {
    object Home {
        const val route = "home"
    }
    // 필요 시 다른 메인 관련 화면 추가 가능
}

object DetailScreen {
    object Detail {
        const val route = "detail"
    }
}
