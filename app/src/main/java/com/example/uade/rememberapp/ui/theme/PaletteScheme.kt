package com.example.uade.rememberapp.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.AppPalette

/**
 * Colores para el fondo de pantalla en degradé sutil.
 */
data class AppBackgroundColors(
    val top: Color,
    val bottom: Color,
)

/**
 * Colores para la barra accesible inferior flotante y el botón de acción principal (+).
 */
data class AppNavBarColors(
    val container: Color,
    val selectedBackground: Color,
    val selectedContent: Color,
    val unselectedContent: Color,
)

val LocalAppBackgroundColors = compositionLocalOf {
    AppBackgroundColors(
        top = Color(0xFF13232E),
        bottom = Color(0xFF0B141B),
    )
}

val LocalAppNavBarColors = compositionLocalOf {
    AppNavBarColors(
        container = Color(0xFFB9E3FF),
        selectedBackground = Color(0xFF0F1B23),
        selectedContent = Color(0xFFFFFFFF),
        unselectedContent = Color(0xFF0F1B23),
    )
}

val LocalIsDarkTheme = compositionLocalOf { true }

/**
 * Información de cada paleta disponible.
 */
data class PaletteInfo(
    val palette: AppPalette,
    @get:StringRes val nameRes: Int,
)

val AvailablePalettes = listOf(
    PaletteInfo(AppPalette.Dynamic, R.string.settings_palette_dynamic),
    PaletteInfo(AppPalette.HeyBlue, R.string.settings_palette_hey_blue),
    PaletteInfo(AppPalette.EmeraldGreen, R.string.settings_palette_emerald_green),
    PaletteInfo(AppPalette.CoralOrange, R.string.settings_palette_coral_orange),
    PaletteInfo(AppPalette.OliveEarth, R.string.settings_palette_olive_earth),
    PaletteInfo(AppPalette.AmberGold, R.string.settings_palette_amber_gold),
    PaletteInfo(AppPalette.PurpleLavender, R.string.settings_palette_purple_lavender),
    PaletteInfo(AppPalette.RosePink, R.string.settings_palette_rose_pink),
)

/**
 * Obtiene los colores de muestra (swatch) para los círculos de selección según sea tema claro u oscuro.
 */
fun getPaletteSwatchColors(palette: AppPalette, isDarkTheme: Boolean): List<Color> {
    return if (isDarkTheme) {
        when (palette) {
            AppPalette.Dynamic -> listOf(Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D), Color(0xFFBA68C8))
            AppPalette.HeyBlue -> listOf(Color(0xFF9CD3F7), Color(0xFF1F5B80), Color(0xFF64B5F6))
            AppPalette.EmeraldGreen -> listOf(Color(0xFF7EDB94), Color(0xFF005328), Color(0xFF60D394))
            AppPalette.CoralOrange -> listOf(Color(0xFFFFB59D), Color(0xFF7D2E0A), Color(0xFFFF8A65))
            AppPalette.OliveEarth -> listOf(Color(0xFFB8D18C), Color(0xFF394E10), Color(0xFFA5D6A7))
            AppPalette.AmberGold -> listOf(Color(0xFFF5C842), Color(0xFF594400), Color(0xFFFFD54F))
            AppPalette.PurpleLavender -> listOf(Color(0xFFD0BCFF), Color(0xFF4F378B), Color(0xFFCCC2DC))
            AppPalette.RosePink -> listOf(Color(0xFFFFB1C8), Color(0xFF7B2949), Color(0xFFE5BDC6))
        }
    } else {
        when (palette) {
            AppPalette.Dynamic -> listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), Color(0xFF7B1FA2))
            AppPalette.HeyBlue -> listOf(Color(0xFF006495), Color(0xFFCBE6FF), Color(0xFF50606E))
            AppPalette.EmeraldGreen -> listOf(Color(0xFF006D37), Color(0xFF9AF8AF), Color(0xFF436651))
            AppPalette.CoralOrange -> listOf(Color(0xFFA13E14), Color(0xFFFFDBCF), Color(0xFF77574C))
            AppPalette.OliveEarth -> listOf(Color(0xFF4C6627), Color(0xFFD3EEA6), Color(0xFF57624A))
            AppPalette.AmberGold -> listOf(Color(0xFF755B00), Color(0xFFFFE088), Color(0xFF685E40))
            AppPalette.PurpleLavender -> listOf(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFF625B71))
            AppPalette.RosePink -> listOf(Color(0xFF982552), Color(0xFFFFD9E2), Color(0xFF73575F))
        }
    }
}

