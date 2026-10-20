package com.example.pikaroo.ui.order.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.order.model.DeliveryMethod
import com.example.pikaroo.ui.order.model.OrderState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.cart.view.CartIconButton
import com.example.pikaroo.ui.theme.PikarooTheme
import com.example.pikaroo.ui.order.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderView(
    cartItemCount: Int = 0,
    cart: CartUiState = CartUiState(),
    onConfirmOrder: ((OrderState) -> Unit)? = null,
    onOpenCart: () -> Unit = {},
    viewModel: OrderViewModel = viewModel()
) {
    val state = viewModel.state
    var showReview by remember { mutableStateOf(false) }
    if (showReview && state.canReview(cart)) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text("Revisa tu pedido") },
            text = { Text("${if (state.method == DeliveryMethod.PICKUP) "Pickup · Sucursal Centro · Recoger hoy" else "Domicilio · ${state.address.trim()}"}\nTotal: ${formatPrice(state.total(cart))}\nPago pendiente: ${if (state.method == DeliveryMethod.PICKUP) "en tienda" else "contra entrega"}") },
            confirmButton = {
                if (onConfirmOrder != null) {
                    TextButton(onClick = {
                        showReview = false
                        onConfirmOrder(state.copy(address = state.address.trim()))
                    }) { Text("Confirmar pedido") }
                } else {
                    TextButton(onClick = { showReview = false }) { Text("Volver al resumen") }
                }
            }
        )
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text("Buscar...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                CartIconButton(itemCount = cartItemCount, onClick = onOpenCart)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF7F2), Color.White)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Método de Entrega",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Elige cómo recibir tu pedido",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Selecciona el método",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeliveryMethodCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Storefront,
                        title = "Pickup",
                        subtitle = if (state.method == DeliveryMethod.PICKUP) "Seleccionado" else "Recoge en sucursal",
                        onClick = { viewModel.selectMethod(DeliveryMethod.PICKUP) }
                    )
                    DeliveryMethodCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        title = "Domicilio",
                        subtitle = if (state.method == DeliveryMethod.DELIVERY) "Seleccionado" else "Envío a domicilio",
                        onClick = { viewModel.selectMethod(DeliveryMethod.DELIVERY) }
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (state.method == DeliveryMethod.PICKUP) {
                    Text("Sucursal Centro · Recoger hoy (ejemplo)")
                    Text("Pago en tienda · pendiente")
                } else if (state.method == DeliveryMethod.DELIVERY) {
                    Text("Pago contra entrega · pendiente")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.address,
                        onValueChange = viewModel::updateAddress,
                        label = { Text("Dirección de entrega") },
                        placeholder = { Text("Calle, número, colonia y ciudad") },
                        supportingText = {
                            Text(if (state.address.isBlank()) {
                                "Agrega una dirección para continuar"
                            } else {
                                "Incluye número interior si corresponde"
                            })
                        },
                        isError = state.address.isNotEmpty() && state.address.isBlank(),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address")
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text("Resumen del pedido", style = MaterialTheme.typography.titleLarge)
                if (cart.isEmpty) Text("Tu carrito está vacío")
                cart.items.forEach { item ->
                    Text("${item.quantity} × ${item.name} · ${formatPrice(item.lineTotal)}")
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Subtotal: ${formatPrice(cart.subtotal)}")
                Text("Envío: ${formatPrice(state.deliveryFee(cart))}")
                Text("Total: ${formatPrice(state.total(cart))}",
                    modifier = Modifier.testTag("checkout_total"),
                    fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showReview = true },
                    enabled = state.canReview(cart),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                        .testTag("checkout_review")
                ) { Text("Revisar pedido") }
            }
        }
    }
}

@Composable
fun DeliveryMethodCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color(0xFFF5F5F5)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OrderViewPreview() {
    PikarooTheme {
        OrderView()
    }
}
