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
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import kotlinx.coroutines.launch

@Composable
fun YoursContextScreen(contactId: String, navController: NavController) {
    val contact = remember { mutableStateOf<Contact?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhoneNumber by remember { mutableStateOf("") }
    var editedRelationship by remember { mutableStateOf("") }
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")

    LaunchedEffect(contactId) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                contact.value = contactRepository.getContactById(contactId)
                contact.value?.let {
                    editedName = it.name
                    editedPhoneNumber = it.phoneNumber
                    editedRelationship = it.relationship
                }
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
                        editedPhoneNumber = contactData.phoneNumber
                        editedRelationship = contactData.relationship
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
                    Column (
                        modifier = Modifier
                            .background(
                                Color(0xFFFFE4C4),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ){
                        TextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("상대방 이름 (별명)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 추가: 전화번호 입력 필드
                        TextField(
                            value = editedPhoneNumber,
                            onValueChange = { editedPhoneNumber = it },
                            label = { Text("전화번호") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // 추가: 관계 선택 드롭다운
                        RelationshipDropdown(
                            relationships,
                            selectedRelationship = editedRelationship,
                            onRelationshipSelected = { editedRelationship = it }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        contact.value?.let { currentContact ->
                            // 이름이 변경된 경우에만 업데이트
                            if (editedName != currentContact.name ||
                                editedPhoneNumber != currentContact.phoneNumber ||
                                editedRelationship != currentContact.relationship) {
                                coroutineScope.launch {
                                    try {
                                        val contactRepository = ContactRepository()
                                        // 업데이트된 Contact 객체 생성
                                        val updatedContact = currentContact.copy(
                                            name = editedName,
                                            phoneNumber = editedPhoneNumber,
                                            relationship = editedRelationship)
                                        // Firebase 업데이트
                                        contactRepository.updateContact(contactId, updatedContact)
                                        // 로컬 상태 업데이트
                                        contact.value = updatedContact
                                        showEditDialog = false
                                    } catch (e: Exception) {
                                        println("사용자 정보 업데이트 실패: ${e.message}")
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
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp)
        )
        Text(
            text = text,
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
