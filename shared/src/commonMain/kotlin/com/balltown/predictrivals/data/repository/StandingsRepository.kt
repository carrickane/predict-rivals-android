package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.StandingDto
import com.balltown.predictrivals.data.dto.UserStatsDto
import com.balltown.predictrivals.domain.model.Standing
import com.balltown.predictrivals.domain.model.UserStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class StandingsRepository(private val client: HttpClient) {

    suspend fun standings(tournamentId: Int): List<Standing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/standings").body<List<StandingDto>>().map { it.toDomain() }

    suspend fun topScorers(tournamentId: Int): List<Standing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/top-scorers").body<List<StandingDto>>().map { it.toDomain() }

    suspend fun userStats(tournamentId: Int, userId: Int): UserStats =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/users/$userId/stats").body<UserStatsDto>().let {
            UserStats(it.userId, it.name, it.totalPoints, it.exactCount, it.totalPredictions, it.scoredPredictions, it.accuracy)
        }

    private fun StandingDto.toDomain() = Standing(rank, userId, name, totalPoints, exactCount)
}
