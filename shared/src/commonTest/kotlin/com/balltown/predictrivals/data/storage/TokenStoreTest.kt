package com.balltown.predictrivals.data.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStoreTest {

    @Test
    fun `load returns null when nothing saved`() {
        val store = TokenStore(MapSettings())
        assertNull(store.load())
    }

    @Test
    fun `save then load round-trips both tokens`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair(accessToken = "access-1", refreshToken = "refresh-1"))
        assertEquals(TokenPair("access-1", "refresh-1"), store.load())
    }

    @Test
    fun `save again overwrites the previous pair (refresh tokens are single-use)`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair("access-1", "refresh-1"))
        store.save(TokenPair("access-2", "refresh-2"))
        assertEquals(TokenPair("access-2", "refresh-2"), store.load())
    }

    @Test
    fun `clear removes both tokens`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair("access-1", "refresh-1"))
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `load returns null when only one of the two tokens is present`() {
        val settings = MapSettings()
        val store = TokenStore(settings)
        store.save(TokenPair("access-1", "refresh-1"))

        settings.remove("refresh_token")
        assertNull(store.load())

        store.save(TokenPair("access-1", "refresh-1"))
        settings.remove("access_token")
        assertNull(store.load())
    }
}
