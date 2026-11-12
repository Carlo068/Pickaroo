package com.example.pikaroo.ui.driver

import com.example.pikaroo.common.mock.InMemoryStringStore
import com.example.pikaroo.ui.driver.location.DriverLocationPreferences
import com.example.pikaroo.ui.driver.location.DriverTracking
import com.example.pikaroo.ui.driver.location.GeoFix
import com.example.pikaroo.ui.driver.location.LocationMode
import com.example.pikaroo.ui.driver.location.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverTrackingTest {

    private val tracking = DriverTracking()
    private val fix = GeoFix(19.42, -99.14, atMillis = 1_000)

    @Test
    fun `nothing is being shared at first`() {
        val state = tracking.state.value

        assertFalse(state.isActive)
        assertNull(state.orderId)
        assertNull(state.lastFix)
        assertNull(state.stopReason)
    }

    @Test
    fun `starting marks the order as being shared`() {
        tracking.started("order_1")

        assertTrue(tracking.state.value.isActive)
        assertEquals("order_1", tracking.state.value.orderId)
    }

    @Test
    fun `positions update the last fix without changing the rest`() {
        tracking.started("order_1")

        tracking.fix(fix)

        assertEquals(fix, tracking.state.value.lastFix)
        assertTrue(tracking.state.value.isActive)
        assertEquals("order_1", tracking.state.value.orderId)
    }

    @Test
    fun `stopping records why and keeps the last position`() {
        tracking.started("order_1")
        tracking.fix(fix)

        tracking.stopped(StopReason.DELIVERY_ENDED)

        assertFalse(tracking.state.value.isActive)
        assertEquals(StopReason.DELIVERY_ENDED, tracking.state.value.stopReason)
        assertEquals(fix, tracking.state.value.lastFix)
    }

    @Test
    fun `a new start forgets the previous delivery`() {
        tracking.started("order_1")
        tracking.fix(fix)
        tracking.stopped(StopReason.SESSION_EXPIRED)

        tracking.started("order_2")

        assertEquals("order_2", tracking.state.value.orderId)
        assertNull(tracking.state.value.lastFix)
        assertNull(tracking.state.value.stopReason)
        assertTrue(tracking.state.value.isActive)
    }

    @Test
    fun `the real gps is the default location mode`() {
        assertEquals(LocationMode.GPS, DriverLocationPreferences(InMemoryStringStore()).mode.value)
    }

    @Test
    fun `the chosen location mode is remembered`() {
        val storage = InMemoryStringStore()
        val preferences = DriverLocationPreferences(storage)

        preferences.setMode(LocationMode.SIMULATED)

        assertEquals(LocationMode.SIMULATED, preferences.mode.value)
        assertEquals(LocationMode.SIMULATED, DriverLocationPreferences(storage).mode.value)
        preferences.setMode(LocationMode.GPS)
        assertEquals(LocationMode.GPS, DriverLocationPreferences(storage).mode.value)
    }

    @Test
    fun `an unknown stored mode falls back to the real gps`() {
        val storage = InMemoryStringStore().apply { put("driver_location_mode", "SATELLITE") }

        assertEquals(LocationMode.GPS, DriverLocationPreferences(storage).mode.value)
    }
}
