package com.example.uade.rememberapp.ui.reminders.capture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.FadedDivider
import com.example.uade.rememberapp.ui.components.observeVerticalDrag
import com.example.uade.rememberapp.ui.tags.TagPickerContent
import com.example.uade.rememberapp.ui.tags.TagPickerActions
import com.example.uade.rememberapp.ui.tags.TagPickerPanel
import com.example.uade.rememberapp.ui.tags.previewTagPickerState
import com.example.uade.rememberapp.ui.reminders.capture.components.CaptureToolbar
import com.example.uade.rememberapp.ui.reminders.capture.components.CaptureTriggerRow
import com.example.uade.rememberapp.ui.reminders.capture.components.PlaceOptionsPanel
import com.example.uade.rememberapp.ui.reminders.capture.components.ReminderDateDialog
import com.example.uade.rememberapp.ui.reminders.capture.components.TimeOptionsPanel
import com.example.uade.rememberapp.ui.reminders.sample.SampleQuickCapture
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.launch
import kotlin.math.exp

/** Margen lateral del contenido del modal. */
private val SheetPadding = 16.dp

/**
 * Modal que sube desde abajo para crear una nota rápida: título, cuándo avisar (hora y/o
 * lugar), etiqueta y guardar.
 *
 * Esta función es la "con estado": consigue el ViewModel y arma el ModalBottomSheet. El diseño
 * está en [QuickCaptureContent], que se puede previsualizar sin el modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    viewModel: QuickCaptureViewModel = viewModel(factory = QuickCaptureViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Guardado: baja el modal con su animación y recién después lo saca (y reinicia el estado).
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            sheetState.hide()
            viewModel.onDismissed()
            onDismiss()
        }
    }
    val density = LocalDensity.current
    val maxStretchPx = with(density) { HandleMaxStretch.toPx() }

    // Cuánto se "estira" el contenido (en px) al tirar de la manija hacia arriba: el efecto de
    // que el modal no puede crecer más. Vuelve a 0 con un rebote al soltar.
    val stretch = remember { Animatable(0f) }

    ModalBottomSheet(
        onDismissRequest = {
            // Al cerrar se descarta lo escrito, así la próxima vez abre vacío.
            viewModel.onDismissed()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        // Los insets (barra de navegación y teclado) los maneja el contenido.
        contentWindowInsets = { WindowInsets(0) },
        // La manija la dibujamos nosotros (abajo): la de Material3 viene envuelta en un
        // clickable que, con el modal abierto, lo cierra, y se disparaba al tirar hacia arriba.
        dragHandle = null,
    ) {
        QuickCaptureDragHandle(
            onDrag = { dy ->
                // Solo hacia arriba, con resistencia: cuanto más tira, menos se estira.
                val pull = (-dy).coerceAtLeast(0f)
                scope.launch { stretch.snapTo(rubberBand(pull, maxStretchPx)) }
            },
            onRelease = {
                // El modal no tiene una "segunda extensión": al soltar solo vuelve a su lugar.
                scope.launch {
                    stretch.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            },
        )
        QuickCaptureContent(
            uiState = uiState,
            actions = QuickCaptureActions(
                onTitleChanged = viewModel::onTitleChanged,
                onPanelToggle = viewModel::onPanelToggle,
                onTimeSelected = viewModel::onTimeSelected,
                onRepeatSelected = viewModel::onRepeatSelected,
                onPlaceSelected = viewModel::onPlaceSelected,
                onPlaceEventSelected = viewModel::onPlaceEventSelected,
                onSearchAddress = viewModel::onSearchAddress,
                onVoice = viewModel::onVoiceClick,
                onPhoto = viewModel::onPhotoClick,
                onChecklist = viewModel::onChecklistClick,
                onToggleFullScreen = viewModel::onFullScreenToggle,
                onSave = viewModel::onSave,
            ),
            modifier = Modifier
                // Estiramiento desde abajo (el borde de abajo queda fijo, el de arriba sube).
                .graphicsLayer {
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
                    if (size.height > 0f) scaleY = 1f + stretch.value / size.height
                }
                .navigationBarsPadding()
                .imePadding(),
            tagsPanel = {
                TagPickerPanel(
                    initialAssigned = uiState.selectedTagIds,
                    onAssignedChange = viewModel::onTagsChanged,
                    modifier = Modifier.padding(horizontal = SheetPadding),
                )
            },
        )
    }

    if (uiState.isDatePickerOpen) {
        ReminderDateDialog(
            initialDate = uiState.pickedDate,
            initialTime = uiState.pickedTime,
            onConfirm = viewModel::onDatePicked,
            onDismiss = viewModel::onDatePickerDismiss,
        )
    }
}

/** Lo máximo que se puede estirar el contenido al tirar de la manija. */
private val HandleMaxStretch = 32.dp