/**
 * Genera los colores del fondo de la aplicación adaptados a la paleta elegida y al modo claro/oscuro.
 */
fun buildBackgroundColors(
    palette: AppPalette,
    isDarkTheme: Boolean,
    usePureBlack: Boolean,
    colorScheme: ColorScheme,
): AppBackgroundColors {
    if (isDarkTheme) {
        if (usePureBlack) {
            return AppBackgroundColors(
                top = Color(0xFF0C0C0E),
                bottom = Color(0xFF000000),
            )
        }
        return when (palette) {
            AppPalette.HeyBlue -> AppBackgroundColors(top = Color(0xFF13232E), bottom = Color(0xFF0B141B))
            AppPalette.EmeraldGreen -> AppBackgroundColors(top = Color(0xFF10281A), bottom = Color(0xFF09160E))
            AppPalette.CoralOrange -> AppBackgroundColors(top = Color(0xFF2A1913), bottom = Color(0xFF17100D))
            AppPalette.OliveEarth -> AppBackgroundColors(top = Color(0xFF202715), bottom = Color(0xFF11150C))
            AppPalette.AmberGold -> AppBackgroundColors(top = Color(0xFF292310), bottom = Color(0xFF161309))
            AppPalette.PurpleLavender -> AppBackgroundColors(top = Color(0xFF231830), bottom = Color(0xFF130E1A))
            AppPalette.RosePink -> AppBackgroundColors(top = Color(0xFF2B1622), bottom = Color(0xFF180D13))
            AppPalette.Dynamic -> AppBackgroundColors(top = colorScheme.surfaceContainerHigh, bottom = colorScheme.background)
        }
    } else {
        return when (palette) {
            AppPalette.HeyBlue -> AppBackgroundColors(top = Color(0xFFDBEAF5), bottom = Color(0xFFF6FAFD))
            AppPalette.EmeraldGreen -> AppBackgroundColors(top = Color(0xFFD8EFE0), bottom = Color(0xFFF4FAF6))
            AppPalette.CoralOrange -> AppBackgroundColors(top = Color(0xFFFDE4DB), bottom = Color(0xFFFAF6F4))
            AppPalette.OliveEarth -> AppBackgroundColors(top = Color(0xFFE2EDD2), bottom = Color(0xFFF7FAF3))
            AppPalette.AmberGold -> AppBackgroundColors(top = Color(0xFFFDF1D2), bottom = Color(0xFFFAF8F2))
            AppPalette.PurpleLavender -> AppBackgroundColors(top = Color(0xFFEBDDFC), bottom = Color(0xFFF8F5FA))
            AppPalette.RosePink -> AppBackgroundColors(top = Color(0xFFFCDDE7), bottom = Color(0xFFFAF4F6))
            AppPalette.Dynamic -> AppBackgroundColors(top = colorScheme.surfaceContainerHigh, bottom = colorScheme.background)
        }
    }
}

/**
 * Genera los colores para la barra accesible inferior flotante, asegurando un contraste óptimo.
 */
