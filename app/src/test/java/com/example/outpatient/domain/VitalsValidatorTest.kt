package com.example.outpatient.domain

import com.example.outpatient.data.model.GlucoseUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsValidatorTest {

    @Test
    fun blankVitals_areValid() {
        val errors = VitalsValidator.validate(VitalsInput())

        assertFalse(errors.hasErrors)
    }

    @Test
    fun oneSidedBloodPressure_isRejected() {
        val errors = VitalsValidator.validate(VitalsInput(systolic = "120"))

        assertEquals("Enter both systolic and diastolic", errors.systolic)
        assertEquals("Enter both systolic and diastolic", errors.diastolic)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun outOfRangeBloodPressure_isRejected() {
        val errors = VitalsValidator.validate(
            VitalsInput(systolic = "320", diastolic = "90"),
        )

        assertNotNull(errors.systolic)
        assertNull(errors.diastolic)
    }

    @Test
    fun nonNumericVitals_areRejected() {
        val errors = VitalsValidator.validate(
            VitalsInput(pulse = "fast", temperatureC = "warm"),
        )

        assertEquals("Pulse must be a number", errors.pulse)
        assertEquals("Temperature must be a number", errors.temperatureC)
    }

    @Test
    fun glucoseWithoutUnit_isRejected() {
        val errors = VitalsValidator.validate(VitalsInput(glucoseValue = "140"))

        assertNull(errors.glucoseValue)
        assertEquals("Select a glucose unit", errors.glucoseUnit)
    }

    @Test
    fun glucoseWithUnit_isAccepted() {
        val errors = VitalsValidator.validate(
            VitalsInput(glucoseValue = "140", glucoseUnit = GlucoseUnit.MG_PER_DL),
        )

        assertFalse(errors.hasErrors)
    }

    @Test
    fun zeroGlucose_isRejected() {
        val errors = VitalsValidator.validate(
            VitalsInput(glucoseValue = "0", glucoseUnit = GlucoseUnit.MG_PER_DL),
        )

        assertEquals("Glucose must be greater than zero", errors.glucoseValue)
    }

    @Test
    fun fullValidVitals_areAccepted() {
        val errors = VitalsValidator.validate(
            VitalsInput(
                systolic = "120",
                diastolic = "80",
                glucoseValue = "5.6",
                glucoseUnit = GlucoseUnit.MMOL_PER_L,
                pulse = "72",
                temperatureC = "36.8",
                spo2 = "98",
                weightKg = "70.5",
            ),
        )

        assertFalse(errors.hasErrors)
    }
}
