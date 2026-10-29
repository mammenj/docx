package com.jsmammen.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jsmammen.ui.navigation.LocalNavController

@Composable
fun AppBottomBar(
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val currentRoute by navController.currentBackStackEntryAsState()
    AppBottomBar(
        currentRoute = currentRoute?.destination?.route,
        onPatients = { navController.navigate("patients") { launchSingleTop = true } },
        onReports = { navController.navigate("reports") { launchSingleTop = true } },
        onBackup = { navController.navigate("backup") { launchSingleTop = true } },
        onSettings = { navController.navigate("settings") { launchSingleTop = true } },
        modifier = modifier,
    )
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onPatients: () -> Unit,
    onReports: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == "patients",
            onClick = onPatients,
            icon = { Icon(Icons.Default.Home, contentDescription = "Patients") },
            label = { Text("Patients") },
        )
        NavigationBarItem(
            selected = currentRoute == "reports",
            onClick = onReports,
            icon = { Icon(Icons.Default.Description, contentDescription = "Reports") },
            label = { Text("Reports") },
        )
        NavigationBarItem(
            selected = currentRoute == "backup",
            onClick = onBackup,
            icon = { Icon(Icons.Default.Backup, contentDescription = "Backup") },
            label = { Text("Backup") },
        )
        NavigationBarItem(
            selected = currentRoute == "settings",
            onClick = onSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
        )
    }
}
