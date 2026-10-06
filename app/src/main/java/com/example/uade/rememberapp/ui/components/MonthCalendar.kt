package com.example.uade.rememberapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.ui.theme.rememberAppLocale
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendario de un mes:
 * ```
 *  <     Septiembre de 2026     >
 *  D   L   M   M   J   V   S
 *          1   2   3   4   5
 *  …  (29)  30
 * ```
 * - La semana empieza en [firstDayOfWeek] (domingo, como el calendario de Google); los
 *   nombres de los días y del mes salen del idioma de la app.
 * - Los días anteriores a [minDate] se ven apagados y no se pueden elegir.
 *
 * No guarda estado: el mes que se ve ([month]) y el día elegido ([selected]) entran por
 * parámetro; cambiar de mes y tocar un día salen por [onMonthChange] y [onDayClick].
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate?,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
) {
    val locale = rememberAppLocale()
    val weekDays = remember(firstDayOfWeek) { List(7) { firstDayOfWeek.plus(it.toLong()) } }
    val weeks = remember(month, firstDayOfWeek) { monthWeeks(month, firstDayOfWeek) }
    val today = remember { LocalDate.now() }

    Column(modifier = modifier) {
        MonthHeader(
            month = month,
            locale = locale,
            canGoBack = minDate == null || month > YearMonth.from(minDate),
            onPrevious = { onMonthChange(month.minusMonths(1)) },
            onNext = { onMonthChange(month.plusMonths(1)) },
        )

        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.initial(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
            }
        }

        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                locale = locale,
                                isSelected = date == selected,
                                isToday = date == today,
                                isEnabled = minDate == null || !date.isBefore(minDate),
                                onClick = { onDayClick(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    locale: Locale,
    canGoBack: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val pattern = stringResource(R.string.calendar_month_year_pattern)
    val title = remember(month, locale, pattern) {
        month.format(DateTimeFormatter.ofPattern(pattern, locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.calendar_previous_month),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.calendar_next_month),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    locale: Locale,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> colors.onPrimary
        !isEnabled -> colors.onSurface.copy(alpha = 0.38f)
        isToday -> colors.primary
        else -> colors.onSurface
    }
    // TalkBack lee la fecha completa ("29 sept 2026"), no solo el número.
    val description = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) colors.primary else Color.Transparent)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Inicial del día, ej. "M" para miércoles (la abreviatura "narrow" en español da "X"). */
private fun DayOfWeek.initial(locale: Locale): String =
    getDisplayName(TextStyle.FULL, locale).first().uppercase(locale)

/**
 * Las semanas del mes como filas de 7, con null en los huecos antes del día 1 y después del
 * último día, empezando por [firstDayOfWeek].
 */
private fun monthWeeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    return days.chunked(7).map { week -> week + List(7 - week.size) { null } }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A3944)
@Composable
private fun MonthCalendarPreview() {
    RememberAppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            MonthCalendar(
                month = YearMonth.of(2026, 9),
                selected = LocalDate.of(2026, 9, 29),
                onMonthChange = {},
                onDayClick = {},
                minDate = LocalDate.of(2026, 9, 10),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
