package com.example.uade.rememberapp.ui.reminders.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.Reminder
import com.example.uade.rememberapp.ui.reminders.sample.SampleReminders
import com.example.uade.rememberapp.ui.theme.RememberAppTheme
import com.example.uade.rememberapp.ui.theme.SwipeActionContent
import com.example.uade.rememberapp.ui.theme.SwipeArchiveColor
import com.example.uade.rememberapp.ui.theme.SwipeDoneColor
import com.example.uade.rememberapp.ui.theme.SwipeTrashColor
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt

/** Parte del ancho que pueden ocupar las acciones: la card nunca queda por debajo del 45%. */
private const val RevealFraction = 0.55f

/** Radio de las esquinas de [ReminderCard]; el bloque de acciones usa el mismo. */
private val CornerRadius = 20.dp

/**
 * Redondeado solo a la derecha: el lado izquierdo queda debajo de la card, y si también fuera
 * redondeado se vería una muesca de fondo entre su curva y la de la card.
 */
private val ActionsShape = RoundedCornerShape(topEnd = CornerRadius, bottomEnd = CornerRadius)

/** Velocidad (px/s) a partir de la cual un deslizamiento corto igual abre o cierra. */
private const val FlingVelocity = 1000f

/**
 * [ReminderCard] que, al deslizarla hacia la izquierda, deja ver tres acciones en un solo bloque:
 * ```
 * ┌──────────────╮────────┬────────┬────────┐
 * │ card angosta │   ✓    │   ⬇    │   🗑   │
 * │  (≥ 45%)     │ Hecho  │Archivar│Papelera│
 * └──────────────╯────────┴────────┴────────┘
 * ```
 * - Las acciones van pegadas a la card: el bloque arranca por debajo de sus esquinas
 *   redondeadas, así en esas esquinas se ve el verde de "Hecho" y no un hueco de fondo.
 * - Toman exactamente el alto de la card (no la agrandan) y solo se dibujan mientras está
 *   abierta: cerrada no asoma nada detrás de sus esquinas.
 * - Al soltar, queda abierta o cerrada según cuánto se deslizó (o qué tan rápido). Con la card
 *   abierta, tocarla la cierra en vez de abrir el recordatorio.
 * - Qué card está abierta lo guarda [revealGroup], compartido por toda la lista: hay una sola
 *   abierta a la vez, y tocar en cualquier otro lado la cierra (ver [closeSwipeOnTapOutside]).
 *
 * Es estado puramente visual (dónde está la card), así que vive en la UI con `remember` y no en
 * el ViewModel.
 */
@Composable
fun SwipeableReminderCard(
    reminder: Reminder,
    placeName: String?,
    now: Instant,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
    revealGroup: SwipeRevealGroupState = rememberSwipeRevealGroupState(),
) {
    val isOpen = revealGroup.openId == reminder.id
    fun setOpen(open: Boolean) = revealGroup.setOpen(reminder.id, open)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // Los toques sobre la card abierta (y sus acciones) los maneja ella: no cuentan como
            // "afuera".
            .then(if (isOpen) Modifier.markAsInsideOpenCard(revealGroup) else Modifier),
    ) {
        val revealWidth = maxWidth * RevealFraction
        val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
        val scope = rememberCoroutineScope()
        // Cuánto se deslizó, en px: 0 = cerrada, -revealPx = abierta. La card se angosta en esa medida.
        val offset = remember { Animatable(0f) }

        // Se abre o cierra cuando cambia el grupo: al soltar el dedo, o porque se tocó afuera o
        // se abrió otra card.
        LaunchedEffect(isOpen, revealPx) {
            offset.animateTo(if (isOpen) -revealPx else 0f)
        }

        fun settle(open: Boolean) {
            // Si el grupo ya estaba así, el LaunchedEffect no se dispara: se anima acá igual.
            scope.launch { offset.animateTo(if (open) -revealPx else 0f) }
            setOpen(open)
        }

        // matchParentSize: las acciones no cuentan para el tamaño, lo define solo la card.
        SwipeActions(
            revealWidth = revealWidth,
            // Se lee en la fase de dibujo: arrastrar no recompone.
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = if (offset.value < 0f) 1f else 0f },
            // Cada acción cierra la card además de avisar, así no queda abierta.
            onDone = { onDone(); settle(open = false) },
            onArchive = { onArchive(); settle(open = false) },
            onTrash = { onTrash(); settle(open = false) },
        )

        ReminderCard(
            reminder = reminder,
            placeName = placeName,
            now = now,
            onClick = { if (isOpen) settle(open = false) else onClick() },
            modifier = Modifier
                .shrinkBy { offset.value }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Solo hacia la izquierda, y no más allá de las acciones.
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val open = when {
                            velocity < -FlingVelocity -> true
                            velocity > FlingVelocity -> false
                            else -> offset.value < -revealPx / 2
                        }
                        settle(open)
                    },
                ),
        )
    }
}

