package com.jsmammen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatientValidatorTest {

    @Test
    fun nameAndUidCannotBeBlank() {
        assertEquals("Patient name is required", PatientValidator.validateName("  "))
        assertEquals("Hospital UID is required", PatientValidator.validateUid(""))
    }

    @Test
    fun ageMayBeBlank() {
        assertNull(PatientValidator.validateAge(""))
    }

    @Test
    fun ageMustBeANumber() {
        assertEquals("Age must be a whole number", PatientValidator.validateAge("forty"))
    }

    @Test
    fun ageMustBeInPlausibleRange() {
        assertNull(PatientValidator.validateAge("0"))
        assertNull(PatientValidator.validateAge("130"))
        assertEquals("Age must be between 0 and 130", PatientValidator.validateAge("131"))
    }
}
