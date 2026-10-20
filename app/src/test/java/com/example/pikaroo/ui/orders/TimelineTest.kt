package com.example.pikaroo.ui.orders

import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderEventDto
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
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.orders.model.buildTimeline
import com.example.pikaroo.ui.orders.model.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {

    private fun order(status: OrderStatus, fulfilment: Fulfilment) = OrderDto(
        id = "order_1", shopId = "shop_1", customerId = "user_1", fulfilment = fulfilment, status = status,
        items = emptyList(), subtotal = 0.0, deliveryFee = 0.0, total = 0.0, pickup = null, delivery = null,
        createdAt = "2026-10-20T14:00:00Z"
    )

    private fun events(vararg statuses: OrderStatus) = statuses.mapIndexed { index, status ->
        OrderEventDto(status, "2026-10-20T14:0$index:00Z", "user_1")
    }

    private fun states(steps: List<com.example.pikaroo.ui.orders.model.TimelineStep>) =
        steps.map { it.status to it.state }

    @Test
    fun `pickup order shows the pickup path with the current step marked`() {
        val steps = buildTimeline(order(READY, Fulfilment.PICKUP), events(PLACED, ACCEPTED, PREPARING, READY))

        assertEquals(
            listOf(
                PLACED to StepState.DONE,
                ACCEPTED to StepState.DONE,
                PREPARING to StepState.DONE,
                READY to StepState.CURRENT,
                PICKED_UP to StepState.PENDING
            ),
            states(steps)
        )
    }

    @Test
    fun `delivery order shows the delivery path`() {
        val steps = buildTimeline(
            order(OUT_FOR_DELIVERY, Fulfilment.DELIVERY),
            events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY)
        )

        assertEquals(
            listOf(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED),
            steps.map { it.status }
        )
        assertEquals(StepState.CURRENT, steps.first { it.status == OUT_FOR_DELIVERY }.state)
        assertEquals(StepState.PENDING, steps.first { it.status == DELIVERED }.state)
    }

    @Test
    fun `a new order starts at the first step`() {
        val steps = buildTimeline(order(PLACED, Fulfilment.PICKUP), events(PLACED))

        assertEquals(StepState.CURRENT, steps.first().state)
        assertTrue(steps.drop(1).all { it.state == StepState.PENDING })
    }

    @Test
    fun `finished orders have every step done and no current step`() {
        val picked = buildTimeline(order(PICKED_UP, Fulfilment.PICKUP), events(PLACED, ACCEPTED, PREPARING, READY, PICKED_UP))
        val delivered = buildTimeline(
            order(DELIVERED, Fulfilment.DELIVERY),
            events(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED)
        )

        assertTrue(picked.all { it.state == StepState.DONE })
        assertTrue(delivered.all { it.state == StepState.DONE })
    }

    @Test
    fun `a cancelled order shows what it reached and the cancellation`() {
        val steps = buildTimeline(order(CANCELLED, Fulfilment.PICKUP), events(PLACED, ACCEPTED, CANCELLED))

        assertEquals(
            listOf(PLACED to StepState.DONE, ACCEPTED to StepState.DONE, CANCELLED to StepState.CANCELLED),
            states(steps)
        )
    }

    @Test
    fun `steps carry the time of their event and pending steps have none`() {
        val steps = buildTimeline(order(PREPARING, Fulfilment.PICKUP), events(PLACED, ACCEPTED, PREPARING))

        assertEquals("2026-10-20T14:00:00Z", steps.first { it.status == PLACED }.at)
        assertEquals("2026-10-20T14:02:00Z", steps.first { it.status == PREPARING }.at)
        assertNull(steps.first { it.status == READY }.at)
    }

    @Test
    fun `a status outside the path of its fulfilment leaves every step pending`() {
        val steps = buildTimeline(order(DELIVERED, Fulfilment.PICKUP), emptyList())

        assertTrue(steps.all { it.state == StepState.PENDING })
    }

    @Test
    fun `ready is worded for pickup and for delivery`() {
        assertEquals("Listo para recoger", READY.label(Fulfilment.PICKUP))
        assertEquals("Listo, esperando repartidor", READY.label(Fulfilment.DELIVERY))
        assertEquals("Cancelado", CANCELLED.label(Fulfilment.PICKUP))
    }

    @Test
    fun `only the end states are final`() {
        val final = OrderStatus.entries.filter { it.isFinal }

        assertEquals(setOf(PICKED_UP, DELIVERED, CANCELLED), final.toSet())
        assertFalse(READY.isFinal)
    }
}
