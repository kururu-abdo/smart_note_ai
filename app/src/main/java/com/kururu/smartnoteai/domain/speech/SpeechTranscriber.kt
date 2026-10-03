package com.kururu.smartnoteai.domain.speech

interface SpeechTranscriber {
    fun start(languageTag: String = "en-US")
    fun pause()
    fun resume()
    fun stop()
    fun release()
    fun setListener(listener: Listener)

    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }
}
