package com.ksj.lastletter.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ksj.lastletter.R

@Composable
fun LoginScreen(
    navController: NavController,
    loginAction: () -> Unit,
    context: Context
) {
    val localContext = LocalContext.current
    // FirebaseAuth 및 기존 로그인 로직은 그대로 유지(이미 로그인되어 있다면 이동)
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("dailyQuestion") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 영역 (로고, 타이틀)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                // 로고 이미지
                 Image(
                     painter = painterResource(id = R.drawable.lastletter),
                     contentDescription = null,
                     modifier = Modifier.size(300.dp)
                 )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Last Letter",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // 중앙 영역: 각 로그인 버튼
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 구글 로그인 버튼 → PhoneTermScreen으로 이동
                Button(
                    onClick = { navController.navigate("phoneTerm") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(text = "Google로 시작하기", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 카카오 로그인 버튼 (더미) → PhoneTermScreen으로 이동
                Button(
                    onClick = { navController.navigate("phoneTerm") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE812))
                ) {
                    Text(text = "카카오로 시작하기", color = Color.Black)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 휴대폰 번호로 시작하기 버튼 → PhoneTermScreen으로 이동
                Button(
                    onClick = { navController.navigate("phoneTerm") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("휴대폰 번호로 시작하기")
                }
            }
            // 하단 영역: 고객센터/문의
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "고객센터/문의",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
