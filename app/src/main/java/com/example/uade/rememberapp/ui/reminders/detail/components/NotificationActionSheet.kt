package com.example.uade.rememberapp.ui.reminders.detail.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.MessageChannel
import com.example.uade.rememberapp.ui.components.DialogDismissButton
import com.example.uade.rememberapp.ui.components.DialogFilledConfirmButton
import com.example.uade.rememberapp.ui.components.SectionLabel
import com.example.uade.rememberapp.ui.reminders.detail.ActionEditor
import com.example.uade.rememberapp.ui.reminders.detail.ActionEditorStep
import com.example.uade.rememberapp.ui.reminders.model.MessageChannelOption
import com.example.uade.rememberapp.ui.reminders.model.NotificationActionKind
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/** Todo lo que se puede hacer en el modal "Acción de notificación" (ver [NotificationActionSheet]). */
data class NotificationActionSheetActions(
    val onKindSelected: (NotificationActionKind) -> Unit = {},
    val onBack: () -> Unit = {},
    val onChannel: (MessageChannel) -> Unit = {},
    val onTarget: (String) -> Unit = {},
    val onText: (String) -> Unit = {},
    val onRemove: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSave: () -> Unit = {},
)

/**
 * Modal "Acción de notificación". Cerrarlo deslizando o tocando afuera es lo mismo que "Cancelar".
 *
 * [currentKind] es el tipo de la acción que ya tiene el recordatorio, para marcarlo en la lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionSheet(
    editor: ActionEditor,
    currentKind: NotificationActionKind?,
    actions: NotificationActionSheetActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        NotificationActionSheetContent(
            editor = editor,
            currentKind = currentKind,
            actions = actions,
            modifier = Modifier
                .navigationBarsPadding()
                // Con el teclado abierto (al escribir el destinatario), el formulario sube.
                .imePadding(),
        )
    }
}

/**
 * Dos pasos, con un fundido entre uno y otro:
 * ```
 * Acción de notificación                    ┌ ← Enviar mensaje ──────────────────────┐
 * Añadir una acción rápida…                 │ ENVIAR POR                               │
 * 📞 Llamar al número                        │ [💬 SMS] [🟢 WhatsApp] [📷 Instagram]    │
 * ➤ Enviar mensaje             ✓    ──►     │ [ Número de teléfono               ]     │
 * ✉ Enviar correo electrónico               │ [ Mensaje (opcional)               ]     │
 * 🗑 Quitar acción  (si había una)            │              Cancelar  (Guardar)        │
 * ```
 */
@Composable
fun NotificationActionSheetContent(
    editor: ActionEditor,
    currentKind: NotificationActionKind?,
    actions: NotificationActionSheetActions,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = editor.step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "actionSheetStep",
        modifier = modifier.fillMaxWidth(),
    ) { step ->
        when (step) {
            ActionEditorStep.List -> ActionList(
                currentKind = currentKind,
                showRemove = editor.hadAction,
                onKindSelected = actions.onKindSelected,
                onRemove = actions.onRemove,
            )
            ActionEditorStep.Form -> ActionForm(editor = editor, actions = actions)
        }
    }
}

/** Paso 1: la lista de acciones, como en el diseño (ícono + texto, sin recuadros). */
@Composable
private fun ActionList(
    currentKind: NotificationActionKind?,
    showRemove: Boolean,
    onKindSelected: (NotificationActionKind) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.reminder_action_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
            Text(
                text = stringResource(R.string.reminder_action_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.selectableGroup()) {
            NotificationActionKind.entries.forEach { kind ->
                ActionRow(
                    icon = kind.icon,
                    label = stringResource(kind.label),
                    selected = kind == currentKind,
                    onClick = { onKindSelected(kind) },
                )
            }
        }
        if (showRemove) {
            ActionRow(
                icon = R.drawable.ic_delete,
                label = stringResource(R.string.reminder_action_remove),
                selected = false,
                onClick = onRemove,
                color = colors.error,
            )
        }
    }
}

