package com.example.uade.rememberapp.ui.reminders.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.platform.location.LocationPermissionHelper
import com.example.uade.rememberapp.ui.components.AppDialogDefaults
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.components.ImageCropDialog
import com.example.uade.rememberapp.ui.components.LocationPermissionDialog
import com.example.uade.rememberapp.ui.places.edit.PlaceEditSheet
import com.example.uade.rememberapp.ui.reminders.components.ImportanceOptionsPanel
import com.example.uade.rememberapp.ui.reminders.components.PlaceOptionsPanel
import com.example.uade.rememberapp.ui.reminders.components.ReminderDateDialog
import com.example.uade.rememberapp.ui.reminders.components.formatReminderTime
import com.example.uade.rememberapp.ui.reminders.components.isSameDay
import com.example.uade.rememberapp.ui.reminders.detail.components.AlarmsSheet
import com.example.uade.rememberapp.ui.reminders.detail.components.AlarmsSheetActions
import com.example.uade.rememberapp.ui.reminders.detail.components.ChecklistDragState
import com.example.uade.rememberapp.ui.reminders.detail.components.NotificationActionSheet
import com.example.uade.rememberapp.ui.reminders.detail.components.NotificationActionSheetActions
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotoStack
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotosSheet
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotosSheetActions
import com.example.uade.rememberapp.ui.reminders.model.NotificationActionKind
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.reminders.voice.VoiceCaptureSheet
import com.example.uade.rememberapp.ui.tags.TagPickerPanel
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.UnsavedChangesGreen
import com.example.uade.rememberapp.ui.theme.appBackground
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Margen lateral de la pantalla. */
private val ScreenPadding = 16.dp

/** Proporción de la portada: la misma del marco de recorte, así se ve tal cual se recortó. */
private const val CoverAspectRatio = 16f / 9f

/**
 * Detalle de un recordatorio: se abre al tocar una card de la Home.
 *
 * Esta función es la "con estado": consigue el ViewModel y lee su UiState. El diseño está en
 * [ReminderDetailContent], que se puede previsualizar con datos inventados.
 */
