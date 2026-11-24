package com.example.uade.rememberapp.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Permisos solicitados en contexto mediante ventana explicativa previa con botones
 * "Permitir" y "Rechazar".
 */
enum class ContextualPermission(
    @get:DrawableRes val iconRes: Int,
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int,
) {
    Notifications(
        iconRes = R.drawable.ic_notifications,
        titleRes = R.string.notifications_permission_dialog_title,
        descriptionRes = R.string.notifications_permission_dialog_desc,
    ),
    ForegroundLocation(
        iconRes = R.drawable.ic_location_on,
        titleRes = R.string.foreground_location_permission_dialog_title,
        descriptionRes = R.string.foreground_location_permission_dialog_desc,
    ),
    BackgroundLocation(
        iconRes = R.drawable.ic_location_on,
        titleRes = R.string.location_permission_dialog_title,
        descriptionRes = R.string.location_permission_dialog_desc,
    ),
    ExactAlarms(
        iconRes = R.drawable.ic_alarm,
        titleRes = R.string.exact_alarm_permission_dialog_title,
        descriptionRes = R.string.exact_alarm_permission_dialog_desc,
    ),
    Microphone(
        iconRes = R.drawable.ic_mic,
        titleRes = R.string.microphone_permission_dialog_title,
        descriptionRes = R.string.microphone_permission_dialog_desc,
    ),
    Contacts(
        iconRes = R.drawable.ic_person,
        titleRes = R.string.contacts_permission_dialog_title,
        descriptionRes = R.string.contacts_permission_dialog_desc,
    ),
}

/**
 * Ventana explicativa previa que informa para qué se necesita el permiso en contexto y ofrece
 * los botones "Permitir" (continúa hacia el permiso/configuración de Android) y "Rechazar".
 */
@Composable
fun ContextualPermissionDialog(
    permission: ContextualPermission,
    onAllow: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onReject,
        modifier = modifier,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        icon = {
            Icon(
                painter = painterResource(permission.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(permission.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(permission.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.permission_dialog_allow),
                onClick = onAllow,
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.permission_dialog_reject),
                onClick = onReject,
            )
        },
    )
}

@Preview
@Composable
private fun ContextualPermissionDialogPreview() {
    RememberAppTheme {
        ContextualPermissionDialog(
            permission = ContextualPermission.Notifications,
            onAllow = {},
            onReject = {},
        )
    }
}
