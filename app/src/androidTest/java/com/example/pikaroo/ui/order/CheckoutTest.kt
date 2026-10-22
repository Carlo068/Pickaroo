package com.example.pikaroo.ui.order

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.order.model.DeliveryMethod
import com.example.pikaroo.ui.order.model.OrderState
import com.example.pikaroo.ui.order.view.OrderView
import com.example.pikaroo.ui.order.viewmodel.OrderViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CheckoutTest {
    @get:Rule val compose = createComposeRule()
    private val cart = CartUiState(listOf(CartItem("1", "Pan", 3.5, "", 2)))

    @Test fun pickupReviewsAndPassesDraftWithoutPlacingOrderItself() {
        var confirmed: OrderState? = null
        val viewModel = OrderViewModel()
        compose.setContent {
            MaterialTheme {
                OrderView(cart = cart, viewModel = viewModel,
                    onConfirmOrder = { confirmed = it })
            }
        }
        compose.onNodeWithTag("checkout_review").assertIsNotEnabled()
        compose.onNodeWithText("Pickup").performClick()
        compose.onNodeWithTag("checkout_total").performScrollTo().assertTextEquals("Total: $7.00")
        compose.onNodeWithTag("checkout_review").performScrollTo().performClick()
        compose.onNodeWithText("Revisa tu pedido").assertIsDisplayed()
        compose.onNodeWithText("Confirmar pedido").performClick()
        compose.runOnIdle { assertEquals(DeliveryMethod.PICKUP, confirmed?.method) }
    }

    @Test fun deliveryCannotContinueWithoutAddress() {
        val viewModel = OrderViewModel()
        compose.setContent { MaterialTheme { OrderView(cart = cart, viewModel = viewModel) } }
        compose.onNodeWithText("Domicilio").performClick()
        compose.onNodeWithTag("checkout_review").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("checkout_total").assertTextEquals("Total: $9.99")
    }
}
