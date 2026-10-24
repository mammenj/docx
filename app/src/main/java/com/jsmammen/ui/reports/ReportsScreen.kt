package com.jsmammen.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.ui.rememberRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: ReportsViewModel = viewModel {
        ReportsViewModel(repository)
    }
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val visitsByPatient by viewModel.visitsByPatient.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatedFile by viewModel.generatedFile.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(generatedFile) {
        generatedFile?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Open report"))
            } catch (exception: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Saved: ${file.absolutePath}")
                }
            }
            viewModel.consumeGeneratedFile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Report type", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportType.values().forEach { type ->
                        FilterChip(
                            selected = filters.type == type,
                            onClick = { viewModel.setType(type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
            }

            item {
                when (filters.type) {
                    ReportType.VISITED_ON_DATE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Date: ${ReportsViewModel.formatDate(filters.dateMillis)}")
                            Button(onClick = { showDatePicker = true }) {
                                Text("Pick date")
                            }
                        }
                    }

                    ReportType.SYMPTOMS -> {
                        OutlinedTextField(
                            value = filters.symptomQuery,
                            onValueChange = viewModel::setSymptomQuery,
                            label = { Text("Symptoms contain") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ReportType.DIAGNOSIS -> {
                        OutlinedTextField(
                            value = filters.diagnosisQuery,
                            onValueChange = viewModel::setDiagnosisQuery,
                            label = { Text("Diagnosis/notes contain") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    ReportType.AGE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = filters.ageMin,
                                onValueChange = viewModel::setAgeMin,
                                label = { Text("Min age") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = filters.ageMax,
                                onValueChange = viewModel::setAgeMax,
                                label = { Text("Max age") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    ReportType.METRICS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = filters.systolicMin,
                                onValueChange = viewModel::setSystolicMin,
                                label = { Text("Systolic >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.diastolicMin,
                                onValueChange = viewModel::setDiastolicMin,
                                label = { Text("Diastolic >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.glucoseMin,
                                onValueChange = viewModel::setGlucoseMin,
                                label = { Text("Glucose >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.pulseMin,
                                onValueChange = viewModel::setPulseMin,
                                label = { Text("Pulse >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.tempMin,
                                onValueChange = viewModel::setTempMin,
                                label = { Text("Temperature >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.spo2Max,
                                onValueChange = viewModel::setSpo2Max,
                                label = { Text("SpO2 <=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = filters.weightMin,
                                onValueChange = viewModel::setWeightMin,
                                label = { Text("Weight >=") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            item {
                Text("Scope", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filters.scope == ReportScope.PATIENTS_ONLY,
                        onClick = { viewModel.setScope(ReportScope.PATIENTS_ONLY) },
                        label = { Text("Patients only") },
                    )
                    FilterChip(
                        selected = filters.scope == ReportScope.PATIENTS_AND_VISITS,
                        onClick = { viewModel.setScope(ReportScope.PATIENTS_AND_VISITS) },
                        label = { Text("Patients + visits") },
                    )
                }
            }

            item {
                Text("Results: ${results.size}", style = MaterialTheme.typography.titleSmall)
            }

            items(results.take(25)) { patient ->
                androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(patient.name, style = MaterialTheme.typography.titleSmall)
                        Text("UID ${patient.uid}", style = MaterialTheme.typography.bodySmall)
                        patient.idNumber?.let { Text("ID $it", style = MaterialTheme.typography.bodySmall) }
                        patient.ageYears?.let { Text("Age $it", style = MaterialTheme.typography.bodySmall) }
                        val visits = viewModel.reportVisits(patient, filters, visitsByPatient[patient.id].orEmpty())
                        if (visits.isNotEmpty()) {
                            Text("Visits: ${visits.size}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            visits.take(5).forEach { visit ->
                                val vitals = reportVitalsSummary(visit)
                                Text(
                                    text = "${com.jsmammen.ui.util.formatTimestamp(visit.recordedAt)}" +
                                        if (vitals.isNotEmpty()) " - ${vitals.joinToString(", ")}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (visit.symptoms.isNotBlank()) {
                                    Text("  Symptoms: ${visit.symptoms}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (visit.notes.isNotBlank()) {
                                    Text("  Notes: ${visit.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (visits.size > 5) {
                                Text("...and ${visits.size - 5} more visits", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (results.size > 25) {
                item {
                    Text("...and ${results.size - 25} more")
                }
            }

            item {
                Button(
                    onClick = { viewModel.generatePdf(context) },
                    enabled = results.isNotEmpty() && !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isGenerating) "Generating PDF..." else "Generate PDF")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = filters.dateMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDateMillis(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatMetric(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(java.util.Locale.getDefault(), "%.1f", value)

private fun reportVitalsSummary(visit: com.jsmammen.data.model.Visit): List<String> = buildList {
    if (visit.systolic != null && visit.diastolic != null) {
        add("BP ${visit.systolic}/${visit.diastolic}")
    }
    if (visit.glucoseValue != null && visit.glucoseUnit != null) {
        add("Glu ${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}")
    }
    visit.pulse?.let { add("Pulse $it") }
    visit.temperatureC?.let { add("Temp ${formatMetric(it)}") }
    visit.spo2?.let { add("SpO2 $it%") }
    visit.weightKg?.let { add("Wt ${formatMetric(it)}") }
}

private fun ReportType.label(): String = when (this) {
    ReportType.VISITED_ON_DATE -> "Visited on date"
    ReportType.SYMPTOMS -> "Symptoms"
    ReportType.DIAGNOSIS -> "Diagnosis"
    ReportType.AGE -> "Age"
    ReportType.METRICS -> "Metrics"
}
