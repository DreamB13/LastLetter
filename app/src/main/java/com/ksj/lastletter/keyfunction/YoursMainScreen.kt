package com.ksj.lastletter.keyfunction

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ksj.lastletter.firebase.Contact
import com.ksj.lastletter.firebase.ContactRepository
import com.ksj.lastletter.firebase.DocumentContact
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoursMainScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    // Firestore 참조
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    val uid = currentUser?.uid

    // 연락처 목록
    val contacts = remember { mutableStateListOf<DocumentContact>() }

    // 사용자 추가 다이얼로그
    var showAddUserDialog by remember { mutableStateOf(false) }
    // 광고 팝업 다이얼로그
    var showAdDialog by remember { mutableStateOf(false) }

    // 새 연락처 입력 필드
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    val relationships = listOf("배우자", "자녀", "부모", "연인", "형제", "친구")

    // 남은 무료 추가 횟수 (Firebase에서 로드)
    var remainingAdds by remember { mutableStateOf(2) }

    // 연락처가 하나도 없을 경우 자동 팝업
    var showNoContactsDialog by remember { mutableStateOf(false) }

    // 최초 로딩 시 연락처 목록 & remainingAdds 불러오기
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
                        val dbRemaining = doc.getLong("remainingAdds") // Long?
                        if (dbRemaining != null) {
                            remainingAdds = dbRemaining.toInt()
                        } else {
                            // Firestore에 없는 경우 기본값 2로 저장
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
                    // 사용자 아이콘 + 남은 추가 횟수 표시 (예: x2, x0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = "편지 받을 사람",
                            tint = Color.Black
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

    // 사용자 추가 다이얼로그
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
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

                            // 남은 횟수 감소
                            remainingAdds--

                            // 입력 필드 초기화
                            name = ""
                            phoneNumber = ""
                            relationship = ""
                            showAddUserDialog = false

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
                }) {
                    Text("저장", color = Color.Black)
                }
            },
            dismissButton = {
                Button(onClick = {
                    showAddUserDialog = false
                }) {
                    Text("취소", color = Color.Black)
                }
            }
        )
    }

    // 광고 팝업 다이얼로그
    if (showAdDialog) {
        AlertDialog(
            onDismissRequest = { showAdDialog = false },
            title = { Text("광고 보기", color = Color.Black) },
            text = {
                Text(
                    text = "광고를 시청하시면 추가 사용자 등록이 가능합니다.",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 광고 시청했다고 가정 -> 남은 횟수 1 증가
                        remainingAdds++
                        showAdDialog = false
                        Toast.makeText(
                            null,
                            "광고 시청 완료! 남은 추가 횟수가 1 증가했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Firestore 업데이트
                        if (uid != null) {
                            coroutineScope.launch {
                                try {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(uid)
                                        .update("remainingAdds", remainingAdds)
                                        .await()
                                } catch (e: Exception) {
                                    println("Error updating remainingAdds: ${e.message}")
                                }
                            }
                        }
                    }
                ) {
                    Text("광고 시청하기", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { showAdDialog = false }) {
                    Text("취소", color = Color.Black)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
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
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
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
            relationships.forEach { rel ->
                DropdownMenuItem(
                    text = { Text(text = rel, color = Color.Black) },
                    onClick = {
                        onRelationshipSelected(rel)
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
            .size(width = 48.dp, height = 48.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFFFDCA8), shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "+", color = Color(0xFFFFDCA8))
    }
}
