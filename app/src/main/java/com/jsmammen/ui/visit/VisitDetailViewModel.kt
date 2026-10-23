package com.jsmammen.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import com.jsmammen.ui.util.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VisitDetailViewModel(
    private val repository: OutpatientRepository,
    private val visitId: Long,
) : ViewModel() {

    private val _visit = MutableStateFlow<Visit?>(null)
    val visit = _visit.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _newNote = MutableStateFlow("")
    val newNote = _newNote.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()

    init {
        viewModelScope.launch {
            _visit.value = repository.getVisit(visitId)
            _loading.value = false
        }
    }

    fun onNewNoteChange(value: String) = _newNote.update { value }

    fun addNote() {
        val current = _visit.value ?: return
        val note = _newNote.value.trim()
        if (note.isBlank()) return

        viewModelScope.launch {
            _saving.value = true
            val timestamp = formatTimestamp(System.currentTimeMillis())
            val updatedNotes = if (current.notes.isBlank()) {
                "[$timestamp] $note"
            } else {
                "${current.notes}\n\n[$timestamp] $note"
            }
            val updated = current.copy(notes = updatedNotes)
            repository.updateVisit(updated)
            _visit.value = updated
            _newNote.value = ""
            _saving.value = false
        }
    }
}
