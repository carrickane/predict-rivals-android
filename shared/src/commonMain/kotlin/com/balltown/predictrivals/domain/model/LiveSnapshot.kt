package com.balltown.predictrivals.domain.model

data class LiveStanding(
    val rank: Int,
    val userId: Int,
    val name: String,
    val totalPoints: Int? = null,
    val exactCount: Int? = null,
    val leaguePoints: Int? = null,
    val wins: Int? = null,
    val draws: Int? = null,
    val losses: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
) {
    val isRoundRobin get() = leaguePoints != null
}

data class LiveRoundScore(val userId: Int, val name: String, val roundPoints: Int)

data class LiveSnapshot(val matches: List<Match>, val standings: List<LiveStanding>, val roundScores: List<LiveRoundScore> = emptyList())
