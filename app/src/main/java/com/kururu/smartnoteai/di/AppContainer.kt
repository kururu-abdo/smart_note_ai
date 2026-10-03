package com.kururu.smartnoteai.di

import android.content.Context
import androidx.room.Room
import com.google.mlkit.genai.prompt.Generation
import com.kururu.smartnoteai.data.local.AppDatabase
import com.kururu.smartnoteai.data.repository.RoomNoteRepository
import com.kururu.smartnoteai.domain.ai.AiNoteGenerator
import com.kururu.smartnoteai.domain.repository.NoteRepository
import com.kururu.smartnoteai.domain.usecase.GenerateSmartNotesUseCase

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(context, AppDatabase::class.java, "smart_notes.db").build()

    val noteRepository: NoteRepository = RoomNoteRepository(database.noteDao())
    val aiNoteGenerator: AiNoteGenerator = com.kururu.smartnoteai.data.ai.GeminiNanoNoteGenerator(Generation.getClient())
    val generateSmartNotesUseCase = GenerateSmartNotesUseCase(aiNoteGenerator)
}
