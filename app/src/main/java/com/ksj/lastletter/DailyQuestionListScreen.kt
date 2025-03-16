package com.ksj.lastletter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

data class QuestionAnswer(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val timestamp: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionListScreen(navController: NavController) {
    val backgroundColor = Color(0xFFFDFBF4)
    var questionAnswers by remember { mutableStateOf(listOf<QuestionAnswer>()) }
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("DailyQuestion")
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<QuestionAnswer>()
                    for (document in result) {
                        val qa = document.toObject<QuestionAnswer>().copy(id = document.id)
                        list.add(qa)
                    }
                    questionAnswers = list.sortedByDescending { it.timestamp ?: 0L }
                }
                .addOnFailureListener { exception ->
                    // 에러 처리
                }
        }
    }

    Scaffold(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = backgroundColor
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    Text(text = "매일 질문 모음", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(backgroundColor)
                .fillMaxSize()
        ) {
            if (questionAnswers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "아직 작성된 질문이 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(questionAnswers) { qa ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RectangleShape)
                                .background(Color(0xFFE9F4F7))
                                .clickable {
                                    navController.navigate("dailyQuestionDetail/${qa.id}")
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = qa.question,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
