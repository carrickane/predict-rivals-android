package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.PredictionDto
import com.balltown.predictrivals.data.dto.SubmitPredictionRequestDto
import com.balltown.predictrivals.domain.model.Prediction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PredictionRepository(private val client: HttpClient) {

    /** Same endpoint handles both first submission and edits, per the doc. */
    suspend fun submit(matchId: Int, homeScore: Int, awayScore: Int): Prediction =
        client.post("$API_BASE_URL/api/predictions") {
            contentType(ContentType.Application.Json)
            setBody(SubmitPredictionRequestDto(matchId, homeScore, awayScore))
        }.body<PredictionDto>().let { Prediction(it.matchId, it.predictedHomeScore, it.predictedAwayScore) }
}
