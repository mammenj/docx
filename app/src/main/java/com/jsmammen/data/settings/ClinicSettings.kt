package com.jsmammen.data.settings

import android.content.Context

class ClinicSettings(context: Context) {
    private val prefs = context.getSharedPreferences("clinic_settings", Context.MODE_PRIVATE)

    var doctorName: String
        get() = prefs.getString(KEY_DOCTOR_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOCTOR_NAME, value.trim()).apply()

    var doctorContact: String
        get() = prefs.getString(KEY_DOCTOR_CONTACT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOCTOR_CONTACT, value.trim()).apply()

    var hospitalName: String
        get() = prefs.getString(KEY_HOSPITAL_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOSPITAL_NAME, value.trim()).apply()

    companion object {
        private const val KEY_DOCTOR_NAME = "doctor_name"
        private const val KEY_DOCTOR_CONTACT = "doctor_contact"
        private const val KEY_HOSPITAL_NAME = "hospital_name"
    }
}
