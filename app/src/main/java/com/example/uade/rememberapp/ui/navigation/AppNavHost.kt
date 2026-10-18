package com.example.uade.rememberapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.ui.reminders.detail.ReminderDetailScreen

/**
 * Raíz de la app: el grafo de navegación.
 *
 * - [Routes.MAIN]: [MainTabs], con las cuatro pantallas principales como páginas y la barra
 *   inferior.
 * - [Routes.REMINDER_DETAIL]: el detalle de un recordatorio, que se abre encima al tocar una
 *   card de la Home.
 * - [Routes.NEW_NOTE]: una nota nueva ("+" → "Nota"), con la misma pantalla que el detalle.
 * - [Routes.NEW_CHECKLIST]: una lista nueva ("+" → "Lista").
 *
 * Es el único lugar que conoce el [NavHostController]; las pantallas reciben la navegación
 * como lambdas.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.MAIN) {
            MainTabs(
                onReminderClick = { id ->
                    // launchSingleTop: un doble toque rápido no abre el detalle dos veces.
                    navController.navigate(Routes.reminderDetail(id)) { launchSingleTop = true }
                },
                onCreateNote = {
                    navController.navigate(Routes.NEW_NOTE) { launchSingleTop = true }
                },
                onCreateChecklist = {
                    navController.navigate(Routes.NEW_CHECKLIST) { launchSingleTop = true }
                },
            )
        }
        // La misma pantalla que el detalle: sin id en la ruta, el ViewModel arranca vacío.
        composable(Routes.NEW_NOTE) {
            ReminderDetailScreen(
                onBack = { navController.navigateUp() },
                initialType = ReminderType.Note,
            )
        }
        composable(Routes.NEW_CHECKLIST) {
            ReminderDetailScreen(
                onBack = { navController.navigateUp() },
                initialType = ReminderType.Checklist,
            )
        }
        composable(
            route = Routes.REMINDER_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_REMINDER_ID) { type = NavType.LongType }),
        ) {
            ReminderDetailScreen(onBack = { navController.navigateUp() })
        }
    }
}
