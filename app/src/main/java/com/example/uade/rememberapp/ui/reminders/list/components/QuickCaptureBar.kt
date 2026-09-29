package com.example.uade.rememberapp.ui.reminders.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.CircleIconButton
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Barra de captura rápida: "+  Recordame algo…  🎤 🖼".
 *
 * Por ahora es solo un botón con forma de campo: tocarla avisa por [onClick].
 * TODO: convertirla en un campo de texto real que cree una nota al confirmar.
 */
@Composable
fun QuickCaptureBar(
    onClick: () -> Unit,
    onNewReminder: () -> Unit,
    onVoice: () -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(
                icon = R.drawable.ic_add,
                contentDescription = stringResource(R.string.reminders_quick_capture_new),
                onClick = onNewReminder,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.reminders_quick_capture_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onVoice) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.reminders_quick_capture_voice),
                )
            }
            IconButton(onClick = onAddPhoto) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_photo_alternate),
                    contentDescription = stringResource(R.string.reminders_quick_capture_photo),
                )
            }
        }
    }
}

@Preview
@Composable
private fun QuickCaptureBarPreview() {
    RememberAppTheme {
        QuickCaptureBar(
            onClick = {},
            onNewReminder = {},
            onVoice = {},
            onAddPhoto = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
