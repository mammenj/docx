package com.jsmammen.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.Patient
import com.jsmammen.data.settings.ClinicSettings
import com.jsmammen.ui.backup.BackupViewModel
import com.jsmammen.ui.components.AppScaffold
import com.jsmammen.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onAddPatient: () -> Unit,
    onPatientClick: (Long) -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onBackup: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: PatientListViewModel = viewModel {
        PatientListViewModel(repository)
    }
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val visitsByPatient by viewModel.visitsByPatient.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clinicSettings = remember { ClinicSettings(context) }
    val backupViewModel: BackupViewModel = viewModel { BackupViewModel(repository) }
    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    var showRestoreDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patients) {
        if (patients.isEmpty() && !prefs.getBoolean("restore_prompted", false)) {
            showRestoreDialog = true
        } else if (patients.isNotEmpty()) {
            showRestoreDialog = false
        }
    }

    val restorePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupViewModel.importFrom(context, it) }
    }

    if (showRestoreDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                prefs.edit().putBoolean("restore_prompted", true).apply()
            },
            title = { Text("Restore data?") },
            text = { Text("No patient records found. Restore from a backup file?") },
            confirmButton = {
                TextButton(onClick = { restorePicker.launch(arrayOf("application/json", "text/plain")) }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    prefs.edit().putBoolean("restore_prompted", true).apply()
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    AppScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text("Outpatients", style = MaterialTheme.typography.titleLarge)
                            val subtitle = listOfNotNull(
                                clinicSettings.doctorName.takeIf { it.isNotBlank() }?.let { "Dr. $it" },
                                clinicSettings.hospitalName.takeIf { it.isNotBlank() },
                            ).joinToString("  •  ")
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPatient,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New patient") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search patients or visits") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (patients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (query.isBlank()) {
                                "No patients yet"
                            } else {
                                "No patients match this search"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            text = if (query.isBlank()) {
                                "Tap “New patient” to register the first outpatient."
                            } else {
                                "Try a different name or UID."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(patients, key = { it.id }) { patient ->
                        PatientCard(
                            patient = patient,
                            visits = visitsByPatient[patient.id].orEmpty(),
                            query = query,
                            onClick = { onPatientClick(patient.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: Patient,
    visits: List<com.jsmammen.data.model.Visit>,
    query: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = patient.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "UID ${patient.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val details = listOfNotNull(
                        patient.idNumber?.let { "ID $it" },
                        patient.ageYears?.let { "$it yrs" },
                        patient.sex?.takeIf { it.isNotBlank() },
                    )
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val matchingVisits = if (query.isNotBlank()) {
                visits.filter { visit ->
                    visit.department.contains(query, ignoreCase = true) ||
                        visit.symptoms.contains(query, ignoreCase = true) ||
                        visit.notes.contains(query, ignoreCase = true) ||
                        visit.systolic?.toString()?.contains(query) == true ||
                        visit.diastolic?.toString()?.contains(query) == true ||
                        visit.glucoseValue?.toString()?.contains(query) == true ||
                        visit.pulse?.toString()?.contains(query) == true ||
                        visit.temperatureC?.toString()?.contains(query) == true ||
                        visit.spo2?.toString()?.contains(query) == true ||
                        visit.weightKg?.toString()?.contains(query) == true
                }
            } else {
                emptyList()
            }
            if (matchingVisits.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    matchingVisits.take(3).forEach { visit ->
                        Text(
                            text = "${com.jsmammen.ui.util.formatTimestamp(visit.recordedAt)} - " +
                                listOfNotNull(
                                    "Dept: ${visit.department}",
                                    if (visit.systolic != null && visit.diastolic != null) "BP ${visit.systolic}/${visit.diastolic}" else null,
                                    visit.glucoseValue?.let { "Glu $it" },
                                    visit.symptoms.takeIf { it.isNotBlank() },
                                ).joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (matchingVisits.size > 3) {
                        Text(
                            text = "...and ${matchingVisits.size - 3} more matching visits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}