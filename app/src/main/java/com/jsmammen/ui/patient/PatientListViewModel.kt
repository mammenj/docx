package com.jsmammen.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientListViewModel(
    private val repository: OutpatientRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _allPatients = MutableStateFlow<List<Patient>>(emptyList())
    private val _visitsByPatient = MutableStateFlow<Map<Long, List<Visit>>>(emptyMap())

    val visitsByPatient: StateFlow<Map<Long, List<Visit>>> = _visitsByPatient

    private val _visitCollectors = mutableMapOf<Long, kotlinx.coroutines.Job>()

    init {
        viewModelScope.launch {
            repository.observePatients().collect { patients ->
                _allPatients.value = patients
                val currentIds = patients.map { it.id }.toSet()
                patients.forEach { patient ->
                    if (!_visitCollectors.containsKey(patient.id)) {
                        _visitCollectors[patient.id] = viewModelScope.launch {
                            repository.observeVisits(patient.id).collect { visits ->
                                _visitsByPatient.value =
                                    _visitsByPatient.value.toMutableMap().apply {
                                        put(patient.id, visits)
                                    }
                            }
                        }
                    }
                }
                _visitCollectors.keys.toList().forEach { id ->
                    if (id !in currentIds) {
                        _visitCollectors.remove(id)?.cancel()
                        _visitsByPatient.value =
                            _visitsByPatient.value.toMutableMap().apply { remove(id) }
                    }
                }
            }
        }
    }

    val patients: StateFlow<List<Patient>> = combine(
        _query,
        _allPatients,
        _visitsByPatient,
    ) { query, patients, visits ->
        val q = query.trim()
        if (q.isBlank()) {
            patients
        } else {
            patients.filter { patient ->
                patientMatches(patient, visits[patient.id].orEmpty(), q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun visitMatches(visit: Visit, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return false
        return visit.department.contains(q, ignoreCase = true) ||
            visit.symptoms.contains(q, ignoreCase = true) ||
            visit.notes.contains(q, ignoreCase = true) ||
            visit.systolic?.toString()?.contains(q) == true ||
            visit.diastolic?.toString()?.contains(q) == true ||
            visit.glucoseValue?.toString()?.contains(q) == true ||
            visit.pulse?.toString()?.contains(q) == true ||
            visit.temperatureC?.toString()?.contains(q) == true ||
            visit.spo2?.toString()?.contains(q) == true ||
            visit.weightKg?.toString()?.contains(q) == true
    }

    private fun patientMatches(patient: Patient, visits: List<Visit>, query: String): Boolean {
        if (patient.name.contains(query, ignoreCase = true)) return true
        if (patient.uid.contains(query, ignoreCase = true)) return true
        patient.idNumber?.let { if (it.contains(query, ignoreCase = true)) return true }
        patient.sex?.let { if (it.contains(query, ignoreCase = true)) return true }
        patient.phone?.let { if (it.contains(query, ignoreCase = true)) return true }
        patient.ageYears?.let { if (it.toString().contains(query)) return true }

        return visits.any { visit ->
            visit.department.contains(query, ignoreCase = true) ||
                visit.symptoms.contains(query, ignoreCase = true) ||
                visit.notes.contains(query, ignoreCase = true) ||
                visit.systolic?.toString()?.contains(query) == true ||
                visit.diastolic?.toString()?.contains(query) == true ||
                visit.glucoseValue?.toString()?.contains(query) == true ||
                visit.pulse?.toString()?.contains(query) == true ||
                visit.temperatureC?.toString()?.contains(query) == true ||
                visit.spo2?.toString()?.contains(query) == true ||
                visit.weightKg?.toString()?.contains(query) == true
        }
    }
}
