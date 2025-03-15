package com.ksj.lastletter

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionScreen(navController: NavController) {
    val answer = remember { mutableStateOf("") }
    val maxLength = 300
    val questionText = "내가 살면서 가장 행복했던 순간들은?"
    val db: FirebaseFirestore = Firebase.firestore
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFFDFBF4),
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(text = "Last Letter", color = Color.Black) }

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
                .background(Color(0xFFFDFBF4)),
        ) {
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
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = questionText,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFFDFBF4), shape = RoundedCornerShape(8.dp))
            )
            {
                OutlinedTextField(
                    value = answer.value,
                    onValueChange = {
                        if (it.length <= maxLength) answer.value = it
                        // maxLength 를 초과 하면 토스트 메세지 띄우기
                        else {
                            Toast.makeText(
                                context,
                                "최대 $maxLength 자까지 입력 가능합니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    placeholder = {
                        Text("답변을 적어주세요")
                    },
                    // outlinedTextField의 스타일을 설정합니다.
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFFFE8F2E),
                        unfocusedBorderColor = Color(0xFFFE8F2E),
                    ),
                )
                Text(
                    text = "${answer.value.length}/$maxLength 자",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.BottomEnd)
                )


            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val dailyQuestion =
                        hashMapOf("question" to questionText, "answer" to answer.value)
                    db.collection("DailyQuestion")
                        .add(dailyQuestion)
                        .addOnSuccessListener { answer.value = "" }
                        .addOnFailureListener { exception ->
                            println("Error adding document: ${exception.message}")
                        }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("저장하기") }
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
                Text(
                    text = "편지를 써보는건 어때요?",
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
fun DailyQuestionScreenPreview() {
    DailyQuestionScreen(navController = NavController(LocalContext.current))
}