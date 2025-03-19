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
import androidx.compose.ui.text.input.KeyboardType
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
    // 전화번호가 "010"으로 시작하고 전체 길이가 11자리라면, 뒤 8자리를 반환
    if (digits.startsWith("010") && digits.length >= 11) {
        return digits.substring(3, 11)
    }
    return ""
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
                        colors = ButtonDefaults.buttonColors()
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
    // 전화번호는 고정된 "+82"와 뒤의 사용자가 입력하는 부분으로 구성
    var phoneNumber by remember { mutableStateOf("+82") }
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
            // 고정된 "+82"와 뒤에 editable한 번호 입력란을 Row로 구성
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "+82",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                OutlinedTextField(
                    value = phoneNumber.removePrefix("+82"),
                    onValueChange = {
                        phoneNumber = "+82" + it
                    },
                    label = { Text("전화번호 (0 제외)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // 전화번호 정규화: +82 뒤의 숫자만 남기고, 선행 0은 제거 (Firebase에 전달할 형식은 "+82" + 나머지)
                    val digitsPart = phoneNumber.removePrefix("+82").filter { it.isDigit() }
                    val cleanDigits = if (digitsPart.startsWith("0")) digitsPart.substring(1) else digitsPart
                    normalizedPhone = "+82" + cleanDigits

                    // Activity 추출
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
                                auth.signInWithCredential(credential)
                                    .addOnCompleteListener { task ->
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("로그인", fontSize = 16.sp)
            }
        }
    }
}
