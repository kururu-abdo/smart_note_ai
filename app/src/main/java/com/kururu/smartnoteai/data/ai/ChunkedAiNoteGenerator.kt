package com.kururu.smartnoteai.data.ai

import com.kururu.smartnoteai.domain.ai.AiNoteGenerator

/** Keeps local Gemini Nano requests below the Prompt API input limit. */
class ChunkedAiNoteGenerator(
    private val delegate: AiNoteGenerator,
    private val maxCharsPerChunk: Int = 8_000,
) : AiNoteGenerator {
    override suspend fun generate(transcript: String): String {
        val chunks = transcript.chunked(maxCharsPerChunk)
        if (chunks.size == 1) return delegate.generate(chunks.first())
        val partials = chunks.mapIndexed { index, chunk ->
            delegate.generate("""
                Summarize this transcript segment #${index + 1} into concise factual notes.
                Preserve important decisions, action items, questions, and facts.
                Do not invent missing information.

                $chunk
            """.trimIndent())
        }
        return delegate.generate("""
            Merge these transcript segment summaries into one concise Notion-style note.
            Remove duplicates. Preserve only facts supported by the summaries.
            Return: # Executive Summary, ## 🎯 Key Points, ## 🚀 Action Items,
            ## 💡 Decisions, ## ❓ Open Questions, ## 📌 Takeaways.

            SEGMENT SUMMARIES:
            ${partials.joinToString("\n\n---\n\n")}
        """.trimIndent())
    }
}
