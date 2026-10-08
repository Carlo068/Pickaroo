package com.example.pikaroo.ui.cart

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.cart.data.CartPreferences
import com.example.pikaroo.ui.cart.model.CartItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CartPreferencesTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val alice = "test_alice_${System.nanoTime()}"
    private val bob = "test_bob_${System.nanoTime()}"

    @After
    fun cleanUp() {
        AppPreferences(context).remove(CartPreferences.keyFor(alice), CartPreferences.keyFor(bob))
    }

    @Test
    fun savedCartIsReadBackByANewInstance() {
        val items = listOf(
            CartItem("1", "Manzana", 2.5, "img", 3),
            CartItem("2", "Pan", 3.0, "img", 1)
        )

        CartPreferences(context, alice).save(items)

        assertEquals(items, CartPreferences(context, alice).load())
    }

    @Test
    fun eachUserHasASeparateCart() {
        CartPreferences(context, alice).save(listOf(CartItem("1", "Manzana", 2.5, "img", 1)))

        assertTrue(CartPreferences(context, bob).load().isEmpty())
    }

    @Test
    fun savingAnEmptyCartClearsIt() {
        val storage = CartPreferences(context, alice)
        storage.save(listOf(CartItem("1", "Manzana", 2.5, "img", 1)))

        storage.save(emptyList())

        assertTrue(CartPreferences(context, alice).load().isEmpty())
    }
}
