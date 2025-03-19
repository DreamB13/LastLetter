package com.ksj.lastletter.dailyquestion

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ksj.lastletter.AppViewModel
import com.ksj.lastletter.FastAPI.EmotionRequest
import com.ksj.lastletter.FastAPI.RetrofitInstance2
import com.ksj.lastletter.R
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch
import java.time.LocalDate

// 감정에 따른 배경색 매핑 함수 (필요에 따라 색상을 수정하세요)
fun getColorForEmotion(emotion: String): Color {
    return when (emotion) {
        "😊" -> Color(0xFFFFF5E9)  // 기쁨: 연노랑
        "😲" -> Color(0xFFE9FFE9)  // 놀라움: 연파랑
        "❤️" -> Color(0xFFFFE9E9)  // 사랑: 연빨강
        "😡" -> Color(0xFFFFE9FE)  // 분노: 빨강
        "😢" -> Color(0xFFE9EDFF)  // 슬픔: 연파랑(조금 어둡게)
        "😐" -> Color(0xFFF7FFC8)  // 중립: 회색
        else -> Color(0xFFE9F4F7)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionScreen(navController: NavController) {
    val db: FirebaseFirestore = Firebase.firestore
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val appViewModel: AppViewModel = viewModel()
    val contacts: List<DocumentContact> = appViewModel.contacts.value

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    if (isLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("분석 중...") },
            text = { CircularProgressIndicator() },
            confirmButton = {}
        )
    }

    // 오늘 날짜의 dayOfYear를 이용해 문서 ID를 정합니다 (예: "DailyQuestion_74")
    val today = LocalDate.now().dayOfYear
    val docId = "DailyQuestion_$today"

    // 오늘의 질문 가져오기
    val dailyQuestions = DailyQuestion.values()
    val dailyQuestionIndex = today % dailyQuestions.size
    val questionText = dailyQuestions[dailyQuestionIndex].question

    var isEditing by remember { mutableStateOf(true) }
    // 사용자가 입력한 답변
    val answer = remember { mutableStateOf("") }
    val maxLength = 300

    // 감정 선택(6가지)
    val emotionList = listOf("😊", "😲", "❤️", "😡", "😢", "😐")
    var selectedEmotion by remember { mutableStateOf(emotionList[0]) }
    var emotionMenuExpanded by remember { mutableStateOf(false) }

    // Firestore에서 문서가 있는지 확인 (문서가 있으면 보기 모드)
    LaunchedEffect(Unit) {
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("DailyQuestion").document(docId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        answer.value = document.getString("answer") ?: ""
                        val savedEmotion = document.getString("emotion")
                        if (savedEmotion != null && savedEmotion in emotionList) {
                            selectedEmotion = savedEmotion
                        }
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                }
                .addOnFailureListener {
                    isEditing = true
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFFDFBF4)),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Last Letter", color = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { /* 알림 */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "알림", tint = Color.Black)
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정", tint = Color.Black)
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
            // 상단 영역: NEW, 오늘의 질문, 감정 선택
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFC107), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = "NEW", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "오늘의 질문", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Text(
                        text = selectedEmotion,
                        modifier = Modifier
                            .clickable { emotionMenuExpanded = true }
                            .padding(4.dp)
                    )
                    DropdownMenu(
                        expanded = emotionMenuExpanded,
                        onDismissRequest = { emotionMenuExpanded = false }
                    ) {
                        emotionList.forEach { emotion ->
                            DropdownMenuItem(
                                text = { Text(emotion) },
                                onClick = {
                                    selectedEmotion = emotion
                                    emotionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = questionText,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = answer.value,
                    onValueChange = {
                        if (it.length <= maxLength) {
                            answer.value = it
                        } else {
                            Toast.makeText(context, "최대 $maxLength 자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                    placeholder = { Text("답변을 적어주세요") },
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFE8F2E),
                        unfocusedBorderColor = Color(0xFFFE8F2E)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Text(
                    text = "${answer.value.length}/$maxLength 자",
                    modifier = Modifier.padding(top = 4.dp, end = 16.dp).align(Alignment.End),
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (uid != null) {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val response =
                                        RetrofitInstance2.api.analyzeText(
                                            EmotionRequest(
                                                answer.value
                                            )
                                        )
                                    selectedEmotion = response.emotion  // 서버 응답을 표시
                                } catch (e: Exception) {
                                    selectedEmotion = "오류 발생: ${e.message}"
                                } finally {
                                    selectedEmotion = when (selectedEmotion) {
                                        "기쁨" -> "😊"
                                        "놀라움" -> "😲"
                                        "사랑" -> "❤️"
                                        "분노" -> "😡"
                                        "슬픔" -> "😢"
                                        "중립" -> "😐"
                                        else -> selectedEmotion // 기본적으로 기존 값을 유지
                                    }
                                    isLoading = false
                                }
                            }
                            val dailyQuestionData = hashMapOf(
                                "question" to questionText,
                                "answer" to answer.value,
                                "timestamp" to System.currentTimeMillis(),
                                "emotion" to selectedEmotion
                            )
                            FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .collection("DailyQuestion")
                                .document(docId)
                                .set(dailyQuestionData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                    isEditing = false
                                }
                                .addOnFailureListener { exception ->
                                    println("Error adding document: ${exception.message}")
                                }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                        .padding(end = 16.dp)
                ) {
                    Text("저장하기", color = Color.Black)
                }
            } else {
                // 보기 모드: 답변 박스의 배경색은 선택한 감정에 따라 변경
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RectangleShape)
                        .background(getColorForEmotion(selectedEmotion))
                        .padding(16.dp)

                ) {
                    Text(
                        text = answer.value,
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = {
                        // 광고 시청 후 수정하기 (예시로 바로 수정 모드로 전환)
                        isEditing = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("광고 시청 후 수정하기")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.letter),
                    contentDescription = "편지쓰기",
                    modifier = Modifier
                        .size(200.dp)
                        .clickable { navController.navigate("yoursMain") },
                )

                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "편지를 써보는건 어때요?", color = Color.Gray)
            }
        }
    }
}
