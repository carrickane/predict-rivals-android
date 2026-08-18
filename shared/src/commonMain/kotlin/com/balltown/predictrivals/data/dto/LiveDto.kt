package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveStandingDto(
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
)

@Serializable
data class LiveRoundScoreDto(val userId: Int, val name: String, val roundPoints: Int)

@Serializable
data class LiveSnapshotDto(
    val matches: List<MatchDto>,
    val standings: List<LiveStandingDto>,
    val roundScores: List<LiveRoundScoreDto> = emptyList(),
)