@Composable
fun ReminderDetailScreen(
    onBack: () -> Unit,
    viewModel: ReminderDetailViewModel = viewModel(factory = ReminderDetailViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // "Ahora" fijo mientras la pantalla está abierta: decide si el aviso dice "Hoy 18:00".
    val now = remember { Instant.now() }

    // Guardado con ✓: recién ahí se cierra, así no se sale antes de que termine de guardar.
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    val context = LocalContext.current
    var showLocationPermissionDialog by remember { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            val app = context.applicationContext as? RememberApp
            app?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.container.rescheduleRemindersUseCase()
                }
            }
        }
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted && !LocationPermissionHelper.hasBackgroundLocation(context)) {
            showLocationPermissionDialog = true
        }
    }

    // El selector de fotos de Android (Photo Picker): no pide permisos, porque la app solo recibe
    // las fotos que el usuario elige. Devuelve hasta MAX_PHOTOS URIs (lista vacía si se cancela).
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PHOTOS),
    ) { uris -> viewModel.onPhotosPicked(uris.map { it.toString() }) }
    val pickPhotos = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // Sin fotos, "Imagen" va directo al selector; con fotos, abre el modal para elegir la portada.
    val onImageClick = {
        if (uiState.photos.isEmpty()) pickPhotos() else viewModel.onImageClick()
    }
    // "Agregar banner": una sola foto, que queda de portada.
    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onBannerPicked(uri?.toString()) }
    val pickBanner = {
        bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Salir (con ← o con el "atrás" del sistema) con cambios sin guardar pregunta antes, para no
    // perder lo escrito. Sin cambios, sale directo. Mientras guarda, no hay nada que preguntar.
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    val requestBack = {
        if (uiState.hasUnsavedChanges && !uiState.isSaving) showUnsavedDialog = true else onBack()
    }
    BackHandler(enabled = uiState.hasUnsavedChanges && !uiState.isSaving && !uiState.isSaved) {
        showUnsavedDialog = true
    }
    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            onSave = {
                showUnsavedDialog = false
                viewModel.onDone()
            },
            onDiscard = {
                showUnsavedDialog = false
                onBack()
            },
            onKeepEditing = { showUnsavedDialog = false },
        )
    }

    ReminderDetailContent(
        uiState = uiState,
        now = now,
        onBack = requestBack,
        onShare = viewModel::onShareClick,
        onDone = viewModel::onDone,
        onTitleChange = viewModel::onTitleChange,
        onTagsClick = viewModel::onTagsClick,
        onTimeClick = viewModel::onTimeClick,
        onPlaceClick = viewModel::onPlaceClick,
        onImageClick = onImageClick,
        onBehaviorClick = viewModel::onBehaviorClick,
        onActionClick = viewModel::onActionClick,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAddBanner = pickBanner,
        checklistActions = ChecklistActions(
            onToggleItem = viewModel::onToggleItem,
            onItemTextChange = viewModel::onItemTextChange,
            onItemNoteChange = viewModel::onItemNoteChange,
            onToggleItemNoteExpanded = viewModel::onToggleItemNoteExpanded,
            onDeleteItem = viewModel::onDeleteItem,
            onAddItem = viewModel::onAddItem,
            onItemImeNext = viewModel::onItemImeNext,
            onItemFocused = viewModel::onItemFocused,
            onMoveItem = viewModel::onMoveItem,
            onToggleCompletedExpanded = viewModel::onToggleCompletedExpanded,
            onCheckAll = viewModel::onCheckAll,
            onUndoCheckAll = viewModel::onUndoCheckAll,
            onOpenPasteDialog = viewModel::onOpenPasteDialog,
            onOpenVoiceCapture = viewModel::onOpenVoiceCapture,
            onFinalizeReminder = viewModel::onFinalizeReminder,
        ),
    )

    uiState.tagsEditor?.let { assigned ->
        TagsSheet(
            assignedIds = assigned,
            onAssignedChange = viewModel::onTagsChanged,
            onCancel = viewModel::onCancelTags,
            onSave = viewModel::onSaveTags,
        )
    }

    uiState.actionEditor?.let { editor ->
        NotificationActionSheet(
            editor = editor,
            currentKind = uiState.action?.let { NotificationActionKind.of(it) },
            actions = NotificationActionSheetActions(
                onKindSelected = viewModel::onActionKindSelected,
                onBack = viewModel::onActionBack,
                onChannel = viewModel::onActionChannel,
                onTarget = viewModel::onActionTarget,
                onText = viewModel::onActionText,
                onRemove = viewModel::onRemoveAction,
                onCancel = viewModel::onCancelAction,
                onSave = viewModel::onSaveAction,
            ),
        )
    }

    // "Imágenes": hasta MAX_PHOTOS fotos; tocar una abre el recorte encima del modal para usarla de portada.
    uiState.photosEditor?.let { editor ->
        PhotosSheet(
            editor = editor,
            actions = PhotosSheetActions(
                onAdd = pickPhotos,
                onPhotoClick = viewModel::onPhotoClick,
                onRemove = viewModel::onRemovePhoto,
                onCancel = viewModel::onCancelPhotos,
                onSave = viewModel::onSavePhotos,
            ),
        )

        editor.cropping?.let { path ->
            ImageCropDialog(
                imagePath = path,
                aspectRatio = CoverAspectRatio,
                onConfirm = viewModel::onCropConfirm,
                onDismiss = viewModel::onCropDismiss,
            )
        }
    }

    uiState.placeEditor?.let { editor ->
        PlaceSheet(
            places = uiState.favoritePlaces,
            editor = editor,
            onPlaceClick = viewModel::onPlaceSelected,
            onSearchAddress = viewModel::onSearchAddress,
            onEventClick = viewModel::onPlaceEventSelected,
            onSave = {
                viewModel.onSavePlace()
                if (editor.selectedPlaceId != null) {
                    if (!LocationPermissionHelper.hasFineLocation(context)) {
                        foregroundPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    } else if (!LocationPermissionHelper.hasBackgroundLocation(context)) {
                        showLocationPermissionDialog = true
                    }
                }
            },
            onClose = viewModel::onClosePlace,
        )
    }

    if (showLocationPermissionDialog) {
        LocationPermissionDialog(
            onConfirm = {
                showLocationPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    LocationPermissionHelper.openAppSettings(context)
                }
            },
            onDismiss = { showLocationPermissionDialog = false },
        )
    }

    if (uiState.isCreatingPlace) {
        PlaceEditSheet(
            onDismiss = viewModel::onDismissPlaceSheet,
        )
    }

    uiState.importanceEditor?.let { selected ->
        ImportanceSheet(
            selected = selected,
            onSelect = viewModel::onImportanceSelected,
            onCancel = viewModel::onCancelImportance,
            onSave = viewModel::onSaveImportance,
        )
    }

    // "Establecer recordatorio": varios avisos por hora. Guardar los pasa al borrador (se
    // persisten con ✓); Cancelar o cerrar el modal los descarta.
    uiState.alarmsEditor?.let { editor ->
        AlarmsSheet(
            editor = editor,
            now = now,
            actions = AlarmsSheetActions(
                onShortcut = viewModel::onAlarmShortcut,
                onRepeat = viewModel::onAlarmRepeat,
                onToggleExpanded = viewModel::onToggleAlarmExpanded,
                onAdd = viewModel::onAddAlarm,
                onClear = viewModel::onClearAlarms,
                onCancel = viewModel::onCancelAlarms,
                onSave = viewModel::onSaveAlarms,
            ),
        )

        // "Elegir fecha…" de una tarjeta: el mismo calendario que la nota rápida, encima del modal.
        editor.datePickerFor?.let { index ->
            val draft = editor.items.getOrNull(index)
            ReminderDateDialog(
                initialDate = draft?.pickedDate,
                initialTime = draft?.pickedTime,
                onConfirm = viewModel::onAlarmDatePicked,
                onDismiss = viewModel::onAlarmDatePickerDismiss,
            )
        }
    }

    if (uiState.isPasteDialogOpen) {
        PasteItemsDialog(
            onDismiss = viewModel::onClosePasteDialog,
            onSubmit = viewModel::onPasteItemsSubmit,
        )
    }

    if (uiState.isVoiceCaptureOpen) {
        VoiceCaptureSheet(
            onDismiss = viewModel::onCloseVoiceCapture,
            onAiAnalysisReady = viewModel::onAiAnalysisItemsReady,
            onAudioRecordedForConfig = viewModel::onVoiceItemsRecorded,
        )
    }
}

