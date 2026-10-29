package com.jsmammen.ui.components

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jsmammen.ui.navigation.LocalNavController

@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val navController = LocalNavController.current
    val currentRoute by navController.currentBackStackEntryAsState()
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute?.destination?.route,
                onPatients = { navController.navigate("patients") { launchSingleTop = true } },
                onReports = { navController.navigate("reports") { launchSingleTop = true } },
                onBackup = { navController.navigate("backup") { launchSingleTop = true } },
                onSettings = { navController.navigate("settings") { launchSingleTop = true } },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
