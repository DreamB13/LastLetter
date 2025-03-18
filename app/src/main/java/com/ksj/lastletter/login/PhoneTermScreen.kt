package com.ksj.lastletter.login

import android.util.Log
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.KeyboardType
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
fun PhoneTermScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // 리소스에 저장된 약관 및 개인정보 처리방침 내용
    val tosText = stringResource(id = R.string.PRIVACY_TITLE)
    val privacyText = stringResource(id = R.string.PRIVACY_CONTENT)

    // 전화번호 입력 상태 (최대 8자리)
    var phoneNumberInput by remember { mutableStateOf("") }
    val isPhoneValid = phoneNumberInput.length == 8

    // 약관 체크 상태
    var termsChecked by remember { mutableStateOf(false) }       // 이용약관 (필수)
    var privacyChecked by remember { mutableStateOf(false) }     // 개인정보 처리방침 (필수)
    var marketingChecked by remember { mutableStateOf(false) }   // 마케팅 (선택)
    var ageChecked by remember { mutableStateOf(false) }         // 만 14세 이상 (필수)

    // "내용 보기" 팝업 상태
    var showTosDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // 필수 항목 체크 여부
    val isAllRequiredChecked = termsChecked && privacyChecked && ageChecked
    // "다음" 버튼 활성화 조건
    val isNextEnabled = isPhoneValid && isAllRequiredChecked

    // 구글 로그인 관련 런처
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
                        // 로그인 성공 후 Firestore에 전화번호 저장 후 DailyQuestionScreen으로 이동
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            firestore.collection("users")
                                .document(uid)
                                .set(
                                    mapOf("phoneNumber" to formatPhoneNumber(phoneNumberInput)),
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
                value = formatPhoneNumber(phoneNumberInput),
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }
                    val phonePart = if (digits.startsWith("010")) digits.drop(3) else digits
                    phoneNumberInput = phonePart.take(8)
                },
                label = { Text("휴대폰 번호") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
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
                        // 전화번호를 Firestore에 저장하고 DailyQuestionScreen으로 이동
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            firestore.collection("users")
                                .document(uid)
                                .set(
                                    mapOf("phoneNumber" to formatPhoneNumber(phoneNumberInput)),
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

/**
 * "내용 보기"가 포함된 체크 항목
 */
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

/**
 * "내용 보기"가 없는 일반 체크 항목
 */
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

/**
 * 입력된 전화번호(최대 8자리)를 "010-xxxx-xxxx" 형태로 반환
 */
fun formatPhoneNumber(input: String): String {
    return when {
        input.length <= 4 -> "010-$input"
        else -> {
            val first4 = input.take(4)
            val last4 = input.drop(4)
            "010-$first4-$last4"
        }
    }
}
