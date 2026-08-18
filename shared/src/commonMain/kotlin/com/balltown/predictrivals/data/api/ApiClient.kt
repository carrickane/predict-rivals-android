package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.di.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val API_BASE_URL = "https://predict-rivals-backend-production.up.railway.app"

fun buildApiClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokenStore: TokenStore,
    authApi: AuthApi,
    sessionStore: SessionStore,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
    }
    install(WebSockets)
    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.load()?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }
            refreshTokens {
                val current = tokenStore.load() ?: return@refreshTokens null
                val refreshed = runCatching { authApi.refresh(current.refreshToken) }.getOrNull()
                if (refreshed == null) {
                    // Refresh token is dead (expired/rotated elsewhere) — drop the stale session
                    // instead of leaving the app looking logged-in while every call 401s forever.
                    tokenStore.clear()
                    sessionStore.clear()
                    return@refreshTokens null
                }
                tokenStore.save(TokenPair(refreshed.accessToken, refreshed.refreshToken))
                BearerTokens(refreshed.accessToken, refreshed.refreshToken)
            }
        }
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                throw apiExceptionFor(response.status.value, response.bodyAsText())
            }
        }
    }
}
