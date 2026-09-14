package com.example.pikaroo.ui.auth.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pikaroo.ui.auth.data.SessionPreferences
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.LoginState
import com.example.pikaroo.ui.auth.model.LoginUiState
import com.example.pikaroo.ui.auth.model.RegisterState
import com.example.pikaroo.ui.auth.network.AuthRetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException

/**
 * ViewModel de la pantalla de login. Dueño del estado ([LoginUiState]) y de la
 * lógica: llamar a la API, guardar la sesión y traducir los errores a mensajes.
 *
 * Extiende [AndroidViewModel] para pasarle el Context a [SessionPreferences]
 * sin arriesgar fugas de memoria (mismo criterio que OnboardingViewModel).
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)
    private val authService = AuthRetrofitClient.authService

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    var isLoginTab by mutableStateOf(true)
        private set

    var loginState by mutableStateOf(LoginState())
        private set

    var registerState by mutableStateOf(RegisterState())
        private set

    fun toggleTab(isLogin: Boolean) {
        isLoginTab = isLogin
        clearError()
    }

    fun onLoginUsernameChange(username: String) {
        loginState = loginState.copy(username = username)
        clearError()
    }

    fun onLoginPasswordChange(password: String) {
        loginState = loginState.copy(password = password)
        clearError()
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

    /**
     * Intenta iniciar sesión con lo escrito en el formulario. Flujo:
     * 1. Valida que los campos no estén vacíos.
     * 2. isLoading = true, limpia error previo.
     * 3. POST /api/auth/login/ (con un reintento si el servidor está dormido).
     * 4. Guarda access + refresh + user y pone isLoggedIn = true.
     * 5. Cualquier fallo se traduce a un mensaje para el formulario.
     */
    fun login() {
        val username = loginState.username.trim()
        val password = loginState.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa usuario y contraseña") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = loginWithRetry(LoginRequest(username, password))
                session.saveSession(response.access, response.refresh, response.user)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isLoading = false, error = messageForHttp(e.code())) }
            } catch (e: SocketTimeoutException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "El servidor está iniciando. Vuelve a intentar.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Sin conexión. Revisa tu internet e intenta de nuevo.")
                }
            }
        }
    }

    /** Limpia el mensaje de error (p. ej. cuando el usuario vuelve a escribir). */
    fun clearError() {
        if (_uiState.value.error != null) {
            _uiState.update { it.copy(error = null) }
        }
    }

    /**
     * Llama al login y, si el primer intento cae por timeout (cold start de Render),
     * reintenta una sola vez.
     */
    private suspend fun loginWithRetry(request: LoginRequest): LoginResponse =
        try {
            authService.login(request)
        } catch (e: SocketTimeoutException) {
            authService.login(request)
        }

    private fun messageForHttp(code: Int): String = when (code) {
        401 -> "Usuario o contraseña incorrectos"
        400 -> "Revisa los datos ingresados"
        else -> "No se pudo iniciar sesión (error $code)"
    }
}
