package com.balltown.predictrivals.domain.scoring

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun isPredictionEditable(kickoffAt: Instant, now: Instant = Clock.System.now()): Boolean = now < kickoffAt
