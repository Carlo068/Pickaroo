package com.example.pikaroo.ui.cart

import com.example.pikaroo.ui.cart.data.CartJson
import com.example.pikaroo.ui.cart.model.CartItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartJsonTest {

    @Test
    fun `encode then decode returns the same items`() {
        val items = listOf(
            CartItem("1", "Manzana", 2.5, "https://img/1.png", 3),
            CartItem("2", "Pan Integral", 3.0, "", 1)
        )

        assertEquals(items, CartJson.decode(CartJson.encode(items)))
    }

    @Test
    fun `blank json decodes to an empty cart`() {
        assertTrue(CartJson.decode("").isEmpty())
        assertTrue(CartJson.decode("   ").isEmpty())
    }

    @Test
    fun `malformed json decodes to an empty cart instead of crashing`() {
        assertTrue(CartJson.decode("{not json").isEmpty())
    }

    @Test
    fun `items with zero quantity are dropped when decoding`() {
        val json = CartJson.encode(
            listOf(
                CartItem("1", "Manzana", 2.5, "", 0),
                CartItem("2", "Pan", 3.0, "", 2)
            )
        )

        assertEquals(listOf("2"), CartJson.decode(json).map { it.productId })
    }

    @Test
    fun `items with missing required fields are dropped instead of crashing later`() {
        val json = """[
            {"productId":"1","name":"Manzana","price":2.5,"imageUrl":"img","quantity":1},
            {"name":"Sin id","price":1.0,"quantity":1},
            {"productId":"3","price":1.0,"quantity":1},
            {"productId":"4","name":"Sin precio","quantity":1},
            {"productId":"5","name":"Sin imagen","price":1.0,"quantity":2},
            null
        ]"""

        val items = CartJson.decode(json)

        assertEquals(listOf("1", "5"), items.map { it.productId })
        assertEquals("", items.last().imageUrl)
    }

    @Test
    fun `json that is not a list decodes to an empty cart`() {
        assertTrue(CartJson.decode("{\"productId\":\"1\"}").isEmpty())
    }
}
