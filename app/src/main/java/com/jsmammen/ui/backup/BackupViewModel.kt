package com.jsmammen.ui.backup

import android.content.Context
import android.net.Uri
import com.jsmammen.OutpatientApp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupViewModel(
    private val repository: OutpatientRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    private val _exportedUri = MutableStateFlow<Uri?>(null)
    val exportedUri = _exportedUri.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    fun export(context: Context) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val patients = repository.getAllPatients()
                val visits = repository.getAllVisits()
                val json = JSONObject().apply {
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("patients", JSONArray(patients.map { it.toJson() }))
                    put("visits", JSONArray(visits.map { it.toJson() }))
                }
                val jsonText = json.toString(2)
                val fileName = "OutpatientBackup.json"
                val uri = com.jsmammen.data.backup.BackupStorage.save(context, fileName, jsonText)
                    ?: throw IllegalStateException("Cannot create backup file")
                _exportedUri.value = uri
                _status.value = "Exported ${patients.size} patients and ${visits.size} visits."
            } catch (exception: Exception) {
                _status.value = "Export failed: ${exception.message}"
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun importFrom(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Cannot read file")
                val json = JSONObject(text)
                val patientsJson = json.getJSONArray("patients")
                val visitsJson = json.getJSONArray("visits")
                val patients = (0 until patientsJson.length()).map { patientsJson.getJSONObject(it).toPatient() }
                val visits = (0 until visitsJson.length()).map { visitsJson.getJSONObject(it).toVisit() }
                repository.importAll(patients, visits)
                (context.applicationContext as OutpatientApp).autoBackupManager.suppressNextBackup()
                _status.value = "Imported ${patients.size} patients and ${visits.size} visits."
            } catch (exception: Exception) {
                _status.value = "Import failed: ${exception.message}"
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun consumeExportedUri() {
        _exportedUri.value = null
    }

    fun clearStatus() {
        _status.value = null
    }

    private fun Patient.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("uid", uid)
        put("name", name)
        put("idNumber", idNumber ?: JSONObject.NULL)
        put("ageYears", ageYears ?: JSONObject.NULL)
        put("sex", sex ?: JSONObject.NULL)
        put("phone", phone ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("deletedAt", deletedAt ?: JSONObject.NULL)
    }

    private fun Visit.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("patientId", patientId)
        put("recordedAt", recordedAt)
        put("symptoms", symptoms)
        put("notes", notes)
        put("systolic", systolic ?: JSONObject.NULL)
        put("diastolic", diastolic ?: JSONObject.NULL)
        put("glucoseValue", glucoseValue ?: JSONObject.NULL)
        put("glucoseUnit", glucoseUnit?.name ?: JSONObject.NULL)
        put("pulse", pulse ?: JSONObject.NULL)
        put("temperatureC", temperatureC ?: JSONObject.NULL)
        put("spo2", spo2 ?: JSONObject.NULL)
        put("weightKg", weightKg ?: JSONObject.NULL)
    }

    private fun JSONObject.toPatient(): Patient = Patient(
        id = getLong("id"),
        uid = getString("uid"),
        name = getString("name"),
        idNumber = if (isNull("idNumber")) null else getString("idNumber"),
        ageYears = if (isNull("ageYears")) null else getInt("ageYears"),
        sex = if (isNull("sex")) null else getString("sex"),
        phone = if (isNull("phone")) null else getString("phone"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        deletedAt = if (isNull("deletedAt")) null else getLong("deletedAt"),
    )

    private fun JSONObject.toVisit(): Visit = Visit(
        id = getLong("id"),
        patientId = getLong("patientId"),
        recordedAt = getLong("recordedAt"),
        symptoms = getString("symptoms"),
        notes = getString("notes"),
        systolic = if (isNull("systolic")) null else getInt("systolic"),
        diastolic = if (isNull("diastolic")) null else getInt("diastolic"),
        glucoseValue = if (isNull("glucoseValue")) null else getDouble("glucoseValue"),
        glucoseUnit = if (isNull("glucoseUnit")) null else com.jsmammen.data.model.GlucoseUnit.valueOf(getString("glucoseUnit")),
        pulse = if (isNull("pulse")) null else getInt("pulse"),
        temperatureC = if (isNull("temperatureC")) null else getDouble("temperatureC"),
        spo2 = if (isNull("spo2")) null else getInt("spo2"),
        weightKg = if (isNull("weightKg")) null else getDouble("weightKg"),
    )
}
