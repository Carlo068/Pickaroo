package com.example.pikaroo.ui.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pikaroo.ui.auth.data.SessionPreferences
import com.example.pikaroo.ui.auth.model.LogoutRequest
import com.example.pikaroo.ui.auth.network.AuthRetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Scope con vida de proceso para la petición "best-effort" de logout. No usamos
 * viewModelScope porque al cerrar sesión navegamos y destruimos este ViewModel:
 * eso cancelaría la llamada antes de que el servidor reciba el refresh.
 */
private val logoutScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * ViewModel para cerrar sesión.
 *
 * Cerrar sesión son dos cosas independientes:
 * 1. Borrar los tokens del dispositivo → se hace SIEMPRE y de inmediato; es lo que
 *    controla la sesión local (con JWT el access sigue vivo hasta expirar).
 * 2. Llamar a /api/auth/logout/ para quemar el refresh en el servidor → se hace
 *    en segundo plano, sin bloquear la navegación. Si falla (sin red, 400/401, o el
 *    servidor dormido tardando 60 s), da igual: la sesión local ya se cerró.
 */
class LogoutViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionPreferences(application)
    private val authService = AuthRetrofitClient.authService

    /**
     * Cierra la sesión local al instante e invoca [onLoggedOut] (para navegar a
     * "login"). La invalidación del refresh en el servidor queda corriendo aparte.
     */
    fun logout(onLoggedOut: () -> Unit) {
        val access = session.accessToken()
        val refresh = session.refreshToken()

        session.clearSession()
        onLoggedOut()

        if (access.isNotBlank() && refresh.isNotBlank()) {
            logoutScope.launch {
                try {
                    authService.logout("Bearer $access", LogoutRequest(refresh))
                } catch (_: Exception) {
                    // Best-effort: la sesión local ya se cerró.
                }
            }
        }
    }
}
