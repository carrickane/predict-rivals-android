package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.FixtureCandidateDto
import com.balltown.predictrivals.domain.model.Fixture
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FixtureRepository(private val client: HttpClient) {

    // UNVERIFIED: from/to/search param names are a guess — endpoint returned [] for every
    // combination tried against the live backend. Correct these once real params are known.
    suspend fun candidates(from: String, to: String, search: String?): List<Fixture> =
        client.get("$API_BASE_URL/api/fixtures/candidates") {
            parameter("from", from)
            parameter("to", to)
            search?.let { parameter("search", it) }
        }.body<List<FixtureCandidateDto>>().map { Fixture(it.id, it.homeTeam, it.awayTeam, it.kickoffAt, it.competition) }
}
