package com.example.pikaroo.ui.driver

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.driver.location.DeliveryTrackingService
import com.example.pikaroo.ui.driver.location.LocationMode
import com.example.pikaroo.ui.driver.location.StopReason
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverTrackingServiceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val container = context.appContainer()

    @Before
    fun setUp() {
        // Conceder un permiso en tiempo de ejecución no reinicia la app; quitarlo sí, por eso no se revoca.
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        container.sessionRepository.clear()
        container.mockApi.reset()
        container.driverLocationPreferences.setMode(LocationMode.SIMULATED)
        runBlocking {
            container.sessionRepository.save(container.authApi.login(LoginRequest("driver1", "demo1234")))
        }
    }

    @After
    fun tearDown() {
        DeliveryTrackingService.stop(context)
        container.driverLocationPreferences.setMode(LocationMode.GPS)
        container.sessionRepository.clear()
        container.mockApi.reset()
    }

    private fun waitUntil(timeoutMillis: Long = 20_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("Condition not met in $timeoutMillis ms")
            Thread.sleep(100)
        }
    }

    private fun customerSeesDriverAt(orderId: String) = runBlocking {
        val bearer = "Bearer " + container.authApi.login(LoginRequest("owner1", "demo1234")).access
        runCatching { container.ordersApi.driverLocation(bearer, orderId) }.getOrNull()
    }

    @Test
    fun theServiceStartsSharingAndThePositionsReachTheCustomer() {
        DeliveryTrackingService.start(context, "order_1009")

        waitUntil { container.driverTracking.state.value.isActive }
        waitUntil { container.driverTracking.state.value.lastFix != null }
        val seeded = 19.423
        waitUntil { customerSeesDriverAt("order_1009")?.lat != seeded }

        val seen = customerSeesDriverAt("order_1009")!!
        assertTrue("the position should be on the way from the shop to the delivery address", seen.lat in 19.4180..19.4330)
        assertEquals("order_1009", container.driverTracking.state.value.orderId)
    }

    @Test
    fun theReportedPositionKeepsMovingAlongTheRoute() {
        DeliveryTrackingService.start(context, "order_1009")
        waitUntil { container.driverTracking.state.value.lastFix != null }

        val first = container.driverTracking.state.value.lastFix!!
        waitUntil { container.driverTracking.state.value.lastFix != first }

        assertNotEquals(first, container.driverTracking.state.value.lastFix)
    }

    @Test
    fun theServiceStopsByItselfWhenTheDeliveryIsDelivered() {
        DeliveryTrackingService.start(context, "order_1009")
        waitUntil { container.driverTracking.state.value.isActive }

        runBlocking {
            val bearer = "Bearer " + container.authApi.login(LoginRequest("driver1", "demo1234")).access
            container.driverApi.deliver(bearer, "order_1009")
        }

        waitUntil { !container.driverTracking.state.value.isActive }
        assertEquals(StopReason.DELIVERY_ENDED, container.driverTracking.state.value.stopReason)
    }

    @Test
    fun theServiceStopsWhenItIsToldTo() {
        DeliveryTrackingService.start(context, "order_1009")
        waitUntil { container.driverTracking.state.value.isActive }

        DeliveryTrackingService.stop(context)

        waitUntil { !container.driverTracking.state.value.isActive }
    }

    @Test
    fun anOrderThatIsNotTheDriversEndsTheSharingRightAway() {
        DeliveryTrackingService.start(context, "order_1010")

        waitUntil { container.driverTracking.state.value.stopReason == StopReason.DELIVERY_ENDED }
        assertTrue(!container.driverTracking.state.value.isActive)
        val stored = runBlocking { container.mockApi.call { find(ORDERS, "order_1010")!!.get("status").asString } }
        assertEquals("READY", stored)
    }
}
