package com.kururu.smartnoteai.presentation

import com.kururu.smartnoteai.domain.model.Note
import com.kururu.smartnoteai.domain.repository.NoteRepository
import com.kururu.smartnoteai.domain.usecase.GenerateSmartNotesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class MainPresenter(
    private val repository: NoteRepository,
    private val generateSmartNotes: GenerateSmartNotesUseCase,
    private val scope: CoroutineScope,
) {
    private var observeJob: Job? = null
    private val viewRef = AtomicReference<MainContract.View?>()

    fun attach(view: MainContract.View) {
        viewRef.set(view)
        observeJob?.cancel()
        observeJob = scope.launch {
            repository.observeNotes().catch { view.showError(it.message ?: "Unable to load notes") }.collect {
                view.render(MainContract.State(notes = it))
            }
        }
    }

    fun detach() { viewRef.set(null); observeJob?.cancel() }

    fun saveNote(note: Note) {
        scope.launch {
            repository.save(note)
        }
    }

    fun generate(transcript: String, onResult: (String) -> Unit) {
        scope.launch {
            runCatching { generateSmartNotes(transcript) }
                .onSuccess(onResult)
                .onFailure { viewRef.get()?.showError(it.message ?: "AI generation failed") }
        }
    }
}
