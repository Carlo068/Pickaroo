package com.example.pikaroo.ui.order

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.order.model.OrderState
import com.example.pikaroo.ui.order.view.OrderView
import com.example.pikaroo.ui.order.viewmodel.OrderViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DeliveryAddressTest {
    @get:Rule val compose = createComposeRule()
    private val cart = CartUiState(listOf(CartItem("1", "Pan", 3.5, "", 2)))

    @Test fun deliveryValidatesInputAndPassesTrimmedAddressToConfirmation() {
        val model = OrderViewModel()
        var confirmed: OrderState? = null
        compose.setContent {
            MaterialTheme { OrderView(cart = cart, viewModel = model, onConfirmOrder = { confirmed = it }) }
        }
        compose.onNodeWithText("Domicilio").performClick()
        compose.onNodeWithTag("checkout_review").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("checkout_address").performScrollTo().performTextInput("   ")
        compose.onNodeWithTag("checkout_review").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("checkout_address").performScrollTo()
            .performTextReplacement("  Calle Primera 123, Centro, Chihuahua  ")
        compose.onNodeWithTag("checkout_review").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar pedido").performClick()
        compose.runOnIdle { assertEquals("Calle Primera 123, Centro, Chihuahua", confirmed?.address) }
    }

    @Test fun switchingMethodsRetainsTheTypedAddress() {
        val model = OrderViewModel()
        compose.setContent { MaterialTheme { OrderView(cart = cart, viewModel = model) } }
        compose.onNodeWithText("Domicilio").performClick()
        compose.onNodeWithTag("checkout_address").performScrollTo().performTextInput("Calle Primera 123")
        compose.onNodeWithText("Pickup").performScrollTo().performClick()
        compose.onNodeWithTag("checkout_address").assertDoesNotExist()
        compose.onNodeWithTag("checkout_total").performScrollTo().assertTextEquals("Total: $7.00")
        compose.onNodeWithText("Domicilio").performScrollTo().performClick()
        compose.onNodeWithTag("checkout_address").performScrollTo().assertTextContains("Calle Primera 123")
        compose.onNodeWithTag("checkout_total").performScrollTo().assertTextEquals("Total: $9.99")
    }
}