/**
 * Modal "Cómo avisar": el mismo panel de importancia que en la nota rápida (un nivel a la vez,
 * con su descripción), con "Cancelar / Guardar" como los otros modales del detalle. Guardar pasa
 * el nivel al borrador; se persiste con ✓.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportanceSheet(
    selected: Importance,
    onSelect: (Importance) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ImportanceOptionsPanel(
                options = ReminderImportance.entries,
                selected = ReminderImportance.of(selected),
                onSelect = { onSelect(it.importance) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onCancel)
                DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = onSave)
            }
        }
    }
}

/**
 * Modal "Ubicación": el mismo panel que en la nota rápida (lugares favoritos y "Al llegar / Al
 * salir"), con "Cancelar / Guardar" como los otros modales del detalle.
 * TODO: por ahora solo se marca; "Guardar" cierra igual que "Cancelar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSheet(
    places: List<Place>,
    editor: PlaceEditor,
    onPlaceClick: (Long) -> Unit,
    onSearchAddress: () -> Unit,
    onEventClick: (PlaceEvent) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlaceOptionsPanel(
                places = places,
                selectedPlaceId = editor.selectedPlaceId,
                events = PlaceEvent.entries,
                selectedEvent = editor.event,
                onPlaceClick = onPlaceClick,
                onSearchAddress = onSearchAddress,
                onEventClick = onEventClick,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onClose)
                DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = onSave)
            }
        }
    }
}

/**
 * Modal de etiquetas: el mismo panel que en la nota rápida (asignar tocando, crear, editar y
 * borrar manteniendo presionada), pero en su propio modal y sin el campo de título.
 *
 * - Asignar y quitar trabaja sobre una copia: "Guardar" la pasa al borrador (se persiste con ✓)
 *   y "Cancelar", o cerrar el modal, la descarta.
 * - Crear, editar y borrar etiquetas sí se guarda al momento (son de todas las notas).
 *
 * Hay un solo par de botones a la vez: con el editor de etiquetas desplegado se ocultan
 * "Cancelar / Guardar" del modal y quedan los del editor ("Cancelar / Hecho"), que solo
 * afectan a la etiqueta que se está creando. Así no hay dos "Cancelar" que hacen cosas distintas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsSheet(
    assignedIds: Set<Long>,
    onAssignedChange: (Set<Long>) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    // Estado visual del modal: si el editor está desplegado (lo avisa el panel).
    var isEditorOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(start = ScreenPadding, end = ScreenPadding, bottom = 16.dp)
                // Con el teclado abierto (al escribir una etiqueta nueva), el contenido sube.
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TagPickerPanel(
                initialAssigned = assignedIds,
                onAssignedChange = onAssignedChange,
                onCreatorExpandedChange = { isEditorOpen = it },
            )
            AnimatedVisibility(visible = !isEditorOpen, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = onCancel)
                    DialogFilledConfirmButton(text = stringResource(R.string.reminder_alarms_save), onClick = onSave)
                }
            }
        }
    }
}

/**
 * ```
 * ←                                   ⇪  ✓
 * [📄] Vacuna de Tomi                       ← editable; vacío: "Título"
 * ┌ foto (si tiene) ───────────────────────┐
 * Llevar libreta sanitaria y DNI…          ← editable; vacía: "Nota vacía / Toca aquí…"
 *                                            (ocupa todo el alto libre)
 * ┌───────────────────────────────────────┐ ← abajo; se oculta con el teclado abierto
 * │ 🏷 Etiquetas [● Salud]                 │
 * │ ┌ ⏰ Recordatorio ┐ ┌ 📍 Ubicación   ┐ │
 * │ ┌ 👁 Comportam.   ┐ ┌ ⚡ Acción notif. ┐ │
 * │ ┌ 🖼 Imagen       ┐ ┌ 📎 Adjuntos    ┐ │
 * │ ┌ ⇪ Compartir     ┐                    │
 * └───────────────────────────────────────┘
 * ```
 * Las opciones muestran el valor actual del recordatorio. Una nota nueva se ve igual, vacía y
 * con las opciones en sus valores por defecto ("Sin etiquetas", "Sin fecha"…).
 * TODO: cada opción va a abrir su sheet para cambiarla; por ahora no se pueden tocar.
 */
@Composable
private fun ReminderDetailContent(
    uiState: ReminderDetailUiState,
    now: Instant,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
    onTitleChange: (String) -> Unit,
    onTagsClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onImageClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onActionClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddBanner: () -> Unit = {},
    checklistActions: ChecklistActions = ChecklistActions(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .appBackground()
            // safeDrawing incluye el teclado: con el teclado abierto, el contenido se achica y
            // se puede desplazar hasta el texto que se está escribiendo.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DetailTopBar(
            hasUnsavedChanges = uiState.hasUnsavedChanges,
            onBack = onBack,
            onShare = onShare,
            onDone = onDone,
        )

        when {
            uiState.isEditable -> ReminderDetailBody(
                uiState = uiState,
                now = now,
                onTitleChange = onTitleChange,
                onTagsClick = onTagsClick,
                onTimeClick = onTimeClick,
                onPlaceClick = onPlaceClick,
                onImageClick = onImageClick,
                onBehaviorClick = onBehaviorClick,
                onActionClick = onActionClick,
                onDescriptionChange = onDescriptionChange,
                onAddBanner = onAddBanner,
                checklistActions = checklistActions,
            )

            uiState.isNotFound -> Text(
                text = stringResource(R.string.reminder_detail_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ScreenPadding),
            )

            // Cargando: es una lectura local muy rápida, no vale la pena un indicador.
            else -> Unit
        }
    }
}

/**
 * ← a la izquierda; compartir y ✓ a la derecha. Con cambios sin guardar el ✓ se pinta de verde,
 * para que se note que hay algo por guardar.
 */
@Composable
private fun DetailTopBar(
    hasUnsavedChanges: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit,
) {
    val defaultTint = MaterialTheme.colorScheme.onSurface
    // Animado: el cambio de color se nota sin ser brusco.
    val doneTint by animateColorAsState(
        targetValue = if (hasUnsavedChanges) UnsavedChangesGreen else defaultTint,
        label = "doneTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarButton(R.drawable.ic_arrow_back, stringResource(R.string.reminder_detail_back), onBack)
        Spacer(Modifier.weight(1f))
        TopBarButton(R.drawable.ic_share, stringResource(R.string.reminder_detail_share), onShare)
        TopBarButton(R.drawable.ic_check, stringResource(R.string.reminder_detail_done), onDone, tint = doneTint)
    }
}

