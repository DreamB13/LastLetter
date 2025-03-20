package com.ksj.lastletter.login

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneTermScreen(
    navController: NavController,
    preFilledPhone: String = ""
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // 전화번호 입력 상태
    var phoneNumberInput by remember { mutableStateOf(TextFieldValue("")) }

    // string.xml에 정의된 약관/개인정보 문구
    val tosText = stringResource(id = R.string.PRIVACY_TITLE).trim()
    val privacyText = stringResource(id = R.string.PRIVACY_CONTENT).trim()

    // preFilledPhone이 있을 경우 우선 사용, 없으면 phoneNumberInput.text 사용
    val effectivePhone = if (preFilledPhone.isNotEmpty()) preFilledPhone else phoneNumberInput.text
    // 숫자만 뽑아내서 길이가 11자리인지 확인(예: 01012345678)
    val isPhoneValid = effectivePhone.filter { it.isDigit() }.length == 11

    // 체크박스 상태
    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    var marketingChecked by remember { mutableStateOf(false) }
    var ageChecked by remember { mutableStateOf(false) }

    // 다이얼로그 표시 여부
    var showTosDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // 필수 항목(이용약관, 개인정보, 만14세)에 모두 체크되어야 "다음" 버튼 활성화
    val isAllRequiredChecked = termsChecked && privacyChecked && ageChecked
    val isNextEnabled = isPhoneValid && isAllRequiredChecked

    // 구글 로그인 런처
    val clientId = stringResource(id = R.string.default_web_client_id)
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            firestore.collection("users")
                                .document(uid)
                                .set(
                                    mapOf("phoneNumber" to formatPhoneNumber(TextFieldValue(effectivePhone)).text),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    navController.navigate("dailyQuestion") {
                                        popUpTo("phoneTerm") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "전화번호 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    navController.navigate("dailyQuestion") {
                                        popUpTo("phoneTerm") { inclusive = true }
                                    }
                                }
                        } else {
                            navController.navigate("dailyQuestion") {
                                popUpTo("phoneTerm") { inclusive = true }
                            }
                        }
                    } else {
                        Toast.makeText(context, "구글 로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "idToken이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "구글 로그인 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 메인 화면
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("약관 동의 및 전화번호 입력") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "전화번호를 입력해 주세요", fontSize = 18.sp)

            OutlinedTextField(
                value = if (preFilledPhone.isNotEmpty()) {
                    TextFieldValue(preFilledPhone)
                } else {
                    phoneNumberInput
                },
                onValueChange = { newValue ->
                    if (preFilledPhone.isEmpty()) {
                        phoneNumberInput = formatPhoneNumber(newValue)
                    }
                },
                label = { Text("휴대폰 번호") },
                placeholder = { Text("예: 010-1234-5678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = preFilledPhone.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )

            CheckItemWithContent(
                text = "이용약관 동의 (필수)",
                checked = termsChecked,
                onCheckedChange = { termsChecked = it },
                onContentClick = { showTosDialog = true }
            )
            CheckItemWithContent(
                text = "개인정보 수집, 이용 동의 (필수)",
                checked = privacyChecked,
                onCheckedChange = { privacyChecked = it },
                onContentClick = { showPrivacyDialog = true }
            )
            CheckItemWithContent(
                text = "마케팅 정보 수신 동의 (선택)",
                checked = marketingChecked,
                onCheckedChange = { marketingChecked = it },
                onContentClick = {
                    Toast.makeText(context, "마케팅 동의 내용(예시)", Toast.LENGTH_SHORT).show()
                }
            )
            CheckItem(
                text = "만 14세 이상입니다. (필수)",
                checked = ageChecked,
                onCheckedChange = { ageChecked = it }
            )

            Button(
                onClick = {
                    if (isNextEnabled) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            firestore.collection("users")
                                .document(uid)
                                .set(
                                    mapOf("phoneNumber" to formatPhoneNumber(TextFieldValue(effectivePhone)).text),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    navController.navigate("dailyQuestion") {
                                        popUpTo("phoneTerm") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "전화번호 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // 구글 로그인
                            googleLauncher.launch(
                                GoogleSignIn.getClient(
                                    context,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(clientId)
                                        .requestEmail()
                                        .build()
                                ).signInIntent
                            )
                        }
                    } else {
                        Toast.makeText(context, "모든 필수 항목을 체크해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isNextEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("다음")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // "이용 약관 동의" 커스텀 다이얼로그 (옆 폭 확대 & 가독성 개선)
    // ─────────────────────────────────────────────────────────────
    if (showTosDialog) {
        Dialog(onDismissRequest = { showTosDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()  // 화면 너비 95%
                    .fillMaxHeight(0.8f),  // 화면 높이 80%
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "이용 약관 동의",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        IconButton(onClick = { showTosDialog = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.Red
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val tosLines = tosText.lines()

                    // 스크롤 가능한 박스
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            tosLines.forEachIndexed { index, line ->
                                // 빈 줄이면 간격만 추가
                                if (line.isBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                } else {
                                    // 실제 내용 줄
                                    Text(
                                        text = line.trim(),
                                        fontSize = 16.sp,       // 조금 더 큰 폰트
                                        lineHeight = 26.sp,      // 줄 간격 넉넉히
                                        textAlign = TextAlign.Justify, // 양쪽 맞춤
                                        color = Color.Black
                                    )
                                    // 문단 간 여백
                                    if (index < tosLines.size - 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // "개인정보 처리방침" 커스텀 다이얼로그 (옆 폭 확대 & 가독성 개선)
    // ─────────────────────────────────────────────────────────────
    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "개인정보 처리방침",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        IconButton(onClick = { showPrivacyDialog = false }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = Color.Red
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val privacyLines = privacyText.lines()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            privacyLines.forEachIndexed { index, line ->
                                if (line.isBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                } else {
                                    Text(
                                        text = line.trim(),
                                        fontSize = 16.sp,       // 폰트 크기
                                        lineHeight = 26.sp,      // 줄 간격
                                        textAlign = TextAlign.Justify,
                                        color = Color.Black
                                    )
                                    if (index < privacyLines.size - 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 체크박스 + "내용 보기" 버튼
// ─────────────────────────────────────────────────────────────────
@Composable
fun CheckItemWithContent(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onContentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, modifier = Modifier.weight(1f))
        TextButton(onClick = onContentClick) {
            Text("내용 보기")
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 단순 체크박스 항목
// ─────────────────────────────────────────────────────────────────
@Composable
fun CheckItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

// ─────────────────────────────────────────────────────────────────
// 전화번호 자동 포맷팅 함수
// 예: "01012345678" -> "010-1234-5678"
// ─────────────────────────────────────────────────────────────────
fun formatPhoneNumber(input: TextFieldValue): TextFieldValue {
    val digits = input.text.filter { it.isDigit() }
    val limited = digits.take(11)

    val formatted = when {
        limited.length <= 3 -> limited
        limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
        else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
    }

    val digitsBefore = input.text.substring(0, input.selection.start)
        .filter { it.isDigit() }
        .length

    val newCursorPosition = when {
        digitsBefore <= 3 -> digitsBefore
        digitsBefore <= 7 -> digitsBefore + 1
        else -> digitsBefore + 2
    }.coerceAtMost(formatted.length)

    return TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
}
