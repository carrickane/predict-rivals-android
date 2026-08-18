package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CurationRepositoryTest {

    @Test
    fun `createRound posts the 9 fixture ids and maps the round`() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as TextContent).text
            respond(
                """{"roundNumber":1,"matches":[{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":null,"awayScore":null,"status":"scheduled"}]}""",
                HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = CurationRepository(testApiClient(engine))

        val round = repository.createRound(tournamentId = 1, fixtureIds = (1..9).toList())

        assertEquals(1, round.roundNumber)
        assertEquals("""{"fixtureIds":[1,2,3,4,5,6,7,8,9]}""", capturedBody)
    }

    @Test
    fun `overrideScore patches home and away score`() = runTest {
        val engine = MockEngine {
            respond(
                """{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":2,"awayScore":1,"status":"finished"}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = CurationRepository(testApiClient(engine))

        val match = repository.overrideScore(tournamentId = 1, matchId = 1, homeScore = 2, awayScore = 1, status = "finished")

        assertEquals(2, match.homeScore)
    }
}