@Composable
private fun TopBarButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

/**
 * Dos zonas: arriba lo que se escribe (ocupa todo el alto libre y se desplaza si no entra) y
 * abajo, fijo, el panel de opciones. Con el teclado abierto el panel se oculta, así escribir
 * tiene toda la pantalla.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderDetailBody(
    uiState: ReminderDetailUiState,
    now: Instant,
    onTitleChange: (String) -> Unit,
    onTagsClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onImageClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onActionClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddBanner: () -> Unit,
    checklistActions: ChecklistActions,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // La descripción tiene que llegar hasta el panel de opciones, así todo ese espacio se
        // puede tocar para escribir. Dentro de un scroll el alto es infinito y `weight` no
        // reparte nada; pero con un alto MÍNIMO igual al espacio disponible, Compose le da a lo
        // que tiene `weight` todo lo que sobra hasta ese mínimo.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val availableHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = availableHeight)
                    // Abajo, solo un gap chico hasta el panel.
                    .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val stackPhotos = uiState.stackPhotos
                if (stackPhotos.isEmpty()) {
                    // Arriba de todo, donde va a quedar el banner: así se ve que se puede poner
                    // uno (antes había que adivinar que adjuntar una foto lo crea).
                    ChecklistActionPill(
                        icon = R.drawable.ic_add_photo_alternate,
                        label = stringResource(R.string.reminder_detail_add_banner),
                        onClick = onAddBanner,
                    )
                }

                DetailHeader(typeIcon = uiState.typeIcon, title = uiState.title, onTitleChange = onTitleChange)

                if (stackPhotos.isNotEmpty()) {
                    // Hasta 5 fotos apiladas, la portada adelante (con la proporción del recorte)
                    // y "+n" si hay más. Tocarlas abre el modal de imágenes.
                    PhotoStack(
                        photoPaths = stackPhotos,
                        aspectRatio = CoverAspectRatio,
                        onClick = onImageClick,
                    )
                }

                // Una sola nota para todo: el texto siempre está, y la lista aparece debajo cuando
                // tiene ítems. Sin lista, el texto ocupa todo el alto (se puede tocar en cualquier
                // lado para escribir) y debajo se ofrece "Agregar lista".
                DescriptionField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    modifier = if (uiState.hasChecklist) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    },
                )

                if (uiState.hasChecklist) {
                    ChecklistDetailContent(
                        items = uiState.items,
                        isCompletedExpanded = uiState.isCompletedExpanded,
                        expandedItemNoteIndex = uiState.expandedItemNoteIndex,
                        isGeneratingItems = uiState.isGeneratingItems,
                        focusItemIndex = uiState.focusItemIndex,
                        canUndoCheckAll = uiState.checkedBeforeCheckAll != null,
                        actions = checklistActions,
                    )
                } else {
                    ChecklistActionPill(
                        icon = R.drawable.ic_checklist,
                        label = stringResource(R.string.reminders_checklist_add_list),
                        onClick = { checklistActions.onAddItem("") },
                    )
                }
            }
        }

        // Un solo lugar para el panel: fijo abajo, y oculto mientras se escribe para no tapar.
        // Plegarlo con la flecha es estado puramente visual: vive acá (rememberSaveable lo
        // conserva al girar el teléfono) y no en el ViewModel.
        var isOptionsCollapsed by rememberSaveable { mutableStateOf(false) }
        if (!WindowInsets.isImeVisible) {
            ReminderOptionsPanel(
                uiState = uiState,
                now = now,
                isCollapsed = isOptionsCollapsed,
                onToggleCollapsed = { isOptionsCollapsed = !isOptionsCollapsed },
                onTagsClick = onTagsClick,
                onTimeClick = onTimeClick,
                onPlaceClick = onPlaceClick,
                onImageClick = onImageClick,
                onBehaviorClick = onBehaviorClick,
                onActionClick = onActionClick,
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 12.dp),
            )
        }
    }
}

/**
 * La descripción, editable. Mientras está vacía muestra:
 * ```
 * Nota vacía
 * Toca aquí para empezar a escribir…
 * ```
 * Ese texto es el "hint" del campo: tocarlo abre el teclado, y desaparece al escribir.
 */
@Composable
private fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        // El alto lo decide quien lo usa: en el detalle, todo el espacio hasta el panel, así se
        // puede tocar en cualquier lugar de esa zona para empezar a escribir.
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.reminder_detail_empty_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.reminder_detail_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                // El campo real va siempre: es el que recibe el toque y dibuja el cursor.
                innerTextField()
            }
        },
    )
}

/**
 * La lista de la nota, debajo del texto (Mockup 04.B). Solo se muestra si tiene ítems.
 *
 * Muestra los ítems activos reordenables con nota opcional, botones de ayuda IA ("Dictar ítems"
 * y "Pegar texto"), y la sección colapsable "COMPLETADOS (n)".
 */
