package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.CreateRoundRequestDto
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.RoundDto
import com.balltown.predictrivals.data.dto.ScoreOverrideRequestDto
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Round
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CurationRepository(private val client: HttpClient) {

    suspend fun createRound(tournamentId: Int, fixtureIds: List<Int>): Round {
        require(fixtureIds.size == 9) { "A round must be created from exactly 9 fixtures, got ${fixtureIds.size}" }
        val dto = client.post("$API_BASE_URL/api/tournaments/$tournamentId/matches") {
            contentType(ContentType.Application.Json)
            setBody(CreateRoundRequestDto(fixtureIds))
        }.body<RoundDto>()
        return Round(dto.roundNumber, dto.matches.map { it.toDomain() })
    }

    suspend fun overrideScore(tournamentId: Int, matchId: Int, homeScore: Int, awayScore: Int, status: String): Match =
        client.patch("$API_BASE_URL/api/tournaments/$tournamentId/matches/$matchId/score") {
            contentType(ContentType.Application.Json)
            setBody(ScoreOverrideRequestDto(homeScore, awayScore, status))
        }.body<MatchDto>().toDomain()

    private fun MatchDto.toDomain() = Match(id, homeTeam, awayTeam, kickoffAt, homeScore, awayScore, status)
}
