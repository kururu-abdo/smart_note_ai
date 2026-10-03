package com.kururu.smartnoteai.domain.usecase

import com.kururu.smartnoteai.domain.ai.AiNoteGenerator

class GenerateSmartNotesUseCase(
    private val generator: AiNoteGenerator,
) {
    suspend operator fun invoke(transcript: String): String = generator.generate(transcript)
}
