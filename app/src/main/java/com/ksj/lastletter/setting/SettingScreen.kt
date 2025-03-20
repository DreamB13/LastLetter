package com.ksj.lastletter.setting

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.R
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import com.ksj.lastletter.login.formatPhoneNumber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//────────────────────────────────────────────────────────────────────────────
// 공통 다이얼로그 스타일
//────────────────────────────────────────────────────────────────────────────
private val DialogShape = RoundedCornerShape(20.dp)
private val DialogBackground = Color(0xFFFFF2E3) // 연한 살구색 (예시)
private val ConfirmButtonColor = Color(0xFFB2A7FF) // 보라색 (확인)
private val DangerButtonColor = Color(0xFFFFB2A7)  // 핑크 (위험/삭제)
private val EditButtonColor = Color(0xFFF7AC44)    // 주황색 (편집)
private val CancelButtonColor = Color.LightGray      // 취소

//────────────────────────────────────────────────────────────────────────────
// SettingsScreen (기본 화면)
//────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val contacts = remember { mutableStateListOf<DocumentContact>() }
    val coroutineScope = rememberCoroutineScope()
    var editingContact by remember { mutableStateOf<DocumentContact?>(null) }
    var selectedDailyQuestionContactIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDailyQuestionSelectionDialog by remember { mutableStateOf(false) }
    var showChangePhoneDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val dailyQuestionContactIds = contactRepository.getDailyQuestionContactIds()
                val fetchedContacts = contactRepository.getContactsWithCoroutines()
                contacts.clear()
                contacts.addAll(fetchedContacts)
                selectedDailyQuestionContactIds = dailyQuestionContactIds
            } catch (e: Exception) {
                println("Error loading contacts: ${e.message}")
            }
        }
    }

    val backgroundColor = Color(0xFFFDFBF4)

    Scaffold(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = backgroundColor),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                title = { Text(text = "설정", color = Color.Black) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(backgroundColor)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 일일질문 받을 사람 목록 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, Color(0xFFE2FFDE)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2FFDE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "일일질문 받을 사람 목록",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDailyQuestionSelectionDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "선택 편집",
                                tint = Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val selectedNames =
                        contacts.filter { selectedDailyQuestionContactIds.contains(it.id) }
                            .joinToString { it.contact.name }
                    if (selectedNames.isEmpty()) {
                        Text(text = "선택된 사람이 없습니다.")
                    } else {
                        contacts.forEach { documentContact ->
                            if (selectedDailyQuestionContactIds.contains(documentContact.id)) {
                                Text(text = documentContact.contact.name, color = Color.Black)
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.Black,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // 편지 받을 사람 목록 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, Color(0xFFE9EAFF)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EAFF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "편지 받을 사람 목록")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (contacts.isEmpty()) {
                        Text(text = "목록이 비어 있습니다.")
                    } else {
                        contacts.forEachIndexed { _, documentContact ->
                            ContactRow(documentContact = documentContact) {
                                editingContact = documentContact
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.Black,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 설정 메뉴 항목들
            Column(modifier = Modifier.fillMaxWidth()) {
                Settiingitem("글자 크기 변경") { navController.navigate("textSizeSetting") }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("전화번호 변경") { showChangePhoneDialog = true }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("문의하기") {
                    Toast.makeText(context, "문의하기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("제안하기") {
                    Toast.makeText(context, "제안하기 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("로그아웃") {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("회원 탈퇴") { showDeleteAccountDialog = true }
            }
        }
    }

    if (showDailyQuestionSelectionDialog) {
        DailyQuestionSelectionDialog(
            contacts = contacts,
            initialSelectedIds = selectedDailyQuestionContactIds,
            onDismiss = { showDailyQuestionSelectionDialog = false },
            onSave = { newSelectedIds ->
                selectedDailyQuestionContactIds = newSelectedIds
                showDailyQuestionSelectionDialog = false
                coroutineScope.launch {
                    try {
                        val contactRepository = ContactRepository()
                        contactRepository.updateDailyQuestionContacts(newSelectedIds)
                    } catch (e: Exception) {
                        println("Error saving selected contacts: ${e.message}")
                    }
                }
            }
        )
    }

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
                            val index = contacts.indexOfFirst { it.id == docContact.id }
                            if (index != -1) {
                                contacts[index] = DocumentContact(docContact.id, updatedContact)
                            }
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
                        contacts.removeIf { it.id == docContact.id }
                    } catch (e: Exception) {
                        println("삭제 실패: ${e.message}")
                    } finally {
                        editingContact = null
                    }
                }
            }
        )
    }

    if (showChangePhoneDialog) {
        ChangePhoneDialog(
            onDismiss = { showChangePhoneDialog = false },
            onSave = { newPhone ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .update("phoneNumber", newPhone)
                        .addOnSuccessListener {
                            Toast.makeText(context, "전화번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "전화번호 변경 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountFlow(
            navController = navController,
            onDismiss = { showDeleteAccountDialog = false }
        )
    }
}

//────────────────────────────────────────────────────────────────────────────
// ContactRow
//────────────────────────────────────────────────────────────────────────────
@Composable
fun ContactRow(documentContact: DocumentContact, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = documentContact.contact.name,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "편집",
                tint = Color.Black
            )
        }
    }
}

//────────────────────────────────────────────────────────────────────────────
// DailyQuestionSelectionDialog: 일일질문 받을 사람 선택 다이얼로그
//────────────────────────────────────────────────────────────────────────────
@SuppressLint("MutableCollectionMutableState")
@Composable
fun DailyQuestionSelectionDialog(
    contacts: List<DocumentContact>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val tempSelectedIds = remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = DialogBackground,
        title = { Text("일일질문 받을 사람 선택", fontSize = 18.sp, color = Color.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                contacts.forEach { documentContact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (tempSelectedIds.contains(documentContact.id)) {
                                    tempSelectedIds.remove(documentContact.id)
                                } else {
                                    tempSelectedIds.add(documentContact.id)
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelectedIds.contains(documentContact.id),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    tempSelectedIds.add(documentContact.id)
                                } else {
                                    tempSelectedIds.remove(documentContact.id)
                                }
                            },
                            modifier = Modifier.size(24.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF9AD28A),
                                checkmarkColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = documentContact.contact.name, color = Color.Black)
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
                    onClick = { onSave(tempSelectedIds.toList()) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
                ) {
                    Text("저장", color = Color.White)
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
}

//────────────────────────────────────────────────────────────────────────────
// EditContactDialog: 사용자 편집 다이얼로그
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

    // 삭제 확인 다이얼로그
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

    // 사용자 편집 메인 다이얼로그
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
                    placeholder = { Text("남편") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )

                )

                Text("전화번호", fontSize = 14.sp, color = Color.Black)
                TextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        // 입력된 문자에서 숫자만 추출
                        val digits = newValue.text.filter { it.isDigit() }
                        // 최대 11자리까지 허용 (모바일 번호: 010xxxx xxxx → 11자리)
                        val limitedDigits = digits.take(11)
                        // 자동 포맷팅: 첫 3자리, 그 다음 4자리, 나머지 4자리
                        val formatted = when {
                            limitedDigits.length <= 3 -> limitedDigits
                            limitedDigits.length <= 7 -> "${limitedDigits.substring(0, 3)}-${limitedDigits.substring(3)}"
                            else -> "${limitedDigits.substring(0, 3)}-${limitedDigits.substring(3, 7)}-${limitedDigits.substring(7)}"
                        }
                        // 커서 위치는 끝으로 이동
                        phoneNumber = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    placeholder = { Text("예: 010-3764-9287") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
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
                            imageVector = Icons.Default.KeyboardArrowRight,
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
                    colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
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
}

//────────────────────────────────────────────────────────────────────────────
// ChangePhoneDialog: 전화번호 변경 다이얼로그
//────────────────────────────────────────────────────────────────────────────
@Composable
fun ChangePhoneDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    // TextFieldValue로 관리하여 커서 위치를 제어합니다.
    var newPhoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = DialogBackground,
        title = { Text("전화번호 변경", fontSize = 18.sp, color = Color.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = newPhoneNumber,
                    onValueChange = { input ->
                        // 입력값에서 숫자만 추출
                        val digits = input.text.filter { it.isDigit() }
                        // 최대 11자리까지 허용 (예: 01012349287)
                        val limited = digits.take(11)
                        // 자동 포맷팅:
                        //  - 3자리 이하: 그대로
                        //  - 4~7자리: 첫 3자리 후 하이픈 삽입
                        //  - 8자리 이상: "XXX-XXXX-XXXX" 형식
                        val formatted = when {
                            limited.length <= 3 -> limited
                            limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
                            else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
                        }
                        // 입력값의 커서 전까지의 텍스트에서 숫자 개수를 계산
                        val digitsBefore = input.text.substring(0, input.selection.start)
                            .filter { it.isDigit() }
                            .length
                        // 새 문자열에서 커서 위치:
                        // - 3자리 이하: 그대로
                        // - 4~7자리: +1 (첫 하이픈)
                        // - 8자리 이상: +2 (두 하이픈)
                        val newCursorPosition = when {
                            digitsBefore <= 3 -> digitsBefore
                            digitsBefore <= 7 -> digitsBefore + 1
                            else -> digitsBefore + 2
                        }.coerceAtMost(formatted.length)
                        newPhoneNumber = TextFieldValue(
                            text = formatted,
                            selection = TextRange(newCursorPosition)
                        )
                    },
                    label = { Text("새 전화번호") },
                    placeholder = { Text("예: 010-1234-5678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        onSave(newPhoneNumber.text)
                        Toast.makeText(context, "전화번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    enabled = newPhoneNumber.text.length >= 13,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
                ) {
                    Text("저장", color = Color.White)
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
}

//────────────────────────────────────────────────────────────────────────────
// DeleteAccountFlow: 회원 탈퇴 및 재인증 다이얼로그
//────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountFlow(
    navController: NavController,
    onDismiss: () -> Unit
) {
    var showReauthDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    if (showReauthDialog) {
        ReauthenticateDialog(
            onDismiss = { showReauthDialog = false },
            onReauthenticated = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    performAccountDeletion(user, navController, context)
                }
            }
        )
    }
    DeleteAccountDialog(
        onDismiss = onDismiss,
        onConfirm = { showReauthDialog = true }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = DialogBackground,
        title = { Text("회원 탈퇴 확인", fontSize = 18.sp, color = Color.Black) },
        text = { Text("정말로 회원 탈퇴하시겠습니까?\n탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.", fontSize = 14.sp, color = Color.Black) },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
                ) {
                    Text("탈퇴", color = Color.White)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReauthenticateDialog(
    onDismiss: () -> Unit,
    onReauthenticated: () -> Unit
) {
    val context = LocalContext.current
    val clientId = stringResource(id = R.string.default_web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)
    val reauthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null && FirebaseAuth.getInstance().currentUser != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().currentUser!!
                    .reauthenticate(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onReauthenticated()
                        } else {
                            Toast.makeText(context, "재인증 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "ID 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "재인증 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = DialogBackground,
        title = { Text("재인증 필요", fontSize = 18.sp, color = Color.Black) },
        text = { Text("회원 탈퇴를 위해 구글 계정으로 재인증이 필요합니다.\n재로그인 해주세요.", fontSize = 14.sp, color = Color.Black) },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            reauthLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
                ) {
                    Text("재인증", color = Color.White)
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
}

private fun performAccountDeletion(
    user: FirebaseUser,
    navController: NavController,
    context: Context
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = sdf.format(Date())
    val withdrawalData = mapOf(
        "withdrawalDate" to formattedDate,
        "userId" to user.uid,
        "email" to (user.email ?: "이메일 없음")
    )
    FirebaseFirestore.getInstance().collection("withdrawals")
        .document(user.uid)
        .set(withdrawalData)
        .addOnSuccessListener {
            user.delete().addOnCompleteListener { deleteTask ->
                if (deleteTask.isSuccessful) {
                    Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "회원 탈퇴 실패: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "탈퇴 정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

//────────────────────────────────────────────────────────────────────────────
// Settiingitem: 설정 메뉴 항목 UI (원래 그대로 유지)
//────────────────────────────────────────────────────────────────────────────
@Composable
fun Settiingitem(text: String, click: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF5E9), shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
            .clickable { click() }
    ) {
        Text(text = text)
        Icon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 8.dp),
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = text,
            tint = Color.Black
        )
    }
}
