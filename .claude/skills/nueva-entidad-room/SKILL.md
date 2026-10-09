---
name: nueva-entidad-room
description: Implementa la persistencia local con Room para un modelo del dominio (entity, DAO, mapper, RepositoryImpl y cableado en el AppContainer). Usar cuando se pida guardar datos en la base, crear tablas, entidades, DAOs, implementar un repositorio o conectar la capa de datos.
---

# Persistencia con Room para un modelo del dominio

El dominio ya define los modelos (`domain/model/`) y las interfaces de repositorio (`domain/repository/`).
Esta skill crea la implementación en `data/`. **El dominio no se toca para adaptarlo a Room.**

## 0. Dependencias (solo la primera vez)

Revisar `gradle/libs.versions.toml`. Si Room no está:
- Agregar al catálogo `room` (runtime, ktx, compiler) y el plugin `ksp`. La versión de KSP tiene que corresponder a la versión de Kotlin del catálogo (`kotlin = ...`). Buscar la versión compatible; no adivinarla.
- En `app/build.gradle.kts`: `alias(libs.plugins.ksp)` en plugins, `implementation(libs.androidx.room.runtime)`, `implementation(libs.androidx.room.ktx)` y `ksp(libs.androidx.room.compiler)`.
- En el `build.gradle.kts` raíz: `alias(libs.plugins.ksp) apply false`.
- Correr `./gradlew assembleDebug` para confirmar que sincroniza antes de seguir.

## 1. Entity: `data/local/entity/<Modelo>Entity.kt`

```kotlin
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    ...
)
```
- Tipos simples: `Instant` se guarda como `Long` (epoch millis) directamente en la entity. No hace falta un TypeConverter.
- Lo que en el dominio es una lista va en una tabla aparte con `reminderId` y clave foránea con `onDelete = CASCADE` (ver `AlarmEntity`, `PlaceAlertEntity`). Si la relación es de muchos a muchos (ej. etiquetas), con una tabla intermedia (ver `ReminderTagCrossRef`).
- Para leer el modelo completo en una consulta: una clase con `@Embedded` + `@Relation` (ver `ReminderWithDetails`), y `@Transaction` en la consulta del DAO.

## 2. Mapper: `data/local/mapper/<Modelo>Mapper.kt`

Funciones de extensión puras, en los dos sentidos:
```kotlin
fun ReminderEntity.toDomain(): Reminder = ...
fun Reminder.toEntity(): ReminderEntity = ...
```
Los enums se guardan por nombre; si al leer el valor no existe (ej. se renombró), usar un valor por defecto en vez de crashear (ver `enumOrDefault` en `ReminderMapper`).

## 3. DAO: `data/local/dao/<Modelo>Dao.kt`

Un método por cada operación de la interfaz del repositorio:
- `observe...` → `Flow<List<Entity>>` (sin `suspend`)
- `getById` → `suspend fun ...: Entity?`
- `save` → `@Upsert suspend fun upsert(e: Entity): Long`. Ojo: con `@Upsert`, cuando actualiza devuelve `-1`; en el repo, si `id != 0`, devolver ese `id`.
- `delete` → `@Query("DELETE FROM ... WHERE id = :id")`

## 4. Database: `data/local/AppDatabase.kt`

Una sola `@Database(entities = [...], version = N, exportSchema = false)` con todas las entidades. Al agregar una entidad nueva o cambiar columnas, subir `version`. Mientras la app no esté publicada, `fallbackToDestructiveMigration(dropAllTables = true)` alcanza; avisar al usuario que borra los datos.

## 5. Repositorio: `data/repository/<Modelo>RepositoryImpl.kt`

```kotlin
class ReminderRepositoryImpl(private val dao: ReminderDao) : ReminderRepository {
    override fun observeAll() = dao.observeAll().map { list -> list.map { it.toDomain() } }
    ...
}
```
No hace lógica de negocio (por ejemplo, programar alarmas); eso va en un caso de uso.

## 6. AppContainer

- Si no existe, crear `AppContainer.kt` junto a `RememberApp.kt`, con `Room.databaseBuilder(...)` y los repositorios como `by lazy`, expuestos con el tipo de la **interfaz** del dominio.
- Descomentar `container` en `RememberApp`.

## 7. Verificar

- `./gradlew assembleDebug` (KSP genera el código de Room; los errores de queries aparecen acá).
- Si hay tiempo, un test unitario del mapper en `app/src/test/` (ida y vuelta, y valores desconocidos).
