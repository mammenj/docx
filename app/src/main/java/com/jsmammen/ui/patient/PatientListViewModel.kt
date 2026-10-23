package com.jsmammen.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.repository.OutpatientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PatientListViewModel(
    repository: OutpatientRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val patients = _query
        .flatMapLatest { rawQuery ->
            val query = rawQuery.trim()
            repository.observePatients().map { patients ->
                if (query.isBlank()) {
                    patients
                } else {
                    patients.filter { patient ->
                        patient.name.contains(query, ignoreCase = true) ||
                            patient.uid.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList<Patient>(),
        )

    fun onQueryChange(value: String) {
        _query.value = value
    }
}
