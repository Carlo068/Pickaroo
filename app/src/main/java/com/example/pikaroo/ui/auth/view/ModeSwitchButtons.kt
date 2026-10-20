package com.example.pikaroo.ui.auth.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.navigation.NavTestTags

fun RoleMode.switchLabel(): String = when (this) {
    RoleMode.SHOP -> "Cambiar a modo tienda"
    RoleMode.DRIVER -> "Cambiar a modo repartidor"
    RoleMode.CUSTOMER -> "Cambiar a modo cliente"
}

@Composable
fun ModeSwitchButtons(
    modes: List<RoleMode>,
    onSwitch: (RoleMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { mode ->
            OutlinedButton(
                onClick = { onSwitch(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NavTestTags.switchTo(mode)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(mode.switchLabel())
            }
        }
    }
}
