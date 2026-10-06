package com.example.uade.rememberapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.uade.rememberapp.ui.theme.rememberAppLocale

/**
 * Título chico de una sección de opciones, en mayúsculas: "CUÁNDO", "REPETIR"…
 *
 * El texto se guarda normal en strings.xml ("Cuándo") y se pasa a mayúsculas acá, así se lee
 * bien también con TalkBack.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val locale = rememberAppLocale()
    Text(
        text = text.uppercase(locale),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