@Composable
private fun ChecklistDetailContent(
    items: List<ChecklistItem>,
    isCompletedExpanded: Boolean,
    expandedItemNoteIndex: Int?,
    isGeneratingItems: Boolean,
    focusItemIndex: Int?,
    canUndoCheckAll: Boolean,
    actions: ChecklistActions,
    modifier: Modifier = Modifier,
) {
    // Solo se recalcula si cambian los ítems, no en cada recomposición (ej. al tipear el título).
    val activeItemsWithIndices = remember(items) {
        items.mapIndexed { index, item -> index to item }.filter { !it.second.isChecked }
    }
    val completedItemsWithIndices = remember(items) {
        items.mapIndexed { index, item -> index to item }.filter { it.second.isChecked }
    }
    val haptics = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { 6.dp.toPx() }
    val dragState = remember { ChecklistDragState() }
    dragState.spacingPx = spacingPx
    // Cada vez que llegan los ítems ya movidos, se puede volver a mover (ver ChecklistDragState).
    SideEffect { dragState.onItemsUpdated(items) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Ítems activos, reordenables arrastrando la manija ≡.
        activeItemsWithIndices.forEachIndexed { position, (index, item) ->
            val isDragged = dragState.draggedIndex == index
            ChecklistActiveItemRow(
                item = item,
                isNoteExpanded = expandedItemNoteIndex == index,
                requestFocus = focusItemIndex == index,
                onFocused = actions.onItemFocused,
                onToggle = { actions.onToggleItem(index) },
                onTextChange = { actions.onItemTextChange(index, it) },
                onImeNext = { actions.onItemImeNext(index) },
                onNoteChange = { actions.onItemNoteChange(index, it) },
                onToggleNote = { actions.onToggleItemNoteExpanded(index) },
                onDelete = { actions.onDeleteItem(index) },
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragState.start(index)
                },
                onDrag = { dy -> dragState.drag(dy, activeItemsWithIndices.map { it.first }, actions.onMoveItem) },
                onDragEnd = dragState::end,
                modifier = Modifier
                    .onSizeChanged { dragState.rowHeights[position] = it.height }
                    // El que se arrastra va arriba de los demás, levantado y siguiendo al dedo.
                    // El offset se lee al dibujar: arrastrar no recompone la lista.
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragState.offset else 0f
                        shadowElevation = if (isDragged) 8.dp.toPx() else 0f
                        shape = RoundedCornerShape(12.dp)
                        clip = isDragged
                    }
                    .background(
                        if (isDragged) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    ),
            )
        }

        // "+ Añadir elemento" a la izquierda y "Marcar todo" (o "Deshacer") a la derecha.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { actions.onAddItem("") }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.reminders_checklist_add_item),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            // Contra el toque sin querer: está separado de "Añadir elemento" y, una vez usado, el
            // mismo botón pasa a "Deshacer" hasta que se toque otro ítem.
            if (canUndoCheckAll) {
                TextButton(onClick = actions.onUndoCheckAll) {
                    Text(stringResource(R.string.reminders_undo))
                }
            } else if (activeItemsWithIndices.isNotEmpty() && items.size > 1) {
                TextButton(onClick = actions.onCheckAll) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done_all),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reminders_checklist_check_all))
                }
            }
        }

        // Botones de acciones IA: [ 🎤 Dictar ítems ] [ ✨ Pegar texto ]
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            ChecklistActionPill(
                icon = R.drawable.ic_mic,
                label = stringResource(R.string.reminders_checklist_dictate),
                onClick = actions.onOpenVoiceCapture,
            )
            ChecklistActionPill(
                icon = R.drawable.ic_auto_awesome,
                label = stringResource(R.string.reminders_checklist_paste),
                onClick = actions.onOpenPasteDialog,
            )
        }

        if (isGeneratingItems) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.reminders_checklist_generating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Sección COMPLETADOS (n)
        if (completedItemsWithIndices.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = actions.onToggleCompletedExpanded)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reminders_checklist_completed_header, completedItemsWithIndices.size),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(
                        if (isCompletedExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (isCompletedExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    completedItemsWithIndices.forEach { (index, item) ->
                        ChecklistCompletedItemRow(
                            item = item,
                            onToggle = { actions.onToggleItem(index) },
                            onDelete = { actions.onDeleteItem(index) },
                        )
                    }
                }
            }
        }

        if (activeItemsWithIndices.isEmpty() && completedItemsWithIndices.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = actions.onFinalizeReminder),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.checklist_all_completed_banner),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Los eventos de la lista de la nota, agrupados (como RemindersListActions) para no pasar quince
 * lambdas sueltas por cada nivel. Los índices son posiciones en `items`.
 */
data class ChecklistActions(
    val onToggleItem: (Int) -> Unit = {},
    val onItemTextChange: (Int, String) -> Unit = { _, _ -> },
    val onItemNoteChange: (Int, String) -> Unit = { _, _ -> },
    val onToggleItemNoteExpanded: (Int) -> Unit = {},
    val onDeleteItem: (Int) -> Unit = {},
    val onAddItem: (String) -> Unit = {},
    val onItemImeNext: (Int) -> Unit = {},
    val onItemFocused: () -> Unit = {},
    val onMoveItem: (Int, Int) -> Unit = { _, _ -> },
    val onToggleCompletedExpanded: () -> Unit = {},
    val onCheckAll: () -> Unit = {},
    val onUndoCheckAll: () -> Unit = {},
    val onOpenPasteDialog: () -> Unit = {},
    val onOpenVoiceCapture: () -> Unit = {},
    val onFinalizeReminder: () -> Unit = {},
)

/**
 * "¿Guardar los cambios?" al salir con cambios sin guardar:
 * ```
 * ¿Guardar los cambios?
 * Tenés cambios sin guardar. Si salís sin guardar, se pierden.
 *                                  [Descartar]  [Guardar]
 * ```
 * Tocar afuera o el "atrás" del sistema cierra el diálogo y sigue editando: la opción segura.
 */
@Composable
private fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.reminder_detail_unsaved_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.reminder_detail_unsaved_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(text = stringResource(R.string.reminder_detail_unsaved_save), onClick = onSave)
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.reminder_detail_unsaved_discard), onClick = onDiscard)
        },
    )
}

