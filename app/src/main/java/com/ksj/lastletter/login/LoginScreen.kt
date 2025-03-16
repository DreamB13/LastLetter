package com.ksj.lastletter.login

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ksj.lastletter.R
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

@Composable
fun LoginScreen(
    navController: NavController,
    loginAction: () -> Unit,
    context: Context
) {
    val localContext = LocalContext.current

    // 이미 로그인되어 있으면 바로 "dailyQuestion" 화면으로 이동
    val currentUser = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("dailyQuestion") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    val clientId = stringResource(R.string.default_web_client_id)

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.android_light_rd_si),
            contentDescription = "Google Login",
            modifier = Modifier
                .clickable {
                    // GoogleSignInClient 생성 후 signOut() 호출하여 캐시된 계정 해제
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(clientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
                .height(50.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.kakao_login_medium_narrow),
            contentDescription = "Kakao Login",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable {
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
                }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("dailyQuestion") },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "게스트 로그인")
        }
    }
}
