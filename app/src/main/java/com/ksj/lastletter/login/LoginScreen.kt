package com.ksj.lastletter.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.R
import java.util.concurrent.TimeUnit

private val EditButtonColor = Color(0xFFF7AC44)    // 주황색 (편집)
// 확장 함수: Context에서 Activity 찾기
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Helper: 정규화된 전화번호("+821037649287")에서 국내번호의 뒷자리(예, "37649287") 추출
fun extractLocalPhoneNumber(normalized: String): String {
    var digits = normalized
    if (digits.startsWith("+82")) {
        digits = digits.substring(3)
    }
    if (!digits.startsWith("0")) {
        digits = "0$digits"
    }
    // 전화번호가 "010"으로 시작하고 전체 길이가 11자리라면, 뒤 10자리를 반환
    // 첫 3자리 뒤 4자리찍 - 으로 구분
    if (digits.startsWith("010") && digits.length == 11) {
        digits = digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7)
    }
    return digits

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    loginAction: () -> Unit,
    context: Context
) {
    val localContext = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    var showPhoneAuthDialog by remember { mutableStateOf(false) }

    // 이미 로그인되어 있다면 Firestore에서 사용자 문서 존재 여부를 확인 후 DailyQuestionScreen 또는 PhoneTermScreen으로 이동
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val uid = currentUser.uid
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("phoneNumber") != null) {
                        navController.navigate("dailyQuestion") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("phoneTerm/")  // 빈 파라미터로 호출
                    }
                }
                .addOnFailureListener {
                    navController.navigate("phoneTerm/")  // 빈 파라미터로 호출
                }
        }
    }

    Scaffold { innerPadding ->
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 영역: 로고 및 타이틀
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Image(
                        painter = painterResource(id = R.drawable.lastletter),
                        contentDescription = null,
                        modifier = Modifier.size(300.dp)
                    )
                }
                // 중앙 영역: 로그인 버튼들 (구글, 카카오, 휴대폰번호)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 구글 로그인 버튼: 바로 구글 로그인 플로우 실행
                    val googleLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        try {
                            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                                .getResult(ApiException::class.java)
                            val idToken = account?.idToken
                            if (idToken != null) {
                                val credential = GoogleAuthProvider.getCredential(idToken, null)
                                auth.signInWithCredential(credential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val uid = auth.currentUser?.uid
                                            if (uid != null) {
                                                firestore.collection("users").document(uid).get()
                                                    .addOnSuccessListener { document ->
                                                        if (document.exists() && document.getString("phoneNumber") != null) {
                                                            navController.navigate("dailyQuestion") {
                                                                popUpTo("login") { inclusive = true }
                                                            }
                                                        } else {
                                                            navController.navigate("phoneTerm/")  // 파라미터 없이 호출 시 빈 문자열 전달
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        navController.navigate("phoneTerm/")
                                                    }
                                            } else {
                                                navController.navigate("phoneTerm/")
                                            }
                                        } else {
                                            Toast.makeText(
                                                localContext,
                                                "구글 로그인 실패: ${task.exception?.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(localContext, "idToken이 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: ApiException) {
                            Toast.makeText(
                                localContext,
                                "구글 로그인 오류: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    Button(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(localContext.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "Google로 시작하기", color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // 카카오 로그인 버튼 (더미 구현)
                    Button(
                        onClick = {
                            navController.navigate("dailyQuestion") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE812))
                    ) {
                        Text(text = "카카오로 시작하기", color = Color.Black)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // 휴대폰 번호 로그인 버튼: 팝업 다이얼로그로 전화번호 인증 UI 띄우기
                    Button(
                        onClick = { showPhoneAuthDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
                    ) {
                        Text("휴대폰 번호로 시작하기")
                    }
                    // 팝업 다이얼로그로 전화번호 인증 UI 띄우기
                    if (showPhoneAuthDialog) {
                        Dialog(onDismissRequest = { showPhoneAuthDialog = false }) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                PhoneAuthDialogContent(
                                    navController = navController,
                                    auth = auth,
                                    onDismiss = { showPhoneAuthDialog = false }
                                )
                            }
                        }
                    }
                }
                // 하단 영역: 고객센터/문의
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "고객센터/문의",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthDialogContent(
    navController: NavController,
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 전화번호 상태: 이제 +82 접두어 없이 전체 번호(예: "01012349287")를 입력
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var normalizedPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCodeSent) {
            // 전화번호 입력 필드: 사용자가 전체 번호를 입력하면 자동 포맷팅됨
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    // 입력된 텍스트에서 숫자만 추출
                    val digits = newValue.text.filter { it.isDigit() }
                    // 최대 11자리까지 허용 (예: 01012349287)
                    val limited = digits.take(11)
                    // 자동 포맷팅:
                    // - 3자리 이하: 그대로
                    // - 4~7자리: "XXX-..." 형식
                    // - 8자리 이상: "XXX-XXXX-XXXX" 형식
                    val formatted = when {
                        limited.length <= 3 -> limited
                        limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
                        else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
                    }
                    // 입력값의 커서 전까지의 숫자 개수를 계산
                    val digitsBefore = newValue.text.substring(0, newValue.selection.start)
                        .filter { it.isDigit() }
                        .length
                    // 커서 위치: 3자리 이하 → 그대로, 4~7자리 → +1, 8자리 이상 → +2
                    val newCursorPosition = when {
                        digitsBefore <= 3 -> digitsBefore
                        digitsBefore <= 7 -> digitsBefore + 1
                        else -> digitsBefore + 2
                    }.coerceAtMost(formatted.length)
                    phoneNumber = TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
                },
                label = { Text("전화번호") },
                placeholder = { Text("예: 010-1234-5678") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // 정규화: 입력된 전화번호의 숫자만 남김 (예: "01012349287")
                    val digits = phoneNumber.text.filter { it.isDigit() }
                    normalizedPhone = "+82" + if (digits.startsWith("0")) digits.drop(1) else digits

                    val activity = context.findActivity()
                    if (activity == null) {
                        Toast.makeText(context, "Activity를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(normalizedPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                val localPhone = extractLocalPhoneNumber(normalizedPhone)
                                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onDismiss()
                                        navController.navigate("phoneTerm/$localPhone") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            override fun onVerificationFailed(e: FirebaseException) {
                                Toast.makeText(context, "인증 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            override fun onCodeSent(
                                verification: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                verificationId = verification
                                isCodeSent = true
                                Toast.makeText(context, "인증 코드가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
            ) {
                Text("인증 코드 받기", fontSize = 16.sp)
            }
        } else {
            // 인증 코드 입력란: 최대 6자리 숫자만 입력
            OutlinedTextField(
                value = code.filter { it.isDigit() }.take(6),
                onValueChange = { code = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("인증 코드 입력") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val credential = PhoneAuthProvider.getCredential(verificationId, code)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val localPhone = extractLocalPhoneNumber(normalizedPhone)
                            onDismiss()
                            navController.navigate("phoneTerm/$localPhone") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditButtonColor)
            ) {
                Text("로그인", fontSize = 16.sp)
            }
        }
    }
}