package com.balltown.predictrivals.ui.tournament.predictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.PredictionRepository
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PredictionsUiState {
    data object Loading : PredictionsUiState()
    data class Loaded(val round: Round?) : PredictionsUiState()
    data class Error(val message: String) : PredictionsUiState()
}

class PredictionsViewModel(
    private val calendarRepository: CalendarRepository,
    private val predictionRepository: PredictionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<PredictionsUiState>(PredictionsUiState.Loading)
    val state: StateFlow<PredictionsUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private suspend fun fetch(tournamentId: Int): PredictionsUiState = try {
        PredictionsUiState.Loaded(calendarRepository.currentRound(tournamentId))
    } catch (e: ApiException) {
        PredictionsUiState.Error(e.message)
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

    fun submit(matchId: Int, homeScore: Int, awayScore: Int) {
        viewModelScope.launch {
            try {
                predictionRepository.submit(matchId, homeScore, awayScore)
            } catch (e: ApiException) {
                _state.value = PredictionsUiState.Error(e.message)
            }
        }
    }
}
