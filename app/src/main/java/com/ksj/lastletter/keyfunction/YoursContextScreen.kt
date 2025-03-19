package com.ksj.lastletter.keyfunction

import android.net.Uri
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// 편지 데이터를 저장할 데이터 클래스
data class LetterInfo(
    val date: String,
    val title: String,
    val docId: String,
    val emotion: String = "중립" // 기본값은 중립으로 설정
)

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

// 편지 데이터 구조 변경 - emotion도 포함하도록
            var letterData by remember {
                mutableStateOf<List<LetterInfo>>(emptyList())
            }

// Firebase에서 데이터 불러오기
            LaunchedEffect(contactId) {
                coroutineScope.launch {
                    try {
                        // Firebase에서 현재 연락처(contactId)의 편지 목록 가져오기
                        val db = FirebaseFirestore.getInstance()
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            val lettersRef = db.collection("users").document(userId)
                                .collection("Yours").document(contactId)
                                .collection("letters")
                                .orderBy(
                                    "timestamp",
                                    com.google.firebase.firestore.Query.Direction.DESCENDING
                                )

                            val result = withContext(Dispatchers.IO) {
                                lettersRef.get().await()
                            }

                            val fetchedLetters = mutableListOf<LetterInfo>()
                            for (doc in result) {
                                val date = doc.getString("date") ?: ""
                                val title = doc.getString("title") ?: ""
                                val docId = doc.id
                                val emotion =
                                    doc.getString("emotion") ?: "중립" // emotion 정보 추가로 불러오기
                                fetchedLetters.add(LetterInfo(date, title, docId, emotion))
                            }
                            letterData = fetchedLetters
                        }
                    } catch (e: Exception) {
                        println("편지 로드 실패: ${e.message}")
                    }
                }
            }

// 편지 목록 표시 부분 수정
            if (letterData.isEmpty()) {
                Text(
                    text = "아직 작성된 편지가 없습니다.",
                    modifier = Modifier.padding(top = 16.dp),
                    color = Color.Gray
                )
            } else {
                // 수정된 코드
                letterData.forEach { letterInfo ->
                    // 카드에 클릭 기능 추가
                    Box(modifier = Modifier.clickable {
                        // InputText 화면으로 이동
                        navController.navigate(
                            "inputtextscreen/${Uri.encode(letterInfo.docId)}/${
                                Uri.encode(
                                    contactId
                                )
                            }/${Uri.encode("fromfirebase")}"
                        )
                    }) {
                        ContextInfoCard(letterInfo.date, letterInfo.title, letterInfo.emotion)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("이름 수정") },
                text = {
                    Column(
                        modifier = Modifier
                            .background(
                                Color(0xFFFFE4C4),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
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
                                editedRelationship != currentContact.relationship
                            ) {
                                coroutineScope.launch {
                                    try {
                                        val contactRepository = ContactRepository()
                                        // 업데이트된 Contact 객체 생성
                                        val updatedContact = currentContact.copy(
                                            name = editedName,
                                            phoneNumber = editedPhoneNumber,
                                            relationship = editedRelationship
                                        )
                                        // Firebase 업데이트
                                        contactRepository.updateContact(
                                            contactId,
                                            updatedContact
                                        )
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
fun ContextInfoCard(date: String, text: String, emotion: String = "중립") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = when (emotion) {
                    "기쁨" -> Color(0xFFFFF5E9)
                    "놀라움" -> Color(0xFFE9FFE9)
                    "사랑" -> Color(0xFFFFE9E9)
                    "분노" -> Color(0xFFFFE9FE)
                    "슬픔" -> Color(0xFFE9EDFF)
                    "중립" -> Color(0xFFF7FFC8)
                    else -> Color(0xFFF7FFC8) // 기본값은 중립 색상
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
