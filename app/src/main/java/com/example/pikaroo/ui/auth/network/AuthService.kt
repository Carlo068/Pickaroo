package com.example.pikaroo.ui.auth.network

import com.example.pikaroo.ui.auth.model.LoginRequest
import com.example.pikaroo.ui.auth.model.LoginResponse
import com.example.pikaroo.ui.auth.model.LogoutRequest
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Endpoints de sesión de la API de autenticación.
 *
 * Base URL: https://androidbasics-auth-api.onrender.com
 *
 * Todas las funciones son suspend para llamarse desde corrutinas (mismo estilo
 * que GistService).
 */
interface AuthService {
    /**
     * POST /api/auth/login/ — público, sin token.
     *
     * @return [LoginResponse] con access, refresh y user (HTTP 200).
     * @throws retrofit2.HttpException 401 si las credenciales son incorrectas,
     *   400 si falta un campo.
     */
    @POST("api/auth/login/")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    /**
     * POST /api/auth/logout/ — requiere Authorization: Bearer <access>.
     * Mete el refresh en la blacklist del servidor. Responde 205 sin cuerpo.
     *
     * Se devuelve [Response] (en vez de Unit) para poder leer el código HTTP
     * sin que Retrofit lance excepción en el 205.
     */
    @POST("api/auth/logout/")
    suspend fun logout(
        @Header("Authorization") bearer: String,
        @Body body: LogoutRequest
    ): Response<Unit>
}

/**
 * Cliente Retrofit para [AuthService]. Mismo patrón que RetrofitClient, pero con
 * timeouts de 60 s: Render (free tier) duerme el servicio tras ~15 min sin tráfico
 * y la primera petición tras eso puede tardar 30–50 s (cold start).
 */
object AuthRetrofitClient {

    private const val BASE_URL = "https://androidbasics-auth-api.onrender.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val authService: AuthService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthService::class.java)
}
