package com.example.pikaroo.ui.driver

import com.example.pikaroo.ui.driver.location.SimulatedRouteSource
import com.example.pikaroo.ui.orders.model.MapPoint
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedRouteSourceTest {

    private val shop = MapPoint(19.4326, -99.1332)
    private val destination = MapPoint(19.4180, -99.1450)

    @Test
    fun `a single leg is split into equal steps including both ends`() {
        val points = SimulatedRouteSource.interpolate(listOf(MapPoint(0.0, 0.0), MapPoint(4.0, 8.0)), stepsPerLeg = 4)

        assertEquals(
            listOf(MapPoint(0.0, 0.0), MapPoint(1.0, 2.0), MapPoint(2.0, 4.0), MapPoint(3.0, 6.0), MapPoint(4.0, 8.0)),
            points
        )
    }

    @Test
    fun `a route with several legs passes through every waypoint`() {
        val waypoints = listOf(MapPoint(0.0, 0.0), MapPoint(2.0, 2.0), MapPoint(2.0, 6.0))

        val points = SimulatedRouteSource.interpolate(waypoints, stepsPerLeg = 2)

        assertEquals(5, points.size)
        assertTrue(points.containsAll(waypoints))
        assertEquals(MapPoint(2.0, 4.0), points[3])
    }

    @Test
    fun `a route with fewer than two points is returned as it is`() {
        assertEquals(emptyList<MapPoint>(), SimulatedRouteSource.interpolate(emptyList(), 5))
        assertEquals(listOf(shop), SimulatedRouteSource.interpolate(listOf(shop), 5))
    }

    @Test
    fun `positions are emitted one per interval along the route`() = runTest {
        val source = SimulatedRouteSource(listOf(MapPoint(0.0, 0.0), MapPoint(2.0, 0.0)), stepsPerLeg = 2, intervalMillis = 1_000) { currentTime }

        val fixes = source.fixes().take(3).toList()

        assertEquals(listOf(0.0, 1.0, 2.0), fixes.map { it.lat })
        assertEquals(listOf(0L, 1_000L, 2_000L), fixes.map { it.atMillis })
    }

    @Test
    fun `at the end of the route the last position keeps being reported`() = runTest {
        val source = SimulatedRouteSource(listOf(MapPoint(0.0, 0.0), MapPoint(1.0, 1.0)), stepsPerLeg = 1, intervalMillis = 500)

        val fixes = source.fixes().take(5).toList()

        assertEquals(listOf(0.0, 1.0, 1.0, 1.0, 1.0), fixes.map { it.lat })
    }

    @Test
    fun `an empty route emits nothing`() = runTest {
        assertTrue(SimulatedRouteSource(emptyList()).fixes().toList().isEmpty())
    }

    @Test
    fun `heading to the shop the driver starts away from it and ends at the destination`() = runTest {
        val points = SimulatedRouteSource.forDelivery(shop, destination, headingToShop = true).fixes().take(25).toList()

        assertTrue(points.first().lat > shop.lat && points.first().lng > shop.lng)
        assertTrue(points.any { it.lat == shop.lat && it.lng == shop.lng })
        assertEquals(destination.lat, points.last().lat, 0.0000001)
        assertEquals(destination.lng, points.last().lng, 0.0000001)
    }

    @Test
    fun `once the order is picked up the driver starts at the shop`() = runTest {
        val first = SimulatedRouteSource.forDelivery(shop, destination, headingToShop = false).fixes().take(1).toList().single()

        assertEquals(shop.lat, first.lat, 0.0000001)
        assertEquals(shop.lng, first.lng, 0.0000001)
    }
}
