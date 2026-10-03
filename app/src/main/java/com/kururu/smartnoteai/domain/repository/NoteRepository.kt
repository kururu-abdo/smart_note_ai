package com.kururu.smartnoteai.domain.repository

import com.kururu.smartnoteai.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun save(note: Note)
}
