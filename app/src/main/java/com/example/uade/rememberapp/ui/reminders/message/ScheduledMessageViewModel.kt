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
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.domain.model.ReminderType
import com.example.uade.rememberapp.domain.repository.TagRepository
import com.example.uade.rememberapp.domain.usecase.GetReminderUseCase
import com.example.uade.rememberapp.domain.usecase.ObservePlacesUseCase
import com.example.uade.rememberapp.domain.usecase.SaveReminderUseCase
import com.example.uade.rememberapp.ui.navigation.Routes
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.reminders.model.resolveReminderTime
import com.example.uade.rememberapp.ui.reminders.model.toDomain
import com.example.uade.rememberapp.ui.reminders.model.toUi
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
    private val now: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val reminderId: Long? = savedStateHandle[Routes.ARG_REMINDER_ID]

    private val _uiState = MutableStateFlow(
        ScheduledMessageUiState(
            reminderId = reminderId,
            isNew = reminderId == null,
            isLoading = reminderId != null,
            scheduledDate = LocalDate.now(zone),
            scheduledTime = LocalTime.now(zone).plusHours(1).truncatedTo(ChronoUnit.MINUTES),
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
                        else -> reminder.title.orEmpty()
                    }

                    val message = when (action) {
                        is NotificationAction.Message -> action.text ?: reminder.description.orEmpty()
                        else -> reminder.description.orEmpty()
                    }

                    val hasPlaces = reminder.places.isNotEmpty()
                    val scheduleType = if (hasPlaces) ScheduleTriggerType.Location else ScheduleTriggerType.Time

                    val firstAlarm = reminder.alarms.firstOrNull()?.at
                    val alarmZdt = firstAlarm?.atZone(zone)
                    val scheduledDate = alarmZdt?.toLocalDate() ?: LocalDate.now(zone)
                    val scheduledTime = alarmZdt?.toLocalTime() ?: LocalTime.now(zone).plusHours(1).truncatedTo(ChronoUnit.MINUTES)

                    val firstPlace = reminder.places.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalReminder = reminder,
                            channel = channel,
                            recipient = recipient,
                            subject = subject,
                            messageText = message,
                            scheduleType = scheduleType,
                            scheduledDate = scheduledDate,
                            scheduledTime = scheduledTime,
                            selectedPlaceId = firstPlace?.placeId,
                            placeEvent = firstPlace?.event?.toUi() ?: PlaceEvent.Arrive,
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

    fun onChannelChange(channel: ScheduledMessageChannel) {
        _uiState.update { it.copy(channel = channel) }
    }

    fun onRecipientChange(recipient: String) {
        _uiState.update { it.copy(recipient = recipient) }
    }

    fun onSubjectChange(subject: String) {
        _uiState.update { it.copy(subject = subject) }
    }

    fun onMessageTextChange(messageText: String) {
        _uiState.update { it.copy(messageText = messageText) }
    }

    fun onScheduleTypeChange(type: ScheduleTriggerType) {
        _uiState.update { it.copy(scheduleType = type) }
    }

    fun onShortcutClick(shortcut: TimeShortcut) {
        if (shortcut == TimeShortcut.PickDate) {
            _uiState.update { it.copy(isDatePickerOpen = true) }
            return
        }
        val currentZdt = ZonedDateTime.ofInstant(now(), zone)
        val resolved = resolveReminderTime(shortcut, null, null, currentZdt)
        if (resolved != null) {
            _uiState.update {
                it.copy(
                    scheduledDate = resolved.toLocalDate(),
                    scheduledTime = resolved.toLocalTime(),
                    selectedTimeShortcut = shortcut,
                )
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                scheduledDate = date,
                isDatePickerOpen = false,
                selectedTimeShortcut = TimeShortcut.PickDate,
            )
        }
    }

    fun onDatePickerDismiss() {
        _uiState.update { it.copy(isDatePickerOpen = false) }
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update {
            it.copy(
                scheduledTime = time,
                isTimePickerOpen = false,
                selectedTimeShortcut = TimeShortcut.PickDate,
            )
        }
    }

    fun onTimePickerDismiss() {
        _uiState.update { it.copy(isTimePickerOpen = false) }
    }

    fun onOpenDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = true) }
    }

    fun onOpenTimePicker() {
        _uiState.update { it.copy(isTimePickerOpen = true) }
    }

    fun onPlaceClick(placeId: Long) {
        _uiState.update {
            it.copy(selectedPlaceId = if (it.selectedPlaceId == placeId) null else placeId)
        }
    }

    fun onPlaceEventClick(event: PlaceEvent) {
        _uiState.update { it.copy(placeEvent = event) }
    }

    fun onTagClick(tagId: Long) {
        _uiState.update { state ->
            val updated = if (tagId in state.selectedTagIds) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = updated)
        }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            val title = when (state.channel) {
                ScheduledMessageChannel.WhatsApp -> "WhatsApp a ${state.recipient}"
                ScheduledMessageChannel.Sms -> "SMS a ${state.recipient}"
                ScheduledMessageChannel.Email -> state.subject.ifBlank { "Correo a ${state.recipient}" }
            }

            val alarms = if (state.scheduleType == ScheduleTriggerType.Time) {
                val scheduledInstant = state.scheduledDate
                    .atTime(state.scheduledTime)
                    .atZone(zone)
                    .toInstant()
                listOf(Alarm(at = scheduledInstant))
            } else {
                emptyList()
            }

            val places = if (state.scheduleType == ScheduleTriggerType.Location && state.selectedPlaceId != null) {
                listOf(PlaceAlert(placeId = state.selectedPlaceId, event = state.placeEvent.toDomain()))
            } else {
                emptyList()
            }

            val action = state.toNotificationAction()
            val tags = state.allTags.filter { it.id in state.selectedTagIds }

            val reminderToSave = Reminder(
                id = state.reminderId ?: 0L,
                type = ReminderType.ScheduledMessage,
                title = title,
                description = state.messageText.trim(),
                alarms = alarms,
                places = places,
                action = action,
                tags = tags,
                importance = Importance.High,
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
                )
            }
        }
    }
}
