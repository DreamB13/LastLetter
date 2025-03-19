package com.ksj.lastletter.keyfunction

import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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

data class LetterInfo(
    val date: String,
    val title: String,
    val docId: String,
    val emotion: String = "중립"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoursContextScreen(contactId: String, navController: NavController) {
    val contactState = remember { mutableStateOf<Contact?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 편집 다이얼로그 상태
    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhoneNumber by remember { mutableStateOf("") }
    var editedRelationship by remember { mutableStateOf("") }
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")

    // 편지 목록
    var letterData by remember { mutableStateOf<List<LetterInfo>>(emptyList()) }

    // 연락처 로드
    LaunchedEffect(contactId) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val loadedContact = contactRepository.getContactById(contactId)
                contactState.value = loadedContact
                loadedContact?.let {
                    editedName = it.name
                    editedPhoneNumber = it.phoneNumber
                    editedRelationship = it.relationship
                }
            } catch (e: Exception) {
                println("연락처 로드 실패: ${e.message}")
            }
        }
    }

    // 편지 목록 로드
    LaunchedEffect(contactId) {
        coroutineScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val lettersRef = db.collection("users").document(userId)
                        .collection("Yours").document(contactId)
                        .collection("letters")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)

                    val result = withContext(Dispatchers.IO) {
                        lettersRef.get().await()
                    }

                    val fetchedLetters = mutableListOf<LetterInfo>()
                    for (doc in result) {
                        val date = doc.getString("date") ?: ""
                        val title = doc.getString("title") ?: ""
                        val docId = doc.id
                        val emotion = doc.getString("emotion") ?: "중립"
                        fetchedLetters.add(LetterInfo(date, title, docId, emotion))
                    }
                    letterData = fetchedLetters
                }
            } catch (e: Exception) {
                println("편지 로드 실패: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            // TopAppBar 안에 뒤로가기 + (연락처 이름 + 편집 아이콘) + (편지 개수)
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFFDFBF4)),
                navigationIcon = {
                    // 뒤로가기 버튼
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    // 가운데(or 왼쪽)에 Row로 묶어서 연락처 이름 + 편집 아이콘 + (Spacer) + 편지개수
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 연락처 이름
                        Text(
                            text = contactState.value?.name ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        // 편집 아이콘
                        IconButton(
                            onClick = {
                                contactState.value?.let {
                                    editedName = it.name
                                    editedPhoneNumber = it.phoneNumber
                                    editedRelationship = it.relationship
                                }
                                showEditDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "이름 수정",
                                tint = Color(0xFFFFDCA8)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // 편지 아이콘 + 개수
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "편지",
                            tint = Color(0xFFFFDCA8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "x${letterData.size}",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFFDFBF4)
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFFFFFBF5)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                // + 버튼 (편지 작성)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    ContextAddButton(
                        onClick = {
                            contactState.value?.let { contactData ->
                                navController.navigate("recording/$contactId/${contactData.name}")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 편지 목록
                if (letterData.isEmpty()) {
                    Text(
                        text = "아직 작성된 편지가 없습니다.",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.Gray
                    )
                } else {
                    letterData.forEach { letterInfo ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    // InputText 화면으로 이동
                                    val encodedDocId = Uri.encode(letterInfo.docId)
                                    val encodedContactId = Uri.encode(contactId)
                                    navController.navigate("inputtextscreen/$encodedDocId/$encodedContactId/${Uri.encode("fromfirebase")}")
                                }
                                .padding(bottom = 8.dp)
                        ) {
                            ContextInfoCard(letterInfo.date, letterInfo.title, letterInfo.emotion)
                        }
                    }
                }
            }

            // 이름 수정 다이얼로그
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
                            TextField(
                                value = editedPhoneNumber,
                                onValueChange = { editedPhoneNumber = it },
                                label = { Text("전화번호") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RelationshipDropdown(
                                relationships,
                                selectedRelationship = editedRelationship,
                                onRelationshipSelected = { editedRelationship = it }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            contactState.value?.let { currentContact ->
                                if (editedName != currentContact.name ||
                                    editedPhoneNumber != currentContact.phoneNumber ||
                                    editedRelationship != currentContact.relationship
                                ) {
                                    val updatedContact = currentContact.copy(
                                        name = editedName,
                                        phoneNumber = editedPhoneNumber,
                                        relationship = editedRelationship
                                    )
                                    coroutineScope.launch {
                                        try {
                                            val contactRepository = ContactRepository()
                                            contactRepository.updateContact(contactId, updatedContact)
                                            contactState.value = updatedContact
                                        } catch (e: Exception) {
                                            println("사용자 정보 업데이트 실패: ${e.message}")
                                        } finally {
                                            showEditDialog = false
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
                    else -> Color(0xFFF7FFC8) // 중립, 기타
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
