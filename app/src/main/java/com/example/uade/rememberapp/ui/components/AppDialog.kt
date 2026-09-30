package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Estilo común de los diálogos de la app (calendario, reloj, y los que vengan), para que se
 * vean como el resto de la interfaz y no como los de Material por defecto:
 * - fondo del mismo color que el modal de captura y las cards;
 * - esquinas de 28dp, como el modal;
 * - botón principal en celeste y semibold (como "Guardar"), y el de cancelar en gris.
 */
object AppDialogDefaults {

    val shape: Shape = RoundedCornerShape(28.dp)

    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Colores del reloj: esfera y horas en los tonos de las cards y chips, aguja celeste. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun timePickerColors(): TimePickerColors {
        val colors = MaterialTheme.colorScheme
        return TimePickerDefaults.colors(
            containerColor = containerColor,
            clockDialColor = colors.surfaceVariant,
            clockDialSelectedContentColor = colors.onPrimary,
            clockDialUnselectedContentColor = colors.onSurface,
            selectorColor = colors.primary,
            timeSelectorSelectedContainerColor = colors.primaryContainer,
            timeSelectorSelectedContentColor = colors.onPrimaryContainer,
            timeSelectorUnselectedContainerColor = colors.surfaceVariant,
            timeSelectorUnselectedContentColor = colors.onSurface,
        )
    }
}

/** Botón principal de un diálogo ("Listo", "Aceptar"): celeste y semibold. */
@Composable
fun DialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Variante rellena del botón principal (píldora celeste con texto oscuro), para diálogos
 * donde la acción de confirmar tiene que destacarse más, ej. "Hecho" en Etiquetas.
 */
@Composable
fun DialogFilledConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Botón secundario de un diálogo ("Cancelar"): gris, para que no compita con el principal. */
@Composable
fun DialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    }
}
