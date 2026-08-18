package com.balltown.predictrivals.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) = runAuthCall { authRepository.login(email, password) }

    fun register(email: String, password: String, name: String) = runAuthCall { authRepository.register(email, password, name) }

    private fun runAuthCall(call: suspend () -> User) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val user = call()
                sessionStore.set(user.id)
                AuthUiState.Success
            } catch (e: ApiException) {
                AuthUiState.Error(e.message)
            }
        }
    }
}
