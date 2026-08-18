package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.api.buildApiClient
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.di.SessionStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

const val TEST_BASE_URL = "https://predict-rivals-backend-production.up.railway.app"

fun testApiClient(engine: MockEngine): HttpClient {
    val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("test-access", "test-refresh")) }
    return buildApiClient(engine, TEST_BASE_URL, tokenStore, AuthApi(engine, TEST_BASE_URL), SessionStore())
}
