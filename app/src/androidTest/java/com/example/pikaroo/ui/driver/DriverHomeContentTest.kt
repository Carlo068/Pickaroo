package com.example.pikaroo.ui.driver

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.view.DriverHomeContent
import com.example.pikaroo.ui.driver.view.DriverTestTags
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeState
import com.example.pikaroo.ui.navigation.NavTestTags
import com.example.pikaroo.ui.orders.model.DeliveryDto
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.ShopSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DriverHomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shop = ShopSummaryDto("shop_2", "Panadería La Espiga", "Avenida Muestra 45", 19.4194, -99.1625)

    private fun available(id: String, fee: Double = 1.5) = AvailableOrderDto(
        id = id, shop = shop, delivery = DeliveryDto("Calle Falsa 9, Col. Roma Norte", 19.42, -99.16, null),
        deliveryFee = fee, itemsCount = 3, createdAt = "2026-10-20T15:10:00Z"
    )

    private val carrying = OrderDto(
        id = "order_1009", shopId = "shop_2", customerId = "user_3", fulfilment = Fulfilment.DELIVERY,
        status = OrderStatus.OUT_FOR_DELIVERY, items = emptyList(), subtotal = 0.0, deliveryFee = 1.5, total = 1.5,
        pickup = null, delivery = DeliveryDto("Avenida Demo 310", 19.418, -99.145, "user_5"),
        createdAt = "2026-10-20T14:50:00Z", shop = shop
    )

    private class Calls {
        val accepted = mutableListOf<String>()
        val online = mutableListOf<Boolean>()
        val switched = mutableListOf<RoleMode>()
        var logouts = 0
        var dismissed = 0
    }

    private fun show(state: DriverHomeState, modes: List<RoleMode> = emptyList(), calls: Calls = Calls()): Calls {
        composeRule.setContent {
            DriverHomeContent(
                state = state,
                userName = "Repartidor Demo",
                otherModes = modes,
                onSwitchMode = { calls.switched += it },
                onLogout = { calls.logouts++ },
                onOnlineChange = { calls.online += it },
                onAccept = { calls.accepted += it },
                onDismissMessage = { calls.dismissed++ }
            )
        }
        return calls
    }

    @Test
    fun availableOrdersShowTheRouteTheEarningsAndAnAcceptButton() {
        show(DriverHomeState(available = listOf(available("order_1010", fee = 1.5)), isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.available("order_1010")).assertExists()
        composeRule.onNodeWithText("Pedido #1010").assertExists()
        composeRule.onNodeWithText("Ganas \$1.50").assertExists()
        composeRule.onNodeWithText("Panadería La Espiga · Avenida Muestra 45").assertExists()
        composeRule.onNodeWithText("Calle Falsa 9, Col. Roma Norte").assertExists()
        composeRule.onNodeWithText("3 productos").assertExists()
        composeRule.onNodeWithTag(DriverTestTags.accept("order_1010")).assertIsEnabled()
    }

    @Test
    fun tappingAcceptReportsTheRightOrder() {
        val calls = show(DriverHomeState(available = listOf(available("order_1"), available("order_2")), isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.accept("order_2")).performScrollTo().performClick()

        assertEquals(listOf("order_2"), calls.accepted)
    }

    @Test
    fun whileAcceptingEveryButtonIsDisabledAndTheChosenOneSaysSo() {
        show(
            DriverHomeState(
                available = listOf(available("order_1"), available("order_2")),
                isLoading = false,
                acceptingId = "order_1"
            )
        )

        composeRule.onNodeWithTag(DriverTestTags.accept("order_1")).assertIsNotEnabled()
        composeRule.onNodeWithTag(DriverTestTags.accept("order_2")).assertIsNotEnabled()
        composeRule.onNodeWithText("Aceptando…").assertExists()
    }

    @Test
    fun anEmptyListSaysThereAreNoOrders() {
        show(DriverHomeState(isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.EMPTY).assertExists()
    }

    @Test
    fun whileLoadingNeitherTheListNorTheEmptyMessageShows() {
        show(DriverHomeState(isLoading = true))

        composeRule.onNodeWithTag(DriverTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun offlineDriversAreToldToTurnAvailabilityOn() {
        show(DriverHomeState(isOnline = false, isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.OFFLINE).assertExists()
        composeRule.onNodeWithTag(DriverTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun theAvailabilitySwitchReportsItsNewValue() {
        val calls = show(DriverHomeState(isOnline = true, isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.ONLINE_SWITCH).performClick()

        assertEquals(listOf(false), calls.online)
    }

    @Test
    fun aDeliveryInProgressHidesTheAvailableOrders() {
        show(DriverHomeState(current = carrying, available = listOf(available("order_1010")), isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.current("order_1009")).assertExists()
        composeRule.onNodeWithText("Pedido #1009").assertExists()
        composeRule.onNodeWithText("En camino").assertExists()
        composeRule.onNodeWithText("Avenida Demo 310").assertExists()
        composeRule.onNodeWithTag(DriverTestTags.available("order_1010")).assertDoesNotExist()
    }

    @Test
    fun aMessageIsShownAndCanBeDismissed() {
        val calls = show(DriverHomeState(message = "Otro repartidor ya tomó este pedido", isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.MESSAGE).assertTextContains("Otro repartidor", substring = true)
        composeRule.onNodeWithTag(DriverTestTags.MESSAGE_DISMISS).performClick()

        assertEquals(1, calls.dismissed)
    }

    @Test
    fun aLoadingErrorIsShownWithoutHidingTheList() {
        show(DriverHomeState(available = listOf(available("order_1")), error = "Sin conexión. Reintentando…", isLoading = false))

        composeRule.onNodeWithTag(DriverTestTags.ERROR).assertExists()
        composeRule.onNodeWithTag(DriverTestTags.available("order_1")).assertExists()
    }

    @Test
    fun modeSwitchAndLogoutAreAvailableAtTheBottom() {
        val calls = show(DriverHomeState(isLoading = false), modes = listOf(RoleMode.CUSTOMER))

        composeRule.onNodeWithTag(NavTestTags.switchTo(RoleMode.CUSTOMER)).performScrollTo().performClick()
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()

        assertEquals(listOf(RoleMode.CUSTOMER), calls.switched)
        assertEquals(1, calls.logouts)
    }
}