/**
 * Qué [SwipeableReminderCard] de una lista está abierta (como mucho una). Se crea con
 * [rememberSwipeRevealGroupState] en la pantalla y se pasa a cada card de la lista.
 */
@Stable
class SwipeRevealGroupState {
    /** Id del recordatorio cuya card está abierta, o null si están todas cerradas. */
    var openId: Long? by mutableStateOf(null)
        private set

    /** Lo marca la card abierta en cada toque que cae sobre ella; ver [closeSwipeOnTapOutside]. */
    internal var touchInsideOpenCard = false

    internal fun setOpen(id: Long, open: Boolean) {
        if (open) {
            openId = id
        } else if (openId == id) {
            openId = null
        }
    }

    fun closeAll() {
        openId = null
    }
}

@Composable
fun rememberSwipeRevealGroupState(): SwipeRevealGroupState = remember { SwipeRevealGroupState() }

/**
 * Cierra la card abierta de [state] cuando se toca en cualquier lado que no sea ella misma ni sus
 * acciones. Va sobre el contenedor de toda la pantalla.
 *
 * Solo observa: el toque sigue llegando a lo que se haya tocado (un botón, la lista, otra card).
 * Funciona por el orden de las pasadas de un toque en Compose: en la Initial (de padre a hijo) la
 * card abierta se marca como tocada, y en la Final (otra vez de padre a hijo, al terminar) este
 * contenedor revisa la marca. Si nadie la puso, el toque fue afuera.
 */
fun Modifier.closeSwipeOnTapOutside(state: SwipeRevealGroupState): Modifier = pointerInput(state) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        if (!state.touchInsideOpenCard) state.closeAll()
        state.touchInsideOpenCard = false
    }
}

/** Marca los toques que caen sobre la card abierta, para que [closeSwipeOnTapOutside] los ignore. */
private fun Modifier.markAsInsideOpenCard(state: SwipeRevealGroupState): Modifier = pointerInput(state) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        state.touchInsideOpenCard = true
    }
}

/**
 * Angosta el elemento desde la derecha según [dx] (px, negativo = más angosto), sin cambiar el
 * lugar que ocupa en el padre: así la card deja ver las acciones de atrás.
 *
 * [dx] se lee recién al medir (no al componer): mientras se arrastra solo se vuelve a medir la
 * card, sin recomponer nada.
 */
private fun Modifier.shrinkBy(dx: () -> Float): Modifier = layout { measurable, constraints ->
    val width = (constraints.maxWidth + dx()).roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
}

/**
 * El bloque de las tres acciones, pegado a la derecha. Ocupa [revealWidth] (lo que se angosta la
 * card) más [CornerRadius] que queda debajo de la card: esa franja oculta se pinta del color de
 * la primera acción, para que las esquinas redondeadas de la card no dejen ver el fondo.
 */
@Composable
private fun SwipeActions(
    revealWidth: Dp,
    onDone: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Row(
            modifier = Modifier
                .width(revealWidth + CornerRadius)
                .fillMaxHeight()
                .clip(ActionsShape),
        ) {
            Box(
                Modifier
                    .width(CornerRadius)
                    .fillMaxHeight()
                    .background(SwipeDoneColor),
            )
            SwipeAction(R.drawable.ic_check_circle, stringResource(R.string.reminders_action_done), SwipeDoneColor, onDone)
            SwipeAction(R.drawable.ic_archive, stringResource(R.string.reminders_action_archive), SwipeArchiveColor, onArchive)
            SwipeAction(R.drawable.ic_delete, stringResource(R.string.reminders_action_trash), SwipeTrashColor, onTrash)
        }
    }
}

/** Una acción: ícono arriba, texto abajo, sobre su color. Las tres se reparten el ancho. */
@Composable
private fun RowScope.SwipeAction(
    @DrawableRes icon: Int,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        // El texto ya describe la acción: el ícono es decorativo.
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SwipeActionContent,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = SwipeActionContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B141B, widthDp = 360)
@Composable
private fun SwipeableReminderCardPreview() {
    val now = Instant.now()
    val reminders = SampleReminders.previewReminders(now)
    RememberAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cerrada: no asoma nada detrás.
            SwipeableReminderCard(reminders[3], null, now, {}, {}, {}, {})
            // Abierta (así queda al soltar): una card alta y una baja.
            listOf(reminders[0], reminders[3]).forEach { reminder ->
                BoxWithConstraints {
                    val reveal = maxWidth * RevealFraction
                    SwipeActions(revealWidth = reveal, onDone = {}, onArchive = {}, onTrash = {}, modifier = Modifier.matchParentSize())
                    ReminderCard(reminder, null, now, {}, Modifier.padding(end = reveal))
                }
            }
        }
    }
}
