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
import com.example.uade.rememberapp.ui.theme.NavBarContainer
import com.example.uade.rememberapp.ui.theme.NavBarContent
import com.example.uade.rememberapp.ui.theme.NavBarOnSelected
import com.example.uade.rememberapp.ui.theme.NavBarSelected
import com.example.uade.rememberapp.ui.theme.RememberAppTheme

/**
 * Destinos principales de la app. El orden es el de la barra y también el de las páginas:
 * deslizar a la izquierda pasa al siguiente.
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
 * Barra de navegación inferior flotante (una "píldora" celeste). El destino elegido se
 * muestra con ícono y texto sobre fondo oscuro; los demás, solo con el ícono.
 *
 * Se dibuja una sola vez, en [MainTabs], para todas las pantallas principales.
 */
@Composable
fun AppBottomBar(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(NavBarContainer)
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
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) NavBarOnSelected else NavBarContent
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) NavBarSelected else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = if (selected) 18.dp else 14.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(destination.icon),
            // Si no está elegido no hay texto visible, así que el ícono lleva la descripción.
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
