package com.jsmammen.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.Patient
import com.jsmammen.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onAddPatient: () -> Unit,
    onPatientClick: (Long) -> Unit,
    onReports: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: PatientListViewModel = viewModel {
        PatientListViewModel(repository)
    }
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Outpatients") },
                actions = {
                    TextButton(onClick = onReports) {
                        Text("Reports")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPatient) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
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
                    Text(
                        text = if (query.isBlank()) {
                            "No patients yet.\nTap + to register the first patient."
                        } else {
                            "No patients match this search."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = patient.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "UID ${patient.uid}",
                style = MaterialTheme.typography.bodySmall,
            )
            val details = listOfNotNull(
                patient.ageYears?.let { "$it yrs" },
                patient.sex?.takeIf { it.isNotBlank() },
                patient.phone?.takeIf { it.isNotBlank() },
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
