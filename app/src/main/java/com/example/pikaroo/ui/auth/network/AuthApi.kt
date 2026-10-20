package com.example.pikaroo.ui.auth.network

import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.LogoutRequest
import com.example.pikaroo.ui.auth.model.MeResponse
import com.example.pikaroo.ui.auth.model.RefreshRequest
import com.example.pikaroo.ui.auth.model.RefreshResponse
import com.example.pikaroo.ui.auth.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** Contrato de autenticación de PickarooAPI (docs/PLAN.md §5). Lo implementan el mock y, luego, Retrofit. */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): RefreshResponse

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") bearer: String, @Body body: LogoutRequest)

    @GET("me")
    suspend fun me(@Header("Authorization") bearer: String): MeResponse
}

class RemoteAuthApi : AuthApi {
    private fun notConnected(): Nothing =
        throw UnsupportedOperationException("PickarooAPI todavía no está conectada")

    override suspend fun register(body: RegisterRequest) = notConnected()
    override suspend fun login(body: LoginRequest) = notConnected()
    override suspend fun refresh(body: RefreshRequest) = notConnected()
    override suspend fun logout(bearer: String, body: LogoutRequest) = notConnected()
    override suspend fun me(bearer: String) = notConnected()
}
