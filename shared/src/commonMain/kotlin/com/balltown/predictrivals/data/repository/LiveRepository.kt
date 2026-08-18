package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.LiveSnapshotDto
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.StandingDto
import com.balltown.predictrivals.domain.model.LiveSnapshot
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Standing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

private val liveJson = Json { ignoreUnknownKeys = true }

class LiveRepository(private val client: HttpClient) {

    suspend fun snapshot(tournamentId: Int): LiveSnapshot =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/live").body<LiveSnapshotDto>().toDomain()

    /**
     * Pushes a [LiveSnapshot] on every WebSocket frame. If the socket can't be established after
     * [maxReconnectAttempts] tries (exponential backoff), falls back to polling [snapshot] every
     * [pollIntervalMillis] instead of giving up.
     */
    fun observe(
        tournamentId: Int,
        accessToken: String,
        maxReconnectAttempts: Int = 3,
        pollIntervalMillis: Long = 10_000,
    ): Flow<LiveSnapshot> = flow {
        var attempt = 0
        var socketSucceededOnce = false
        while (attempt < maxReconnectAttempts && !socketSucceededOnce) {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = API_BASE_URL.removePrefix("https://").removePrefix("http://"),
                    path = "/ws/tournaments/$tournamentId/live",
                    request = { url.parameters.append("token", accessToken) },
                ) {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            socketSucceededOnce = true
                            emit(liveJson.decodeFromString<LiveSnapshotDto>(frame.readText()).toDomain())
                        }
                    }
                }
            } catch (e: Exception) {
                attempt++
                delay(backoffMillis(attempt))
            }
        }
        while (!socketSucceededOnce) {
            emit(snapshot(tournamentId))
            delay(pollIntervalMillis)
        }
    }.catch { emit(snapshot(tournamentId)) }

    private fun backoffMillis(attempt: Int): Long = 500L * (1 shl attempt)

    private fun LiveSnapshotDto.toDomain() = LiveSnapshot(
        matches.map { Match(it.id, it.homeTeam, it.awayTeam, it.kickoffAt, it.homeScore, it.awayScore, it.status) },
        standings.map { Standing(it.rank, it.userId, it.name, it.totalPoints, it.exactCount) },
    )
}
