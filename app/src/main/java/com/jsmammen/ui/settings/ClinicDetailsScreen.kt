package com.jsmammen.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.jsmammen.ui.components.AppScaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.jsmammen.data.settings.ClinicSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicDetailsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { ClinicSettings(context) }
    var doctorName by remember { mutableStateOf(settings.doctorName) }
    var doctorContact by remember { mutableStateOf(settings.doctorContact) }
    var hospitalName by remember { mutableStateOf(settings.hospitalName) }
    var saved by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinic details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                text = "These details appear in generated PDF reports.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = doctorName,
                onValueChange = { doctorName = it; saved = false },
                label = { Text("Doctor name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = doctorContact,
                onValueChange = { doctorContact = it; saved = false },
                label = { Text("Doctor contact (phone/email)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = hospitalName,
                onValueChange = { hospitalName = it; saved = false },
                label = { Text("Hospital / clinic name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    settings.doctorName = doctorName
                    settings.doctorContact = doctorContact
                    settings.hospitalName = hospitalName
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            if (saved) {
                Text(
                    text = "Clinic details saved.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            val versionName = remember(context) {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.9"
                } catch (_: Exception) {
                    "1.9"
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Outpatient Records v$versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val uriHandler = LocalUriHandler.current
                TextButton(onClick = { uriHandler.openUri("https://jsonresume.org/@mammenj") }) {
                    Text("For Support: @mammenj")
                }
            }
        }
    }
}
