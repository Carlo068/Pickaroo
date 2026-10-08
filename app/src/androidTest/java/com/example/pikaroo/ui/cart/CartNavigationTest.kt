package com.example.pikaroo.ui.cart

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.ui.cart.data.CartPreferences
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.view.CartTestTags
import com.example.pikaroo.ui.navigation.TabsScaffold
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CartNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = CartPreferences(context)
    private lateinit var originalCart: List<CartItem>

    @Before
    fun seedCart() {
        originalCart = storage.load()
        storage.save(listOf(CartItem("1", "Manzana", 2.5, "", 1)))
    }

    @After
    fun restoreCart() {
        storage.save(originalCart)
    }

    private fun openCartFromHome() {
        composeRule.setContent { TabsScaffold() }
        composeRule.onNodeWithContentDescription("Carrito").performClick()
        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).assertExists()
    }

    @Test
    fun checkoutThenHomeTabShowsHomeNotCart() {
        openCartFromHome()

        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).performClick()
        composeRule.onNodeWithText("Método de Entrega").assertExists()

        composeRule.onNodeWithText("Inicio").performClick()

        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).assertDoesNotExist()
        composeRule.onNodeWithText("Buscar productos...").assertExists()
    }

    @Test
    fun switchingTabWhileCartIsOpenDoesNotRestoreCart() {
        openCartFromHome()

        composeRule.onNodeWithText("Usuario").performClick()
        composeRule.onNodeWithText("Inicio").performClick()

        composeRule.onNodeWithTag(CartTestTags.CHECKOUT).assertDoesNotExist()
        composeRule.onNodeWithText("Buscar productos...").assertExists()
    }
}
