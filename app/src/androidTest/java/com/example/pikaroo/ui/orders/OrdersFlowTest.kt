package com.example.pikaroo.ui.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.appContainer
import com.example.pikaroo.common.mock.MockCollections.DRIVER_LOCATIONS
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.navigation.AppNavigation
import com.example.pikaroo.ui.navigation.NavTestTags
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.driver.data.ApiDriverRepository
import com.example.pikaroo.ui.driver.location.LocationReporter
import com.example.pikaroo.ui.driver.location.SimulatedRouteSource
import com.example.pikaroo.ui.orders.model.MapPoint
import com.example.pikaroo.ui.orders.view.OSM_MAP_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.pikaroo.ui.orders.view.OrdersTestTags
import com.example.pikaroo.ui.orders.view.TrackingTestTags
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OrdersFlowTest {

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
    }

    @After
    fun tearDown() {
        container.sessionRepository.clear()
        container.mockApi.reset()
        preferences.putBoolean("completed_onboarding", onboardingWasCompleted)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openMyOrdersAs(username: String) {
        runBlocking {
            container.sessionRepository.save(container.authApi.login(LoginRequest(username, "demo1234")))
        }
        container.sessionRepository.setActiveMode(RoleMode.CUSTOMER)
        composeRule.setContent { AppNavigation() }
        waitForTag(NavTestTags.CUSTOMER_HOME)
        composeRule.onNodeWithText("Usuario").performClick()
        composeRule.onNodeWithTag("user_orders").performScrollTo().performClick()
    }

    @Test
    fun customerSeesOnlyTheirOwnOrders() {
        openMyOrdersAs("customer1")

        waitForTag(OrdersTestTags.row("order_1004"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1010")).assertExists()
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1008")).assertExists()
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1002")).assertDoesNotExist()
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1009")).assertDoesNotExist()
    }

    @Test
    fun openingAPickupOrderShowsItsTimelineAndCode() {
        openMyOrdersAs("customer1")
        waitForTag(OrdersTestTags.row("order_1004"))

        composeRule.onNodeWithTag(OrdersTestTags.row("order_1004")).performClick()

        waitForTag(TrackingTestTags.step(OrderStatus.PREPARING, StepState.CURRENT))
        composeRule.onNodeWithTag(TrackingTestTags.TITLE).assertTextEquals("Pedido #1004")
        composeRule.onNodeWithTag(TrackingTestTags.step(OrderStatus.READY, StepState.PENDING)).assertExists()
        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertIsDisplayed().assertTextEquals("H6Z2LC")
    }

    @Test
    fun openingADeliveryOrderShowsTheDriverSteps() {
        openMyOrdersAs("owner1")
        waitForTag(OrdersTestTags.row("order_1009"))

        composeRule.onNodeWithTag(OrdersTestTags.row("order_1009")).performClick()

        waitForTag(TrackingTestTags.step(OrderStatus.OUT_FOR_DELIVERY, StepState.CURRENT))
        composeRule.onNodeWithTag(TrackingTestTags.step(OrderStatus.DRIVER_ASSIGNED, StepState.DONE)).assertExists()
        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertDoesNotExist()
    }

    @Test
    fun backFromTrackingReturnsToTheOrdersList() {
        openMyOrdersAs("customer1")
        waitForTag(OrdersTestTags.row("order_1004"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1004")).performClick()
        waitForTag(TrackingTestTags.TITLE)

        composeRule.onNodeWithContentDescription("Regresar").performClick()

        waitForTag(OrdersTestTags.row("order_1004"))
    }

    @Test
    fun switchingTabsAfterOpeningAnOrderDoesNotBringTheOrderScreenBack() {
        openMyOrdersAs("customer1")
        waitForTag(OrdersTestTags.row("order_1004"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1004")).performClick()
        waitForTag(TrackingTestTags.TITLE)

        composeRule.onNodeWithText("Inicio").performClick()
        composeRule.onNodeWithText("Usuario").performClick()

        composeRule.onNodeWithTag(TrackingTestTags.TITLE).assertDoesNotExist()
        composeRule.onNodeWithTag("user_orders").assertExists()
    }

    private fun mapDescription(): String? =
        composeRule.onNodeWithTag(OSM_MAP_TAG).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun waitForMap(expected: String) {
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(OSM_MAP_TAG).fetchSemanticsNodes().isNotEmpty() &&
                mapDescription()?.contains(expected) == true
        }
    }

    @Test
    fun theDriverPinMovesOnTheCustomersMapWhenTheDriverReportsANewPosition() {
        openMyOrdersAs("owner1")
        waitForTag(OrdersTestTags.row("order_1009"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1009")).performClick()

        waitForMap("driver=19.4230,-99.1420")

        runBlocking {
            container.mockApi.call {
                put(DRIVER_LOCATIONS, JsonObject().apply {
                    addProperty("id", "user_5")
                    addProperty("order_id", "order_1009")
                    addProperty("lat", 19.4201)
                    addProperty("lng", -99.1433)
                    addProperty("updated_at", "2026-10-20T15:07:00Z")
                })
            }
        }

        waitForMap("driver=19.4201,-99.1433")
    }

    @Test
    fun theMapAlsoShowsTheShopAndTheDeliveryAddress() {
        openMyOrdersAs("owner1")
        waitForTag(OrdersTestTags.row("order_1009"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1009")).performClick()

        waitForMap("shop=19.4326,-99.1332;destination=19.4180,-99.1450")
    }

    @Test
    fun aPickupOrderHasNoMap() {
        openMyOrdersAs("customer1")
        waitForTag(OrdersTestTags.row("order_1004"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1004")).performClick()
        waitForTag(TrackingTestTags.TITLE)

        composeRule.onNodeWithTag(OSM_MAP_TAG).assertDoesNotExist()
    }

    /** Un teléfono de repartidor aparte: su propia sesión, sin tocar la del cliente que usa la app. */
    private fun driverReporter(route: List<MapPoint>): LocationReporter = runBlocking {
        val store = object : com.example.pikaroo.common.preferences.StringStore {
            private val data = mutableMapOf<String, String>()
            override fun get(key: String) = data[key]
            override fun put(key: String, value: String) { data[key] = value }
            override fun remove(key: String) { data.remove(key) }
        }
        val driverSession = SessionRepository(store)
        driverSession.save(container.authApi.login(LoginRequest("driver1", "demo1234")))
        val repository = ApiDriverRepository(container.driverApi, AuthorizedCalls(driverSession, container.authApi))
        LocationReporter(repository, SimulatedRouteSource(route, stepsPerLeg = 6, intervalMillis = 600), minIntervalMillis = 600)
    }

    @Test
    fun theCustomersMapFollowsTheDriverAsTheyMove() {
        openMyOrdersAs("owner1")
        waitForTag(OrdersTestTags.row("order_1009"))
        composeRule.onNodeWithTag(OrdersTestTags.row("order_1009")).performClick()
        waitForMap("driver=19.4230,-99.1420")

        val reporter = driverReporter(listOf(MapPoint(19.4326, -99.1332), MapPoint(19.4180, -99.1450)))
        val job = CoroutineScope(Dispatchers.Default).launch { reporter.run("order_1009") }
        try {
            val seen = linkedSetOf<String>()
            composeRule.waitUntil(30_000) {
                mapDescription()?.substringAfter("driver=")?.let { seen += it }
                seen.size >= 4
            }
            // Se movió respecto a la posición inicial y fue acercándose al destino (la latitud baja de 19.4326 a 19.4180).
            val latitudes = seen.drop(1).map { it.substringBefore(",").toDouble() }
            assertTrue("positions: $seen", latitudes.size >= 3)
            assertTrue("positions: $seen", latitudes.zipWithNext().all { (a, b) -> b <= a })
            assertTrue("positions: $seen", latitudes.all { it in 19.4180..19.4326 })
        } finally {
            job.cancel()
        }
    }
}
