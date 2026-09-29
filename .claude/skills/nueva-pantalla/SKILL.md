---
name: nueva-pantalla
description: Crea una pantalla nueva en Jetpack Compose (Screen + Content + ViewModel + UiState + strings + preview) siguiendo el patrón del proyecto y la registra en la navegación. Usar cuando se pida agregar una pantalla, vista, screen, formulario o detalle nuevo.
---

# Nueva pantalla Compose

El patrón de referencia es `ui/reminders/list/RemindersListScreen.kt` + `RemindersListViewModel.kt`.
Leé esos dos archivos antes de empezar y copiá su estilo.

## Ubicación y nombres

- Carpeta: `ui/<feature>/<tipo>/`, donde `tipo` es `list`, `edit` o `detail`. Ejemplo: `ui/places/list/`.
- Archivos: `<Feature><Tipo>Screen.kt`, `<Feature><Tipo>ViewModel.kt` y `<Feature><Tipo>UiState.kt` (el UiState y sus enums en archivo aparte). Ejemplo: `PlacesListScreen.kt`, `PlacesListViewModel.kt`, `PlacesListUiState.kt`.
- Dónde va cada componente:
  - `ui/<feature>/<tipo>/components/`: piezas que solo usa esta pantalla.
  - `ui/<feature>/components/`: piezas que comparten varias pantallas de la feature.
  - `ui/components/`: piezas genéricas que no saben de ninguna feature.
- Datos de ejemplo para previews: `ui/<feature>/sample/`. Ejemplo: `SampleReminders`.
- Si el ViewModel ya existe (hay esqueletos en `places/edit`, `places/list`, `reminders/edit`, `reminders/detail`), completarlo en lugar de crear otro.

## 1. ViewModel + UiState (`<X>ViewModel.kt`)

```kotlin
data class XUiState(
    val isLoading: Boolean = false,
    // campos con valores por defecto
) {
    // lo derivado va como propiedad calculada, no como campo guardado
}

class XViewModel(
    // casos de uso por constructor (cuando existan)
) : ViewModel() {
    private val _uiState = MutableStateFlow(XUiState())
    val uiState: StateFlow<XUiState> = _uiState.asStateFlow()

    fun onAlgoChanged(valor: String) {
        _uiState.update { it.copy(algo = valor) }
    }
}
```

Reglas:
- El ViewModel **no** recibe `Context` ni importa `R` para leer textos. Si necesita un texto variable, guarda el `@StringRes Int`.
- Para trabajo asíncrono, usar `viewModelScope.launch { }`.
- Si recibe dependencias, crear un `companion object { val Factory = viewModelFactory { initializer { ... } } }` que las tome del `AppContainer` de `RememberApp`. Si el `AppContainer` todavía no existe, dejar un `TODO:` como en `RemindersListViewModel` y no inventarlo.

## 2. Pantalla (`<X>Screen.kt`)

Dos composables, siempre:

```kotlin
/** KDoc: qué muestra esta pantalla. */
@Composable
fun XScreen(
    viewModel: XViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},   // la navegación entra como lambda, nunca un NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    XContent(
        uiState = uiState,
        onAlgoChanged = viewModel::onAlgoChanged,
    )
}

@Composable
private fun XContent(
    uiState: XUiState,
    onAlgoChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding -> ... }
}

@Preview(showBackground = true)
@Composable
private fun XContentPreview() {
    RememberAppTheme {
        XContent(uiState = XUiState(/* datos de ejemplo */), onAlgoChanged = {})
    }
}
```

- `XContent` no toca el ViewModel ni el dominio: todo entra por parámetros y sale por lambdas.
- Agregar una preview por cada estado relevante: con contenido, vacío y cargando.
- Reutilizar lo que ya existe en `ui/components/` (`CircleIconButton`, `DropdownFilterChip`, `LabelChip`, `SegmentedSelector`), en `ui/<feature>/components/` (ej. `ReminderCard`) y en `ui/navigation/AppBottomBar`.
- Si la pantalla tiene muchos eventos, agruparlos en `data class XActions(val onAlgo: () -> Unit = {}, ...)`, como `RemindersListActions`.
- Padding horizontal de pantalla: `16.dp`. Títulos: `MaterialTheme.typography.headlineMedium` en negrita (ver `HomeHeader`).
- La app es oscura: fondo con `Brush.verticalGradient(listOf(BackgroundTop, Background))` y `Scaffold(containerColor = Color.Transparent)`.
- Íconos nuevos: vector drawable de Material Symbols (ver `CLAUDE.md`), nunca una librería de íconos.

## 3. Strings

- Agregar todos los textos a `app/src/main/res/values/strings.xml`, dentro de un bloque con comentario `<!-- Nombre de la pantalla -->`.
- Prefijo por feature: `places_list_title`, `reminder_edit_save`, etc.
- Si un texto tiene parámetros, documentarlos en el comentario (`%1$s = ..., %2$d = ...`), como hace `reminders_tab_with_count`.

## 4. Navegación

- Todo lo de navegación vive en `ui/navigation/`. Hay dos casos:
  - **Destino principal** (aparece en la barra inferior y se llega deslizando de costado): agregarlo a `AppDestination` en `AppBottomBar.kt` (el orden del enum es el orden de las páginas) y al `when` de `MainTabPage` en `MainTabs.kt`. No lleva ruta.
  - **Pantalla que se abre encima** (detalle, edición): agregar la ruta a `Routes.kt` (con argumento: `"reminders/{id}"`) y registrarla en `AppNavHost.kt` con `composable(Routes.X) { … }`. Estas pantallas no muestran la barra inferior.
- Las pantallas principales reciben `contentPadding: PaddingValues`, con el alto de la barra de estado y de la barra inferior, y lo aplican por dentro (como `contentPadding` de la lista o `padding` del contenido). Si la pantalla tiene su propio `Scaffold`, usar `contentWindowInsets = WindowInsets(0)` para no sumar los insets dos veces (ver `RemindersListScreen`).
- Para una sección que todavía no está hecha, usar `PlaceholderScreen` de `ui/components/` (ver `AudiosScreen`).
- No agregar el plugin de kotlinx-serialization para rutas type-safe sin preguntar antes.
- Las pantallas reciben la navegación como lambdas (`onReminderClick: (Long) -> Unit`); solo `AppNavHost` conoce el `NavController`.

## 5. Verificar

Correr `./gradlew assembleDebug` (skill `build-y-test`) y corregir los errores antes de dar la tarea por terminada.
