package com.example.uade.rememberapp.ui.reminders.message

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AlarmRepeat
import com.example.uade.rememberapp.domain.model.Importance
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.domain.model.NotificationAction
import com.example.uade.rememberapp.domain.model.PlaceAlert
import com.example.uade.rememberapp.domain.model.PlaceAlertEvent
import com.example.uade.rememberapp.domain.model.Tag
import com.example.uade.rememberapp.platform.contacts.ContactInfo
import com.example.uade.rememberapp.platform.contacts.ContactPickerHelper
import com.example.uade.rememberapp.platform.message.MessageLauncherHelper
import com.example.uade.rememberapp.ui.components.ImageCropDialog
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.components.ImportanceSheet
import com.example.uade.rememberapp.ui.reminders.components.PlaceSheet
import com.example.uade.rememberapp.ui.reminders.components.ReminderDateDialog
import com.example.uade.rememberapp.ui.reminders.components.TagsSheet
import com.example.uade.rememberapp.ui.reminders.components.formatReminderTime
import com.example.uade.rememberapp.ui.reminders.components.isSameDay
import com.example.uade.rememberapp.ui.reminders.detail.components.AlarmsSheet
import com.example.uade.rememberapp.ui.reminders.detail.components.AlarmsSheetActions
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotoStack
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotosSheet
import com.example.uade.rememberapp.ui.reminders.detail.components.PhotosSheetActions
import com.example.uade.rememberapp.ui.reminders.model.PlaceEvent
import com.example.uade.rememberapp.ui.reminders.model.ReminderImportance
import com.example.uade.rememberapp.ui.reminders.model.RepeatOption
import com.example.uade.rememberapp.ui.reminders.model.TimeShortcut
import com.example.uade.rememberapp.ui.theme.CreateScheduledMessageColor
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CoverAspectRatio = 16f / 9f

/**
 * Pantalla para crear o editar un mensaje programado (WhatsApp, SMS o Correo),
 * con selector de contactos, soporte de imágenes y menú de opciones completo.
 */
