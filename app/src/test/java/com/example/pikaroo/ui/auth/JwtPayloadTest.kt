package com.example.pikaroo.ui.auth

import com.example.pikaroo.ui.auth.data.JwtPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JwtPayloadTest {

    private fun token(payloadJson: String) =
        "header." + JwtPayload.encodeSegment(payloadJson.toByteArray(Charsets.UTF_8)) + ".sig"

    @Test
    fun `decodes the claims of a valid token`() {
        val payload = JwtPayload.decode(
            token("""{"sub":"user_3","roles":["owner","customer"],"iat":1000,"exp":1900}""")
        )

        assertEquals(JwtPayload("user_3", listOf("owner", "customer"), 1000, 1900), payload)
    }

    @Test
    fun `payload with accents survives the round trip`() {
        val payload = JwtPayload.decode(
            token("""{"sub":"usuario_ñ","roles":[],"iat":1,"exp":2}""")
        )

        assertEquals("usuario_ñ", payload!!.subject)
    }

    @Test
    fun `token expires exactly at exp`() {
        val payload = JwtPayload("u", emptyList(), issuedAt = 1000, expiresAt = 1900)

        assertFalse(payload.isExpired(1899))
        assertTrue(payload.isExpired(1900))
        assertTrue(payload.isExpired(5000))
    }

    @Test
    fun `malformed tokens decode to null`() {
        assertNull(JwtPayload.decode(""))
        assertNull(JwtPayload.decode("only.two"))
        assertNull(JwtPayload.decode("a.b.c.d"))
        assertNull(JwtPayload.decode("header.@@not-base64@@.sig"))
        assertNull(JwtPayload.decode(token("not json")))
        assertNull(JwtPayload.decode(token("""["an","array"]""")))
    }

    @Test
    fun `token missing a claim decodes to null`() {
        assertNull(JwtPayload.decode(token("""{"roles":[],"iat":1,"exp":2}""")))
        assertNull(JwtPayload.decode(token("""{"sub":"u","iat":1,"exp":2}""")))
        assertNull(JwtPayload.decode(token("""{"sub":"u","roles":[],"iat":1}""")))
    }
}
