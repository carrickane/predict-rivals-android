package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchDto( // UNVERIFIED: never observed a real round; shape inferred from the doc
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: String,
)

@Serializable
data class RoundDto(val roundNumber: Int, val matches: List<MatchDto>) // UNVERIFIED
