package com.example.uade.rememberapp.domain.usecase

import com.example.uade.rememberapp.domain.model.CropRect
import com.example.uade.rememberapp.domain.model.MAX_PHOTOS
import com.example.uade.rememberapp.domain.repository.PhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPhotosUseCaseTest {

    /** "Importa" devolviendo una ruta inventada, y anota qué le pidieron. */
    private class FakePhotoRepository : PhotoRepository {
        val imported = mutableListOf<String>()
        override suspend fun import(uri: String): String = "/fotos/$uri.jpg".also { imported += uri }
        override suspend fun saveCover(sourcePath: String, crop: CropRect): String = "/fotos/portada.jpg"
        override suspend fun delete(path: String) = Unit
    }

    private val repository = FakePhotoRepository()
    private val importPhotos = ImportPhotosUseCase(repository)

    /** URIs inventadas: "u0", "u1"… */
    private fun uris(count: Int) = List(count) { "u$it" }

    @Test
    fun `sin fotos previas importa hasta el máximo, las primeras elegidas`() = runBlocking {
        val paths = importPhotos(uris(MAX_PHOTOS + 2), alreadyCount = 0)

        assertEquals(uris(MAX_PHOTOS).map { "/fotos/$it.jpg" }, paths)
        assertEquals(uris(MAX_PHOTOS), repository.imported)
    }

    @Test
    fun `con fotos previas solo importa las que entran`() = runBlocking {
        assertEquals(listOf("/fotos/u0.jpg"), importPhotos(uris(2), alreadyCount = MAX_PHOTOS - 1))
    }

    @Test
    fun `con el máximo ya cubierto no importa nada`() = runBlocking {
        assertEquals(emptyList<String>(), importPhotos(uris(1), alreadyCount = MAX_PHOTOS))
        assertEquals(emptyList<String>(), repository.imported)
    }
}
