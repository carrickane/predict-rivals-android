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

data class RoundRobinStanding(
    val rank: Int,
    val userId: Int,
    val name: String,
    val leaguePoints: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
)

data class RoundRobinTopScorer(val rank: Int, val userId: Int, val name: String, val goalsFor: Int)
