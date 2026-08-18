package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingDto(val roundNumber: Int, val opponentUserId: Int?, val opponentName: String?, val isBotMatch: Boolean)
