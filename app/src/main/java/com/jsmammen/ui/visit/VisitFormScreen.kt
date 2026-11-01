package com.jsmammen.ui.visit

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.data.model.GlucoseUnit
import com.jsmammen.ui.components.AppScaffold
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

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            copyImageToAppStorage(context, uri)?.let { path ->
                viewModel.onPhotoAdd(path)
            }
        }
    }

    val cameraFile = remember { File(context.cacheDir, "visit_photo_${System.currentTimeMillis()}.jpg") }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            viewModel.onPhotoAdd(cameraFile.absolutePath)
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    AppScaffold(
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
                .verticalScroll(rememberScrollState())
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                GlucoseUnit.entries.forEach { unit ->
                    FilterChip(
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

            Text("Photos (optional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Add from Gallery")
                }
                Button(onClick = { cameraLauncher.launch(cameraUri) }) {
                    Text("Take Photo")
                }
            }

            if (state.photoPaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photoPaths, key = { it }) { path ->
                        val bitmap = remember(path) {
                            android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap()
                        }
                        bitmap?.let { bm ->
                            Box(modifier = Modifier.size(100.dp)) {
                                Image(
                                    bitmap = bm,
                                    contentDescription = "Visit photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                IconButton(
                                    onClick = { viewModel.onPhotoRemove(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
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
        val file = File(dir, "photo_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
