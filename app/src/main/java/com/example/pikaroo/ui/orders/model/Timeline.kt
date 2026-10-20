package com.example.pikaroo.ui.orders.model

import com.example.pikaroo.ui.orders.model.OrderStatus.ACCEPTED
import com.example.pikaroo.ui.orders.model.OrderStatus.CANCELLED
import com.example.pikaroo.ui.orders.model.OrderStatus.DELIVERED
import com.example.pikaroo.ui.orders.model.OrderStatus.DRIVER_ASSIGNED
import com.example.pikaroo.ui.orders.model.OrderStatus.OUT_FOR_DELIVERY
import com.example.pikaroo.ui.orders.model.OrderStatus.PICKED_UP
import com.example.pikaroo.ui.orders.model.OrderStatus.PLACED
import com.example.pikaroo.ui.orders.model.OrderStatus.PREPARING
import com.example.pikaroo.ui.orders.model.OrderStatus.READY

enum class StepState { DONE, CURRENT, PENDING, CANCELLED }

data class TimelineStep(
    val status: OrderStatus,
    val label: String,
    val state: StepState,
    val at: String?
)

private val PICKUP_PATH = listOf(PLACED, ACCEPTED, PREPARING, READY, PICKED_UP)
private val DELIVERY_PATH = listOf(PLACED, ACCEPTED, PREPARING, READY, DRIVER_ASSIGNED, OUT_FOR_DELIVERY, DELIVERED)

fun OrderStatus.label(fulfilment: Fulfilment): String = when (this) {
    PLACED -> "Pedido recibido"
    ACCEPTED -> "Pedido aceptado"
    PREPARING -> "Preparando tu pedido"
    READY -> if (fulfilment == Fulfilment.PICKUP) "Listo para recoger" else "Listo, esperando repartidor"
    PICKED_UP -> "Recogido"
    DRIVER_ASSIGNED -> "Repartidor asignado"
    OUT_FOR_DELIVERY -> "En camino"
    DELIVERED -> "Entregado"
    CANCELLED -> "Cancelado"
}

/** Pasos de seguimiento del pedido; si se canceló, solo los que alcanzó y el cierre cancelado. */
fun buildTimeline(order: OrderDto, events: List<OrderEventDto>): List<TimelineStep> {
    val firstEventAt = events.groupBy { it.status }.mapValues { (_, same) -> same.last().at }
    fun step(status: OrderStatus, state: StepState) =
        TimelineStep(status, status.label(order.fulfilment), state, firstEventAt[status])

    if (order.status == CANCELLED) {
        val reached = events.map { it.status }.filter { it != CANCELLED }.distinct()
        return reached.map { step(it, StepState.DONE) } + step(CANCELLED, StepState.CANCELLED)
    }

    val path = if (order.fulfilment == Fulfilment.PICKUP) PICKUP_PATH else DELIVERY_PATH
    val current = path.indexOf(order.status)
    return path.mapIndexed { index, status ->
        val state = when {
            index < current -> StepState.DONE
            index == current -> if (status.isFinal) StepState.DONE else StepState.CURRENT
            else -> StepState.PENDING
        }
        step(status, state)
    }
}
