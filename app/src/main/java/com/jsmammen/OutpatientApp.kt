package com.jsmammen

import android.app.Application
import com.jsmammen.data.backup.AutoBackupManager
import com.jsmammen.data.local.OutpatientDatabase
import com.jsmammen.data.repository.OutpatientRepository

class OutpatientApp : Application() {

    val repository: OutpatientRepository by lazy {
        val database = OutpatientDatabase.get(this)
        OutpatientRepository(database.patientDao(), database.visitDao())
    }

    val autoBackupManager: AutoBackupManager by lazy {
        AutoBackupManager(this, repository).also { it.start() }
    }

    override fun onCreate() {
        super.onCreate()
        autoBackupManager
    }
}