/**
 * Resistencia tipo "banda elástica": al principio sigue al dedo y después cada vez cuesta más,
 * sin pasar nunca de [max].
 */
private fun rubberBand(pull: Float, max: Float): Float = max * (1f - exp(-pull / max))

/**
 * La manija "—" de arriba del modal.
 *
 * - Arrastrarla hacia abajo cierra el modal (lo maneja el ModalBottomSheet).
 * - Arrastrarla hacia arriba avisa el recorrido por [onDrag] (para el estiramiento) y el fin
 *   del gesto por [onRelease]. No abre nada: el modal no crece más.
 *
 * Ocupa todo el ancho para que sea fácil de agarrar. El gesto solo se observa, así el arrastre
 * hacia abajo del modal sigue funcionando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureDragHandle(
    onDrag: (dy: Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .observeVerticalDrag(onDrag = onDrag, onRelease = { _, _ -> onRelease() }),
        contentAlignment = Alignment.Center,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/** Todo lo que el usuario puede hacer en el modal (ver [QuickCaptureSheet]). */
data class QuickCaptureActions(
    val onTitleChanged: (String) -> Unit = {},
    val onPanelToggle: (CapturePanel) -> Unit = {},
    val onTimeSelected: (TimeShortcut) -> Unit = {},
    val onRepeatSelected: (RepeatOption) -> Unit = {},
    val onPlaceSelected: (placeId: Long) -> Unit = {},
    val onPlaceEventSelected: (PlaceEvent) -> Unit = {},
    val onSearchAddress: () -> Unit = {},
    val onVoice: () -> Unit = {},
    val onPhoto: () -> Unit = {},
    val onChecklist: () -> Unit = {},
    val onToggleFullScreen: () -> Unit = {},
    val onSave: () -> Unit = {},
)

/**
 * Contenido del modal, sin estado:
 * ```
 * Llamar al plomero|
 * ───────────────────────────────────── (separador fino)
 * ┌ panel de hora o de lugar (solo si hay uno abierto) ┐
 * ───────────────────────────────────── (separador, solo con un panel abierto)
 * [🕒 Fecha y hora] [📍 Ubicación] [🏷 Etiqueta]
 * 🎤 🖼 ☑ ⤢                             Guardar
 * ```
 * Al abrirse no pide el foco: el teclado aparece recién cuando el usuario toca el título.
 *
 * En pantalla completa ([QuickCaptureUiState.isFullScreen]) ocupa todo el alto disponible y
 * los chips y la barra de herramientas quedan pegados abajo.
 *
 * [tagsPanel] es el panel de etiquetas: entra como slot porque tiene su propio ViewModel, así
 * este contenido sigue sin estado (las previews le pasan una versión sin estado).
 */
@Composable
fun QuickCaptureContent(
    uiState: QuickCaptureUiState,
    actions: QuickCaptureActions,
    modifier: Modifier = Modifier,
    tagsPanel: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (uiState.isFullScreen) Modifier.fillMaxHeight() else Modifier)
            // Única animación de alto del modal: abrir/cerrar paneles y pantalla completa.
            .animateContentSize()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TitleField(
            title = uiState.title,
            onTitleChanged = actions.onTitleChanged,
            modifier = Modifier.padding(horizontal = SheetPadding),
        )

        // Separa lo que se escribe (el título) de las opciones de abajo.
        FadedDivider(modifier = Modifier.padding(horizontal = SheetPadding))

        // Al abrir o cambiar de panel, el alto lo anima una sola vez el animateContentSize() del
        // Column; acá el tamaño salta de golpe (snap) y solo se anima la opacidad. Si los dos
        // animaran el alto, se pisarían y el modal quedaría atrasado respecto del contenido.
        //
        // weight(fill = false) + verticalScroll: si con el teclado abierto no entra todo, solo el
        // panel se achica y se desplaza; el título, los chips y "Guardar" siguen a la vista.
        AnimatedContent(
            targetState = uiState.expandedPanel,
            transitionSpec = {
                fadeIn(tween(durationMillis = 220, delayMillis = 90)) togetherWith
                    fadeOut(tween(durationMillis = 90)) using
                    SizeTransform(clip = false) { _, _ -> snap() }
            },
            label = "capturePanel",
            modifier = Modifier.weight(1f, fill = false),
        ) { panel ->
            Box(Modifier.verticalScroll(rememberScrollState())) {
                CapturePanelContent(panel = panel, uiState = uiState, actions = actions, tagsPanel = tagsPanel)
            }
        }

        if (uiState.isFullScreen) {
            Spacer(Modifier.weight(1f))
        }

        // Con un panel abierto, separa sus opciones de los chips que lo abren. Solo fundido: el
        // alto lo anima el animateContentSize() del Column.
        AnimatedVisibility(
            visible = uiState.expandedPanel != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FadedDivider(modifier = Modifier.padding(horizontal = SheetPadding))
        }

        CaptureTriggerRow(
            expandedPanel = uiState.expandedPanel,
            hasTime = uiState.hasTime,
            selectedPlace = uiState.selectedPlace,
            placeEvent = uiState.selectedPlaceEvent,
            onPanelClick = actions.onPanelToggle,
            tagCount = uiState.selectedTagIds.size,
            contentPadding = PaddingValues(horizontal = SheetPadding),
        )

        CaptureToolbar(
            isFullScreen = uiState.isFullScreen,
            onVoice = actions.onVoice,
            onPhoto = actions.onPhoto,
            onChecklist = actions.onChecklist,
            onToggleFullScreen = actions.onToggleFullScreen,
            onSave = actions.onSave,
            canSave = uiState.canSave,
            modifier = Modifier.padding(start = 4.dp, end = SheetPadding),
        )
    }
}

