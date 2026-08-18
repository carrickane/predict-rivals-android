package com.balltown.predictrivals.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/** Small pieces of local state that must survive process death, alongside [TokenStore]. */
class AppPreferences(private val settings: Settings = Settings()) {

    var userId: Int?
        get() = settings[KEY_USER_ID, -1].takeIf { it != -1 }
        set(value) {
            if (value == null) settings.remove(KEY_USER_ID) else settings[KEY_USER_ID] = value
        }

    var lastTournamentId: Int?
        get() = settings[KEY_LAST_TOURNAMENT_ID, -1].takeIf { it != -1 }
        set(value) {
            if (value == null) settings.remove(KEY_LAST_TOURNAMENT_ID) else settings[KEY_LAST_TOURNAMENT_ID] = value
        }

    var hasAcceptedTerms: Boolean
        get() = settings[KEY_ACCEPTED_TERMS, false]
        set(value) { settings[KEY_ACCEPTED_TERMS] = value }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_LAST_TOURNAMENT_ID = "last_tournament_id"
        const val KEY_ACCEPTED_TERMS = "accepted_terms"
    }
}
