package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.RoundDto
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Round
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CalendarRepository(private val client: HttpClient) {

    suspend fun calendar(tournamentId: Int): List<Round> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/calendar").body<List<RoundDto>>().map { it.toDomain() }

    suspend fun currentRound(tournamentId: Int): Round? =
        try {
            client.get("$API_BASE_URL/api/tournaments/$tournamentId/rounds/current").body<RoundDto>().toDomain()
        } catch (e: ApiException.NotFound) {
            null
        }

    private fun RoundDto.toDomain() = Round(roundNumber, matches.map { it.toDomain() })
    private fun MatchDto.toDomain() = Match(id, homeTeam, awayTeam, kickoffAt, homeScore, awayScore, status)
}
