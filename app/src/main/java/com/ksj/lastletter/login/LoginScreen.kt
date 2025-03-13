package com.ksj.lastletter.login


import android.app.Application
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.google.gson.GsonBuilder
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.ksj.lastletter.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Kakao Sdk 초기화
        KakaoSdk.init(this, "KAKAO_API_KEY")
    }
}


class RetrofitInstance {
    companion object{
        val BASE_URL = "https://sample.com/oauth"
        fun getRetrofitInstance() : Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
        }

        fun init(authlogin: GlobalApplication) {
            val retrofit = getRetrofitInstance()

        }
    }
}




@Composable
fun LoginScreen(navController: NavController, loginAction: () -> Unit, context: Context) {
    // LocalContext가 이미 파라미터로 넘어오지만, 여기서 다시 가져오는 경우가 있으므로 주의
    val localContext = LocalContext.current
    val token = stringResource(R.string.default_web_client_id)

    // Google Sign-In 결과를 처리하는 런처
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 로그인 성공 시 "yoursMain"으로 이동
                        navController.navigate("yoursMain")
                    }
                }
        } catch (e: ApiException) {
            Log.w("TAG", "GoogleSign in Failed", e)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        // 구글 로그인 버튼
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background), // 구글 로그인용 로고 리소스
            contentDescription = "Logo",
            modifier = Modifier.clickable {
                val gso = GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(token)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(localContext, gso)
                launcher.launch(googleSignInClient.signInIntent)
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        // 카카오 로그인 버튼 (이미지)
        Image(
            painter = painterResource(id = R.drawable.kakao_login_medium_narrow), // 카카오 로그인용 로고 리소스
            contentDescription = "Kakao Login",
            modifier = Modifier
                .size(120.dp)
                .clickable {
                    // 카카오톡 앱을 통한 로그인 시도
                    if (UserApiClient.instance.isKakaoTalkLoginAvailable(localContext)) {
                        UserApiClient.instance.loginWithKakaoTalk(localContext) { token, error ->
                            if (error != null) {
                                // 사용자가 로그인 취소한 경우 아무 작업도 하지 않음
                                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                    return@loginWithKakaoTalk
                                }
                                // 카카오톡 로그인 실패 시 카카오계정으로 로그인 시도
                                UserApiClient.instance.loginWithKakaoAccount(localContext) { token, error ->
                                    if (error != null) {
                                        Log.e("KakaoLogin", "Kakao account login failed: ${error.message}")
                                    } else if (token != null) {
                                        navController.navigate("yoursMain")
                                    }
                                }
                            } else if (token != null) {
                                navController.navigate("yoursMain")
                            }
                        }
                    } else {
                        // 카카오톡이 설치되어 있지 않으면 카카오계정으로 로그인 시도
                        UserApiClient.instance.loginWithKakaoAccount(localContext) { token, error ->
                            if (error != null) {
                                Log.e("KakaoLogin", "Kakao account login failed: ${error.message}")
                            } else if (token != null) {
                                navController.navigate("yoursMain")
                            }
                        }
                    }
                }
        )
        // 게스트 로그인 버튼
        Button(onClick = {
            navController.navigate("Dayquestion")
        }) {
            Text(text = "게스트 로그인")
        }
    }
}



