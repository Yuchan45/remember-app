package com.example.uade.rememberapp.ui.reminders.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.CreateIconContent
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import kotlinx.coroutines.launch

/**
 * Modal "Crear" que abre el botón "+": una grilla de dos columnas con los tipos de
 * recordatorio que se pueden crear ([CreateReminderOption]).
 *
 * Tocar una opción primero baja el modal (con su animación) y recién al terminar avisa por
 * [onOptionClick]. Si se avisara enseguida, la pantalla que se abre (ej. la nota nueva)
 * aparecería por debajo del modal, que se dibuja en su propia ventana encima de todo y tarda en
 * irse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderSheet(
    onOptionClick: (CreateReminderOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnOptionClick by rememberUpdatedState(onOptionClick)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = { WindowInsets(0) },
        // La manija va dentro del contenido: la de Material3 cierra el modal al tocarla o
        // arrastrarla hacia arriba (mismo motivo que en QuickCaptureSheet).
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BottomSheetDefaults.DragHandle()
        }
        CreateReminderContent(
            options = CreateReminderOption.entries,
            onOptionClick = { option ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    // Solo si terminó de bajar (no si se canceló, ej. porque se volvió a arrastrar).
                    if (!sheetState.isVisible) currentOnOptionClick(option)
                }
            },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * ```
 * Crear
 * [ 📄 Nota        ] [ ☑ Checklist        ]
 * [ 🎤 Audio       ] [ ✨ Texto largo      ]
 * [ ➤ Mensaje prog.]
 * ```
 * Las cards de cada fila miden lo mismo; si la última fila queda con una sola, ocupa media.
 */
@Composable
fun CreateReminderContent(
    options: List<CreateReminderOption>,
    onOptionClick: (CreateReminderOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.reminders_create_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        options.chunked(2).forEach { row ->
            // height(IntrinsicSize.Min) + fillMaxHeight: las dos cards de la fila quedan del
            // mismo alto aunque una descripción ocupe dos líneas.
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { option ->
                    CreateOptionCard(
                        option = option,
                        onClick = { onOptionClick(option) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Card de una opción: ícono en un recuadro de color, nombre y descripción. */
@Composable
private fun CreateOptionCard(
    option: CreateReminderOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = option.iconBackground,
                contentColor = CreateIconContent,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                text = stringResource(option.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(option.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun CreateReminderContentPreview() {
    RememberAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            CreateReminderContent(
                options = CreateReminderOption.entries,
                onOptionClick = {},
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
