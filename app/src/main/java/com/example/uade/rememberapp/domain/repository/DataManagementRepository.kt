package com.example.uade.rememberapp.domain.repository

import com.example.uade.rememberapp.domain.model.backup.BackupMetadata
import com.example.uade.rememberapp.domain.model.backup.ImportResult
import java.io.File

/**
 * Contrato del dominio para la gestión de datos, respaldos, exportación/importación
 * y almacenamiento de la aplicación.
 */
interface DataManagementRepository {
    /**
     * Genera un archivo de copia de seguridad local con todos los recordatorios, lugares y etiquetas.
     */
    suspend fun createLocalBackup(): Result<BackupMetadata>

    /**
     * Retorna los metadatos de la copia de seguridad local más reciente, o null si no existe ninguna.
     */
    suspend fun getLatestLocalBackup(): BackupMetadata?

    /**
     * Restaura los datos a partir del respaldo local más reciente.
     */
    suspend fun restoreFromLatestLocalBackup(): Result<ImportResult>

    /**
     * Exporta los datos en formato JSON y devuelve el archivo listo para compartir o guardar.
     */
    suspend fun exportBackupFile(): Result<File>

    /**
     * Importa datos desde una cadena JSON, agregando o actualizando recordatorios, etiquetas y lugares.
     */
    suspend fun importBackupFromJson(jsonString: String): Result<ImportResult>

    /**
     * Obtiene el tamaño acumulado en bytes del directorio de caché (mapas, imágenes, temporales).
     */
    fun getCacheSizeBytes(): Long

    /**
     * Elimina todos los archivos del directorio de caché y devuelve la cantidad de bytes liberados.
     */
    suspend fun clearCache(): Long

    /**
     * Borra de forma definitiva e irreversible todos los datos: base de datos, fotos, audios,
     * caché y preferencias, reprogramando y cancelando alarmas y geocercas activas.
     */
    suspend fun clearAllData(): Boolean
}
