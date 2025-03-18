package com.ksj.lastletter.dailyquestion

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionScreen(navController: NavController) {
    val db: FirebaseFirestore = Firebase.firestore
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // 오늘 날짜의 dayOfYear를 이용해 문서 ID를 정합니다 (예: "DailyQuestion_74")
    val today = LocalDate.now().dayOfYear
    val docId = "DailyQuestion_$today"

    // 오늘의 질문 가져오기
    val dailyQuestions = DailyQuestion.values()
    val dailyQuestionIndex = today % dailyQuestions.size
    val questionText = dailyQuestions[dailyQuestionIndex].question

    // 입력 상태/보기 상태를 관리하는 변수
    var isEditing by remember { mutableStateOf(true) }

    // 사용자가 입력한 답변
    val answer = remember { mutableStateOf("") }
    val maxLength = 300

    // 화면이 처음 표시될 때, Firestore에서 오늘 문서가 있는지 확인
    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection("users")
                .document(uid)
                .collection("DailyQuestion")
                .document(docId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // 문서가 있으면 → 보기 모드
                        val savedAnswer = document.getString("answer") ?: ""
                        answer.value = savedAnswer
                        isEditing = false
                    } else {
                        // 문서가 없으면 → 입력 모드
                        isEditing = true
                    }
                }
                .addOnFailureListener {
                    // 실패 시에도 입력 모드로 시작
                    isEditing = true
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFFDFBF4)
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Last Letter", color = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { /* 알림 아이콘 클릭 로직 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "알림 아이콘",
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFDFBF4))
        ) {
            // 상단 "NEW"와 "오늘의 질문" 표시
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFC107), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "오늘의 질문",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))
            // 실제 질문 텍스트
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = questionText,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 답변 영역(텍스트 필드 or 답변 텍스트)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFFDFBF4), shape = RoundedCornerShape(8.dp))
            ) {
                if (isEditing) {
                    // ----- 입력 모드 -----
                    OutlinedTextField(
                        value = answer.value,
                        onValueChange = {
                            if (it.length <= maxLength) {
                                answer.value = it
                            } else {
                                Toast
                                    .makeText(context, "최대 $maxLength 자까지 입력 가능합니다.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        placeholder = { Text("답변을 적어주세요") },
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 5,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFFFE8F2E),
                            unfocusedBorderColor = Color(0xFFFE8F2E)
                        )
                    )
                    Text(
                        text = "${answer.value.length}/$maxLength 자",
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.BottomEnd)
                    )
                } else {
                    // ----- 보기 모드 -----
//                    Text(
//                        text = answer.value,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color.White, shape = RoundedCornerShape(8.dp))
//                            .padding(16.dp)
//                            .align(Alignment.TopStart),
//                        color = Color.Black
//                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RectangleShape)
                            .background(Color(0xFFE9F4F7))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = answer.value,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 저장하기 / 광고 시청 후 수정하기 버튼
            if (isEditing) {
                // ----- 입력 모드 -----
                Button(
                    onClick = {
                        if (uid != null) {
                            val dailyQuestionData = hashMapOf(
                                "question" to questionText,
                                "answer" to answer.value,
                                "timestamp" to System.currentTimeMillis()
                            )
                            // 날짜 문서(docId)에 저장 (예: "DailyQuestion_74")
                            db.collection("users")
                                .document(uid)
                                .collection("DailyQuestion")
                                .document(docId)
                                .set(dailyQuestionData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    // 저장 후 보기 모드로 전환
                                    isEditing = false
                                }
                                .addOnFailureListener { exception ->
                                    println("Error adding document: ${exception.message}")
                                }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("저장하기")
                }
            } else {
                // ----- 보기 모드 -----
                Button(
                    onClick = {
                        // 광고 시청 로직(예: Rewarded Ad) 후 콜백에서 아래 로직 실행
                        // 예시로 바로 isEditing = true로 전환
                        isEditing = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("광고 시청 후 수정하기")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "편지 아이콘",
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "편지를 써보는건 어때요?", color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyQuestionScreenPreview() {
    DailyQuestionScreen(navController = NavController(LocalContext.current))
}