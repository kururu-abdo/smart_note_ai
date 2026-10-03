package com.kururu.smartnoteai.data.repository

import com.kururu.smartnoteai.domain.model.Note
import com.kururu.smartnoteai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())
    override fun observeNotes(): Flow<List<Note>> = notes.asStateFlow()
    override suspend fun save(note: Note) { notes.value = listOf(note) + notes.value }
}
