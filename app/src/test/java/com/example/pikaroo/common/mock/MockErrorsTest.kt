package com.example.pikaroo.common.mock

import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MockErrorsTest {

    @Test
    fun `helpers use the status codes of the contract`() {
        assertEquals(400, MockErrors.badRequest("INVALID_FIELD").code())
        assertEquals(401, MockErrors.unauthorized().code())
        assertEquals(403, MockErrors.forbidden().code())
        assertEquals(404, MockErrors.notFound().code())
        assertEquals(409, MockErrors.conflict("SLOT_FULL").code())
    }

    @Test
    fun `body has the shared error shape and nothing else`() {
        val body = MockErrors.conflict("SLOT_FULL", "That slot is full").response()!!.errorBody()!!.string()

        val root = JsonParser().parse(body).asJsonObject
        val error = root.getAsJsonObject("error")

        assertEquals(setOf("error"), root.keySet())
        assertEquals(setOf("code", "message"), error.keySet())
        assertEquals("SLOT_FULL", error.get("code").asString)
        assertEquals("That slot is full", error.get("message").asString)
    }

    @Test
    fun `body is declared as json`() {
        val mediaType = MockErrors.notFound().response()!!.errorBody()!!.contentType()

        assertEquals("application/json", "${mediaType!!.type}/${mediaType.subtype}")
    }

    @Test
    fun `default codes and messages are set`() {
        assertEquals(ApiError("UNAUTHORIZED", "Missing, invalid or expired session"), MockErrors.unauthorized().toApiError())
        assertEquals("FORBIDDEN", MockErrors.forbidden().toApiError()!!.code)
        assertEquals("NOT_FOUND", MockErrors.notFound().toApiError()!!.code)
    }

    @Test
    fun `custom codes and messages are kept, including accents`() {
        val error = MockErrors.badRequest("INVALID_NAME", "El nombre es obligatorio").toApiError()

        assertEquals(ApiError("INVALID_NAME", "El nombre es obligatorio"), error)
    }

    @Test
    fun `http builds any status`() {
        val exception = MockErrors.http(422, "UNPROCESSABLE", "Cannot process")

        assertEquals(422, exception.code())
        assertEquals("UNPROCESSABLE", exception.toApiError()!!.code)
    }

    @Test
    fun `toApiError is null when the body is not an api error`() {
        fun failure(body: String) = HttpException(
            Response.error<Any>(500, body.toResponseBody("application/json".toMediaType()))
        )

        assertNull(failure("oops").toApiError())
        assertNull(failure("{\"detail\":\"nope\"}").toApiError())
        assertNull(failure("{\"error\":{\"code\":\"X\"}}").toApiError())
        assertNull(failure("").toApiError())
    }

    @Test
    fun `message of the exception mentions the status`() {
        assertTrue(MockErrors.forbidden().message!!.contains("403"))
    }
}
