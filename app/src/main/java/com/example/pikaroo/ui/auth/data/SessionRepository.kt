package com.example.pikaroo.ui.auth.data

import android.content.Context
import com.example.pikaroo.common.preferences.PreferencesStringStore
import com.example.pikaroo.common.preferences.StringStore
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.RoleAssignment
import com.example.pikaroo.ui.auth.model.RoleMode
import com.example.pikaroo.ui.auth.model.UserDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
    val roles: List<RoleAssignment>,
    val permissions: Set<String>
)

/**
 * Sesión del usuario: tokens, datos, roles, permisos y modo activo. Se guarda en [StringStore]
 * (SharedPreferences plano, igual que el resto del proyecto; cifrarlo quedaría aislado aquí).
 */
class SessionRepository(private val storage: StringStore) {

    private val gson = Gson()
    private var cached: Session? = null
    private var cacheLoaded = false

    @Synchronized
    fun save(response: LoginResponse) {
        storage.put(KEY_ACCESS, response.access)
        storage.put(KEY_REFRESH, response.refresh)
        storage.put(KEY_USER, gson.toJson(response.user))
        storage.put(KEY_ROLES, gson.toJson(response.roles))
        storage.put(KEY_PERMISSIONS, gson.toJson(response.permissions))
        storage.remove(KEY_MODE)
        cacheLoaded = false
    }

    @Synchronized
    fun updateTokens(access: String, refresh: String) {
        if (session() == null) return
        storage.put(KEY_ACCESS, access)
        storage.put(KEY_REFRESH, refresh)
        cacheLoaded = false
    }

    @Synchronized
    fun clear() {
        ALL_KEYS.forEach(storage::remove)
        cacheLoaded = false
    }

    @Synchronized
    fun session(): Session? {
        if (!cacheLoaded) {
            cached = read()
            cacheLoaded = true
        }
        return cached
    }

    fun isLoggedIn(): Boolean = session() != null

    fun accessToken(): String = session()?.accessToken.orEmpty()
    fun refreshToken(): String = session()?.refreshToken.orEmpty()
    fun userId(): String = session()?.user?.id.orEmpty()
    fun username(): String = session()?.user?.username.orEmpty()
    fun roles(): List<RoleAssignment> = session()?.roles.orEmpty()

    fun can(permission: String): Boolean = session()?.permissions?.contains(permission) == true

    /** Solo lee el contenido del token; la firma la verifica el servidor. Sin token válido cuenta como vencido. */
    fun isAccessTokenExpired(nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean =
        JwtPayload.decode(accessToken())?.isExpired(nowSeconds) ?: true

    fun availableModes(): List<RoleMode> =
        roles().mapNotNull { RoleMode.of(it.role) }.distinct().sortedBy { it.ordinal }

    fun activeMode(): RoleMode? {
        val available = availableModes()
        val saved = storage.get(KEY_MODE)?.let { name -> RoleMode.entries.firstOrNull { it.name == name } }
        return saved?.takeIf { it in available } ?: available.firstOrNull()
    }

    @Synchronized
    fun setActiveMode(mode: RoleMode) {
        require(mode in availableModes()) { "The user has no role for mode $mode" }
        storage.put(KEY_MODE, mode.name)
    }

    private fun read(): Session? {
        val access = storage.get(KEY_ACCESS) ?: return null
        val refresh = storage.get(KEY_REFRESH) ?: return null
        return try {
            Session(
                accessToken = access,
                refreshToken = refresh,
                user = gson.fromJson(storage.get(KEY_USER), UserDto::class.java)!!,
                roles = gson.fromJson<List<RoleAssignment>>(storage.get(KEY_ROLES), rolesType)!!,
                permissions = gson.fromJson<List<String>>(storage.get(KEY_PERMISSIONS), permissionsType)!!.toSet()
            )
        } catch (_: Exception) {
            ALL_KEYS.forEach(storage::remove)
            null
        }
    }

    companion object {
        private const val KEY_ACCESS = "session_access"
        private const val KEY_REFRESH = "session_refresh"
        private const val KEY_USER = "session_user"
        private const val KEY_ROLES = "session_roles"
        private const val KEY_PERMISSIONS = "session_permissions"
        private const val KEY_MODE = "session_mode"
        private val ALL_KEYS = listOf(KEY_ACCESS, KEY_REFRESH, KEY_USER, KEY_ROLES, KEY_PERMISSIONS, KEY_MODE)

        private val rolesType = object : TypeToken<List<RoleAssignment>>() {}.type
        private val permissionsType = object : TypeToken<List<String>>() {}.type

        fun create(context: Context) = SessionRepository(PreferencesStringStore(context))
    }
}
