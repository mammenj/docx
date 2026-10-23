package com.example.outpatient.domain

object PatientValidator {

    fun validateName(name: String): String? =
        if (name.isBlank()) "Patient name is required" else null

    fun validateUid(uid: String): String? =
        if (uid.isBlank()) "Hospital UID is required" else null

    fun validateAge(age: String): String? {
        if (age.isBlank()) return null
        val parsed = age.trim().toIntOrNull() ?: return "Age must be a whole number"
        return if (parsed in 0..130) null else "Age must be between 0 and 130"
    }
}
