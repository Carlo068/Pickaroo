package com.example.pikaroo.ui.orders.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.pikaroo.ui.orders.model.MapPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

const val OSM_MAP_TAG = "osm_map"

/** Mapa de OpenStreetMap con la tienda, el destino y, si se conoce, el repartidor. */
@Composable
fun OsmMapView(
    shop: MapPoint?,
    destination: MapPoint?,
    driver: MapPoint?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            // Dentro de una pantalla con scroll, arrastrar sobre el mapa mueve el mapa.
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }
    val overlays = remember(map) { MapOverlays(context, map) }
    val fitState = remember(map) { FitState() }

    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }

    AndroidView(
        factory = { map },
        modifier = modifier
            .testTag(OSM_MAP_TAG)
            .semantics { stateDescription = describe(shop, destination, driver) },
        update = {
            overlays.update(shop, destination, driver)
            fitState.fitOnce(map, listOfNotNull(shop, destination, driver), key = listOf(shop, destination))
        }
    )
}

/** Texto con las posiciones actuales; sirve a las pruebas y a los lectores de pantalla. */
fun describe(shop: MapPoint?, destination: MapPoint?, driver: MapPoint?): String =
    "shop=${shop.text()};destination=${destination.text()};driver=${driver.text()}"

private fun MapPoint?.text() = this?.let { String.format(Locale.US, "%.4f,%.4f", it.lat, it.lng) } ?: "none"

private class FitState {
    private var lastKey: Any? = null

    /** Encuadra el mapa una sola vez por tienda/destino; el repartidor se mueve sin cambiar el zoom. */
    fun fitOnce(map: MapView, points: List<MapPoint>, key: Any) {
        if (points.isEmpty() || key == lastKey) return
        lastKey = key
        val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
        val fit = {
            if (geoPoints.size == 1) {
                map.controller.setCenter(geoPoints.first())
            } else {
                map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geoPoints), false, FIT_PADDING_PX)
            }
        }
        if (map.isLayoutOccurred) map.post(fit) else map.addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
    }

    private companion object {
        const val FIT_PADDING_PX = 120
    }
}

private class MapOverlays(context: Context, private val map: MapView) {

    private val route = Polyline(map).apply {
        outlinePaint.color = Color.parseColor("#9E9E9E")
        outlinePaint.strokeWidth = 8f
    }
    private val shopMarker = marker(context, "Tienda", Color.parseColor("#1E88E5"))
    private val destinationMarker = marker(context, "Destino", Color.parseColor("#43A047"))
    private val driverMarker = marker(context, "Repartidor", Color.parseColor("#F15A24"))

    init {
        map.overlays.addAll(listOf(route, shopMarker, destinationMarker, driverMarker))
    }

    fun update(shop: MapPoint?, destination: MapPoint?, driver: MapPoint?) {
        route.setPoints(listOfNotNull(shop, destination).map { GeoPoint(it.lat, it.lng) })
        route.isEnabled = shop != null && destination != null
        place(shopMarker, shop)
        place(destinationMarker, destination)
        place(driverMarker, driver)
        map.invalidate()
    }

    private fun place(marker: Marker, point: MapPoint?) {
        marker.isEnabled = point != null
        if (point != null) marker.position = GeoPoint(point.lat, point.lng)
    }

    private fun marker(context: Context, title: String, color: Int) = Marker(map).apply {
        this.title = title
        icon = dot(context, color)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        isEnabled = false
    }

    private fun dot(context: Context, color: Int): Drawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke((2 * density).toInt(), Color.WHITE)
            setSize((22 * density).toInt(), (22 * density).toInt())
        }
    }
}
