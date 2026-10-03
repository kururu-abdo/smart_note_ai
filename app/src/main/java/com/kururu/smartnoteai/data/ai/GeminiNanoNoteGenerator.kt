package com.kururu.smartnoteai.data.ai

import android.content.Context
import com.google.mlkit.genai.prompt.GenerativeModel
import com.kururu.smartnoteai.domain.ai.AiNoteGenerator

class GeminiNanoNoteGenerator(
    private val context: Context,
) : AiNoteGenerator {
    override suspend fun generate(transcript: String): String {
        val model = GenerativeModel(context)
        val prompt = """
            You are a concise meeting and lecture note assistant.
            Transform the transcript into Notion-style Markdown.
            Use only facts present in the transcript. Never invent people,
            dates, decisions, tasks, or deadlines.

            Return these sections when supported:
            # Executive Summary
            ## 🎯 Key Points
            ## 🚀 Action Items
            ## 💡 Decisions
            ## ❓ Open Questions
            ## 📌 Takeaways

            Keep it concise and useful. Preserve the transcript language.

            TRANSCRIPT:
            $transcript
        """.trimIndent()
        return model.generateContent(prompt).text.orEmpty()
    }
}
