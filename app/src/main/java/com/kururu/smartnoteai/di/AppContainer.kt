package com.kururu.smartnoteai.di

import android.content.Context
import androidx.room.Room
import com.google.mlkit.genai.prompt.Generation
import com.kururu.smartnoteai.data.ai.ChunkedAiNoteGenerator
import com.kururu.smartnoteai.data.ai.GeminiNanoNoteGenerator
import com.kururu.smartnoteai.data.audio.AndroidAudioRecorder
import com.kururu.smartnoteai.data.local.AppDatabase
import com.kururu.smartnoteai.data.repository.RoomNoteRepository
import com.kururu.smartnoteai.data.speech.AndroidSpeechTranscriber
import com.kururu.smartnoteai.domain.ai.AiNoteGenerator
import com.kururu.smartnoteai.domain.audio.AudioRecorder
import com.kururu.smartnoteai.domain.repository.NoteRepository
import com.kururu.smartnoteai.domain.speech.SpeechTranscriber
import com.kururu.smartnoteai.domain.usecase.GenerateSmartNotesUseCase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(appContext, AppDatabase::class.java, "smart_notes.db").build()
    private val generativeModel = Generation.getClient()
    private val geminiNano = GeminiNanoNoteGenerator(generativeModel)

    val noteRepository: NoteRepository = RoomNoteRepository(database.noteDao())
    val aiNoteGenerator: AiNoteGenerator = ChunkedAiNoteGenerator(geminiNano)
    val generateSmartNotesUseCase = GenerateSmartNotesUseCase(aiNoteGenerator)
    val speechTranscriber: SpeechTranscriber = AndroidSpeechTranscriber(appContext)
    val audioRecorder: AudioRecorder = AndroidAudioRecorder(appContext)
}