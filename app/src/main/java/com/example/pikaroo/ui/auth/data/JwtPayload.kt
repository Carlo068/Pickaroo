package com.example.pikaroo.ui.auth.data

import com.google.gson.JsonParser
import kotlin.io.encoding.Base64

/** Contenido del access token. Solo se lee; la firma la verifica el servidor, nunca la app. */
data class JwtPayload(
    val subject: String,
    val roles: List<String>,
    val issuedAt: Long,
    val expiresAt: Long
) {
    fun isExpired(nowSeconds: Long): Boolean = nowSeconds >= expiresAt

    companion object {
        private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

        fun encodeSegment(bytes: ByteArray): String =
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

        fun decode(token: String): JwtPayload? {
            val parts = token.split('.')
            if (parts.size != 3) return null
            return try {
                val json = JsonParser().parse(String(base64Url.decode(parts[1]), Charsets.UTF_8)).asJsonObject
                JwtPayload(
                    subject = json.get("sub").asString,
                    roles = json.getAsJsonArray("roles").map { it.asString },
                    issuedAt = json.get("iat").asLong,
                    expiresAt = json.get("exp").asLong
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