fun buildNavBarColors(
    palette: AppPalette,
    isDarkTheme: Boolean,
    colorScheme: ColorScheme,
): AppNavBarColors {
    return if (isDarkTheme) {
        when (palette) {
            AppPalette.HeyBlue -> AppNavBarColors(
                container = Color(0xFFB9E3FF),
                selectedBackground = Color(0xFF0F1B23),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF0F1B23),
            )
            AppPalette.EmeraldGreen -> AppNavBarColors(
                container = Color(0xFFB0F2C2),
                selectedBackground = Color(0xFF082312),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF082312),
            )
            AppPalette.CoralOrange -> AppNavBarColors(
                container = Color(0xFFFFCCBC),
                selectedBackground = Color(0xFF2B1209),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF2B1209),
            )
            AppPalette.OliveEarth -> AppNavBarColors(
                container = Color(0xFFD6EEAA),
                selectedBackground = Color(0xFF131D08),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF131D08),
            )
            AppPalette.AmberGold -> AppNavBarColors(
                container = Color(0xFFFFE082),
                selectedBackground = Color(0xFF251A03),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF251A03),
            )
            AppPalette.PurpleLavender -> AppNavBarColors(
                container = Color(0xFFE8DEF8),
                selectedBackground = Color(0xFF1D1429),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF1D1429),
            )
            AppPalette.RosePink -> AppNavBarColors(
                container = Color(0xFFFFD9E2),
                selectedBackground = Color(0xFF2B101C),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF2B101C),
            )
            AppPalette.Dynamic -> AppNavBarColors(
                container = colorScheme.primaryContainer,
                selectedBackground = colorScheme.inverseSurface,
                selectedContent = colorScheme.inverseOnSurface,
                unselectedContent = colorScheme.onPrimaryContainer,
            )
        }
    } else {
        when (palette) {
            AppPalette.HeyBlue -> AppNavBarColors(
                container = Color(0xFFCBE6FF),
                selectedBackground = Color(0xFF006495),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF001E30),
            )
            AppPalette.EmeraldGreen -> AppNavBarColors(
                container = Color(0xFFC4F2CF),
                selectedBackground = Color(0xFF006D37),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF00210D),
            )
            AppPalette.CoralOrange -> AppNavBarColors(
                container = Color(0xFFFFDBCF),
                selectedBackground = Color(0xFFA13E14),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF380C00),
            )
            AppPalette.OliveEarth -> AppNavBarColors(
                container = Color(0xFFD8EEB2),
                selectedBackground = Color(0xFF4C6627),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF111F00),
            )
            AppPalette.AmberGold -> AppNavBarColors(
                container = Color(0xFFFFE088),
                selectedBackground = Color(0xFF755B00),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF241A00),
            )
            AppPalette.PurpleLavender -> AppNavBarColors(
                container = Color(0xFFEADDFF),
                selectedBackground = Color(0xFF6750A4),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF21005D),
            )
            AppPalette.RosePink -> AppNavBarColors(
                container = Color(0xFFFFD9E2),
                selectedBackground = Color(0xFF982552),
                selectedContent = Color(0xFFFFFFFF),
                unselectedContent = Color(0xFF3E001D),
            )
            AppPalette.Dynamic -> AppNavBarColors(
                container = colorScheme.primaryContainer,
                selectedBackground = colorScheme.primary,
                selectedContent = colorScheme.onPrimary,
                unselectedContent = colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Crea el [ColorScheme] de Material3 a partir de la paleta elegida, el modo (oscuro/claro)
 * y la opción de negro puro (AMOLED).
 */
fun buildColorScheme(
    context: Context,
    palette: AppPalette,
    isDarkTheme: Boolean,
    usePureBlack: Boolean,
): ColorScheme {
    if (palette == AppPalette.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val baseScheme = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (isDarkTheme && usePureBlack) {
            applyPureBlackSurfaces(baseScheme)
        } else {
            baseScheme
        }
    }

    val resolvedPalette = if (palette == AppPalette.Dynamic) AppPalette.HeyBlue else palette
    val scheme = if (isDarkTheme) {
        getDarkColorScheme(resolvedPalette)
    } else {
        getLightColorScheme(resolvedPalette)
    }

    return if (isDarkTheme && usePureBlack) {
        applyPureBlackSurfaces(scheme)
    } else {
        scheme
    }
}

private fun applyPureBlackSurfaces(base: ColorScheme): ColorScheme = base.copy(
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6EDF2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6EDF2),
    surfaceVariant = Color(0xFF1A1A1E),
    onSurfaceVariant = Color(0xFFA0ACB6),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0D10),
    surfaceContainer = Color(0xFF141418),
    surfaceContainerHigh = Color(0xFF1E1E24),
    surfaceContainerHighest = Color(0xFF282830),
    outline = Color(0xFF383842),
    outlineVariant = Color(0xFF26262D),
    inverseSurface = Color(0xFFE6EDF2),
    inverseOnSurface = Color(0xFF000000),
)

