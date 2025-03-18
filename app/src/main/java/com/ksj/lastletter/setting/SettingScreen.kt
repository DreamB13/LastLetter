package com.ksj.lastletter.setting

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
                Settiingitem("회원 탈퇴") { showDeleteAccountDialog = true }
                Spacer(modifier = Modifier.height(8.dp))
                Settiingitem("로그아웃") {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
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

@Composable
fun DailyQuestionSelectionDialog(
    contacts: List<DocumentContact>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var tempSelectedIds by remember { mutableStateOf(initialSelectedIds.toMutableList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일일질문 받을 사람 선택") },
        text = {
            Column {
                contacts.forEach { documentContact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelectedIds.contains(documentContact.id),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (!tempSelectedIds.contains(documentContact.id)) {
                                        tempSelectedIds.add(documentContact.id)
                                    }
                                } else {
                                    tempSelectedIds.remove(documentContact.id)
                                }
                                tempSelectedIds = tempSelectedIds.toMutableList()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF9AD28A))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = documentContact.contact.name, color = Color.Black)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempSelectedIds) }) {
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
            fontSize = 16.sp,
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
    var name by remember { mutableStateOf(documentContact.contact.name) }
    var phoneNumber by remember { mutableStateOf(documentContact.contact.phoneNumber) }
    var relationship by remember { mutableStateOf(documentContact.contact.relationship) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("사용자 편집") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("전화번호") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("관계") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { showDeleteConfirmation = true }) {
                    Text("삭제")
                }
                Button(onClick = onDismiss) {
                    Text("취소")
                }
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onSave(Contact(name, phoneNumber, relationship))
                    }
                }) {
                    Text("저장")
                }
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
                        val digits = input.filter { it.isDigit() }
                        newPhoneNumber = digits.take(8)
                    },
                    label = { Text("새 전화번호 (010 고정)") },
                    placeholder = { Text("예: 12345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = "전화번호는 010-xxxx-xxxx 형식으로 저장됩니다.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = formatPhoneNumber(newPhoneNumber)
                    onSave(formatted)
                },
                enabled = newPhoneNumber.length == 8
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
                    Toast.makeText(context, "회원 탈퇴 실패: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "탈퇴 정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}