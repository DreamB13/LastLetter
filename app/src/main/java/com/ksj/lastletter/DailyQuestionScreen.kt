package com.ksj.lastletter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionScreen(navController: NavController) {
    // 답변 입력 상태
    val answer = remember { mutableStateOf("") }
    val maxLength = 300

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Last Letter",
                        color = Color.Black
                    )
                },
                actions = {
                    // 우측 알림(벨) 아이콘
                    IconButton(onClick = { /* TODO: 알림 아이콘 클릭 로직 */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "알림 아이콘",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate("settings") // 설정 화면으로 이동
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = Color.Black
                        )
                    }

                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* TODO: 네비게이션 로직 */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "홈"
                        )
                    },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* TODO: 네비게이션 로직 */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "편지쓰기"
                        )
                    },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* TODO: 네비게이션 로직 */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "마이페이지"
                        )
                    },

                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* TODO: 네비게이션 로직 */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "우리의 돈줄"
                        )
                    },
                )
            }
        }
    ) { innerPadding ->
        // Scaffold 내에서 실제 화면을 구성하는 영역
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // "NEW" + "오늘의 질문" 부분
            Row(verticalAlignment = Alignment.CenterVertically,) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFC107), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart),
                        text = "NEW",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "오늘의 질문",
                    style = MaterialTheme.typography.titleMedium, // 필요에 따라 수정
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 실제 질문 문구
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "내가 살면서 가장 행복했던 순간들은?",
                style = MaterialTheme.typography.titleSmall, // 필요에 따라 수정
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 답변 작성 영역
            OutlinedTextField(
                value = answer.value,
                onValueChange = {
                    if (it.length <= maxLength) {
                        answer.value = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("답변을 적어주세요") },
                maxLines = 5
            )

            // 글자 수 표시
            Text(
                text = "${answer.value.length}/$maxLength 자",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )

            // 저장하기 버튼
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {

                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("저장하기")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "편지를 써보는건 어때요?" 섹션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "편지 아이콘",
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "편지를 써보는건 어때요?",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun DayquestionScreenPreview() {
    DailyQuestionScreen(navController = NavController(context = LocalContext.current))
}