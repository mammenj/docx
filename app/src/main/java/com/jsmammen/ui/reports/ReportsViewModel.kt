package com.jsmammen.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import com.jsmammen.util.PdfReportGenerator as PdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReportFilters(
    val searchQuery: String = "",
)

class ReportsViewModel(
    private val repository: OutpatientRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(ReportFilters())
    val filters: StateFlow<ReportFilters> = _filters

    private val _allPatients = MutableStateFlow<List<Patient>>(emptyList())
    private val _visitsByPatient = MutableStateFlow<Map<Long, List<Visit>>>(emptyMap())
    val visitsByPatient: StateFlow<Map<Long, List<Visit>>> = _visitsByPatient

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _generatedFile = MutableStateFlow<java.io.File?>(null)
    val generatedFile: StateFlow<java.io.File?> = _generatedFile

    val results: StateFlow<List<Patient>> = combine(
        _filters,
        _allPatients,
        _visitsByPatient,
    ) { filter, patients, visits ->
        val q = filter.searchQuery.trim()
        if (q.isBlank()) {
            patients
        } else {
            patients.filter { patient ->
                patientMatches(patient, visits[patient.id].orEmpty(), q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun setSearchQuery(value: String) {
        _filters.value = _filters.value.copy(searchQuery = value)
    }

    fun generatePdf(context: android.content.Context) {
        viewModelScope.launch {
            _isGenerating.value = true
            val filter = _filters.value
            val patients = results.value
            val visitsMap = patients.associate { patient ->
                patient.id to reportVisits(patient, filter, _visitsByPatient.value[patient.id].orEmpty())
            }
            val title = reportTitle(filter)
            val file = PdfGenerator.generate(context.applicationContext, title, patients, visitsMap)
            _generatedFile.value = file
            _isGenerating.value = false
        }
    }

    fun consumeGeneratedFile() {
        _generatedFile.value = null
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

    fun reportVisits(patient: Patient, filter: ReportFilters, visits: List<Visit>): List<Visit> {
        return visits
    }

    fun reportTitle(filter: ReportFilters): String {
        val q = filter.searchQuery.trim()
        return if (q.isBlank()) "All Patient Records" else "Search results for: $q"
    }
}
