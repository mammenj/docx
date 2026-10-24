package com.jsmammen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jsmammen.data.model.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(patient: Patient): Long

    @Update
    suspend fun update(patient: Patient)

    @Query("UPDATE patients SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("DELETE FROM patients")
    suspend fun deleteAll()

    @Query("SELECT * FROM patients WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<Patient>>

    @Query("SELECT * FROM patients")
    suspend fun getAll(): List<Patient>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): Patient?

    @Query("SELECT * FROM patients WHERE id = :id")
    fun observeById(id: Long): Flow<Patient?>

    @Query("SELECT * FROM patients WHERE uid = :uid AND deletedAt IS NULL LIMIT 1")
    suspend fun findActiveByUid(uid: String): Patient?

    @Query("SELECT COUNT(*) FROM patients WHERE uid = :uid AND deletedAt IS NULL")
    suspend fun countActiveByUid(uid: String): Int

    @Query("SELECT COUNT(*) FROM patients WHERE uid = :uid AND id != :excludeId AND deletedAt IS NULL")
    suspend fun countActiveByUidExcluding(uid: String, excludeId: Long): Int
}
