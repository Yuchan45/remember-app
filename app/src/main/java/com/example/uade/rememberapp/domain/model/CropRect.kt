package com.example.uade.rememberapp.domain.model

/**
 * Una porción de una imagen, en proporciones de 0 a 1 (0,0 = arriba a la izquierda; 1,1 = abajo
 * a la derecha). Va así y no en píxeles para no depender del tamaño con que se mostró la foto:
 * quien recorta el archivo la multiplica por su ancho y alto reales.
 */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "El recorte va en proporciones de 0 a 1"
        }
        require(left < right && top < bottom) { "El recorte no puede estar vacío" }
    }
}
