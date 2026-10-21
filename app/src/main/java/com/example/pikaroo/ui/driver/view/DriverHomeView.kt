package com.example.pikaroo.ui.driver.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeSwitchButtons
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeState
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeViewModel
import com.example.pikaroo.ui.orders.model.MapPoint
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.label
import com.example.pikaroo.ui.orders.view.OsmMapView
import com.example.pikaroo.ui.theme.PikarooBackground
import com.example.pikaroo.ui.theme.PikarooLightOrange
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

object DriverTestTags {
    const val ONLINE_SWITCH = "driver_online_switch"
    const val EMPTY = "driver_empty"
    const val OFFLINE = "driver_offline"
    const val MESSAGE = "driver_message"
    const val MESSAGE_DISMISS = "driver_message_dismiss"
    const val ERROR = "driver_error"
    const val PICKUP_BUTTON = "driver_pickup_button"
    const val DELIVER_BUTTON = "driver_deliver_button"
    fun available(orderId: String) = "driver_available_$orderId"
    fun accept(orderId: String) = "driver_accept_$orderId"
    fun current(orderId: String) = "driver_current_$orderId"
}

@Composable
fun DriverHomeView(
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: DriverHomeViewModel = viewModel(factory = DriverHomeViewModel.Factory)
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.poll() }
    }

    DriverHomeContent(
        state = state,
        userName = userName,
        otherModes = otherModes,
        onSwitchMode = onSwitchMode,
        onLogout = onLogout,
        onOnlineChange = viewModel::setOnline,
        onAccept = viewModel::accept,
        onPickup = viewModel::pickUp,
        onDeliver = viewModel::deliver,
        onDismissMessage = viewModel::dismissMessage
    )
}

@Composable
fun DriverHomeContent(
    state: DriverHomeState,
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit,
    onOnlineChange: (Boolean) -> Unit,
    onAccept: (String) -> Unit,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    onDismissMessage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PikarooBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header(userName, state.isOnline, onOnlineChange) }

        state.message?.let { message ->
            item { MessageCard(message, onDismissMessage) }
        }
        state.error?.let { error ->
            item { Text(error, color = PikarooTextGray, fontSize = 13.sp, modifier = Modifier.testTag(DriverTestTags.ERROR)) }
        }

        when {
            state.current != null -> item { CurrentDeliveryCard(state.current, state.acting, onPickup, onDeliver) }

            !state.isOnline -> item {
                Text(
                    "Estás desconectado. Activa «Disponible» para ver pedidos.",
                    color = PikarooTextGray,
                    modifier = Modifier.testTag(DriverTestTags.OFFLINE)
                )
            }

            state.isLoading -> item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = PikarooOrange)
                }
            }

            state.available.isEmpty() -> item {
                Text(
                    "No hay pedidos disponibles por ahora",
                    color = PikarooTextGray,
                    modifier = Modifier.testTag(DriverTestTags.EMPTY)
                )
            }

            else -> items(state.available, key = { it.id }) { order ->
                AvailableOrderCard(
                    order = order,
                    busy = state.acceptingId != null,
                    accepting = state.acceptingId == order.id,
                    onAccept = { onAccept(order.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            ModeSwitchButtons(modes = otherModes, onSwitch = onSwitchMode)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar sesión", color = Color.White)
            }
        }
    }
}

@Composable
private fun Header(userName: String, isOnline: Boolean, onOnlineChange: (Boolean) -> Unit) {
    Column {
        Text("Modo repartidor", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(userName, fontSize = 14.sp, color = PikarooTextGray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Disponible", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = isOnline,
                onCheckedChange = onOnlineChange,
                colors = SwitchDefaults.colors(checkedTrackColor = PikarooOrange),
                modifier = Modifier.testTag(DriverTestTags.ONLINE_SWITCH)
            )
        }
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PikarooLightOrange)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f).testTag(DriverTestTags.MESSAGE), fontSize = 14.sp)
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(DriverTestTags.MESSAGE_DISMISS)) {
                Text("Entendido", color = PikarooOrange)
            }
        }
    }
}

@Composable
private fun AvailableOrderCard(order: AvailableOrderDto, busy: Boolean, accepting: Boolean, onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.available(order.id)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pedido #${order.number}", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Ganas ${formatPrice(order.deliveryFee)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PikarooOrange)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recoger en", fontSize = 12.sp, color = PikarooTextGray)
            Text("${order.shop.name} · ${order.shop.address}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Entregar en", fontSize = 12.sp, color = PikarooTextGray)
            Text(order.delivery.address, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${order.itemsCount} productos", fontSize = 12.sp, color = PikarooTextGray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.accept(order.id)),
                colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (accepting) "Aceptando…" else "Aceptar", color = Color.White)
            }
        }
    }
}

@Composable
private fun CurrentDeliveryCard(order: OrderDto, acting: Boolean, onPickup: () -> Unit, onDeliver: () -> Unit) {
    val goingToShop = order.status == OrderStatus.DRIVER_ASSIGNED
    val shop = order.shop
    val destination = order.delivery

    Card(
        modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.current(order.id)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PikarooLightOrange)
    ) {
        Column {
            if (shop != null && destination != null) {
                OsmMapView(
                    shop = MapPoint(shop.lat, shop.lng),
                    destination = MapPoint(destination.lat, destination.lng),
                    driver = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Entrega en curso", fontSize = 14.sp, color = PikarooTextGray)
                Text("Pedido #${order.number}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(order.status.label(order.fulfilment), fontSize = 14.sp, color = PikarooOrange, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (goingToShop) "Ve a recoger el pedido a" else "Entrega el pedido en",
                    fontSize = 12.sp,
                    color = PikarooTextGray
                )
                Text(
                    text = if (goingToShop) shop?.let { "${it.name} · ${it.address}" }.orEmpty() else destination?.address.orEmpty(),
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = if (goingToShop) onPickup else onDeliver,
                    enabled = !acting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (goingToShop) DriverTestTags.PICKUP_BUTTON else DriverTestTags.DELIVER_BUTTON),
                    colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            acting -> "Guardando…"
                            goingToShop -> "Recogí el pedido"
                            else -> "Entregado"
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}
