package com.example.pikaroo.ui.auth.data

import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.network.AuthApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class SessionExpiredException : RuntimeException("Session expired")

/**
 * Punto único para llamadas que necesitan sesión: renueva el access token vencido antes de
 * llamar, reintenta una vez si el servidor responde 401 y, si ni el refresh sirve, cierra la
 * sesión y avisa por [sessionExpired] para volver al login.
 */
class AuthorizedCalls(
    private val session: SessionRepository,
    private val authApi: AuthApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) {

    private val refreshLock = Mutex()
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    suspend fun <T> call(block: suspend (bearer: String) -> T): T {
        if (!session.isLoggedIn()) throw SessionExpiredException()
        if (session.isAccessTokenExpired(nowSeconds())) refresh(session.refreshToken())

        val usedRefreshToken = session.refreshToken()
        return try {
            block("Bearer ${session.accessToken()}")
        } catch (e: HttpException) {
            if (e.code() != 401) throw e
            refresh(usedRefreshToken)
            block("Bearer ${session.accessToken()}")
        }
    }

    // Los refresh tokens rotan y nunca se repiten, así que sirven para saber si alguien ya renovó.
    private suspend fun refresh(staleRefreshToken: String) = refreshLock.withLock {
        if (!session.isLoggedIn()) throw SessionExpiredException()
        if (session.refreshToken() != staleRefreshToken) return@withLock

        try {
            val tokens = authApi.refresh(RefreshRequest(session.refreshToken()))
            session.updateTokens(tokens.access, tokens.refresh)
        } catch (e: HttpException) {
            if (e.code() !in 400..499) throw e
            session.clear()
            _sessionExpired.tryEmit(Unit)
            throw SessionExpiredException()
        }
    }
}
