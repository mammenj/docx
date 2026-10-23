package com.example.outpatient.data.local

import androidx.room.TypeConverter
import com.example.outpatient.data.model.GlucoseUnit

class Converters {
    @TypeConverter
    fun fromGlucoseUnit(unit: GlucoseUnit?): String? = unit?.name

    @TypeConverter
    fun toGlucoseUnit(value: String?): GlucoseUnit? = value?.let(GlucoseUnit::valueOf)
}
