package com.example.pikaroo.ui.products

import com.example.pikaroo.MainDispatcherRule
import com.example.pikaroo.ui.products.data.ProductsRepository
import com.example.pikaroo.ui.products.model.Product
import com.example.pikaroo.ui.products.viewmodel.ProductsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apple = Product("1", "Manzana", "Roja", 2.5, "img", "Frutas")
    private val bread = Product("2", "Pan", "Integral", 3.0, "img", "Panadería")
    private val banana = Product("3", "Plátano", "Maduro", 1.2, "img", "Frutas")

    private class FakeProductsRepository(
        private val products: List<Product> = emptyList(),
        private val failure: Exception? = null
    ) : ProductsRepository {
        var calls = 0
            private set

        override suspend fun getProducts(): List<Product> {
            calls++
            failure?.let { throw it }
            return products
        }
    }

    @Test
    fun `loads products from the repository`() {
        val repository = FakeProductsRepository(listOf(apple, bread, banana))

        val viewModel = ProductsViewModel(repository)

        assertEquals(1, repository.calls)
        assertEquals(listOf(apple, bread, banana), viewModel.state.products)
        assertEquals(listOf(apple, bread, banana), viewModel.state.filteredProducts)
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `categories start with Todos and are sorted without duplicates`() {
        val viewModel = ProductsViewModel(FakeProductsRepository(listOf(bread, apple, banana)))

        assertEquals(listOf("Todos", "Frutas", "Panadería"), viewModel.state.categories)
    }

    @Test
    fun `repository failure sets the error and stops loading`() {
        val viewModel = ProductsViewModel(
            FakeProductsRepository(failure = IllegalStateException("sin conexión"))
        )

        assertEquals("sin conexión", viewModel.state.error)
        assertFalse(viewModel.state.isLoading)
        assertTrue(viewModel.state.products.isEmpty())
    }

    @Test
    fun `selecting a category filters the products`() {
        val viewModel = ProductsViewModel(FakeProductsRepository(listOf(apple, bread, banana)))

        viewModel.selectCategory("Frutas")

        assertEquals("Frutas", viewModel.state.selectedCategory)
        assertEquals(listOf(apple, banana), viewModel.state.filteredProducts)
    }

    @Test
    fun `selecting Todos shows every product again`() {
        val viewModel = ProductsViewModel(FakeProductsRepository(listOf(apple, bread, banana)))
        viewModel.selectCategory("Panadería")

        viewModel.selectCategory("Todos")

        assertEquals(listOf(apple, bread, banana), viewModel.state.filteredProducts)
    }
}
