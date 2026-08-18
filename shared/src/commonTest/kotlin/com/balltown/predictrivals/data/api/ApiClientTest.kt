package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.di.SessionStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiClientTest {

    @Test
    fun `attaches the stored access token as a bearer header`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("valid-access", "valid-refresh")) }
        var seenAuthHeader: String? = null
        val engine = MockEngine { request ->
            seenAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"id":1,"name":"x","ownerUserId":1,"joinCode":"AB","playerLimit":2,"playerCount":1,"format":"solo_points","status":"open","createdAt":"now"}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val authApi = AuthApi(engine = engine, baseUrl = BASE_URL)
        val client = buildApiClient(engine = engine, baseUrl = BASE_URL, tokenStore = tokenStore, authApi = authApi, sessionStore = SessionStore())

        client.get("$BASE_URL/api/tournaments/1")

        assertEquals("Bearer valid-access", seenAuthHeader)
    }

    @Test
    fun `refreshes once on 401 then retries the original request with the new token`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("expired-access", "old-refresh")) }
        var call = 0
        val engine = MockEngine { request ->
            call++
            when {
                request.url.encodedPath == "/api/auth/refresh" ->
                    respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""",
                        HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                call == 1 -> respond("""{"error":"Invalid access token"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("""{"id":1,"name":"x","ownerUserId":1,"joinCode":"AB","playerLimit":2,"playerCount":1,"format":"solo_points","status":"open","createdAt":"now"}""",
                    HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val authApi = AuthApi(engine = engine, baseUrl = BASE_URL)
        val client = buildApiClient(engine = engine, baseUrl = BASE_URL, tokenStore = tokenStore, authApi = authApi, sessionStore = SessionStore())

        client.get("$BASE_URL/api/tournaments/1")

        assertEquals(TokenPair("new-access", "new-refresh"), tokenStore.load())
    }

    @Test
    fun `a dead refresh token clears the stored tokens and session instead of looping`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("expired-access", "dead-refresh")) }
        val sessionStore = SessionStore().apply { set(1) }
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/auth/refresh" ->
                    respond("""{"error":"Invalid refresh token"}""", HttpStatusCode.Unauthorized,
                        headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("""{"error":"Invalid access token"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val authApi = AuthApi(engine = engine, baseUrl = BASE_URL)
        val client = buildApiClient(engine = engine, baseUrl = BASE_URL, tokenStore = tokenStore, authApi = authApi, sessionStore = sessionStore)

        runCatching { client.get("$BASE_URL/api/tournaments/1") }

        assertNull(tokenStore.load())
        assertNull(sessionStore.currentUserId.value)
    }

    private companion object {
        const val BASE_URL = "https://predict-rivals-backend-production.up.railway.app"
    }
}
