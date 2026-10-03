package com.kururu.smartnoteai.data.ai

import com.google.mlkit.genai.prompt.GenerativeModel
import com.kururu.smartnoteai.domain.ai.AiNoteGenerator

class GeminiNanoNoteGenerator(
    private val model: GenerativeModel,
) : AiNoteGenerator {
    override suspend fun generate(transcript: String): String {
        require(transcript.isNotBlank())
        val status = model.checkStatus()
        check(status.name == "AVAILABLE") { "Gemini Nano is not available on this device" }
        val prompt = """
            You are SmartNote AI, an on-device meeting and lecture note assistant.
            Create concise Notion-style Markdown from the transcript.
            Use only facts present in the transcript. Never invent names, decisions, tasks, dates or deadlines.
            Preserve the transcript language.

            # Executive Summary
            ## 🎯 Key Points
            ## 🚀 Action Items
            ## 💡 Decisions
            ## ❓ Open Questions
            ## 📌 Takeaways

            Omit unsupported sections. Keep the result concise.

            TRANSCRIPT:
            $transcript
        """.trimIndent()
        return model.generateContent(prompt).text.orEmpty()
    }
}
