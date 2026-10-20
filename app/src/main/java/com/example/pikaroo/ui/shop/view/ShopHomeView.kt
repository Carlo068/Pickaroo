package com.example.pikaroo.ui.shop.view

import androidx.compose.runtime.Composable
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeHomeScreen

@Composable
fun ShopHomeView(
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit
) {
    ModeHomeScreen(
        title = "Modo tienda",
        description = "Aquí llegarán los pedidos de tu tienda.",
        userName = userName,
        otherModes = otherModes,
        onSwitchMode = onSwitchMode,
        onLogout = onLogout
    )
}
