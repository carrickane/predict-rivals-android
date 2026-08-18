package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveSnapshotDto(val matches: List<MatchDto>, val standings: List<StandingDto>) // UNVERIFIED payload shape
