package com.example.uade.rememberapp.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.Instant

/**
 * Lista de recordatorios, o [emptyText] si no hay ninguno.
 *
 * Recibe la lista ya filtrada: no sabe de pestañas ni de ViewModel, así sirve igual para
 * "Pendientes", "Hechos" o los pendientes de un lugar.
 */
@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onReminderCheckedChange: (id: Long, isDone: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = stringResource(R.string.reminders_empty),
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = reminders,
                key = { it.id },
            ) { reminder ->
                // TODO: gestos de swipe sobre cada fila (ej. envolviendo ReminderRow en SwipeToDismissBox):
                //  - swipe a la derecha → marcar como hecho: onReminderCheckedChange(reminder.id, true)
                //  - swipe a la izquierda → volver a pendiente: onReminderCheckedChange(reminder.id, false)
                ReminderRow(
                    reminder = reminder,
                    onCheckedChange = { checked -> onReminderCheckedChange(reminder.id, checked) },
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ReminderListPreview() {
    val now = Instant.now()
    RememberAppTheme {
        ReminderList(
            reminders = listOf(
                // Solo texto: corto, y largo para ver cómo corta en varias líneas.
                Reminder(id = 1, text = "Pagar la cuota de la facu", createdAt = now),
                Reminder(
                    id = 2,
                    text = "Llamar al dentista para cambiar el turno del jueves y preguntar si atienden por la obra social",
                    createdAt = now,
                ),
                // Texto + foto: corto, y largo para ver que la miniatura no se deforma.
                Reminder(id = 3, text = "Comprar el cargador", photoPath = "preview", createdAt = now),
                Reminder(
                    id = 4,
                    text = "Comprar el cargador que vi en la vidriera del local de la esquina, el de carga rápida",
                    photoPath = "preview",
                    createdAt = now,
                ),
                // Solo foto.
                Reminder(id = 5, text = "", photoPath = "preview", createdAt = now),
                // Hechos: con el checkbox tildado.
                Reminder(id = 6, text = "Devolver el libro a la biblioteca", isDone = true, createdAt = now),
                Reminder(id = 7, text = "Sacar turno en el banco", photoPath = "preview", isDone = true, createdAt = now),
            ),
            onReminderCheckedChange = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 200)
@Composable
private fun ReminderListEmptyPreview() {
    RememberAppTheme {
        ReminderList(
            reminders = emptyList(),
            onReminderCheckedChange = { _, _ -> },
        )
    }
}
