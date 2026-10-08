package com.example.pikaroo.ui.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.pikaroo.ui.cart.data.CartStorage
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.view.CartTestTags
import com.example.pikaroo.ui.cart.view.CartView
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class InMemoryStorage(var items: List<CartItem>) : CartStorage {
        override fun load() = items
        override fun save(items: List<CartItem>) {
            this.items = items
        }
    }

    private fun setCart(vararg items: CartItem, onCheckout: () -> Unit = {}): CartViewModel {
        val viewModel = CartViewModel(InMemoryStorage(items.toList()))
        composeRule.setContent {
            CartView(cartViewModel = viewModel, onBack = {}, onCheckout = onCheckout)
        }
        return viewModel
    }

    @Test
    fun emptyCartShowsEmptyState() {
        setCart()

        composeRule.onNodeWithTag(CartTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).assertDoesNotExist()
    }

    @Test
    fun showsItemsAndTotals() {
        setCart(
            CartItem("1", "Manzana", 2.50, "", 2),
            CartItem("2", "Pan", 3.00, "", 1)
        )

        composeRule.onNodeWithTag(CartTestTags.quantity("1")).assertTextEquals("2")
        composeRule.onNodeWithTag(CartTestTags.SUBTOTAL).assertTextEquals("$8.00")
        composeRule.onNodeWithTag(CartTestTags.DELIVERY_FEE).assertTextEquals("$2.99")
        composeRule.onNodeWithTag(CartTestTags.TOTAL).assertTextEquals("$10.99")
    }

    @Test
    fun incrementUpdatesQuantityAndTotal() {
        setCart(CartItem("1", "Manzana", 2.50, "", 1))

        composeRule.onNodeWithTag(CartTestTags.increment("1")).performClick()

        composeRule.onNodeWithTag(CartTestTags.quantity("1")).assertTextEquals("2")
        composeRule.onNodeWithTag(CartTestTags.SUBTOTAL).assertTextEquals("$5.00")
    }

    @Test
    fun decrementingLastUnitShowsEmptyState() {
        val viewModel = setCart(CartItem("1", "Manzana", 2.50, "", 1))

        composeRule.onNodeWithTag(CartTestTags.decrement("1")).performClick()

        composeRule.onNodeWithTag(CartTestTags.EMPTY).assertIsDisplayed()
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun removeDeletesOnlyThatItem() {
        val viewModel = setCart(
            CartItem("1", "Manzana", 2.50, "", 3),
            CartItem("2", "Pan", 3.00, "", 1)
        )

        composeRule.onNodeWithTag(CartTestTags.remove("1")).performClick()

        composeRule.onNodeWithTag(CartTestTags.quantity("1")).assertDoesNotExist()
        composeRule.onNodeWithTag(CartTestTags.quantity("2")).assertTextEquals("1")
        assertEquals(listOf("2"), viewModel.uiState.value.items.map { it.productId })
    }

    @Test
    fun checkoutButtonInvokesCallback() {
        var checkoutClicks = 0
        setCart(CartItem("1", "Manzana", 2.50, "", 1), onCheckout = { checkoutClicks++ })

        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).performClick()

        assertEquals(1, checkoutClicks)
    }
}
