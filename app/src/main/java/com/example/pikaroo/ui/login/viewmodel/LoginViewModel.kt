package com.example.pikaroo.ui.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pikaroo.ui.login.model.LoginState
import com.example.pikaroo.ui.login.model.RegisterState

class LoginViewModel : ViewModel() {
    var isLoginTab by mutableStateOf(true)
    
    var loginState by mutableStateOf(LoginState())
    var registerState by mutableStateOf(RegisterState())

    fun toggleTab(isLogin: Boolean) {
        isLoginTab = isLogin
    }

    fun onLoginEmailChange(email: String) {
        loginState = loginState.copy(email = email)
    }

    fun onLoginPasswordChange(password: String) {
        loginState = loginState.copy(password = password)
    }

    fun toggleLoginPasswordVisibility() {
        loginState = loginState.copy(isPasswordVisible = !loginState.isPasswordVisible)
    }

    fun onRegisterNameChange(name: String) {
        registerState = registerState.copy(name = name)
    }

    fun onRegisterEmailChange(email: String) {
        registerState = registerState.copy(email = email)
    }

    fun onRegisterPasswordChange(password: String) {
        registerState = registerState.copy(password = password)
    }

    fun onRegisterConfirmPasswordChange(confirmPassword: String) {
        registerState = registerState.copy(confirmPassword = confirmPassword)
    }

    fun toggleRegisterPasswordVisibility() {
        registerState = registerState.copy(isPasswordVisible = !registerState.isPasswordVisible)
    }
}
