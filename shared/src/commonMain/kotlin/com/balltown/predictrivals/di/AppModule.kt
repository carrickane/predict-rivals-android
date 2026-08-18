package com.balltown.predictrivals.di

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.api.buildApiClient
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.CurationRepository
import com.balltown.predictrivals.data.repository.FixtureRepository
import com.balltown.predictrivals.data.repository.LiveRepository
import com.balltown.predictrivals.data.repository.PredictionRepository
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.data.storage.AppPreferences
import com.balltown.predictrivals.data.storage.TokenStore
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module

expect fun platformHttpClientEngine(): HttpClientEngine

val appModule = module {
    single { TokenStore() }
    single { AppPreferences() }
    single { AuthApi(platformHttpClientEngine(), API_BASE_URL) }
    single { buildApiClient(platformHttpClientEngine(), API_BASE_URL, get(), get(), get()) }
    single { AuthRepository(get(), get(), get(), get()) }
    single { TournamentRepository(get()) }
    single { StandingsRepository(get()) }
    single { CalendarRepository(get()) }
    single { FixtureRepository(get()) }
    single { CurationRepository(get()) }
    single { PredictionRepository(get()) }
    single { LiveRepository(get()) }
}
