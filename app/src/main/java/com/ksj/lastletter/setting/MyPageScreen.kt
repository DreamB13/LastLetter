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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ksj.lastletter.AppViewModel
import com.ksj.lastletter.firebase.DocumentContact
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(navController: NavController) {
    // AppViewModel 불러오기
    val appViewModel: AppViewModel = viewModel()
    val contacts: List<DocumentContact> = appViewModel.contacts.value

    // 출석체크 로직 관련 상태
    // 1) 현재까지 출석한 횟수(0부터 시작)
    var currentAttendance by remember { mutableStateOf(0) }
    val totalAttendance = 10

    // 2) 출석 완료 여부 (버튼을 비활성화할 때 사용)
    var isAttendanceComplete by remember { mutableStateOf(false) }

    // 3) 마지막으로 출석한 날짜
    var lastAttendanceDate by remember { mutableStateOf<LocalDate?>(null) }

    // 화면이 다시 그려질 때마다, “오늘 날짜가 바뀌었고 오전 8시 이후라면” 출석을 초기화
    val now = LocalDateTime.now()
    LaunchedEffect(key1 = now) {
        // 날짜가 바뀌었고, 현재 시간이 오전 8시 이상이라면
        if (lastAttendanceDate != now.toLocalDate() && now.hour >= 8) {
            isAttendanceComplete = false
            lastAttendanceDate = now.toLocalDate()
        }
    }

    // 배경색
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
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "알림",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = backgroundColor)
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
                // 출석 체크 영역
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
                    // 현재 출석 / 총 출석
                    Text(
                        text = "$currentAttendance/$totalAttendance",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 출석 진행도
                    val progress = currentAttendance.toFloat() / totalAttendance
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = Color(0xFF86CE86),
                        trackColor = Color(0xFFECECEC)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 출석 버튼
                    Button(
                        onClick = {
                            if (!isAttendanceComplete && currentAttendance < totalAttendance) {
                                currentAttendance++
                                isAttendanceComplete = true
                                lastAttendanceDate = now.toLocalDate()
                            }
                        },
                        enabled = !isAttendanceComplete
                    ) {
                        Text(
                            text = if (isAttendanceComplete) "출석완료" else "출석하기"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 기존 상품 영역 UI
                ItemRow(
                    title = "편지 보낼 사람 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ItemRow(
                    title = "편지 10장 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ItemRow(
                    title = "골드 회원",
                    price = "월 1,900원"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 메뉴 아이템들
                MenuItem("자주 묻는 질문") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("문의하기") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("질문 제안하기") { /* 클릭 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                MenuItem("버전 정보") { /* 클릭 처리 */ }

                // 캐시된 연락처 목록 예시
                if (contacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "연락처 목록", fontSize = 18.sp, color = Color.Black)
                    contacts.forEach { documentContact ->
                        InfoCard(
                            text = documentContact.contact.name,
                            modifier = Modifier.clickable {
                                navController.navigate("yoursContext/${documentContact.id}")
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(title: String, price: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 16.sp, color = Color.Black)
        Text(text = price, fontSize = 16.sp, color = Color.Gray)
    }
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFFFFF5E9), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFFFF4E6), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}
