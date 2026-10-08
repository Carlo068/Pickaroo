package com.example.pikaroo.ui.cart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.pikaroo.ui.cart.view.CART_BADGE_TAG
import com.example.pikaroo.ui.cart.view.CartIconButton
import com.example.pikaroo.ui.products.model.Product
import com.example.pikaroo.ui.products.view.ProductCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CartEntryPointsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun badgeIsHiddenWhenCartIsEmpty() {
        composeRule.setContent { CartIconButton(itemCount = 0, onClick = {}) }

        composeRule.onNodeWithTag(CART_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun badgeShowsItemCountAndCapsAt99() {
        var count by mutableIntStateOf(3)
        composeRule.setContent { CartIconButton(itemCount = count, onClick = {}) }

        composeRule.onNodeWithTag(CART_BADGE_TAG, useUnmergedTree = true).assertTextEquals("3")

        count = 150
        composeRule.onNodeWithTag(CART_BADGE_TAG, useUnmergedTree = true).assertTextEquals("99+")
    }

    @Test
    fun cartIconClickOpensCart() {
        var clicks = 0
        composeRule.setContent { CartIconButton(itemCount = 1, onClick = { clicks++ }) }

        composeRule.onNodeWithContentDescription("Carrito").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun productCardAddButtonAddsThatProduct() {
        val product = Product("7", "Queso", "Fresco", 4.25, "", "Lácteos")
        val added = mutableListOf<Product>()
        composeRule.setContent { ProductCard(product = product, onAdd = { added += product }) }

        composeRule.onNodeWithContentDescription("Añadir Queso").performClick()

        assertEquals(listOf(product), added)
    }
}
