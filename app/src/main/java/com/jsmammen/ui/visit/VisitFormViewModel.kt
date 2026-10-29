package com.jsmammen.ui.visit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.GlucoseUnit
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import com.jsmammen.domain.VitalsErrors
import com.jsmammen.domain.VitalsInput
import com.jsmammen.domain.VitalsValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VisitFormState(
    val symptoms: String = "",
    val notes: String = "",
    val department: String = "General Medicine",
    val photoPath: String? = null,
    val systolic: String = "",
    val diastolic: String = "",
    val glucoseValue: String = "",
    val glucoseUnit: GlucoseUnit? = null,
    val pulse: String = "",
    val temperatureC: String = "",
    val spo2: String = "",
    val weightKg: String = "",
    val errors: VitalsErrors = VitalsErrors(),
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class VisitFormViewModel(
    private val repository: OutpatientRepository,
    private val patientId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(VisitFormState())
    val state = _state.asStateFlow()

    fun onSymptomsChange(value: String) = _state.update { it.copy(symptoms = value) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun onDepartmentChange(value: String) = _state.update { it.copy(department = value) }

    fun onPhotoChange(path: String?) = _state.update { it.copy(photoPath = path) }

    fun onSystolicChange(value: String) =
        _state.update { it.copy(systolic = value, errors = it.errors.copy(systolic = null)) }

    fun onDiastolicChange(value: String) =
        _state.update { it.copy(diastolic = value, errors = it.errors.copy(diastolic = null)) }

    fun onGlucoseValueChange(value: String) =
        _state.update { it.copy(glucoseValue = value, errors = it.errors.copy(glucoseValue = null)) }

    fun onGlucoseUnitChange(unit: GlucoseUnit) =
        _state.update { it.copy(glucoseUnit = unit, errors = it.errors.copy(glucoseUnit = null)) }

    fun onPulseChange(value: String) =
        _state.update { it.copy(pulse = value, errors = it.errors.copy(pulse = null)) }

    fun onTemperatureChange(value: String) =
        _state.update { it.copy(temperatureC = value, errors = it.errors.copy(temperatureC = null)) }

    fun onSpo2Change(value: String) =
        _state.update { it.copy(spo2 = value, errors = it.errors.copy(spo2 = null)) }

    fun onWeightChange(value: String) =
        _state.update { it.copy(weightKg = value, errors = it.errors.copy(weightKg = null)) }

    fun save() {
        val current = _state.value
        val errors = VitalsValidator.validate(
            VitalsInput(
                systolic = current.systolic,
                diastolic = current.diastolic,
                glucoseValue = current.glucoseValue,
                glucoseUnit = current.glucoseUnit,
                pulse = current.pulse,
                temperatureC = current.temperatureC,
                spo2 = current.spo2,
                weightKg = current.weightKg,
            ),
        )
        if (errors.hasErrors) {
            _state.update { it.copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.addVisit(
                Visit(
                    patientId = patientId,
                    recordedAt = System.currentTimeMillis(),
                    department = current.department.trim().ifBlank { "General Medicine" },
                    photoPath = current.photoPath,
                    symptoms = current.symptoms.trim(),
                    notes = current.notes.trim(),
                    systolic = current.systolic.trim().toIntOrNull(),
                    diastolic = current.diastolic.trim().toIntOrNull(),
                    glucoseValue = current.glucoseValue.trim().toDoubleOrNull(),
                    glucoseUnit = current.glucoseUnit,
                    pulse = current.pulse.trim().toIntOrNull(),
                    temperatureC = current.temperatureC.trim().toDoubleOrNull(),
                    spo2 = current.spo2.trim().toIntOrNull(),
                    weightKg = current.weightKg.trim().toDoubleOrNull(),
                ),
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
