package com.jsmammen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jsmammen.ui.patient.PatientDetailScreen
import com.jsmammen.ui.patient.PatientEditScreen
import com.jsmammen.ui.patient.PatientFormScreen
import com.jsmammen.ui.backup.BackupScreen
import com.jsmammen.ui.patient.PatientListScreen
import com.jsmammen.ui.reports.ReportsScreen
import com.jsmammen.ui.settings.ClinicDetailsScreen
import com.jsmammen.ui.visit.VisitDetailScreen
import com.jsmammen.ui.visit.VisitFormScreen

object Routes {
    const val PATIENT_LIST = "patients"
    const val NEW_PATIENT = "patients/new"
    const val PATIENT_DETAIL = "patients/{patientId}"
    const val NEW_VISIT = "patients/{patientId}/visits/new"
    const val EDIT_PATIENT = "patients/{patientId}/edit"
    const val VISIT_DETAIL = "visits/{visitId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"

    fun patientDetail(patientId: Long) = "patients/$patientId"

    fun newVisit(patientId: Long) = "patients/$patientId/visits/new"

    fun editPatient(patientId: Long) = "patients/$patientId/edit"

    fun visitDetail(visitId: Long) = "visits/$visitId"
}

private const val PATIENT_ID_ARG = "patientId"

@Composable
fun OutpatientNavHost(
    navController: NavHostController = rememberNavController(),
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = Routes.PATIENT_LIST,
        ) {
        composable(Routes.PATIENT_LIST) {
            PatientListScreen(
                onAddPatient = { navController.navigate(Routes.NEW_PATIENT) },
                onPatientClick = { navController.navigate(Routes.patientDetail(it)) },
                onReports = { navController.navigate(Routes.REPORTS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onBackup = { navController.navigate(Routes.BACKUP) },
            )
        }
        composable(Routes.NEW_PATIENT) {
            PatientFormScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.PATIENT_DETAIL,
            arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val patientId = entry.arguments?.getLong(PATIENT_ID_ARG) ?: 0L
            PatientDetailScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() },
                onAddVisit = { navController.navigate(Routes.newVisit(patientId)) },
                onVisitClick = { navController.navigate(Routes.visitDetail(it)) },
                onEdit = { navController.navigate(Routes.editPatient(patientId)) },
            )
        }
        composable(
            route = Routes.NEW_VISIT,
            arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val patientId = entry.arguments?.getLong(PATIENT_ID_ARG) ?: 0L
            VisitFormScreen(
                patientId = patientId,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.VISIT_DETAIL,
            arguments = listOf(navArgument("visitId") { type = NavType.LongType }),
        ) { entry ->
            val visitId = entry.arguments?.getLong("visitId") ?: 0L
            VisitDetailScreen(
                visitId = visitId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDIT_PATIENT,
            arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val patientId = entry.arguments?.getLong(PATIENT_ID_ARG) ?: 0L
            PatientEditScreen(
                patientId = patientId,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            ClinicDetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}
