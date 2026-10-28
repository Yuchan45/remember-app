package com.example.uade.rememberapp.ui.places.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.uade.rememberapp.R
import com.example.uade.rememberapp.domain.model.PlaceKind

/** Ícono de cada tipo de lugar. Vive en la UI porque el dominio no sabe de recursos. */
@DrawableRes
fun PlaceKind.iconRes(): Int = when (this) {
    PlaceKind.Home -> R.drawable.ic_home
    PlaceKind.Work -> R.drawable.ic_work
    PlaceKind.Study -> R.drawable.ic_school
    PlaceKind.Parking -> R.drawable.ic_local_parking
    PlaceKind.Other -> R.drawable.ic_location_on
}

/** Nombre legible de cada tipo de lugar. */
@StringRes
fun PlaceKind.labelRes(): Int = when (this) {
    PlaceKind.Home -> R.string.places_kind_home
    PlaceKind.Work -> R.string.places_kind_work
    PlaceKind.Study -> R.string.places_kind_study
    PlaceKind.Parking -> R.string.places_kind_parking
    PlaceKind.Other -> R.string.places_kind_other
}
