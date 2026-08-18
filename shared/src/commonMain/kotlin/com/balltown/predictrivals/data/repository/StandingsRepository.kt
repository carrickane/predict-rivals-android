package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.RoundRobinStandingDto
import com.balltown.predictrivals.data.dto.RoundRobinTopScorerDto
import com.balltown.predictrivals.data.dto.StandingDto
import com.balltown.predictrivals.data.dto.UserStatsDto
import com.balltown.predictrivals.domain.model.RoundRobinStanding
import com.balltown.predictrivals.domain.model.RoundRobinTopScorer
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

    suspend fun roundRobinStandings(tournamentId: Int): List<RoundRobinStanding> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/standings").body<List<RoundRobinStandingDto>>().map { it.toDomain() }

    suspend fun roundRobinTopScorers(tournamentId: Int): List<RoundRobinTopScorer> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/top-scorers").body<List<RoundRobinTopScorerDto>>().map { it.toDomain() }

    private fun StandingDto.toDomain() = Standing(rank, userId, name, totalPoints, exactCount)

    private fun RoundRobinStandingDto.toDomain() =
        RoundRobinStanding(rank, userId, name, leaguePoints, wins, draws, losses, goalsFor, goalsAgainst, goalDifference)

    private fun RoundRobinTopScorerDto.toDomain() = RoundRobinTopScorer(rank, userId, name, goalsFor)
}
