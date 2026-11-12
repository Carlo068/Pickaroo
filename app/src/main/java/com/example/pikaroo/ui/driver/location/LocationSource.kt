package com.example.pikaroo.ui.driver.location

import com.example.pikaroo.ui.orders.model.MapPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GeoFix(val lat: Double, val lng: Double, val atMillis: Long)

fun GeoFix.toMapPoint() = MapPoint(lat, lng)

/** De dónde salen las posiciones del repartidor: el GPS real o una ruta simulada para demos y pruebas. */
interface LocationSource {
    /** Emite posiciones hasta que quien recoge deja de recogerlas. Sin permiso lanza SecurityException. */
    fun fixes(): Flow<GeoFix>
}

/** Recorre una ruta paso a paso y, al llegar al final, sigue reportando el último punto. */
class SimulatedRouteSource(
    private val waypoints: List<MapPoint>,
    private val stepsPerLeg: Int = 12,
    private val intervalMillis: Long = 2_000,
    private val now: () -> Long = System::currentTimeMillis
) : LocationSource {

    override fun fixes(): Flow<GeoFix> = flow {
        val path = interpolate(waypoints, stepsPerLeg)
        if (path.isEmpty()) return@flow
        for (point in path) {
            emit(GeoFix(point.lat, point.lng, now()))
            delay(intervalMillis)
        }
        while (true) {
            emit(GeoFix(path.last().lat, path.last().lng, now()))
            delay(intervalMillis)
        }
    }

    companion object {
        /** Puntos intermedios entre cada par de puntos de la ruta, incluidos el primero y el último. */
        fun interpolate(waypoints: List<MapPoint>, stepsPerLeg: Int): List<MapPoint> {
            if (waypoints.size < 2) return waypoints
            val points = mutableListOf<MapPoint>()
            for (leg in 0 until waypoints.size - 1) {
                val from = waypoints[leg]
                val to = waypoints[leg + 1]
                for (step in 0 until stepsPerLeg) {
                    val t = step.toDouble() / stepsPerLeg
                    points += MapPoint(from.lat + (to.lat - from.lat) * t, from.lng + (to.lng - from.lng) * t)
                }
            }
            points += waypoints.last()
            return points
        }

        /** Antes de recoger sale de unos 600 m de la tienda; después va directo de la tienda al destino. */
        fun forDelivery(shop: MapPoint, destination: MapPoint, headingToShop: Boolean): SimulatedRouteSource {
            val start = MapPoint(shop.lat + START_OFFSET_DEGREES, shop.lng + START_OFFSET_DEGREES)
            val route = if (headingToShop) listOf(start, shop, destination) else listOf(shop, destination)
            return SimulatedRouteSource(route)
        }

        private const val START_OFFSET_DEGREES = 0.005
    }
}
