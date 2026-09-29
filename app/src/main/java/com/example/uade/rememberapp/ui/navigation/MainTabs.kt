package com.example.uade.rememberapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.audios.AudiosScreen
import com.example.uade.rememberapp.ui.places.list.PlacesListScreen
import com.example.uade.rememberapp.ui.reminders.list.RemindersListScreen
import com.example.uade.rememberapp.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Las cuatro pantallas principales como páginas deslizables, con la barra inferior encima.
 *
 * - Deslizar hacia los costados pasa a la pantalla vecina y la barra se actualiza sola.
 * - Tocar un destino de la barra desliza hasta esa página.
 * - "Atrás" desde otra pestaña vuelve a Inicio; desde Inicio sale de la app.
 *
 * El Scaffold calcula cuánto ocupan la barra de estado y la barra inferior y se lo pasa a
 * cada pantalla como `contentPadding`, así el contenido pasa por detrás de la barra flotante.
 */
@Composable
fun MainTabs(
    modifier: Modifier = Modifier,
) {
    val destinations = AppDestination.entries
    val pagerState = rememberPagerState(pageCount = { destinations.size })
    val scope = rememberCoroutineScope()

    // targetPage cambia apenas el gesto decide a qué página va, así la barra no espera a que
    // termine la animación del deslizamiento.
    val selected = destinations[pagerState.targetPage]

    BackHandler(enabled = pagerState.currentPage != AppDestination.Home.ordinal) {
        scope.launch { pagerState.animateScrollToPage(AppDestination.Home.ordinal) }
    }

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
                    onSelected = { destination ->
                        scope.launch { pagerState.animateScrollToPage(destination.ordinal) }
                    },
                )
            }
        },
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { destinations[it].name },
        ) { page ->
            MainTabPage(
                destination = destinations[page],
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun MainTabPage(
    destination: AppDestination,
    contentPadding: PaddingValues,
) {
    when (destination) {
        AppDestination.Home -> RemindersListScreen(contentPadding = contentPadding)
        AppDestination.Audios -> AudiosScreen(contentPadding = contentPadding)
        AppDestination.Places -> PlacesListScreen(contentPadding = contentPadding)
        AppDestination.Settings -> SettingsScreen(contentPadding = contentPadding)
    }
}
