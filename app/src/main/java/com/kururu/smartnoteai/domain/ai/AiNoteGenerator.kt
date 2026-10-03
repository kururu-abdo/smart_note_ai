package com.kururu.smartnoteai.domain.ai

interface AiNoteGenerator {
    suspend fun generate(transcript: String): String
}
