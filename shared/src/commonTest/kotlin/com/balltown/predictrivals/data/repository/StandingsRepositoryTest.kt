package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StandingsRepositoryTest {

    @Test
    fun `standings maps rank, user, and points`() = runTest {
        val engine = MockEngine {
            respond("""[{"rank":1,"userId":1,"name":"ProbeTest","totalPoints":0,"exactCount":0}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = StandingsRepository(testApiClient(engine))

        val standings = repository.standings(tournamentId = 1)

        assertEquals(1, standings.single().rank)
        assertEquals("ProbeTest", standings.single().name)
    }

    @Test
    fun `userStats maps accuracy and prediction counts`() = runTest {
        val engine = MockEngine {
            respond("""{"userId":1,"name":"ProbeTest","totalPoints":0,"exactCount":0,"totalPredictions":0,"scoredPredictions":0,"accuracy":0.0}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = StandingsRepository(testApiClient(engine))

        val stats = repository.userStats(tournamentId = 1, userId = 1)

        assertEquals(0.0, stats.accuracy)
    }
}
