package com.balltown.predictrivals.data.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a real register-or-login response`() {
        val body = """
            {"tokens":{"accessToken":"acc-123","refreshToken":"ref-123"},"user":{"id":1,"name":"ProbeTest","role":"player"}}
        """.trimIndent()
        val parsed = json.decodeFromString<AuthResponseDto>(body)
        assertEquals("acc-123", parsed.tokens.accessToken)
        assertEquals("ref-123", parsed.tokens.refreshToken)
        assertEquals(1, parsed.user.id)
        assertEquals("ProbeTest", parsed.user.name)
        assertEquals("player", parsed.user.role)
    }

    @Test
    fun `serializes a register request with the real field name (name, not displayName)`() {
        val request = RegisterRequestDto(email = "a@b.com", password = "pw", name = "A")
        assertEquals("""{"email":"a@b.com","password":"pw","name":"A"}""", json.encodeToString(request))
    }
}
