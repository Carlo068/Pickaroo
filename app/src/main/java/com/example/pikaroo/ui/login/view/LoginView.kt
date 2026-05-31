package com.example.pikaroo.ui.login.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.login.viewmodel.LoginViewModel
import com.example.pikaroo.ui.theme.*

@Composable
fun LoginView(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val isLogin = viewModel.isLoginTab
    val loginState = viewModel.loginState
    val registerState = viewModel.registerState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(PikarooOrange)
        ) {
            // Background circles decorative
            Surface(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = 280.dp, y = (-60).dp),
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(100)
            ) {}

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.BottomStart)
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = PikarooOrange,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isLogin) "Bienvenido" else "Crear cuenta",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isLogin) "Inicia sesión para continuar comprando" else "Regístrate y empieza a comprar",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Selector
            TabSelector(
                isLogin = isLogin,
                onTabSelected = { viewModel.toggleTab(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            if (!isLogin) {
                LoginTextField(
                    value = registerState.name,
                    onValueChange = { viewModel.onRegisterNameChange(it) },
                    label = "Nombre completo",
                    placeholder = "Tu nombre",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            LoginTextField(
                value = if (isLogin) loginState.email else registerState.email,
                onValueChange = { if (isLogin) viewModel.onLoginEmailChange(it) else viewModel.onRegisterEmailChange(it) },
                label = "Correo electrónico",
                placeholder = "correo@ejemplo.com",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoginTextField(
                value = if (isLogin) loginState.password else registerState.password,
                onValueChange = { if (isLogin) viewModel.onLoginPasswordChange(it) else viewModel.onRegisterPasswordChange(it) },
                label = "Contraseña",
                placeholder = ".........",
                icon = Icons.Default.Lock,
                isPassword = true,
                isPasswordVisible = if (isLogin) loginState.isPasswordVisible else registerState.isPasswordVisible,
                onPasswordToggle = { if (isLogin) viewModel.toggleLoginPasswordVisibility() else viewModel.toggleRegisterPasswordVisibility() }
            )

            if (!isLogin) {
                Spacer(modifier = Modifier.height(16.dp))
                LoginTextField(
                    value = registerState.confirmPassword,
                    onValueChange = { viewModel.onRegisterConfirmPasswordChange(it) },
                    label = "Confirmar contraseña",
                    placeholder = ".........",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    isPasswordVisible = registerState.isPasswordVisible,
                    onPasswordToggle = { viewModel.toggleRegisterPasswordVisibility() }
                )
            }

            if (isLogin) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = PikarooOrange,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                        .clickable { /* Handle forgot password */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PikarooOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLogin) "Iniciar Sesión" else "Crear Cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Link
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLogin) "¿No tienes cuenta? " else "¿Ya tienes cuenta? ",
                    color = PikarooTextGray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isLogin) "Regístrate" else "Inicia sesión",
                    color = PikarooOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.toggleTab(!isLogin) }
                )
            }
        }
    }
}

@Composable
fun TabSelector(isLogin: Boolean, onTabSelected: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = PikarooGray,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize()
        ) {
            TabItem(
                text = "Iniciar Sesión",
                isSelected = isLogin,
                onClick = { onTabSelected(true) },
                modifier = Modifier.weight(1f)
            )
            TabItem(
                text = "Registrarse",
                isSelected = !isLogin,
                onClick = { onTabSelected(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TabItem(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isSelected) Color.Black else PikarooTextGray,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = Color.LightGray) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = PikarooTextGray) },
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = onPasswordToggle) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = PikarooTextGray
                        )
                    }
                }
            },
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PikarooOrange,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true
        )
    }
}
