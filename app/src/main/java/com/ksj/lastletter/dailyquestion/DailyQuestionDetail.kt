package com.ksj.lastletter.dailyquestion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionDetail(navController: NavController, docId: String) {
    var questionAnswer by remember { mutableStateOf<QuestionAnswer?>(null) }
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(docId, uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("DailyQuestion")
                .document(docId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        questionAnswer = parseQuestionAnswer(document)
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error reading document: ${exception.message}")
                }
        }
    }

    val backgroundColor = Color(0xFFFDFBF4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.KeyboardArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundColor)
        ) {
            if (questionAnswer == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = questionAnswer?.question ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val timeText = remember(questionAnswer?.timestamp) {
                        questionAnswer?.timestamp?.let {
                            val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                            sdf.format(Date(it))
                        } ?: ""
                    }
                    Text(
                        text = "1번째 질문 $timeText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(getColorForEmotion(questionAnswer?.emotion ?: "😊"), shape = RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = questionAnswer?.answer ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            // navController.navigate("adScreen")
                        }) {
                            Text("광고 보고 수정하기", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Firestore의 DocumentSnapshot를 파싱하여 QuestionAnswer 객체로 변환하는 함수
 */
private fun parseQuestionAnswer(document: DocumentSnapshot): QuestionAnswer {
    val id = document.id
    val question = document.getString("question") ?: ""
    val answer = document.getString("answer") ?: ""
    val rawTimestamp = document.get("timestamp")
    val numericTimestamp = when (rawTimestamp) {
        is Number -> rawTimestamp.toLong()
        is String -> {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.parse(rawTimestamp)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
        else -> 0L
    }
    val emotion = document.getString("emotion") ?: "😊"
    return QuestionAnswer(
        id = id,
        question = question,
        answer = answer,
        timestamp = numericTimestamp,
        emotion = emotion
    )
}
