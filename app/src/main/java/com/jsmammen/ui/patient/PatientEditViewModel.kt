package com.jsmammen.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.repository.OutpatientRepository
import com.jsmammen.domain.PatientValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientEditState(
    val name: String = "",
    val uid: String = "",
    val idNumber: String = "",
    val age: String = "",
    val sex: String = "",
    val phone: String = "",
    val nameError: String? = null,
    val uidError: String? = null,
    val ageError: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class PatientEditViewModel(
    private val repository: OutpatientRepository,
    private val patientId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientEditState())
    val state = _state.asStateFlow()

    private var originalPatient: Patient? = null

    init {
        viewModelScope.launch {
            val patient = repository.getPatient(patientId)
            originalPatient = patient
            if (patient != null) {
                _state.value = PatientEditState(
                    name = patient.name,
                    uid = patient.uid,
                    idNumber = patient.idNumber.orEmpty(),
                    age = patient.ageYears?.toString().orEmpty(),
                    sex = patient.sex.orEmpty(),
                    phone = patient.phone.orEmpty(),
                    loading = false,
                )
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }

    fun onUidChange(value: String) = _state.update { it.copy(uid = value, uidError = null) }

    fun onIdNumberChange(value: String) = _state.update { it.copy(idNumber = value) }

    fun onAgeChange(value: String) = _state.update { it.copy(age = value, ageError = null) }

    fun onSexChange(value: String) = _state.update { it.copy(sex = value) }

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value) }

    fun save() {
        val current = _state.value
        val nameError = PatientValidator.validateName(current.name)
        val uidError = PatientValidator.validateUid(current.uid)
        val ageError = PatientValidator.validateAge(current.age)

        if (nameError != null || uidError != null || ageError != null) {
            _state.update {
                it.copy(nameError = nameError, uidError = uidError, ageError = ageError)
            }
            return
        }

        val original = originalPatient ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val uid = current.uid.trim()
            if (repository.isUidTaken(uid, excludingPatientId = original.id)) {
                _state.update { it.copy(saving = false, uidError = "UID already exists") }
                return@launch
            }

            val now = System.currentTimeMillis()
            repository.savePatient(
                original.copy(
                    uid = uid,
                    name = current.name.trim(),
                    idNumber = current.idNumber.trim().ifBlank { null },
                    ageYears = current.age.trim().toIntOrNull(),
                    sex = current.sex.trim().ifBlank { null },
                    phone = current.phone.trim().ifBlank { null },
                    updatedAt = now,
                ),
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
