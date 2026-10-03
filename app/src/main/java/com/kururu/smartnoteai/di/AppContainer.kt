package com.kururu.smartnoteai.di

import android.content.Context
import com.kururu.smartnoteai.data.ai.GeminiNanoNoteGenerator
import com.kururu.smartnoteai.data.repository.InMemoryNoteRepository
import com.kururu.smartnoteai.domain.ai.AiNoteGenerator
import com.kururu.smartnoteai.domain.repository.NoteRepository
import com.kururu.smartnoteai.domain.usecase.GenerateSmartNotesUseCase

class AppContainer(context: Context) {
    val noteRepository: NoteRepository = InMemoryNoteRepository()
    val aiNoteGenerator: AiNoteGenerator = GeminiNanoNoteGenerator(context)
    val generateSmartNotesUseCase = GenerateSmartNotesUseCase(aiNoteGenerator)
}
