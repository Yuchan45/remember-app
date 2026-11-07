package com.example.uade.rememberapp.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.ui.reminders.detail.ReminderDetailScreen
import com.example.uade.rememberapp.ui.reminders.message.ScheduledMessageScreen

/**
 * Raíz de la app: el grafo de navegación.
 *
 * - [Routes.MAIN]: [MainTabs], con las cuatro pantallas principales como páginas y la barra
 *   inferior.
 * - [Routes.REMINDER_DETAIL]: el detalle de un recordatorio, que se abre encima al tocar una
 *   card de la Home o del Historial. Tocar un chip de la card (hora, lugar, etiquetas) lo abre con
 *   ese modal ya abierto.
 * - [Routes.NEW_NOTE]: una nota nueva ("+" → "Nota"), con la misma pantalla que el detalle. Puede
 *   arrancar con una lista vacía (botón de lista de la nota rápida).
 * - [Routes.NEW_SCHEDULED_MESSAGE]: un mensaje programado nuevo ("+" → "Mensaje programado").
 * - [Routes.SCHEDULED_MESSAGE_DETAIL]: detalle/edición de un mensaje programado.
 *
 * Es el único lugar que conoce el [NavHostController]; las pantallas reciben la navegación
 * como lambdas. [reminderToOpen] llega de MainActivity cuando se toca una notificación.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    /** La ruta a abrir porque se tocó una notificación (ver MainActivity), o null. */
    reminderToOpen: String? = null,
    onReminderOpened: () -> Unit = {},
) {
    // Una notificación pidió abrir un recordatorio: se abre su detalle encima de lo que haya.
    LaunchedEffect(reminderToOpen) {
        val route = reminderToOpen ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        onReminderOpened()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.MAIN) {
            MainTabs(
                onReminderClick = { id, type ->
                    val route = if (type == ReminderType.ScheduledMessage) {
                        Routes.scheduledMessageDetail(id)
                    } else {
                        Routes.reminderDetail(id)
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onReminderSectionClick = { id, section ->
                    navController.navigate(Routes.reminderDetail(id, section)) { launchSingleTop = true }
                },
                onCreateNote = { withList, title ->
                    navController.navigate(Routes.newNote(withList, title)) { launchSingleTop = true }
                },
                onCreateScheduledMessage = {
                    navController.navigate(Routes.NEW_SCHEDULED_MESSAGE) { launchSingleTop = true }
                },
            )
        }
        // La misma pantalla que el detalle: sin id en la ruta, el ViewModel arranca vacío.
        composable(
            route = Routes.NEW_NOTE,
            arguments = listOf(
                navArgument(Routes.ARG_WITH_LIST) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Routes.ARG_TITLE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ReminderDetailScreen(onBack = { navController.navigateUp() })
        }
        composable(Routes.NEW_SCHEDULED_MESSAGE) {
            ScheduledMessageScreen(
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            route = Routes.SCHEDULED_MESSAGE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_REMINDER_ID) { type = NavType.LongType }),
        ) {
            ScheduledMessageScreen(
                onBack = { navController.navigateUp() },
            )
        }
        composable(
            route = Routes.REMINDER_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_REMINDER_ID) { type = NavType.LongType },
                navArgument(Routes.ARG_OPEN_SECTION) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ReminderDetailScreen(onBack = { navController.navigateUp() })
        }
    }
}
