package com.example.uade.rememberapp.ui.reminders.capture.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Barra inferior del modal: 🎤 🖼 ☑ ⤢ a la izquierda y "Guardar" a la derecha.
 *
 * El último ícono alterna la pantalla completa: ⤢ para abrirla y ⤡ para volver al modal chico.
 * "Guardar" se ve deshabilitado si [canSave] es false (ej. sin título).
 */
@Composable
fun CaptureToolbar(
    isFullScreen: Boolean,
    onVoice: () -> Unit,
    onPhoto: () -> Unit,
    onChecklist: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    canSave: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(R.drawable.ic_mic, stringResource(R.string.reminders_quick_capture_voice), onVoice)
        ToolButton(R.drawable.ic_add_photo_alternate, stringResource(R.string.reminders_quick_capture_photo), onPhoto)
        ToolButton(R.drawable.ic_checklist, stringResource(R.string.reminders_capture_tool_checklist), onChecklist)
        if (isFullScreen) {
            ToolButton(R.drawable.ic_close_fullscreen, stringResource(R.string.reminders_capture_tool_collapse), onToggleFullScreen)
        } else {
            ToolButton(R.drawable.ic_open_in_full, stringResource(R.string.reminders_capture_tool_expand), onToggleFullScreen)
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onSave, enabled = canSave) {
            Text(
                text = stringResource(R.string.reminders_capture_save),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ToolButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun CaptureToolbarPreview() {
    RememberAppTheme {
        CaptureToolbar(
            isFullScreen = false,
            onVoice = {},
            onPhoto = {},
            onChecklist = {},
            onToggleFullScreen = {},
            onSave = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}
