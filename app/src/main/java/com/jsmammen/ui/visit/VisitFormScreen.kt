package com.jsmammen.ui.visit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.ui.rememberRepository
import com.jsmammen.ui.util.VoiceInputButton
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    patientId: Long,
    onDone: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: VisitFormViewModel = viewModel {
        VisitFormViewModel(repository, patientId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selected ->
            val path = copyImageToAppStorage(context, selected)
            viewModel.onPhotoChange(path)
        }
    }

    val cameraFile = remember { File(context.cacheDir, "visit_photo_${System.currentTimeMillis()}.jpg") }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.onPhotoChange(cameraFile.absolutePath)
        }
    }

    androidx.compose.runtime.LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New visit") },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Record symptoms and vitals for this visit. Empty vital fields are not saved.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.department,
                onValueChange = viewModel::onDepartmentChange,
                label = { Text("Department") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Symptoms", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.symptoms,
                        onValueChange = viewModel::onSymptomsChange,
                        minLines = 3,
                        modifier = Modifier.weight(1f),
                    )
                    VoiceInputButton { text -> viewModel.onSymptomsChange(state.symptoms + " " + text) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notes / diagnosis", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::onNotesChange,
                        minLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    VoiceInputButton { text -> viewModel.onNotesChange(state.notes + " " + text) }
                }
            }

            Text("Blood pressure", style = MaterialTheme.typography.titleSmall)
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

            Text("Blood glucose", style = MaterialTheme.typography.titleSmall)
            VitalField(
                value = state.glucoseValue,
                onValueChange = viewModel::onGlucoseValueChange,
                label = "Glucose",
                error = state.errors.glucoseValue,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.jsmammen.data.model.GlucoseUnit.entries.forEach { unit ->
                    androidx.compose.material3.FilterChip(
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

            Text("Photo (optional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Gallery")
                }
                Button(onClick = { cameraLauncher.launch(cameraUri) }) {
                    Text("Camera")
                }
            }
            state.photoPath?.let { path ->
                val bitmap = remember(path) {
                    android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap()
                }
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Visit photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(200.dp),
                    )
                }
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

private fun copyImageToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "visit_photos").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
