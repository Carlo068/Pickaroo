package com.example.pikaroo.ui.auth.network

import com.example.pikaroo.common.mock.MockCollections.ROLES
import com.example.pikaroo.common.mock.MockCollections.SESSIONS
import com.example.pikaroo.common.mock.MockCollections.USERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockPickarooApi
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.data.JwtPayload
import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.LogoutRequest
import com.example.pikaroo.ui.auth.model.MeResponse
import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.model.RefreshResponse
import com.example.pikaroo.ui.auth.model.RegisterRequest
import com.example.pikaroo.ui.auth.model.RoleAssignment
import com.example.pikaroo.ui.auth.model.UserDto
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.UUID

/**
 * Imita auth de PickarooAPI sobre users.json + roles.json: access token con forma de JWT
 * (15 min), refresh rotativo (7 días) guardado como sesión y revocable.
 */
class MockAuthService(
    private val api: MockPickarooApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
    private val newToken: () -> String = { UUID.randomUUID().toString() }
) : AuthApi {

    override suspend fun register(body: RegisterRequest): LoginResponse = api.call {
        val name = body.name.trim()
        val username = body.username.trim()
        val email = body.email.trim()

        if (name.isEmpty()) throw MockErrors.badRequest("INVALID_NAME", "Name is required")
        if (username.length !in 3..64 || username.any { it.isWhitespace() }) {
            throw MockErrors.badRequest("INVALID_USERNAME", "Username must be 3-64 characters without spaces")
        }
        if (!EMAIL.matches(email)) throw MockErrors.badRequest("INVALID_EMAIL", "Email is not valid")
        if (body.password.length < MIN_PASSWORD_LENGTH) {
            throw MockErrors.badRequest("WEAK_PASSWORD", "Password must have at least $MIN_PASSWORD_LENGTH characters")
        }

        val users = all(USERS)
        if (users.any { it.string("username").equals(username, ignoreCase = true) }) {
            throw MockErrors.conflict("USERNAME_TAKEN", "Username is already in use")
        }
        if (users.any { it.string("email").equals(email, ignoreCase = true) }) {
            throw MockErrors.conflict("EMAIL_TAKEN", "Email is already in use")
        }

        val nextNumber = users.mapNotNull { it.string("id").removePrefix("user_").toIntOrNull() }.maxOrNull() ?: 0
        val user = JsonObject().apply {
            addProperty("id", "user_${nextNumber + 1}")
            addProperty("username", username)
            addProperty("password_mock", body.password)
            addProperty("name", name)
            addProperty("email", email)
            add("roles", JsonArray().apply { add(JsonObject().apply { addProperty("role", "customer") }) })
        }
        put(USERS, user)
        startSession(user)
    }

    override suspend fun login(body: LoginRequest): LoginResponse = api.call {
        val username = body.username.trim()
        if (username.isEmpty() || body.password.isEmpty()) {
            throw MockErrors.badRequest("MISSING_FIELDS", "Username and password are required")
        }
        val user = all(USERS).firstOrNull { it.string("username").equals(username, ignoreCase = true) }
        if (user == null || user.string("password_mock") != body.password) {
            throw MockErrors.unauthorized("INVALID_CREDENTIALS", "Wrong username or password")
        }
        startSession(user)
    }

    override suspend fun refresh(body: RefreshRequest): RefreshResponse = api.call {
        val session = find(SESSIONS, body.refresh)
        if (session == null || session.get("revoked").asBoolean) {
            throw MockErrors.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is not valid")
        }
        if (nowSeconds() >= session.get("expires_at").asLong) {
            throw MockErrors.unauthorized("REFRESH_EXPIRED", "Refresh token has expired")
        }
        val user = find(USERS, session.string("user_id"))
            ?: throw MockErrors.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token is not valid")

        session.addProperty("revoked", true)
        put(SESSIONS, session)

        val started = startSession(user)
        RefreshResponse(access = started.access, refresh = started.refresh)
    }

    override suspend fun logout(bearer: String, body: LogoutRequest) {
        api.call {
            val user = authenticate(bearer)
            val session = find(SESSIONS, body.refresh)
            if (session != null) {
                if (session.string("user_id") != user.string("id")) {
                    throw MockErrors.forbidden(message = "That refresh token belongs to another user")
                }
                session.addProperty("revoked", true)
                put(SESSIONS, session)
            }
        }
    }

    override suspend fun me(bearer: String): MeResponse = api.call {
        val user = authenticate(bearer)
        MeResponse(user = user.toDto(), roles = user.roleAssignments(), permissions = permissionsFor(user))
    }

    private fun MockStore.startSession(user: JsonObject): LoginResponse {
        val now = nowSeconds()
        val refresh = "rt_${newToken()}"
        put(SESSIONS, JsonObject().apply {
            addProperty("id", refresh)
            addProperty("user_id", user.string("id"))
            addProperty("expires_at", now + REFRESH_TTL_SECONDS)
            addProperty("revoked", false)
        })
        return LoginResponse(
            access = signAccessToken(user, now),
            refresh = refresh,
            user = user.toDto(),
            roles = user.roleAssignments(),
            permissions = permissionsFor(user)
        )
    }

    private fun MockStore.authenticate(bearer: String): JsonObject {
        val token = bearer.removePrefix("Bearer ").takeIf { bearer.startsWith("Bearer ") }
            ?: throw MockErrors.unauthorized()
        val payload = JwtPayload.decode(token)
        if (payload == null || token.substringAfterLast('.') != SIGNATURE) {
            throw MockErrors.unauthorized("INVALID_TOKEN", "Access token is not valid")
        }
        if (payload.isExpired(nowSeconds())) {
            throw MockErrors.unauthorized("TOKEN_EXPIRED", "Access token has expired")
        }
        return find(USERS, payload.subject)
            ?: throw MockErrors.unauthorized("INVALID_TOKEN", "Access token is not valid")
    }

    private fun signAccessToken(user: JsonObject, now: Long): String {
        val header = JsonObject().apply {
            addProperty("alg", "mock")
            addProperty("typ", "JWT")
        }
        val payload = JsonObject().apply {
            addProperty("sub", user.string("id"))
            addProperty("jti", newToken())
            add("roles", JsonArray().apply { user.roleAssignments().forEach { add(it.role) } })
            addProperty("iat", now)
            addProperty("exp", now + ACCESS_TTL_SECONDS)
        }
        return listOf(header, payload).joinToString(".") {
            JwtPayload.encodeSegment(it.toString().toByteArray(Charsets.UTF_8))
        } + ".$SIGNATURE"
    }

    private fun MockStore.permissionsFor(user: JsonObject): List<String> {
        val roleIds = user.roleAssignments().map { it.role }.toSet()
        return all(ROLES)
            .filter { it.string("id") in roleIds }
            .flatMap { role -> role.getAsJsonArray("permissions").map { it.asString } }
            .distinct()
    }

    private fun JsonObject.roleAssignments(): List<RoleAssignment> =
        getAsJsonArray("roles").map { element ->
            val assignment = element.asJsonObject
            RoleAssignment(
                role = assignment.string("role"),
                shopId = assignment.get("shop_id")?.takeIf { !it.isJsonNull }?.asString
            )
        }

    private fun JsonObject.toDto() = UserDto(
        id = string("id"),
        username = string("username"),
        name = string("name"),
        email = string("email")
    )

    private fun JsonObject.string(key: String): String = get(key).asString

    companion object {
        const val ACCESS_TTL_SECONDS = 15 * 60L
        const val REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60L
        const val MIN_PASSWORD_LENGTH = 6
        private const val SIGNATURE = "mock"
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
