package com.balltown.predictrivals.domain.model

data class Standing(val rank: Int, val userId: Int, val name: String, val totalPoints: Int, val exactCount: Int)

data class UserStats(
    val userId: Int,
    val name: String,
    val totalPoints: Int,
    val exactCount: Int,
    val totalPredictions: Int,
    val scoredPredictions: Int,
    val accuracy: Double,
)
