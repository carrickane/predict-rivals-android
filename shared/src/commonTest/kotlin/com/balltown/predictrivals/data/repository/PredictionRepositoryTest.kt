package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PredictionRepositoryTest {

    @Test
    fun `submit posts matchId and both scores`() = runTest {
        val engine = MockEngine {
            respond("""{"matchId":10,"predictedHomeScore":2,"predictedAwayScore":1}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = PredictionRepository(testApiClient(engine))

        val prediction = repository.submit(matchId = 10, homeScore = 2, awayScore = 1)

        assertEquals(10, prediction.matchId)
        assertEquals(2, prediction.predictedHomeScore)
    }
}