/** Píldora de acción de ayuda con IA. */
@Composable
private fun ChecklistActionPill(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF132029),
        border = BorderStroke(1.dp, Color(0xFF2A3D4C)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

/**
 * Fila de un ítem activo en la lista, fiel al mockup 04.B.
 *
 * Con [requestFocus] el campo toma el foco (y abre el teclado) apenas se dibuja: es lo que pasa al
 * tocar "Añadir elemento", así se escribe directo. Después avisa por [onFocused] para que el
 * ViewModel no lo vuelva a pedir en la próxima recomposición. "Siguiente" en el teclado
 * ([onImeNext]) crea el ítem de abajo.
 *
 * ```
 * ≡  ☐  Texto del ítem                 📝  ✕
 *        Nota (opcional)
 * ```
 * La primera línea es una Row centrada verticalmente: así el checkbox queda a la altura del
 * texto (con `Alignment.Top` y paddings a mano quedaba más abajo). La nota va en otra fila,
 * corrida para quedar debajo del texto.
 *
 * ≡ es la manija para reordenar: mantenerla apretada y arrastrar mueve el ítem (avisa por
 * [onDragStart], [onDrag] con cuánto se movió el dedo y [onDragEnd]). Quien arma la lista decide
 * cuándo cambiarlo de lugar; esta fila solo se dibuja corrida con [modifier].
 */
@Composable
private fun ChecklistActiveItemRow(
    item: ChecklistItem,
    isNoteExpanded: Boolean,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onImeNext: () -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleNote: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNoteVisible = !item.note.isNullOrBlank() || isNoteExpanded
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocused()
        }
    }
    // El gesto empieza en esta fila pero, si el ítem se mueve, la fila pasa a mostrar otro: con
    // rememberUpdatedState el gesto en curso sigue usando las lambdas más nuevas sin reiniciarse.
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Manija para arrastrar: mantener apretado y mover.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { currentOnDragStart() },
                            onDrag = { change, amount ->
                                change.consume()
                                currentOnDrag(amount.y)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reorder_lines),
                    contentDescription = stringResource(R.string.reminders_checklist_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }

            // Checkbox para marcar el ítem como terminado.
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_box_outline_blank),
                    contentDescription = stringResource(R.string.reminders_checklist_mark_done),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(4.dp))

            BasicTextField(
                value = item.text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onImeNext() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (item.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.reminders_checklist_item_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            IconButton(
                onClick = onToggleNote,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (isNoteVisible) R.drawable.ic_expand_less else R.drawable.ic_notes,
                    ),
                    contentDescription = stringResource(
                        if (isNoteVisible) R.string.reminders_checklist_hide_note else R.string.reminders_checklist_add_note,
                    ),
                    tint = if (isNoteVisible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.reminders_checklist_delete_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (isNoteVisible) {
            BasicTextField(
                value = item.note.orEmpty(),
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    // Debajo del texto: manija (32) + checkbox (32) + separación (4) a la izquierda,
                    // y los dos botones (32 + 32 + 2 + 2) a la derecha.
                    .padding(start = 68.dp, end = 68.dp, bottom = 4.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (item.note.isNullOrEmpty()) {
                            Text(
                                text = stringResource(R.string.reminders_checklist_note_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

/** Fila de un ítem completado (tachado con checkbox marcado). */
@Composable
private fun ChecklistCompletedItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_box),
                contentDescription = stringResource(R.string.reminders_checklist_unmark),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            if (!item.note.isNullOrBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    ),
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.reminders_checklist_delete_item),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Diálogo interactivo para pegar texto y parsear con IA. */
@Composable
private fun PasteItemsDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auto_awesome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = stringResource(R.string.reminders_checklist_paste))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.reminders_checklist_paste_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.reminders_checklist_paste_example),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
            }
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.reminders_checklist_generate),
                onClick = { onSubmit(text) },
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
            )
        },
    )
}

/** Ícono del tipo (en un recuadro) y el título, editable (vacío muestra "Título"). */
@Composable
private fun DetailHeader(
    @DrawableRes typeIcon: Int,
    title: String,
    onTitleChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        color = colors.onSurface,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(typeIcon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.weight(1f),
            textStyle = titleStyle,
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reminder_detail_title_hint),
                            style = titleStyle.copy(color = colors.onSurfaceVariant),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

/**
 * El panel de opciones, compacto: etiquetas en una fila y el resto en una grilla de 2 columnas.
 * Cada opción muestra su valor actual.
 *
 * Arriba, centrada, una flecha para plegarlo ([isCollapsed]): queda solo la flecha y la nota
 * gana todo ese alto para leerla completa. Tocarla otra vez lo vuelve a mostrar.
 * ```
 *        ⌄               ← desplegado: tocar pliega
 * 🏷 Etiquetas …
 * ┌ ⏰ … ┐ ┌ 📍 … ┐
 *
 *        ⌃               ← plegado: tocar despliega
 * ```
 */
@Composable
private fun ReminderOptionsPanel(
    uiState: ReminderDetailUiState,
    now: Instant,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onTagsClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onImageClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        // Siempre a todo el ancho: plegado no se achica, queda una barra con la flecha.
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        // Arrastrar la barra de la flecha también pliega (hacia abajo) o despliega (hacia arriba),
        // como la manija de un bottom sheet. Se decide al soltar: con haber movido el dedo 24dp
        // alcanza, o con un deslizamiento rápido aunque sea corto.
        val currentIsCollapsed by rememberUpdatedState(isCollapsed)
        val currentOnToggle by rememberUpdatedState(onToggleCollapsed)
        val thresholdPx = with(LocalDensity.current) { 24.dp.toPx() }
        var dragTotal by remember { mutableFloatStateOf(0f) }
        val dragState = rememberDraggableState { delta -> dragTotal += delta }

        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = if (isCollapsed) 0.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Todo el ancho del panel se puede tocar o arrastrar (no solo la flecha), y es bajo
            // para no comerle alto a la nota. Un Box y no un IconButton: el IconButton dibuja el
            // efecto del toque como un círculo, que a todo el ancho se vería raro.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragTotal = 0f },
                        onDragStopped = { velocity ->
                            // Positivo = hacia abajo (en pantalla, y crece hacia abajo).
                            val down = dragTotal > thresholdPx || velocity > 1000f
                            val up = dragTotal < -thresholdPx || velocity < -1000f
                            if ((down && !currentIsCollapsed) || (up && currentIsCollapsed)) currentOnToggle()
                        },
                    )
                    .clickable(role = Role.Button, onClick = onToggleCollapsed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (isCollapsed) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                    ),
                    contentDescription = stringResource(
                        if (isCollapsed) R.string.reminder_detail_options_show else R.string.reminder_detail_options_hide,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // expandVertically / shrinkVertically: el panel crece o se achica desde la flecha en
            // vez de aparecer o desaparecer de golpe.
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                ReminderOptionsGrid(
                    uiState = uiState,
                    now = now,
                    onTagsClick = onTagsClick,
                    onTimeClick = onTimeClick,
                    onPlaceClick = onPlaceClick,
                    onImageClick = onImageClick,
                    onBehaviorClick = onBehaviorClick,
                    onActionClick = onActionClick,
                )
            }
        }
    }
}

