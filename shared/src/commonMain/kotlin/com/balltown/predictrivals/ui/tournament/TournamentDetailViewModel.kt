package com.balltown.predictrivals.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TournamentDetailUiState {
    data object Loading : TournamentDetailUiState()
    data class Loaded(val tournament: Tournament, val currentUserId: Int) : TournamentDetailUiState()
    data class Error(val message: String) : TournamentDetailUiState()
}

class TournamentDetailViewModel(
    private val tournamentRepository: TournamentRepository,
    private val currentUserId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<TournamentDetailUiState>(TournamentDetailUiState.Loading)
    val state: StateFlow<TournamentDetailUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private suspend fun fetch(tournamentId: Int): TournamentDetailUiState = try {
        TournamentDetailUiState.Loaded(tournamentRepository.get(tournamentId), currentUserId)
    } catch (e: ApiException) {
        TournamentDetailUiState.Error(e.message)
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

    fun startEarly(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                TournamentDetailUiState.Loaded(tournamentRepository.start(tournamentId), currentUserId)
            } catch (e: ApiException) {
                TournamentDetailUiState.Error(e.message)
            }
        }
    }
}
