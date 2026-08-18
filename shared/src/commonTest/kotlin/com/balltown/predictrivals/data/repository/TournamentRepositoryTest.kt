package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.ApiException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val TOURNAMENT_JSON =
    """{"id":1,"name":"Probe Cup","ownerUserId":1,"joinCode":"9VFVPN","playerLimit":10,"playerCount":1,"format":"solo_points","status":"open","createdAt":"2026-08-17T08:54:01.979038Z"}"""

class TournamentRepositoryTest {

    @Test
    fun `create posts name and playerLimit and maps the response`() = runTest {
        val engine = MockEngine { respond(TOURNAMENT_JSON, HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val tournament = repository.create(name = "Probe Cup", playerLimit = 10)

        assertEquals("9VFVPN", tournament.joinCode)
        assertEquals("open", tournament.status)
    }

    @Test
    fun `join surfaces the backend's conflict message when already started`() = runTest {
        val engine = MockEngine { respond("""{"error":"Tournament has already started"}""", HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val error = assertFailsWith<ApiException.Conflict> { repository.join(joinCode = "9VFVPN") }
        assertEquals("Tournament has already started", error.message)
    }

    @Test
    fun `mine maps a list of tournaments`() = runTest {
        val engine = MockEngine { respond("[$TOURNAMENT_JSON]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val tournaments = repository.mine()

        assertEquals(1, tournaments.size)
        assertEquals("Probe Cup", tournaments.first().name)
    }
}
