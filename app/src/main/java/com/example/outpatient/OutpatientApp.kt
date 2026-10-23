package com.example.outpatient

import android.app.Application
import com.example.outpatient.data.local.OutpatientDatabase
import com.example.outpatient.data.repository.OutpatientRepository

class OutpatientApp : Application() {

    val repository: OutpatientRepository by lazy {
        val database = OutpatientDatabase.get(this)
        OutpatientRepository(database.patientDao(), database.visitDao())
    }
}
