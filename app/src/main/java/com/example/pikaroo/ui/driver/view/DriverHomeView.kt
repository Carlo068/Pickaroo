package com.example.pikaroo.ui.driver.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.BuildConfig
import com.example.pikaroo.appContainer
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeSwitchButtons
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.driver.location.DeliveryTrackingService
import com.example.pikaroo.ui.driver.location.LocationMode
import com.example.pikaroo.ui.driver.location.toMapPoint
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeState
import com.example.pikaroo.ui.driver.viewmodel.DriverHomeViewModel
import com.example.pikaroo.ui.orders.model.MapPoint
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.labelRes
import com.example.pikaroo.ui.orders.view.OsmMapView
import com.example.pikaroo.ui.theme.PikarooBackground
import com.example.pikaroo.ui.theme.PikarooLightOrange
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.common.text.asString

object DriverTestTags {
    const val ONLINE_SWITCH = "driver_online_switch"
    const val EMPTY = "driver_empty"
    const val OFFLINE = "driver_offline"
    const val MESSAGE = "driver_message"
    const val MESSAGE_DISMISS = "driver_message_dismiss"
    const val ERROR = "driver_error"
    const val PICKUP_BUTTON = "driver_pickup_button"
    const val DELIVER_BUTTON = "driver_deliver_button"
    const val TRACKING_ACTIVE = "driver_tracking_active"
    const val LOCATION_PERMISSION = "driver_location_permission"
    const val LOCATION_PERMISSION_BUTTON = "driver_location_permission_button"
    const val SIMULATE_SWITCH = "driver_simulate_switch"
    fun available(orderId: String) = "driver_available_$orderId"
    fun accept(orderId: String) = "driver_accept_$orderId"
    fun current(orderId: String) = "driver_current_$orderId"
}

/** Lo relativo a compartir la ubicación, ya resuelto para dibujar. */
data class DriverTrackingUi(
    val isSharing: Boolean = false,
    val needsPermission: Boolean = false,
    val simulate: Boolean = false,
    val showSimulateToggle: Boolean = false,
    val driverPosition: MapPoint? = null
)

private fun hasLocationPermission(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun requestedPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

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
    val context = LocalContext.current
    val container = context.appContainer()
    val trackingState by container.driverTracking.state.collectAsState()
    val mode by container.driverLocationPreferences.mode.collectAsState()

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermission = hasLocationPermission(context)
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.poll() }
    }

    val activeOrderId = state.current
        ?.takeIf { it.status == OrderStatus.DRIVER_ASSIGNED || it.status == OrderStatus.OUT_FOR_DELIVERY }
        ?.id

    // El servicio comparte la ubicación mientras haya una entrega en curso, también con la app en segundo plano.
    LaunchedEffect(activeOrderId, hasPermission, mode, state.isLoading) {
        when {
            activeOrderId != null && hasPermission -> DeliveryTrackingService.start(context, activeOrderId)
            activeOrderId == null && !state.isLoading -> DeliveryTrackingService.stop(context)
        }
    }

    val tracking = DriverTrackingUi(
        isSharing = trackingState.isActive && trackingState.orderId == activeOrderId,
        needsPermission = activeOrderId != null && !hasPermission,
        simulate = mode == LocationMode.SIMULATED,
        showSimulateToggle = BuildConfig.DEBUG,
        driverPosition = trackingState.lastFix
            ?.takeIf { activeOrderId != null && trackingState.orderId == activeOrderId }
            ?.toMapPoint()
    )

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
        onDismissMessage = viewModel::dismissMessage,
        tracking = tracking,
        onRequestPermission = { permissionLauncher.launch(requestedPermissions()) },
        onSimulateChange = { container.driverLocationPreferences.setMode(if (it) LocationMode.SIMULATED else LocationMode.GPS) }
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
    onDismissMessage: () -> Unit,
    tracking: DriverTrackingUi = DriverTrackingUi(),
    onRequestPermission: () -> Unit = {},
    onSimulateChange: (Boolean) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PikarooBackground)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Header(userName, state.isOnline, onOnlineChange, tracking, onSimulateChange) }

        state.message?.let { message ->
            item { MessageCard(message, onDismissMessage) }
        }
        state.error?.let { error ->
            item { Text(error.asString(), color = PikarooTextGray, fontSize = 13.sp, modifier = Modifier.testTag(DriverTestTags.ERROR)) }
        }

        when {
            state.current != null -> item {
                CurrentDeliveryCard(state.current, state.acting, onPickup, onDeliver, tracking, onRequestPermission)
            }

            !state.isOnline -> item {
                Text(
                    stringResource(R.string.driver_offline_hint),
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
                    stringResource(R.string.driver_no_orders),
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
                Text(stringResource(R.string.common_logout), color = Color.White)
            }
        }
    }
}

