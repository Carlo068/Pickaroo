package com.example.pikaroo.ui.order

import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.order.model.DeliveryMethod
import com.example.pikaroo.ui.order.model.OrderState
import org.junit.Assert.*
import org.junit.Test

class CheckoutStateTest {
    private val cart = CartUiState(listOf(CartItem("1", "Pan", 3.5, "", 2)))

    @Test fun emptyCartAndMissingMethodCannotContinue() {
        assertFalse(OrderState().canReview(cart))
        assertFalse(OrderState(DeliveryMethod.PICKUP).canReview(CartUiState()))
    }

    @Test fun pickupHasNoDeliveryFee() {
        val state = OrderState(DeliveryMethod.PICKUP)
        assertTrue(state.canReview(cart))
        assertEquals(0.0, state.deliveryFee(cart), 0.001)
        assertEquals(7.0, state.total(cart), 0.001)
    }

    @Test fun deliveryRequiresAddressAndIncludesFee() {
        val state = OrderState(DeliveryMethod.DELIVERY)
        assertFalse(state.canReview(cart))
        assertFalse(state.copy(address = "   ").canReview(cart))
        assertTrue(state.copy(address = "Calle 1").canReview(cart))
        assertEquals(9.99, state.total(cart), 0.001)
    }

    @Test fun changingMethodPreservesAddressAndRecalculatesTotal() {
        val delivery = OrderState(DeliveryMethod.DELIVERY, "Calle 1")
        val pickup = delivery.copy(method = DeliveryMethod.PICKUP)
        assertEquals("Calle 1", pickup.address)
        assertEquals(7.0, pickup.total(cart), 0.001)
        assertEquals(9.99, pickup.copy(method = DeliveryMethod.DELIVERY).total(cart), 0.001)
    }
}
