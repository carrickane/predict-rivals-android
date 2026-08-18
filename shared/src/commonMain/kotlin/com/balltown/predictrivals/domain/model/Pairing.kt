package com.balltown.predictrivals.domain.model

data class Pairing(val roundNumber: Int, val opponentUserId: Int?, val opponentName: String?, val isBotMatch: Boolean)
