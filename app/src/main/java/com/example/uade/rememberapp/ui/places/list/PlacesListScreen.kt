package com.example.uade.rememberapp.ui.places.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.PlaceholderScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Lugares guardados por el usuario. Por ahora es una pantalla template.
 *
 * Ya está conectada al ViewModel para que, cuando haya datos, solo cambie [PlacesListContent].
 */
@Composable
fun PlacesListScreen(
    viewModel: PlacesListViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlacesListContent(
        uiState = uiState,
        contentPadding = contentPadding,
    )
}

/** TODO: lista de lugares (nombre, radio, cantidad de pendientes) y botón para agregar uno. */
@Composable
private fun PlacesListContent(
    uiState: PlacesListUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    PlaceholderScreen(
        title = stringResource(R.string.places_list_title),
        icon = R.drawable.ic_location_on,
        message = stringResource(R.string.places_list_placeholder),
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Preview
@Composable
private fun PlacesListContentPreview() {
    RememberAppTheme {
        PlacesListContent(uiState = PlacesListUiState())
    }
}
