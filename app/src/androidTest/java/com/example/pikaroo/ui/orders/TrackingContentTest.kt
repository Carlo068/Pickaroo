package com.example.pikaroo.ui.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.pikaroo.ui.orders.model.DeliveryDto
import com.example.pikaroo.ui.orders.model.DriverLocationDto
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
import com.example.pikaroo.ui.orders.model.OrderLineDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.OrderStatus.ACCEPTED
import com.example.pikaroo.ui.orders.model.OrderStatus.CANCELLED
import com.example.pikaroo.ui.orders.model.OrderStatus.DELIVERED
import com.example.pikaroo.ui.orders.model.OrderStatus.DRIVER_ASSIGNED
import com.example.pikaroo.ui.orders.model.OrderStatus.OUT_FOR_DELIVERY
import com.example.pikaroo.ui.orders.model.OrderStatus.PICKED_UP
import com.example.pikaroo.ui.orders.model.OrderStatus.PLACED
import com.example.pikaroo.ui.orders.model.OrderStatus.PREPARING
import com.example.pikaroo.ui.orders.model.OrderStatus.READY
import com.example.pikaroo.ui.orders.model.PickupDto
import com.example.pikaroo.ui.orders.model.ShopSummaryDto
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.orders.model.buildTimeline
import com.example.pikaroo.ui.orders.view.OSM_MAP_TAG
import com.example.pikaroo.ui.orders.view.TrackingContent
import com.example.pikaroo.ui.orders.view.TrackingTestTags
import com.example.pikaroo.ui.orders.viewmodel.TrackingUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrackingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val line = OrderLineDto("1", "Manzanas Rojas", 2, 3.49, 10, 6.28)

    private fun pickupOrder(status: OrderStatus) = OrderDto(
        id = "order_1003", shopId = "shop_1", customerId = "user_1", fulfilment = Fulfilment.PICKUP, status = status,
        items = listOf(line), subtotal = 6.28, deliveryFee = 0.0, total = 6.28,
        pickup = PickupDto("2026-10-20T16:30:00Z", "W3N8RB"), delivery = null, createdAt = "2026-10-20T14:10:00Z"
    )

    private fun deliveryOrder(status: OrderStatus) = OrderDto(
        id = "order_1009", shopId = "shop_1", customerId = "user_1", fulfilment = Fulfilment.DELIVERY, status = status,
        items = listOf(line), subtotal = 6.28, deliveryFee = 2.99, total = 9.27,
        pickup = null, delivery = DeliveryDto("Avenida Demo 310", 19.418, -99.145, "user_5"), createdAt = "2026-10-20T14:50:00Z",
        shop = ShopSummaryDto("shop_1", "Pickaroo Centro", "Calle Ejemplo 120", 19.4326, -99.1332)
    )

    private fun events(vararg statuses: OrderStatus) = statuses.mapIndexed { i, s ->
        OrderEventDto(s, "2026-10-20T14:0$i:00Z", "user_1")
    }

    private fun show(
        order: OrderDto,
        events: List<OrderEventDto>,
        error: String? = null,
        driverLocation: DriverLocationDto? = null,
        onBack: () -> Unit = {}
    ) {
        composeRule.setContent {
            TrackingContent(
                state = TrackingUiState(order, events, buildTimeline(order, events), driverLocation, isLoading = false, error = error),
                onBack = onBack
            )
        }
    }

    private fun assertStep(status: OrderStatus, state: StepState) {
        composeRule.onNodeWithTag(TrackingTestTags.step(status, state)).assertExists()
    }

    @Test
    fun pickupTimelineMarksDoneCurrentAndPendingSteps() {
        show(pickupOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY))

        assertStep(PLACED, StepState.DONE)
        assertStep(ACCEPTED, StepState.DONE)
        assertStep(PREPARING, StepState.DONE)
        assertStep(READY, StepState.CURRENT)
        assertStep(PICKED_UP, StepState.PENDING)
        composeRule.onNodeWithText("Listo para recoger").assertExists()
        composeRule.onNodeWithTag(TrackingTestTags.TITLE).assertTextEquals("Pedido #1003")
    }

    @Test
    fun pickupOrderShowsTheCodeToCollectIt() {
        show(pickupOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY))

        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertIsDisplayed().assertTextEquals("W3N8RB")
    }

    @Test
    fun theCodeDisappearsOnceTheOrderWasCollected() {
        show(pickupOrder(PICKED_UP), events(PLACED, ACCEPTED, PREPARING, READY, PICKED_UP))

        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertDoesNotExist()
        assertStep(PICKED_UP, StepState.DONE)
    }

    @Test
    fun deliveryTimelineHasDriverSteps() {
        show(
            deliveryOrder(OUT_FOR_DELIVERY),
            events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY)
        )

        assertStep(DRIVER_ASSIGNED, StepState.DONE)
        assertStep(OUT_FOR_DELIVERY, StepState.CURRENT)
        assertStep(DELIVERED, StepState.PENDING)
        composeRule.onNodeWithText("En camino").assertExists()
        composeRule.onNodeWithText("Entrega a domicilio").assertExists()
    }

    @Test
    fun deliveryOrderShowsNoPickupCodeButShowsTheFeeAndAddress() {
        show(deliveryOrder(OUT_FOR_DELIVERY), events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY))

        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertDoesNotExist()
        composeRule.onNodeWithText("Envío").assertExists()
        composeRule.onNodeWithText("\$2.99").assertExists()
        composeRule.onNodeWithText("Avenida Demo 310").assertExists()
    }

    @Test
    fun deliveredOrderHasEveryStepDone() {
        show(
            deliveryOrder(DELIVERED),
            events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED)
        )

        assertStep(DELIVERED, StepState.DONE)
        assertStep(READY, StepState.DONE)
    }

    @Test
    fun cancelledOrderShowsTheCancellationAndNoPendingSteps() {
        show(pickupOrder(CANCELLED), events(PLACED, CANCELLED))

        assertStep(PLACED, StepState.DONE)
        assertStep(CANCELLED, StepState.CANCELLED)
        composeRule.onNodeWithTag(TrackingTestTags.step(READY, StepState.PENDING)).assertDoesNotExist()
        composeRule.onNodeWithTag(TrackingTestTags.PICKUP_CODE).assertDoesNotExist()
    }

    @Test
    fun summaryListsTheItemsAndTheTotal() {
        show(pickupOrder(PREPARING), events(PLACED, ACCEPTED, PREPARING))

        composeRule.onNodeWithText("2 × Manzanas Rojas").assertExists()
        composeRule.onNodeWithText("Total").assertExists()
    }

    @Test
    fun aTemporaryErrorKeepsTheOrderOnScreen() {
        show(pickupOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY), error = "Sin conexión. Reintentando…")

        composeRule.onNodeWithTag(TrackingTestTags.ERROR).assertTextContains("Sin conexión", substring = true)
        assertStep(READY, StepState.CURRENT)
    }

    @Test
    fun withoutAnOrderTheErrorIsShownAlone() {
        composeRule.setContent {
            TrackingContent(TrackingUiState(isLoading = false, error = "No se encontró el pedido"), onBack = {})
        }

        composeRule.onNodeWithTag(TrackingTestTags.ERROR).assertTextEquals("No se encontró el pedido")
        composeRule.onNodeWithTag(TrackingTestTags.TITLE).assertTextEquals("Pedido")
    }

    @Test
    fun backButtonIsWired() {
        var backs = 0
        show(pickupOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY), onBack = { backs++ })

        composeRule.onNodeWithContentDescription("Regresar").performClick()

        assertEquals(1, backs)
    }

    private val onTheWay = arrayOf(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY)

    @Test
    fun theLiveMapShowsWhileTheDriverIsOnTheWay() {
        show(deliveryOrder(OUT_FOR_DELIVERY), events(*onTheWay), driverLocation = DriverLocationDto(19.423, -99.142, "2026-10-20T15:05:00Z"))

        composeRule.onNodeWithTag(OSM_MAP_TAG).assertExists()
        composeRule.onNodeWithText("Tienda").assertExists()
        composeRule.onNodeWithText("Destino").assertExists()
        composeRule.onNodeWithText("Repartidor").assertExists()
        composeRule.onNodeWithText("Ubicación actualizada", substring = true).assertExists()
    }

    @Test
    fun theMapExplainsWhenTheDriverHasNotReportedALocation() {
        show(deliveryOrder(DRIVER_ASSIGNED), events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED))

        composeRule.onNodeWithTag(OSM_MAP_TAG).assertExists()
        composeRule.onNodeWithText("Esperando la ubicación del repartidor…").assertExists()
    }

    @Test
    fun noMapForPickupOrdersOrDeliveriesWithoutADriverYetOrFinishedOnes() {
        show(pickupOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY))
        composeRule.onNodeWithTag(OSM_MAP_TAG).assertDoesNotExist()
    }

    @Test
    fun noMapWhileTheDeliveryIsStillWaitingForADriver() {
        show(deliveryOrder(READY), events(PLACED, ACCEPTED, PREPARING, READY))

        composeRule.onNodeWithTag(OSM_MAP_TAG).assertDoesNotExist()
    }

    @Test
    fun noMapOnceTheOrderWasDelivered() {
        show(deliveryOrder(DELIVERED), events(*onTheWay, DELIVERED))

        composeRule.onNodeWithTag(OSM_MAP_TAG).assertDoesNotExist()
    }
}
