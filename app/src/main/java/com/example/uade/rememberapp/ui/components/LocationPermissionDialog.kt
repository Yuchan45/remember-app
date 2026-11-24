package com.example.uade.rememberapp.ui.components

import androidx.compose.runtime.Composable

/**
 * Diálogo explicativo que informa al usuario por qué se requiere el permiso de ubicación
 * "Permitir todo el tiempo" para que las geocercas funcionen al llegar o salir con la app cerrada.
 */
@Composable
fun LocationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ContextualPermissionDialog(
        permission = ContextualPermission.BackgroundLocation,
        onAllow = onConfirm,
        onReject = onDismiss,
    )
}
