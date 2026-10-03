package com.kururu.smartnoteai.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.kururu.smartnoteai.domain.audio.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {
    private val mutableState = MutableStateFlow<AudioRecorder.State>(AudioRecorder.State.Idle)
    override val state = mutableState.asStateFlow()
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs = 0L
    private var accumulatedMs = 0L

    override fun start(): Result<Unit> = runCatching {
        check(recorder == null) { "Recorder is already active." }
        val file = File(context.filesDir, "recording_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = file
        startedAtMs = System.currentTimeMillis()
        accumulatedMs = 0L
        mutableState.value = AudioRecorder.State.Recording(0L)
    }.onFailure { fail(it) }

    override fun pause(): Result<Unit> = runCatching {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { "Pause is not supported on this Android version." }
        val active = checkNotNull(recorder) { "Recorder is not active." }
        active.pause()
        accumulatedMs += System.currentTimeMillis() - startedAtMs
        mutableState.value = AudioRecorder.State.Paused(accumulatedMs)
    }.onFailure { setError(it) }

    override fun resume(): Result<Unit> = runCatching {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { "Resume is not supported on this Android version." }
        checkNotNull(recorder) { "Recorder is not active." }.resume()
        startedAtMs = System.currentTimeMillis()
        mutableState.value = AudioRecorder.State.Recording(accumulatedMs)
    }.onFailure { setError(it) }

    override fun stop(): Result<AudioRecorder.RecordingResult> = runCatching {
        val active = checkNotNull(recorder) { "Recorder is not active." }
        val duration = when (val current = mutableState.value) {
            is AudioRecorder.State.Paused -> current.elapsedMs
            is AudioRecorder.State.Recording -> accumulatedMs + (System.currentTimeMillis() - startedAtMs)
            else -> accumulatedMs
        }
        active.stop()
        active.release()
        recorder = null
        val file = checkNotNull(outputFile)
        outputFile = null
        mutableState.value = AudioRecorder.State.Idle
        AudioRecorder.RecordingResult(file, duration)
    }.onFailure { fail(it) }

    override fun release() {
        recorder?.release()
        recorder = null
        mutableState.value = AudioRecorder.State.Idle
    }

    fun refreshElapsed() {
        if (mutableState.value is AudioRecorder.State.Recording) {
            mutableState.update {
                AudioRecorder.State.Recording(
                    accumulatedMs + (System.currentTimeMillis() - startedAtMs),
                )
            }
        }
    }

    private fun fail(error: Throwable) {
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
        setError(error)
    }

    private fun setError(error: Throwable) {
        mutableState.value = AudioRecorder.State.Error(error.message ?: "Audio recording failed.")
    }
}
