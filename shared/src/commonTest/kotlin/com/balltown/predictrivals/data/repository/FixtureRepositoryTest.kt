package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FixtureRepositoryTest {

    @Test
    fun `candidates maps an empty result (the only response observed live)`() = runTest {
        val engine = MockEngine { respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = FixtureRepository(testApiClient(engine))

        assertTrue(repository.candidates(from = "2026-08-20", to = "2026-08-27", search = null).isEmpty())
    }

    @Test
    fun `candidates maps a hypothetical populated result`() = runTest {
        val engine = MockEngine {
            respond("""[{"id":501,"homeTeam":"Arsenal","awayTeam":"Chelsea","kickoffAt":"2026-08-22T15:00:00Z","competition":"Premier League"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = FixtureRepository(testApiClient(engine))

        val candidates = repository.candidates(from = "2026-08-20", to = "2026-08-27", search = null)

        assertTrue(candidates.single().homeTeam == "Arsenal")
    }
}
