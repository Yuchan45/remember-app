package com.example.uade.rememberapp.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.components.PlaceholderScreen
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Ajustes de la app. Por ahora es una pantalla template.
 *
 * TODO: cuando tenga estado, separarla en SettingsScreen (con ViewModel) + SettingsContent.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        icon = R.drawable.ic_settings,
        message = stringResource(R.string.settings_placeholder),
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    RememberAppTheme {
        SettingsScreen()
    }
}