@Composable
fun ScheduledMessageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduledMessageViewModel = viewModel(factory = ScheduledMessageViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val now = remember { Instant.now() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    // Solicitud de permisos de notificación (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.scheduled_message_notifications_permission_needed),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Selector de contactos
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { contactUri ->
        contactUri?.let { uri ->
            val contactInfo = ContactPickerHelper.resolveContact(context, uri)
            viewModel.onContactPicked(contactInfo)
            if (uiState.channel == ScheduledMessageChannel.Email && contactInfo.email.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.scheduled_message_no_email_found),
                    Toast.LENGTH_SHORT,
                ).show()
            } else if (uiState.channel != ScheduledMessageChannel.Email && contactInfo.phoneNumber.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.scheduled_message_no_phone_found),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.scheduled_message_contacts_permission_denied),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun onPickContactClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            contactPickerLauncher.launch(null)
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Selector de fotos
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PHOTOS),
    ) { uris -> viewModel.onPhotosPicked(uris.map { it.toString() }) }

    val pickPhotos = {
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val onImageClick = {
        if (uiState.photos.isEmpty()) pickPhotos() else viewModel.onImageClick()
    }

    fun onSaveWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Toast.makeText(
                context,
                context.getString(R.string.scheduled_message_notifications_permission_needed),
                Toast.LENGTH_LONG,
            ).show()
        }
        viewModel.onSave()
    }

    ScheduledMessageContent(
        uiState = uiState,
        now = now,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onChannelChange = viewModel::onChannelChange,
        onRecipientChange = viewModel::onRecipientChange,
        onPickContact = ::onPickContactClick,
        onSubjectChange = viewModel::onSubjectChange,
        onMessageTextChange = viewModel::onMessageTextChange,
        onTimeClick = viewModel::onTimeClick,
        onPlaceClick = viewModel::onPlaceClick,
        onBehaviorClick = viewModel::onBehaviorClick,
        onTagsClick = viewModel::onTagsClick,
        onImageClick = onImageClick,
        onSendNow = {
            MessageLauncherHelper.launchMessage(
                context = context,
                action = uiState.toNotificationAction(),
                defaultText = uiState.messageText,
            )
        },
        onSave = ::onSaveWithPermissionCheck,
        modifier = modifier,
    )

    // Modales asociados a las opciones
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
            places = uiState.availablePlaces,
            editor = editor,
            onPlaceClick = viewModel::onSelectPlace,
            onEventClick = viewModel::onPlaceEventSelect,
            onSave = viewModel::onSavePlace,
            onClose = viewModel::onCancelPlace,
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

    if (uiState.isTagsSheetOpen) {
        TagsSheet(
            assignedIds = uiState.tagsDraft,
            onAssignedChange = viewModel::onTagsDraftChange,
            onCancel = viewModel::onCancelTags,
            onSave = viewModel::onSaveTags,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduledMessageContent(
    uiState: ScheduledMessageUiState,
    now: Instant,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onChannelChange: (ScheduledMessageChannel) -> Unit,
    onRecipientChange: (String) -> Unit,
    onPickContact: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageTextChange: (String) -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onTagsClick: () -> Unit,
    onImageClick: () -> Unit,
    onSendNow: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var isOptionsCollapsed by rememberSaveable { mutableStateOf(false) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = if (isOptionsCollapsed) 56.dp else 260.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Título del recordatorio
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(text = stringResource(R.string.scheduled_message_field_title))
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.scheduled_message_field_title_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = uiState.channel.brandColor,
                            focusedLabelColor = uiState.channel.brandColor,
                        ),
                    )
                }

                // Banner de fotos (si tiene alguna foto o portada)
                val stackPhotos = uiState.stackPhotos
                if (stackPhotos.isNotEmpty()) {
                    PhotoStack(
                        photoPaths = stackPhotos,
                        aspectRatio = CoverAspectRatio,
                        onClick = onImageClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Canal de envío
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
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

                // Destinatario con botón para selector de contactos
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        trailingIcon = {
                            IconButton(onClick = onPickContact) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_person),
                                    contentDescription = stringResource(R.string.scheduled_message_pick_contact),
                                    tint = uiState.channel.brandColor,
                                )
                            }
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

                // Contenido del mensaje
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = uiState.channel.brandColor,
                            focusedLabelColor = uiState.channel.brandColor,
                        ),
                    )
                }

                // Botón Acción rápida: Enviar ahora
                OutlinedButton(
                    onClick = onSendNow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, uiState.channel.brandColor),
                ) {
                    Icon(
                        painter = painterResource(uiState.channel.icon),
                        contentDescription = null,
                        tint = uiState.channel.brandColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.scheduled_message_btn_send_now),
                        color = uiState.channel.brandColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Panel de opciones inferior idéntico al menú de la foto
            if (!WindowInsets.isImeVisible) {
                ScheduledMessageOptionsPanel(
                    uiState = uiState,
                    now = now,
                    isCollapsed = isOptionsCollapsed,
                    onToggleCollapsed = { isOptionsCollapsed = !isOptionsCollapsed },
                    onTagsClick = onTagsClick,
                    onTimeClick = onTimeClick,
                    onPlaceClick = onPlaceClick,
                    onBehaviorClick = onBehaviorClick,
                    onImageClick = onImageClick,
                    onActionClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Panel inferior plegable con el menú de opciones (como el de la foto).
 */
@Composable
private fun ScheduledMessageOptionsPanel(
    uiState: ScheduledMessageUiState,
    now: Instant,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onTagsClick: () -> Unit,
    onTimeClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onBehaviorClick: () -> Unit,
    onImageClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        val currentIsCollapsed by rememberUpdatedState(isCollapsed)
        val currentOnToggle by rememberUpdatedState(onToggleCollapsed)
        val thresholdPx = with(LocalDensity.current) { 24.dp.toPx() }
        var dragTotal by remember { mutableFloatStateOf(0f) }
        val dragState = rememberDraggableState { delta -> dragTotal += delta }

        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = if (isCollapsed) 0.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
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
                            value = actionValue(uiState.toNotificationAction()),
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
                            value = stringResource(R.string.reminder_detail_attachments_none),
                        )
                    }
                    OptionRow {
                        OptionCard(
                            icon = R.drawable.ic_share,
                            title = stringResource(R.string.reminder_detail_share_option),
                            value = stringResource(R.string.reminder_detail_share_value),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

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

@Composable
private fun RowScope.OptionCard(
    @DrawableRes icon: Int,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
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

@Composable
private fun imageValue(photoCount: Int, hasCover: Boolean): String = when {
    photoCount == 0 && !hasCover -> stringResource(R.string.reminder_detail_image_none)
    photoCount == 0 -> stringResource(R.string.reminder_detail_image_cover)
    hasCover -> pluralStringResource(R.plurals.reminder_detail_image_cover_count, photoCount, photoCount)
    else -> pluralStringResource(R.plurals.reminder_detail_image_count, photoCount, photoCount)
}

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
    return if (extra > 0) {
        stringResource(R.string.reminder_detail_place_more, first, extra)
    } else {
        first
    }
}
