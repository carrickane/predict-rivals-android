package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(val email: String, val password: String, val name: String)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String)

@Serializable
data class UserDto(val id: Int, val name: String, val role: String)

@Serializable
data class AuthResponseDto(val tokens: TokenPairDto, val user: UserDto)

@Serializable
data class RefreshResponseDto(val accessToken: String, val refreshToken: String) // UNVERIFIED shape, 401 path confirmed live only
