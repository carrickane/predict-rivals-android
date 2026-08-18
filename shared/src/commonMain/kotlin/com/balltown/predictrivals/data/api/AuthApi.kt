package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.dto.AuthResponseDto
import com.balltown.predictrivals.data.dto.LoginRequestDto
import com.balltown.predictrivals.data.dto.RefreshRequestDto
import com.balltown.predictrivals.data.dto.RefreshResponseDto
import com.balltown.predictrivals.data.dto.RegisterRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Unauthenticated client for the three auth endpoints that must never go through the Auth plugin. */
class AuthApi(engine: HttpClientEngine, private val baseUrl: String) {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun register(email: String, password: String, name: String): AuthResponseDto =
        postForAuth("$baseUrl/api/auth/email/register", RegisterRequestDto(email, password, name))

    suspend fun login(email: String, password: String): AuthResponseDto =
        postForAuth("$baseUrl/api/auth/email/login", LoginRequestDto(email, password))

    suspend fun refresh(refreshToken: String): RefreshResponseDto {
        val response = client.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestDto(refreshToken))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw apiExceptionFor(response.status.value, body)
        return Json { ignoreUnknownKeys = true }.decodeFromString(body)
    }

    private suspend inline fun <reified TRequest> postForAuth(url: String, request: TRequest): AuthResponseDto {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw apiExceptionFor(response.status.value, body)
        return Json { ignoreUnknownKeys = true }.decodeFromString(body)
    }
}
