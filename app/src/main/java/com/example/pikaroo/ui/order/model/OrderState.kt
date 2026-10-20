package com.example.pikaroo.ui.order.model

import com.example.pikaroo.ui.cart.model.CartUiState

enum class DeliveryMethod { PICKUP, DELIVERY }

data class OrderState(
    val method: DeliveryMethod? = null,
    val address: String = ""
) {
    fun canReview(cart: CartUiState): Boolean = !cart.isEmpty && method != null &&
        (method == DeliveryMethod.PICKUP || address.isNotBlank())

    fun deliveryFee(cart: CartUiState): Double =
        if (cart.isEmpty || method != DeliveryMethod.DELIVERY) 0.0 else cart.deliveryFee

    fun total(cart: CartUiState): Double = cart.subtotal + deliveryFee(cart)
}