/** El panel abierto: opciones de hora, de lugar o etiquetas (o nada). */
@Composable
private fun CapturePanelContent(
    panel: CapturePanel?,
    uiState: QuickCaptureUiState,
    actions: QuickCaptureActions,
    tagsPanel: @Composable () -> Unit,
) {
    when (panel) {
        CapturePanel.Time -> TimeOptionsPanel(
            shortcuts = uiState.timeShortcuts,
            selectedShortcut = uiState.selectedTime,
            pickedDate = uiState.pickedDate,
            pickedTime = uiState.pickedTime,
            repeatOptions = uiState.repeatOptions,
            selectedRepeat = uiState.selectedRepeat,
            onShortcutClick = actions.onTimeSelected,
            onRepeatClick = actions.onRepeatSelected,
            modifier = Modifier.padding(horizontal = SheetPadding),
        )

        CapturePanel.Place -> PlaceOptionsPanel(
            places = uiState.favoritePlaces,
            selectedPlaceId = uiState.selectedPlaceId,
            events = uiState.placeEvents,
            selectedEvent = uiState.selectedPlaceEvent,
            onPlaceClick = actions.onPlaceSelected,
            onSearchAddress = actions.onSearchAddress,
            onEventClick = actions.onPlaceEventSelected,
            modifier = Modifier.padding(horizontal = SheetPadding),
        )

        CapturePanel.Tags -> tagsPanel()

        null -> Box(Modifier.fillMaxWidth())
    }
}

/** Campo del título: texto grande, sin borde, con el hint "¿Qué querés recordar?". */
@Composable
private fun TitleField(
    title: String,
    onTitleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = title,
        onValueChange = onTitleChanged,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerTextField ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.reminders_capture_title_hint),
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun QuickCaptureContentPreviewFrame(uiState: QuickCaptureUiState) {
    RememberAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            QuickCaptureContent(
                uiState = uiState,
                actions = QuickCaptureActions(),
                modifier = Modifier.padding(top = 16.dp),
                tagsPanel = {
                    TagPickerContent(
                        uiState = previewTagPickerState(),
                        actions = TagPickerActions(),
                        modifier = Modifier.padding(horizontal = SheetPadding),
                    )
                },
            )
        }
    }
}

@Preview(name = "Panel de etiquetas", heightDp = 900)
@Composable
private fun QuickCaptureContentTagsPreview() {
    QuickCaptureContentPreviewFrame(
        SampleQuickCapture.uiState().copy(
            title = "Turno con el médico",
            expandedPanel = CapturePanel.Tags,
            selectedTagIds = setOf(1),
        ),
    )
}

@Preview(name = "Vacío")
@Composable
private fun QuickCaptureContentEmptyPreview() {
    QuickCaptureContentPreviewFrame(SampleQuickCapture.uiState())
}

@Preview(name = "Panel de fecha y hora")
@Composable
private fun QuickCaptureContentTimePreview() {
    QuickCaptureContentPreviewFrame(SampleQuickCapture.timePanelState())
}

@Preview(name = "Panel de ubicación")
@Composable
private fun QuickCaptureContentPlacePreview() {
    QuickCaptureContentPreviewFrame(SampleQuickCapture.placePanelState())
}

@Preview(name = "Pantalla completa", heightDp = 800)
@Composable
private fun QuickCaptureContentFullScreenPreview() {
    QuickCaptureContentPreviewFrame(
        SampleQuickCapture.uiState().copy(title = "Llamar al plomero", isFullScreen = true),
    )
}
