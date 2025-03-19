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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.FastAPI.EmotionRequest
import com.ksj.lastletter.FastAPI.RetrofitClient
import com.ksj.lastletter.FastAPI.RetrofitInstance2
import com.ksj.lastletter.FastAPI.TextRequest
import kotlinx.coroutines.launch

@Composable
fun InputTextScreen(navController: NavController) {
    // 네비게이션에서 전달된 파라미터 추출
    val backStackEntry = navController.currentBackStackEntry
    val arguments = backStackEntry?.arguments

    var titleText by remember {
        val encodedTitle = arguments?.getString("title") ?: ""
        val decodedTitle = try {
            java.net.URLDecoder.decode(encodedTitle, "UTF-8")
        } catch (e: Exception) {
            encodedTitle
        }
        mutableStateOf(decodedTitle)
    }

    var letterText by remember {
        val encodedContent = arguments?.getString("content") ?: ""
        val decodedContent = try {
            java.net.URLDecoder.decode(encodedContent, "UTF-8")
        } catch (e: Exception) {
            encodedContent
        }
        mutableStateOf(decodedContent)
    }

    var selectedEmotion by remember {
        val encodedEmotion = arguments?.getString("emotion") ?: "기쁨"
        val decodedEmotion = try {
            java.net.URLDecoder.decode(encodedEmotion, "UTF-8")
        } catch (e: Exception) {
            encodedEmotion
        }
        mutableStateOf(decodedEmotion)
    }
    var maxTextLength by remember { mutableIntStateOf(500) }
// 날짜 파라미터 (없으면 현재 날짜)
    val receivedDate = arguments?.getString("date")
    val currentDate = java.text.SimpleDateFormat(
        "MM월 dd일",
        java.util.Locale.getDefault()
    ).format(java.util.Date())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    if (letterText.length == maxTextLength) {
        Toast.makeText(context, "글자 수를 초과하셨습니다.", Toast.LENGTH_SHORT).show()
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
                    selectedEmotion = selectedEmotion,
                    onEmotionSelected = { newEmotion -> selectedEmotion = newEmotion },
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
                    .weight(4f)
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
                                "emotion" to selectedEmotion,
                                "timestamp" to com.google.firebase.Timestamp.now()
                            )

                            // 저장 경로: users/{userId}/Yours/{contactId}/letters/{letterId}
                            val contactId = navController.previousBackStackEntry
                                ?.arguments?.getString("contactId") ?: ""

                            db.collection("users").document(userId)
                                .collection("Yours").document(contactId)
                                .collection("letters").add(letterData)
                                .addOnSuccessListener {
                                    Log.d("InputTextScreen", "Letter saved successfully")
                                    // 저장 성공 시 YourContextScreen으로 이동
                                    navController.navigate("yourscontextscreen/${contactId}") {
                                        // 스택에서 현재 화면 제거
                                        popUpTo("yourscontextscreen/${contactId}") {
                                            inclusive = false
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("InputTextScreen", "Error saving letter", e)
                                    // 에러 처리
                                    Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response =
                                    RetrofitClient.apiService.generateText(TextRequest(letterText))
                                titleText = response.generated_text  // 서버 응답을 표시
                            } catch (e: Exception) {
                                titleText = "오류 발생: ${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
                ) {
                    Text(
                        "제목 추천",
                        color = Color.Black
                    )
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response =
                                    RetrofitInstance2.api.analyzeText(EmotionRequest(letterText))
                                selectedEmotion = response.emotion  // 서버 응답을 표시
                            } catch (e: Exception) {
                                selectedEmotion = "오류 발생: ${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
                ) {
                    Text(
                        "감정 분석",
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

    val emotions = listOf("기쁨", "놀라움", "사랑", "슬픔", "분노", "중립")
    val emotionIcons = mapOf(
        "기쁨" to "😆",
        "놀라움" to "😲",
        "사랑" to "😍",
        "슬픔" to "😢",
        "분노" to "😡",
        "중립" to "😐"
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
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
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
                    text = { Text("${emotionIcons[emotion]} $emotion") },
                    onClick = {
                        onEmotionSelected(emotion) // 선택된 감정을 부모로 전달
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InputTextScreenPreview() {
    // NavController는 preview에서 바로 사용할 수 없으므로 임시로 NavController를 넣습니다.
    val navController = rememberNavController() // NavController 생성
    InputTextScreen(navController = navController) // Preview에서 InputTextScreen 호출
}
