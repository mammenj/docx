package com.jsmammen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jsmammen.data.model.Visit
import com.jsmammen.ui.util.formatTimestamp
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitCard(
    visit: Visit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val photoBitmap = remember(visit.photoPath) {
                visit.photoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }
            }
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap,
                    contentDescription = "Visit photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = formatTimestamp(visit.recordedAt),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Department: ${visit.department}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val vitals = vitalsSummary(visit)
            if (vitals.isNotEmpty()) {
                Text(
                    text = vitals.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (visit.symptoms.isNotBlank()) {
                Text(
                    text = "Symptoms: ${visit.symptoms}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (visit.notes.isNotBlank()) {
                Text(
                    text = "Notes: ${visit.notes}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun vitalsSummary(visit: Visit): List<String> = buildList {
    if (visit.systolic != null && visit.diastolic != null) {
        add("BP ${visit.systolic}/${visit.diastolic} mmHg")
    }
    if (visit.glucoseValue != null && visit.glucoseUnit != null) {
        add("Glucose ${formatMetric(visit.glucoseValue)} ${visit.glucoseUnit.label}")
    }
    visit.pulse?.let { add("Pulse $it bpm") }
    visit.temperatureC?.let { add("Temp ${formatMetric(it)} °C") }
    visit.spo2?.let { add("SpO2 $it%") }
    visit.weightKg?.let { add("Weight ${formatMetric(it)} kg") }
}

private fun formatMetric(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
