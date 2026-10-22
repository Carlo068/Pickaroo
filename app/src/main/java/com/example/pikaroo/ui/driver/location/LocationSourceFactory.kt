package com.example.pikaroo.ui.driver.location

import android.content.Context
import com.example.pikaroo.ui.driver.data.DriverRepository
import com.example.pikaroo.ui.orders.model.MapPoint
import com.example.pikaroo.ui.orders.model.OrderStatus
import kotlinx.coroutines.flow.emptyFlow

/** Elige la fuente de posiciones según el modo guardado: GPS real o ruta simulada de la entrega en curso. */
class LocationSourceFactory(
    private val context: Context,
    private val preferences: DriverLocationPreferences,
    private val repository: DriverRepository
) {
    suspend fun create(): LocationSource = when (preferences.mode.value) {
        LocationMode.GPS -> FusedLocationSource(context)
        LocationMode.SIMULATED -> simulatedForCurrentDelivery()
    }

    private suspend fun simulatedForCurrentDelivery(): LocationSource {
        val order = repository.current()
        val shop = order?.shop
        val destination = order?.delivery
        // Un domicilio escrito en el checkout puede no traer coordenadas: no hay ruta que simular.
        if (order == null || shop == null || destination?.lat == null || destination.lng == null) return NO_POSITIONS
        return SimulatedRouteSource.forDelivery(
            shop = MapPoint(shop.lat, shop.lng),
            destination = MapPoint(destination.lat, destination.lng),
            headingToShop = order.status == OrderStatus.DRIVER_ASSIGNED
        )
    }

    private companion object {
        val NO_POSITIONS = object : LocationSource {
            override fun fixes() = emptyFlow<GeoFix>()
        }
    }
}
