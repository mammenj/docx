package com.jsmammen.domain

import com.jsmammen.data.model.GlucoseUnit

data class VitalsInput(
    val systolic: String = "",
    val diastolic: String = "",
    val glucoseValue: String = "",
    val glucoseUnit: GlucoseUnit? = null,
    val pulse: String = "",
    val temperatureC: String = "",
    val spo2: String = "",
    val weightKg: String = "",
)

data class VitalsErrors(
    val systolic: String? = null,
    val diastolic: String? = null,
    val glucoseValue: String? = null,
    val glucoseUnit: String? = null,
    val pulse: String? = null,
    val temperatureC: String? = null,
    val spo2: String? = null,
    val weightKg: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(
            systolic,
            diastolic,
            glucoseValue,
            glucoseUnit,
            pulse,
            temperatureC,
            spo2,
            weightKg,
        ).any { it != null }
}

/**
 * Validates clinician-entered vitals. Parsing is strict, but plausible-range checks are kept
 * conservative so unusual readings are surfaced rather than silently discarded.
 */
object VitalsValidator {

    fun validate(input: VitalsInput): VitalsErrors {
        val systolic = input.systolic.optInt()
        val diastolic = input.diastolic.optInt()
        val glucose = input.glucoseValue.optDouble()
        val pulse = input.pulse.optInt()
        val temperature = input.temperatureC.optDouble()
        val spo2 = input.spo2.optInt()
        val weight = input.weightKg.optDouble()

        val systolicError = when {
            input.systolic.isNotBlank() && systolic == null -> "Systolic must be a number"
            systolic != null && systolic !in 40..300 -> "Systolic must be 40-300 mmHg"
            else -> null
        }
        val diastolicError = when {
            input.diastolic.isNotBlank() && diastolic == null -> "Diastolic must be a number"
            diastolic != null && diastolic !in 20..200 -> "Diastolic must be 20-200 mmHg"
            else -> null
        }
        val bpPairError =
            if (input.systolic.isBlank() != input.diastolic.isBlank()) {
                "Enter both systolic and diastolic"
            } else {
                null
            }

        val glucoseError = when {
            input.glucoseValue.isNotBlank() && glucose == null -> "Glucose must be a number"
            glucose != null && glucose <= 0.0 -> "Glucose must be greater than zero"
            else -> null
        }
        val unitError =
            if (input.glucoseValue.isNotBlank() && input.glucoseUnit == null) {
                "Select a glucose unit"
            } else {
                null
            }

        val pulseError = when {
            input.pulse.isNotBlank() && pulse == null -> "Pulse must be a number"
            pulse != null && pulse !in 20..250 -> "Pulse must be 20-250 bpm"
            else -> null
        }
        val temperatureError = when {
            input.temperatureC.isNotBlank() && temperature == null -> "Temperature must be a number"
            temperature != null && temperature !in 30.0..45.0 -> "Temperature must be 30-45 C"
            else -> null
        }
        val spo2Error = when {
            input.spo2.isNotBlank() && spo2 == null -> "SpO2 must be a number"
            spo2 != null && spo2 !in 50..100 -> "SpO2 must be 50-100%"
            else -> null
        }
        val weightError = when {
            input.weightKg.isNotBlank() && weight == null -> "Weight must be a number"
            weight != null && weight !in 0.5..500.0 -> "Weight must be 0.5-500 kg"
            else -> null
        }

        return VitalsErrors(
            systolic = systolicError ?: bpPairError,
            diastolic = diastolicError ?: bpPairError,
            glucoseValue = glucoseError,
            glucoseUnit = unitError,
            pulse = pulseError,
            temperatureC = temperatureError,
            spo2 = spo2Error,
            weightKg = weightError,
        )
    }

    private fun String.optInt(): Int? = if (isBlank()) null else trim().toIntOrNull()

    private fun String.optDouble(): Double? = if (isBlank()) null else trim().toDoubleOrNull()
}
