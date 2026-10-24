package com.jsmammen.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientDetailViewModel(
    repository: OutpatientRepository,
    patientId: Long,
) : ViewModel() {

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient = _patient.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    val visits = repository.observeVisits(patientId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<Visit>(),
    )

    init {
        viewModelScope.launch {
            repository.observePatient(patientId).collect { patient ->
                _patient.value = patient
                _loading.value = false
            }
        }
    }
}
