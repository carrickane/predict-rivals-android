package com.balltown.predictrivals.ui.tournament.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.PairingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Pairing
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Loaded(val rounds: List<Round>, val opponentsByRound: Map<Int, Pairing>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val tournamentRepository: TournamentRepository,
    private val pairingsRepository: PairingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private suspend fun fetch(tournamentId: Int): CalendarUiState = try {
        val rounds = calendarRepository.calendar(tournamentId)
        val tournament = tournamentRepository.get(tournamentId)
        val opponentsByRound = if (tournament.format == "round_robin") {
            pairingsRepository.mySchedule(tournamentId).associateBy { it.roundNumber }
        } else {
            emptyMap()
        }
        CalendarUiState.Loaded(rounds, opponentsByRound)
    } catch (e: ApiException) {
        CalendarUiState.Error(e.message)
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
