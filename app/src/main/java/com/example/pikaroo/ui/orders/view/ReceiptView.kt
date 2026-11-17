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
import com.example.pikaroo.ui.orders.model.labelRes
import com.example.pikaroo.ui.orders.qr.encodeOrderQr
import com.example.pikaroo.ui.orders.viewmodel.ReceiptUiState
import com.example.pikaroo.ui.orders.viewmodel.ReceiptViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.example.pikaroo.R
import com.example.pikaroo.common.text.asString

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
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White).testTag("receipt_scroll"),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_return)) }
                Text(stringResource(R.string.receipt_title), style = MaterialTheme.typography.titleLarge)
            }
        }
        when {
            state.isLoading -> item { CircularProgressIndicator() }
            state.order == null -> item {
                Text(state.error?.asString() ?: stringResource(R.string.order_error_not_found), Modifier.testTag("receipt_error"))
                TextButton(onClick = onRetry) { Text(stringResource(R.string.receipt_retry)) }
            }
            else -> {
                val order = state.order
                item {
                    Text(stringResource(R.string.order_id_label), style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(order.id, Modifier.testTag("receipt_id")) }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(order.id)) }) { Text(stringResource(R.string.receipt_copy_id)) }
                    Text(stringResource(R.string.receipt_date, formatTime(order.createdAt) ?: order.createdAt))
                    Text(stringResource(order.status.labelRes(order.fulfilment)))
                }
                item { OrderQrImage(order.id) }
                item {
                    val payment = stringResource(if (order.fulfilment == Fulfilment.PICKUP) R.string.checkout_pay_in_store else R.string.checkout_pay_on_delivery)
                    Text(if (order.paymentStatus == "PENDING") stringResource(R.string.receipt_payment_pending, payment) else stringResource(R.string.receipt_payment, payment),
                        fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.receipt_qr_note))
                    if (order.fulfilment == Fulfilment.PICKUP) {
                        Text(stringResource(R.string.receipt_pickup_shop, order.shop?.name ?: stringResource(R.string.order_fulfilment_pickup)))
                        Text(stringResource(R.string.order_time_slot, formatTime(order.pickup?.slotStart) ?: stringResource(R.string.receipt_slot_tbd)))
                        order.pickup?.code?.let { Text(stringResource(R.string.receipt_pickup_code, it)) }
                    } else {
                        Text(stringResource(R.string.checkout_method_delivery))
                        Text(order.delivery?.address.orEmpty())
                    }
                }
                item { HorizontalDivider(); Text(stringResource(R.string.receipt_products), fontWeight = FontWeight.Bold) }
                items(order.items) { line ->
                    Text("${line.quantity} × ${line.name} · ${formatPrice(line.lineTotal)}")
                }
                item {
                    HorizontalDivider()
                    Text(stringResource(R.string.checkout_subtotal_line, formatPrice(order.subtotal)))
                    Text(stringResource(R.string.checkout_delivery_line, formatPrice(order.deliveryFee)))
                    Text(stringResource(R.string.checkout_total_line, formatPrice(order.total)), Modifier.testTag("receipt_total"),
                        style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { onTracking(order.id) }) { Text(stringResource(R.string.receipt_track)) }
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
            result!!.isSuccess -> Image(result!!.getOrThrow().asImageBitmap(), stringResource(R.string.receipt_qr_description),
                Modifier.size(240.dp).background(Color.White).testTag("order_qr"), filterQuality = FilterQuality.None)
            else -> Column {
                Text(stringResource(R.string.receipt_qr_error))
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.receipt_qr_retry)) }
            }
        }
    }
}
