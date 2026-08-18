package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FixtureCandidateDto( // UNVERIFIED: endpoint returned [] for every param tried live
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val competition: String,
)

@Serializable
data class CreateRoundRequestDto(val fixtureIds: List<Int>) // UNVERIFIED

@Serializable
data class ScoreOverrideRequestDto(val homeScore: Int, val awayScore: Int, val status: String) // UNVERIFIED
