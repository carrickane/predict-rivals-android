package com.balltown.predictrivals.ui.tournament.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.RoundRobinStanding
import com.balltown.predictrivals.domain.model.RoundRobinTopScorer
import com.balltown.predictrivals.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StandingsUiState {
    data object Loading : StandingsUiState()
    data class LoadedSolo(val standings: List<Standing>) : StandingsUiState()
    data class LoadedRoundRobin(val standings: List<RoundRobinStanding>, val topScorers: List<RoundRobinTopScorer>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel(
    private val standingsRepository: StandingsRepository,
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** solo_points has no separate "goals" ranking worth surfacing — points are the whole story
     * there, so only round_robin fetches the scorers list alongside the table. */
    private suspend fun fetch(tournamentId: Int): StandingsUiState = try {
        val tournament = tournamentRepository.get(tournamentId)
        if (tournament.format == "round_robin") {
            StandingsUiState.LoadedRoundRobin(
                standingsRepository.roundRobinStandings(tournamentId),
                standingsRepository.roundRobinTopScorers(tournamentId),
            )
        } else {
            StandingsUiState.LoadedSolo(standingsRepository.standings(tournamentId))
        }
    } catch (e: ApiException) {
        StandingsUiState.Error(e.message)
    }

    fun load(tournamentId: Int) {
        viewModelScope.launch { _state.value = fetch(tournamentId) }
    }

    fun refresh(tournamentId: Int) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = fetch(tournamentId)
            _isRefreshing.value = false
        }
    }
}
