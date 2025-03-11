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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Preview
@Composable
fun YoursMainScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    val yours = remember { mutableStateListOf<Contact>() }

    LaunchedEffect(Unit) {
        try {
            val contactRepository = ContactRepository()
            val fetchedContacts = contactRepository.getContacts()
            yours.addAll(fetchedContacts)
        } catch (e: Exception) {
            println("Error loading contacts: ${e.message}")
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
            yours.forEach { contact ->
                InfoCard(text = contact.name)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AddButton {
                    showDialog = true
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("연락처 추가") },
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
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            // Firebase에 데이터 저장
                            try {
                                val contactRepository = ContactRepository()
                                contactRepository.addContact(Contact(name, phoneNumber))
                                yours.add(Contact(name, phoneNumber))
                                showDialog = false
                                name = ""
                                phoneNumber = ""
                            } catch (e: Exception) {
                                println("Error adding contact: ${e.message}")
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
fun InfoCard(text: String) {
    Box(
        modifier = Modifier
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