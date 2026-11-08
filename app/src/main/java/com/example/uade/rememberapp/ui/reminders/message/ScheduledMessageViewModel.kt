package com.example.uade.rememberapp.ui.reminders.message

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uade.rememberapp.RememberApp
import com.example.uade.rememberapp.domain.model.Alarm
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderPhoto
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ImportPhotosUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.SaveCoverUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.platform.contacts.ContactInfo
import com.example.uade.rememberapp.ui.navigation.Routes
import com.example.uade.rememberapp.ui.reminders.detail.AlarmDraft
import com.example.uade.rememberapp.ui.reminders.detail.AlarmsEditor
import com.example.uade.rememberapp.ui.reminders.detail.PhotosEditor
import com.example.uade.rememberapp.ui.reminders.detail.PlaceEditor
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.model.resolveReminderTime
import com.example.uade.rememberapp.ui.reminders.model.toDomain
import com.example.uade.rememberapp.ui.reminders.model.toUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * ViewModel que gestiona la creación y edición de recordatorios de tipo Mensaje Programado.
 */
class ScheduledMessageViewModel(
    savedStateHandle: SavedStateHandle,
    private val saveReminder: SaveReminderUseCase,
    private val getReminder: GetReminderUseCase,
    observePlaces: ObservePlacesUseCase,
    tagRepository: TagRepository,
    private val importPhotos: ImportPhotosUseCase,
    private val saveCover: SaveCoverUseCase,
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val reminderId: Long? = savedStateHandle[Routes.ARG_REMINDER_ID]

    private val defaultAlarmInstant: Instant =
        ZonedDateTime.ofInstant(now(), zone).plusHours(1).truncatedTo(ChronoUnit.MINUTES).toInstant()

    private val _uiState = MutableStateFlow(
        ScheduledMessageUiState(
            reminderId = reminderId,
            isNew = reminderId == null,
            isLoading = reminderId != null,
            alarms = if (reminderId == null) listOf(defaultAlarmInstant) else emptyList(),
        ),
    )
    val uiState: StateFlow<ScheduledMessageUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != null) {
            viewModelScope.launch {
                val reminder = getReminder(reminderId)
                if (reminder != null) {
                    val action = reminder.action
                    val channel = when (action) {
                        is NotificationAction.Message -> when (action.channel) {
                            MessageChannel.WhatsApp -> ScheduledMessageChannel.WhatsApp
                            MessageChannel.Sms -> ScheduledMessageChannel.Sms
                            MessageChannel.Instagram -> ScheduledMessageChannel.WhatsApp
                        }
                        is NotificationAction.Email -> ScheduledMessageChannel.Email
                        else -> ScheduledMessageChannel.WhatsApp
                    }

                    val recipient = when (action) {
                        is NotificationAction.Message -> action.to
                        is NotificationAction.Email -> action.to
                        is NotificationAction.Call -> action.phone
                        null -> ""
                    }

                    val subject = when (action) {
                        is NotificationAction.Email -> action.subject.orEmpty()
                        else -> ""
                    }

                    val message = when (action) {
                        is NotificationAction.Message -> action.text ?: reminder.description.orEmpty()
                        else -> reminder.description.orEmpty()
                    }

                    val alarms = reminder.alarms.map { it.at }
                    val repeats = reminder.alarms.associate { it.at to it.repeat }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalReminder = reminder,
                            title = reminder.title.orEmpty(),
                            channel = channel,
                            recipient = recipient,
                            subject = subject,
                            messageText = message,
                            photos = reminder.photos,
                            photoPath = reminder.photoPath,
                            alarms = alarms,
                            alarmRepeats = repeats,
                            places = reminder.places,
                            importance = reminder.importance,
                            selectedTagIds = reminder.tags.map { tag -> tag.id }.toSet(),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            observePlaces().collect { places ->
                _uiState.update { it.copy(availablePlaces = places) }
            }
        }

        viewModelScope.launch {
            tagRepository.observeAll().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onChannelChange(channel: ScheduledMessageChannel) {
        _uiState.update { it.copy(channel = channel) }
    }

    fun onRecipientChange(recipient: String) {
        _uiState.update { it.copy(recipient = recipient) }
    }

    fun onContactPicked(info: ContactInfo) {
        _uiState.update { state ->
            val target = when (state.channel) {
                ScheduledMessageChannel.Email -> info.email ?: state.recipient
                ScheduledMessageChannel.WhatsApp, ScheduledMessageChannel.Sms -> info.phoneNumber ?: state.recipient
            }
            val newTitle = if (state.title.isBlank() && !info.displayName.isNullOrBlank()) {
                when (state.channel) {
                    ScheduledMessageChannel.WhatsApp -> "WhatsApp a ${info.displayName}"
                    ScheduledMessageChannel.Sms -> "SMS a ${info.displayName}"
                    ScheduledMessageChannel.Email -> "Correo a ${info.displayName}"
                }
            } else {
                state.title
            }
            state.copy(recipient = target, title = newTitle)
        }
    }

    fun onSubjectChange(subject: String) {
        _uiState.update { it.copy(subject = subject) }
    }

    fun onMessageTextChange(messageText: String) {
        _uiState.update { it.copy(messageText = messageText) }
    }

    // --- Modal "Imágenes" (PhotosSheet) -------------------------------------------------------

    fun onImageClick() {
        _uiState.update { state ->
            val editor = PhotosEditor(
                photos = state.photos,
                coverPath = state.photoPath,
            )
            state.copy(photosEditor = editor)
        }
    }

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
                emptyList()
            }
            updatePhotosEditor { current ->
                val combined = (current.photos + imported.map { ReminderPhoto(it) }).take(MAX_PHOTOS)
                val (photosWithCover, cover) = withDefaultCover(combined, current.coverPath)
                current.copy(
                    photos = photosWithCover,
                    coverPath = cover,
                    isImporting = false,
                )
            }
            // Si el editor no estaba abierto explícitamente, aplicamos el cambio al borrador principal
            if (state.photosEditor == null) {
                _uiState.update { s ->
                    val cur = s.photosEditor ?: return@update s
                    s.copy(photos = cur.photos, photoPath = cur.coverPath, photosEditor = null)
                }
            }
        }
    }

    fun onPhotoClick(path: String) {
        updatePhotosEditor { it.copy(cropping = path) }
    }

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

    fun onCropDismiss() {
        updatePhotosEditor { it.copy(cropping = null) }
    }

    fun onRemovePhoto(path: String) {
        updatePhotosEditor { editor ->
            val remaining = editor.photos.filterNot { it.path == path }
            val newCover = if (editor.coverPath == path) remaining.firstOrNull()?.path else editor.coverPath
            editor.copy(
                photos = remaining.map { it.copy(isCover = it.path == newCover) },
                coverPath = newCover,
            )
        }
    }

    fun onSavePhotos() {
        _uiState.update { state ->
            val editor = state.photosEditor ?: return@update state
            val (photos, cover) = withDefaultCover(editor.photos, editor.coverPath)
            state.copy(photos = photos, photoPath = cover, photosEditor = null)
        }
    }

    fun onCancelPhotos() {
        _uiState.update { it.copy(photosEditor = null) }
    }

    private fun withDefaultCover(photos: List<ReminderPhoto>, cover: String?): Pair<List<ReminderPhoto>, String?> {
        if (cover != null || photos.isEmpty()) return photos to cover
        val first = photos.first()
        return photos.map { it.copy(isCover = it.path == first.path) } to first.path
    }

    private fun updatePhotosEditor(transform: (PhotosEditor) -> PhotosEditor) {
        _uiState.update { state -> state.copy(photosEditor = state.photosEditor?.let(transform)) }
    }

    // --- Modal "Recordatorio" (AlarmsSheet) ---------------------------------------------------

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
            state.copy(alarmsEditor = AlarmsEditor(items = items, expanded = if (items.size == 1) 0 else null))
        }
    }

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

    fun onAlarmDatePicked(date: LocalDate, time: LocalTime?) {
        val index = _uiState.value.alarmsEditor?.datePickerFor ?: return
        val calculatedInstant = resolveAt(TimeShortcut.PickDate, date, time)
        updateAlarm(index) {
            it.copy(
                selectedTime = TimeShortcut.PickDate,
                pickedDate = date,
                pickedTime = time,
                at = calculatedInstant,
            )
        }
        updateEditor { it.copy(datePickerFor = null) }
    }

    fun onAlarmDatePickerDismiss() {
        updateEditor { it.copy(datePickerFor = null) }
    }

    fun onAlarmRepeat(index: Int, option: RepeatOption) {
        updateAlarm(index) { it.copy(selectedRepeat = option) }
    }

    fun onToggleAlarmExpanded(index: Int) {
        updateEditor { it.copy(expanded = if (it.expanded == index) null else index) }
    }

    fun onAddAlarm() {
        updateEditor { editor ->
            editor.copy(
                items = editor.items + AlarmDraft(),
                expanded = editor.items.size,
            )
        }
    }

    fun onClearAlarms() {
        updateEditor { AlarmsEditor(items = listOf(AlarmDraft()), expanded = 0) }
    }

    fun onCancelAlarms() {
        // Al cerrar o cancelar, si hay avisos válidos en el editor, los preservamos
        val editor = _uiState.value.alarmsEditor
        val validAlarms = editor?.items?.mapNotNull { it.at }.orEmpty()
        if (validAlarms.isNotEmpty()) {
            onSaveAlarms()
        } else {
            _uiState.update { it.copy(alarmsEditor = null) }
        }
    }

    fun onSaveAlarms() {
        val editor = _uiState.value.alarmsEditor ?: return
        val validAlarms = editor.items.mapNotNull { it.at }
        val repeats = editor.items.mapNotNull { draft ->
            draft.at?.let { at -> at to draft.selectedRepeat.repeat }
        }.toMap()
        _uiState.update { it.copy(alarms = validAlarms, alarmRepeats = repeats, alarmsEditor = null) }
    }

    private fun updateEditor(transform: (AlarmsEditor) -> AlarmsEditor) {
        _uiState.update { state -> state.copy(alarmsEditor = state.alarmsEditor?.let(transform)) }
    }

    private fun updateAlarm(index: Int, transform: (AlarmDraft) -> AlarmDraft) {
        updateEditor { editor ->
            val updated = editor.items.mapIndexed { i, draft ->
                if (i == index) transform(draft) else draft
            }
            editor.copy(items = updated)
        }
    }

    private fun resolveAt(shortcut: TimeShortcut, date: LocalDate?, time: LocalTime?): Instant? {
        val currentZdt = ZonedDateTime.ofInstant(now(), zone)
        return resolveReminderTime(shortcut, date, time, currentZdt)?.toInstant()
    }

    // --- Modal "Ubicación" (PlaceSheet) -------------------------------------------------------

    fun onPlaceClick() {
        _uiState.update { state ->
            val currentAlert = state.places.firstOrNull()
            state.copy(
                placeEditor = PlaceEditor(
                    selectedPlaceId = currentAlert?.placeId,
                    event = currentAlert?.event?.toUi() ?: PlaceEvent.Arrive,
                ),
            )
        }
    }

    fun onSelectPlace(placeId: Long) {
        _uiState.update { state ->
            state.copy(
                placeEditor = state.placeEditor?.let {
                    it.copy(selectedPlaceId = if (it.selectedPlaceId == placeId) null else placeId)
                },
            )
        }
    }

    fun onPlaceEventSelect(event: PlaceEvent) {
        _uiState.update { state ->
            state.copy(placeEditor = state.placeEditor?.copy(event = event))
        }
    }

    fun onSavePlace() {
        val editor = _uiState.value.placeEditor ?: return
        val newPlaces = if (editor.selectedPlaceId != null) {
            listOf(PlaceAlert(placeId = editor.selectedPlaceId, event = editor.event.toDomain()))
        } else {
            emptyList()
        }
        _uiState.update { it.copy(places = newPlaces, placeEditor = null) }
    }

    fun onCancelPlace() {
        _uiState.update { it.copy(placeEditor = null) }
    }

    // --- Modal "Comportamiento" (ImportanceSheet) ---------------------------------------------

    fun onBehaviorClick() {
        _uiState.update { it.copy(importanceEditor = it.importance) }
    }

    fun onImportanceSelected(importance: Importance) {
        _uiState.update { it.copy(importanceEditor = importance) }
    }

    fun onSaveImportance() {
        _uiState.update { state ->
            state.copy(
                importance = state.importanceEditor ?: state.importance,
                importanceEditor = null,
            )
        }
    }

    fun onCancelImportance() {
        _uiState.update { it.copy(importanceEditor = null) }
    }

    // --- Modal "Etiquetas" (TagsSheet) -------------------------------------------------------

    fun onTagsClick() {
        _uiState.update { it.copy(isTagsSheetOpen = true, tagsDraft = it.selectedTagIds) }
    }

    fun onTagsDraftChange(assignedIds: Set<Long>) {
        _uiState.update { it.copy(tagsDraft = assignedIds) }
    }

    fun onSaveTags() {
        _uiState.update { it.copy(selectedTagIds = it.tagsDraft, isTagsSheetOpen = false) }
    }

    fun onCancelTags() {
        _uiState.update { it.copy(isTagsSheetOpen = false) }
    }

    // --- Guardar recordatorio -----------------------------------------------------------------

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            val finalTitle = state.title.trim().ifBlank {
                when (state.channel) {
                    ScheduledMessageChannel.WhatsApp -> "WhatsApp a ${state.recipient}"
                    ScheduledMessageChannel.Sms -> "SMS a ${state.recipient}"
                    ScheduledMessageChannel.Email -> state.subject.ifBlank { "Correo a ${state.recipient}" }
                }
            }

            val alarmsToSave = state.alarms.map { at ->
                Alarm(at = at, repeat = state.alarmRepeats[at] ?: AlarmRepeat.None)
            }

            val action = state.toNotificationAction()
            val tags = state.allTags.filter { it.id in state.selectedTagIds }

            val reminderToSave = Reminder(
                id = state.reminderId ?: 0L,
                type = ReminderType.ScheduledMessage,
                title = finalTitle,
                description = state.messageText.trim(),
                photoPath = state.photoPath,
                photos = state.photos,
                alarms = alarmsToSave,
                places = state.places,
                action = action,
                tags = tags,
                importance = state.importance,
                createdAt = state.originalReminder?.createdAt ?: now(),
            )

            saveReminder(reminderToSave)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as RememberApp
                val savedStateHandle = createSavedStateHandle()
                ScheduledMessageViewModel(
                    savedStateHandle = savedStateHandle,
                    saveReminder = app.container.saveReminderUseCase,
                    getReminder = app.container.getReminderUseCase,
                    observePlaces = app.container.observePlacesUseCase,
                    tagRepository = app.container.tagRepository,
                    importPhotos = app.container.importPhotosUseCase,
                    saveCover = app.container.saveCoverUseCase,
                )
            }
        }
    }
}
