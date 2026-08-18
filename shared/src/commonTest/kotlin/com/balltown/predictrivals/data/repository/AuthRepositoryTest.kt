package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.storage.AppPreferences
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRepositoryTest {

    private fun repositoryWith(engine: MockEngine, tokenStore: TokenStore, appPreferences: AppPreferences = AppPreferences(MapSettings())) =
        AuthRepository(AuthApi(engine, "https://x"), tokenStore, appPreferences, HttpClient(engine))

    @Test
    fun `register saves tokens and the user id, and returns the user`() = runTest {
        val tokenStore = TokenStore(MapSettings())
        val appPreferences = AppPreferences(MapSettings())
        val engine = MockEngine {
            respond("""{"tokens":{"accessToken":"acc","refreshToken":"ref"},"user":{"id":1,"name":"A","role":"player"}}""",
                HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = repositoryWith(engine, tokenStore, appPreferences)

        val user = repository.register(email = "a@b.com", password = "pw", name = "A")

        assertEquals("A", user.name)
        assertEquals(TokenPair("acc", "ref"), tokenStore.load())
        assertEquals(1, appPreferences.userId)
    }

    @Test
    fun `logout clears the stored tokens and user id`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("acc", "ref")) }
        val appPreferences = AppPreferences(MapSettings()).apply { userId = 1 }
        val repository = repositoryWith(MockEngine { error("not called") }, tokenStore, appPreferences)

        repository.logout()

        assertNull(tokenStore.load())
        assertNull(appPreferences.userId)
    }

    @Test
    fun `isLoggedIn reflects whether a token pair is stored`() = runTest {
        val tokenStore = TokenStore(MapSettings())
        val repository = repositoryWith(MockEngine { error("not called") }, tokenStore)

        assertEquals(false, repository.isLoggedIn())
        tokenStore.save(TokenPair("acc", "ref"))
        assertEquals(true, repository.isLoggedIn())
    }

    @Test
    fun `restoreSession returns null when no tokens are stored`() = runTest {
        val repository = repositoryWith(MockEngine { error("not called") }, TokenStore(MapSettings()))

        assertNull(repository.restoreSession())
    }

    @Test
    fun `restoreSession returns the persisted user id when tokens are stored`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("acc", "ref")) }
        val appPreferences = AppPreferences(MapSettings()).apply { userId = 42 }
        val repository = repositoryWith(MockEngine { error("not called") }, tokenStore, appPreferences)

        assertEquals(42, repository.restoreSession())
    }
}
