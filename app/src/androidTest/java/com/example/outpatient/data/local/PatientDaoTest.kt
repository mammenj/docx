package com.example.outpatient.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.outpatient.data.model.Patient
import com.example.outpatient.data.model.Visit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatientDaoTest {

    private lateinit var database: OutpatientDatabase
    private lateinit var patientDao: PatientDao
    private lateinit var visitDao: VisitDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, OutpatientDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        patientDao = database.patientDao()
        visitDao = database.visitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedPatient_appearsInActiveList() = runBlocking {
        patientDao.insert(patient(uid = "UID-1", name = "Asha Rao"))

        val patients = patientDao.observeActive().first()

        assertEquals(1, patients.size)
        assertEquals("Asha Rao", patients.first().name)
    }

    @Test
    fun duplicateUid_isRejected() = runBlocking {
        patientDao.insert(patient(uid = "UID-1", name = "Asha Rao"))

        try {
            patientDao.insert(patient(uid = "UID-1", name = "Different Name"))
            fail("Expected a unique constraint violation")
        } catch (expected: SQLiteConstraintException) {
            // Expected.
        }
    }

    @Test
    fun softDeletedPatient_isHiddenFromActiveList() = runBlocking {
        val id = patientDao.insert(patient(uid = "UID-2", name = "Ravi Kumar"))

        patientDao.softDelete(id, now = 2_000L)

        assertTrue(patientDao.observeActive().first().isEmpty())
        assertEquals(0, patientDao.countActiveByUid("UID-2"))
    }

    @Test
    fun visitsAreScopedToTheirPatient() = runBlocking {
        val first = patientDao.insert(patient(uid = "UID-3", name = "First"))
        val second = patientDao.insert(patient(uid = "UID-4", name = "Second"))
        visitDao.insert(visit(first, symptoms = "Fever"))
        visitDao.insert(visit(second, symptoms = "Cough"))

        val firstVisits = visitDao.observeForPatient(first).first()

        assertEquals(1, firstVisits.size)
        assertEquals("Fever", firstVisits.first().symptoms)
    }

    private fun patient(uid: String, name: String) = Patient(
        uid = uid,
        name = name,
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )

    private fun visit(patientId: Long, symptoms: String) = Visit(
        patientId = patientId,
        recordedAt = 1_000L,
        symptoms = symptoms,
    )
}
