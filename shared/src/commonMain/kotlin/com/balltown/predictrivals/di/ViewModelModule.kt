package com.balltown.predictrivals.di

import com.balltown.predictrivals.ui.auth.AuthViewModel
import com.balltown.predictrivals.ui.home.CreateTournamentViewModel
import com.balltown.predictrivals.ui.home.HomeViewModel
import com.balltown.predictrivals.ui.home.JoinTournamentViewModel
import com.balltown.predictrivals.ui.profile.ProfileViewModel
import com.balltown.predictrivals.ui.tournament.calendar.CalendarViewModel
import com.balltown.predictrivals.ui.tournament.curate.CurateViewModel
import com.balltown.predictrivals.ui.tournament.live.LiveViewModel
import com.balltown.predictrivals.ui.tournament.predictions.PredictionsViewModel
import com.balltown.predictrivals.ui.tournament.standings.StandingsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { CreateTournamentViewModel(get()) }
    viewModel { JoinTournamentViewModel(get()) }
    viewModel { StandingsViewModel(get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get()) }
    viewModel { PredictionsViewModel(get(), get()) }
    viewModel { CurateViewModel(get(), get()) }
    viewModel { LiveViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
}
