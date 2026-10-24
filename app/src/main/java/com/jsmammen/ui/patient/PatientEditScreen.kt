package com.jsmammen.ui.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jsmammen.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditScreen(
    patientId: Long,
    onDone: () -> Unit,
) {
    val repository = rememberRepository()
    val viewModel: PatientEditViewModel = viewModel {
        PatientEditViewModel(repository, patientId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit patient") },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Full name *") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.uid,
                    onValueChange = viewModel::onUidChange,
                    label = { Text("Hospital UID *") },
                    isError = state.uidError != null,
                    supportingText = state.uidError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.idNumber,
                    onValueChange = viewModel::onIdNumberChange,
                    label = { Text("ID (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.age,
                    onValueChange = viewModel::onAgeChange,
                    label = { Text("Age (years)") },
                    isError = state.ageError != null,
                    supportingText = state.ageError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.sex,
                    onValueChange = viewModel::onSexChange,
                    label = { Text("Sex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.saving) "Saving..." else "Save changes")
                }
            }
        }
    }
}
