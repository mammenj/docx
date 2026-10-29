package com.jsmammen.data.backup

import android.content.Context
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.repository.OutpatientRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AutoBackupManager(
    private val context: Context,
    private val repository: OutpatientRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var suppressUntil = 0L

    fun suppressNextBackup() {
        suppressUntil = System.currentTimeMillis() + 10_000
    }

    fun start() {
        scope.launch {
            combine(
                repository.observePatients(),
                repository.observeAllVisits(),
            ) { patients, visits -> patients to visits }
                .drop(1)
                .debounce(2_000)
                .collect { (patients, visits) ->
                    if (System.currentTimeMillis() < suppressUntil) return@collect
                    writeBackup(patients, visits)
                }
        }
    }

    private fun writeBackup(patients: List<Patient>, visits: List<Visit>) {
        try {
            val json = JSONObject().apply {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("patients", JSONArray(patients.map { it.toJson() }))
                put("visits", JSONArray(visits.map { it.toJson() }))
            }
            BackupStorage.save(context, BackupStorage.AUTO_BACKUP_NAME, json.toString(2))
        } catch (_: Exception) {
            // Automatic backup must never crash the app.
        }
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
        put("department", department)
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
        put("photoPath", photoPath ?: JSONObject.NULL)
        val photoBase64 = photoPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP) else null
        }
        put("photoBase64", photoBase64 ?: JSONObject.NULL)
    }
}
