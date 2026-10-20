package com.example.pikaroo.ui.orders.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.OrderStatus
import com.example.pikaroo.ui.orders.model.StepState
import com.example.pikaroo.ui.orders.model.TimelineStep
import com.example.pikaroo.ui.orders.viewmodel.TrackingUiState
import com.example.pikaroo.ui.orders.viewmodel.TrackingViewModel
import com.example.pikaroo.ui.theme.PikarooBackground
import com.example.pikaroo.ui.theme.PikarooLightOrange
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

object TrackingTestTags {
    const val ERROR = "tracking_error"
    const val PICKUP_CODE = "tracking_pickup_code"
    const val TITLE = "tracking_title"
    fun step(status: OrderStatus, state: StepState) = "tracking_step_${status.name}_${state.name}"
}

@Composable
fun TrackingView(orderId: String, onBack: () -> Unit) {
    val viewModel: TrackingViewModel = viewModel(
        key = "tracking_$orderId",
        factory = TrackingViewModel.factory(orderId)
    )
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.poll() }
    }

    TrackingContent(state = state, onBack = onBack)
}

@Composable
fun TrackingContent(state: TrackingUiState, onBack: () -> Unit) {
    val order = state.order

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PikarooBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
            Text(
                text = if (order != null) "Pedido #${order.number}" else "Pedido",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.testTag(TrackingTestTags.TITLE)
            )
        }

        if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.error != null) {
                    Text(state.error, color = Color.Red, modifier = Modifier.testTag(TrackingTestTags.ERROR))
                } else {
                    CircularProgressIndicator(color = PikarooOrange)
                }
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.error != null) {
                Text(
                    text = state.error,
                    color = PikarooTextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag(TrackingTestTags.ERROR)
                )
            }

            TimelineCard(order, state.timeline)

            if (order.fulfilment == Fulfilment.PICKUP && order.pickup != null && !order.status.isFinal) {
                PickupCodeCard(order)
            }

            SummaryCard(order)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimelineCard(order: OrderDto, steps: List<TimelineStep>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (order.fulfilment == Fulfilment.PICKUP) "Recoger en tienda" else "Entrega a domicilio",
                fontSize = 14.sp,
                color = PikarooTextGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            steps.forEachIndexed { index, step ->
                TimelineRow(step, isLast = index == steps.lastIndex)
            }
        }
    }
}

@Composable
private fun TimelineRow(step: TimelineStep, isLast: Boolean) {
    val accent = when (step.state) {
        StepState.DONE, StepState.CURRENT -> PikarooOrange
        StepState.CANCELLED -> Color(0xFFD32F2F)
        StepState.PENDING -> Color(0xFFD0D0D0)
    }

    Row(modifier = Modifier.testTag(TrackingTestTags.step(step.status, step.state))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (step.state == StepState.PENDING) Color.White else accent, CircleShape)
                    .background(if (step.state == StepState.CURRENT) PikarooLightOrange else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (step.state) {
                    StepState.DONE -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    StepState.CANCELLED -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    StepState.CURRENT -> Box(Modifier.size(10.dp).background(PikarooOrange, CircleShape))
                    StepState.PENDING -> Box(Modifier.size(10.dp).background(accent, CircleShape))
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (step.state == StepState.DONE) PikarooOrange else Color(0xFFE0E0E0))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(
                text = step.label,
                fontSize = 16.sp,
                fontWeight = if (step.state == StepState.CURRENT) FontWeight.Bold else FontWeight.Medium,
                color = if (step.state == StepState.PENDING) PikarooTextGray else Color.Black
            )
            val time = formatTime(step.at)
            if (time != null) {
                Text(text = time, fontSize = 12.sp, color = PikarooTextGray)
            }
        }
    }
}

@Composable
private fun PickupCodeCard(order: OrderDto) {
    val pickup = order.pickup ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PikarooLightOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tu código de recolección", fontSize = 14.sp, color = PikarooTextGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pickup.code,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PikarooOrange,
                modifier = Modifier.testTag(TrackingTestTags.PICKUP_CODE)
            )
            val slot = formatTime(pickup.slotStart)
            if (slot != null) {
                Text("Horario: $slot", fontSize = 13.sp, color = PikarooTextGray)
            }
        }
    }
}

@Composable
private fun SummaryCard(order: OrderDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resumen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            order.items.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${line.quantity} × ${line.name}", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text(formatPrice(line.lineTotal), fontSize = 14.sp)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
            SummaryLine("Subtotal", formatPrice(order.subtotal))
            if (order.fulfilment == Fulfilment.DELIVERY) {
                SummaryLine("Envío", formatPrice(order.deliveryFee))
                order.delivery?.let { Text(it.address, fontSize = 12.sp, color = PikarooTextGray) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SummaryLine("Total", formatPrice(order.total), bold = true)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
