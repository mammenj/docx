package com.example.outpatient.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GlucoseUnitTest {

    @Test
    fun mgPerDlToMmolPerL_usesConversionFactor() {
        val converted = GlucoseUnit.MG_PER_DL.convert(180.0, GlucoseUnit.MMOL_PER_L)

        assertEquals(9.9899, converted, 0.001)
    }

    @Test
    fun mmolPerLToMgPerDl_usesConversionFactor() {
        val converted = GlucoseUnit.MMOL_PER_L.convert(10.0, GlucoseUnit.MG_PER_DL)

        assertEquals(180.182, converted, 0.001)
    }

    @Test
    fun sameUnitConversion_returnsOriginalValue() {
        assertEquals(95.0, GlucoseUnit.MG_PER_DL.convert(95.0, GlucoseUnit.MG_PER_DL), 0.0)
    }
}
