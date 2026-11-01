package com.jsmammen.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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
    val context = LocalContext.current
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val photoPaths = remember(visit.photoPaths, visit.photoPath) {
                visit.photoPaths.ifEmpty { visit.photoPath?.let { listOf(it) } ?: emptyList() }
            }
            val pdfPaths = remember(visit.pdfPaths) { visit.pdfPaths }

            if (photoPaths.isNotEmpty() || pdfPaths.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    photoPaths.forEachIndexed { index, path ->
                        val file = File(path)
                        if (file.exists()) {
                            AssistChip(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file,
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "image/*")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val uri = android.net.Uri.fromFile(file)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "image/*")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No app found to open image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                label = { Text(if (photoPaths.size == 1) "Photo" else "Photo ${index + 1}") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }

                    pdfPaths.forEachIndexed { index, path ->
                        val file = File(path)
                        if (file.exists()) {
                            AssistChip(
                                onClick = {
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
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val uri = android.net.Uri.fromFile(file)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                label = { Text(if (pdfPaths.size == 1) "PDF Doc" else "PDF ${index + 1}") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
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
