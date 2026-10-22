package com.example.pikaroo.ui.order

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.pikaroo.common.mock.*
import com.example.pikaroo.common.preferences.StringStore
import com.example.pikaroo.ui.auth.data.AuthorizedCalls
import com.example.pikaroo.ui.auth.data.SessionRepository
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.network.MockAuthService
import com.example.pikaroo.ui.auth.network.MockAuthenticator
import com.example.pikaroo.ui.cart.data.CartStorage
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel
import com.example.pikaroo.ui.order.view.OrderView
import com.example.pikaroo.ui.order.viewmodel.OrderViewModel
import com.example.pikaroo.ui.order.viewmodel.PlaceOrderViewModel
import com.example.pikaroo.ui.orders.data.ApiOrdersRepository
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.network.MockOrdersService
import com.example.pikaroo.ui.orders.view.OrderLookupContent
import com.example.pikaroo.ui.orders.view.ReceiptContent
import com.example.pikaroo.ui.orders.viewmodel.OrderLookupViewModel
import com.example.pikaroo.ui.orders.viewmodel.ReceiptUiState
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class QrOrderFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pickupReceiptQrCanBeReadAndUsedToRetrieveTheSameSavedOrder() = verifyFlow(false)
    @Test fun deliveryReceiptQrCanBeReadAndUsedToRetrieveTheSameSavedOrder() = verifyFlow(true)

    private fun verifyFlow(delivery: Boolean) {
        val values = ConcurrentHashMap<String, String>()
        val storage = object : StringStore {
            override fun get(key: String) = values[key]
            override fun put(key: String, value: String) { values[key] = value }
            override fun remove(key: String) { values.remove(key) }
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val api = MockPickarooApi(MockStore(AssetSeedSource(context), storage)) { 0L }
        val auth = MockAuthService(api)
        val session = SessionRepository(storage)
        runBlocking { session.save(auth.login(LoginRequest("customer1", "demo1234"))) }
        val orders = ApiOrdersRepository(MockOrdersService(api, MockAuthenticator { System.currentTimeMillis() / 1000 }),
            AuthorizedCalls(session, auth))
        var basket = listOf(CartItem("2", "Pan", 3.5, "", 2))
        val cart = CartViewModel(object : CartStorage {
            override fun load() = basket
            override fun save(items: List<CartItem>) { basket = items }
        })
        val checkout = OrderViewModel()
        val placer = PlaceOrderViewModel(orders)
        val lookup = OrderLookupViewModel(orders)
        var page by mutableStateOf("checkout")
        var receipt by mutableStateOf<OrderDto?>(null)
        compose.setContent {
            val cartState by cart.uiState.collectAsState()
            val placement by placer.state.collectAsState()
            val lookupState by lookup.state.collectAsState()
            val scope = rememberCoroutineScope()
            MaterialTheme {
                when (page) {
                    "checkout" -> OrderView(cart = cartState, viewModel = checkout,
                        isSaving = placement.isSaving, saveError = placement.error,
                        onConfirmOrder = { state ->
                            val snapshot = cartState
                            placer.place(snapshot, state) { saved ->
                                cart.removeOrderedItems(snapshot.items)
                                receipt = saved
                                page = "receipt"
                            }
                        },
                        onLookupOrder = { page = "lookup" },
                        onOpenLastOrder = {
                            scope.launch {
                                receipt = orders.listMine().maxByOrNull { it.createdAt }
                                page = "receipt"
                            }
                        })
                    "receipt" -> ReceiptContent(ReceiptUiState(receipt, false),
                        onBack = { page = "checkout" }, onRetry = {}, onTracking = {})
                    "lookup" -> OrderLookupContent(lookupState, lookup::updateInput,
                        onLookup = { lookup.lookup { receipt = it; page = "receipt" } },
                        onScan = lookup::scanCancelled, onBack = { page = "checkout" })
                }
            }
        }
        compose.onNodeWithText(if (delivery) "Domicilio" else "Pickup").performClick()
        if (delivery) compose.onNodeWithTag("checkout_address").performScrollTo()
            .performTextInput("Calle Primera 123, Centro, Chihuahua")
        compose.onNodeWithTag("checkout_review").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar pedido").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("order_qr").fetchSemanticsNodes().isNotEmpty() }
        val id = compose.onNodeWithTag("receipt_id").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        compose.onNodeWithTag("receipt_scroll").performScrollToNode(hasTestTag("order_qr"))
        val bitmap = compose.onNodeWithTag("order_qr").captureToImage().asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))).text
        assertEquals(id, decoded)
        compose.onNodeWithTag("receipt_scroll").performScrollToNode(hasTestTag("receipt_total"))
        compose.onNodeWithTag("receipt_total").assertTextEquals("Total: ${formatPrice(if (delivery) 9.99 else 7.0)}")
        compose.runOnIdle { assertTrue(cart.uiState.value.isEmpty) }
        compose.onNodeWithTag("receipt_scroll").performScrollToNode(hasContentDescription("Volver"))
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Último pedido").performScrollTo().performClick()
        compose.onNodeWithTag("receipt_id").assertTextEquals(id)
        compose.onNodeWithTag("receipt_scroll").performScrollToNode(hasContentDescription("Volver"))
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Consultar QR").performScrollTo().performClick()
        // Simula cancelación/permiso denegado del lector: la entrada manual sigue funcionando.
        compose.onNodeWithText("Escanear QR").performClick()
        compose.onNodeWithText("No se leyó el QR.", substring = true).assertExists()
        compose.onNodeWithTag("order_lookup_id").performTextInput(decoded)
        compose.onNodeWithTag("order_lookup_search").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("receipt_id").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("receipt_id").assertTextEquals(id)
        assertEquals(1, runBlocking { orders.listMine() }.count { it.id == id })
    }
}
