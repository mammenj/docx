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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.jsmammen.ui.components.VisitCard
import com.jsmammen.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    onAddVisit: () -> Unit,
    onVisitClick: (Long) -> Unit,
    onEdit: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: PatientDetailViewModel = viewModel {
        PatientDetailViewModel(repository, patientId)
    }
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val visits by viewModel.visits.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.name ?: "Patient") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit patient")
                    }
                },
            )
        },
        floatingActionButton = {
            if (patient != null) {
                FloatingActionButton(onClick = onAddVisit) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            }
        },
    ) { padding ->
        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            patient == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Patient not found.")
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    PatientHeader(patient = patient!!)
                }
                item {
                    Text(
                        text = "Visit history",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (visits.isEmpty()) {
                    item {
                        Text(
                            text = "No visits recorded yet. Tap + to add the first visit.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(visits, key = { it.id }) { visit ->
                    VisitCard(visit = visit, onClick = { onVisitClick(visit.id) })
                }
            }
        }
    }
}

@Composable
private fun PatientHeader(patient: Patient) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = patient.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "UID ${patient.uid}",
                style = MaterialTheme.typography.bodyMedium,
            )
            val details = listOfNotNull(
                patient.idNumber?.let { "ID $it" },
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
