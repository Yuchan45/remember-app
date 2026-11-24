package com.example.uade.rememberapp.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Muestra "[message] · [actionLabel] ✕" unos segundos cada vez que [targetId] pasa a tener un
 * valor (ej. "1 movido a la papelera · Deshacer"). Tocar la acción avisa por [onAction]; al
 * terminar el aviso (por la acción, la ✕ o porque se venció) avisa por [onDone] para que el
 * ViewModel lo olvide.
 *
 * Si llega otro id mientras se ve el aviso, el LaunchedEffect se reinicia: el aviso anterior se
 * cierra y el nuevo ofrece la acción sobre el último.
 *
 * Es un efecto, no dibuja nada: el snackbar lo dibuja el SnackbarHost del Scaffold.
 */
@Composable
fun UndoSnackbarEffect(
    targetId: Long?,
    message: String,
    actionLabel: String,
    snackbarHostState: SnackbarHostState,
    onAction: (Long) -> Unit,
    onDone: (Long) -> Unit,
) {
    // rememberUpdatedState: si el efecto ya está corriendo, usa las lambdas más nuevas sin
    // reiniciarse (y sin volver a mostrar el aviso).
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(targetId) {
        val id = targetId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = true,
            // Corto (4s): es informativo, para deshacer rápido un error.
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnAction(id)
        currentOnDone(id)
    }
}
