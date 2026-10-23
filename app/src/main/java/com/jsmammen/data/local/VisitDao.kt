package com.jsmammen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsmammen.data.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Insert
    suspend fun insert(visit: Visit): Long

    @Update
    suspend fun update(visit: Visit)

    @Query("SELECT * FROM visits WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun observeForPatient(patientId: Long): Flow<List<Visit>>

    @Query("SELECT * FROM visits ORDER BY recordedAt DESC")
    fun observeAll(): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getById(id: Long): Visit?

    @Query("SELECT * FROM visits")
    suspend fun getAll(): List<Visit>

    @Query("DELETE FROM visits")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM visits WHERE patientId = :patientId")
    suspend fun countForPatient(patientId: Long): Int
}
