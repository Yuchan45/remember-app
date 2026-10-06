package com.example.uade.rememberapp.ui.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Trigger
import com.example.uade.rememberapp.ui.components.PlaceholderMessage
import com.example.uade.rememberapp.ui.components.SegmentedSelector
import com.example.uade.rememberapp.ui.reminders.components.ReminderCard
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.appBackground
import java.time.Instant

/**
 * Sección Archivo: los recordatorios archivados y los eliminados, en dos pestañas.
 *
 * Esta función es la "con estado": consigue el ViewModel y lee su UiState. El diseño está en
 * [ArchiveContent].
 */
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArchiveContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onReminderClick = viewModel::onReminderClick,
        contentPadding = contentPadding,
    )
}

/**
 * ```
 * Archivo
 * [ ▣ Archivados | 🗑 Eliminados ]
 * (cards de la pestaña elegida, o un mensaje si está vacía)
 * ```
 */
@Composable
private fun ArchiveContent(
    uiState: ArchiveUiState,
    onTabSelected: (ArchiveTab) -> Unit,
    onReminderClick: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // "Ahora" fijo mientras la pantalla está abierta, para las horas de las cards.
    val now = remember { Instant.now() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .appBackground()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.archive_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )

        SegmentedSelector(
            options = ArchiveTab.entries,
            selected = uiState.selectedTab,
            onSelected = onTabSelected,
            label = { stringResource(it.label) },
            icon = { it.icon },
        )

        val reminders = uiState.visibleReminders
        if (reminders.isEmpty()) {
            PlaceholderMessage(
                icon = uiState.selectedTab.icon,
                message = stringResource(uiState.selectedTab.emptyMessage),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        placeName = (reminder.trigger as? Trigger.AtPlace)
                            ?.let { uiState.placeNames[it.placeId] },
                        now = now,
                        onClick = { onReminderClick(reminder.id) },
                    )
                }
            }
        }
    }
}

@Preview(name = "Archivados vacío")
@Composable
private fun ArchiveContentEmptyPreview() {
    RememberAppTheme {
        ArchiveContent(uiState = ArchiveUiState(), onTabSelected = {}, onReminderClick = {})
    }
}

@Preview(name = "Eliminados vacío")
@Composable
private fun ArchiveContentDeletedEmptyPreview() {
    RememberAppTheme {
        ArchiveContent(
            uiState = ArchiveUiState(selectedTab = ArchiveTab.Deleted),
            onTabSelected = {},
            onReminderClick = {},
        )
    }
}

@Preview(name = "Archivados con recordatorios", heightDp = 800)
@Composable
private fun ArchiveContentWithItemsPreview() {
    RememberAppTheme {
        ArchiveContent(
            uiState = ArchiveUiState(
                archived = SampleReminders.previewReminders(),
                placeNames = SampleReminders.placeNames,
            ),
            onTabSelected = {},
            onReminderClick = {},
        )
    }
}
