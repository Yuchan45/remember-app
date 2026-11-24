package com.example.uade.rememberapp.data.local.mapper

import com.example.uade.rememberapp.data.local.entity.PlaceEntity
import com.example.uade.rememberapp.domain.model.Place
import com.example.uade.rememberapp.domain.model.PlaceKind

// Traducción entre el modelo de lugar del dominio y su fila de Room.

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default

fun PlaceEntity.toDomain(): Place = Place(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    kind = enumOrDefault(kind, PlaceKind.Other),
)

fun Place.toEntity(): PlaceEntity = PlaceEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    kind = kind.name,
)
