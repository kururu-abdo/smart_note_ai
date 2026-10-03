package com.kururu.smartnoteai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val type: String,
    val durationSeconds: Long,
    val createdAt: Long,
    val summary: String,
    val transcript: String = "",
    val audioPath: String? = null,
)
