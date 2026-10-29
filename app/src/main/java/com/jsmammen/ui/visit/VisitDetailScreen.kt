package com.jsmammen.ui.visit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.ui.rememberRepository
import com.jsmammen.ui.util.formatTimestamp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visitId: Long,
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: VisitDetailViewModel = viewModel(key = "visit_$visitId") {
        VisitDetailViewModel(repository, visitId)
    }
    val visit by viewModel.visit.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val newNote by viewModel.newNote.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
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

            visit == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Visit not found.")
            }

            else -> {
                val currentVisit = visit!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Recorded on",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = formatTimestamp(currentVisit.recordedAt),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Department: ${currentVisit.department}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val vitals = vitalsSummary(currentVisit)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Vitals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (vitals.isEmpty()) {
                                Text(
                                    text = "No vitals recorded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                vitals.forEach { (label, value) ->
                                    Text(
                                        text = "$label: $value",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    currentVisit.photoPath?.let { path ->
                        val file = java.io.File(path)
                        if (file.exists()) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            bitmap?.let {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Image(
                                        bitmap = it,
                                        contentDescription = "Visit photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .size(240.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Symptoms",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = currentVisit.symptoms.ifBlank { "No symptoms recorded." },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = currentVisit.notes.ifBlank { "No notes recorded." },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Add follow-up note",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = newNote,
                                onValueChange = viewModel::onNewNoteChange,
                                label = { Text("Additional note") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = viewModel::addNote,
                                enabled = newNote.isNotBlank() && !saving,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (saving) "Saving..." else "Add note")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun vitalsSummary(visit: com.jsmammen.data.model.Visit): List<Pair<String, String>> =
    buildList {
        if (visit.systolic != null && visit.diastolic != null) {
            add("Blood pressure" to "${visit.systolic}/${visit.diastolic} mmHg")
        }
        if (visit.glucoseValue != null && visit.glucoseUnit != null) {
            add("Blood glucose" to "${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}")
        }
        visit.pulse?.let { add("Pulse" to "$it bpm") }
        visit.temperatureC?.let { add("Temperature" to "${formatMetric(it)} °C") }
        visit.spo2?.let { add("SpO2" to "$it%") }
        visit.weightKg?.let { add("Weight" to "${formatMetric(it)} kg") }
    }

private fun formatMetric(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
