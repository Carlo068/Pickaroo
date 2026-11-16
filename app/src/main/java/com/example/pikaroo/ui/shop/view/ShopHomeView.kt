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
    onAdvance: (OrderDto, String?) -> Unit,
    onRetry: () -> Unit
) {
    var confirmPickup by remember { mutableStateOf<OrderDto?>(null) }
    LaunchedEffect(confirmPickup?.id, state.orders) {
        val id = confirmPickup?.id
        if (id != null && state.orders.none { it.id == id && it.status == OrderStatus.READY }) confirmPickup = null
    }
    confirmPickup?.let { order ->
        var code by remember(order.id) { mutableStateOf("") }
        var submitted by remember(order.id) { mutableStateOf(false) }
        val busy = state.updatingId != null
        AlertDialog(
            onDismissRequest = { if (!busy) confirmPickup = null },
            title = { Text("Confirmar recogida") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el código que muestra el cliente para el pedido #${order.number}.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; submitted = false },
                        label = { Text("Código de recogida") },
                        singleLine = true,
                        enabled = !busy,
                        isError = submitted && state.pickupError != null,
                        modifier = Modifier.testTag("pickup_code_input")
                    )
                    if (submitted) state.pickupError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("pickup_code_error"))
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { submitted = true; onAdvance(order, code) },
                    enabled = code.isNotBlank() && !busy,
                    modifier = Modifier.testTag("pickup_code_confirm")
                ) { Text(if (busy) "Verificando…" else "Confirmar") }
            },
            dismissButton = { TextButton(onClick = { confirmPickup = null }, enabled = !busy) { Text("Volver") } }
        )
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Modo tienda", style = MaterialTheme.typography.headlineMedium)
        Text(userName)
        ModeSwitchButtons(otherModes, onSwitchMode)
        TextButton(onClick = onLogout) { Text("Cerrar sesión") }
        Text("Pedidos de la sucursal", style = MaterialTheme.typography.titleLarge)
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(ShopTestTags.ERROR))
            TextButton(onClick = onRetry) { Text("Actualizar lista") }
        }
        if (!state.loading && state.error == null && state.orders.isEmpty()) {
            Text("Todavía no hay pedidos en esta sucursal", Modifier.testTag(ShopTestTags.EMPTY))
        }
        LazyColumn(Modifier.weight(1f).testTag("shop_orders_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OrderStatus.entries.forEach { status ->
                val orders = state.orders.filter { it.status == status }
                if (orders.isNotEmpty()) {
                    item(key = status.name) { Text("${if (status == OrderStatus.READY) "Listos" else status.label(Fulfilment.PICKUP)} (${orders.size})", style = MaterialTheme.typography.titleMedium) }
                    items(orders, key = { it.id }) { order ->
                        Card(Modifier.fillMaxWidth().testTag(ShopTestTags.order(order.id, order.status))) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Pedido #${order.number}", style = MaterialTheme.typography.titleMedium)
                                Text(if (order.fulfilment == Fulfilment.PICKUP) "Recoger en tienda" else "Entrega a domicilio")
                                Text(order.status.label(order.fulfilment))
                                order.items.forEach { Text("${it.quantity} × ${it.name}") }
                                order.delivery?.let { Text(it.address) }
                                Text("Total: ${formatPrice(order.total)}")
                                nextShopStatus(order.status, order.fulfilment)?.let { next ->
                                    Button(
                                        onClick = { if (next == OrderStatus.PICKED_UP) confirmPickup = order else onAdvance(order, null) },
                                        enabled = state.updatingId == null,
                                        modifier = Modifier.testTag(ShopTestTags.advance(order.id))
                                    ) {
                                        Text(if (state.updatingId == order.id) "Guardando…" else when (next) {
                                            OrderStatus.ACCEPTED -> "Aceptar pedido"
                                            OrderStatus.PREPARING -> "Comenzar preparación"
                                            OrderStatus.READY -> "Marcar listo"
                                            else -> "Confirmar recogida"
                                        })
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
