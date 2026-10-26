package com.example.uade.rememberapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.uade.rememberapp.R

/**
 * Diálogo explicativo que informa al usuario por qué se requiere el permiso de ubicación
 * "Permitir todo el tiempo" para que las geocercas funcionen al llegar o salir con la app cerrada.
 */
@Composable
fun LocationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.location_permission_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.location_permission_dialog_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.location_permission_dialog_action_settings),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.location_permission_dialog_action_dismiss),
                onClick = onDismiss,
            )
        },
    )
}
