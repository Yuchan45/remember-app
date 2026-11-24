package com.example.uade.rememberapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Diálogo contextual que explica por qué se solicita el permiso de alarmas exactas
 * ("Alarmas y recordatorios") antes de derivar al usuario a la configuración de Android (RNF07 / RNF04).
 */
@Composable
fun ExactAlarmPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContextualPermissionDialog(
        permission = ContextualPermission.ExactAlarms,
        onAllow = onConfirm,
        onReject = onDismiss,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ExactAlarmPermissionDialogPreview() {
    RememberAppTheme {
        ExactAlarmPermissionDialog(onConfirm = {}, onDismiss = {})
    }
}
