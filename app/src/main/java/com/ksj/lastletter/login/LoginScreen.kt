package com.ksj.lastletter.login

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
fun LoginScreen(
    navController: NavController,
    loginAction: () -> Unit,
    context: Context
) {
    val localContext = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

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
                        navController.navigate("phoneTerm")
                    }
                }
                .addOnFailureListener {
                    // 실패 시 그냥 PhoneTermScreen으로 이동
                    navController.navigate("phoneTerm")
                }
        }
    }

    Scaffold{ innerPadding ->
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
                                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // 로그인 성공 후, Firestore에서 사용자 문서 확인 후 PhoneTermScreen 또는 DailyQuestionScreen으로 이동
                                        val uid = auth.currentUser?.uid
                                        if (uid != null) {
                                            firestore.collection("users").document(uid).get()
                                                .addOnSuccessListener { document ->
                                                    if (document.exists() && document.getString("phoneNumber") != null) {
                                                        navController.navigate("dailyQuestion") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        navController.navigate("phoneTerm")
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    navController.navigate("phoneTerm")
                                                }
                                        } else {
                                            navController.navigate("phoneTerm")
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
                            Toast.makeText(localContext, "구글 로그인 오류: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            // 더미 구현: 바로 DailyQuestionScreen으로 이동
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
                    // 휴대폰 번호 로그인 버튼: 별도 PhoneTermScreen으로 이동
                    Button(
                        onClick = { navController.navigate("phoneTerm") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("휴대폰 번호로 시작하기")
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
