package com.jsmammen.data.local

import androidx.room.TypeConverter
import com.jsmammen.data.model.GlucoseUnit

class Converters {
    @TypeConverter
    fun fromGlucoseUnit(unit: GlucoseUnit?): String? = unit?.name

    @TypeConverter
    fun toGlucoseUnit(value: String?): GlucoseUnit? = value?.let(GlucoseUnit::valueOf)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString("||")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split("||").filter { it.isNotBlank() }
}
