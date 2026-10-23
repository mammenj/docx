package com.example.outpatient.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.outpatient.data.model.Patient
import com.example.outpatient.data.model.Visit

@Database(
    entities = [Patient::class, Visit::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OutpatientDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao

    abstract fun visitDao(): VisitDao

    companion object {
        private const val DATABASE_NAME = "outpatient.db"

        @Volatile
        private var instance: OutpatientDatabase? = null

        fun get(context: Context): OutpatientDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OutpatientDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
