package com.example.pikaroo.ui.auth.model

import com.google.gson.annotations.SerializedName

/**
 * Datos que viajan hacia y desde la API de autenticación de PickarooAPI
 * (JWT: access corto + refresh rotativo, roles y permisos).
 *
 * Solo contiene datos: ni lógica de red ni de UI. La lógica vive en los ViewModel
 * de ui/auth/viewmodel/ y la persistencia en ui/auth/data/SessionPreferences.
 */

/** Cuerpo del POST auth/register. */
data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String
)

/** Cuerpo del POST auth/login. */
data class LoginRequest(
    val username: String,
    val password: String
)

/** Cuerpo del POST auth/refresh. */
data class RefreshRequest(
    val refresh: String
)

/** Cuerpo del POST auth/logout (invalida el refresh token en el servidor). */
data class LogoutRequest(
    val refresh: String
)

data class UserDto(
    val id: String,
    val username: String,
    val name: String,
    val email: String
)

/** Un rol del usuario; staff y owner van ligados a una tienda ([shopId]). */
data class RoleAssignment(
    val role: String,
    @SerializedName("shop_id") val shopId: String? = null
)

/** Respuesta de auth/login y auth/register: tokens, usuario, roles y permisos. */
data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: UserDto,
    val roles: List<RoleAssignment>,
    val permissions: List<String>
)

/** Respuesta de auth/refresh: el refresh anterior queda revocado. */
data class RefreshResponse(
    val access: String,
    val refresh: String
)

/** Respuesta de GET me. */
data class MeResponse(
    val user: UserDto,
    val roles: List<RoleAssignment>,
    val permissions: List<String>
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

/** Campos del formulario de registro. */
data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false
)
