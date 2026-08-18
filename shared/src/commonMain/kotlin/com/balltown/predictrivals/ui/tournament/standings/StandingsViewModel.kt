package com.balltown.predictrivals.ui.tournament.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StandingsUiState {
    data object Loading : StandingsUiState()
    data class Loaded(val standings: List<Standing>, val showingTopScorers: Boolean) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel(private val standingsRepository: StandingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    fun load(tournamentId: Int, topScorers: Boolean = false) {
        viewModelScope.launch {
            _state.value = try {
                val standings = if (topScorers) standingsRepository.topScorers(tournamentId) else standingsRepository.standings(tournamentId)
                StandingsUiState.Loaded(standings, topScorers)
            } catch (e: ApiException) {
                StandingsUiState.Error(e.message)
            }
        }
    }
}
