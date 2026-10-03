package com.kururu.smartnoteai.domain.audio

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AudioRecorder {
    val state: StateFlow<State>
    fun start(): Result<Unit>
    fun pause(): Result<Unit>
    fun resume(): Result<Unit>
    fun stop(): Result<RecordingResult>
    fun release()

    sealed interface State {
        data object Idle : State
        data class Recording(val elapsedMs: Long) : State
        data class Paused(val elapsedMs: Long) : State
        data class Error(val message: String) : State
    }

    data class RecordingResult(val file: File, val durationMs: Long)
}
