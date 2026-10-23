package com.jsmammen.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    indices = [Index(value = ["uid"], unique = true)],
)
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val name: String,
    val ageYears: Int? = null,
    val sex: String? = null,
    val phone: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
