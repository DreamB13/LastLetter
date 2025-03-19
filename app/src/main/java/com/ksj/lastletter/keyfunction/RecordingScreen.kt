package com.ksj.lastletter.keyfunction

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ksj.lastletter.BuildConfig
import com.ksj.lastletter.FastAPI.EmotionRequest
import com.ksj.lastletter.FastAPI.RetrofitClient
import com.ksj.lastletter.FastAPI.RetrofitInstance2
import com.ksj.lastletter.FastAPI.TextRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

// 녹음 상태 관리를 위한 열거형
enum class RecordingState {
    NOT_STARTED, // 녹음 시작 전
    RECORDING,   // 녹음 중
    PAUSED,      // 일시정지 상태
    STOPPED,     // 정지 상태
    PLAYING,     // 재생 중
    CONVERTING   // STT 변환 중
}

@Composable
fun RecordingScreen(navController: NavController, contactName: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }

    // 상태 관리 변수들
    var recordingState by remember { mutableStateOf(RecordingState.NOT_STARTED) }
    var timerSeconds by remember { mutableFloatStateOf(0.0f) }
    var formattedTime by remember { mutableStateOf("00:00.0") }
    var recognizedText by remember { mutableStateOf("") } // STT 결과 저장 변수

    // 파형 데이터
    var waveformData by remember { mutableStateOf(List(50) { 0.05f }) }

    // 미디어 리소스 관리 - MediaRecorder에서 AudioRecord로 변경
    var audioRecorder by remember { mutableStateOf<AudioRecord?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 코루틴 작업 관리
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var waveformJob by remember { mutableStateOf<Job?>(null) }

    // Google STT API Key
    val googleAPIKey = try {
        BuildConfig.GOOGLE_API_KEY
    } catch (e: Exception) {
        Log.e("RecordingScreen", "API 키를 찾을 수 없습니다. local.properties에 GOOGLE_API_KEY를 추가하세요.")
        ""
    }

    var isConvertingSTT by remember { mutableStateOf(false) }

    // 오디오 파일 경로 (WAV 형식으로 변경)
    val audioFilePath = remember { "${context.cacheDir.absolutePath}/recorded_audio.wav" }

    // 저장 버튼 변수
    var showSaveButton by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // 리소스 정리를 위한 DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            // 타이머 및 파형 애니메이션 중지
            timerJob?.cancel()
            waveformJob?.cancel()

            // 미디어 리소스 해제
            try {
                if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
                    stopRecording(audioRecorder)
                }

                audioRecorder = null

                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
                mediaPlayer = null
            } catch (e: Exception) {
                // 오류 무시
            }
        }
    }

    // 마이크 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording(context, audioFilePath,
                onRecorderPrepared = { recorder ->
                    audioRecorder = recorder
                    recordingState = RecordingState.RECORDING

                    // 타이머 시작
                    timerJob = startTimer(coroutineScope) { newTime ->
                        timerSeconds = newTime
                        formattedTime = formatTime(newTime)
                    }

                    // 파형 애니메이션 시작
                    waveformJob = animateWaveform(coroutineScope) { newData ->
                        waveformData = newData
                    }
                }
            )
        }
    }

    // 인터넷 연결 확인 함수
    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // STT 변환 함수 - REST API 사용
    fun convertAudioToText(audioFile: File, onResult: (String) -> Unit) {
        if (!isNetworkAvailable()) {
            onResult("인터넷 연결이 필요합니다. Wi-Fi 또는 모바일 데이터를 확인해주세요.")
            return
        }

        // API 키 검증
        if (googleAPIKey.isBlank()) {
            onResult("Google API 키가 설정되지 않았습니다. 설정 후 다시 시도해주세요.")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                isConvertingSTT = true
                Log.d("STT", "오디오 파일 크기: ${audioFile.length()} 바이트")

                // WAV 파일 로드 (헤더 건너뛰기)
                val audioBytes = ByteArray(audioFile.length().toInt() - 44)
                RandomAccessFile(audioFile, "r").use { file ->
                    file.seek(44) // WAV 헤더 건너뛰기
                    file.readFully(audioBytes)
                }

                Log.d("STT", "오디오 데이터 로드 완료: ${audioBytes.size} 바이트")

                // Base64로 오디오 데이터 인코딩
                val base64Audio = android.util.Base64.encodeToString(
                    audioBytes, android.util.Base64.NO_WRAP
                )

                // Google Cloud Speech-to-Text REST API 요청 JSON 생성
                val jsonBody = JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("encoding", "LINEAR16")
                        put("sampleRateHertz", 16000)
                        put("languageCode", "ko-KR")
                        put("model", "default")
                    })
                    put("audio", JSONObject().apply {
                        put("content", base64Audio)
                    })
                }

                // OkHttp 클라이언트로 REST 요청 전송
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBody =
                    jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://speech.googleapis.com/v1/speech:recognize?key=$googleAPIKey")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("STT", "Google API 요청 시작: ${request.url}")

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        Log.e(
                            "STT",
                            "API 응답 오류: ${response.code} - ${response.message}, 내용: $errorBody"
                        )
                        throw IOException("API 요청 실패: ${response.code} - ${response.message}")
                    }

                    val responseBody = response.body?.string() ?: ""
                    Log.d("STT", "응답 데이터: $responseBody")

                    val jsonResponse = JSONObject(responseBody)
                    // JSON 응답 파싱
                    val transcript =
                        if (jsonResponse.has("results") && jsonResponse.getJSONArray("results")
                                .length() > 0
                        ) {
                            val results = jsonResponse.getJSONArray("results")
                            val transcriptBuilder = StringBuilder()

                            for (i in 0 until results.length()) {
                                val result = results.getJSONObject(i)
                                if (result.has("alternatives") && result.getJSONArray("alternatives")
                                        .length() > 0
                                ) {
                                    val alternative =
                                        result.getJSONArray("alternatives").getJSONObject(0)
                                    if (alternative.has("transcript")) {
                                        if (transcriptBuilder.isNotEmpty()) transcriptBuilder.append(
                                            " "
                                        )
                                        transcriptBuilder.append(alternative.getString("transcript"))
                                    }
                                }
                            }

                            transcriptBuilder.toString()
                        } else {
                            "인식 결과가 없습니다."
                        }

                    Log.d("STT", "변환 결과: $transcript")

                    // UI 스레드에서 결과 업데이트
                    withContext(Dispatchers.Main) {
                        isConvertingSTT = false
                        onResult(transcript)
                    }
                }
            } catch (e: Exception) {
                Log.e("STT", "변환 오류", e)

                val errorMsg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "요청 시간이 초과되었습니다. 나중에 다시 시도해주세요."

                    e.message?.contains("connect", ignoreCase = true) == true ->
                        "네트워크 오류: Google 서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."

                    e.message?.contains("403", ignoreCase = true) == true ->
                        "인증 오류: API 키가 유효하지 않거나 Speech-to-Text API 권한이 활성화되지 않았습니다."

                    else -> "STT 변환 중 오류 발생: ${e.message}"
                }

                withContext(Dispatchers.Main) {
                    isConvertingSTT = false
                    onResult(errorMsg)
                }
            }
        }
    }

    // 메인 UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBF5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 상단 헤더: 뒤로가기 버튼과 연락처 이름
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = {
                    // 녹음 중이거나 일시정지 상태일 때 대화상자 표시
                    if (recordingState == RecordingState.RECORDING ||
                        recordingState == RecordingState.PAUSED
                    ) {
                        // 녹음 중이라면 일시정지로 변경
                        if (recordingState == RecordingState.RECORDING) {
                            pauseRecording(audioRecorder)
                            recordingState = RecordingState.PAUSED
                            // 타이머 및 파형 애니메이션 중지
                            timerJob?.cancel()
                            waveformJob?.cancel()
                        }

                        showExitDialog = true
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                Text(
                    text = contactName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "녹음 파일을 저장할까요?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    },
                    text = {
                        // 여백 추가를 위한 패딩 적용
                        Column(
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // 취소 버튼
                                Text(
                                    text = "취소",
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (recordingState == RecordingState.PAUSED) {
                                                resumeRecording(audioRecorder)
                                                recordingState = RecordingState.RECORDING
                                                timerJob = startTimer(
                                                    coroutineScope,
                                                    initialValue = timerSeconds
                                                ) { newTime ->
                                                    timerSeconds = newTime
                                                    formattedTime = formatTime(newTime)
                                                }

                                                waveformJob =
                                                    animateWaveform(coroutineScope) { newData ->
                                                        waveformData = newData
                                                    }
                                            }
                                            showExitDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )

                                // 수직 구분선
                                Box(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(1.dp)
                                        .background(Color.LightGray.copy(alpha = 0.5f))
                                        .align(Alignment.CenterVertically)
                                )

                                // 저장 안 함 버튼
                                Text(
                                    text = "저장 안 함",
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            stopRecording(audioRecorder)
                                            audioRecorder = null
                                            timerJob?.cancel()
                                            waveformJob?.cancel()
                                            showExitDialog = false
                                            navController.popBackStack()
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )

                                // 수직 구분선
                                Box(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(1.dp)
                                        .background(Color.LightGray.copy(alpha = 0.5f))
                                        .align(Alignment.CenterVertically)
                                )

                                // 저장 버튼
                                Text(
                                    text = "저장",
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            stopRecording(audioRecorder)
                                            audioRecorder = null
                                            recordingState = RecordingState.STOPPED
                                            timerJob?.cancel()
                                            waveformJob?.cancel()
                                            // Google STT로 변환
                                            recordingState = RecordingState.CONVERTING
                                            // 오디오 파일을 텍스트로 변환
                                            convertAudioToText(File(audioFilePath)) { result ->
                                                recognizedText = result
                                                recordingState = RecordingState.STOPPED
                                                showExitDialog = false
                                                showSaveDialog = true
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {},
                    containerColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (recordingState == RecordingState.NOT_STARTED) {
                // 녹음 시작 전 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { navController.navigate("inputtextscreen") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        Text("텍스트로 입력하기")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 봉투 아이콘을 클릭 가능하게 수정
                        EnvelopeIcon(
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 24.dp)
                                .clickable {
                                    // 마이크 권한 확인
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            // 권한이 있으면 녹음 시작
                                            startRecording(context, audioFilePath,
                                                onRecorderPrepared = { recorder ->
                                                    audioRecorder = recorder
                                                    recordingState = RecordingState.RECORDING
                                                    // 타이머 시작
                                                    timerJob =
                                                        startTimer(coroutineScope) { newTime ->
                                                            timerSeconds = newTime
                                                            formattedTime = formatTime(newTime)
                                                        }
                                                    // 파형 애니메이션 시작
                                                    waveformJob =
                                                        animateWaveform(coroutineScope) { newData ->
                                                            waveformData = newData
                                                        }
                                                }
                                            )
                                        }

                                        else -> {
                                            // 권한이 없으면 요청
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                        )

                        // 안내 텍스트 변경
                        Text(
                            text = "클릭하면 녹음 시작",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        // 마이크 버튼 제거됨
                    }
                }
            } else if (recordingState == RecordingState.RECORDING || recordingState == RecordingState.PAUSED) {
                // 녹음 중 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 타이머 표시
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 40.dp)
                    ) {
                        // 빨간색 녹화 표시 점
                        if (recordingState == RecordingState.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )

                            // 간격 추가
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        // 타이머 텍스트
                        Text(
                            text = formattedTime,
                            fontSize = 70.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }

                    // 파형 애니메이션
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp)
                    ) {
                        AudioWaveform(
                            waveformData = waveformData,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 녹음 컨트롤 버튼
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 일시정지/재개 버튼
                        Button(
                            onClick = {
                                if (recordingState == RecordingState.RECORDING) {
                                    pauseRecording(audioRecorder)
                                    recordingState = RecordingState.PAUSED
                                    timerJob?.cancel()
                                    waveformJob?.cancel()
                                } else {
                                    resumeRecording(audioRecorder)
                                    recordingState = RecordingState.RECORDING
                                    timerJob = startTimer(
                                        coroutineScope,
                                        initialValue = timerSeconds
                                    ) { newTime ->
                                        timerSeconds = newTime
                                        formattedTime = formatTime(newTime)
                                    }
                                    waveformJob = animateWaveform(coroutineScope) { newData ->
                                        waveformData = newData
                                    }
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordingState == RecordingState.RECORDING)
                                    Color(0xFFFFDCA8) else Color.Green,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = if (recordingState == RecordingState.RECORDING)
                                    Icons.Default.Menu else Icons.Default.PlayArrow,
                                contentDescription = if (recordingState == RecordingState.RECORDING)
                                    "Pause Recording" else "Resume Recording",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        // 중지 버튼
                        Button(
                            onClick = {
                                stopRecording(audioRecorder)
                                audioRecorder = null
                                recordingState = RecordingState.STOPPED
                                timerJob?.cancel()
                                waveformJob?.cancel()
                                showSaveButton = true
                            },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop Recording",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else if (recordingState == RecordingState.STOPPED && showSaveButton) {
                // 녹음 정지 후 저장/변환 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "음성편지가 작성되었습니다!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(
                        text = "편지 시간: $formattedTime",
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 재생 버튼
                        Button(
                            onClick = {
                                recordingState = RecordingState.PLAYING
                                playRecordedAudio(
                                    context,
                                    audioFilePath,
                                    onPlayerPrepared = { player ->
                                        mediaPlayer = player
                                    },
                                    onCompletion = {
                                        recordingState = RecordingState.STOPPED
                                        mediaPlayer = null
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFDCA8),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Recording",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("재생")
                        }

                        // STT 변환 버튼
                        Button(
                            onClick = {
                                recordingState = RecordingState.CONVERTING
                                convertAudioToText(File(audioFilePath)) { result ->
                                    recognizedText = result
                                    recordingState = RecordingState.STOPPED
                                    showSaveDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFDCA8),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Convert to Text",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("텍스트 변환")
                        }
                    }
                }
            } else if (recordingState == RecordingState.PLAYING) {
                // 녹음 재생 중 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "재생 중...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // 임시 파형 표시
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp)
                    ) {
                        AudioWaveform(
                            waveformData = waveformData,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Button(
                        onClick = {
                            stopPlayback(mediaPlayer)
                            mediaPlayer = null
                            recordingState = RecordingState.STOPPED
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Playback",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("정지")
                    }
                }
            } else if (recordingState == RecordingState.CONVERTING) {
                // STT 변환 중 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "음성을 텍스트로 변환 중...",
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 로딩 표시를 위한 작은 애니메이션 효과
                        var dotCount by remember { mutableStateOf(1) }

                        LaunchedEffect(key1 = true) {
                            while (isConvertingSTT) {
                                delay(500)
                                dotCount = (dotCount % 3) + 1
                            }
                        }

                        Text(
                            text = ".".repeat(dotCount),
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    }
                }
            }

// 녹음 저장 대화상자
            if (showSaveDialog) {
                var selectedOption by remember { mutableStateOf(0) }
                // 두 번째 옵션 텍스트 상태 추가
                var customDateText by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = {
                        Text(
                            text = "편지 저장",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            // 라디오 버튼 옵션 - 자동 저장
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = 0 }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == 0,
                                    onClick = { selectedOption = 0 },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFFDCA8)
                                    )
                                )

                                Text(
                                    text = "자동 저장",
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // 두 번째 옵션 - 사용자 지정 날짜
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = 1 }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == 1,
                                    onClick = { selectedOption = 1 },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFFFFDCA8)
                                    )
                                )

                                // 선택되었을 때는 편집 가능한 TextField로 변경
                                if (selectedOption == 1) {
                                    TextField(
                                        value = customDateText,
                                        onValueChange = { customDateText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedIndicatorColor = Color(0xFFFFDCA8),
                                            unfocusedIndicatorColor = Color.LightGray
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                    )
                                } else {
                                    Text(
                                        text = customDateText,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (selectedOption == 0) {
                                    // 자동 저장 옵션 선택 시 - 제목 추천 및 감정 분석 API 호출
                                    coroutineScope.launch {
                                        try {
                                            // 제목 추천 API 호출
                                            val titleResponse = RetrofitClient.apiService.generateText(
                                                TextRequest(recognizedText)
                                            )
                                            val recommendedTitle = titleResponse.generated_text

                                            // 감정 분석 API 호출
                                            val emotionResponse = RetrofitInstance2.api.analyzeText(
                                                EmotionRequest(recognizedText)
                                            )
                                            val analyzedEmotion = emotionResponse.emotion

                                            // URL 인코딩 적용
                                            val encodedTitle = java.net.URLEncoder.encode(recommendedTitle, "UTF-8")
                                            val encodedContent = java.net.URLEncoder.encode(recognizedText, "UTF-8")
                                            val encodedEmotion = java.net.URLEncoder.encode(analyzedEmotion, "UTF-8")

                                            // 로그 출력으로 값 확인
                                            Log.d("RecordingScreen", "제목: $recommendedTitle, 감정: $analyzedEmotion")

                                            // UI 스레드에서 네비게이션 실행
                                            withContext(Dispatchers.Main) {
                                                showSaveDialog = false
                                                navController.navigate("inputtextscreen?title=${encodedTitle}&content=${encodedContent}&emotion=${encodedEmotion}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RecordingScreen", "Error processing text: ${e.message}")
                                            // 에러 발생 시 기본 데이터로 전달
                                            withContext(Dispatchers.Main) {
                                                showSaveDialog = false
                                                val encodedContent = java.net.URLEncoder.encode(recognizedText, "UTF-8")
                                                navController.navigate("inputtextscreen?content=${encodedContent}")
                                            }
                                        }
                                    }
                                } else {
                                    // 두 번째 옵션의 경우 사용자가 입력한 제목 사용
                                    val encodedTitle = java.net.URLEncoder.encode(customDateText, "UTF-8")
                                    val encodedContent = java.net.URLEncoder.encode(recognizedText, "UTF-8")
                                    showSaveDialog = false
                                    navController.navigate("inputtextscreen?title=${encodedTitle}&content=${encodedContent}")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFDCA8),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("저장")
                        }
                    }
                    ,
                    dismissButton = {
                        Button(
                            onClick = { showSaveDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("취소")
                        }
                    }
                )
            }
        }
    }
}