/** Las opciones en sí: etiquetas en una fila y el resto en una grilla de 2 columnas. */
@Composable
private fun ReminderOptionsGrid(
    uiState: ReminderDetailUiState,
    now: Instant,
    onTagsClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onImageClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onActionClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TagsOption(tags = uiState.tags, onClick = onTagsClick)
        OptionRow {
            OptionCard(
                icon = R.drawable.ic_alarm,
                title = stringResource(R.string.reminder_detail_time),
                value = timeValue(uiState.alarms, uiState.alarmRepeats, now),
                onClick = onTimeClick,
            )
            OptionCard(
                icon = R.drawable.ic_location_on,
                title = stringResource(R.string.reminder_detail_place),
                value = placeValue(uiState.places, uiState.placeName),
                onClick = onPlaceClick,
            )
        }
        OptionRow {
            OptionCard(
                icon = R.drawable.ic_visibility,
                title = stringResource(R.string.reminder_detail_behavior),
                value = stringResource(ReminderImportance.of(uiState.importance).label),
                onClick = onBehaviorClick,
            )
            OptionCard(
                icon = R.drawable.ic_bolt,
                title = stringResource(R.string.reminder_detail_notification_action),
                value = actionValue(uiState.action),
                onClick = onActionClick,
            )
        }
        OptionRow {
            OptionCard(
                icon = R.drawable.ic_image,
                title = stringResource(R.string.reminder_detail_image),
                value = imageValue(photoCount = uiState.photos.size, hasCover = uiState.photoPath != null),
                onClick = onImageClick,
            )
            OptionCard(
                icon = R.drawable.ic_attach_file,
                title = stringResource(R.string.reminder_detail_attachments),
                // TODO: los adjuntos todavía no existen en el modelo.
                value = stringResource(R.string.reminder_detail_attachments_none),
            )
        }
        OptionRow {
            OptionCard(
                icon = R.drawable.ic_share,
                title = stringResource(R.string.reminder_detail_share_option),
                value = stringResource(R.string.reminder_detail_share_value),
            )
            // Mitad vacía: así "Compartir" tiene el mismo ancho que el resto de la grilla.
            Spacer(Modifier.weight(1f))
        }
    }
}

/** Una fila de la grilla: dos opciones del mismo ancho y alto. */
@Composable
private fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * Etiquetas en una sola fila: ícono, título y una bolita del color de cada asignada
 * (o "Sin etiquetas"). Tocarla abre el modal de etiquetas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsOption(tags: List<Tag>, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OptionIcon(R.drawable.ic_tag)
        OptionTitle(stringResource(R.string.reminder_detail_tags))
        if (tags.isEmpty()) {
            OptionValue(stringResource(R.string.reminder_detail_tags_none))
        } else {
            // Las bolitas no tienen texto: para lectores de pantalla se leen los nombres.
            val names = tags.joinToString { it.name }
            FlowRow(
                modifier = Modifier.clearAndSetSemantics { contentDescription = names },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(tag.colorArgb)),
                    )
                }
            }
        }
    }
}

/** Una opción en su recuadro: ícono a la izquierda, título y valor actual (en una línea). */
@Composable
private fun RowScope.OptionCard(
    @DrawableRes icon: Int,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    // Con onClick se puede tocar (abre su modal); sin él, por ahora solo muestra el valor.
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .then(clickModifier),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionIcon(icon)
            Column {
                OptionTitle(title)
                OptionValue(value)
            }
        }
    }
}

@Composable
private fun OptionIcon(@DrawableRes icon: Int) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun OptionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun OptionValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * El próximo aviso: "Hoy 18:00 · No se repite", "Sáb 10:00 · No se repite", con varios
 * "Hoy 18:00 +2", o "Sin fecha". Si ya pasaron todos, muestra el último.
 */
