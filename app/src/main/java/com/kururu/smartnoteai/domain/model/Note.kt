package com.kururu.smartnoteai.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val description: String,
    val type: String,
    val durationSeconds: Long,
    val date: Long,
    val summary: String,
)
