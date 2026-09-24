package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.ui.theme.PhotoBackground
import com.example.uade.rememberapp.ui.theme.PhotoIcon
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Instant

/**
 * Una fila de la lista. Se adapta a lo que tenga el recordatorio:
 * - solo texto:     [✓] Texto
 * - texto + imagen: [✓] Texto          [img]
 * - solo imagen:    [✓] [img]
 *
 * No son tres componentes distintos: cada parte se dibuja solo si existe, así el Card y el
 * checkbox (y más adelante el chip de fecha/lugar) se escriben una sola vez.
 *
 * No guarda estado: el tilde sale de [Reminder.isDone] y el toque se avisa por [onCheckedChange].
 *
 * TODO: chip de fecha / lugar según el Trigger.
 */
@Composable
fun ReminderRow(
    reminder: Reminder,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasText = reminder.text.isNotBlank()
    val hasPhoto = reminder.photoPath != null

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = reminder.isDone,
                onCheckedChange = onCheckedChange,
            )

            // Columna principal: el texto, o la foto si no hay texto.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, end = 8.dp),
            ) {
                if (hasText) {
                    Text(
                        text = reminder.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else if (hasPhoto) {
                    // Solo foto: ocupa todo el ancho, con un alto fijo que muestra un recorte.
                    // TODO: swipe hacia abajo para expandir la card y ver la foto completa.
                    ReminderPhoto(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
            }

            // Con texto, la foto va a la derecha como miniatura.
            if (hasText && hasPhoto) {
                ReminderPhoto(modifier = Modifier.size(80.dp))
            }
        }
    }
}

/**
 * Recuadro de la foto. El tamaño lo decide quien lo usa (miniatura o ancho completo).
 *
 * TODO: cargar la foto real desde photoPath (con Coil); por ahora es un placeholder.
 */
@Composable
private fun ReminderPhoto(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PhotoBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_image),
            contentDescription = stringResource(R.string.reminder_photo_description),
            tint = PhotoIcon,
        )
    }
}

@Preview(showBackground = true, name = "Solo texto")
@Composable
private fun ReminderRowTextPreview() {
    RememberAppTheme {
        ReminderRow(
            reminder = Reminder(id = 1, text = "Pagar la cuota de la facu", createdAt = Instant.now()),
            onCheckedChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Texto + imagen")
@Composable
private fun ReminderRowTextAndPhotoPreview() {
    RememberAppTheme {
        ReminderRow(
            reminder = Reminder(
                id = 2,
                text = "Comprar el cargador que vi en la vidriera",
                photoPath = "preview",
                createdAt = Instant.now(),
            ),
            onCheckedChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Solo imagen")
@Composable
private fun ReminderRowPhotoPreview() {
    RememberAppTheme {
        ReminderRow(
            reminder = Reminder(id = 3, text = "", photoPath = "preview", createdAt = Instant.now()),
            onCheckedChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
