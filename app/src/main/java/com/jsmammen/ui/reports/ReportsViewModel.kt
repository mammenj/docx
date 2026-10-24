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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class ReportType {
    VISITED_ON_DATE,
    SYMPTOMS,
    DIAGNOSIS,
    AGE,
    METRICS,
}

enum class ReportScope {
    PATIENTS_ONLY,
    PATIENTS_AND_VISITS,
}

data class ReportFilters(
    val type: ReportType = ReportType.VISITED_ON_DATE,
    val dateMillis: Long = ReportsViewModel.todayMillis(),
    val symptomQuery: String = "",
    val diagnosisQuery: String = "",
    val ageMin: String = "",
    val ageMax: String = "",
    val systolicMin: String = "",
    val diastolicMin: String = "",
    val glucoseMin: String = "",
    val pulseMin: String = "",
    val tempMin: String = "",
    val spo2Max: String = "",
    val weightMin: String = "",
    val scope: ReportScope = ReportScope.PATIENTS_ONLY,
)

class ReportsViewModel(
    private val repository: OutpatientRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(ReportFilters())
    val filters: StateFlow<ReportFilters> = _filters

    private val _allPatients = MutableStateFlow<List<Patient>>(emptyList())
    val _visitsByPatient = MutableStateFlow<Map<Long, List<Visit>>>(emptyMap())
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
        patients.filter { patient ->
            matches(filter, patient, visits[patient.id].orEmpty())
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

    fun setType(type: ReportType) {
        _filters.value = _filters.value.copy(type = type)
    }

    fun setDateMillis(millis: Long) {
        _filters.value = _filters.value.copy(dateMillis = millis)
    }

    fun setSymptomQuery(value: String) {
        _filters.value = _filters.value.copy(symptomQuery = value)
    }

    fun setDiagnosisQuery(value: String) {
        _filters.value = _filters.value.copy(diagnosisQuery = value)
    }

    fun setAgeMin(value: String) {
        _filters.value = _filters.value.copy(ageMin = value)
    }

    fun setAgeMax(value: String) {
        _filters.value = _filters.value.copy(ageMax = value)
    }

    fun setSystolicMin(value: String) {
        _filters.value = _filters.value.copy(systolicMin = value)
    }

    fun setDiastolicMin(value: String) {
        _filters.value = _filters.value.copy(diastolicMin = value)
    }

    fun setGlucoseMin(value: String) {
        _filters.value = _filters.value.copy(glucoseMin = value)
    }

    fun setPulseMin(value: String) {
        _filters.value = _filters.value.copy(pulseMin = value)
    }

    fun setTempMin(value: String) {
        _filters.value = _filters.value.copy(tempMin = value)
    }

    fun setSpo2Max(value: String) {
        _filters.value = _filters.value.copy(spo2Max = value)
    }

    fun setWeightMin(value: String) {
        _filters.value = _filters.value.copy(weightMin = value)
    }

    fun setScope(scope: ReportScope) {
        _filters.value = _filters.value.copy(scope = scope)
    }

    fun generatePdf(context: android.content.Context) {
        viewModelScope.launch {
            _isGenerating.value = true
            val filter = _filters.value
            val patients = results.value
            val visitsMap = if (filter.scope == ReportScope.PATIENTS_ONLY) {
                emptyMap()
            } else {
                patients.associate { patient ->
                    patient.id to reportVisits(patient, filter, _visitsByPatient.value[patient.id].orEmpty())
                }
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

    private fun matches(filter: ReportFilters, patient: Patient, visits: List<Visit>): Boolean {
        return when (filter.type) {
            ReportType.VISITED_ON_DATE -> {
                val (start, end) = dateRange(filter.dateMillis)
                visits.any { it.recordedAt in start..end }
            }

            ReportType.SYMPTOMS -> {
                val query = filter.symptomQuery.trim()
                if (query.isBlank()) false
                else visits.any { it.symptoms.contains(query, ignoreCase = true) }
            }

            ReportType.DIAGNOSIS -> {
                val query = filter.diagnosisQuery.trim()
                if (query.isBlank()) false
                else visits.any { it.notes.contains(query, ignoreCase = true) }
            }

            ReportType.AGE -> {
                val age = patient.ageYears ?: return false
                val min = filter.ageMin.trim().toIntOrNull()
                val max = filter.ageMax.trim().toIntOrNull()
                (min == null || age >= min) && (max == null || age <= max)
            }

            ReportType.METRICS -> {
                visits.any { visitMatchesMetrics(it, filter) }
            }
        }
    }

    fun reportVisits(patient: Patient, filter: ReportFilters, visits: List<Visit>): List<Visit> {
        return when (filter.type) {
            ReportType.VISITED_ON_DATE -> {
                val (start, end) = dateRange(filter.dateMillis)
                visits.filter { it.recordedAt in start..end }
            }

            ReportType.SYMPTOMS -> {
                val query = filter.symptomQuery.trim()
                if (query.isBlank()) visits else visits.filter { it.symptoms.contains(query, ignoreCase = true) }
            }

            ReportType.DIAGNOSIS -> {
                val query = filter.diagnosisQuery.trim()
                if (query.isBlank()) visits else visits.filter { it.notes.contains(query, ignoreCase = true) }
            }

            ReportType.METRICS -> {
                val hasThreshold = filter.systolicMin.isNotBlank() || filter.diastolicMin.isNotBlank() ||
                    filter.glucoseMin.isNotBlank() || filter.pulseMin.isNotBlank() ||
                    filter.tempMin.isNotBlank() || filter.spo2Max.isNotBlank() || filter.weightMin.isNotBlank()
                if (!hasThreshold) visits else visits.filter { visitMatchesMetrics(it, filter) }
            }

            ReportType.AGE -> visits
        }
    }

    private fun visitMatchesMetrics(visit: Visit, filter: ReportFilters): Boolean {
        filter.systolicMin.trim().toIntOrNull()?.let { min ->
            if (visit.systolic == null || visit.systolic < min) return false
        }
        filter.diastolicMin.trim().toIntOrNull()?.let { min ->
            if (visit.diastolic == null || visit.diastolic < min) return false
        }
        filter.glucoseMin.trim().toDoubleOrNull()?.let { min ->
            if (visit.glucoseValue == null || visit.glucoseValue < min) return false
        }
        filter.pulseMin.trim().toIntOrNull()?.let { min ->
            if (visit.pulse == null || visit.pulse < min) return false
        }
        filter.tempMin.trim().toDoubleOrNull()?.let { min ->
            if (visit.temperatureC == null || visit.temperatureC < min) return false
        }
        filter.spo2Max.trim().toIntOrNull()?.let { max ->
            if (visit.spo2 == null || visit.spo2 > max) return false
        }
        filter.weightMin.trim().toDoubleOrNull()?.let { min ->
            if (visit.weightKg == null || visit.weightKg < min) return false
        }
        return true
    }

    private fun reportTitle(filter: ReportFilters): String {
        return when (filter.type) {
            ReportType.VISITED_ON_DATE -> "Patients visited on ${formatDate(filter.dateMillis)}"
            ReportType.SYMPTOMS -> "Patients with symptoms: ${filter.symptomQuery.trim()}"
            ReportType.DIAGNOSIS -> "Patients with diagnosis/notes: ${filter.diagnosisQuery.trim()}"
            ReportType.AGE -> {
                val min = filter.ageMin.trim().ifBlank { "any" }
                val max = filter.ageMax.trim().ifBlank { "any" }
                "Patients aged $min to $max"
            }

            ReportType.METRICS -> "Patients matching metric filters"
        }
    }

    private fun dateRange(millis: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
        val start = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1
        return start to end
    }

    companion object {
        fun todayMillis(): Long =
            LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        fun formatDate(millis: Long): String {
            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            return date.toString()
        }
    }
}
