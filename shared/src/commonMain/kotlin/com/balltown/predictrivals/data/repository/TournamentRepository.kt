package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.CreateTournamentRequestDto
import com.balltown.predictrivals.data.dto.JoinTournamentRequestDto
import com.balltown.predictrivals.data.dto.TournamentDto
import com.balltown.predictrivals.domain.model.Tournament
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TournamentRepository(private val client: HttpClient) {

    suspend fun create(name: String, playerLimit: Int): Tournament =
        client.post("$API_BASE_URL/api/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(CreateTournamentRequestDto(name, playerLimit))
        }.body<TournamentDto>().toDomain()

    suspend fun join(joinCode: String): Tournament =
        client.post("$API_BASE_URL/api/tournaments/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinTournamentRequestDto(joinCode))
        }.body<TournamentDto>().toDomain()

    suspend fun start(tournamentId: Int): Tournament =
        client.post("$API_BASE_URL/api/tournaments/$tournamentId/start").body<TournamentDto>().toDomain()

    suspend fun mine(): List<Tournament> =
        client.get("$API_BASE_URL/api/tournaments/mine").body<List<TournamentDto>>().map { it.toDomain() }

    suspend fun get(tournamentId: Int): Tournament =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId").body<TournamentDto>().toDomain()

    private fun TournamentDto.toDomain() = Tournament(id, name, ownerUserId, joinCode, playerLimit, playerCount, format, status, createdAt)
}
