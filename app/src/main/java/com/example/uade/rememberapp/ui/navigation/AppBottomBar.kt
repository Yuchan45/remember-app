package com.example.uade.rememberapp.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.ui.theme.AppNavBarColors
import com.example.uade.rememberapp.ui.theme.LocalAppNavBarColors
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Destinos principales de la app. El orden es el de la barra y también el de las páginas.
 */
enum class AppDestination(
    @get:DrawableRes val icon: Int,
    @get:StringRes val label: Int,
) {
    Home(R.drawable.ic_notes, R.string.nav_home),

    /** Recordatorios archivados y eliminados. */
    Archive(R.drawable.ic_archive, R.string.nav_archive),
    Places(R.drawable.ic_location_on, R.string.nav_places),
    Settings(R.drawable.ic_settings, R.string.nav_settings),
}

/**
 * Barra de navegación inferior flotante. Adapta sus colores reactivamente a la paleta elegida
 * y al modo claro/oscuro del sistema, manteniendo un contraste accesible de alta visibilidad.
 *
 * Se dibuja una sola vez, en [MainTabs], para todas las pantallas principales.
 */
@Composable
fun AppBottomBar(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    colors: AppNavBarColors = LocalAppNavBarColors.current,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.container)
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppDestination.entries.forEach { destination ->
            BottomBarItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onSelected(destination) },
                colors = colors,
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    colors: AppNavBarColors,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) colors.selectedContent else colors.unselectedContent
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.selectedBackground else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = if (selected) 18.dp else 14.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = if (selected) null else stringResource(destination.label),
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        if (selected) {
            Text(
                text = stringResource(destination.label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    RememberAppTheme {
        AppBottomBar(
            selected = AppDestination.Home,
            onSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