private fun getDarkColorScheme(palette: AppPalette): ColorScheme {
    val (primary, onPrimary, primaryContainer, onPrimaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF9CD3F7), Color(0xFF0B141B), Color(0xFF1F5B80), Color(0xFFD6ECFB))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF7EDB94), Color(0xFF00391A), Color(0xFF005328), Color(0xFF9AF8AF))
        AppPalette.CoralOrange -> Quad(Color(0xFFFFB59D), Color(0xFF5C1B00), Color(0xFF7D2E0A), Color(0xFFFFDBCF))
        AppPalette.OliveEarth -> Quad(Color(0xFFB8D18C), Color(0xFF253600), Color(0xFF394E10), Color(0xFFD3EEA6))
        AppPalette.AmberGold -> Quad(Color(0xFFF5C842), Color(0xFF3E2E00), Color(0xFF594400), Color(0xFFFFE088))
        AppPalette.PurpleLavender -> Quad(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF))
        AppPalette.RosePink -> Quad(Color(0xFFFFB1C8), Color(0xFF5E1133), Color(0xFF7B2949), Color(0xFFFFD9E2))
        AppPalette.Dynamic -> Quad(Color(0xFF9CD3F7), Color(0xFF0B141B), Color(0xFF1F5B80), Color(0xFFD6ECFB))
    }

    val (secondary, onSecondary, secondaryContainer, onSecondaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF64B5F6), Color(0xFF003258), Color(0xFF1E3A52), Color(0xFFCBE6FF))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF60D394), Color(0xFF003822), Color(0xFF1B4D36), Color(0xFFB7F2CE))
        AppPalette.CoralOrange -> Quad(Color(0xFFFF8A65), Color(0xFF4E1600), Color(0xFF6E2812), Color(0xFFFFCCBC))
        AppPalette.OliveEarth -> Quad(Color(0xFFA5D6A7), Color(0xFF10381B), Color(0xFF274F30), Color(0xFFC0F2C2))
        AppPalette.AmberGold -> Quad(Color(0xFFFFD54F), Color(0xFF3E2D00), Color(0xFF5B4300), Color(0xFFFFE082))
        AppPalette.PurpleLavender -> Quad(Color(0xFFCCC2DC), Color(0xFF332D41), Color(0xFF4A4458), Color(0xFFE8DEF8))
        AppPalette.RosePink -> Quad(Color(0xFFE5BDC6), Color(0xFF432931), Color(0xFF5B3F47), Color(0xFFFFD9E2))
        AppPalette.Dynamic -> Quad(Color(0xFF64B5F6), Color(0xFF003258), Color(0xFF1E3A52), Color(0xFFCBE6FF))
    }

    val (tertiary, onTertiary, tertiaryContainer, onTertiaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF80DEEA), Color(0xFF00363D), Color(0xFF004F58), Color(0xFFB4F3FD))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF80CBC4), Color(0xFF003733), Color(0xFF00504B), Color(0xFF9CF2EB))
        AppPalette.CoralOrange -> Quad(Color(0xFFFFB74D), Color(0xFF452B00), Color(0xFF644000), Color(0xFFFFDDB3))
        AppPalette.OliveEarth -> Quad(Color(0xFF80CBC4), Color(0xFF003734), Color(0xFF00504C), Color(0xFF9CF2EB))
        AppPalette.AmberGold -> Quad(Color(0xFFFFB300), Color(0xFF452B00), Color(0xFF644000), Color(0xFFFFDF9E))
        AppPalette.PurpleLavender -> Quad(Color(0xFFEFB8C8), Color(0xFF492532), Color(0xFF633B48), Color(0xFFFFD8E4))
        AppPalette.RosePink -> Quad(Color(0xFFF0B7A6), Color(0xFF48251B), Color(0xFF623B30), Color(0xFFFFDBD1))
        AppPalette.Dynamic -> Quad(Color(0xFF80DEEA), Color(0xFF00363D), Color(0xFF004F58), Color(0xFFB4F3FD))
    }

    val (bg, surfaceLow, surfaceNorm, surfaceHigh, surfaceHighest) = when (palette) {
        AppPalette.HeyBlue -> Quint(Color(0xFF0B141B), Color(0xFF15222B), Color(0xFF1E2C36), Color(0xFF273641), Color(0xFF31414D))
        AppPalette.EmeraldGreen -> Quint(Color(0xFF09160E), Color(0xFF132219), Color(0xFF1A2D22), Color(0xFF23392C), Color(0xFF2C4436))
        AppPalette.CoralOrange -> Quint(Color(0xFF17100D), Color(0xFF221713), Color(0xFF2D201A), Color(0xFF382922), Color(0xFF44332B))
        AppPalette.OliveEarth -> Quint(Color(0xFF11150C), Color(0xFF192011), Color(0xFF222B18), Color(0xFF2B3620), Color(0xFF364329))
        AppPalette.AmberGold -> Quint(Color(0xFF161309), Color(0xFF201B0E), Color(0xFF2B2514), Color(0xFF372F1C), Color(0xFF443B24))
        AppPalette.PurpleLavender -> Quint(Color(0xFF130E1A), Color(0xFF1C1425), Color(0xFF261D32), Color(0xFF322740), Color(0xFF3E314F))
        AppPalette.RosePink -> Quint(Color(0xFF180D13), Color(0xFF22131B), Color(0xFF2E1B26), Color(0xFF3B2431), Color(0xFF482E3D))
        AppPalette.Dynamic -> Quint(Color(0xFF0B141B), Color(0xFF15222B), Color(0xFF1E2C36), Color(0xFF273641), Color(0xFF31414D))
    }

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = bg,
        onBackground = Color(0xFFE6EDF2),
        surface = bg,
        onSurface = Color(0xFFE6EDF2),
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = Color(0xFF9AA8B2),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceNorm,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = Color(0xFF3B4B56),
        outlineVariant = Color(0xFF283640),
        inverseSurface = Color(0xFFE6EDF2),
        inverseOnSurface = bg,
        inversePrimary = primaryContainer,
    )
}

