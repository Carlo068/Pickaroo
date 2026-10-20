package com.example.pikaroo.common.mock

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Error body of every failed PickarooAPI call: `{ "error": { "code": "...", "message": "..." } }`. */
data class ApiError(val code: String, val message: String)

object MockErrors {

    fun http(status: Int, code: String, message: String): HttpException {
        val body = JsonObject().apply {
            add("error", JsonObject().apply {
                addProperty("code", code)
                addProperty("message", message)
            })
        }.toString()
        return HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))
    }

    fun badRequest(code: String, message: String = "Invalid request") = http(400, code, message)

    fun unauthorized(
        code: String = "UNAUTHORIZED",
        message: String = "Missing, invalid or expired session"
    ) = http(401, code, message)

    fun forbidden(
        code: String = "FORBIDDEN",
        message: String = "You are not allowed to do this"
    ) = http(403, code, message)

    fun notFound(
        code: String = "NOT_FOUND",
        message: String = "Resource not found"
    ) = http(404, code, message)

    fun conflict(
        code: String,
        message: String = "The request conflicts with the current state"
    ) = http(409, code, message)
}

/** Reads the error body, so it can only be called once per exception. Null if the body isn't an API error. */
fun HttpException.toApiError(): ApiError? {
    val raw = response()?.errorBody()?.string() ?: return null
    return try {
        val error = JsonParser().parse(raw).asJsonObject.getAsJsonObject("error")
        ApiError(error.get("code").asString, error.get("message").asString)
    } catch (_: Exception) {
        null
    }
}
