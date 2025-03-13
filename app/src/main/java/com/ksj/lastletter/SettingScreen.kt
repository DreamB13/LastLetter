package com.ksj.lastletter


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // Firebase에서 불러온 연락처 상태
    val contacts = remember { mutableStateListOf<DocumentContact>() }
    val coroutineScope = rememberCoroutineScope()
    // 편집 중인 연락처 상태 (편지 받을 사람 목록은 기존처럼 편집 다이얼로그로 수정 가능)
    var editingContact by remember { mutableStateOf<DocumentContact?>(null) }
    // 일일질문 받을 사람 선택 상태 (선택한 연락처의 id 목록)
    var selectedDailyQuestionContactIds by remember { mutableStateOf<List<String>>(emptyList()) }
    // 일일질문 받을 사람 선택 다이얼로그 표시 여부
    var showDailyQuestionSelectionDialog by remember { mutableStateOf(false) }

    // Firebase로부터 연락처 목록 로드
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val fetchedContacts = contactRepository.getContactsWithCoroutines()
                contacts.clear()
                contacts.addAll(fetchedContacts)
            } catch (e: Exception) {
                println("Error loading contacts: ${e.message}")
            }
        }
    }

    // 전체 배경색(연한 크림색 계열)
    val backgroundColor = Color(0xFFFFFBEF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "설정") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 첫 번째 박스: 일일질문 받을 사람 목록 (다이얼로그로 복수 선택)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color(0xFF9AD28A)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "일일질문 받을 사람 목록",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
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
                    val selectedNames = contacts.filter { selectedDailyQuestionContactIds.contains(it.id) }
                        .joinToString { it.contact.name }
                    Text(
                        text = if (selectedNames.isEmpty()) "선택 없음" else selectedNames,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 두 번째 박스: 편지 받을 사람 목록 (기존 편집 기능 유지)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(2.dp, Color(0xFFB8A0EA))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "편지 받을 사람 목록",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (contacts.isEmpty()) {
                        Text(text = "목록이 비어 있습니다.")
                    } else {
                        contacts.forEachIndexed { index, documentContact ->
                            // 각 항목을 Row로 구성하여 이름과 편집 버튼 표시
                            ContactRow(documentContact = documentContact) {
                                editingContact = documentContact
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 아래 설정 메뉴 리스트 (글자 크기 변경, 문의하기, 제안하기)
            Column(modifier = Modifier.fillMaxWidth()) {
                RowItem(
                    title = "글자 크기 변경",
                    onClick = { /* TODO: 글자 크기 변경 로직 */ }
                )
                Divider()

                RowItem(
                    title = "문의하기",
                    onClick = { /* TODO: 문의하기 로직 */ }
                )
                Divider()

                RowItem(
                    title = "제안하기",
                    onClick = { /* TODO: 제안하기 로직 */ }
                )
            }
        }
    }

    // 일일질문 받을 사람 선택 다이얼로그
    if (showDailyQuestionSelectionDialog) {
        DailyQuestionSelectionDialog(
            contacts = contacts,
            initialSelectedIds = selectedDailyQuestionContactIds,
            onDismiss = { showDailyQuestionSelectionDialog = false },
            onSave = { newSelectedIds ->
                selectedDailyQuestionContactIds = newSelectedIds
                showDailyQuestionSelectionDialog = false
            }
        )
    }

    // 편집 다이얼로그: 편지 받을 사람 편집은 기존과 동일하게 처리
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
                            // 로컬 contacts 리스트 업데이트
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
            }
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
    // 로컬 선택 상태 (복사본)
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
                            .clickable {
                                if (tempSelectedIds.contains(documentContact.id)) {
                                    tempSelectedIds.remove(documentContact.id)
                                } else {
                                    tempSelectedIds.add(documentContact.id)
                                }
                                // 변경된 리스트로 재할당해서 recomposition 강제
                                tempSelectedIds = tempSelectedIds.toMutableList()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // onClick은 null로 처리하여 Row의 clickable이 실행되게 함
                        androidx.compose.material3.RadioButton(
                            selected = tempSelectedIds.contains(documentContact.id),
                            onClick = null,
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF9AD28A)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = documentContact.contact.name,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
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
    onSave: (Contact) -> Unit
) {
    var name by remember { mutableStateOf(documentContact.contact.name) }
    var phoneNumber by remember { mutableStateOf(documentContact.contact.phoneNumber) }
    var relationship by remember { mutableStateOf(documentContact.contact.relationship) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("연락처 수정") },
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
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(Contact(name = name, phoneNumber = phoneNumber, relationship = relationship))
                }
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
}

@Composable
fun RowItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = Color.Black
        )
    }
}

