package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.PairingDto
import com.balltown.predictrivals.domain.model.Pairing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PairingsRepository(private val client: HttpClient) {

    suspend fun mySchedule(tournamentId: Int): List<Pairing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/pairings").body<List<PairingDto>>().map { it.toDomain() }

    private fun PairingDto.toDomain() = Pairing(roundNumber, opponentUserId, opponentName, isBotMatch)
}
