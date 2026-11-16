package com.example.pikaroo.ui.shop.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeSwitchButtons
import androidx.compose.ui.res.stringResource
import com.example.pikaroo.R
import com.example.pikaroo.common.text.asString
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.orders.model.*
import com.example.pikaroo.ui.shop.model.nextShopStatus
import com.example.pikaroo.ui.shop.viewmodel.ShopOrdersState
import com.example.pikaroo.ui.shop.viewmodel.ShopOrdersViewModel

object ShopTestTags {
    const val EMPTY = "shop_orders_empty"
    const val ERROR = "shop_orders_error"
    fun order(id: String, status: OrderStatus) = "shop_order_${id}_${status.name}"
    fun advance(id: String) = "shop_advance_$id"
}

@Composable
fun ShopHomeView(userName: String, otherModes: List<RoleMode>, onSwitchMode: (RoleMode) -> Unit, onLogout: () -> Unit) {
    val vm: ShopOrdersViewModel = viewModel(factory = ShopOrdersViewModel.Factory)
    val state by vm.state.collectAsState()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(vm, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.poll() }
    }
    ShopOrdersContent(state, userName, otherModes, onSwitchMode, onLogout, vm::advance, vm::retry)
}

@Composable
fun ShopOrdersContent(
    state: ShopOrdersState,
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit,
    onAdvance: (OrderDto) -> Unit,
    onRetry: () -> Unit
) {
    var confirmPickup by remember { mutableStateOf<OrderDto?>(null) }
    confirmPickup?.let { order ->
        AlertDialog(
            onDismissRequest = { confirmPickup = null },
            title = { Text(stringResource(R.string.shop_confirm_pickup)) },
            text = { Text(stringResource(R.string.shop_confirm_pickup_message, order.number)) },
            confirmButton = { TextButton(onClick = { confirmPickup = null; onAdvance(order) }) { Text(stringResource(R.string.shop_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmPickup = null }) { Text(stringResource(R.string.common_return)) } }
        )
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.shop_home_title), style = MaterialTheme.typography.headlineMedium)
        Text(userName)
        ModeSwitchButtons(otherModes, onSwitchMode)
        TextButton(onClick = onLogout) { Text(stringResource(R.string.common_logout)) }
        Text(stringResource(R.string.shop_orders_title), style = MaterialTheme.typography.titleLarge)
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(ShopTestTags.ERROR))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.shop_refresh)) }
        }
        if (!state.loading && state.error == null && state.orders.isEmpty()) {
            Text(stringResource(R.string.shop_orders_empty), Modifier.testTag(ShopTestTags.EMPTY))
        }
        LazyColumn(Modifier.weight(1f).testTag("shop_orders_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OrderStatus.entries.forEach { status ->
                val orders = state.orders.filter { it.status == status }
                if (orders.isNotEmpty()) {
                    item(key = status.name) {
                        val group = stringResource(if (status == OrderStatus.READY) R.string.shop_group_ready else status.labelRes(Fulfilment.PICKUP))
                        Text(stringResource(R.string.shop_group_header, group, orders.size), style = MaterialTheme.typography.titleMedium)
                    }
                    items(orders, key = { it.id }) { order ->
                        Card(Modifier.fillMaxWidth().testTag(ShopTestTags.order(order.id, order.status))) {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.order_number, order.number), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(if (order.fulfilment == Fulfilment.PICKUP) R.string.order_fulfilment_pickup else R.string.order_fulfilment_delivery))
                                Text(stringResource(order.status.labelRes(order.fulfilment)))
                                order.items.forEach { Text("${it.quantity} × ${it.name}") }
                                order.delivery?.let { Text(it.address) }
                                Text(stringResource(R.string.shop_order_total, formatPrice(order.total)))
                                nextShopStatus(order.status, order.fulfilment)?.let { next ->
                                    Button(
                                        onClick = { if (next == OrderStatus.PICKED_UP) confirmPickup = order else onAdvance(order) },
                                        enabled = state.updatingId == null,
                                        modifier = Modifier.testTag(ShopTestTags.advance(order.id))
                                    ) {
                                        Text(stringResource(if (state.updatingId == order.id) R.string.shop_saving else when (next) {
                                            OrderStatus.ACCEPTED -> R.string.shop_action_accept
                                            OrderStatus.PREPARING -> R.string.shop_action_prepare
                                            OrderStatus.READY -> R.string.shop_action_ready
                                            else -> R.string.shop_confirm_pickup
                                        }))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
