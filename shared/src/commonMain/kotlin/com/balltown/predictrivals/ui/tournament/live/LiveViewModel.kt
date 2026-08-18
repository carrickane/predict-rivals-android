package com.balltown.predictrivals.ui.tournament.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.repository.LiveRepository
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.domain.model.LiveSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveViewModel(
    private val liveRepository: LiveRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _snapshot = MutableStateFlow<LiveSnapshot?>(null)
    val snapshot: StateFlow<LiveSnapshot?> = _snapshot.asStateFlow()

    fun observe(tournamentId: Int) {
        val accessToken = tokenStore.load()?.accessToken ?: return
        viewModelScope.launch {
            liveRepository.observe(tournamentId, accessToken).collect { _snapshot.value = it }
        }
    }
}
