package com.ksj.lastletter.keyfunction

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ksj.lastletter.R
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

//────────────────────────────────────────────
// 필요한 상수들 (다이얼로그 스타일, 버튼 색상 등)
//────────────────────────────────────────────
private val DialogShape = RoundedCornerShape(20.dp)
private val DialogBackground = Color(0xFFFFF2E3) // 연한 살구색 예시
private val ConfirmButtonColor = Color(0xFFB2A7FF) // 보라색 (확인)
private val DangerButtonColor = Color(0xFFFFB2A7)  // 핑크 (위험/삭제)
private val CancelButtonColor = Color.LightGray      // 취소

data class LetterInfo(
    val date: String,
    val title: String,
    val docId: String,
    val emotion: String = "중립"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoursContextScreen(contactId: String, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 연락처 정보 상태
    val contactState = remember { mutableStateOf<Contact?>(null) }
    // 편집할 연락처 (문서 형식)
    var editingContact by remember { mutableStateOf<DocumentContact?>(null) }

    // 편지 목록 상태
    var letterData by remember { mutableStateOf<List<LetterInfo>>(emptyList()) }

    // 무료 편지 크레딧 (기본 5)
    var letterCredits by remember { mutableStateOf(5) }

    // 광고 팝업 다이얼로그 상태
    var showAdDialog by remember { mutableStateOf(false) }

    // 관계 목록 (드롭다운용)
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")

    // 1) 연락처 로드
    LaunchedEffect(contactId) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val loadedContact = contactRepository.getContactById(contactId)
                contactState.value = loadedContact
            } catch (e: Exception) {
                println("연락처 로드 실패: ${e.message}")
            }
        }
    }

    // 2) 편지 목록 로드
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
                    val result = withContext(Dispatchers.IO) { lettersRef.get().await() }
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

    // 3) 무료 편지 등록 가능 수 계산
    val freeCreditsAvailable = (letterCredits - letterData.size).coerceAtLeast(0)

    // 4) 계정별 letterCredits 로드
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    val doc = FirebaseFirestore.getInstance().collection("users")
                        .document(uid).get().await()
                    val dbCredits = doc.getLong("letterCredits")?.toInt() ?: 5
                    letterCredits = dbCredits
                } catch (e: Exception) {
                    println("Error loading letterCredits: ${e.message}")
                    letterCredits = 5
                }
            }
        }
    }

    // 5) letterCount 업데이트 (최대 5장 표시)
    LaunchedEffect(letterData) {
        coroutineScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("letterCount", if (letterData.size > 5) 5 else letterData.size)
                        .await()
                } catch (e: Exception) {
                    println("Error updating letterCount: ${e.message}")
                }
            }
        }
    }

    // Scaffold 구성
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFFDFBF4)),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                title = {
                    // 상단바: 연락처 이름 + 편집 아이콘 + Spacer + 편지 아이콘 + 남은 무료 크레딧
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = contactState.value?.name ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        IconButton(onClick = {
                            // 편집 아이콘 클릭 시 현재 연락처를 DocumentContact 형태로 변환하여 editingContact에 저장
                            if (contactState.value != null) {
                                // 실제 구현에서는 Contact와 DocumentContact 간 변환 로직에 맞게 처리하세요.
                                editingContact = DocumentContact("dummyId", contactState.value!!)
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.edit),
                                contentDescription = "편집",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(id = R.drawable.letter),
                            contentDescription = "편지",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "x$freeCreditsAvailable",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFFDFBF4)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 편지 목록
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Column (modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 26.dp)){
                if (letterData.isEmpty()) {
                    Text(
                        text = "아직 작성된 편지가 없습니다.",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.Gray
                    )
                } else {
                    letterData.take(5).forEach { letterInfo ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    val encodedDocId = Uri.encode(letterInfo.docId)
                                    val encodedContactId = Uri.encode(contactId)
                                    navController.navigate(
                                        "inputtextscreen/$encodedDocId/$encodedContactId/${Uri.encode("fromfirebase")}"
                                    )
                                }
                                .padding(bottom = 8.dp)
                        ) {
                            ContextInfoCard(letterInfo.date, letterInfo.title, letterInfo.emotion)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            }


            // 우측상단에 추가 버튼
            // (편지 목록 아래에 위치)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                AddButton(onClick = {
                    if (freeCreditsAvailable > 0) {
                        navController.navigate("recording/$contactId/${contactState.value?.name ?: ""}")
                    } else {
                        showAdDialog = true
                    }
                })
            }

            // 편집 다이얼로그 (editingContact가 있을 때)
            editingContact?.let { docContact ->
                EditContactDialog(
                    documentContact = docContact,
                    onDismiss = { editingContact = null },
                    onSave = { updatedContact ->
                        coroutineScope.launch {
                            try {
                                val contactRepository = ContactRepository()
                                val success = contactRepository.updateContact(docContact.id, updatedContact)
                                if (success) {
                                    // 필요에 따라 로컬 목록 갱신 처리
                                }
                            } catch (e: Exception) {
                                println("업데이트 실패: ${e.message}")
                            } finally {
                                editingContact = null
                            }
                        }
                    },
                    onDelete = {
                        coroutineScope.launch {
                            try {
                                val contactRepository = ContactRepository()
                                contactRepository.deleteContact(docContact.id)
                            } catch (e: Exception) {
                                println("삭제 실패: ${e.message}")
                            } finally {
                                editingContact = null
                            }
                        }
                    }
                )
            }

            // 광고 팝업 다이얼로그
            if (showAdDialog) {
                AlertDialog(
                    onDismissRequest = { showAdDialog = false },
                    shape = DialogShape,
                    containerColor = DialogBackground,
                    title = { Text("광고 보기", color = Color.Black) },
                    text = {
                        Text(
                            text = "광고를 시청하시면 추가 편지 등록이 가능합니다.",
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAdDialog = false
                                navController.navigate("myPage")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7AC44))
                        ) {
                            Text("마이페이지", color = Color.White)
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showAdDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7AC44))) {
                            Text("취소", color = Color.White)
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
            .height(48.dp)
            .background(
                color = when (emotion) {
                    "기쁨" -> Color(0xFFFFF5E9)
                    "놀라움" -> Color(0xFFE9FFE9)
                    "사랑" -> Color(0xFFFFE9E9)
                    "분노" -> Color(0xFFFFE9FE)
                    "슬픔" -> Color(0xFFE9EDFF)
                    else -> Color(0xFFF7FFC8)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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

//────────────────────────────────────────────────────────────────────────────
// EditContactDialog: 사용자 편집 다이얼로그 (SettingsScreen과 동일한 UI)
//────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactDialog(
    documentContact: DocumentContact,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(documentContact.contact.name) }
    var relationship by remember { mutableStateOf(documentContact.contact.relationship) }
    var relationshipExpanded by remember { mutableStateOf(false) }
    var phoneNumber by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (documentContact.contact.phoneNumber.startsWith("010-"))
                    documentContact.contact.phoneNumber
                else "010-",
                selection = TextRange("010-".length)
            )
        )
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = DialogBackground,
        title = { Text("사용자 편집", fontSize = 18.sp, color = Color.Black) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("상대방 이름 (별명)", fontSize = 14.sp, color = Color.Black)
                TextField(
                    value = name,
                    onValueChange = { name = it.take(15) },
                    placeholder = { Text("예: 남편") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("전화번호", fontSize = 14.sp, color = Color.Black)
                TextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }
                        val limited = digits.take(11)
                        val formatted = when {
                            limited.length <= 3 -> limited
                            limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
                            else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
                        }
                        val digitsBefore = newValue.text.substring(0, newValue.selection.start)
                            .filter { it.isDigit() }
                            .length
                        val newCursorPosition = when {
                            digitsBefore <= 3 -> digitsBefore
                            digitsBefore <= 7 -> digitsBefore + 1
                            else -> digitsBefore + 2
                        }.coerceAtMost(formatted.length)
                        phoneNumber = TextFieldValue(
                            text = formatted,
                            selection = TextRange(newCursorPosition)
                        )
                    },
                    placeholder = { Text("예: 010-3764-9287") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("상대방과의 관계", fontSize = 14.sp, color = Color.Black)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { relationshipExpanded = true }
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (relationship.isEmpty()) "배우자" else relationship, color = Color.Black)
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "드롭다운 열기",
                            tint = Color.Black
                        )
                    }
                }
                DropdownMenu(
                    expanded = relationshipExpanded,
                    onDismissRequest = { relationshipExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")
                    relationships.forEach { rel ->
                        DropdownMenuItem(
                            text = { Text(rel, color = Color.Black) },
                            onClick = {
                                relationship = rel
                                relationshipExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        val updatedContact = Contact(
                            name = name,
                            phoneNumber = phoneNumber.text,
                            relationship = relationship
                        )
                        onSave(updatedContact)
                        Toast.makeText(context, "사용자가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ConfirmButtonColor)
                ) {
                    Text("저장", color = Color.White)
                }
                Button(
                    onClick = { showDeleteConfirmation = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerButtonColor)
                ) {
                    Text("삭제하기", color = Color.White)
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = CancelButtonColor)
                ) {
                    Text("취소", color = Color.White)
                }
            }
        }
    )


    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            shape = DialogShape,
            containerColor = DialogBackground,
            title = { Text("정말로 삭제하시겠습니까?", fontSize = 18.sp, color = Color.Black) },
            text = { Text("저장된 정보와 작성한 편지가 모두 삭제됩니다.", fontSize = 14.sp, color = Color.Black) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteConfirmation = false
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerButtonColor)
                    ) {
                        Text("예, 삭제하겠습니다", color = Color.White)
                    }
                    Button(
                        onClick = { showDeleteConfirmation = false },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = CancelButtonColor)
                    ) {
                        Text("취소", color = Color.White)
                    }
                }
            }
        )
    }
}
