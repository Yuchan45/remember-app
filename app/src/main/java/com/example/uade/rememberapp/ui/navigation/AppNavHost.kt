package com.example.uade.rememberapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uade.rememberapp.ui.audios.AudiosScreen
import com.example.uade.rememberapp.ui.places.list.PlacesListScreen
import com.example.uade.rememberapp.ui.reminders.list.RemindersListScreen
import com.example.uade.rememberapp.ui.settings.SettingsScreen

/**
 * Raíz de la app: la barra de navegación inferior y el grafo con todas las pantallas.
 *
 * Es el único lugar que conoce el [NavHostController]; las pantallas reciben la navegación
 * como lambdas.
 *
 * El Scaffold calcula cuánto ocupan la barra de estado y la barra inferior y se lo pasa a
 * cada pantalla como `contentPadding`. Las pantallas ocupan todo el alto y usan ese padding
 * por dentro, así su contenido pasa por detrás de la barra flotante.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selected = AppDestination.entries.firstOrNull { destination ->
        backStackEntry?.destination?.hierarchy?.any { it.route == destination.route } == true
    } ?: AppDestination.Home

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Cada pantalla dibuja su propio fondo.
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppBottomBar(
                    selected = selected,
                    onSelected = { navController.navigateToTopLevel(it) },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.REMINDERS_LIST,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.REMINDERS_LIST) {
                RemindersListScreen(contentPadding = contentPadding)
            }
            composable(Routes.AUDIOS) {
                AudiosScreen(contentPadding = contentPadding)
            }
            composable(Routes.PLACES_LIST) {
                PlacesListScreen(contentPadding = contentPadding)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(contentPadding = contentPadding)
            }
        }
    }
}

/**
 * Navegación entre destinos de la barra inferior:
 * - vuelve a la Home antes de abrir el destino, así "atrás" siempre lleva a la Home y no se
 *   apilan pantallas al ir y venir;
 * - no abre dos veces el mismo destino si ya está arriba;
 * - guarda y restaura el estado de cada destino (ej. el scroll de la lista).
 */
private fun NavHostController.navigateToTopLevel(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
