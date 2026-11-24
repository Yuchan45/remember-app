package com.example.uade.rememberapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.example.uade.rememberapp.domain.model.AppSettings
import com.example.uade.rememberapp.domain.model.ThemeMode

/**
 * Tema principal de Hey!.
 *
 * Aplica de forma reactiva el esquema de color (paleta elegida, modo claro/oscuro/sistema,
 * negro puro AMOLED), la tipografía accesible con escala de texto, el fondo degradado adaptado
 * y los colores accesibles de la barra inferior.
 */
@Composable
fun RememberAppTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = buildColorScheme(
        context = context,
        palette = settings.palette,
        isDarkTheme = isDarkTheme,
        usePureBlack = settings.usePureBlack,
    )

    val backgroundColors = buildBackgroundColors(
        palette = settings.palette,
        isDarkTheme = isDarkTheme,
        usePureBlack = settings.usePureBlack,
        colorScheme = colorScheme,
    )

    val navBarColors = buildNavBarColors(
        palette = settings.palette,
        isDarkTheme = isDarkTheme,
        colorScheme = colorScheme,
    )

    val typography = createAppTypography(
        fontFamily = settings.fontFamily,
        fontScale = settings.fontScale,
    )

    CompositionLocalProvider(
        LocalUseGradientBackground provides settings.useGradientBackground,
        LocalAppBackgroundColors provides backgroundColors,
        LocalAppNavBarColors provides navBarColors,
        LocalIsDarkTheme provides isDarkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
