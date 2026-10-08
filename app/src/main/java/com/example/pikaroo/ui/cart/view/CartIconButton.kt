package com.example.pikaroo.ui.cart.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pikaroo.ui.theme.PikarooOrange

const val CART_BADGE_TAG = "cart_badge"

@Composable
fun CartIconButton(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (itemCount > 0) {
                    Badge(
                        containerColor = PikarooOrange,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (itemCount > 99) "99+" else itemCount.toString(),
                            modifier = Modifier.testTag(CART_BADGE_TAG)
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = "Carrito",
                tint = Color.Black,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
