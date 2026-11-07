package com.example.uade.rememberapp.ui.reminders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.AiAnalysisResult
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.ChecklistItem
import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.AiReminderService
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ImportPhotosUseCase
import com.example.uade.rememberapp.domain.usecase.MarkReminderDoneUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.SaveCoverUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.navigation.Routes
import com.example.uade.rememberapp.ui.reminders.model.NotificationActionKind
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.ReminderDetailSection
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.model.resolveReminderTime
import com.example.uade.rememberapp.ui.reminders.model.toDomain
import com.example.uade.rememberapp.ui.reminders.model.toUi
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * ViewModel de la pantalla de un recordatorio. Sirve para dos casos:
 *
 * - **Existente** (`reminders/{id}`): lee el recordatorio cuyo id viene en la ruta.
 * - **Nota nueva** (`notes/new`, desde "+" → "Nota"): la ruta no trae id, así que no consulta
 *   la base; arranca vacía y se crea al tocar ✓.
 *
 * El id llega por [SavedStateHandle]: Navigation pone ahí los argumentos de la ruta, y además
 * sobrevive si Android cierra el proceso en segundo plano.
 *
 * Se editan el título y la descripción, y se guardan con ✓ (← vuelve sin guardar).
 *
 * TODO: abrir el sheet de cada opción (etiquetas, fecha y hora, ubicación…). También se va a
 * abrir por deep link desde la notificación.
 */
class ReminderDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getReminder: GetReminderUseCase,
    private val saveReminder: SaveReminderUseCase,
    tagRepository: TagRepository,
    private val importPhotos: ImportPhotosUseCase,
    private val saveCover: SaveCoverUseCase,
    observePlaces: ObservePlacesUseCase,
    private val aiReminderService: AiReminderService? = null,
    private val markReminderDone: MarkReminderDoneUseCase? = null,
    /**
     * Modo borrador: editar un recordatorio que todavía no está guardado (ej. una propuesta de la
     * IA en la revisión de una nota de voz). Es el mismo editor, pero ✓ no guarda en la base:
     * deja el resultado en [ReminderDetailUiState.draftResult] para que lo use quien lo abrió.
     */
    draft: Reminder? = null,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    /** null = nota nueva (la ruta no trae id). */
    private val reminderId: Long? = savedStateHandle[Routes.ARG_REMINDER_ID]

    private val _uiState = MutableStateFlow(
        if (draft != null) {
            ReminderDetailUiState(isLoading = false, isDraft = true).withLoaded(draft)
        } else if (reminderId == null) {
            // Nota nueva: puede venir con el título de la nota rápida y con la lista ya abierta
            // (un ítem vacío, para escribir el primero).
            ReminderDetailUiState(
                isLoading = false,
                isNew = true,
                title = savedStateHandle.get<String>(Routes.ARG_TITLE).orEmpty(),
                items = if (savedStateHandle.get<Boolean>(Routes.ARG_WITH_LIST) == true) {
                    listOf(ChecklistItem(text = ""))
                } else {
                    emptyList()
                },
                // Con la lista abierta, el foco va al primer ítem para escribir enseguida.
                focusItemIndex = if (savedStateHandle.get<Boolean>(Routes.ARG_WITH_LIST) == true) 0 else null,
            )
        } else {
            ReminderDetailUiState()
        },
    )
    val uiState: StateFlow<ReminderDetailUiState> = _uiState.asStateFlow()

    init {
        // Se tocó un chip de la card en la Home (hora, lugar, etiquetas): al cargar se abre ese
        // modal. Se saca de la ruta guardada para que no se vuelva a abrir si Android recrea
        // la pantalla (ej. al girar el teléfono, o al volver de otra app).
        val openSection = savedStateHandle.get<String>(Routes.ARG_OPEN_SECTION)
            ?.let { name -> ReminderDetailSection.entries.firstOrNull { it.name == name } }
        savedStateHandle.remove<String>(Routes.ARG_OPEN_SECTION)
        if (reminderId != null) {
            viewModelScope.launch {
                val reminder = getReminder(reminderId)
                _uiState.update { it.copy(isLoading = false).withLoaded(reminder) }
                // Recién con el recordatorio cargado: el modal arranca con sus valores actuales.
                if (reminder != null) {
                    when (openSection) {
                        ReminderDetailSection.Time -> onTimeClick()
                        ReminderDetailSection.Place -> onPlaceClick()
                        ReminderDetailSection.Tags -> onTagsClick()
                        null -> Unit
                    }
                }
            }
        }
        // Todas las etiquetas, al día: para mostrar las asignadas con su nombre y color actual
        // (si se renombra o borra una desde el panel, se ve enseguida).
        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
        viewModelScope.launch {
            observePlaces().collect { places ->
                val names = places.associate { it.id to it.name }
                _uiState.update { it.copy(favoritePlaces = places, placeNames = names) }
            }
        }
    }

    /** Tocar "Etiquetas" en el panel de opciones abre el modal de etiquetas. */
    fun onTagsClick() {
        _uiState.update { it.copy(tagsEditor = it.tagIds) }
    }

    /** El panel del modal cambió las asignadas: solo cambia la copia del modal. */
    fun onTagsChanged(ids: Set<Long>) {
        _uiState.update { state -> state.tagsEditor?.let { state.copy(tagsEditor = ids) } ?: state }
    }

    /** "Guardar" del modal: lo asignado pasa al borrador. Se persiste recién con ✓. */
    fun onSaveTags() {
        _uiState.update { state ->
            state.tagsEditor?.let { state.copy(tagIds = it, tagsEditor = null) } ?: state
        }
    }

    /** "Cancelar" (o cerrar el modal): descarta lo asignado o quitado en el modal. */
    fun onCancelTags() {
        _uiState.update { it.copy(tagsEditor = null) }
    }

    // --- Modal "Cómo avisar" (importancia) ---------------------------------------------------
    // Mismo patrón que los otros modales: trabaja sobre una copia que "Guardar" pasa al borrador.

    /** Tocar "Comportamiento" abre el modal con el nivel actual marcado. */
    fun onBehaviorClick() {
        _uiState.update { it.copy(importanceEditor = it.importance) }
    }

    /** Elegir un nivel en el modal: solo cambia la copia del modal (siempre hay uno marcado). */
    fun onImportanceSelected(importance: Importance) {
        _uiState.update { state -> state.importanceEditor?.let { state.copy(importanceEditor = importance) } ?: state }
    }

    /** "Guardar": el nivel marcado pasa al borrador. Se persiste recién con ✓. */
    fun onSaveImportance() {
        _uiState.update { state ->
            state.importanceEditor?.let { state.copy(importance = it, importanceEditor = null) } ?: state
        }
    }

    /** "Cancelar" (o cerrar el modal): deja la importancia como estaba. */
    fun onCancelImportance() {
        _uiState.update { it.copy(importanceEditor = null) }
    }

    // --- Modal "Acción de notificación" ------------------------------------------------------
    // Dos pasos (lista → datos). Mismo patrón que los otros modales: trabaja sobre una copia que
    // "Guardar" pasa al borrador `action`; se persiste recién con ✓.

    /**
     * Tocar "Acción de notificación": sin acción, abre la lista; con una, abre directo sus datos
     * (es lo más probable que se quiera cambiar).
     */
    fun onActionClick() {
        _uiState.update { state ->
            val editor = when (val action = state.action) {
                null -> ActionEditor()
                is NotificationAction.Call -> ActionEditor(
                    step = ActionEditorStep.Form,
                    kind = NotificationActionKind.Call,
                    target = action.phone,
                )
                is NotificationAction.Message -> ActionEditor(
                    step = ActionEditorStep.Form,
                    kind = NotificationActionKind.Message,
                    channel = action.channel,
                    target = action.to,
                    text = action.text.orEmpty(),
                )
                is NotificationAction.Email -> ActionEditor(
                    step = ActionEditorStep.Form,
                    kind = NotificationActionKind.Email,
                    target = action.to,
                    text = action.subject.orEmpty(),
                )
            }
            state.copy(actionEditor = editor.copy(hadAction = state.action != null))
        }
    }

    /** Elegir una acción de la lista pasa a sus datos. Si es otra, arranca con los campos vacíos. */
    fun onActionKindSelected(kind: NotificationActionKind) {
        updateActionEditor { editor ->
            if (editor.kind == kind) {
                editor.copy(step = ActionEditorStep.Form)
            } else {
                ActionEditor(step = ActionEditorStep.Form, kind = kind, hadAction = editor.hadAction)
            }
        }
    }

    /** ← en los datos: vuelve a la lista (sin perder lo escrito, por si vuelve a la misma). */
    fun onActionBack() {
        updateActionEditor { it.copy(step = ActionEditorStep.List) }
    }

    fun onActionChannel(channel: MessageChannel) {
        updateActionEditor { it.copy(channel = channel) }
    }

    /** Instagram se guarda sin "@" (la UI lo muestra como prefijo). */
    fun onActionTarget(target: String) {
        updateActionEditor { it.copy(target = target.removePrefix("@")) }
    }

    fun onActionText(text: String) {
        updateActionEditor { it.copy(text = text) }
    }

    /** "Guardar": arma la acción con lo escrito y la pasa al borrador. Sin destinatario no hace nada. */
    fun onSaveAction() {
        _uiState.update { state ->
            val editor = state.actionEditor ?: return@update state
            if (!editor.canSave) return@update state
            val target = editor.target.trim()
            val text = editor.text.trim().ifEmpty { null }
            val action = when (editor.kind) {
                NotificationActionKind.Call -> NotificationAction.Call(phone = target)
                NotificationActionKind.Message -> NotificationAction.Message(editor.channel, to = target, text = text)
                NotificationActionKind.Email -> NotificationAction.Email(to = target, subject = text)
                null -> return@update state
            }
            state.copy(action = action, actionEditor = null)
        }
    }

    /** "Quitar acción": el recordatorio queda sin acción (se persiste con ✓). */
    fun onRemoveAction() {
        _uiState.update { it.copy(action = null, actionEditor = null) }
    }

    /** "Cancelar" (o cerrar el modal): deja la acción como estaba. */
    fun onCancelAction() {
        _uiState.update { it.copy(actionEditor = null) }
    }

    private fun updateActionEditor(transform: (ActionEditor) -> ActionEditor) {
        _uiState.update { state -> state.copy(actionEditor = state.actionEditor?.let(transform)) }
    }

    // --- Modal "Imágenes" ---------------------------------------------------------------------
    // Mismo patrón que los otros modales: trabaja sobre una copia (PhotosEditor) que "Guardar"
    // pasa al borrador `photos` / `photoPath`; se persiste recién con ✓.
    //
    // Los archivos sí se crean al momento (copiar las fotos y recortar la portada tarda: no se
    // puede dejar para el ✓). TODO: borrar los que queden sin usar (fotos quitadas, modal
    // cancelado, portadas reemplazadas).

    /** Tocar "Imagen" con fotos ya elegidas abre el modal (sin fotos, la pantalla abre el selector). */
    fun onImageClick() {
        _uiState.update { it.copy(photosEditor = PhotosEditor(photos = it.photos, coverPath = it.photoPath)) }
    }

    /**
     * Volvió el selector de Android con [uris] (vacía = no eligió nada). Las copia dentro de la
     * app, hasta completar 3, y abre el modal si todavía no estaba abierto.
     */
    fun onPhotosPicked(uris: List<String>) {
        if (uris.isEmpty()) return
        val state = _uiState.value
        val editor = state.photosEditor ?: PhotosEditor(photos = state.photos, coverPath = state.photoPath)
        _uiState.update { it.copy(photosEditor = editor.copy(isImporting = true)) }
        viewModelScope.launch {
            val imported = try {
                importPhotos(uris, alreadyCount = editor.photos.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Una foto que no se pudo leer (ej. se borró de la galería): se ignora.
                emptyList()
            }
            updatePhotosEditor { current ->
                current.copy(
                    photos = (current.photos + imported.map { ReminderPhoto(it) }).take(MAX_PHOTOS),
                    isImporting = false,
                )
            }
        }
    }

    /** ✕ en una miniatura: la quita. Si era la portada, la nota queda sin portada. */
    fun onRemovePhoto(path: String) {
        updatePhotosEditor { editor ->
            val removed = editor.photos.firstOrNull { it.path == path }
            editor.copy(
                photos = editor.photos.filterNot { it.path == path },
                coverPath = if (removed?.isCover == true) null else editor.coverPath,
            )
        }
    }

    /** Tocar una miniatura abre el recorte para usarla de portada. */
    fun onPhotoClick(path: String) {
        updatePhotosEditor { it.copy(cropping = path) }
    }

    /**
     * "Guardar" en el recorte: guarda la porción elegida como portada y marca esa foto (y solo
     * esa) como la portada.
     */
    fun onCropConfirm(crop: CropRect) {
        val editor = _uiState.value.photosEditor ?: return
        val source = editor.cropping ?: return
        if (editor.isSavingCover) return
        updatePhotosEditor { it.copy(isSavingCover = true) }
        viewModelScope.launch {
            val cover = try {
                saveCover(source, crop)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            updatePhotosEditor { current ->
                if (cover == null) {
                    current.copy(cropping = null, isSavingCover = false)
                } else {
                    current.copy(
                        photos = current.photos.map { it.copy(isCover = it.path == source) },
                        coverPath = cover,
                        cropping = null,
                        isSavingCover = false,
                    )
                }
            }
        }
    }

    /** ✕ o "Cancelar" en el recorte: vuelve al modal sin cambiar la portada. */
    fun onCropDismiss() {
        updatePhotosEditor { it.copy(cropping = null) }
    }

    /** "Guardar" del modal: las fotos y la portada pasan al borrador. Se persisten con ✓. */
    fun onSavePhotos() {
        _uiState.update { state ->
            val editor = state.photosEditor ?: return@update state
            val (photos, cover) = withDefaultCover(editor.photos, editor.coverPath)
            state.copy(photos = photos, photoPath = cover, photosEditor = null)
        }
    }

    /**
     * "Agregar banner": la foto elegida se copia dentro de la app y pasa a ser la portada (la
     * primera de la pila). Es el camino explícito; el otro es adjuntar fotos con "Imagen", donde
     * la primera queda de portada si no se elige otra (ver [withDefaultCover]).
     */
    fun onBannerPicked(uri: String?) {
        if (uri == null) return
        viewModelScope.launch {
            val imported = try {
                importPhotos(listOf(uri), alreadyCount = 0).firstOrNull()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } ?: return@launch
            _uiState.update { state ->
                val photos = listOf(ReminderPhoto(imported, isCover = true)) +
                    state.photos.map { it.copy(isCover = false) }
                state.copy(photos = photos.take(MAX_PHOTOS), photoPath = imported)
            }
        }
    }

    /**
     * Si hay fotos pero ninguna es la portada, la primera pasa a serlo (sin recortar; se puede
     * recortar después desde "Imágenes"). Así una nota con fotos siempre tiene banner, sin que el
     * usuario tenga que saber que hay que elegirlo.
     */
    private fun withDefaultCover(photos: List<ReminderPhoto>, cover: String?): Pair<List<ReminderPhoto>, String?> {
        if (cover != null || photos.isEmpty()) return photos to cover
        val first = photos.first()
        return photos.map { it.copy(isCover = it.path == first.path) } to first.path
    }

    /** "Cancelar" (o cerrar el modal): deja las fotos como estaban. */
    fun onCancelPhotos() {
        _uiState.update { it.copy(photosEditor = null) }
    }

    private fun updatePhotosEditor(transform: (PhotosEditor) -> PhotosEditor) {
        _uiState.update { state -> state.copy(photosEditor = state.photosEditor?.let(transform)) }
    }

    // --- Modal "Ubicación" --------------------------------------------------------------------

    /** Tocar "Ubicación" abre el modal con la alerta existente seleccionada (o vacío). */
    fun onPlaceClick() {
        val currentAlert = _uiState.value.places.firstOrNull()
        _uiState.update {
            it.copy(
                placeEditor = PlaceEditor(
                    selectedPlaceId = currentAlert?.placeId,
                    event = currentAlert?.event?.toUi() ?: PlaceEvent.Arrive,
                ),
            )
        }
    }

    /** Tocar un lugar lo marca; tocar el marcado lo desmarca. */
    fun onPlaceSelected(placeId: Long) {
        updatePlaceEditor { it.copy(selectedPlaceId = if (it.selectedPlaceId == placeId) null else placeId) }
    }

    fun onPlaceEventSelected(event: PlaceEvent) {
        updatePlaceEditor { it.copy(event = event) }
    }

    fun onSearchAddress() {
        _uiState.update { it.copy(isCreatingPlace = true) }
    }

    fun onDismissPlaceSheet() {
        _uiState.update { it.copy(isCreatingPlace = false) }
    }

    /** "Guardar": pasa la selección del modal al borrador de lugares. */
    fun onSavePlace() {
        val editor = _uiState.value.placeEditor
        val newPlaces = if (editor?.selectedPlaceId != null) {
            listOf(PlaceAlert(placeId = editor.selectedPlaceId, event = editor.event.toDomain()))
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                placeEditor = null,
                placesDraft = newPlaces,
            )
        }
    }

    /** "Cancelar" o cerrar el modal descarta los cambios no guardados. */
    fun onClosePlace() {
        _uiState.update { it.copy(placeEditor = null) }
    }

    private fun updatePlaceEditor(transform: (PlaceEditor) -> PlaceEditor) {
        _uiState.update { state -> state.copy(placeEditor = state.placeEditor?.let(transform)) }
    }

    // --- Modal "Establecer recordatorio" -------------------------------------------------------
    // Trabaja sobre una copia (AlarmsEditor): "Guardar" la pasa al borrador `alarms` y "Cancelar"
    // la descarta. Como el resto, el borrador se guarda en la base recién con ✓.

    /**
     * Tocar "Recordatorio" abre el modal con una tarjeta por aviso. Cada hora ya elegida se
     * muestra como "Elegir fecha…" con su día y hora. Sin avisos, arranca con una tarjeta vacía.
     */
    fun onTimeClick() {
        _uiState.update { state ->
            val items = state.alarms.map { at ->
                val local = at.atZone(zone)
                AlarmDraft(
                    at = at,
                    selectedTime = TimeShortcut.PickDate,
                    pickedDate = local.toLocalDate(),
                    pickedTime = local.toLocalTime(),
                    selectedRepeat = RepeatOption.of(state.alarmRepeats[at] ?: AlarmRepeat.None),
                )
            }.ifEmpty { listOf(AlarmDraft()) }
            // Con una sola tarjeta, desplegada; con varias, todas plegadas (se ve el resumen).
            state.copy(alarmsEditor = AlarmsEditor(items = items, expanded = if (items.size == 1) 0 else null))
        }
    }

    /**
     * Tocar un atajo en la tarjeta [index] lo elige y calcula el aviso en ese momento (ej. "En
     * 1 hora" = ahora + 1 h). Tocarlo de nuevo lo deselecciona. "Elegir fecha…" abre el calendario.
     */
    fun onAlarmShortcut(index: Int, shortcut: TimeShortcut) {
        if (shortcut == TimeShortcut.PickDate) {
            updateEditor { it.copy(datePickerFor = index) }
            return
        }
        updateAlarm(index) { draft ->
            if (draft.selectedTime == shortcut) {
                AlarmDraft(selectedRepeat = draft.selectedRepeat)
            } else {
                draft.copy(
                    selectedTime = shortcut,
                    at = resolveAt(shortcut, date = null, time = null),
                )
            }
        }
    }

    /** "Listo" en el calendario: en la tarjeta que lo abrió queda "Elegir fecha…" con ese día y hora. */
    fun onAlarmDatePicked(date: LocalDate, time: LocalTime?) {
        val index = _uiState.value.alarmsEditor?.datePickerFor ?: return
        updateAlarm(index) {
            it.copy(
                selectedTime = TimeShortcut.PickDate,
                pickedDate = date,
                pickedTime = time,
                at = resolveAt(TimeShortcut.PickDate, date, time),
            )
        }
        updateEditor { it.copy(datePickerFor = null) }
    }

    fun onAlarmDatePickerDismiss() {
        updateEditor { it.copy(datePickerFor = null) }
    }

    /** TODO: guardar la repetición cuando exista en el modelo; por ahora solo se marca. */
    fun onAlarmRepeat(index: Int, option: RepeatOption) {
        updateAlarm(index) { it.copy(selectedRepeat = option) }
    }

    /** El chevron de la tarjeta: la despliega (y pliega la otra) o la pliega. */
    fun onToggleAlarmExpanded(index: Int) {
        updateEditor { it.copy(expanded = if (it.expanded == index) null else index) }
    }

    /** "+ Añadir recordatorio": suma una tarjeta vacía al final y la despliega. */
    fun onAddAlarm() {
        updateEditor { it.copy(items = it.items + AlarmDraft(), expanded = it.items.size) }
    }

    /** "Borrar todo": deja una sola tarjeta vacía. Al guardar, la nota queda sin avisos por hora. */
    fun onClearAlarms() {
        updateEditor { AlarmsEditor(items = listOf(AlarmDraft()), expanded = 0) }
    }

    /** "Cancelar" (o cerrar el modal): descarta lo hecho en el modal. */
    fun onCancelAlarms() {
        _uiState.update { it.copy(alarmsEditor = null) }
    }

    /**
     * "Guardar": el borrador pasa a ser lo elegido en las tarjetas, ordenado y sin tarjetas vacías
     * ni horas repetidas. Se guarda en la base recién con ✓.
     */
    fun onSaveAlarms() {
        _uiState.update { state ->
            val editor = state.alarmsEditor ?: return@update state
            val chosen = editor.items.filter { it.at != null }.distinctBy { it.at }
            state.copy(
                alarms = chosen.mapNotNull { it.at }.sorted(),
                // Solo las que se repiten: sin entrada en el mapa = no se repite.
                alarmRepeats = chosen
                    .filter { it.selectedRepeat != RepeatOption.None }
                    .associate { it.at!! to it.selectedRepeat.repeat },
                alarmsEditor = null,
            )
        }
    }

    private fun updateEditor(transform: (AlarmsEditor) -> AlarmsEditor) {
        _uiState.update { state -> state.copy(alarmsEditor = state.alarmsEditor?.let(transform)) }
    }

    private fun updateAlarm(index: Int, transform: (AlarmDraft) -> AlarmDraft) {
        updateEditor { editor ->
            editor.copy(items = editor.items.mapIndexed { i, draft -> if (i == index) transform(draft) else draft })
        }
    }

    /** El momento del aviso para lo elegido, con la misma regla que la nota rápida. */
    private fun resolveAt(shortcut: TimeShortcut, date: LocalDate?, time: LocalTime?): Instant? =
        resolveReminderTime(shortcut, date, time, now().atZone(zone))?.toInstant()

    fun onToggleItem(index: Int) {
        _uiState.update { state ->
            val updated = state.items.mapIndexed { i, item ->
                if (i == index) item.copy(isChecked = !item.isChecked) else item
            }
            // Tocar un ítem a mano después de "Marcar todo" confirma el nuevo estado.
            state.copy(items = updated, checkedBeforeCheckAll = null)
        }
    }

    /**
     * "Marcar todo": tilda todos los ítems y guarda cuáles estaban tildados, así el mismo botón
     * pasa a "Deshacer" (la protección contra un toque sin querer). Se persiste recién con ✓.
     */
    fun onCheckAll() {
        _uiState.update { state ->
            if (state.items.all { it.isChecked }) return@update state
            state.copy(
                checkedBeforeCheckAll = state.items.map { it.isChecked },
                items = state.items.map { it.copy(isChecked = true) },
            )
        }
    }

    /**
     * "Deshacer" de "Marcar todo": vuelve a dejar tildados solo los que lo estaban. Restaura solo
     * las marcas (no los textos), así no se pierde nada de lo que se escribió mientras tanto.
     */
    fun onUndoCheckAll() {
        _uiState.update { state ->
            val previous = state.checkedBeforeCheckAll ?: return@update state
            if (previous.size != state.items.size) return@update state.copy(checkedBeforeCheckAll = null)
            state.copy(
                items = state.items.mapIndexed { i, item -> item.copy(isChecked = previous[i]) },
                checkedBeforeCheckAll = null,
            )
        }
    }

    /** El ítem que tenía que recibir el foco ya lo recibió (ver [ReminderDetailUiState.focusItemIndex]). */
    fun onItemFocused() {
        _uiState.update { it.copy(focusItemIndex = null) }
    }

    fun onItemTextChange(index: Int, text: String) {
        _uiState.update { state ->
            val updated = state.items.mapIndexed { i, item ->
                if (i == index) item.copy(text = text) else item
            }
            state.copy(items = updated)
        }
    }

    fun onItemNoteChange(index: Int, note: String) {
        _uiState.update { state ->
            val updated = state.items.mapIndexed { i, item ->
                if (i == index) item.copy(note = note.ifBlank { null }) else item
            }
            state.copy(items = updated)
        }
    }

    fun onToggleItemNoteExpanded(index: Int) {
        _uiState.update { state ->
            val current = state.expandedItemNoteIndex
            state.copy(expandedItemNoteIndex = if (current == index) null else index)
        }
    }

    fun onDeleteItem(index: Int) {
        _uiState.update { state ->
            val updated = state.items.filterIndexed { i, _ -> i != index }
            state.copy(
                items = updated,
                expandedItemNoteIndex = if (state.expandedItemNoteIndex == index) null else state.expandedItemNoteIndex,
                checkedBeforeCheckAll = null,
            )
        }
    }

    /**
     * Agrega un ítem al final ("Añadir elemento" o "Agregar lista") y le pasa el foco, así se
     * puede escribir directamente sin tocarlo.
     */
    fun onAddItem(text: String = "") {
        _uiState.update { state ->
            state.copy(
                items = state.items + ChecklistItem(text = text),
                focusItemIndex = state.items.size,
                checkedBeforeCheckAll = null,
            )
        }
    }

    /**
     * "Siguiente" del teclado en el ítem [index]: crea uno nuevo justo debajo y pasa el foco ahí,
     * como en Keep o Notas. Si el ítem está vacío no crea otro (evita filas vacías en cadena).
     */
    fun onItemImeNext(index: Int) {
        _uiState.update { state ->
            if (state.items.getOrNull(index)?.text.isNullOrBlank()) return@update state
            val list = state.items.toMutableList().apply { add(index + 1, ChecklistItem(text = "")) }
            state.copy(
                items = list,
                focusItemIndex = index + 1,
                checkedBeforeCheckAll = null,
                // La nota abierta se corre una posición si estaba debajo del ítem nuevo.
                expandedItemNoteIndex = state.expandedItemNoteIndex?.let { if (it > index) it + 1 else it },
            )
        }
    }

    fun onMoveItem(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.items.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val moved = list.removeAt(fromIndex)
                list.add(toIndex, moved)
                state.copy(items = list)
            } else {
                state
            }
        }
    }

    fun onToggleCompletedExpanded() {
        _uiState.update { it.copy(isCompletedExpanded = !it.isCompletedExpanded) }
    }

    fun onOpenPasteDialog() {
        _uiState.update { it.copy(isPasteDialogOpen = true) }
    }

    fun onClosePasteDialog() {
        _uiState.update { it.copy(isPasteDialogOpen = false) }
    }

    fun onOpenVoiceCapture() {
        _uiState.update { it.copy(isVoiceCaptureOpen = true) }
    }

    fun onCloseVoiceCapture() {
        _uiState.update { it.copy(isVoiceCaptureOpen = false) }
    }

    fun onPasteItemsSubmit(rawText: String) {
        if (rawText.isBlank()) {
            _uiState.update { it.copy(isPasteDialogOpen = false) }
            return
        }
        _uiState.update { it.copy(isPasteDialogOpen = false, isGeneratingItems = true) }
        viewModelScope.launch {
            try {
                val result = aiReminderService?.analyzeText(rawText, referenceIso())
                val firstProposal = result?.getOrNull()?.proposedReminders?.firstOrNull()
                val extracted = result?.getOrNull()?.let { analysis ->
                    analysis.proposedReminders.flatMap { p ->
                        if (p.items.isNotEmpty()) {
                            p.items
                        } else {
                            listOf(ChecklistItem(text = p.title, note = null))
                        }
                    }
                }?.ifEmpty { null } ?: fallbackParseChecklist(rawText)

                _uiState.update { state ->
                    val detectedListName = firstProposal?.listName
                    val proposalTitle = firstProposal?.title
                    val newTitle = if (state.title.isBlank()) {
                        if (!proposalTitle.isNullOrBlank() && proposalTitle != "Nuevo recordatorio") {
                            proposalTitle
                        } else if (!detectedListName.isNullOrBlank()) {
                            detectedListName
                        } else {
                            state.title
                        }
                    } else state.title
                    val proposedAlarms = firstProposal?.alarms.orEmpty().map { it.at }
                    val updatedAlarms = if (state.alarms.isEmpty() && proposedAlarms.isNotEmpty()) proposedAlarms else state.alarms
                    state.copy(
                        items = state.items + extracted,
                        title = newTitle,
                        alarms = updatedAlarms,
                        isGeneratingItems = false,
                    )
                }
            } catch (e: Exception) {
                val fallback = fallbackParseChecklist(rawText)
                _uiState.update { it.copy(items = it.items + fallback, isGeneratingItems = false) }
            }
        }
    }

    fun onVoiceItemsRecorded(audioFilePath: String) {
        _uiState.update { it.copy(isVoiceCaptureOpen = false, isGeneratingItems = true) }
        viewModelScope.launch {
            try {
                val result = aiReminderService?.processAudio(audioFilePath, referenceIso())
                val firstProposal = result?.getOrNull()?.proposedReminders?.firstOrNull()
                val extracted = result?.getOrNull()?.let { analysis ->
                    analysis.proposedReminders.flatMap { p ->
                        if (p.items.isNotEmpty()) {
                            p.items
                        } else {
                            listOf(ChecklistItem(text = p.title, note = null))
                        }
                    }
                } ?: emptyList()

                _uiState.update { state ->
                    val detectedListName = firstProposal?.listName
                    val proposalTitle = firstProposal?.title
                    val newTitle = if (state.title.isBlank()) {
                        if (!proposalTitle.isNullOrBlank() && proposalTitle != "Nuevo recordatorio") {
                            proposalTitle
                        } else if (!detectedListName.isNullOrBlank()) {
                            detectedListName
                        } else {
                            state.title
                        }
                    } else state.title
                    val proposedAlarms = firstProposal?.alarms.orEmpty().map { it.at }
                    val updatedAlarms = if (state.alarms.isEmpty() && proposedAlarms.isNotEmpty()) proposedAlarms else state.alarms
                    state.copy(
                        items = state.items + extracted,
                        title = newTitle,
                        alarms = updatedAlarms,
                        isGeneratingItems = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingItems = false) }
            }
        }
    }

    fun onAiAnalysisItemsReady(result: AiAnalysisResult) {
        val firstProposal = result.proposedReminders.firstOrNull()
        val extracted = result.proposedReminders.flatMap { p ->
            if (p.items.isNotEmpty()) {
                p.items
            } else {
                listOf(ChecklistItem(text = p.title, note = null))
            }
        }.ifEmpty {
            fallbackParseChecklist(result.rawTranscript)
        }

        _uiState.update { state ->
            val detectedListName = firstProposal?.listName
            val proposalTitle = firstProposal?.title
            val newTitle = if (state.title.isBlank()) {
                if (!proposalTitle.isNullOrBlank() && proposalTitle != "Nuevo recordatorio") {
                    proposalTitle
                } else if (!detectedListName.isNullOrBlank()) {
                    detectedListName
                } else {
                    state.title
                }
            } else state.title
            val proposedAlarms = firstProposal?.alarms.orEmpty().map { it.at }
            val updatedAlarms = if (state.alarms.isEmpty() && proposedAlarms.isNotEmpty()) proposedAlarms else state.alarms
            state.copy(
                items = state.items + extracted,
                title = newTitle,
                alarms = updatedAlarms,
                isVoiceCaptureOpen = false,
                isGeneratingItems = false,
            )
        }
    }

    private fun fallbackParseChecklist(text: String): List<ChecklistItem> {
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val clean = line.replace(Regex("""^[-*•\d.]+\s*"""), "").trim()
                val noteMatch = Regex("""\((.*?)\)""").find(clean)
                val note = noteMatch?.groupValues?.get(1)?.trim()
                val textOnly = clean.replace(Regex("""\(.*?\)"""), "").trim()
                ChecklistItem(text = if (textOnly.isNotBlank()) textOnly else clean, note = note)
            }
            .filter { it.text.isNotBlank() }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * ✓: guarda y cierra. Título y descripción vacíos se guardan como null. Las etiquetas y los
     * avisos se guardan como quedaron en sus modales.
     *
     * - Nota nueva: la crea, salvo que no tenga título, descripción, foto ni ítems.
     * - Existente: guarda los cambios, salvo que no haya cambios.
     *
     * El tipo se deduce del contenido: con ítems es [ReminderType.Checklist] y si no,
     * [ReminderType.Note]. Se sigue guardando para la card, la IA y los datos viejos.
     */
    fun onDone() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val title = state.cleanTitle
        val description = state.cleanDescription
        val reminder = state.reminder
        val validItems = state.validItems
        val type = typeFor(validItems)

        val toSave = when {
            !state.hasUnsavedChanges || !state.hasContent -> null
            state.isNew -> Reminder(
                type = type,
                title = title,
                description = description,
                items = validItems,
                tags = state.tags,
                alarms = state.alarmsToSave,
                places = state.places,
                importance = state.importance,
                action = state.action,
                photoPath = state.photoPath,
                photos = state.photos,
                createdAt = now(),
            )
            reminder != null ->
                // Lo que no se edita acá (audio, estado) queda como estaba: copy lo conserva.
                reminder.copy(
                    type = type,
                    title = title,
                    description = description,
                    items = validItems,
                    tags = state.tags,
                    alarms = state.alarmsToSave,
                    places = state.places,
                    importance = state.importance,
                    action = state.action,
                    photoPath = state.photoPath,
                    photos = state.photos,
                )
            else -> null
        }

        if (toSave == null) {
            _uiState.update { it.copy(isSaved = true) }
            return
        }
        if (state.isDraft) {
            // Borrador: no se guarda acá; quien abrió el editor recibe el resultado.
            _uiState.update { it.copy(isSaved = true, draftResult = toSave) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveReminder(toSave)
            _uiState.update { it.copy(isSaved = true, isSaving = false) }
        }
    }

    /**
     * Finaliza el recordatorio marcándolo como hecho (isDone = true).
     */
    fun onFinalizeReminder() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        // Un borrador todavía no existe: no se puede marcar como hecho, solo aplicar los cambios.
        if (state.isDraft) {
            onDone()
            return
        }
        val validItems = state.validItems
        val reminder = state.reminder
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (reminder == null) {
                val toSave = Reminder(
                    type = typeFor(validItems),
                    title = state.cleanTitle,
                    description = state.cleanDescription,
                    items = validItems,
                    tags = state.tags,
                    alarms = state.alarmsToSave,
                    places = state.places,
                    importance = state.importance,
                    action = state.action,
                    photoPath = state.photoPath,
                    photos = state.photos,
                    isDone = true,
                    statusChangedAt = now(),
                    createdAt = now(),
                )
                saveReminder(toSave)
            } else {
                val updated = reminder.copy(
                    type = typeFor(validItems),
                    title = state.cleanTitle,
                    description = state.cleanDescription,
                    items = validItems,
                    tags = state.tags,
                    alarms = state.alarmsToSave,
                    places = state.places,
                    importance = state.importance,
                    action = state.action,
                    photoPath = state.photoPath,
                    photos = state.photos,
                    isDone = true,
                )
                saveReminder(updated)
                markReminderDone?.invoke(reminder.id)
            }
            _uiState.update { it.copy(isSaved = true, isSaving = false) }
        }
    }

    /**
     * "Ahora" para la IA, con la zona horaria del teléfono (ej. 2026-11-07T15:30:00-03:00). Con
     * Instant.toString() iba en UTC ("…T18:30:00Z") y el modelo calculaba "a las 9" o "mañana"
     * con 3 horas de diferencia.
     */
    private fun referenceIso(): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now().atZone(zone))

    /** El tipo que se guarda: con ítems, lista; si no, nota (el usuario no lo elige). */
    private fun typeFor(items: List<ChecklistItem>): ReminderType =
        if (items.isNotEmpty()) ReminderType.Checklist else ReminderType.Note

    /**
     * El estado con los datos de [reminder] en los campos del editor. Lo usan cargar uno de la
     * base y abrir un borrador, para que los dos arranquen igual.
     */
    private fun ReminderDetailUiState.withLoaded(reminder: Reminder?): ReminderDetailUiState = copy(
        reminder = reminder,
        items = reminder?.items.orEmpty(),
        title = reminder?.title.orEmpty(),
        description = reminder?.description.orEmpty(),
        tagIds = reminder?.tags.orEmpty().map { tag -> tag.id }.toSet(),
        alarms = reminder?.alarms.orEmpty().map { alarm -> alarm.at }.sorted(),
        alarmRepeats = reminder?.alarms.orEmpty()
            .filter { alarm -> alarm.repeat != AlarmRepeat.None }
            .associate { alarm -> alarm.at to alarm.repeat },
        importance = reminder?.importance ?: Importance.Default,
        action = reminder?.action,
        photos = reminder?.photos.orEmpty(),
        photoPath = reminder?.photoPath,
    )

    // TODO: compartir el recordatorio (crear link). Por ahora es maquetado.
    fun onShareClick() = Unit

    companion object {
        /** Crea el ViewModel con el id de la ruta (si hay) y los casos de uso del [RememberApp.container]. */
        val Factory = factory(draft = null)

        /** El mismo editor sobre un recordatorio sin guardar (ver el parámetro `draft`). */
        fun draftFactory(draft: Reminder) = factory(draft)

        private fun factory(draft: Reminder?) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                ReminderDetailViewModel(
                    draft = draft,
                    savedStateHandle = createSavedStateHandle(),
                    getReminder = app.container.getReminderUseCase,
                    saveReminder = app.container.saveReminderUseCase,
                    tagRepository = app.container.tagRepository,
                    importPhotos = app.container.importPhotosUseCase,
                    saveCover = app.container.saveCoverUseCase,
                    observePlaces = app.container.observePlacesUseCase,
                    aiReminderService = app.container.aiReminderService,
                    markReminderDone = app.container.markReminderDoneUseCase,
                )
            }
        }
    }
}
