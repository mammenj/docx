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
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.Patient
import com.jsmammen.ui.rememberRepository
import com.jsmammen.ui.backup.BackupViewModel
import com.jsmammen.data.settings.ClinicSettings

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

    val context = LocalContext.current
    val clinicSettings = remember { ClinicSettings(context) }
    val backupViewModel: BackupViewModel = viewModel { BackupViewModel(repository) }
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var showRestoreDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patients) {
        if (patients.isEmpty() && !prefs.getBoolean("restore_prompted", false)) {
            showRestoreDialog = true
        } else if (patients.isNotEmpty()) {
            showRestoreDialog = false
        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { backupViewModel.importFrom(context, it) }
    }

    if (showRestoreDialog) {
        AlertDialog(
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

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Column {
                        Text(
                            text = clinicSettings.hospitalName.ifBlank { "Outpatient Records" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (clinicSettings.doctorName.isNotBlank()) {
                            Text(
                                text = "Dr. ${clinicSettings.doctorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider()
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                NavigationDrawerItem(
                    label = { Text("Reports") },
                    selected = false,
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onReports()
                    },
                )
                NavigationDrawerItem(
                    label = { Text("Clinic details") },
                    selected = false,
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettings()
                    },
                )
                NavigationDrawerItem(
                    label = { Text("Backup & restore") },
                    selected = false,
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onBackup()
                    },
                )
            }
        },
    ) {
        Scaffold(
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
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    scrollBehavior = scrollBehavior,
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
                    label = { Text("Search name or UID") },
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
                                onClick = { onPatientClick(patient.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
    }
}
