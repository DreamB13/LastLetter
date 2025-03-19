package com.ksj.lastletter.keyfunction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch

@Composable
fun YoursMainScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }
    var showNoContactsDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")
    val contacts = remember { mutableStateListOf<DocumentContact>() }
    val coroutineScope = rememberCoroutineScope()

    // 연락처 목록 불러오기 (AppViewModel 등 캐시를 사용해도 되고, 여기서 직접 불러오도록 함)
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
            } catch (e: Exception) {
                println("Error loading contacts: ${e.message}")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "남은 너에게",
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {navController.navigate("recording/1/1")}) {
                Text("바로가기")
            }
            contacts.forEach { documentContact ->
                InfoCard(
                    text = documentContact.contact.name,
                    modifier = Modifier.clickable {
                        navController.navigate("yoursContext/${documentContact.id}")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AddButton(onClick = { showDialog = true })
            }
        }
    }

    // 연락처가 하나도 없을 경우 자동 팝업 (배경은 어둡게 처리됨)
    if (showNoContactsDialog) {
        AlertDialog(
            onDismissRequest = { /* 팝업 강제 유지: dismiss 안되도록 */ },
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
                        text = "※ 앱 이용 30일 후 확인 알람이 옵니다.\n\n※ 앱 이용 37일이 지나면 설정한 번호로 편지가 전송됩니다.\n\n※ 잘못된 번호 입력이나 내용으로 인한 피해는 책임지지 않습니다.",
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
            containerColor = Color(0xFFFFFFFF), // 팝업창 배경: 흰색
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "사용자 추가",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFFFE4C4), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("상대방 이름 (별명)") },
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
                    RelationshipDropdown(
                        relationships,
                        selectedRelationship = relationship,
                        onRelationshipSelected = { relationship = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        try {
                            val contactRepository = ContactRepository()
                            val newContact = Contact(name, phoneNumber, relationship)
                            contactRepository.addContact(newContact)
                            val updatedContacts = contactRepository.getContactsWithCoroutines()
                            contacts.clear()
                            contacts.addAll(updatedContacts)
                            showDialog = false
                            name = ""
                            phoneNumber = ""
                            relationship = ""
                        } catch (e: Exception) {
                            println("Error adding contact: ${e.message}")
                        }
                    }
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun RelationshipDropdown(
    relationships: List<String>,
    selectedRelationship: String,
    onRelationshipSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = selectedRelationship,
            onValueChange = {},
            readOnly = true,
            label = { Text("상대방과의 관계") },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFE4C4), shape = RoundedCornerShape(12.dp))
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFFFFF4E6))
        ) {
            relationships.forEach { relationship ->
                DropdownMenuItem(
                    text = { Text(text = relationship, color = Color.Black) },
                    onClick = {
                        onRelationshipSelected(relationship)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(color = Color(0xFFFFF4E6), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(width = 48.dp, height = 48.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "+", fontSize = 24.sp, color = Color(0xFFFFDCA8))
    }
}
