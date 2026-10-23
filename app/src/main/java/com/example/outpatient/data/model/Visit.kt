package com.example.outpatient.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["patientId"])],
)
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val recordedAt: Long,
    val symptoms: String = "",
    val notes: String = "",
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val glucoseValue: Double? = null,
    val glucoseUnit: GlucoseUnit? = null,
    val pulse: Int? = null,
    val temperatureC: Double? = null,
    val spo2: Int? = null,
    val weightKg: Double? = null,
)
