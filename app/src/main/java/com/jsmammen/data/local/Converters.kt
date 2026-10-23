package com.jsmammen.data.local

import androidx.room.TypeConverter
import com.jsmammen.data.model.GlucoseUnit

class Converters {
    @TypeConverter
    fun fromGlucoseUnit(unit: GlucoseUnit?): String? = unit?.name

    @TypeConverter
    fun toGlucoseUnit(value: String?): GlucoseUnit? = value?.let(GlucoseUnit::valueOf)
}
