---
name: caso-de-uso
description: Crea un caso de uso en domain/usecase que coordina repositorios y schedulers del dominio, y lo conecta al ViewModel que lo necesita. Usar cuando se pida agregar lógica de negocio (crear, completar o borrar recordatorios, guardar lugares, programar avisos) o sacar lógica de un ViewModel.
---

# Nuevo caso de uso

## Reglas

- Un caso de uso por clase, en `domain/usecase/`, con nombre `<Verbo><Sustantivo>UseCase` (ej. `CompleteReminderUseCase`, `SaveReminderUseCase`, `ObserveRemindersUseCase`).
- **Kotlin puro**: nada de `android.*`, `Context`, Room ni Compose. Solo modelos, interfaces de `domain/repository/` e interfaces de `domain/scheduler/`.
- Dependencias por constructor, siempre como interfaces.
- Se invoca con `operator fun invoke(...)`: `suspend` si ejecuta una acción; si observa datos, sin `suspend` y devolviendo `Flow`.

```kotlin
/** KDoc: qué regla de negocio encapsula y por qué no vive en el ViewModel. */
class CompleteReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: Long, isDone: Boolean) {
        val reminder = reminders.getById(id) ?: return
        reminders.save(reminder.copy(isDone = isDone))
        if (isDone) scheduler.cancel(id)
    }
}
```

## Dónde va cada lógica

- **Caso de uso**: reglas que coordinan más de una cosa. Ej.: al guardar un recordatorio con `Trigger.AtTime`, programar el aviso; con `Trigger.AtPlace`, asegurar la geofence del lugar; al cambiar el trigger, cancelar el anterior.
- **ViewModel**: estado de pantalla y validaciones de formulario (campo vacío, etc.).
- **Repositorio**: solo leer y escribir datos.

## Conectarlo

1. Registrarlo en el `AppContainer` (si existe) como `val xUseCase by lazy { XUseCase(repo, scheduler) }` o como función que crea una instancia nueva.
2. Pasarlo por constructor al ViewModel y actualizar su `Factory` (ver skill `nueva-pantalla`).
3. Reemplazar los `TODO:` o los datos de ejemplo (`sampleReminders()`) del ViewModel si corresponde.

## Test

Agregar un test unitario en `app/src/test/java/com/example/uade/rememberapp/domain/usecase/` con fakes a mano de los repositorios (clases `Fake...Repository` que guardan en un `MutableMap`). No agregar Mockito ni MockK sin preguntar.
