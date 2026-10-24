package com.example.uade.rememberapp.ui.places.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.components.DialogConfirmButton
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.PlaceholderMessage
import com.example.uade.rememberapp.ui.places.components.iconRes
import com.example.uade.rememberapp.ui.places.components.labelRes
import com.example.uade.rememberapp.ui.places.edit.PlaceEditSheet
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.appBackground

/**
 * Pantalla principal de la sección Lugares: lista los lugares guardados por el usuario
 * y permite agregar nuevos o editarlos/eliminarlos.
 */
@Composable
fun PlacesListScreen(
    viewModel: PlacesListViewModel = viewModel(factory = PlacesListViewModel.Factory),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Diálogo de confirmación para eliminar lugar
    val placeToDelete = uiState.placeToDelete
    if (placeToDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelDeletePlace,
            shape = AppDialogDefaults.shape,
            containerColor = AppDialogDefaults.containerColor,
            title = {
                Text(
                    text = stringResource(R.string.places_list_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.places_list_delete_confirm_message, placeToDelete.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                DialogConfirmButton(
                    text = stringResource(R.string.places_list_delete_confirm_action),
                    onClick = viewModel::onConfirmDeletePlace,
                )
            },
            dismissButton = {
                DialogDismissButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = viewModel::onCancelDeletePlace,
                )
            },
        )
    }

    // Modal para crear o editar un lugar
    if (uiState.isCreatingNewPlace || uiState.editingPlace != null) {
        PlaceEditSheet(
            place = uiState.editingPlace,
            onDismiss = viewModel::onDismissSheet,
        )
    }

    PlacesListContent(
        uiState = uiState,
        onAddNewPlace = viewModel::onAddNewPlaceClick,
        onEditPlace = viewModel::onEditPlaceClick,
        onDeletePlace = viewModel::onRequestDeletePlace,
        contentPadding = contentPadding,
    )
}

@Composable
private fun PlacesListContent(
    uiState: PlacesListUiState,
    onAddNewPlace: () -> Unit,
    onEditPlace: (Place) -> Unit,
    onDeletePlace: (Place) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .appBackground()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.places_list_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.places_list_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                CircleIconButton(
                    icon = R.drawable.ic_add,
                    contentDescription = stringResource(R.string.places_list_add),
                    onClick = onAddNewPlace,
                )
            }

            if (uiState.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        PlaceholderMessage(
                            icon = R.drawable.ic_location_on,
                            message = stringResource(R.string.places_list_empty_message),
                        )
                        Button(
                            onClick = onAddNewPlace,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.places_list_add))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                ) {
                    items(items = uiState.places, key = { it.id }) { place ->
                        PlaceItemCard(
                            place = place,
                            onEdit = { onEditPlace(place) },
                            onDelete = { onDeletePlace(place) },
                        )
                    }
                }
            }
        }

        // FAB flotante cuando hay lugares
        if (!uiState.isEmpty) {
            FloatingActionButton(
                onClick = onAddNewPlace,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.places_list_add),
                )
            }
        }
    }
}

@Composable
private fun PlaceItemCard(
    place: Place,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(place.kind.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(place.kind.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.places_list_radius_format, place.radiusMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CircleIconButton(
                    icon = R.drawable.ic_edit,
                    contentDescription = stringResource(R.string.places_edit_title_edit),
                    onClick = onEdit,
                )
                CircleIconButton(
                    icon = R.drawable.ic_delete,
                    contentDescription = stringResource(R.string.places_list_delete_confirm_action),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlacesListContentPreview() {
    RememberAppTheme {
        PlacesListContent(
            uiState = PlacesListUiState(
                places = listOf(
                    Place(id = 1, name = "Casa", latitude = -34.588, longitude = -58.430, kind = PlaceKind.Home),
                    Place(id = 2, name = "Facultad", latitude = -34.617, longitude = -58.381, radiusMeters = 300, kind = PlaceKind.Study),
                ),
            ),
            onAddNewPlace = {},
            onEditPlace = {},
            onDeletePlace = {},
        )
    }
}

@Preview
@Composable
private fun PlacesListEmptyPreview() {
    RememberAppTheme {
        PlacesListContent(
            uiState = PlacesListUiState(places = emptyList()),
            onAddNewPlace = {},
            onEditPlace = {},
            onDeletePlace = {},
        )
    }
}
