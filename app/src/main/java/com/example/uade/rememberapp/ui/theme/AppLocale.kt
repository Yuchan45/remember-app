package com.example.uade.rememberapp.ui.theme

import androidx.compose.runtime.Composable
import java.util.Locale

/**
 * Idioma en que la app formatea fechas y horas ("Septiembre de 2026", "Mar 29 sept").
 *
 * Es fijo y no sale del teléfono: la app tiene los textos solo en español, y con el teléfono
 * en inglés las fechas saldrían en inglés al lado de todo lo demás en español.
 *
 * Es una constante y no un recurso de strings.xml a propósito: lo usan componentes que están
 * en todas las pantallas (SectionLabel, MonthCalendar, las cards), y si leer el recurso falla
 * (pasa en las previews de Android Studio cuando la clase R quedó desactualizada), se rompen
 * todas las previews. Si algún día se traduce la app, acá habría que tomar el idioma de la
 * configuración de recursos.
 */
val AppLocale: Locale = Locale.forLanguageTag("es-AR")

/** [AppLocale] para usar desde composables. */
@Composable
fun rememberAppLocale(): Locale = AppLocale
