package com.ksj.lastletter.keyfunction

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.FastAPI.EmotionRequest
import com.ksj.lastletter.FastAPI.RetrofitClient
import com.ksj.lastletter.FastAPI.RetrofitInstance2
import com.ksj.lastletter.FastAPI.TextRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun InputTextScreen(
    navController: NavController,
    recognizedText: String,
    customDateText: String,
    selectedEmotion: String
) {
    var titleText by remember { mutableStateOf("") }
    var maxTextLength by remember { mutableIntStateOf(500) }
    var letterText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var emotion by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    if (letterText.length == maxTextLength) {
        Toast.makeText(context, "글자 수를 초과하셨습니다.", Toast.LENGTH_SHORT).show()
    }
    val backStackEntry = navController.currentBackStackEntry
    val arguments = backStackEntry?.arguments
    var currentDate by remember {
        mutableStateOf(
            java.text.SimpleDateFormat("MM월 dd일", java.util.Locale.getDefault())
                .format(java.util.Date())
        )
    }
    // Firebase에서 데이터 로드 여부 확인
    if (selectedEmotion == "fromfirebase") {
        // recognizedText는 docId, customDateText는 contactId로 사용
        val docId = recognizedText
        val contactId = customDateText

        // Firebase에서 데이터 가져오기
        LaunchedEffect(docId, contactId) {
            isLoading = true
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val db = FirebaseFirestore.getInstance()
                    val docRef = db.collection("users").document(userId)
                        .collection("Yours").document(contactId)
                        .collection("letters").document(docId)

                    val document = docRef.get().await()
                    if (document.exists()) {
                        titleText = document.getString("title") ?: ""
                        letterText = document.getString("content") ?: ""
                        emotion = document.getString("emotion") ?: ""
                        currentDate = document.getString("date") ?: ""
                    } else {
                        Toast.makeText(context, "문서를 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "데이터 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    } else {
        // 기존 방식대로 파라미터에서 초기화
        titleText = customDateText
        letterText = recognizedText
        emotion = selectedEmotion
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {

            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1.5f)
            ) {
                TextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = { Text("제목", color = Color(0xffAFADAD)) },
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                    modifier = Modifier
                        .border(1.dp, shape = RoundedCornerShape(20.dp), color = Color(0xffABABAB))
                        .padding(0.dp)
                        .fillMaxSize()
                        .weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Spacer(modifier = Modifier.weight(4f))
                EmotionSelector(
                    selectedEmotion = emotion,
                    onEmotionSelected = { newEmotion -> emotion = newEmotion },
                    modifier = Modifier.weight(1.5f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
                    .weight(0.6f)
            ) {
                Text(
                    "작성일 : $currentDate",
                    color = Color(0xffAFADAD),
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.CenterVertically)
                )
                Text(
                    "글자 수: ${letterText.length}/${maxTextLength}",
                    color = Color(0xffAFADAD),
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            TextField(
                value = letterText,
                onValueChange = { inputText ->
                    if (inputText.length <= maxTextLength) {
                        letterText = inputText
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xffFFF6FE),
                    unfocusedContainerColor = Color(0xffFFF6FE)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxSize()
                    .weight(3f)
            )
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(5f)
            ) {
                Button(
                    onClick = {/*광고 띄우면서 최대 글자수 1000으로 변경*/
                        maxTextLength = 1000
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xffFFEBEB)),
                ) {
                    Text(
                        "광고 보고 1000자로 늘리기",
                        color = Color.Black
                    )
                }
                Button(
                    onClick = {
                        // Firebase에 저장
                        val db = FirebaseFirestore.getInstance()
                        val userId = FirebaseAuth.getInstance().currentUser?.uid

                        if (userId != null) {
                            // 편지 데이터 생성
                            val letterData = hashMapOf(
                                "date" to currentDate,
                                "title" to titleText,
                                "content" to letterText,
                                "emotion" to emotion,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )

                            // 저장 경로: users/{userId}/Yours/{contactId}/letters/{letterId}
                            val contactId = arguments?.getString("contactId")
                                ?: navController.previousBackStackEntry?.arguments?.getString("contactId")
                                ?: ""

                            db.collection("users").document(userId)
                                .collection("Yours").document(contactId)
                                .collection("letters").add(letterData)
// 저장 버튼의 onClick 부분만 수정
                                .addOnSuccessListener {
                                    Log.d("InputTextScreen", "Letter saved successfully")

                                    // 저장 후 YoursContextScreen으로 직접 이동하도록 수정
                                    try {
                                        // contactId를 사용하여 YoursContextScreen으로 이동
                                        navController.navigate("yoursContext/${contactId}") {
                                            // 불필요한 화면 스택 제거
                                            popUpTo("yoursContext/${contactId}") {
                                                inclusive = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InputTextScreen", "Navigation error: ${e.message}")
                                        // 오류 발생 시 대체 네비게이션
                                        navController.popBackStack()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("InputTextScreen", "Error saving letter", e)
                                    // 에러 처리
                                    Toast.makeText(
                                        context,
                                        "저장 실패: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            // 로그인되지 않은 경우
                            Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
                ) {
                    Text(
                        "저장하기",
                        color = Color.Black
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(5f)
            )
        }
    }
}

@Composable
fun EmotionSelector(
    selectedEmotion: String,
    onEmotionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val emotions = listOf("기쁨", "놀라움", "사랑", "슬픔", "분노", "중립", "해당 없음")
    val emotionIcons = mapOf(
        "기쁨" to "😆",
        "놀라움" to "😲",
        "사랑" to "😍",
        "슬픔" to "😢",
        "분노" to "😡",
        "중립" to "😐",
        "해당 없음" to "??"
    )

    Box(
        modifier = modifier // 외부에서 전달된 Modifier 사용
            .border(1.dp, shape = RoundedCornerShape(20.dp), color = Color(0xffABABAB))
            .background(Color.White)
            .clickable { expanded = true }
            .padding(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${emotionIcons[selectedEmotion]}",
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                color = Color.Black,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "드롭다운",
                modifier = Modifier.clickable { expanded = true }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            emotions.forEach { emotion ->
                DropdownMenuItem(
                    text = { Text("${emotionIcons[emotion]} $emotion", color = Color.Black) },
                    onClick = {
                        onEmotionSelected(emotion) // 선택된 감정을 부모로 전달
                        expanded = false
                    }
                )
            }
        }
    }
}

