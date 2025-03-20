package com.ksj.lastletter.dailyquestion

import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuestionDetail(navController: NavController, docId: String) {
    var questionAnswer by remember { mutableStateOf<QuestionAnswer?>(null) }
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var editMode by remember { mutableStateOf(false) }
    var maxTextLength by remember { mutableIntStateOf(300) }
    val context = LocalContext.current

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
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
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
                    if (questionAnswer == null) {
                        AnswerIsNull(questionAnswer!!,
                            onClick = {
                                editMode = true
                            })
                    } else {
                        if (editMode) {
                            DailyQuestionTextField(
                                questionAnswer!!,
                                context,
                                maxTextLength,
                                onClick = {
                                    maxTextLength = 600
                                    Toast.makeText(context, "글자 수가 늘어났어요!", Toast.LENGTH_SHORT)
                                        .show()
                                },
                                onClickSave = { updatedText, selectedEmotion ->
                                    val updatedData = hashMapOf(
                                        "question" to questionAnswer!!.question,
                                        "answer" to updatedText,
                                        "timestamp" to System.currentTimeMillis(),
                                        "emotion" to selectedEmotion
                                    )

                                    uid?.let { userId ->
                                        db.collection("users")
                                            .document(userId)
                                            .collection("DailyQuestion")
                                            .document(docId)
                                            .set(updatedData)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                                                questionAnswer = questionAnswer?.copy(
                                                    answer = updatedText,
                                                    emotion = selectedEmotion
                                                )?.let { newQuestionAnswer -> mutableStateOf(newQuestionAnswer).value }
                                                editMode = false
                                            }
                                            .addOnFailureListener { exception ->
                                                println("Error updating document: ${exception.message}")
                                            }
                                    }
                                }
                            )
                        } else {
                            DailyQuestionText(questionAnswer!!,
                                onClick = {
                                    editMode = true
                                })
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun AnswerIsNull(questionAnswer: QuestionAnswer, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                getColorForEmotion(questionAnswer.emotion),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "없음",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffFFEBEB)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("광고 보고 수정하기", color = Color.Black)
        }
    }
}

@Composable
fun DailyQuestionText(questionAnswer: QuestionAnswer, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                getColorForEmotion(questionAnswer.emotion),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = questionAnswer.answer,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffFFEBEB)),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("광고 보고 수정하기", color = Color.Black)
        }
    }
}

@Composable
fun DailyQuestionTextField(
    questionAnswer: QuestionAnswer,
    context: Context,
    maxTextLength: Int,
    onClick: () -> Unit,
    onClickSave: (String, String) -> Unit
) {
    var letterText by remember { mutableStateOf(questionAnswer.answer) }
    if (letterText.length == maxTextLength) {
        Toast.makeText(context, "${maxTextLength}자까지 입력할 수 있습니다.", Toast.LENGTH_SHORT)
            .show()
    }
    // 감정 선택(6가지)
    val emotionList = listOf("😊", "😲", "❤️", "😡", "😢", "😐")
    var selectedEmotion by remember { mutableStateOf(emotionList[0]) }
    var emotionMenuExpanded by remember { mutableStateOf(false) }

    Box (
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.White , RoundedCornerShape(10.dp))
            .border(1.dp,Color.Gray, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ){
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
    TextField(
        value = letterText,
        onValueChange = { inputText ->
            if (inputText.length <= maxTextLength) {
                letterText = inputText
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = getColorForEmotion(questionAnswer.emotion),
            focusedContainerColor = getColorForEmotion(questionAnswer.emotion),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "글자 수: ${letterText.length}/${maxTextLength}",
            color = Color.Gray
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffFFEBEB)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("광고 보고 600자로 늘리기", color = Color.Black)
        }
        Button(
            onClick = { onClickSave(letterText, selectedEmotion) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("저장하기", color = Color.Black)
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
