package com.example.pikaroo.ui.driver

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockCollections.ORDERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.driver.view.DriverTestTags
import com.example.pikaroo.ui.navigation.AppNavigation
import com.example.pikaroo.ui.navigation.NavTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DriverFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container = context.appContainer()
    private val preferences = AppPreferences(context)
    private var onboardingWasCompleted = false

    @Before
    fun setUp() {
        onboardingWasCompleted = preferences.getBoolean("completed_onboarding")
        preferences.putBoolean("completed_onboarding", true)
        container.sessionRepository.clear()
        container.mockApi.reset()
    }

    @After
    fun tearDown() {
        container.sessionRepository.clear()
        container.mockApi.reset()
        preferences.putBoolean("completed_onboarding", onboardingWasCompleted)
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForNoTag(tag: String) {
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
    }

    private fun openDriverHome() {
        runBlocking {
            container.sessionRepository.save(container.authApi.login(LoginRequest("driver1", "demo1234")))
        }
        composeRule.setContent { AppNavigation() }
        waitForTag(NavTestTags.DRIVER_HOME)
    }

    private fun finishCurrentDelivery() = runBlocking {
        container.mockApi.call {
            val order = find(ORDERS, "order_1009")!!
            order.addProperty("status", "DELIVERED")
            put(ORDERS, order)
        }
    }

    private fun statusOf(orderId: String) = runBlocking {
        container.mockApi.call { find(ORDERS, orderId)!!.get("status").asString }
    }

    @Test
    fun aDriverWithADeliveryInProgressSeesItAndNoList() {
        openDriverHome()

        waitForTag(DriverTestTags.current("order_1009"))
        composeRule.onNodeWithTag(DriverTestTags.available("order_1010")).assertDoesNotExist()
    }

    @Test
    fun onceTheDeliveryIsFinishedTheReadyOrderAppears() {
        openDriverHome()
        waitForTag(DriverTestTags.current("order_1009"))

        finishCurrentDelivery()

        waitForTag(DriverTestTags.available("order_1010"))
        composeRule.onNodeWithTag(DriverTestTags.current("order_1009")).assertDoesNotExist()
    }

    @Test
    fun acceptingAnOrderMakesItTheCurrentDeliveryAndTheCustomerSeesIt() {
        finishCurrentDelivery()
        openDriverHome()
        waitForTag(DriverTestTags.accept("order_1010"))

        composeRule.onNodeWithTag(DriverTestTags.accept("order_1010")).performClick()

        waitForTag(DriverTestTags.current("order_1010"))
        composeRule.onNodeWithTag(DriverTestTags.available("order_1010")).assertDoesNotExist()
        assertEquals("DRIVER_ASSIGNED", statusOf("order_1010"))
    }

    @Test
    fun anOrderTakenByAnotherDriverShowsAMessage() {
        finishCurrentDelivery()
        openDriverHome()
        waitForTag(DriverTestTags.accept("order_1010"))
        container.mockApi.failNext { MockErrors.conflict("ORDER_NOT_AVAILABLE") }

        composeRule.onNodeWithTag(DriverTestTags.accept("order_1010")).performClick()

        waitForTag(DriverTestTags.MESSAGE)
        composeRule.onNodeWithTag(DriverTestTags.MESSAGE).assertTextContains("Otro repartidor ya tomó este pedido", substring = true)
        composeRule.onNodeWithTag(DriverTestTags.MESSAGE_DISMISS).performClick()
        waitForNoTag(DriverTestTags.MESSAGE)
        assertEquals("READY", statusOf("order_1010"))
    }

    @Test
    fun goingOfflineHidesTheOrders() {
        finishCurrentDelivery()
        openDriverHome()
        waitForTag(DriverTestTags.available("order_1010"))

        composeRule.onNodeWithTag(DriverTestTags.ONLINE_SWITCH).performClick()

        waitForTag(DriverTestTags.OFFLINE)
        composeRule.onNodeWithTag(DriverTestTags.available("order_1010")).assertDoesNotExist()
    }

    @Test
    fun aDriverWhoAlreadyAcceptedSeesTheDeliveryAfterRelaunching() {
        finishCurrentDelivery()
        runBlocking {
            val bearer = "Bearer " + container.authApi.login(LoginRequest("driver1", "demo1234")).access
            container.driverApi.accept(bearer, "order_1010")
        }

        openDriverHome()

        waitForTag(DriverTestTags.current("order_1010"))
    }
}
