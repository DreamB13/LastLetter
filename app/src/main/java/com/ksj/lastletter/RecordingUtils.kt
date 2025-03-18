package com.ksj.lastletter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.text.DecimalFormat
import kotlin.random.Random

// AudioRecord 전역 변수
private var audioRecord: AudioRecord? = null
private var isRecording = false
private var recordingThread: Thread? = null

// 오디오 설정
private val sampleRate = 16000 // 16kHz - STT에 적합한 샘플레이트
private val channelConfig = AudioFormat.CHANNEL_IN_MONO
private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

fun startRecording(
    context: Context,
    outputFile: String,
    onRecorderPrepared: (AudioRecord) -> Unit
) {
    val file = File(outputFile)
    if (file.exists()) {
        file.delete()
    }

    // 권한 확인 코드 추가
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
        Log.e("RecordingUtils", "RECORD_AUDIO 권한이 없습니다.")
        return
    }

    try {
        // 버퍼 크기 계산
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
            Log.e("AudioRecord", "지원되지 않는 오디오 파라미터")
            return
        }

        val bufferSize = minBufferSize * 2 // 최소 크기의 2배로 설정

        // AudioRecord 인스턴스 생성 - try-catch로 감싸기
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e("RecordingUtils", "오디오 녹음 권한이 거부되었습니다", e)
            return
        }

        // 초기화 상태 확인
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord 초기화 실패")
            return
        }

        isRecording = true
        // 별도 스레드에서 녹음 실행
        recordingThread = Thread {
            val data = ByteArray(bufferSize)
            FileOutputStream(file).use { outputStream ->
                // WAV 헤더 작성 (16kHz, 16비트, 모노)
                writeWavHeader(outputStream, 0) // 임시 데이터 크기로 헤더 작성
                audioRecord?.startRecording()
                var totalBytesRead = 0
                while (isRecording) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                    if (read > 0) {
                        outputStream.write(data, 0, read)
                        totalBytesRead += read
                    }
                }
                audioRecord?.stop()
                // 실제 데이터 크기로 WAV 헤더 업데이트
                updateWavHeader(file, totalBytesRead)
            }
        }

        recordingThread?.start()
        onRecorderPrepared(audioRecord!!)
    } catch (e: Exception) {
        Log.e("RecordingUtils", "startRecording: ", e)
    }
}

fun pauseRecording(recorder: AudioRecord?) {
    // AudioRecord는 직접적인 pause 메서드가 없어 임시 중지 방식 구현
    if (isRecording) {
        isRecording = false
        // AudioRecord는 stop만 있고 pause가 없어서 일시정지 효과를 위해 녹음 중지
        recorder?.stop()
    }
}

fun resumeRecording(recorder: AudioRecord?) {
    // 녹음 재개
    if (!isRecording && recorder != null) {
        isRecording = true
        recorder.startRecording()
    }
}

fun stopRecording(recorder: AudioRecord?) {
    try {
        isRecording = false
        try {
            recordingThread?.join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        recorder?.release()
        audioRecord = null
    } catch (e: Exception) {
        Log.e("RecordingUtils", "stopRecording: ", e)
    }
}

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
        Log.e("RecordingUtils", "playRecordedAudio: ", e)
    }
}

fun pausePlayback(player: MediaPlayer?) {
    try {
        if (player?.isPlaying == true) {
            player.pause()
        }
    } catch (e: Exception) {
        Log.e("RecordingUtils", "pausePlayback: ", e)
    }
}

fun stopPlayback(player: MediaPlayer?) {
    try {
        player?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
    } catch (e: Exception) {
        Log.e("RecordingUtils", "stopPlayback: ", e)
    }
}

fun startTimer(
    coroutineScope: CoroutineScope,
    initialValue: Float = 0f,
    onTimerUpdate: (Float) -> Unit
): Job {
    var currentTime = initialValue
    return coroutineScope.launch {
        while (true) {
            delay(100)
            currentTime += 0.1f
            onTimerUpdate(currentTime)
        }
    }
}

