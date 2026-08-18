package com.balltown.predictrivals.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ErrorBody(val error: String)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

sealed class ApiException(override val message: String) : Exception(message) {
    class Unauthorized(message: String) : ApiException(message)
    class Forbidden(message: String) : ApiException(message)
    class NotFound(message: String) : ApiException(message)
    class Conflict(message: String) : ApiException(message)
    class ServerError(message: String) : ApiException(message)
    class NetworkError(message: String) : ApiException(message)
}

private const val GENERIC_MESSAGE = "Something went wrong. Please try again."

fun apiExceptionFor(statusCode: Int, rawBody: String): ApiException {
    val message = runCatching { errorBodyJson.decodeFromString<ErrorBody>(rawBody).error }
        .getOrElse { GENERIC_MESSAGE }
    return when (statusCode) {
        401 -> ApiException.Unauthorized(message)
        403 -> ApiException.Forbidden(message)
        404 -> ApiException.NotFound(message)
        409 -> ApiException.Conflict(message)
        else -> ApiException.ServerError(message)
    }
}
