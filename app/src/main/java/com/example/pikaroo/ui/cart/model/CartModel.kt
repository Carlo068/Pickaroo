package com.example.pikaroo.ui.cart.model

import com.example.pikaroo.ui.products.model.Product
import java.util.Locale

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
) {
    val lineTotal: Double get() = price * quantity

    companion object {
        fun from(product: Product, quantity: Int = 1) = CartItem(
            productId = product.id,
            name = product.name,
            price = product.price,
            imageUrl = product.imageUrl,
            quantity = quantity
        )
    }
}

data class CartUiState(
    val items: List<CartItem> = emptyList()
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val deliveryFee: Double get() = if (items.isEmpty()) 0.0 else DELIVERY_FEE
    val total: Double get() = subtotal + deliveryFee
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        const val DELIVERY_FEE = 2.99
    }
}

fun formatPrice(amount: Double): String = String.format(Locale.US, "$%.2f", amount)