/** Una fila de la lista: ícono, texto y un ✓ si es la acción que ya tiene el recordatorio. */
@Composable
private fun ActionRow(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.Button)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Paso 2: los datos de la acción elegida. El primer campo pide el foco al entrar. */
@Composable
private fun ActionForm(
    editor: ActionEditor,
    actions: NotificationActionSheetActions,
) {
    val kind = editor.kind ?: return
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(kind) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.reminder_action_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(kind.label),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        when (kind) {
            NotificationActionKind.Call -> ActionField(
                value = editor.target,
                onValueChange = actions.onTarget,
                label = stringResource(R.string.reminder_action_field_phone),
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.focusRequester(focusRequester),
            )

            NotificationActionKind.Message -> {
                val channel = MessageChannelOption.of(editor.channel)
                SectionLabel(text = stringResource(R.string.reminder_action_send_via))
                Row(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MessageChannelOption.entries.forEach { option ->
                        ChannelCard(
                            option = option,
                            selected = option == channel,
                            onClick = { actions.onChannel(option.channel) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                ActionField(
                    value = editor.target,
                    onValueChange = actions.onTarget,
                    label = stringResource(channel.targetLabel),
                    keyboardType = if (channel.isPhone) KeyboardType.Phone else KeyboardType.Text,
                    // Instagram: el usuario se escribe sin "@", que se muestra fijo adelante.
                    prefix = if (channel.isPhone) null else "@",
                    modifier = Modifier.focusRequester(focusRequester),
                )
                ActionField(
                    value = editor.text,
                    onValueChange = actions.onText,
                    label = stringResource(R.string.reminder_action_field_message),
                    keyboardType = KeyboardType.Text,
                    singleLine = false,
                )
            }

            NotificationActionKind.Email -> {
                ActionField(
                    value = editor.target,
                    onValueChange = actions.onTarget,
                    label = stringResource(R.string.reminder_action_field_email),
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                ActionField(
                    value = editor.text,
                    onValueChange = actions.onText,
                    label = stringResource(R.string.reminder_action_field_subject),
                    keyboardType = KeyboardType.Text,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DialogDismissButton(text = stringResource(R.string.common_cancel), onClick = actions.onCancel)
            DialogFilledConfirmButton(
                text = stringResource(R.string.reminder_alarms_save),
                onClick = actions.onSave,
                // Sin destinatario no hay acción posible.
                enabled = editor.canSave,
            )
        }
    }
}

/**
 * Una tarjeta de canal: logo arriba y nombre abajo. La elegida lleva borde y un fondo suave del
 * color de la marca (o del acento de la app, para SMS).
 */
@Composable
private fun ChannelCard(
    option: MessageChannelOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = option.brandColor ?: colors.primary
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = stringResource(option.label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            )
        }
    }
}

/** Campo de texto con etiqueta flotante, del estilo de Material3 y los colores del tema. */
@Composable
private fun ActionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) {
                KeyboardCapitalization.Sentences
            } else {
                KeyboardCapitalization.None
            },
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
    )
}

@Preview(name = "Lista, sin acción", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ActionListPreview() {
    RememberAppTheme {
        NotificationActionSheetContent(ActionEditor(), currentKind = null, actions = NotificationActionSheetActions())
    }
}

@Preview(name = "Lista, con acción", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ActionListWithActionPreview() {
    RememberAppTheme {
        NotificationActionSheetContent(
            ActionEditor(hadAction = true),
            currentKind = NotificationActionKind.Message,
            actions = NotificationActionSheetActions(),
        )
    }
}

@Preview(name = "Mensaje por WhatsApp", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ActionFormMessagePreview() {
    RememberAppTheme {
        NotificationActionSheetContent(
            ActionEditor(
                step = ActionEditorStep.Form,
                kind = NotificationActionKind.Message,
                channel = MessageChannel.WhatsApp,
                target = "11 5555-5555",
            ),
            currentKind = null,
            actions = NotificationActionSheetActions(),
        )
    }
}

@Preview(name = "Correo", showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun ActionFormEmailPreview() {
    RememberAppTheme {
        NotificationActionSheetContent(
            ActionEditor(step = ActionEditorStep.Form, kind = NotificationActionKind.Email),
            currentKind = null,
            actions = NotificationActionSheetActions(),
        )
    }
}
