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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

enum class RecordingState {
    NOT_STARTED,
    RECORDING,
    PAUSED,
    STOPPED,
    PLAYING
}

@Composable
fun RecordingScreen(navController: NavController, contactName: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var recordingState by remember { mutableStateOf(RecordingState.NOT_STARTED) }
    var timerSeconds by remember { mutableFloatStateOf(0.0f) }
    var formattedTime by remember { mutableStateOf("00:00.0") }
    var recognizedText by remember { mutableStateOf("") }

    var waveformData by remember { mutableStateOf(List(50) { 0.05f }) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var timerJob by remember { mutableStateOf<Job?>(null) }
    var waveformJob by remember { mutableStateOf<Job?>(null) }

    val audioFilePath = remember { "${context.cacheDir.absolutePath}/recorded_audio.3gp" }

    DisposableEffect(Unit) {
        onDispose {
            timerJob?.cancel()
            waveformJob?.cancel()

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
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording(context, audioFilePath,
                onRecorderPrepared = { recorder ->
                    mediaRecorder = recorder
                    recordingState = RecordingState.RECORDING

                    timerJob = startTimer(coroutineScope) { newTime ->
                        timerSeconds = newTime
                        formattedTime = formatTime(newTime)
                    }

                    waveformJob = animateWaveform(coroutineScope) { newData ->
                        waveformData = newData
                    }
                }
            )
        }
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
                Text(
                    text = contactName,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (recordingState == RecordingState.NOT_STARTED) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {navController.navigate("inputtextscreen")},
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffF7AC44)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        Text("텍스트로 입력하기")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EnvelopeIcon(
                            modifier = Modifier
                                .size(180.dp)
                                .clickable {
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startRecording(context, audioFilePath,
                                            onRecorderPrepared = { recorder ->
                                                mediaRecorder = recorder
                                                recordingState = RecordingState.RECORDING

                                                timerJob = startTimer(coroutineScope) { newTime ->
                                                    timerSeconds = newTime
                                                    formattedTime = formatTime(newTime)
                                                }

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
                        Text(text = "클릭하면 녹음 시작", color = Color.Black)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.padding(top = 40.dp, bottom = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                    EnvelopeIcon(modifier = Modifier.size(100.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (recordingState) {
                            RecordingState.RECORDING -> "녹음 중입니다"
                            RecordingState.PAUSED -> "녹음 일시정지"
                            RecordingState.PLAYING -> "재생 중입니다"
                            RecordingState.STOPPED -> "녹음 완료"
                            else -> ""
                        },
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    AudioWaveform(
                        waveformData = waveformData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (recordingState == RecordingState.STOPPED) {
                                    playRecordedAudio(context, audioFilePath,
                                        onPlayerPrepared = { player ->
                                            mediaPlayer = player
                                            recordingState = RecordingState.PLAYING

                                            timerSeconds = 0f
                                            timerJob = startTimer(coroutineScope) { newTime ->
                                                timerSeconds = newTime
                                                formattedTime = formatTime(newTime)
                                            }

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

                        IconButton(
                            onClick = {
                                when (recordingState) {
                                    RecordingState.RECORDING -> {
                                        pauseRecording(mediaRecorder)
                                        recordingState = RecordingState.PAUSED
                                        timerJob?.cancel()
                                        waveformJob?.cancel()
                                    }

                                    RecordingState.PAUSED -> {
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

                                    RecordingState.PLAYING -> {
                                        pausePlayback(mediaPlayer)
                                        recordingState = RecordingState.STOPPED
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

                        IconButton(
                            onClick = {
                                when (recordingState) {
                                    RecordingState.RECORDING, RecordingState.PAUSED -> {
                                        stopRecording(mediaRecorder)
                                        mediaRecorder = null
                                        recordingState = RecordingState.STOPPED
                                        timerJob?.cancel()
                                        waveformJob?.cancel()
                                        recognizedText = "녹음된 내용이 텍스트로 변환되었습니다."
                                    }

                                    RecordingState.PLAYING -> {
                                        stopPlayback(mediaPlayer)
                                        mediaPlayer = null
                                        recordingState = RecordingState.STOPPED
                                        timerJob?.cancel()
                                        waveformJob?.cancel()
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
