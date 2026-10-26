package com.jsmammen.ui.visit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.GlucoseUnit
import com.jsmammen.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    patientId: Long,
    onDone: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: VisitFormViewModel = viewModel(key = "patient_$patientId") {
        VisitFormViewModel(repository, patientId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New visit") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Record symptoms and vitals for this visit. Empty vital fields are not saved.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.symptoms,
                onValueChange = viewModel::onSymptomsChange,
                label = { Text("Symptoms") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes / diagnosis") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Blood pressure",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VitalField(
                    value = state.systolic,
                    onValueChange = viewModel::onSystolicChange,
                    label = "Systolic (mmHg)",
                    error = state.errors.systolic,
                    modifier = Modifier.weight(1f),
                )
                VitalField(
                    value = state.diastolic,
                    onValueChange = viewModel::onDiastolicChange,
                    label = "Diastolic (mmHg)",
                    error = state.errors.diastolic,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Blood glucose",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VitalField(
                    value = state.glucoseValue,
                    onValueChange = viewModel::onGlucoseValueChange,
                    label = "Glucose",
                    error = state.errors.glucoseValue,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlucoseUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = state.glucoseUnit == unit,
                        onClick = { viewModel.onGlucoseUnitChange(unit) },
                        label = { Text(unit.label) },
                    )
                }
            }
            state.errors.glucoseUnit?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VitalField(
                    value = state.pulse,
                    onValueChange = viewModel::onPulseChange,
                    label = "Pulse (bpm)",
                    error = state.errors.pulse,
                    modifier = Modifier.weight(1f),
                )
                VitalField(
                    value = state.temperatureC,
                    onValueChange = viewModel::onTemperatureChange,
                    label = "Temp (°C)",
                    error = state.errors.temperatureC,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VitalField(
                    value = state.spo2,
                    onValueChange = viewModel::onSpo2Change,
                    label = "SpO2 (%)",
                    error = state.errors.spo2,
                    modifier = Modifier.weight(1f),
                )
                VitalField(
                    value = state.weightKg,
                    onValueChange = viewModel::onWeightChange,
                    label = "Weight (kg)",
                    error = state.errors.weightKg,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Saving..." else "Save visit")
            }
        }
    }
}

@Composable
private fun VitalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    )
}
