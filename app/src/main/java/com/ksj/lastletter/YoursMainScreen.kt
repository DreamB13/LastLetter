package com.ksj.lastletter

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation.NavHostController
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch


@Composable
fun YoursMainScreen(navController: NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") } // 관계 상태 추가
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")
    val contacts = remember { mutableStateListOf<DocumentContact>() }
    val coroutineScope = rememberCoroutineScope()

    // 코루틴 스코프로 데이터 로드
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val contactRepository = ContactRepository()
                val fetchedContacts = contactRepository.getContactsWithCoroutines()
                contacts.clear()
                contacts.addAll(fetchedContacts)
                println("Load contacts: ${fetchedContacts.size}")
            } catch (e: Exception) {
                println("Error loading contacts: ${e.message}")
            }
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "남은 너에게",
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            contacts.forEach { documentContact ->
                InfoCard(
                    text = documentContact.contact.name,
                    modifier = Modifier.clickable {
                        println("Navigating to yoursContext/${documentContact.id}")
                        navController.navigate("yoursContext/${documentContact.id}")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AddButton (onClick = {
                    showDialog = true
                })
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
                                .background(
                                    Color(0xFFFFE4C4),
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                            // 코루틴 스코프로 데이터 저장
                            coroutineScope.launch {
                                try {
                                    val contactRepository = ContactRepository()
                                    val newContact = Contact(name, phoneNumber, relationship)
                                    contactRepository.addContact(newContact)

                                    val updatedContacts =
                                        contactRepository.getContactsWithCoroutines()
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
                    text = {  // 필수 text 파라미터 추가
                        Text(
                            text = relationship,
                            color = Color.Black
                        )
                    },
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
fun InfoCard(
    text: String,
    modifier:Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = Color(0xFFFFF4E6),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(width = 48.dp, height = 48.dp) // 라운드 직사각형 크기 설정
            .background(Color.White, shape = RoundedCornerShape(12.dp)) // 흰색 배경과 둥근 모서리 설정
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)), // 테두리 색상 설정
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            fontSize = 24.sp,
            color = Color(0xFFFFDCA8),
        )
    }
}