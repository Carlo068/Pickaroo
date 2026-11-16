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
import androidx.compose.ui.res.stringResource
import com.example.pikaroo.R
import com.example.pikaroo.common.text.UiText
import com.example.pikaroo.common.text.asString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderView(
    cartItemCount: Int = 0,
    cart: CartUiState = CartUiState(),
    isSaving: Boolean = false,
    saveError: UiText? = null,
    onLookupOrder: (() -> Unit)? = null,
    onOpenLastOrder: (() -> Unit)? = null,
    onConfirmOrder: ((OrderState) -> Unit)? = null,
    onOpenCart: () -> Unit = {},
    viewModel: OrderViewModel = viewModel()
) {
    val state = viewModel.state
    var showReview by remember { mutableStateOf(false) }
    if (showReview && state.canReview(cart)) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text(stringResource(R.string.checkout_review_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.checkout_review_message,
                        if (state.method == DeliveryMethod.PICKUP) stringResource(R.string.checkout_review_pickup)
                        else stringResource(R.string.checkout_review_delivery, state.address.trim()),
                        formatPrice(state.total(cart)),
                        stringResource(
                            if (state.method == DeliveryMethod.PICKUP) R.string.checkout_pay_in_store
                            else R.string.checkout_pay_on_delivery
                        )
                    )
                )
            },
            confirmButton = {
                if (onConfirmOrder != null) {
                    TextButton(onClick = {
                        showReview = false
                        if (!isSaving) onConfirmOrder(state.copy(address = state.address.trim()))
                    }) { Text(stringResource(R.string.checkout_confirm)) }
                } else {
                    TextButton(onClick = { showReview = false }) { Text(stringResource(R.string.checkout_back_to_summary)) }
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
                    placeholder = { Text(stringResource(R.string.checkout_search_placeholder)) },
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
                .imePadding()
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
                        text = stringResource(R.string.checkout_method_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.checkout_method_subtitle),
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
                    text = stringResource(R.string.checkout_method_select),
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
                        title = stringResource(R.string.checkout_method_pickup),
                        subtitle = if (state.method == DeliveryMethod.PICKUP) stringResource(R.string.checkout_method_selected) else stringResource(R.string.checkout_method_pickup_hint),
                        onClick = { if (!isSaving) viewModel.selectMethod(DeliveryMethod.PICKUP) }
                    )
                    DeliveryMethodCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalShipping,
                        title = stringResource(R.string.checkout_method_delivery),
                        subtitle = if (state.method == DeliveryMethod.DELIVERY) stringResource(R.string.checkout_method_selected) else stringResource(R.string.checkout_method_delivery_hint),
                        onClick = { if (!isSaving) viewModel.selectMethod(DeliveryMethod.DELIVERY) }
                    )
                }
                Spacer(Modifier.height(20.dp))
                if (state.method == DeliveryMethod.PICKUP) {
                    Text(stringResource(R.string.checkout_pickup_info))
                    Text(stringResource(R.string.checkout_pickup_payment))
                } else if (state.method == DeliveryMethod.DELIVERY) {
                    Text(stringResource(R.string.checkout_delivery_payment))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.address,
                        enabled = !isSaving,
                        onValueChange = viewModel::updateAddress,
                        label = { Text(stringResource(R.string.checkout_address_label)) },
                        placeholder = { Text(stringResource(R.string.checkout_address_placeholder)) },
                        supportingText = {
                            Text(if (state.address.isBlank()) {
                                stringResource(R.string.checkout_address_required)
                            } else {
                                stringResource(R.string.checkout_address_hint)
                            })
                        },
                        isError = state.address.isNotEmpty() && state.address.isBlank(),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address")
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.checkout_summary_title), style = MaterialTheme.typography.titleLarge)
                if (cart.isEmpty) Text(stringResource(R.string.checkout_cart_empty))
                cart.items.forEach { item ->
                    Text("${item.quantity} × ${item.name} · ${formatPrice(item.lineTotal)}")
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.checkout_subtotal_line, formatPrice(cart.subtotal)))
                Text(stringResource(R.string.checkout_delivery_line, formatPrice(state.deliveryFee(cart))))
                Text(stringResource(R.string.checkout_total_line, formatPrice(state.total(cart))),
                    modifier = Modifier.testTag("checkout_total"),
                    fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showReview = true },
                    enabled = !isSaving && state.canReview(cart),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                        .testTag("checkout_review")
                ) { Text(if (isSaving) stringResource(R.string.checkout_saving) else stringResource(R.string.checkout_review_button)) }
                saveError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                onLookupOrder?.let { open ->
                    TextButton(onClick = open, enabled = !isSaving) { Text(stringResource(R.string.checkout_lookup_qr)) }
                }
                onOpenLastOrder?.let { open ->
                    TextButton(onClick = open, enabled = !isSaving) { Text(stringResource(R.string.checkout_last_order)) }
                }
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