fun animateWaveform(
    coroutineScope: CoroutineScope,
    onWaveformUpdate: (List<Float>) -> Unit
): Job {
    return coroutineScope.launch {
        while (true) {
            delay(150)
            val newData = List(50) { Random.nextFloat() * 0.8f + 0.1f }
            onWaveformUpdate(newData)
        }
    }
}

fun formatTime(seconds: Float): String {
    val minutes = (seconds / 60).toInt()
    val secs = seconds % 60
    val decimalFormat = DecimalFormat("00")
    val decimalSecondsFormat = DecimalFormat("00.0")
    return "${decimalFormat.format(minutes)}:${decimalSecondsFormat.format(secs)}"
}

// WAV 헤더 작성
fun writeWavHeader(outputStream: FileOutputStream, dataSize: Int) {
    val channels = 1 // 모노
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val totalDataLen = 36 + dataSize
    val header = ByteArray(44)
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    writeInt(header, 4, totalDataLen)
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    writeInt(header, 16, 16) // PCM 서브청크 사이즈
    header[20] = 1 // AudioFormat (PCM)
    header[21] = 0
    header[22] = channels.toByte()
    header[23] = 0
    writeInt(header, 24, sampleRate)
    writeInt(header, 28, byteRate)
    header[32] = (channels * bitsPerSample / 8).toByte()
    header[33] = 0
    header[34] = bitsPerSample.toByte()
    header[35] = 0
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    writeInt(header, 40, dataSize)
    outputStream.write(header, 0, 44)
}

// WAV 헤더 업데이트 - 파일의 실제 크기에 맞게 조정
fun updateWavHeader(file: File, dataSize: Int) {
    try {
        RandomAccessFile(file, "rw").use { randomAccessFile ->
            // 파일 크기 업데이트 (RIFF 청크)
            randomAccessFile.seek(4)
            val totalDataLen = 36 + dataSize
            writeRandomInt(randomAccessFile, totalDataLen)
            // 데이터 청크 크기 업데이트
            randomAccessFile.seek(40)
            writeRandomInt(randomAccessFile, dataSize)
        }
    } catch (e: IOException) {
        Log.e("RecordingUtils", "WAV 헤더 업데이트 실패: ", e)
    }
}

// RandomAccessFile에 Int 값 쓰기
private fun writeRandomInt(file: RandomAccessFile, value: Int) {
    file.write(value and 0xff)
    file.write((value shr 8) and 0xff)
    file.write((value shr 16) and 0xff)
    file.write((value shr 24) and 0xff)
}

// 헬퍼: 지정 위치에 리틀 엔디언 형식의 int 값을 기록
fun writeInt(header: ByteArray, offset: Int, value: Int) {
    header[offset] = (value and 0xff).toByte()
    header[offset + 1] = ((value shr 8) and 0xff).toByte()
    header[offset + 2] = ((value shr 16) and 0xff).toByte()
    header[offset + 3] = ((value shr 24) and 0xff).toByte()
}

@Composable
fun AudioWaveform(waveformData: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / waveformData.size
        val centerY = height / 2

        waveformData.forEachIndexed { index, amplitude ->
            val barHeight = height * amplitude
            val startY = centerY - barHeight / 2

            drawRect(
                color = Color.Black,
                topLeft = Offset(index * barWidth, startY),
                size = Size(barWidth - 2, barHeight)
            )
        }

        drawLine(
            color = Color.Red,
            start = Offset(width * 0.9f, 0f),
            end = Offset(width * 0.9f, height),
            strokeWidth = 2f
        )
    }
}

@Composable
fun EnvelopeIcon(
    modifier: Modifier = Modifier,
    envelopeColor: Color = Color(0xFFFFF4E6),
    borderColor: Color = Color(0xFFFFDCA8)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        drawRoundRect(
            color = envelopeColor,
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.1f)
        )

        drawRoundRect(
            color = borderColor,
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.1f),
            style = Stroke(width = width * 0.02f)
        )

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
