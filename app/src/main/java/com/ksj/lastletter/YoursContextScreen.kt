package com.ksj.lastletter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun YoursContextScreen(contactId: String, navController: NavController) {
    val contact = remember { mutableStateOf<Contact?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    // Firestore에서 데이터 로드
    LaunchedEffect(contactId) {
        coroutineScope.launch { // 추가: 코루틴 스코프 사용
            try {
                val contactRepository = ContactRepository()
                contact.value = contactRepository.getContactById(contactId)
                println("연락처 로드 성공: ${contact.value?.name}")
                contact.value?.let { editedName = it.name }
            } catch (e: Exception) {
                println("연락처 로드 실패: ${e.message}")
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            contact.value?.let { contactData ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = contactData.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    IconButton(onClick = {
                        editedName = contactData.name
                        showEditDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFFFFDCA8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ContextAddButton(
                        onClick = {
                            // 녹음 화면으로 네비게이션
                            contact.value?.let { contactData ->
                                navController.navigate("recording/${contactId}/${contactData.name}")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val cardData = listOf(
                Pair("1월 7일", "창 밖을 바라보다가"),
                Pair("12월 4일", "설거지는 자기 전에"),
                Pair("12월 2일", "햇살이 좋아서"),
                Pair("11월 26일", "너가 어렸을 때"),
                Pair("10월 7일", "바람이 차, 감기 조심")
            )

            cardData.forEach { (date, text) ->
                ContextInfoCard(date, text)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("이름 수정") },
                text = {
                    Column {
                        TextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("상대방 이름 (별명)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        contact.value?.let { currentContact ->
                            // 이름이 변경된 경우에만 업데이트
                            if (editedName != currentContact.name && editedName.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        val contactRepository = ContactRepository()
                                        // 업데이트된 Contact 객체 생성
                                        val updatedContact = currentContact.copy(name = editedName)
                                        // Firebase 업데이트
                                        contactRepository.updateContact(contactId, updatedContact)
                                        // 로컬 상태 업데이트
                                        contact.value = updatedContact
                                        showEditDialog = false
                                    } catch (e: Exception) {
                                        println("이름 업데이트 실패: ${e.message}")
                                    }
                                }
                            } else {
                                showEditDialog = false
                            }
                        }
                    }) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    Button(onClick = { showEditDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}


@Composable
fun ContextInfoCard(date: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = when (date) {
                    "1월 7일" -> Color(0xFFFFF4E6)
                    "12월 4일" -> Color(0xFFFFE6E6)
                    "12월 2일" -> Color(0xFFE6FFE6)
                    "11월 26일" -> Color(0xFFE6E6FF)
                    else -> Color(0xFFFFE6FF)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = date,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
fun ContextAddButton(onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(width = 48.dp, height = 48.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            fontSize = 24.sp,
            color = Color(0xFFFFDCA8)
        )
    }
}