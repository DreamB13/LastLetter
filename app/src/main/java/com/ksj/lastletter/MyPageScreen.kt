package com.ksj.lastletter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(navController: NavController) {
    // 출석 관련 상태
    var currentAttendance by remember { mutableStateOf(4) }
    val totalAttendance = 10

    // 전체 배경색(이미지의 연한 베이지/크림 계열)
    val backgroundColor = Color(0xFFFFF5E9)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "나의 마지막 편지", color = Color.Black)
                },
                actions = {
                    IconButton(onClick = { /* 알림 아이콘 클릭 처리 */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "알림",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = { /* 설정 아이콘 클릭 처리 */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // 1) 출석 체크 영역
                Text(
                    text = "출석 체크",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 진행 상황 (예: 4/10)
                    Text(
                        text = "${currentAttendance}/${totalAttendance}",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 진행도 표시 (LinearProgressIndicator)
                    val progress = currentAttendance.toFloat() / totalAttendance
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = Color(0xFF86CE86), // 원하는 진행 색상
                        trackColor = Color(0xFFECECEC) // 남은 영역 색상
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // 출석하기 로직 (최대값 초과하지 않도록 예시 처리)
                            if (currentAttendance < totalAttendance) {
                                currentAttendance++
                            }
                        }
                    ) {
                        Text("출석하기")
                    }
                }

                // 구분선
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 2) 상품(?) 영역: 편지 보낼 사람 추가 / 편지 10장 추가 / 골드 회원
                // 첫 번째 아이템
                ItemRow(
                    title = "편지 보낼 사람 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 두 번째 아이템
                ItemRow(
                    title = "편지 10장 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 세 번째 아이템
                ItemRow(
                    title = "골드 회원",
                    price = "월 1,900원"
                )

                // 구분선
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 3) 하단 메뉴: 자주 묻는 질문 / 문의하기 / 질문 제안하기 / 버전 정보
                MenuItem("자주 묻는 질문") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("문의하기") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("질문 제안하기") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("버전 정보") { /* 클릭 처리 */ }
            }
        }
    }
}

@Composable
fun ItemRow(title: String, price: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Black
        )
        Text(
            text = price,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}
