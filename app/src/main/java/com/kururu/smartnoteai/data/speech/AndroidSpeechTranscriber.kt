package com.kururu.smartnoteai.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.kururu.smartnoteai.domain.speech.SpeechTranscriber

class AndroidSpeechTranscriber(context: Context) : SpeechTranscriber {
    private val appContext = context.applicationContext
    private val recognizer: SpeechRecognizer =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }

    private val handler = Handler(Looper.getMainLooper())
    private var listener: SpeechTranscriber.Listener? = null
    private var languageTag = "en-US"
    private var running = false
    private var paused = false
    private var destroyed = false
    private var restartScheduled = false

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf(String::isNotBlank)
                    ?.let { listener?.onPartial(it) }
            }

            override fun onResults(results: android.os.Bundle?) {
                restartScheduled = false
                results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf(String::isNotBlank)
                    ?.let { listener?.onFinal(it) }

                scheduleRestartIfNeeded()
            }

            override fun onError(error: Int) {
                restartScheduled = false
                if (running && !paused && isRecoverable(error)) {
                    scheduleRestart()
                } else if (running && !paused) {
                    listener?.onError(errorMessage(error))
                }
            }
        })
    }

    override fun setListener(listener: SpeechTranscriber.Listener) {
        this.listener = listener
    }

    override fun start(languageTag: String) {
        check(!destroyed) { "Speech transcriber has been released." }
        this.languageTag = languageTag
        running = true
        paused = false
        cancelRestart()
        startListening()
    }

    override fun pause() {
        if (!running || paused) return
        paused = true
        cancelRestart()
        recognizer.stopListening()
    }

    override fun resume() {
        if (!running || !paused) return
        paused = false
        startListening()
    }

    override fun stop() {
        running = false
        paused = false
        cancelRestart()
        recognizer.stopListening()
    }

    override fun release() {
        running = false
        paused = false
        destroyed = true
        cancelRestart()
        recognizer.destroy()
        listener = null
    }

    private fun startListening() {
        if (!running || paused || destroyed) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        runCatching { recognizer.startListening(intent) }
            .onFailure {
                listener?.onError(it.message ?: "Unable to start speech recognition.")
            }
    }

    private fun scheduleRestartIfNeeded() {
        if (running && !paused) scheduleRestart()
    }

    private fun scheduleRestart() {
        if (restartScheduled || destroyed || !running || paused) return
        restartScheduled = true
        handler.postDelayed({
            restartScheduled = false
            if (running && !paused && !destroyed) {
                startListening()
            }
        }, RESTART_DELAY_MS)
    }

    private fun cancelRestart() {
        handler.removeCallbacksAndMessages(null)
        restartScheduled = false
    }

    private fun isRecoverable(error: Int): Boolean =
        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
            error == SpeechRecognizer.ERROR_NO_MATCH ||
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
            error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognition service disconnected."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_NO_MATCH -> "Speech could not be recognized."
        else -> "Speech recognition error ($error)."
    }

    private companion object {
        const val RESTART_DELAY_MS = 250L
    }
}
