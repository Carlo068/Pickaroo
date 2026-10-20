package com.example.pikaroo.ui.driver.view

import androidx.compose.runtime.Composable
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.view.ModeHomeScreen

@Composable
fun DriverHomeView(
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit
) {
    ModeHomeScreen(
        title = "Modo repartidor",
        description = "Aquí verás los pedidos disponibles para entregar.",
        userName = userName,
        otherModes = otherModes,
        onSwitchMode = onSwitchMode,
        onLogout = onLogout
    )
}
