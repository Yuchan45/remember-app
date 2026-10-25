package com.example.uade.rememberapp.ui.places.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.uade.rememberapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

/**
 * Fuente de mapa basada en OpenStreetMap (con espejos oficiales de alta disponibilidad).
 * 100% libre, sin requerimiento de API Key ni marcas de agua.
 */
private val OsmMapTileSource = XYTileSource(
    "OsmDarkMap",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://tile.openstreetmap.org/",
        "https://tile.openstreetmap.de/",
    ),
    "© OpenStreetMap contributors",
)

/**
 * Vista previa de mapa con OpenStreetMap para visualizar la ubicación
 * seleccionada, el marcador y el radio de geofence configurado.
 *
 * Utiliza un filtro de color oscuro para integrarse a la paleta nocturna de la app
 * y actualiza de forma reactiva el radio de geofence y las coordenadas.
 */
@Composable
fun PlaceMapPreview(
    latitude: Double,
    longitude: Double,
    radiusMeters: Int,
    modifier: Modifier = Modifier,
    address: String? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(12.dp)

    // Zoom según el tamaño del radio para mantener el círculo visible
    val targetZoom = when {
        radiusMeters <= 100 -> 17.5
        radiusMeters <= 150 -> 17.0
        radiusMeters <= 300 -> 16.2
        else -> 15.4
    }

    val mapView = remember {
        // Aseguramos la inicialización del User-Agent aún en live-edit o recargas en caliente
        val prefs = context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        val osmConfig = Configuration.getInstance()
        osmConfig.load(context, prefs)
        osmConfig.userAgentValue = "HeyRememberApp/1.0 (Android; org.uade.rememberapp)"
        osmConfig.save(context, prefs)

        MapView(context).apply {
            setTileSource(OsmMapTileSource)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true

            // Filtro oscuro para adaptar los tiles claros de OSM a la paleta nocturna de la app
            overlayManager.tilesOverlay.setColorFilter(createDarkColorFilter())

            // Evitar que el scroll del bottom sheet intercepte los gestos dentro del mapa
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = shape,
            ),
    ) {
        AndroidView(
            factory = { mapView },
            update = { map ->
                val geoPoint = GeoPoint(latitude, longitude)
                map.controller.setZoom(targetZoom)
                map.controller.setCenter(geoPoint)

                // Limpiar overlays anteriores (círculos y marcadores previos)
                map.overlays.clear()

                // Círculo del radio de aviso (geofence)
                val circle = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(geoPoint, radiusMeters.toDouble())
                    fillPaint.color = primaryColor.copy(alpha = 0.22f).toArgb()
                    outlinePaint.color = primaryColor.toArgb()
                    outlinePaint.strokeWidth = 4f
                }
                map.overlays.add(circle)

                // Marcador en el centro
                val marker = Marker(map).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = createMarkerIcon(context, R.drawable.ic_location_on, primaryColor.toArgb())
                    infoWindow = null
                }
                map.overlays.add(marker)

                map.invalidate()
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Tarjeta flotante con la dirección legible sobre el mapa
        if (!address.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .fillMaxWidth(0.95f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_on),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Filtro matricial que invierte el brillo y desatura los colores de los tiles OSM
 * para mimetizar un mapa en Dark Mode de alta fidelidad.
 */
private fun createDarkColorFilter(): ColorFilter {
    val inverseMatrix = ColorMatrix(
        floatArrayOf(
            -0.85f, 0f, 0f, 0f, 255f,
            0f, -0.85f, 0f, 0f, 255f,
            0f, 0f, -0.85f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    val desaturateMatrix = ColorMatrix().apply { setSaturation(0.25f) }
    inverseMatrix.postConcat(desaturateMatrix)
    return ColorMatrixColorFilter(inverseMatrix)
}

/**
 * Convierte un vector drawable en un BitmapDrawable con color primario
 * compatible con los overlays de OSMDroid.
 */
private fun createMarkerIcon(
    context: Context,
    @DrawableRes drawableRes: Int,
    tintColor: Int,
): BitmapDrawable {
    val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate()
        ?: return BitmapDrawable(context.resources, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    drawable.setTint(tintColor)
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}
