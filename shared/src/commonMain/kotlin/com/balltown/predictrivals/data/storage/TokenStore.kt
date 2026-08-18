package com.balltown.predictrivals.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

data class TokenPair(val accessToken: String, val refreshToken: String)

class TokenStore(private val settings: Settings = Settings()) {

    fun save(tokens: TokenPair) {
        settings[KEY_ACCESS] = tokens.accessToken
        settings[KEY_REFRESH] = tokens.refreshToken
    }

    fun load(): TokenPair? {
        val access: String? = settings[KEY_ACCESS]
        val refresh: String? = settings[KEY_REFRESH]
        if (access == null || refresh == null) return null
        return TokenPair(access, refresh)
    }

    fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
