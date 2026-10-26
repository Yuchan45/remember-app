package com.example.uade.rememberapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Diálogo que se muestra cuando el usuario rechazó permanentemente permisos
 * ("No volver a preguntar"), ofreciendo un botón para abrir los Ajustes del sistema.
 */
@Composable
fun PermanentDenialDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = AppDialogDefaults.shape,
        containerColor = AppDialogDefaults.containerColor,
        title = {
            Text(
                text = stringResource(R.string.startup_permissions_permanent_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.startup_permissions_permanent_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            DialogFilledConfirmButton(
                text = stringResource(R.string.startup_permissions_btn_settings),
                onClick = onOpenSettings,
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.startup_permissions_btn_close),
                onClick = onDismiss,
            )
        },
    )
}

@Preview
@Composable
private fun PermanentDenialDialogPreview() {
    RememberAppTheme {
        PermanentDenialDialog(
            onOpenSettings = {},
            onDismiss = {},
        )
    }
}
