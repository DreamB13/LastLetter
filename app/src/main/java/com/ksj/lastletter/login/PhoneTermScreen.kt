package com.ksj.lastletter.login

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // 전화번호 상태: TextFieldValue로 관리 (preFilledPhone이 없을 때)
    var phoneNumberInput by remember { mutableStateOf(TextFieldValue("")) }

    val tosText = stringResource(id = R.string.PRIVACY_TITLE)
    val privacyText = stringResource(id = R.string.PRIVACY_CONTENT)

    // effectivePhone: preFilledPhone이 있으면 그 값을 사용, 없으면 phoneNumberInput.text를 사용
    val effectivePhone = if (preFilledPhone.isNotEmpty()) preFilledPhone else phoneNumberInput.text
    // 유효성 검사: effectivePhone에서 숫자만 추출한 길이가 11이어야 함
    val isPhoneValid = effectivePhone.filter { it.isDigit() }.length == 11

    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    var marketingChecked by remember { mutableStateOf(false) }
    var ageChecked by remember { mutableStateOf(false) }

    var showTosDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val isAllRequiredChecked = termsChecked && privacyChecked && ageChecked
    val isNextEnabled = isPhoneValid && isAllRequiredChecked

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
                                    // preFilledPhone이 있으면 그대로, 없으면 포맷팅된 phoneNumberInput의 text 사용
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
                        // newValue를 포맷팅 함수에 넣어 업데이트 (커서 위치도 자동 보정됨)
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

    if (showTosDialog) {
        AlertDialog(
            onDismissRequest = { showTosDialog = false },
            title = { Text("LastLetter 앱 이용 약관") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = tosText)
                }
            },
            confirmButton = {
                Button(onClick = { showTosDialog = false }) {
                    Text("확인")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("개인정보 처리방침") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = privacyText)
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

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

fun formatPhoneNumber(input: TextFieldValue): TextFieldValue {
    // 입력된 텍스트에서 숫자만 추출하고 최대 11자리까지 제한
    val digits = input.text.filter { it.isDigit() }
    val limited = digits.take(11)

    // 자동 포맷팅:
    // - 3자리 이하: 그대로
    // - 4자리부터 7자리: "XXX-..." 형식
    // - 8자리 이상: "XXX-XXXX-XXXX" 형식
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

    return TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
}
