package com.example.pikaroo.ui.order

import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.order.model.DeliveryMethod
import com.example.pikaroo.ui.order.viewmodel.OrderViewModel
import org.junit.Assert.*
import org.junit.Test

class DeliveryAddressTest {
    private val cart = CartUiState(listOf(CartItem("1", "Pan", 3.5, "", 2)))

    @Test fun addressValidationFollowsEdits() {
        val model = OrderViewModel()
        model.selectMethod(DeliveryMethod.DELIVERY)
        assertFalse(model.state.canReview(cart))
        model.updateAddress(" \n\t ")
        assertFalse(model.state.canReview(cart))
        model.updateAddress("Calle Primera 123, Centro, Chihuahua")
        assertTrue(model.state.canReview(cart))
        model.updateAddress("")
        assertFalse(model.state.canReview(cart))
    }

    @Test fun methodChangesRetainTheAddressAndUpdateShipping() {
        val model = OrderViewModel()
        model.selectMethod(DeliveryMethod.DELIVERY)
        model.updateAddress("Calle Primera 123, Centro, Chihuahua")
        model.selectMethod(DeliveryMethod.PICKUP)
        assertEquals(7.0, model.state.total(cart), 0.001)
        model.selectMethod(DeliveryMethod.DELIVERY)
        assertEquals("Calle Primera 123, Centro, Chihuahua", model.state.address)
        assertTrue(model.state.canReview(cart))
        assertEquals(9.99, model.state.total(cart), 0.001)
    }
}
