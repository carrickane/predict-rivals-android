package com.balltown.predictrivals.ui.tournament.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.LiveRepository
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.domain.model.LiveSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class LiveUiState {
    data object Loading : LiveUiState()
    data class Loaded(val snapshot: LiveSnapshot) : LiveUiState()
    data class Error(val message: String) : LiveUiState()
}

class LiveViewModel(
    private val liveRepository: LiveRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    fun observe(tournamentId: Int) {
        val accessToken = tokenStore.load()?.accessToken ?: return
        viewModelScope.launch {
            liveRepository.observe(tournamentId, accessToken)
                .catch { e -> _state.value = LiveUiState.Error(if (e is ApiException) e.message else "Something went wrong") }
                .collect { _state.value = LiveUiState.Loaded(it) }
        }
    }
}
