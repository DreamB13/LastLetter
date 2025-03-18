package com.ksj.lastletter.login

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.*
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider
import com.ksj.lastletter.R
import com.kakao.sdk.common.model.*
import com.kakao.sdk.user.UserApiClient
import java.util.concurrent.TimeUnit

@Composable
fun LoginScreen(
    navController: NavController,
    loginAction: () -> Unit,
    context: Context
) {
    val localContext = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // 이미 로그인되어 있으면 바로 "dailyQuestion" 화면으로 이동
    val currentUser = auth.currentUser
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("dailyQuestion") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    val clientId = stringResource(R.string.default_web_client_id)

    // 구글 로그인 런처
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navController.navigate("yoursMain")
                        } else {
                            Log.e("GoogleLogin", "Firebase sign in failed: ${task.exception?.message}")
                        }
                    }
            } else {
                Log.e("GoogleLogin", "idToken is null")
            }
        } catch (e: ApiException) {
            Log.w("GoogleLogin", "Google sign in failed", e)
        }
    }

    // 전화번호 로그인 팝업 표시 여부
    var showPhoneDialog by remember { mutableStateOf(false) }

    // 전체 배경(여기서는 MaterialTheme 배경을 사용하거나, 필요시 Box로 감쌀 수 있음)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단(이미지 + 타이틀) / 중단(버튼들) / 하단(고객센터/문의) 순으로 구성
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 영역
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                // 편지 이미지는 R.drawable.XXXX 로 변경하세요 (예: R.drawable.ic_letter)
                Image(
                    painter = painterResource(id = R.drawable.lastletter), // 편지 이미지를 여기에
                    contentDescription = "편지 이미지",
                    modifier = Modifier.size(340.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 중앙 영역: 구글/카카오/휴대폰 번호로 시작하기 버튼
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 구글 로그인 버튼
                Button(
                    onClick = {
                        val gso = GoogleSignInOptions
                            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(clientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                        // 기존에 로그인된 구글 계정 캐시 해제
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "Google로 시작하기",
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 카카오 로그인 버튼
                Button(
                    onClick = {
                        if (UserApiClient.instance.isKakaoTalkLoginAvailable(localContext)) {
                            UserApiClient.instance.loginWithKakaoTalk(localContext) { token, error ->
                                if (error != null) {
                                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                        Log.w("KakaoLogin", "KakaoTalk login cancelled")
                                        return@loginWithKakaoTalk
                                    }
                                    Log.e("KakaoLogin", "KakaoTalk login failed: ${error.message}")
                                    UserApiClient.instance.loginWithKakaoAccount(localContext) { tokenAlt, errorAlt ->
                                        if (errorAlt != null) {
                                            Log.e("KakaoLogin", "KakaoAccount login failed: ${errorAlt.message}")
                                        } else if (tokenAlt != null) {
                                            navController.navigate("yoursMain")
                                        }
                                    }
                                } else if (token != null) {
                                    navController.navigate("yoursMain")
                                }
                            }
                        } else {
                            UserApiClient.instance.loginWithKakaoAccount(localContext) { token, error ->
                                if (error != null) {
                                    Log.e("KakaoLogin", "KakaoAccount login failed: ${error.message}")
                                } else if (token != null) {
                                    navController.navigate("yoursMain")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFE812) // 카카오 톡 노란색
                    )
                ) {
                    Text(
                        text = "카카오로 시작하기",
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 휴대폰 번호 로그인 버튼
                Button(
                    onClick = {
                        showPhoneDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("휴대폰 번호로 시작하기")
                }
            }

            // 하단 영역: "고객센터/문의"
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "고객센터/문의",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "로그인, 회원가입시 개인정보 수집 및 이용에 동의한 것으로 간주합니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // 전화번호 로그인 팝업
    if (showPhoneDialog) {
        PhoneLoginDialog(
            auth = auth,
            onDismiss = { showPhoneDialog = false },
            onSuccess = {
                // 로그인 성공 시 이동
                navController.navigate("dailyQuestion") {
                    popUpTo("login") { inclusive = true }
                }
            }
        )
    }
}

/**
 * 전화번호 인증을 AlertDialog로 구현
 */
@Composable
fun PhoneLoginDialog(
    auth: FirebaseAuth,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val localContext = LocalContext.current

    // 인증 단계 상태
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }

    // 콜백
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onDismiss()
                    onSuccess()
                } else {
                    Log.e("PhoneAuth", "Auto verification sign-in failed: ${task.exception?.message}")
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("PhoneAuth", "Verification failed: ${e.message}")
            Toast.makeText(localContext, "인증 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(vid, token)
            verificationId = vid
            isCodeSent = true
            Toast.makeText(localContext, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("전화번호 로그인") },
        text = {
            Column {
                // 전화번호 입력
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("휴대폰 번호 (앞의 0 제외)") },
                    placeholder = { Text("예: 1012345678") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 인증번호 입력(코드 발송 후에만 표시)
                if (isCodeSent) {
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("인증코드 6자리") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            // 버튼 텍스트: isCodeSent 에 따라 변경
            val buttonText = if (!isCodeSent) "인증코드 받기" else "인증하기"

            Button(onClick = {
                if (!isCodeSent) {
                    // 인증코드 받기 로직
                    if (phoneNumber.isBlank()) {
                        Toast.makeText(localContext, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val fullPhoneNumber = if (phoneNumber.startsWith("0")) {
                        "+82${phoneNumber.drop(1)}"
                    } else {
                        "+82$phoneNumber"
                    }

                    val activity = localContext as? Activity
                    if (activity == null) {
                        Toast.makeText(localContext, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(fullPhoneNumber) // +82 붙인 번호
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)

                } else {
                    // 인증하기 로직
                    if (verificationCode.isBlank()) {
                        Toast.makeText(localContext, "인증코드를 입력해주세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val verId = verificationId
                    if (verId != null) {
                        val credential = PhoneAuthProvider.getCredential(verId, verificationCode)
                        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onDismiss()
                                onSuccess()
                            } else {
                                Log.e("PhoneAuth", "Sign-in failed: ${task.exception?.message}")
                                Toast.makeText(localContext, "인증 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }) {
                Text(buttonText)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
