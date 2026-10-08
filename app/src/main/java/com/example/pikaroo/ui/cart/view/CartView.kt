package com.example.pikaroo.ui.cart.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pikaroo.ui.cart.model.CartItem
import com.example.pikaroo.ui.cart.model.CartUiState
import com.example.pikaroo.ui.cart.model.formatPrice
import com.example.pikaroo.ui.cart.viewmodel.CartViewModel
import com.example.pikaroo.ui.theme.PikarooBackground
import com.example.pikaroo.ui.theme.PikarooLightOrange
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

object CartTestTags {
    const val EMPTY = "cart_empty"
    const val SUBTOTAL = "cart_subtotal"
    const val DELIVERY_FEE = "cart_delivery_fee"
    const val TOTAL = "cart_total"
    const val CHECKOUT = "cart_checkout"
    fun quantity(productId: String) = "cart_qty_$productId"
    fun increment(productId: String) = "cart_inc_$productId"
    fun decrement(productId: String) = "cart_dec_$productId"
    fun remove(productId: String) = "cart_remove_$productId"
}

@Composable
fun CartView(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val state by cartViewModel.uiState.collectAsState()
    CartContent(
        state = state,
        onBack = onBack,
        onIncrement = cartViewModel::increment,
        onDecrement = cartViewModel::decrement,
        onRemove = cartViewModel::remove,
        onCheckout = onCheckout
    )
}

@Composable
fun CartContent(
    state: CartUiState,
    onBack: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit
) {
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
                text = "Mi carrito",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            if (!state.isEmpty) {
                Text(
                    text = "${state.itemCount} artículos",
                    fontSize = 14.sp,
                    color = PikarooTextGray,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }

        if (state.isEmpty) {
            EmptyCart(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.productId }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { onIncrement(item.productId) },
                        onDecrement = { onDecrement(item.productId) },
                        onRemove = { onRemove(item.productId) }
                    )
                }
            }
            CartSummary(state = state, onCheckout = onCheckout)
        }
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .testTag(CartTestTags.EMPTY),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(PikarooLightOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = null,
                tint = PikarooOrange,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Tu carrito está vacío",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Agrega productos para empezar tu pedido",
            fontSize = 14.sp,
            color = PikarooTextGray
        )
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatPrice(item.price),
                    fontSize = 13.sp,
                    color = PikarooTextGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                QuantityStepper(
                    item = item,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag(CartTestTags.remove(item.productId))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar ${item.name}",
                        tint = Color(0xFFE57373)
                    )
                }
                Text(
                    text = formatPrice(item.lineTotal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            onClick = onDecrement,
            background = Color.Transparent,
            modifier = Modifier.testTag(CartTestTags.decrement(item.productId))
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Quitar uno", modifier = Modifier.size(16.dp))
        }
        Text(
            text = item.quantity.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .testTag(CartTestTags.quantity(item.productId))
        )
        StepperButton(
            onClick = onIncrement,
            background = PikarooOrange,
            modifier = Modifier.testTag(CartTestTags.increment(item.productId))
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Agregar uno",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun StepperButton(
    onClick: () -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun CartSummary(state: CartUiState, onCheckout: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryRow("Subtotal", formatPrice(state.subtotal), CartTestTags.SUBTOTAL)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("Envío", formatPrice(state.deliveryFee), CartTestTags.DELIVERY_FEE)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            SummaryRow("Total", formatPrice(state.total), CartTestTags.TOTAL, emphasized = true)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(CartTestTags.CHECKOUT),
                colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continuar al pago",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, tag: String, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = if (emphasized) 18.sp else 15.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) Color.Black else PikarooTextGray
        )
        Text(
            text = value,
            modifier = Modifier.testTag(tag),
            fontSize = if (emphasized) 18.sp else 15.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = Color.Black
        )
    }
}
