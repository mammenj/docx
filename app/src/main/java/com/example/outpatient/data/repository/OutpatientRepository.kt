package com.example.outpatient.data.repository

import com.example.outpatient.data.local.PatientDao
import com.example.outpatient.data.local.VisitDao
import com.example.outpatient.data.model.Patient
import com.example.outpatient.data.model.Visit
import kotlinx.coroutines.flow.Flow

class OutpatientRepository(
    private val patientDao: PatientDao,
    private val visitDao: VisitDao,
) {

    fun observePatients(): Flow<List<Patient>> = patientDao.observeActive()

    fun observeVisits(patientId: Long): Flow<List<Visit>> = visitDao.observeForPatient(patientId)

    suspend fun getPatient(id: Long): Patient? = patientDao.getById(id)

    suspend fun isUidTaken(uid: String, excludingPatientId: Long? = null): Boolean =
        if (excludingPatientId == null) {
            patientDao.countActiveByUid(uid) > 0
        } else {
            patientDao.countActiveByUidExcluding(uid, excludingPatientId) > 0
        }

    suspend fun savePatient(patient: Patient): Long =
        if (patient.id == 0L) {
            patientDao.insert(patient)
        } else {
            patientDao.update(patient)
            patient.id
        }

    suspend fun softDeletePatient(id: Long, now: Long = System.currentTimeMillis()) =
        patientDao.softDelete(id, now)

    suspend fun addVisit(visit: Visit): Long = visitDao.insert(visit)

    suspend fun updateVisit(visit: Visit) = visitDao.update(visit)
}
