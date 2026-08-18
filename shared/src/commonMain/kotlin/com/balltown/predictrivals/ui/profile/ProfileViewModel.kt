package com.balltown.predictrivals.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.domain.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    /** [stats] is null when the user has no current tournament selected — that's expected, not an error. */
    data class Loaded(val stats: UserStats?) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class DeleteAccountState {
    data object Idle : DeleteAccountState()
    data object Loading : DeleteAccountState()
    data object Deleted : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

class ProfileViewModel(
    private val standingsRepository: StandingsRepository,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val currentTournamentStore: CurrentTournamentStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    fun load(tournamentId: Int?) {
        val userId = sessionStore.currentUserId.value
        if (tournamentId == null || userId == null) {
            _state.value = ProfileUiState.Loaded(stats = null)
            return
        }
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                ProfileUiState.Loaded(standingsRepository.userStats(tournamentId, userId))
            } catch (e: ApiException) {
                ProfileUiState.Error(e.message)
            }
        }
    }

    fun logout() {
        authRepository.logout()
        sessionStore.clear()
        currentTournamentStore.clear()
    }

    fun deleteAccount() {
        _deleteAccountState.value = DeleteAccountState.Loading
        viewModelScope.launch {
            _deleteAccountState.value = try {
                authRepository.deleteAccount()
                sessionStore.clear()
                currentTournamentStore.clear()
                DeleteAccountState.Deleted
            } catch (e: ApiException) {
                DeleteAccountState.Error(e.message)
            }
        }
    }
}
