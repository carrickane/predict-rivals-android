package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarRepositoryTest {

    @Test
    fun `calendar maps an empty list when no rounds exist yet`() = runTest {
        val engine = MockEngine { respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = CalendarRepository(testApiClient(engine))

        assertTrue(repository.calendar(tournamentId = 1).isEmpty())
    }

    @Test
    fun `currentRound returns null instead of throwing on the confirmed 404`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"No rounds found for tournament 1"}""", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = CalendarRepository(testApiClient(engine))

        assertNull(repository.currentRound(tournamentId = 1))
    }

    @Test
    fun `currentRound maps a real round when one exists`() = runTest {
        val engine = MockEngine {
            respond(
                """{"roundNumber":1,"matches":[{"id":10,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":null,"awayScore":null,"status":"scheduled"}]}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = CalendarRepository(testApiClient(engine))

        val round = repository.currentRound(tournamentId = 1)

        assertEquals(1, round?.roundNumber)
        assertEquals(1, round?.matches?.size)
    }
}
