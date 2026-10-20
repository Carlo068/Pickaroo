package com.example.pikaroo.ui.auth.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.theme.PikarooOrange
import com.example.pikaroo.ui.theme.PikarooTextGray

@Composable
fun ModeHomeScreen(
    title: String,
    description: String,
    userName: String,
    otherModes: List<RoleMode>,
    onSwitchMode: (RoleMode) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = userName, fontSize = 16.sp, color = PikarooTextGray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, fontSize = 14.sp, color = PikarooTextGray)

        Spacer(modifier = Modifier.height(32.dp))

        ModeSwitchButtons(modes = otherModes, onSwitch = onSwitchMode)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cerrar sesión", color = Color.White)
        }
    }
}