@Composable
private fun timeValue(alarms: List<Instant>, repeats: Map<Instant, AlarmRepeat>, now: Instant): String {
    val next = alarms.filter { !it.isBefore(now) }.minOrNull() ?: alarms.maxOrNull()
        ?: return stringResource(R.string.reminder_detail_time_none)
    val locale = rememberAppLocale()
    val whenText = if (isSameDay(next, now)) {
        val time = next.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm", locale))
        stringResource(R.string.reminder_detail_time_today, time)
    } else {
        formatReminderTime(next, now, locale)
    }
    val extra = alarms.size - 1
    return if (extra > 0) {
        stringResource(R.string.reminder_detail_time_more, whenText, extra)
    } else {
        val repeat = repeats[next] ?: AlarmRepeat.None
        if (repeat == AlarmRepeat.None) {
            stringResource(R.string.reminder_detail_time_value, whenText)
        } else {
            stringResource(R.string.reminder_detail_time_repeat, whenText, stringResource(RepeatOption.of(repeat).label))
        }
    }
}

/**
 * La acción de notificación en una línea: "Llamar: 11 5555-5555", "WhatsApp: 11 5555-5555",
 * "Instagram: @usuario", "Correo: ana@mail.com" o "Sin acción".
 */
@Composable
private fun actionValue(action: NotificationAction?): String = when (action) {
    null -> stringResource(R.string.reminder_detail_notification_action_none)
    is NotificationAction.Call -> stringResource(R.string.reminder_action_summary_call, action.phone)
    is NotificationAction.Email -> stringResource(R.string.reminder_action_summary_email, action.to)
    is NotificationAction.Message -> stringResource(
        when (action.channel) {
            MessageChannel.Sms -> R.string.reminder_action_summary_sms
            MessageChannel.WhatsApp -> R.string.reminder_action_summary_whatsapp
            MessageChannel.Instagram -> R.string.reminder_action_summary_instagram
        },
        action.to,
    )
}

/**
 * El primer lugar: "Al llegar a Casa" (o "Al llegar a un lugar" si no se sabe el nombre), con
 * varios "Al llegar a Casa +1", o "Sin ubicación".
 */
@Composable
private fun placeValue(places: List<PlaceAlert>, placeName: String?): String {
    if (places.isEmpty()) return stringResource(R.string.reminder_detail_place_none)
    val isLeave = places.firstOrNull()?.event == PlaceAlertEvent.Leave
    val first = if (placeName != null) {
        if (isLeave) {
            stringResource(R.string.reminder_detail_place_leave, placeName)
        } else {
            stringResource(R.string.reminder_detail_place_arrive, placeName)
        }
    } else {
        if (isLeave) {
            stringResource(R.string.reminder_detail_place_leave_unknown)
        } else {
            stringResource(R.string.reminder_detail_place_unknown)
        }
    }
    val extra = places.size - 1
    return if (extra > 0) stringResource(R.string.reminder_detail_place_more, first, extra) else first
}

/** "Sin imagen", "Portada · 3 fotos", o "2 fotos" si todavía no eligió portada. */
@Composable
private fun imageValue(photoCount: Int, hasCover: Boolean): String = when {
    photoCount == 0 && !hasCover -> stringResource(R.string.reminder_detail_image_none)
    // Una nota vieja puede tener portada sin fotos guardadas (de antes de que existieran).
    photoCount == 0 -> stringResource(R.string.reminder_detail_image_cover)
    hasCover -> pluralStringResource(R.plurals.reminder_detail_image_cover_count, photoCount, photoCount)
    else -> pluralStringResource(R.plurals.reminder_detail_image_count, photoCount, photoCount)
}

/** Para las previews: la pantalla con un estado inventado y sin acciones. */
@Composable
private fun DetailPreviewFrame(uiState: ReminderDetailUiState, now: Instant = Instant.now()) {
    RememberAppTheme {
        ReminderDetailContent(
            uiState = uiState,
            now = now,
            onBack = {},
            onShare = {},
            onDone = {},
            onTitleChange = {},
            onTagsClick = {},
            onTimeClick = {},
            onPlaceClick = {},
            onImageClick = {},
            onBehaviorClick = {},
            onActionClick = {},
            onDescriptionChange = {},
        )
    }
}

/** Estado de una nota existente ya cargada, con su título y descripción en los campos. */
private fun loadedState(reminder: Reminder) = ReminderDetailUiState(
    isLoading = false,
    reminder = reminder,
    title = reminder.title.orEmpty(),
    description = reminder.description.orEmpty(),
    tagIds = reminder.tags.map { it.id }.toSet(),
    allTags = reminder.tags,
    alarms = reminder.alarms.map { it.at },
    importance = reminder.importance,
    action = reminder.action,
    photos = reminder.photos,
    photoPath = reminder.photoPath,
)

@Preview(name = "Con foto, etiqueta y aviso", showBackground = true, heightDp = 900)
@Composable
private fun ReminderDetailContentPreview() {
    val now = Instant.now()
    // La de la vacuna: con foto, descripción, etiqueta y aviso hoy.
    DetailPreviewFrame(loadedState(SampleReminders.previewReminders(now).first()), now)
}

@Preview(name = "Solo título", showBackground = true, heightDp = 700)
@Composable
private fun ReminderDetailContentSimplePreview() {
    DetailPreviewFrame(loadedState(Reminder(title = "Sacar turno en el banco", createdAt = Instant.now())))
}

@Preview(name = "Nota nueva", showBackground = true, heightDp = 700)
@Composable
private fun ReminderDetailContentNewPreview() {
    DetailPreviewFrame(ReminderDetailUiState(isLoading = false, isNew = true))
}

@Preview(name = "No encontrado", showBackground = true, heightDp = 300)
@Composable
private fun ReminderDetailContentNotFoundPreview() {
    DetailPreviewFrame(ReminderDetailUiState(isLoading = false))
}
