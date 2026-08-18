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
    data class LoadedSolo(val standings: List<Standing>, val showingTopScorers: Boolean) : StandingsUiState()
    data class LoadedRoundRobinStandings(val standings: List<RoundRobinStanding>) : StandingsUiState()
    data class LoadedRoundRobinTopScorers(val topScorers: List<RoundRobinTopScorer>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel(
    private val standingsRepository: StandingsRepository,
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    fun load(tournamentId: Int, topScorers: Boolean = false) {
        viewModelScope.launch {
            _state.value = try {
                val tournament = tournamentRepository.get(tournamentId)
                if (tournament.format == "round_robin") {
                    if (topScorers) {
                        StandingsUiState.LoadedRoundRobinTopScorers(standingsRepository.roundRobinTopScorers(tournamentId))
                    } else {
                        StandingsUiState.LoadedRoundRobinStandings(standingsRepository.roundRobinStandings(tournamentId))
                    }
                } else {
                    val standings = if (topScorers) standingsRepository.topScorers(tournamentId) else standingsRepository.standings(tournamentId)
                    StandingsUiState.LoadedSolo(standings, topScorers)
                }
            } catch (e: ApiException) {
                StandingsUiState.Error(e.message)
            }
        }
    }
}
