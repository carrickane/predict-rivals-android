package com.balltown.predictrivals.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiExceptionTest {

    @Test
    fun `maps known status codes to their typed exception`() {
        assertIs<ApiException.Unauthorized>(apiExceptionFor(401, """{"error":"nope"}"""))
        assertIs<ApiException.Forbidden>(apiExceptionFor(403, """{"error":"nope"}"""))
        assertIs<ApiException.NotFound>(apiExceptionFor(404, """{"error":"nope"}"""))
        assertIs<ApiException.Conflict>(apiExceptionFor(409, """{"error":"nope"}"""))
        assertIs<ApiException.ServerError>(apiExceptionFor(500, """{"error":"nope"}"""))
        assertIs<ApiException.ServerError>(apiExceptionFor(502, """not json"""))
    }

    @Test
    fun `extracts the backend's error message when the body is the standard error shape`() {
        val ex = apiExceptionFor(404, """{"error":"No tournament found for that join code"}""")
        assertEquals("No tournament found for that join code", ex.message)
    }

    @Test
    fun `falls back to a generic message when the body isn't the standard error shape`() {
        val ex = apiExceptionFor(500, "not json")
        assertEquals("Something went wrong. Please try again.", ex.message)
    }
}
