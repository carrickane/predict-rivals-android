package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StandingDto(val rank: Int, val userId: Int, val name: String, val totalPoints: Int, val exactCount: Int)

@Serializable
data class UserStatsDto(
    val userId: Int,
    val name: String,
    val totalPoints: Int,
    val exactCount: Int,
    val totalPredictions: Int,
    val scoredPredictions: Int,
    val accuracy: Double,
)
