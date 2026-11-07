package com.example.uade.rememberapp.ui.reminders.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.platform.message.MessageLauncherHelper
import com.example.uade.rememberapp.ui.components.ClockTimePickerDialog
import com.example.uade.rememberapp.ui.components.OptionChip
import com.example.uade.rememberapp.ui.components.OptionChipStyle
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.places.components.iconRes
import com.example.uade.rememberapp.ui.reminders.components.ReminderDateDialog
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.theme.CreateScheduledMessageColor
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pantalla para crear o editar un mensaje programado (WhatsApp, SMS o Correo).
 */
@Composable
fun ScheduledMessageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduledMessageViewModel = viewModel(factory = ScheduledMessageViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    ScheduledMessageContent(
        uiState = uiState,
        onBack = onBack,
        onChannelChange = viewModel::onChannelChange,
        onRecipientChange = viewModel::onRecipientChange,
        onSubjectChange = viewModel::onSubjectChange,
        onMessageTextChange = viewModel::onMessageTextChange,
        onScheduleTypeChange = viewModel::onScheduleTypeChange,
        onShortcutClick = viewModel::onShortcutClick,
        onDateSelected = viewModel::onDateSelected,
        onDatePickerDismiss = viewModel::onDatePickerDismiss,
        onTimeSelected = viewModel::onTimeSelected,
        onTimePickerDismiss = viewModel::onTimePickerDismiss,
        onOpenDatePicker = viewModel::onOpenDatePicker,
        onOpenTimePicker = viewModel::onOpenTimePicker,
        onPlaceClick = viewModel::onPlaceClick,
        onPlaceEventClick = viewModel::onPlaceEventClick,
        onSendNow = {
            MessageLauncherHelper.launchMessage(
                context = context,
                action = uiState.toNotificationAction(),
                defaultText = uiState.messageText,
            )
        },
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduledMessageContent(
    uiState: ScheduledMessageUiState,
    onBack: () -> Unit,
    onChannelChange: (ScheduledMessageChannel) -> Unit,
    onRecipientChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageTextChange: (String) -> Unit,
    onScheduleTypeChange: (ScheduleTriggerType) -> Unit,
    onShortcutClick: (TimeShortcut) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDatePickerDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onTimePickerDismiss: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onPlaceEventClick: (PlaceEvent) -> Unit,
    onSendNow: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isNew) R.string.scheduled_message_title_new else R.string.scheduled_message_title_edit,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.reminder_detail_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = uiState.canSave,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(R.string.reminder_alarms_save),
                            tint = if (uiState.canSave) CreateScheduledMessageColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Sección: Canal de envío
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = stringResource(R.string.scheduled_message_section_channel))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ScheduledMessageChannel.entries.forEach { channel ->
                        val isSelected = uiState.channel == channel
                        val borderColor = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.outlineVariant
                        val containerColor = if (isSelected) channel.brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = containerColor,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onChannelChange(channel) },
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(channel.icon),
                                    contentDescription = null,
                                    tint = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = stringResource(channel.label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Sección: Destinatario
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = stringResource(R.string.scheduled_message_section_recipient))
                val isEmail = uiState.channel == ScheduledMessageChannel.Email
                OutlinedTextField(
                    value = uiState.recipient,
                    onValueChange = onRecipientChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (isEmail) R.string.scheduled_message_field_email else R.string.scheduled_message_field_phone,
                            ),
                        )
                    },
                    placeholder = {
                        Text(
                            stringResource(
                                if (isEmail) R.string.scheduled_message_field_email_placeholder else R.string.scheduled_message_field_phone_placeholder,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(if (isEmail) R.drawable.ic_mail else R.drawable.ic_call),
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isEmail) KeyboardType.Email else KeyboardType.Phone,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = uiState.channel.brandColor,
                        focusedLabelColor = uiState.channel.brandColor,
                    ),
                )
            }

            // Sección: Contenido del mensaje
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = stringResource(R.string.scheduled_message_section_message))

                if (uiState.channel == ScheduledMessageChannel.Email) {
                    OutlinedTextField(
                        value = uiState.subject,
                        onValueChange = onSubjectChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.scheduled_message_field_subject)) },
                        placeholder = { Text(stringResource(R.string.scheduled_message_field_subject_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = uiState.channel.brandColor,
                            focusedLabelColor = uiState.channel.brandColor,
                        ),
                    )
                }

                OutlinedTextField(
                    value = uiState.messageText,
                    onValueChange = onMessageTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.scheduled_message_field_text)) },
                    placeholder = { Text(stringResource(R.string.scheduled_message_field_text_placeholder)) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = uiState.channel.brandColor,
                        focusedLabelColor = uiState.channel.brandColor,
                    ),
                )
            }

            // Sección: Programación (¿Cuándo enviar?)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(text = stringResource(R.string.scheduled_message_section_when))

                SegmentedSelector(
                    options = ScheduleTriggerType.entries,
                    selected = uiState.scheduleType,
                    onSelected = onScheduleTypeChange,
                    label = { stringResource(it.label) },
                    icon = { it.icon },
                )

                if (uiState.scheduleType == ScheduleTriggerType.Time) {
                    // Atajos de hora
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OptionChip(
                            text = stringResource(R.string.scheduled_message_time_in_15_min),
                            icon = R.drawable.ic_timer,
                            selected = false,
                            onClick = {
                                val in15 = LocalTime.now().plusMinutes(15)
                                onTimeSelected(in15)
                            },
                            style = OptionChipStyle.Outlined,
                        )
                        OptionChip(
                            text = stringResource(R.string.scheduled_message_time_in_1_hour),
                            icon = R.drawable.ic_timer,
                            selected = uiState.selectedTimeShortcut == TimeShortcut.InOneHour,
                            onClick = { onShortcutClick(TimeShortcut.InOneHour) },
                            style = OptionChipStyle.Outlined,
                        )
                        OptionChip(
                            text = stringResource(R.string.scheduled_message_time_tonight),
                            icon = R.drawable.ic_bedtime,
                            selected = uiState.selectedTimeShortcut == TimeShortcut.Tonight,
                            onClick = { onShortcutClick(TimeShortcut.Tonight) },
                            style = OptionChipStyle.Outlined,
                        )
                        OptionChip(
                            text = stringResource(R.string.scheduled_message_time_tomorrow),
                            icon = R.drawable.ic_light_mode,
                            selected = uiState.selectedTimeShortcut == TimeShortcut.TomorrowMorning,
                            onClick = { onShortcutClick(TimeShortcut.TomorrowMorning) },
                            style = OptionChipStyle.Outlined,
                        )
                    }

                    // Selector de fecha y hora explícita
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Fecha
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenDatePicker() },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_calendar_month),
                                    contentDescription = null,
                                    tint = CreateScheduledMessageColor,
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.scheduled_message_date_picker_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = uiState.scheduledDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        // Hora
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenTimePicker() },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_alarm),
                                    contentDescription = null,
                                    tint = CreateScheduledMessageColor,
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.scheduled_message_time_picker_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = uiState.scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Resumen informativo
                    Text(
                        text = stringResource(
                            R.string.scheduled_message_summary_time,
                            uiState.scheduledDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM")),
                            uiState.scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Selector de evento por lugar (llegar / salir)
                    SegmentedSelector(
                        options = PlaceEvent.entries,
                        selected = uiState.placeEvent,
                        onSelected = onPlaceEventClick,
                        label = { stringResource(it.label) },
                        icon = { it.icon },
                    )

                    // Lista de lugares
                    if (uiState.availablePlaces.isEmpty()) {
                        Text(
                            text = stringResource(R.string.scheduled_message_location_no_places),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.availablePlaces.forEach { place ->
                                OptionChip(
                                    text = place.name,
                                    icon = place.kind.iconRes(),
                                    selected = place.id == uiState.selectedPlaceId,
                                    onClick = { onPlaceClick(place.id) },
                                    style = OptionChipStyle.Outlined,
                                )
                            }
                        }
                    }

                    uiState.selectedPlaceName?.let { placeName ->
                        val summaryRes = if (uiState.placeEvent == PlaceEvent.Arrive) {
                            R.string.scheduled_message_summary_location_arrive
                        } else {
                            R.string.scheduled_message_summary_location_leave
                        }
                        Text(
                            text = stringResource(summaryRes, placeName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Botones inferiores de acción
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.recipient.isNotBlank() && uiState.messageText.isNotBlank()) {
                    OutlinedButton(
                        onClick = onSendNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, uiState.channel.brandColor),
                    ) {
                        Icon(
                            painter = painterResource(uiState.channel.icon),
                            contentDescription = null,
                            tint = uiState.channel.brandColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.scheduled_message_btn_send_now),
                            color = uiState.channel.brandColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Button(
                    onClick = onSave,
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CreateScheduledMessageColor,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            if (uiState.isNew) R.string.scheduled_message_btn_schedule else R.string.scheduled_message_btn_save_changes,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    // Diálogo de calendario
    if (uiState.isDatePickerOpen) {
        ReminderDateDialog(
            initialDate = uiState.scheduledDate,
            initialTime = uiState.scheduledTime,
            onConfirm = { date, time ->
                onDateSelected(date)
                if (time != null) onTimeSelected(time)
            },
            onDismiss = onDatePickerDismiss,
        )
    }

    // Diálogo de reloj
    if (uiState.isTimePickerOpen) {
        ClockTimePickerDialog(
            initialTime = uiState.scheduledTime,
            onConfirm = onTimeSelected,
            onDismiss = onTimePickerDismiss,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B)
@Composable
private fun ScheduledMessageContentPreview() {
    RememberAppTheme {
        ScheduledMessageContent(
            uiState = ScheduledMessageUiState(
                recipient = "+54 9 11 5555-5555",
                messageText = "Hola! Te aviso cuando llegue a la reunión.",
                availablePlaces = listOf(
                    Place(id = 1, name = "Casa", latitude = -34.6037, longitude = -58.3816),
                    Place(id = 2, name = "Trabajo", latitude = -34.6037, longitude = -58.3816),
                ),
                selectedPlaceId = 1,
            ),
            onBack = {},
            onChannelChange = {},
            onRecipientChange = {},
            onSubjectChange = {},
            onMessageTextChange = {},
            onScheduleTypeChange = {},
            onShortcutClick = {},
            onDateSelected = {},
            onDatePickerDismiss = {},
            onTimeSelected = {},
            onTimePickerDismiss = {},
            onOpenDatePicker = {},
            onOpenTimePicker = {},
            onPlaceClick = {},
            onPlaceEventClick = {},
            onSendNow = {},
            onSave = {},
        )
    }
}
