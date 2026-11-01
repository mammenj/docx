package com.jsmammen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit

@Database(
    entities = [Patient::class, Visit::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OutpatientDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao

    abstract fun visitDao(): VisitDao

    companion object {
        private const val DATABASE_NAME = "outpatient.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patients ADD COLUMN idNumber TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN department TEXT NOT NULL DEFAULT 'General Medicine'")
                db.execSQL("ALTER TABLE visits ADD COLUMN photoPath TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN photoPaths TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE visits SET photoPaths = photoPath WHERE photoPath IS NOT NULL AND photoPath != ''")
            }
        }

        @Volatile
        private var instance: OutpatientDatabase? = null

        fun get(context: Context): OutpatientDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    OutpatientDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
