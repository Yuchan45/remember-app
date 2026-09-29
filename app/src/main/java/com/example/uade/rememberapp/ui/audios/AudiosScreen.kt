package com.example.uade.rememberapp.ui.audios

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.PlaceholderScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Audios: a futuro, grabar y enviar recordatorios de voz. Por ahora es una pantalla template.
 *
 * TODO: cuando tenga estado, separarla en AudiosScreen (con ViewModel) + AudiosContent.
 */
@Composable
fun AudiosScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    PlaceholderScreen(
        title = stringResource(R.string.audios_title),
        icon = R.drawable.ic_queue_music,
        message = stringResource(R.string.audios_placeholder),
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Preview
@Composable
private fun AudiosScreenPreview() {
    RememberAppTheme {
        AudiosScreen()
    }
}
