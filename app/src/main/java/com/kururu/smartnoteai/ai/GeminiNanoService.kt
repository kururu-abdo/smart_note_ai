package com.kururu.smartnoteai.ai

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel

/** On-device Gemini Nano gateway. No API key and no backend required. */
class GeminiNanoService {
    private val model: GenerativeModel = Generation.getClient()

    suspend fun status(): FeatureStatus = model.checkStatus()

    suspend fun prepare(): Boolean = when (model.checkStatus()) {
        FeatureStatus.AVAILABLE -> true
        FeatureStatus.DOWNLOADABLE -> {
            model.download().collect { Log.d("GeminiNano", "download=$it") }
            model.checkStatus() == FeatureStatus.AVAILABLE
        }
        else -> false
    }

    suspend fun generateSmartNotes(transcript: String, title: String): String {
        require(transcript.isNotBlank())
        if (model.checkStatus() != FeatureStatus.AVAILABLE) {
            error("Gemini Nano is not available on this device")
        }
        val prompt = """
            You are SmartNote AI, an on-device meeting and lecture note assistant.
            Create concise Notion-style notes from the transcript below.
            Preserve facts and uncertainty. Never invent decisions, tasks, names, dates,
            or quotes. Keep the language of the transcript unless the title strongly implies otherwise.

            Return Markdown with exactly these sections when information exists:
            # $title
            ## Executive Summary
            ## 🎯 Key Points
            ## 🚀 Action Items
            ## 💡 Decisions
            ## ❓ Open Questions
            ## 📌 Takeaways

            Transcript:
            $transcript
        """.trimIndent()
        return model.generateContent(prompt).text ?: "No notes were generated."
    }
}
