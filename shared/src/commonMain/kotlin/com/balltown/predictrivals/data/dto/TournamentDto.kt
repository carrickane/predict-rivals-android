package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TournamentDto(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val joinCode: String,
    val playerLimit: Int,
    val playerCount: Int,
    val format: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class CreateTournamentRequestDto(val name: String, val playerLimit: Int, val format: String = "round_robin")

@Serializable
data class JoinTournamentRequestDto(val joinCode: String)
