package com.example.pikaroo.ui.cart

import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.products.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartModelTest {

    private val apple = CartItem("1", "Manzana", 2.50, "url", quantity = 2)
    private val bread = CartItem("2", "Pan", 3.00, "url", quantity = 1)

    @Test
    fun `empty cart has zero totals and no delivery fee`() {
        val state = CartUiState()

        assertTrue(state.isEmpty)
        assertEquals(0, state.itemCount)
        assertEquals(0.0, state.subtotal, 0.0001)
        assertEquals(0.0, state.deliveryFee, 0.0001)
        assertEquals(0.0, state.total, 0.0001)
    }

    @Test
    fun `totals are derived from items`() {
        val state = CartUiState(listOf(apple, bread))

        assertFalse(state.isEmpty)
        assertEquals(3, state.itemCount)
        assertEquals(8.00, state.subtotal, 0.0001)
        assertEquals(CartUiState.DELIVERY_FEE, state.deliveryFee, 0.0001)
        assertEquals(8.00 + CartUiState.DELIVERY_FEE, state.total, 0.0001)
    }

    @Test
    fun `line total multiplies price by quantity`() {
        assertEquals(5.00, apple.lineTotal, 0.0001)
    }

    @Test
    fun `cart item is built from a product with quantity one`() {
        val product = Product("p9", "Leche", "Entera", 1.99, "img", "Lácteos")

        val item = CartItem.from(product)

        assertEquals(CartItem("p9", "Leche", 1.99, "img", 1), item)
    }

    @Test
    fun `prices are formatted with two decimals`() {
        assertEquals("$2.50", formatPrice(2.5))
        assertEquals("$10.99", formatPrice(10.989))
        assertEquals("$0.00", formatPrice(0.0))
    }
}
