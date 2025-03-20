package com.ksj.lastletter.keyfunction

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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.internal.wait

// ─────────────────────────────────────────────────────────────────────────────
// 다이얼로그 스타일 (EditContactDialog와 동일하게)
// ─────────────────────────────────────────────────────────────────────────────
private val DialogShape = RoundedCornerShape(20.dp)
private val DialogBackground = Color(0xFFFFF2E3) // 연한 살구색
private val ConfirmButtonColor = Color(0xFFB2A7FF) // 보라색 (확인)
private val CancelButtonColor = Color.LightGray    // 취소

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoursMainScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid

    // 연락처 목록
    val contacts = remember { mutableStateListOf<DocumentContact>() }

    // 사용자 추가 다이얼로그
    var showAddUserDialog by remember { mutableStateOf(false) }
    // 광고 팝업 다이얼로그
    var showAdDialog by remember { mutableStateOf(false) }

    // 남은 무료 추가 횟수
    var remainingAdds by remember { mutableStateOf(2) }

    // 연락처가 하나도 없을 경우 자동 팝업
    var showNoContactsDialog by remember { mutableStateOf(false) }

    // 최초 로딩
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val fetchedContacts = contactRepository.getContactsWithCoroutines()
                contacts.clear()
                contacts.addAll(fetchedContacts)
                if (contacts.isEmpty()) {
                    showNoContactsDialog = true
                }

                // 남은 횟수 Firestore에서 로드
                if (uid != null) {
                    val doc = firestore.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val dbRemaining = doc.getLong("remainingAdds")
                        if (dbRemaining != null) {
                            remainingAdds = dbRemaining.toInt()
                        } else {
                            // 없으면 2로 초기화
                            firestore.collection("users").document(uid)
                                .set(mapOf("remainingAdds" to 2), SetOptions.merge())
                            remainingAdds = 2
                        }
                    } else {
                        // 문서가 없으면 새로 생성
                        firestore.collection("users").document(uid)
                            .set(mapOf("remainingAdds" to 2), SetOptions.merge())
                        remainingAdds = 2
                    }
                }
            } catch (e: Exception) {
                println("Error loading contacts: ${e.message}")
            }
        }
    }

    // 화면 UI
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
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "남은 너에게",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                actions = {
                    // 남은 추가 횟수
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.person),
                            contentDescription = "편지 받을 사람",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "x$remainingAdds", color = Color.Black)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 연락처 목록 표시
            contacts.forEach { documentContact ->
                InfoCard(
                    text = documentContact.contact.name,
                    modifier = Modifier.clickable {
                        navController.navigate("yoursContext/${documentContact.id}")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 가운데 + 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AddButton(
                    onClick = {
                        // 남은 횟수가 있으면 AddUserDialog, 없으면 광고 팝업
                        if (remainingAdds > 0) {
                            showAddUserDialog = true
                        } else {
                            showAdDialog = true
                        }
                    }
                )
            }
        }
    }

    // 처음 앱 실행 시 연락처가 없다면 알림 팝업
    if (showNoContactsDialog) {
        AlertDialog(
            onDismissRequest = { /* 팝업 강제 유지 */ },
            title = { Text("알림", color = Color.Black) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "※ 앱 이용 30일 후 확인 알람이 옵니다.\n\n" +
                                "※ 앱 이용 37일이 지나면 설정한 번호로 편지가 전송됩니다.\n\n" +
                                "※ 잘못된 번호 입력이나 내용으로 인한 피해는 책임지지 않습니다.",
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNoContactsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDCA8))
                ) {
                    Text("본 내용을 확인 했습니다", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // "사용자 추가" 다이얼로그 (새 디자인)
    if (showAddUserDialog) {
        AddUserDialog(
            onDismiss = { showAddUserDialog = false },
            onSave = { newContact ->
                // Firebase에 새 연락처 저장
                coroutineScope.launch {
                    try {
                        val contactRepository = ContactRepository()
                        contactRepository.addContact(newContact)
                        val updatedContacts = contactRepository.getContactsWithCoroutines()
                        contacts.clear()
                        contacts.addAll(updatedContacts)

                        // 남은 횟수 감소
                        remainingAdds--

                        // 남은 횟수 Firebase에 저장
                        if (uid != null) {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .update("remainingAdds", remainingAdds)
                                .addOnFailureListener {
                                    println("Error updating remainingAdds: ${it.message}")
                                }
                        }
                    } catch (e: Exception) {
                        println("Error adding contact: ${e.message}")
                    }
                }
            }
        )
    }

    // 광고 팝업 다이얼로그
    if (showAdDialog) {
        AlertDialog(
            onDismissRequest = { showAdDialog = false },
            title = { Text("사용 인원을 초과했어요!", color = Color.Black) },
            text = {
                Text(
                    text = "추가 결제를 통해서 편지를 보낼 사람을 추가할 수 있어요",
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
                Button(
                    onClick = { showAdDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7AC44))
                ) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddUserDialog: "사용자 추가"를 EditContactDialog와 같은 디자인으로
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    // 입력 상태
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var relationshipExpanded by remember { mutableStateOf(false) }

    // 전화번호는 "010-"에서 시작, 커서도 끝에 위치
    var phoneNumber by remember {
        mutableStateOf(
            TextFieldValue(
                text = "010-",
                selection = TextRange("010-".length)
            )
        )
    }

    // 관계 목록
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,               // RoundedCornerShape(20.dp)
        containerColor = DialogBackground, // Color(0xFFFFF2E3)
        title = { Text("사용자 추가", fontSize = 18.sp, color = Color.Black) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 이름
                Text("상대방 이름 (별명)", fontSize = 14.sp, color = Color.Black)
                TextField(
                    value = name,
                    onValueChange = { newValue -> name = newValue.take(15) },
                    placeholder = { Text("예: 남편") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),


                    )

                // 전화번호
                Text("전화번호", fontSize = 14.sp, color = Color.Black)
                TextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        // 숫자만 추출
                        val digits = newValue.text.filter { it.isDigit() }
                        // 최대 11자리 제한
                        val limitedDigits = digits.take(11)
                        // 자동 하이픈
                        val formatted = when {
                            limitedDigits.length <= 3 -> limitedDigits
                            limitedDigits.length <= 7 ->
                                "${limitedDigits.substring(0, 3)}-${limitedDigits.substring(3)}"

                            else ->
                                "${limitedDigits.substring(0, 3)}-${
                                    limitedDigits.substring(
                                        3,
                                        7
                                    )
                                }-${limitedDigits.substring(7)}"
                        }
                        phoneNumber = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    placeholder = { Text("예: 010-3764-9287") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // 관계
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
                        Text(
                            text = if (relationship.isEmpty()) "배우자" else relationship,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
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
                // "저장" 버튼
                Button(
                    onClick = {
                        val newContact = Contact(
                            name = name,
                            phoneNumber = phoneNumber.text,
                            relationship = relationship
                        )
                        onSave(newContact)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ConfirmButtonColor)
                ) {
                    Text("저장", color = Color.White)
                }
                // "취소" 버튼
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
}

// ─────────────────────────────────────────────────────────────────────────────
// 기존의 InfoCard, AddButton (변경 없음)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(color = Color(0xFFFFF4E6), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(width = 48.dp, height = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.add),
            contentDescription = "Add",
            tint = Color.Unspecified
        )
    }
}
