package com.example.pikaroo.ui.orders.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.orders.model.Fulfilment
import com.example.pikaroo.ui.orders.model.OrderDto
import com.example.pikaroo.ui.orders.model.label
import com.example.pikaroo.ui.orders.viewmodel.OrdersUiState
import com.example.pikaroo.ui.orders.viewmodel.OrdersViewModel
import com.example.pikaroo.ui.theme.PikarooBackground
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

object OrdersTestTags {
    const val EMPTY = "orders_empty"
    const val ERROR = "orders_error"
    fun row(orderId: String) = "orders_row_$orderId"
}

@Composable
fun OrdersView(onBack: () -> Unit, onOpenOrder: (String) -> Unit) {
    val viewModel: OrdersViewModel = viewModel(factory = OrdersViewModel.Factory)
    val state by viewModel.state.collectAsState()

    OrdersContent(state = state, onBack = onBack, onOpenOrder = onOpenOrder)
}

@Composable
fun OrdersContent(state: OrdersUiState, onBack: () -> Unit, onOpenOrder: (String) -> Unit) {
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
            Text("Mis pedidos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        when {
            state.isLoading && state.orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = PikarooOrange)
            }

            state.error != null && state.orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(state.error, color = Color.Red, modifier = Modifier.testTag(OrdersTestTags.ERROR))
            }

            state.orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Todavía no tienes pedidos", color = PikarooTextGray, modifier = Modifier.testTag(OrdersTestTags.EMPTY))
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderRow(order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(OrdersTestTags.row(order.id)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pedido #${order.number}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (order.fulfilment == Fulfilment.PICKUP) "Recoger en tienda" else "Entrega a domicilio",
                    fontSize = 12.sp,
                    color = PikarooTextGray
                )
                Text(
                    text = order.status.label(order.fulfilment),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PikarooOrange
                )
            }
            Text(formatPrice(order.total), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