@Composable
private fun Header(
    userName: String,
    isOnline: Boolean,
    onOnlineChange: (Boolean) -> Unit,
    tracking: DriverTrackingUi,
    onSimulateChange: (Boolean) -> Unit
) {
    Column {
        Text(stringResource(R.string.driver_mode_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(userName, fontSize = 14.sp, color = PikarooTextGray)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.driver_available), modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = isOnline,
                onCheckedChange = onOnlineChange,
                colors = SwitchDefaults.colors(checkedTrackColor = PikarooOrange),
                modifier = Modifier.testTag(DriverTestTags.ONLINE_SWITCH)
            )
        }
        if (tracking.showSimulateToggle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.driver_simulate_route), modifier = Modifier.weight(1f), fontSize = 13.sp, color = PikarooTextGray)
                Switch(
                    checked = tracking.simulate,
                    onCheckedChange = onSimulateChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = PikarooOrange),
                    modifier = Modifier.testTag(DriverTestTags.SIMULATE_SWITCH)
                )
            }
        }
    }
}

@Composable
private fun MessageCard(message: UiText, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PikarooLightOrange)
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message.asString(), modifier = Modifier.weight(1f).testTag(DriverTestTags.MESSAGE), fontSize = 14.sp)
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(DriverTestTags.MESSAGE_DISMISS)) {
                Text(stringResource(R.string.driver_message_dismiss), color = PikarooOrange)
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
                Text(stringResource(R.string.order_number, order.number), modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.driver_earnings, formatPrice(order.deliveryFee)), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PikarooOrange)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.driver_pickup_at), fontSize = 12.sp, color = PikarooTextGray)
            Text("${order.shop.name} · ${order.shop.address}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(stringResource(R.string.driver_deliver_to), fontSize = 12.sp, color = PikarooTextGray)
            Text(order.delivery.address, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(pluralStringResource(R.plurals.driver_items_count, order.itemsCount, order.itemsCount), fontSize = 12.sp, color = PikarooTextGray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.accept(order.id)),
                colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(if (accepting) R.string.driver_accepting else R.string.driver_accept), color = Color.White)
            }
        }
    }
}

@Composable
private fun CurrentDeliveryCard(
    order: OrderDto,
    acting: Boolean,
    onPickup: () -> Unit,
    onDeliver: () -> Unit,
    tracking: DriverTrackingUi,
    onRequestPermission: () -> Unit
) {
    val goingToShop = order.status == OrderStatus.DRIVER_ASSIGNED
    val shop = order.shop
    val destination = order.delivery

    Card(
        modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.current(order.id)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PikarooLightOrange)
    ) {
        Column {
            if (shop != null && destination?.lat != null && destination.lng != null) {
                OsmMapView(
                    shop = MapPoint(shop.lat, shop.lng),
                    destination = MapPoint(destination.lat, destination.lng),
                    driver = tracking.driverPosition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (tracking.needsPermission) {
                    LocationPermissionPrompt(onRequestPermission)
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (tracking.isSharing) {
                    SharingIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(stringResource(R.string.driver_current_delivery), fontSize = 14.sp, color = PikarooTextGray)
                Text(stringResource(R.string.order_number, order.number), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(order.status.labelRes(order.fulfilment)), fontSize = 14.sp, color = PikarooOrange, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(if (goingToShop) R.string.driver_go_to_shop else R.string.driver_deliver_at),
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
                            acting -> stringResource(R.string.driver_saving)
                            goingToShop -> stringResource(R.string.driver_picked_up_button)
                            else -> stringResource(R.string.driver_delivered_button)
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SharingIndicator() {
    Row(
        modifier = Modifier.testTag(DriverTestTags.TRACKING_ACTIVE),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(Color(0xFF43A047), CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.driver_sharing_location), fontSize = 13.sp, color = Color(0xFF2E7D32))
    }
}

@Composable
private fun LocationPermissionPrompt(onRequestPermission: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.driver_location_prompt),
            fontSize = 14.sp,
            modifier = Modifier.testTag(DriverTestTags.LOCATION_PERMISSION)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().testTag(DriverTestTags.LOCATION_PERMISSION_BUTTON),
            colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.driver_allow_location), color = Color.White)
        }
    }
}
