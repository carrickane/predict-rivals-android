package com.balltown.predictrivals.domain.model

data class Match(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String,
)

data class Round(val roundNumber: Int, val matches: List<Match>)
