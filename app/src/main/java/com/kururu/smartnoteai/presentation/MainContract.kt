package com.kururu.smartnoteai.presentation

import com.kururu.smartnoteai.domain.model.Note

interface MainContract {
    data class State(
        val notes: List<Note> = emptyList(),
        val isGenerating: Boolean = false,
        val error: String? = null,
    )

    interface View {
        fun render(state: State)
        fun showError(message: String)
    }
}
