package com.example.pikaroo.ui.auth.model

/**
 * Datos que viajan hacia y desde la API de autenticación
 * (Django REST + JWT · https://androidbasics-auth-api.onrender.com).
 *
 * Solo contiene datos: ni lógica de red ni de UI. La lógica vive en los ViewModel
 * de ui/auth/viewmodel/ y la persistencia en ui/auth/data/SessionPreferences.
 */

/** Cuerpo del POST /api/auth/login/. */
data class LoginRequest(
    val username: String,
    val password: String
)

/** Cuerpo del POST /api/auth/logout/ (invalida el refresh token en el servidor). */
data class LogoutRequest(
    val refresh: String
)

/**
 * Respuesta 200 OK del login: un access token corto (60 min), un refresh
 * largo (7 días) y los datos del usuario.
 */
data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: UserDto
)

/**
 * Datos del usuario devueltos por el login. Solo se mapean los campos que la app
 * usa hoy; el resto del JSON (first_name, last_name, date_joined) se ignora.
 */
data class UserDto(
    val id: Int,
    val username: String,
    val email: String
)

/**
 * Estado observable de la pantalla de login. Mismo patrón que OnboardingUiState.
 *
 * @property isLoading true mientras se hace la petición (mostrar spinner).
 * @property error mensaje a mostrar bajo el formulario, o null si no hay error.
 * @property isLoggedIn pasa a true cuando el login fue correcto y ya se guardó la
 *   sesión; la vista lo observa para navegar a "tabs".
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

/** Campos del formulario de inicio de sesión (lo que el usuario escribe). */
data class LoginState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false
)

/**
 * Campos del formulario de registro. La API no expone registro todavía, así que
 * esta pestaña sigue siendo solo de UI.
 */
data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false
)
