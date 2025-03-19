package com.ksj.lastletter.setting

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val contacts = remember { mutableStateListOf<DocumentContact>() }
    val coroutineScope = rememberCoroutineScope()
    var editingContact by remember { mutableStateOf<DocumentContact?>(null) }
    var selectedDailyQuestionContactIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDailyQuestionSelectionDialog by remember { mutableStateOf(false) }
    // 전화번호 변경, 회원 탈퇴 다이얼로그 표시 여부
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
                            }
                        }
                    }
                }
            }

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
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Settiingitem("글자 크기 변경") { navController.navigate("textSizeSetting") }
                Settiingitem("전화번호 변경") { showChangePhoneDialog = true }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("문의하기") { /* 문의하기 처리 */ }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("제안하기") { /* 제안하기 처리 */ }
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
                            Toast.makeText(context, "전화번호 변경 실패: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
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

@SuppressLint("MutableCollectionMutableState")
@Composable
fun DailyQuestionSelectionDialog(
    contacts: List<DocumentContact>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    // mutableStateListOf를 사용하여 리스트 변경 시 자동으로 recomposition이 일어나도록 함
    val tempSelectedIds =
        remember { mutableStateListOf<String>().apply { addAll(initialSelectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일일질문 받을 사람 선택") },
        text = {
            Column {
                contacts.forEach { documentContact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            // Row 전체를 클릭해도 토글되도록 설정
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
            Button(onClick = { onSave(tempSelectedIds.toList()) }) {
                Text("저장")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

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
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var relationshipExpanded by remember { mutableStateOf(false) }
    var newrelationship by remember { mutableStateOf("") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사용자 편집") },
        text = {
            Column {
                // 이름: 최대 15자까지 입력 가능
                TextField(
                    value = name,
                    onValueChange = { name = it.take(15) },
                    label = { Text("이름 (최대 15자)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 전화번호: "010-" 접두사를 사용하고, 이후 숫자 8자리를 입력받으며 4자리마다 하이픈(-)을 추가

                TextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        val prefix = "010-"
                        // newValue.text가 항상 prefix로 시작하도록 강제
                        val currentText =
                            if (newValue.text.startsWith(prefix)) newValue.text else prefix + newValue.text.filter { it.isDigit() }
                        // 접두사 이후의 사용자 입력 추출 (숫자만)
                        val userInput = currentText.substring(prefix.length)
                        val digits = userInput.filter { it.isDigit() }
                        // 최대 8자리까지만 허용
                        val limitedDigits = digits.take(8)
                        // 4자리 이상이면 중간에 하이픈 삽입 (예: "1234-5678")
                        val formattedDigits = if (limitedDigits.length > 4) {
                            "${limitedDigits.substring(0, 4)}-${limitedDigits.substring(4)}"
                        } else {
                            limitedDigits
                        }
                        // 최종 텍스트: 고정된 prefix + 포맷팅된 숫자
                        val newText = prefix + formattedDigits
                        // 커서 위치는 항상 prefix 이후로 고정 (사용자가 접두사 앞쪽으로 이동하지 못함)
                        val newCursorPosition = maxOf(newValue.selection.start, prefix.length)
                        phoneNumber =
                            TextFieldValue(text = newText, selection = TextRange(newCursorPosition))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 관계: 드롭다운 메뉴로 선택
                Text(text = "관계")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { relationshipExpanded = true }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = relationship.ifEmpty { "관계 선택" }, color = Color.Black)
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "드롭다운 열기",
                        tint = Color.Black
                    )
                }
                DropdownMenu(
                    expanded = relationshipExpanded,
                    onDismissRequest = { relationshipExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")
                    relationships.forEach { rel ->
                        DropdownMenuItem(
                            text = { Text(text = rel) },
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
            // 확인 버튼 구현
            Button(onClick = {
                val updatedContact = Contact(
                    name = name,
                    phoneNumber = phoneNumber.text,
                    relationship = relationship
                )
                onSave(updatedContact)
                Toast.makeText(context, "사용자가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) {
                Text("저장")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("삭제 확인") },
            text = { Text("정말로 삭제 하시겠습니까?\n저장 되어 있던 정보와 작성한 편지가 모두 삭제됩니다") },
            confirmButton = {
                Button(onClick = {
                    onDelete()
                    showDeleteConfirmation = false
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("삭제 취소")
                }
            }
        )
    }
}

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

// ChangePhoneDialog: 전화번호 변경 다이얼로그
@Composable
fun ChangePhoneDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var newPhoneNumber by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("전화번호 변경") },
        text = {
            Column {
                OutlinedTextField(
                    value = newPhoneNumber,
                    onValueChange = { input ->
                        // "010-" 접두사 고정
                        val prefix = "010-"
                        // 입력값이 "010-"로 시작하지 않으면 접두사 추가
                        if (!input.startsWith(prefix)) {
                            newPhoneNumber = prefix + input.filter { it.isDigit() }
                        }

                        val digits = input.filter { it.isDigit() }
                        newPhoneNumber = digits.take(8)
                        // 4자리 이상이면 하이픈(-) 삽입
                        if (newPhoneNumber.length > 4) {
                            newPhoneNumber =
                                "${
                                    newPhoneNumber.substring(
                                        0,
                                        4
                                    )
                                }-${newPhoneNumber.substring(4)}"
                        }
                    },
                    label = { Text("새 전화번호 (010 고정)") },
                    placeholder = { Text("예: 12345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = formatPhoneNumber(newPhoneNumber)
                    onSave(formatted)
                },
                enabled = newPhoneNumber.length == 9
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// DeleteAccountDialog: 회원 탈퇴 다이얼로그 (재인증 포함)
// DeleteAccountFlow를 호출하여 재인증 후 탈퇴 진행
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

// DeleteAccountDialog: 회원 탈퇴 확인 다이얼로그
@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원 탈퇴 확인") },
        text = { Text("정말로 회원 탈퇴하시겠습니까?\n탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("탈퇴")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// ReauthenticateDialog: 구글 계정으로 재인증 다이얼로그
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReauthenticateDialog(
    onDismiss: () -> Unit,
    onReauthenticated: () -> Unit
) {
    val context = LocalContext.current
    val clientId = stringResource(id = R.string.default_web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(clientId) // 실제 클라이언트 ID로 변경
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
                            Toast.makeText(
                                context,
                                "재인증 실패: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
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
        title = { Text("재인증 필요") },
        text = { Text("회원 탈퇴를 위해 구글 계정으로 재인증이 필요합니다.\n재로그인 해주세요.") },
        confirmButton = {
            Button(onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    reauthLauncher.launch(googleSignInClient.signInIntent)
                }
            }) {
                Text("재인증")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("취소") }
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
    // 추가 정보를 함께 저장
    val withdrawalData = mapOf(
        "withdrawalDate" to formattedDate,
        "userId" to user.uid,
        "email" to (user.email ?: "이메일 없음"),
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
                    Toast.makeText(
                        context,
                        "회원 탈퇴 실패: ${deleteTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "탈퇴 정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
