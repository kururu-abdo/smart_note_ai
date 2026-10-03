package com.kururu.smartnoteai.data.repository

import com.kururu.smartnoteai.data.local.NoteDao
import com.kururu.smartnoteai.data.local.NoteEntity
import com.kururu.smartnoteai.domain.model.Note
import com.kururu.smartnoteai.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomNoteRepository(private val dao: NoteDao) : NoteRepository {
    override fun observeNotes(): Flow<List<Note>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(note: Note) {
        dao.insert(
            NoteEntity(note.id, note.title, note.description, note.type, note.durationSeconds, note.date, note.summary)
        )
    }

    private fun NoteEntity.toDomain() = Note(id, title, description, type, durationSeconds, createdAt, summary)
}
