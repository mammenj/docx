package com.example.outpatient.data.model

/**
 * Blood glucose measurement units. Values are never converted implicitly; a conversion must
 * request an explicit target unit so the original record stays interpretable.
 */
enum class GlucoseUnit(val label: String, val mgDlPerUnit: Double) {
    MG_PER_DL("mg/dL", 1.0),
    MMOL_PER_L("mmol/L", 18.0182),
    ;

    fun convert(value: Double, target: GlucoseUnit): Double =
        if (this == target) value else value * mgDlPerUnit / target.mgDlPerUnit
}