private fun getLightColorScheme(palette: AppPalette): ColorScheme {
    val (primary, onPrimary, primaryContainer, onPrimaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF006495), Color(0xFFFFFFFF), Color(0xFFCBE6FF), Color(0xFF001E30))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF006D37), Color(0xFFFFFFFF), Color(0xFF9AF8AF), Color(0xFF00210D))
        AppPalette.CoralOrange -> Quad(Color(0xFFA13E14), Color(0xFFFFFFFF), Color(0xFFFFDBCF), Color(0xFF380C00))
        AppPalette.OliveEarth -> Quad(Color(0xFF4C6627), Color(0xFFFFFFFF), Color(0xFFD3EEA6), Color(0xFF111F00))
        AppPalette.AmberGold -> Quad(Color(0xFF755B00), Color(0xFFFFFFFF), Color(0xFFFFE088), Color(0xFF241A00))
        AppPalette.PurpleLavender -> Quad(Color(0xFF6750A4), Color(0xFFFFFFFF), Color(0xFFEADDFF), Color(0xFF21005D))
        AppPalette.RosePink -> Quad(Color(0xFF982552), Color(0xFFFFFFFF), Color(0xFFFFD9E2), Color(0xFF3E001D))
        AppPalette.Dynamic -> Quad(Color(0xFF006495), Color(0xFFFFFFFF), Color(0xFFCBE6FF), Color(0xFF001E30))
    }

    val (secondary, onSecondary, secondaryContainer, onSecondaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF50606E), Color(0xFFFFFFFF), Color(0xFFD3E4F5), Color(0xFF0C1D29))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF436651), Color(0xFFFFFFFF), Color(0xFFC5ECCF), Color(0xFF012111))
        AppPalette.CoralOrange -> Quad(Color(0xFF77574C), Color(0xFFFFFFFF), Color(0xFFFFDBCF), Color(0xFF2C160E))
        AppPalette.OliveEarth -> Quad(Color(0xFF57624A), Color(0xFFFFFFFF), Color(0xFFDBE7C8), Color(0xFF151E0B))
        AppPalette.AmberGold -> Quad(Color(0xFF685E40), Color(0xFFFFFFFF), Color(0xFFF0E2BC), Color(0xFF221B04))
        AppPalette.PurpleLavender -> Quad(Color(0xFF625B71), Color(0xFFFFFFFF), Color(0xFFE8DEF8), Color(0xFF1E192B))
        AppPalette.RosePink -> Quad(Color(0xFF73575F), Color(0xFFFFFFFF), Color(0xFFFFD9E2), Color(0xFF2B151C))
        AppPalette.Dynamic -> Quad(Color(0xFF50606E), Color(0xFFFFFFFF), Color(0xFFD3E4F5), Color(0xFF0C1D29))
    }

    val (tertiary, onTertiary, tertiaryContainer, onTertiaryContainer) = when (palette) {
        AppPalette.HeyBlue -> Quad(Color(0xFF006875), Color(0xFFFFFFFF), Color(0xFFA1EFFF), Color(0xFF001F24))
        AppPalette.EmeraldGreen -> Quad(Color(0xFF006A63), Color(0xFFFFFFFF), Color(0xFF9CF2EB), Color(0xFF00201D))
        AppPalette.CoralOrange -> Quad(Color(0xFF78590C), Color(0xFFFFFFFF), Color(0xFFFFDF9E), Color(0xFF261A00))
        AppPalette.OliveEarth -> Quad(Color(0xFF386663), Color(0xFFFFFFFF), Color(0xFFBBECE8), Color(0xFF00201E))
        AppPalette.AmberGold -> Quad(Color(0xFF46662C), Color(0xFFFFFFFF), Color(0xFFC7EDAA), Color(0xFF0C2000))
        AppPalette.PurpleLavender -> Quad(Color(0xFF7D5260), Color(0xFFFFFFFF), Color(0xFFFFD8E4), Color(0xFF31111D))
        AppPalette.RosePink -> Quad(Color(0xFF7F5548), Color(0xFFFFFFFF), Color(0xFFFFDBD1), Color(0xFF32130A))
        AppPalette.Dynamic -> Quad(Color(0xFF006875), Color(0xFFFFFFFF), Color(0xFFA1EFFF), Color(0xFF001F24))
    }

    val (bg, surfaceLow, surfaceNorm, surfaceHigh, surfaceHighest) = when (palette) {
        AppPalette.HeyBlue -> Quint(Color(0xFFF6FAFD), Color(0xFFEEF5FA), Color(0xFFE5EEF5), Color(0xFFDCE6EE), Color(0xFFD2DCE5))
        AppPalette.EmeraldGreen -> Quint(Color(0xFFF4FAF6), Color(0xFFEDF7EF), Color(0xFFE2F0E5), Color(0xFFD7E8DC), Color(0xFFCDE0D2))
        AppPalette.CoralOrange -> Quint(Color(0xFFFAF6F4), Color(0xFFF8EFEA), Color(0xFFF2E5DF), Color(0xFFEBDCD5), Color(0xFFE2D2CA))
        AppPalette.OliveEarth -> Quint(Color(0xFFF7FAF3), Color(0xFFEFF5E8), Color(0xFFE4ECD9), Color(0xFFD9E3CE), Color(0xFFCEDAC2))
        AppPalette.AmberGold -> Quint(Color(0xFFFAF8F2), Color(0xFFF6F2E5), Color(0xFFEFE8D6), Color(0xFFE6DEC9), Color(0xFFDCD2BB))
        AppPalette.PurpleLavender -> Quint(Color(0xFFF8F5FA), Color(0xFFF2ECF7), Color(0xFFE8E0EF), Color(0xFFDDD3E6), Color(0xFFD2C7DD))
        AppPalette.RosePink -> Quint(Color(0xFFFAF4F6), Color(0xFFF7ECF0), Color(0xFFEFE0E6), Color(0xFFE6D4DC), Color(0xFFDCC8D1))
        AppPalette.Dynamic -> Quint(Color(0xFFF7FAFD), Color(0xFFF1F5F9), Color(0xFFE8EEF5), Color(0xFFDFE6F0), Color(0xFFD4DFEC))
    }

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = bg,
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceNorm,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
        inverseSurface = Color(0xFF1E293B),
        inverseOnSurface = Color(0xFFF8FAFC),
        inversePrimary = primaryContainer,
    )
}

private data class Quad(
    val first: Color,
    val second: Color,
    val third: Color,
    val fourth: Color,
)

private data class Quint(
    val first: Color,
    val second: Color,
    val third: Color,
    val fourth: Color,
    val fifth: Color,
)
