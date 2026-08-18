package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubmitPredictionRequestDto(val matchId: Int, val homeScore: Int, val awayScore: Int) // UNVERIFIED

@Serializable
data class PredictionDto(val matchId: Int, val predictedHomeScore: Int, val predictedAwayScore: Int) // UNVERIFIED
