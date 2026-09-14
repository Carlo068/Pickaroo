package com.example.pikaroo.ui.auth.data

import android.content.Context
import com.example.pikaroo.common.preferences.AppPreferences
import com.example.pikaroo.ui.auth.model.UserDto

/**
 * Expone, con nombres de dominio, la sesión del usuario (tokens JWT + username).
 * La persistencia real vive en [AppPreferences], igual que OnboardingPreferences.
 *
 * Nota: se guarda en SharedPreferences plano para mantener el estándar del
 * proyecto. Si se quisiera cifrar, el cambio queda aislado en esta clase.
 */
class SessionPreferences(context: Context) {

    private val preferences = AppPreferences(context)

    /** Guarda la sesión tras un login correcto. */
    fun saveSession(access: String, refresh: String, user: UserDto) {
        preferences.putString(KEY_ACCESS, access)
        preferences.putString(KEY_REFRESH, refresh)
        preferences.putString(KEY_USERNAME, user.username)
    }

    fun accessToken(): String = preferences.getString(KEY_ACCESS)
    fun refreshToken(): String = preferences.getString(KEY_REFRESH)
    fun username(): String = preferences.getString(KEY_USERNAME)

    /** Hay sesión si tenemos un refresh token guardado. */
    fun isLoggedIn(): Boolean = refreshToken().isNotBlank()

    /** Borra la sesión del dispositivo (logout). */
    fun clearSession() {
        preferences.remove(KEY_ACCESS, KEY_REFRESH, KEY_USERNAME)
    }

    companion object {
        private const val KEY_ACCESS = "auth_access"
        private const val KEY_REFRESH = "auth_refresh"
        private const val KEY_USERNAME = "auth_username"
    }
}
