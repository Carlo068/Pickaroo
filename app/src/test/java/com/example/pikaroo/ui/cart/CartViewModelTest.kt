package com.example.pikaroo.ui.cart

import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel
import com.example.pikaroo.ui.products.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CartViewModelTest {

    private val apple = Product("1", "Manzana", "Roja", 2.50, "img1", "Frutas")
    private val bread = Product("2", "Pan", "Integral", 3.00, "img2", "Panadería")

    private lateinit var storage: FakeCartStorage
    private lateinit var viewModel: CartViewModel

    @Before
    fun setUp() {
        storage = FakeCartStorage()
        viewModel = CartViewModel(storage)
    }

    private fun quantityOf(id: String) =
        viewModel.uiState.value.items.firstOrNull { it.productId == id }?.quantity

    @Test
    fun `starts with the items saved in storage`() {
        val saved = listOf(CartItem("1", "Manzana", 2.5, "img1", 4))

        val restored = CartViewModel(FakeCartStorage(saved))

        assertEquals(saved, restored.uiState.value.items)
    }

    @Test
    fun `adding a new product adds it with quantity one`() {
        viewModel.add(apple)

        assertEquals(1, quantityOf("1"))
        assertEquals(1, viewModel.uiState.value.itemCount)
    }

    @Test
    fun `adding the same product twice increases its quantity`() {
        viewModel.add(apple)
        viewModel.add(apple)

        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals(2, quantityOf("1"))
    }

    @Test
    fun `different products are separate lines`() {
        viewModel.add(apple)
        viewModel.add(bread)

        assertEquals(listOf("1", "2"), viewModel.uiState.value.items.map { it.productId })
        assertEquals(5.50, viewModel.uiState.value.subtotal, 0.0001)
    }

    @Test
    fun `increment and decrement change the quantity`() {
        viewModel.add(apple)

        viewModel.increment("1")
        viewModel.increment("1")
        assertEquals(3, quantityOf("1"))

        viewModel.decrement("1")
        assertEquals(2, quantityOf("1"))
    }

    @Test
    fun `decrementing to zero removes the item`() {
        viewModel.add(apple)
        viewModel.add(bread)

        viewModel.decrement("1")

        assertEquals(listOf("2"), viewModel.uiState.value.items.map { it.productId })
    }

    @Test
    fun `remove deletes the whole line regardless of quantity`() {
        viewModel.add(apple)
        viewModel.increment("1")
        viewModel.add(bread)

        viewModel.remove("1")

        assertEquals(listOf("2"), viewModel.uiState.value.items.map { it.productId })
    }

    @Test
    fun `clear empties the cart`() {
        viewModel.add(apple)
        viewModel.add(bread)

        viewModel.clear()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertTrue(storage.saved.isEmpty())
    }

    @Test
    fun `operations on unknown ids do nothing harmful`() {
        viewModel.add(apple)

        viewModel.increment("missing")
        viewModel.decrement("missing")
        viewModel.remove("missing")

        assertEquals(1, quantityOf("1"))
    }

    @Test
    fun `every change is persisted`() {
        viewModel.add(apple)
        viewModel.increment("1")
        viewModel.add(bread)
        viewModel.decrement("2")

        assertEquals(4, storage.saveCount)
        assertEquals(viewModel.uiState.value.items, storage.saved)
        assertEquals(listOf(CartItem("1", "Manzana", 2.50, "img1", 2)), storage.saved)
    }
}
