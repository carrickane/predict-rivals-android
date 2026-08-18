package com.balltown.predictrivals.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreCalculatorTest {

    @Test
    fun `exact score match scores 3`() {
        assertEquals(3, pointsFor(predictedHome = 2, predictedAway = 1, actualHome = 2, actualAway = 1))
    }

    @Test
    fun `correct result and correct goal difference (but not exact score) scores 2`() {
        assertEquals(2, pointsFor(predictedHome = 3, predictedAway = 1, actualHome = 2, actualAway = 0))
    }

    @Test
    fun `correct result only (wrong goal difference) scores 1`() {
        assertEquals(1, pointsFor(predictedHome = 3, predictedAway = 1, actualHome = 1, actualAway = 0))
    }

    @Test
    fun `correctly guessed draw scores 1 even with the wrong exact scoreline`() {
        assertEquals(1, pointsFor(predictedHome = 1, predictedAway = 1, actualHome = 2, actualAway = 2))
    }

    @Test
    fun `wrong result scores 0`() {
        assertEquals(0, pointsFor(predictedHome = 2, predictedAway = 0, actualHome = 0, actualAway = 1))
    }
}
