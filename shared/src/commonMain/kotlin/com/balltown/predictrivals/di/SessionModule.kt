package com.balltown.predictrivals.di

import com.balltown.predictrivals.data.storage.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/** Holds the signed-in user's id for the lifetime of the process; cleared on logout. */
class SessionStore {
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()
    fun set(userId: Int) { _currentUserId.value = userId }
    fun clear() { _currentUserId.value = null }
}

/**
 * Which tournament the Calendar/Live/Standings tabs act on. Seeded from the last tournament the
 * user opened/created/joined ([AppPreferences.lastTournamentId]) so it survives process death;
 * null means the user hasn't picked one yet (or has none) and those tabs should show an empty state.
 */
class CurrentTournamentStore(private val appPreferences: AppPreferences) {
    private val _currentTournamentId = MutableStateFlow(appPreferences.lastTournamentId)
    val currentTournamentId: StateFlow<Int?> = _currentTournamentId.asStateFlow()

    fun set(tournamentId: Int) {
        _currentTournamentId.value = tournamentId
        appPreferences.lastTournamentId = tournamentId
    }

    fun clear() {
        _currentTournamentId.value = null
        appPreferences.lastTournamentId = null
    }
}

val sessionModule = module {
    single { SessionStore() }
    single { CurrentTournamentStore(get()) }
}
