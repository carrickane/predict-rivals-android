package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LIVE_JSON =
    """{"matches":[{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":1,"awayScore":0,"status":"live"}],"standings":[{"rank":1,"userId":1,"name":"P","totalPoints":3,"exactCount":1}]}"""

class LiveRepositoryTest {

    @Test
    fun `snapshot fetches and maps matches and standings`() = runTest {
        val engine = MockEngine { respond(LIVE_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = LiveRepository(testApiClient(engine))

        val snapshot = repository.snapshot(tournamentId = 1)

        assertEquals(1, snapshot.matches.size)
        assertEquals(1, snapshot.standings.single().rank)
    }
}
