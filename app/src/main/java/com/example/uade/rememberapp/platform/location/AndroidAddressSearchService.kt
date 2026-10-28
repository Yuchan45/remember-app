package com.example.uade.rememberapp.platform.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.uade.rememberapp.domain.model.AddressSuggestion
import com.example.uade.rememberapp.domain.repository.AddressSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Implementación de [AddressSearchService] usando el [Geocoder] y [LocationManager] del sistema.
 */
class AndroidAddressSearchService(
    private val context: Context,
) : AddressSearchService {

    private val geocoder: Geocoder by lazy { Geocoder(context, Locale.getDefault()) }

    override suspend fun search(query: String): List<AddressSuggestion> {
        val clean = query.trim()
        if (clean.isBlank() || !Geocoder.isPresent()) return emptyList()

        val addresses = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<List<Address>> { continuation ->
                        geocoder.getFromLocationName(
                            clean,
                            5,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    continuation.resume(addresses)
                                }

                                override fun onError(errorMessage: String?) {
                                    continuation.resume(emptyList())
                                }
                            },
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(clean, 5).orEmpty()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        return addresses.mapNotNull { it.toSuggestion() }
    }

    override suspend fun getCurrentLocation(): AddressSuggestion? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val location: Location? = try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: SecurityException) {
            null
        }

        if (location == null) return null

        val addressName = reverseGeocode(location.latitude, location.longitude)
        return AddressSuggestion(
            addressLine = addressName ?: "${location.latitude}, ${location.longitude}",
            latitude = location.latitude,
            longitude = location.longitude,
            featureName = addressName,
        )
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(
                            latitude,
                            longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                                }

                                override fun onError(errorMessage: String?) {
                                    continuation.resume(null)
                                }
                            },
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun Address.toSuggestion(): AddressSuggestion? {
        val line = getAddressLine(0) ?: featureName ?: locality ?: return null
        return AddressSuggestion(
            addressLine = line,
            latitude = latitude,
            longitude = longitude,
            featureName = featureName ?: locality,
        )
    }
}
