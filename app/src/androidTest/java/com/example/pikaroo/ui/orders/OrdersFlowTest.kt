package com.example.pikaroo.ui.orders

import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.navigation.AppNavigation
import com.example.pikaroo.ui.navigation.NavTestTags
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.orders.view.OrdersTestTags
import com.example.pikaroo.ui.orders.view.TrackingTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
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
        preferences.putBoolean("completed_onboarding", onboardingWasCompleted)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun openMyOrdersAs(username: String) {
        runBlocking {
            container.sessionRepository.save(container.authApi.login(LoginRequest(username, "demo1234")))
        }
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
}
