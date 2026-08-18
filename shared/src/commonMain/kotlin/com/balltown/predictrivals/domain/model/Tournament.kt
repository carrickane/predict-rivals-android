package com.balltown.predictrivals.domain.model

data class Tournament(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val joinCode: String,
    val playerLimit: Int,
    val playerCount: Int,
    val format: String,
    val status: String,
    val createdAt: String,
) {
    val isOpen get() = status == "open"
    val isActive get() = status == "active"
    fun isOwnedBy(userId: Int) = ownerUserId == userId
}
