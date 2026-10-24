package com.example.uade.rememberapp.ui.places.edit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.places.components.iconRes
import com.example.uade.rememberapp.ui.places.components.labelRes
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Modal bottom sheet para crear o editar un lugar guardado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditSheet(
    place: Place? = null,
    onDismiss: () -> Unit,
    viewModel: PlaceEditViewModel = viewModel(factory = PlaceEditViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(fineGranted || coarseGranted)
    }

    LaunchedEffect(place) {
        viewModel.init(place)
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onLocationPermissionResult(hasFine || hasCoarse)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        PlaceEditContent(
            uiState = uiState,
            onNameChanged = viewModel::onNameChanged,
            onKindChanged = viewModel::onKindChanged,
            onRadiusChanged = viewModel::onRadiusChanged,
            onAddressQueryChanged = viewModel::onAddressQueryChanged,
            onSearchAddress = viewModel::onSearchAddress,
            onSuggestionSelected = viewModel::onAddressSuggestionSelected,
            onUseCurrentLocation = {
                val hasFine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    viewModel.onUseCurrentLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
            onSave = viewModel::onSave,
            onDismiss = onDismiss,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceEditContent(
    uiState: PlaceEditUiState,
    onNameChanged: (String) -> Unit,
    onKindChanged: (PlaceKind) -> Unit,
    onRadiusChanged: (Int) -> Unit,
    onAddressQueryChanged: (String) -> Unit,
    onSearchAddress: () -> Unit,
    onSuggestionSelected: (AddressSuggestion) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabecera: Título y botón cerrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (uiState.isNew) R.string.places_edit_title_new else R.string.places_edit_title_edit,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            CircleIconButton(
                icon = R.drawable.ic_close,
                contentDescription = stringResource(R.string.places_edit_cancel),
                onClick = onDismiss,
            )
        }

        // 1. Nombre del lugar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel(text = stringResource(R.string.places_edit_name_label))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                placeholder = { Text(stringResource(R.string.places_edit_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
        }

        // 2. Tipo de lugar (Kind)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.places_edit_kind_label))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceKind.entries.forEach { kind ->
                    OptionChip(
                        text = stringResource(kind.labelRes()),
                        icon = kind.iconRes(),
                        selected = uiState.kind == kind,
                        onClick = { onKindChanged(kind) },
                        style = OptionChipStyle.Filled,
                    )
                }
            }
        }

        // 3. Dirección y Ubicación
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.places_edit_address_label))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.addressQuery,
                    onValueChange = onAddressQueryChanged,
                    placeholder = { Text(stringResource(R.string.places_edit_address_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onSearchAddress()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.reminders_capture_place_search),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onSearchAddress()
                        },
                    ),
                )

                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        onUseCurrentLocation()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_near_me),
                        contentDescription = stringResource(R.string.places_edit_use_current_location),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (uiState.isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.places_edit_searching),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (uiState.hasSearched && uiState.searchResults.isEmpty() && !uiState.isSearching) {
                Text(
                    text = stringResource(R.string.places_edit_no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            // Resultados de búsqueda
            uiState.searchResults.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionSelected(suggestion) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_location_on),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = suggestion.addressLine,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Ubicación seleccionada
            AnimatedVisibility(visible = uiState.hasSelectedLocation) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.places_edit_selected_location),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = uiState.formattedAddress
                                    ?: "${uiState.latitude}, ${uiState.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        // 4. Radio de aviso
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = stringResource(R.string.places_edit_radius_label))
            val radiusOptions = listOf(100, 150, 300, 500)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                radiusOptions.forEach { meters ->
                    val text = if (meters == 150) {
                        stringResource(R.string.places_edit_radius_recommended, meters)
                    } else {
                        stringResource(R.string.places_edit_radius_chip, meters)
                    }
                    OptionChip(
                        text = text,
                        selected = uiState.radiusMeters == meters,
                        onClick = { onRadiusChanged(meters) },
                        style = OptionChipStyle.Outlined,
                    )
                }
            }
        }

        // Mensaje de error
        if (uiState.errorMessageRes != null) {
            Text(
                text = stringResource(uiState.errorMessageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.places_edit_cancel))
            }

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.places_edit_save))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121A20)
@Composable
private fun PlaceEditContentPreview() {
    RememberAppTheme {
        PlaceEditContent(
            uiState = PlaceEditUiState(
                name = "Facultad",
                kind = PlaceKind.Study,
                latitude = -34.617,
                longitude = -58.381,
                formattedAddress = "Lima 757, CABA",
                radiusMeters = 150,
            ),
            onNameChanged = {},
            onKindChanged = {},
            onRadiusChanged = {},
            onAddressQueryChanged = {},
            onSearchAddress = {},
            onSuggestionSelected = {},
            onUseCurrentLocation = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
