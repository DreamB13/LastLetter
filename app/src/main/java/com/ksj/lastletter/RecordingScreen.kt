package com.ksj.lastletter

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Job

// 녹음 상태 관리를 위한 열거형
enum class RecordingState {
    NOT_STARTED,  // 녹음 시작 전
    RECORDING,    // 녹음 중
    PAUSED,       // 일시정지 상태
    STOPPED,      // 정지 상태
    PLAYING       // 재생 중
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

    // 미디어 리소스 관리
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 코루틴 작업 관리
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var waveformJob by remember { mutableStateOf<Job?>(null) }

    // 오디오 파일 경로
    val audioFilePath = remember { "${context.cacheDir.absolutePath}/recorded_audio.3gp" }

    //저장 버튼 변수
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
                mediaRecorder?.apply {
                    if (recordingState == RecordingState.RECORDING) {
                        stop()
                    }
                    release()
                }
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
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
                    mediaRecorder = recorder
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
                IconButton(onClick = { // 녹음 중이거나 일시정지 상태일 때 대화상자 표시
                    if (recordingState == RecordingState.RECORDING ||
                        recordingState == RecordingState.PAUSED
                    ) { // 녹음 중이라면 일시정지로 변경
                        if (recordingState == RecordingState.RECORDING) {
                            pauseRecording(mediaRecorder)
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
                                                resumeRecording(mediaRecorder)
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
                                            showExitDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                            stopRecording(mediaRecorder)
                                            mediaRecorder = null
                                            timerJob?.cancel()
                                            waveformJob?.cancel()
                                            showExitDialog = false
                                            navController.popBackStack()
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                            stopRecording(mediaRecorder)
                                            mediaRecorder = null
                                            recordingState = RecordingState.STOPPED
                                            timerJob?.cancel()
                                            waveformJob?.cancel()
                                            recognizedText = "녹음된 내용이 텍스트로 변환되었습니다."
                                            showExitDialog = false
                                            showSaveDialog = true
                                        }
                                        .padding(vertical = 12.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EnvelopeIcon(
                            modifier = Modifier
                                .size(180.dp)
                                .clickable {
                                    // 마이크 권한 확인 및 녹음 시작
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startRecording(context, audioFilePath,
                                            onRecorderPrepared = { recorder ->
                                                mediaRecorder = recorder
                                                recordingState = RecordingState.RECORDING

                                                // 타이머 시작
                                                timerJob = startTimer(coroutineScope) { newTime ->
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
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "클릭하면 녹음 시작", fontSize = 18.sp, color = Color.Black)
                    }
                }
            } else {
                // 녹음/재생/일시정지/정지 화면
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 타이머 표시
                    Row(
                        modifier = Modifier.padding(top = 40.dp, bottom = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 녹음 중일 때만 빨간 점 표시
                        if (recordingState == RecordingState.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = formattedTime,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // 편지 아이콘
                    EnvelopeIcon(modifier = Modifier.size(100.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    // 상태 텍스트
                    Text(
                        text = when (recordingState) {
                            RecordingState.RECORDING -> "녹음 중입니다"
                            RecordingState.PAUSED -> "녹음 일시정지"
                            RecordingState.PLAYING -> "재생 중입니다"
                            RecordingState.STOPPED -> "녹음 완료"
                            else -> ""
                        },
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // 오디오 파형
                    AudioWaveform(
                        waveformData = waveformData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // 저장 버튼 표시 조건
                    if (recordingState == RecordingState.STOPPED && showSaveButton) {
                        Button(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFDCA8),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "편지 저장",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    if (showSaveDialog) {
                        SaveLetterDialog(
                            recognizedText = recognizedText,
                            onDismiss = { showSaveDialog = false },
                            onSave = { title, content ->
                                // 여기서 저장 로직을 구현
                                // Firestore에 저장하는 코드를 추가할 수 있음
                                showSaveDialog = false
                                showSaveButton = false
                                navController.popBackStack() // 저장 후 이전 화면으로 돌아가기
                            }
                        )
                    }


                    Spacer(modifier = Modifier.weight(1f))

                    // 컨트롤 버튼들
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 재생 버튼
                        IconButton(
                            onClick = {
                                if (recordingState == RecordingState.STOPPED) {
                                    // 녹음된 오디오 재생
                                    playRecordedAudio(context, audioFilePath,
                                        onPlayerPrepared = { player ->
                                            mediaPlayer = player
                                            recordingState = RecordingState.PLAYING

                                            // 타이머 리셋 및 시작
                                            timerSeconds = 0f
                                            timerJob = startTimer(coroutineScope) { newTime ->
                                                timerSeconds = newTime
                                                formattedTime = formatTime(newTime)
                                            }

                                            // 파형 애니메이션 시작
                                            waveformJob =
                                                animateWaveform(coroutineScope) { newData ->
                                                    waveformData = newData
                                                }
                                        },
                                        onCompletion = {
                                            recordingState = RecordingState.STOPPED
                                            timerJob?.cancel()
                                            waveformJob?.cancel()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            enabled = recordingState == RecordingState.STOPPED
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (recordingState == RecordingState.STOPPED)
                                    Color(0xFFFFDCA8) else Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // 일시정지/재개 버튼
                        IconButton(
                            onClick = {
                                when (recordingState) {
                                    RecordingState.RECORDING -> {
                                        // 녹음 일시정지
                                        pauseRecording(mediaRecorder)
                                        recordingState = RecordingState.PAUSED

                                        // 타이머 및 파형 애니메이션 중지
                                        timerJob?.cancel()
                                        waveformJob?.cancel()
                                    }

                                    RecordingState.PAUSED -> {
                                        // 녹음 재개
                                        resumeRecording(mediaRecorder)
                                        recordingState = RecordingState.RECORDING

                                        // 타이머 및 파형 애니메이션 재개
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

                                    RecordingState.PLAYING -> {
                                        // 재생 일시정지
                                        pausePlayback(mediaPlayer)
                                        recordingState = RecordingState.STOPPED

                                        // 타이머 및 파형 애니메이션 중지
                                        timerJob?.cancel()
                                        waveformJob?.cancel()
                                    }

                                    else -> {}
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White, CircleShape)
                                .border(2.dp, Color(0xFFFFDCA8), CircleShape),
                            enabled = recordingState in listOf(
                                RecordingState.RECORDING,
                                RecordingState.PAUSED,
                                RecordingState.PLAYING
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Pause",
                                tint = Color(0xFFFFDCA8),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // 정지 버튼
                        IconButton(
                            onClick = {
                                when (recordingState) {
                                    RecordingState.RECORDING, RecordingState.PAUSED -> {
                                        // 녹음 정지
                                        stopRecording(mediaRecorder)
                                        mediaRecorder = null
                                        recordingState = RecordingState.STOPPED

                                        // 타이머 및 파형 애니메이션 중지
                                        timerJob?.cancel()
                                        waveformJob?.cancel()

                                        // STT 변환 (실제 구현에서는 여기서 STT API 호출)
                                        // 예시로 텍스트 설정
                                        recognizedText = "녹음된 내용이 텍스트로 변환되었습니다."

                                        // 저장 버튼 표시
                                        showSaveButton = true
                                    }

                                    RecordingState.PLAYING -> {
                                        // 재생 정지
                                        stopPlayback(mediaPlayer)
                                        mediaPlayer = null
                                        recordingState = RecordingState.STOPPED

                                        // 타이머 및 파형 애니메이션 중지
                                        timerJob?.cancel()
                                        waveformJob?.cancel()

                                        showSaveButton = true
                                    }

                                    else -> {}
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            enabled = recordingState in listOf(
                                RecordingState.RECORDING,
                                RecordingState.PAUSED,
                                RecordingState.PLAYING
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = if (recordingState in listOf(
                                        RecordingState.RECORDING,
                                        RecordingState.PAUSED,
                                        RecordingState.PLAYING
                                    )
                                ) Color(0xFFFFDCA8) else Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveLetterDialog(
    recognizedText: String,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var selectedOption by remember { mutableStateOf("자동 저장") }
    var date by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(recognizedText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "편지 저장",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFFE4C4),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                // 라디오 버튼 옵션들
                Text(
                    text = "저장 방식",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "자동 저장",
                        onClick = { selectedOption = "자동 저장" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFDCA8),
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = "자동 저장",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = selectedOption == "직접 말하기",
                        onClick = { selectedOption = "직접 말하기" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFFFDCA8),
                            unselectedColor = Color.Gray
                        )
                    )
                    Text(
                        text = "직접 말하기",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 날짜 입력 필드
                TextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("날짜") },
                    placeholder = { Text("예: 12월 26일") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFFFFDCA8),
                        cursorColor = Color(0xFFFFDCA8)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 내용 입력 필드 (인식된 텍스트로 미리 채워짐)
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("내용") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = Color(0xFFFFDCA8),
                        cursorColor = Color(0xFFFFDCA8)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, content) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFDCA8),
                    contentColor = Color.Black
                )
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text("취소")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}