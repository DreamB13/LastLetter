package com.ksj.lastletter.ui

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ksj.lastletter.AppViewModel
import com.ksj.lastletter.firebase.DocumentContact
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(navController: NavController) {
    // AppViewModel 불러오기 (연락처 목록 등 사용)
    val appViewModel: AppViewModel = viewModel()
    val contacts: List<DocumentContact> = appViewModel.contacts.value

    val context = LocalContext.current

    // ─────────────────────────────────────────────────────────────────────
    // 출석체크 관련 상태: rememberSaveable로 화면 이동 후에도 유지
    // ─────────────────────────────────────────────────────────────────────
    var currentAttendance by rememberSaveable { mutableIntStateOf(0) }       // 누적 출석 횟수
    var isAttendanceComplete by rememberSaveable { mutableStateOf(false) } // 오늘 출석 완료 여부
    var lastAttendanceDate by rememberSaveable { mutableStateOf<LocalDate?>(null) } // 마지막 출석 날짜

    val totalAttendance = 10

    // 현재 시각
    val now = LocalDateTime.now()

    // 오전 8시 이후 날짜가 바뀌면 출석 초기화
    LaunchedEffect(key1 = now) {
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
                // ─────────────────────────────────────────────────────
                // 출석 체크 영역
                // ─────────────────────────────────────────────────────
                Text(
                    text = "출석 체크",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 현재 출석 / 총 출석
                    Text(
                        text = "$currentAttendance/$totalAttendance",
                        color = Color.Gray,
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 진행도 (Progress)
                    val progress = currentAttendance.toFloat() / totalAttendance
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp),
                        color = Color(0xFF86CE86), // 진행 바 색
                        trackColor = Color(0xFFECECEC)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // 출석하기 버튼
                    Button(
                        onClick = {
                            if (!isAttendanceComplete && currentAttendance < totalAttendance) {
                                currentAttendance++

                                // 만약 출석 횟수가 10번이 되었다면 보상(편지지 한장) 지급
                                if (currentAttendance == totalAttendance) {
                                    // 0으로 초기화
                                    currentAttendance = 0
                                    Toast.makeText(context, "편지지 한장을 받았습니다.", Toast.LENGTH_SHORT).show()
                                }

                                isAttendanceComplete = true
                                lastAttendanceDate = now.toLocalDate()
                            }
                        },
                        enabled = !isAttendanceComplete
                    ) {
                        Text(text = if (isAttendanceComplete) "출석완료" else "출석하기")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ─────────────────────────────────────────────────────
                // 상품 영역
                // ─────────────────────────────────────────────────────
                ProductItem(
                    icon = Icons.Filled.Email,  // 임의 아이콘
                    title = "편지 보낼 사람 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductItem(
                    icon = Icons.Filled.Edit,   // 임의 아이콘
                    title = "편지 10장 추가",
                    price = "4,900원"
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductItem(
                    icon = Icons.Filled.Star,   // 임의 아이콘
                    title = "골드 회원",
                    price = "월 1,900원"
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // ─────────────────────────────────────────────────────
                // 메뉴 항목 (스크롤 가능하도록 구성)
                // ─────────────────────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    MenuItem(
                        icon = Icons.Filled.Info,
                        text = "자주 묻는 질문"
                    ) {
                        /* 클릭 처리 */
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MenuItem(
                        icon = Icons.Filled.Call,
                        text = "문의하기"
                    ) {
                        /* 클릭 처리 */
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MenuItem(
                        icon = Icons.Filled.Edit,
                        text = "질문 제안하기"
                    ) {
                        /* 클릭 처리 */
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MenuItem(
                        icon = Icons.Filled.Info,
                        text = "버전 정보"
                    ) {
                        /* 클릭 처리 */
                    }

                    // ─────────────────────────────────────────────────────
                    // 캐시된 연락처 목록 (InfoCard)
                    // ─────────────────────────────────────────────────────
                    if (contacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "연락처 목록", fontSize = 18.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        contacts.forEach { documentContact ->
                            InfoCard(
                                text = documentContact.contact.name,
                                modifier = Modifier.clickable {
                                    // 예: 편지 상세 화면으로 이동
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
}

// ──────────────────────────────────────────────────────────────────────────
// 상품 아이템 (아이콘 + 제목 + 가격)
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun ProductItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    price: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF595959))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 16.sp, color = Color.Black)
        }
        Text(text = price, fontSize = 16.sp, color = Color.Gray)
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 메뉴 항목 (아이콘 + 텍스트) - 배경색, 둥근 모서리
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF5E9), shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF595959))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

// ──────────────────────────────────────────────────────────────────────────
// InfoCard: 연락처 목록 등에 사용 (박스 형태)
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFFFFF4E6), shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}
