package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Línea separadora fina que se desvanece hacia los bordes: más sutil que un divisor común,
 * para separar zonas dentro de una misma superficie (ej. el título de la nota y sus opciones).
 */
@Composable
fun FadedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, color, color, Color.Transparent))),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E2C36)
@Composable
private fun FadedDividerPreview() {
    RememberAppTheme {
        FadedDivider(modifier = Modifier.padding(16.dp))
    }
}
