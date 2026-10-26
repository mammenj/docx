package com.jsmammen.ui.reports

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.Patient
import com.jsmammen.data.model.Visit
import com.jsmammen.data.settings.ClinicSettings
import com.jsmammen.ui.rememberRepository
import kotlinx.coroutines.launch
import java.util.Locale

@Preview(showBackground = true)
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Report type", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportType.entries.forEach { type ->
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
                Text("Document Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                PdfDocumentPreview(
                    title = viewModel.reportTitle(filters),
                    patients = results,
                    visitsByPatient = visitsByPatient,
                    filters = filters,
                    viewModel = viewModel,
                )
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

@Composable
private fun PdfDocumentPreview(
    title: String,
    patients: List<Patient>,
    visitsByPatient: Map<Long, List<Visit>>,
    filters: ReportFilters,
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = remember(context) { ClinicSettings(context) }
    val hospitalName = settings.hospitalName.ifBlank { "Hospital / Clinic" }
    val doctorName = settings.doctorName.ifBlank { "Doctor" }
    val doctorContact = settings.doctorContact

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E6F0)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Document Header
            Text(
                text = hospitalName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F4C81),
            )
            Text(
                text = doctorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
            )
            if (doctorContact.isNotBlank()) {
                Text(
                    text = doctorContact,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF60708A),
                )
            }

            HorizontalDivider(
                color = Color(0xFF0F4C81),
                thickness = 1.5.dp,
            )

            // Report Details
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Text(
                text = "Generated ${com.jsmammen.ui.util.formatTimestamp(System.currentTimeMillis())}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF60708A),
            )
            Text(
                text = "Patients: ${patients.size}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF60708A),
            )

            if (patients.isEmpty()) {
                Text(
                    text = "No patients match this criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                patients.forEach { patient ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = patient.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                        val meta = listOfNotNull(
                            "UID: ${patient.uid}",
                            patient.idNumber?.let { "ID: $it" },
                            patient.ageYears?.let { "Age: $it" },
                            patient.sex?.takeIf { it.isNotBlank() }?.let { "Sex: $it" },
                            patient.phone?.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
                        ).joinToString("   |   ")
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF60708A),
                        )

                        val visits = if (filters.scope == ReportScope.PATIENTS_ONLY) {
                            emptyList()
                        } else {
                            viewModel.reportVisits(patient, filters, visitsByPatient[patient.id].orEmpty())
                        }

                        if (visits.isNotEmpty()) {
                            // Table Container with Horizontal Scroll for Responsive Alignment
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                Column {
                                    // Table Header Row
                                    Surface(
                                        color = Color(0xFF0F4C81),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            TableHeaderCell("Date", 110.dp)
                                            TableHeaderCell("BP (mmHg)", 75.dp)
                                            TableHeaderCell("Glucose", 85.dp)
                                            TableHeaderCell("Pulse", 55.dp)
                                            TableHeaderCell("Temp (°C)", 65.dp)
                                            TableHeaderCell("SpO2", 55.dp)
                                            TableHeaderCell("Weight (kg)", 80.dp)
                                        }
                                    }

                                    // Table Rows
                                    visits.forEach { visit ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            TableCell(com.jsmammen.ui.util.formatTimestamp(visit.recordedAt), 110.dp)
                                            TableCell(
                                                if (visit.systolic != null && visit.diastolic != null) "${visit.systolic}/${visit.diastolic}" else "-",
                                                75.dp,
                                            )
                                            TableCell(
                                                if (visit.glucoseValue != null && visit.glucoseUnit != null) "${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}" else "-",
                                                85.dp,
                                            )
                                            TableCell(visit.pulse?.toString() ?: "-", 55.dp)
                                            TableCell(visit.temperatureC?.let { formatMetric(it) } ?: "-", 65.dp)
                                            TableCell(visit.spo2?.let { "$it%" } ?: "-", 55.dp)
                                            TableCell(visit.weightKg?.let { formatMetric(it) } ?: "-", 80.dp)
                                        }
                                        HorizontalDivider(color = Color(0xFFE0E6F0), thickness = 0.5.dp)
                                    }
                                }
                            }
                        } else if (filters.scope == ReportScope.PATIENTS_AND_VISITS) {
                            Text(
                                text = "No visits in this report.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF60708A),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        // Visit Notes & Symptoms Section
                        val visitsWithText = visits.filter { it.symptoms.isNotBlank() || it.notes.isNotBlank() }
                        if (visitsWithText.isNotEmpty()) {
                            Text(
                                text = "Visit notes:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            visitsWithText.forEach { visit ->
                                Text(
                                    text = "- ${com.jsmammen.ui.util.formatTimestamp(visit.recordedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (visit.symptoms.isNotBlank()) {
                                    Text(
                                        text = "  Symptoms: ${visit.symptoms}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (visit.notes.isNotBlank()) {
                                    Text(
                                        text = "  Notes: ${visit.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFDCE4F0),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // PDF Footer
            HorizontalDivider(
                color = Color(0xFF0F4C81),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = hospitalName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60708A),
                )
                Text(
                    text = "Confidential - Patient Data",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60708A),
                )
                Text(
                    text = "Page 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60708A),
                )
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.width(width).padding(horizontal = 2.dp),
    )
}

@Composable
private fun TableCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Black,
        modifier = Modifier.width(width).padding(horizontal = 2.dp),
    )
}

private fun formatMetric(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.getDefault(), "%.1f", value)

private fun ReportType.label(): String = when (this) {
    ReportType.VISITED_ON_DATE -> "Visited on date"
    ReportType.SYMPTOMS -> "Symptoms"
    ReportType.DIAGNOSIS -> "Diagnosis"
    ReportType.AGE -> "Age"
    ReportType.METRICS -> "Metrics"
}