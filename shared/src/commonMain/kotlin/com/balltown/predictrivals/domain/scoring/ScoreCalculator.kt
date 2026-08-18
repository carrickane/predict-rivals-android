package com.balltown.predictrivals.domain.scoring

fun pointsFor(predictedHome: Int, predictedAway: Int, actualHome: Int, actualAway: Int): Int {
    if (predictedHome == actualHome && predictedAway == actualAway) return 3

    val predictedResult = result(predictedHome, predictedAway)
    val actualResult = result(actualHome, actualAway)
    if (predictedResult != actualResult) return 0

    if (predictedResult == MatchResult.DRAW) return 1 // correctly guessed draw, wrong exact scoreline

    val predictedDifference = predictedHome - predictedAway
    val actualDifference = actualHome - actualAway
    return if (predictedDifference == actualDifference) 2 else 1
}

private enum class MatchResult { HOME_WIN, AWAY_WIN, DRAW }

private fun result(home: Int, away: Int): MatchResult = when {
    home > away -> MatchResult.HOME_WIN
    home < away -> MatchResult.AWAY_WIN
    else -> MatchResult.DRAW
}
