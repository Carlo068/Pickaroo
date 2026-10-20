package com.example.pikaroo.ui.orders.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.label
import com.example.pikaroo.ui.orders.qr.encodeOrderQr
import com.example.pikaroo.ui.orders.viewmodel.ReceiptUiState
import com.example.pikaroo.ui.orders.viewmodel.ReceiptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReceiptView(orderId: String?, onBack: () -> Unit, onTracking: (String) -> Unit) {
    val model: ReceiptViewModel = viewModel(factory = ReceiptViewModel.factory(orderId))
    val state by model.state.collectAsState()
    ReceiptContent(state, onBack, model::load, onTracking)
}

@Composable
fun ReceiptContent(
    state: ReceiptUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTracking: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                Text("Comprobante de pedido", style = MaterialTheme.typography.titleLarge)
            }
        }
        when {
            state.isLoading -> item { CircularProgressIndicator() }
            state.order == null -> item {
                Text(state.error ?: "No se encontró el pedido", Modifier.testTag("receipt_error"))
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
            else -> {
                val order = state.order
                item {
                    Text("ID de la orden", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(order.id, Modifier.testTag("receipt_id")) }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(order.id)) }) { Text("Copiar ID") }
                    Text("Fecha: ${formatTime(order.createdAt) ?: order.createdAt}")
                    Text(order.status.label(order.fulfilment))
                }
                item { OrderQrImage(order.id) }
                item {
                    val payment = if (order.fulfilment == Fulfilment.PICKUP) "en tienda" else "contra entrega"
                    Text(if (order.paymentStatus == "PENDING") "Pago pendiente · $payment" else "Pago $payment",
                        fontWeight = FontWeight.SemiBold)
                    Text("Este QR identifica tu pedido; no confirma un pago.")
                    if (order.fulfilment == Fulfilment.PICKUP) {
                        Text("Pickup · ${order.shop?.name ?: "Recoger en tienda"}")
                        Text("Horario: ${formatTime(order.pickup?.slotStart) ?: "Por confirmar"}")
                        order.pickup?.code?.let { Text("Clave de recogida: $it") }
                    } else {
                        Text("Domicilio")
                        Text(order.delivery?.address.orEmpty())
                    }
                }
                item { HorizontalDivider(); Text("Productos", fontWeight = FontWeight.Bold) }
                items(order.items) { line ->
                    Text("${line.quantity} × ${line.name} · ${formatPrice(line.lineTotal)}")
                }
                item {
                    HorizontalDivider()
                    Text("Subtotal: ${formatPrice(order.subtotal)}")
                    Text("Envío: ${formatPrice(order.deliveryFee)}")
                    Text("Total: ${formatPrice(order.total)}", Modifier.testTag("receipt_total"),
                        style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { onTracking(order.id) }) { Text("Ver seguimiento") }
                }
            }
        }
    }
}

@Composable
private fun OrderQrImage(orderId: String) {
    var attempt by remember(orderId) { mutableIntStateOf(0) }
    val result by produceState<Result<Bitmap>?>(null, orderId, attempt) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val matrix = encodeOrderQr(orderId)
                val pixels = IntArray(matrix.width * matrix.height) { index ->
                    if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            }
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when {
            result == null -> CircularProgressIndicator()
            result!!.isSuccess -> Image(result!!.getOrThrow().asImageBitmap(), "QR de la orden",
                Modifier.size(240.dp).background(Color.White).testTag("order_qr"), filterQuality = FilterQuality.None)
            else -> Column {
                Text("No se pudo generar el QR. Puedes usar el ID de la orden.")
                TextButton(onClick = { attempt++ }) { Text("Generar QR de nuevo") }
            }
        }
    }
}
