package com.balltown.predictrivals.domain.scoring

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class PredictionDeadlineTest {

    @Test
    fun `editable before kickoff`() {
        val kickoff = Instant.parse("2026-08-20T18:00:00Z")
        val now = Instant.parse("2026-08-20T17:59:59Z")
        assertTrue(isPredictionEditable(kickoffAt = kickoff, now = now))
    }

    @Test
    fun `locked at or after kickoff`() {
        val kickoff = Instant.parse("2026-08-20T18:00:00Z")
        assertFalse(isPredictionEditable(kickoffAt = kickoff, now = kickoff))
        assertFalse(isPredictionEditable(kickoffAt = kickoff, now = Instant.parse("2026-08-20T18:00:01Z")))
    }
}
