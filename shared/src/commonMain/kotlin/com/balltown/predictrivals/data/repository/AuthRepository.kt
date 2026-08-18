package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.storage.AppPreferences
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.domain.model.User
import io.ktor.client.HttpClient
import io.ktor.client.request.delete

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val appPreferences: AppPreferences,
    private val client: HttpClient,
) {
    suspend fun register(email: String, password: String, name: String): User {
        val response = authApi.register(email, password, name)
        tokenStore.save(TokenPair(response.tokens.accessToken, response.tokens.refreshToken))
        appPreferences.userId = response.user.id
        return User(response.user.id, response.user.name, response.user.role)
    }

    suspend fun login(email: String, password: String): User {
        val response = authApi.login(email, password)
        tokenStore.save(TokenPair(response.tokens.accessToken, response.tokens.refreshToken))
        appPreferences.userId = response.user.id
        return User(response.user.id, response.user.name, response.user.role)
    }

    fun logout() {
        tokenStore.clear()
        appPreferences.userId = null
        appPreferences.lastTournamentId = null
    }

    fun isLoggedIn(): Boolean = tokenStore.load() != null

    /** Non-null when a prior session's tokens are still on disk — lets the app skip the login screen. */
    fun restoreSession(): Int? {
        if (!isLoggedIn()) return null
        return appPreferences.userId
    }

    // UNVERIFIED: no delete-account endpoint is documented for this backend; guessed as
    // DELETE /api/auth/me following REST convention. Confirm against the live backend before relying on it.
    suspend fun deleteAccount() {
        client.delete("$API_BASE_URL/api/auth/me")
        logout()
    }
}
