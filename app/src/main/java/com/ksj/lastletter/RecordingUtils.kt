package com.ksj.lastletter

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import kotlin.random.Random

// 녹음 시작 함수
fun startRecording(
    context: Context,
    outputFile: String,
    onRecorderPrepared: (MediaRecorder) -> Unit
) {
    // 파일 생성
    val file = File(outputFile)
    if (file.exists()) {
        file.delete()
    }

    try {
        // MediaRecorder 초기화
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile)
            prepare()
            start()
        }

        onRecorderPrepared(recorder)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 녹음 일시정지 함수
fun pauseRecording(recorder: MediaRecorder?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            recorder?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 녹음 재개 함수
fun resumeRecording(recorder: MediaRecorder?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            recorder?.resume()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 녹음 정지 함수
fun stopRecording(recorder: MediaRecorder?) {
    try {
        recorder?.apply {
            stop()
            release()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 녹음된 오디오 재생 함수
fun playRecordedAudio(
    context: Context,
    audioFilePath: String,
    onPlayerPrepared: (MediaPlayer) -> Unit,
    onCompletion: () -> Unit
) {
    try {
        val player = MediaPlayer().apply {
            setDataSource(audioFilePath)
            prepare()

            setOnCompletionListener {
                onCompletion()
                release()
            }

            start()
        }

        onPlayerPrepared(player)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 재생 일시정지 함수
fun pausePlayback(player: MediaPlayer?) {
    try {
        if (player?.isPlaying == true) {
            player.pause()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 재생 정지 함수
fun stopPlayback(player: MediaPlayer?) {
    try {
        player?.apply {
            stop()
            release()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// 타이머 시작 함수
fun startTimer(
    coroutineScope: CoroutineScope,
    initialValue: Float = 0f,
    onTimerUpdate: (Float) -> Unit
): Job {
    var currentTime = initialValue

    return coroutineScope.launch {
        while (true) {
            delay(100) // 0.1초마다 업데이트
            currentTime += 0.1f
            onTimerUpdate(currentTime)
        }
    }
}

// 파형 애니메이션 함수
fun animateWaveform(
    coroutineScope: CoroutineScope,
    onWaveformUpdate: (List<Float>) -> Unit
): Job {
    return coroutineScope.launch {
        while (true) {
            delay(150) // 0.15초마다 업데이트
            val newData = List(50) { Random.nextFloat() * 0.8f + 0.1f }
            onWaveformUpdate(newData)
        }
    }
}

// 시간 포맷팅 함수
fun formatTime(seconds: Float): String {
    val minutes = (seconds / 60).toInt()
    val secs = seconds % 60
    val decimalFormat = DecimalFormat("00")
    val decimalSecondsFormat = DecimalFormat("00.0")
    return "${decimalFormat.format(minutes)}:${decimalSecondsFormat.format(secs)}"
}

// 오디오 파형 표시 컴포넌트
@Composable
fun AudioWaveform(waveformData: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / waveformData.size
        val centerY = height / 2

        // 파형 막대 그리기
        waveformData.forEachIndexed { index, amplitude ->
            val barHeight = height * amplitude
            val startY = centerY - barHeight / 2

            drawRect(
                color = Color.Black,
                topLeft = Offset(index * barWidth, startY),
                size = Size(barWidth - 2, barHeight)
            )
        }

        // 현재 위치 표시 빨간 선
        drawLine(
            color = Color.Red,
            start = Offset(width * 0.9f, 0f),
            end = Offset(width * 0.9f, height),
            strokeWidth = 2f
        )
    }
}

// 편지 아이콘 컴포넌트
@Composable
fun EnvelopeIcon(
    modifier: Modifier = Modifier,
    envelopeColor: Color = Color(0xFFFFF4E6),
    borderColor: Color = Color(0xFFFFDCA8)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 편지 배경
        drawRoundRect(
            color = envelopeColor,
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.1f)
        )

        // 편지 테두리
        drawRoundRect(
            color = borderColor,
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.1f),
            style = Stroke(width = width * 0.02f)
        )

        // 삼각형 접힌 부분
        val trianglePath = Path().apply {
            moveTo(0f, 0f)
            lineTo(width / 2, height / 2)
            lineTo(width, 0f)
            close()
        }
        drawPath(
            path = trianglePath,
            color = borderColor,
            style = Stroke(width = width * 0.02f)
        )

        // 편지 줄
        val lineSpacing = height * 0.1f
        val lineStart = width * 0.65f
        val lineEnd = width * 0.9f
        val firstLineY = height * 0.6f

        for (i in 0..2) {
            drawLine(
                color = borderColor,
                start = Offset(lineStart, firstLineY + (i * lineSpacing)),
                end = Offset(lineEnd, firstLineY + (i * lineSpacing)),
                strokeWidth = width * 0.02f
            )
        }
    }
}
