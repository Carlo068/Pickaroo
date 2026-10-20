package com.example.pikaroo.ui.auth.network

import com.example.pikaroo.common.mock.MockCollections.ROLES
import com.example.pikaroo.common.mock.MockCollections.USERS
import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.common.mock.MockStore
import com.example.pikaroo.ui.auth.data.JwtPayload
import com.example.pikaroo.ui.auth.model.RoleAssignment
import com.google.gson.JsonObject

class AuthenticatedUser(
    val user: JsonObject,
    val roles: List<RoleAssignment>,
    val permissions: Set<String>
) {
    val id: String get() = user.get("id").asString

    fun can(permission: String) = permission in permissions
}

/** Lo que haría el servidor con el header Authorization: validar el token y saber quién llama. */
class MockAuthenticator(private val nowSeconds: () -> Long) {

    fun authenticate(store: MockStore, bearer: String): AuthenticatedUser {
        val token = bearer.removePrefix("Bearer ").takeIf { bearer.startsWith("Bearer ") }
            ?: throw MockErrors.unauthorized()
        val payload = JwtPayload.decode(token)
        if (payload == null || token.substringAfterLast('.') != SIGNATURE) {
            throw MockErrors.unauthorized("INVALID_TOKEN", "Access token is not valid")
        }
        if (payload.isExpired(nowSeconds())) {
            throw MockErrors.unauthorized("TOKEN_EXPIRED", "Access token has expired")
        }
        val user = store.find(USERS, payload.subject)
            ?: throw MockErrors.unauthorized("INVALID_TOKEN", "Access token is not valid")
        val roles = roleAssignmentsOf(user)
        return AuthenticatedUser(user, roles, permissionsFor(store, roles).toSet())
    }

    fun permissionsFor(store: MockStore, roles: List<RoleAssignment>): List<String> {
        val roleIds = roles.map { it.role }.toSet()
        return store.all(ROLES)
            .filter { it.get("id").asString in roleIds }
            .flatMap { role -> role.getAsJsonArray("permissions").map { it.asString } }
            .distinct()
    }

    fun roleAssignmentsOf(user: JsonObject): List<RoleAssignment> =
        user.getAsJsonArray("roles").map { element ->
            val assignment = element.asJsonObject
            RoleAssignment(
                role = assignment.get("role").asString,
                shopId = assignment.get("shop_id")?.takeIf { !it.isJsonNull }?.asString
            )
        }

    companion object {
        const val SIGNATURE = "mock"
    }
}
