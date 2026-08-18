# Round-Robin Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consume the round_robin backend (already implemented — see
[2026-08-18-round-robin-backend.md](../../../predict-rivals-backend/docs/superpowers/plans/2026-08-18-round-robin-backend.md))
from the KMM/Compose client: format picker at creation, a league-table standings screen, a
per-round opponent indicator, and a live-round breakdown.

**Architecture:** New DTOs/domain models/repository methods for the round_robin-shaped
endpoints (standings, top-scorers, pairings), added alongside the existing solo-shaped ones —
existing solo code paths are untouched. `StandingsViewModel`/`CalendarViewModel` each resolve
the current tournament's `format` (via `TournamentRepository.get()`) and branch accordingly.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material3), Ktor client, Koin.

**Reference:** [2026-08-18-round-robin-format-design.md](../../../predict-rivals-backend/docs/superpowers/specs/2026-08-18-round-robin-format-design.md) section 7.

**Hard constraints for this session (project CLAUDE.md):** no `git` commands, no
`./gradlew`/compiling. Every task creates/edits files only.

**Scope note — opponent indicator lives in the Calendar screen, not the Live screen.** The
live payload (`/live`) is one shared broadcast per tournament (not per-viewer), so per-user
opponent info deliberately isn't in it (see backend plan Task 10). The Calendar screen already
iterates round-by-round with each round's number, which is exactly what's needed to show "who am
I playing this round" without any live-payload changes.

---

### Task 1: Tournament creation — format field end to end

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/TournamentDto.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/TournamentRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home/CreateTournamentScreen.kt`

- [ ] **Step 1: `TournamentDto.kt` — add `format` to the create request**

Change:
```kotlin
@Serializable
data class CreateTournamentRequestDto(val name: String, val playerLimit: Int)
```
to:
```kotlin
@Serializable
data class CreateTournamentRequestDto(val name: String, val playerLimit: Int, val format: String = "round_robin")
```

- [ ] **Step 2: `TournamentRepository.kt` — pass format through**

Change:
```kotlin
    suspend fun create(name: String, playerLimit: Int): Tournament =
        client.post("$API_BASE_URL/api/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(CreateTournamentRequestDto(name, playerLimit))
        }.body<TournamentDto>().toDomain()
```
to:
```kotlin
    suspend fun create(name: String, playerLimit: Int, format: String): Tournament =
        client.post("$API_BASE_URL/api/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(CreateTournamentRequestDto(name, playerLimit, format))
        }.body<TournamentDto>().toDomain()
```

- [ ] **Step 3: `CreateTournamentScreen.kt` — dropdown + wire format through the ViewModel**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class CreateTournamentUiState {
    data object Idle : CreateTournamentUiState()
    data object Loading : CreateTournamentUiState()
    data class Created(val tournament: Tournament) : CreateTournamentUiState()
    data class Error(val message: String) : CreateTournamentUiState()
}

class CreateTournamentViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {
    private val _state = MutableStateFlow<CreateTournamentUiState>(CreateTournamentUiState.Idle)
    val state: StateFlow<CreateTournamentUiState> = _state.asStateFlow()

    fun create(name: String, playerLimit: Int, format: String) {
        _state.value = CreateTournamentUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                CreateTournamentUiState.Created(tournamentRepository.create(name, playerLimit, format))
            } catch (e: ApiException) {
                CreateTournamentUiState.Error(e.message)
            }
        }
    }
}

private data class TournamentFormatOption(val value: String, val label: String, val enabled: Boolean)

private val FORMAT_OPTIONS = listOf(
    TournamentFormatOption("round_robin", "Round robin", enabled = true),
    TournamentFormatOption("solo_points", "Solo", enabled = true),
    TournamentFormatOption("playoff", "Playoff (coming soon)", enabled = false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(onCreated: (Tournament) -> Unit, viewModel: CreateTournamentViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var playerLimit by remember { mutableStateOf("10") }
    var selectedFormat by remember { mutableStateOf(FORMAT_OPTIONS.first()) }
    var formatMenuExpanded by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()

    (state as? CreateTournamentUiState.Created)?.let { onCreated(it.tournament) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("New tournament")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = playerLimit,
            onValueChange = { playerLimit = it.filter(Char::isDigit) },
            label = { Text("Player limit (2-50)") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(expanded = formatMenuExpanded, onExpandedChange = { formatMenuExpanded = it }) {
            OutlinedTextField(
                value = selectedFormat.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tournament type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenuDefaults.ExposedDropdownMenu(expanded = formatMenuExpanded, onDismissRequest = { formatMenuExpanded = false }) {
                FORMAT_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        enabled = option.enabled,
                        onClick = {
                            selectedFormat = option
                            formatMenuExpanded = false
                        },
                    )
                }
            }
        }
        Button(
            onClick = { playerLimit.toIntOrNull()?.let { viewModel.create(name, it, selectedFormat.value) } },
            enabled = state !is CreateTournamentUiState.Loading,
        ) { Text("Create") }
        if (state is CreateTournamentUiState.Loading) CircularProgressIndicator()
        if (state is CreateTournamentUiState.Error) Text((state as CreateTournamentUiState.Error).message)
    }
}
```

**Unverified detail:** `ExposedDropdownMenuBox`/`.menuAnchor()` API shape (whether
`menuAnchor()` takes a `MenuAnchorType` argument in this project's exact Compose Material3
version) can't be confirmed without compiling — if the build fails here, adjust `.menuAnchor()`
to whatever this version's signature requires; the rest of the dropdown logic is unaffected.

---

### Task 2: Round-robin standings/top-scorers — DTOs, domain, repository

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/StandingsDto.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Standing.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/StandingsRepository.kt`

- [ ] **Step 1: `StandingsDto.kt` — add the round_robin response shapes**

Add to the file (existing `StandingDto`/`UserStatsDto` stay unchanged):
```kotlin
@Serializable
data class RoundRobinStandingDto(
    val rank: Int,
    val userId: Int,
    val name: String,
    val leaguePoints: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
)

@Serializable
data class RoundRobinTopScorerDto(val rank: Int, val userId: Int, val name: String, val goalsFor: Int)
```

- [ ] **Step 2: `Standing.kt` — add the round_robin domain models**

Add to the file (existing `Standing`/`UserStats` stay unchanged):
```kotlin
data class RoundRobinStanding(
    val rank: Int,
    val userId: Int,
    val name: String,
    val leaguePoints: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
)

data class RoundRobinTopScorer(val rank: Int, val userId: Int, val name: String, val goalsFor: Int)
```

- [ ] **Step 3: `StandingsRepository.kt` — add the fetch methods**

Add imports `com.balltown.predictrivals.data.dto.RoundRobinStandingDto`,
`com.balltown.predictrivals.data.dto.RoundRobinTopScorerDto`,
`com.balltown.predictrivals.domain.model.RoundRobinStanding`,
`com.balltown.predictrivals.domain.model.RoundRobinTopScorer`. Add to the class:
```kotlin
    suspend fun roundRobinStandings(tournamentId: Int): List<RoundRobinStanding> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/standings").body<List<RoundRobinStandingDto>>().map { it.toDomain() }

    suspend fun roundRobinTopScorers(tournamentId: Int): List<RoundRobinTopScorer> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/top-scorers").body<List<RoundRobinTopScorerDto>>().map { it.toDomain() }

    private fun RoundRobinStandingDto.toDomain() =
        RoundRobinStanding(rank, userId, name, leaguePoints, wins, draws, losses, goalsFor, goalsAgainst, goalDifference)

    private fun RoundRobinTopScorerDto.toDomain() = RoundRobinTopScorer(rank, userId, name, goalsFor)
```
(these are additive — the existing `standings()`/`topScorers()`/`userStats()` methods and their
`StandingDto.toDomain()` helper stay exactly as they are.)

---

### Task 3: Standings screen — branch by tournament format

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/standings/StandingsViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/standings/StandingsScreen.kt`

- [ ] **Step 1: `StandingsViewModel.kt` — resolve format, branch, fetch**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.tournament.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.RoundRobinStanding
import com.balltown.predictrivals.domain.model.RoundRobinTopScorer
import com.balltown.predictrivals.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StandingsUiState {
    data object Loading : StandingsUiState()
    data class LoadedSolo(val standings: List<Standing>, val showingTopScorers: Boolean) : StandingsUiState()
    data class LoadedRoundRobinStandings(val standings: List<RoundRobinStanding>) : StandingsUiState()
    data class LoadedRoundRobinTopScorers(val topScorers: List<RoundRobinTopScorer>) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel(
    private val standingsRepository: StandingsRepository,
    private val tournamentRepository: TournamentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    fun load(tournamentId: Int, topScorers: Boolean = false) {
        viewModelScope.launch {
            _state.value = try {
                val tournament = tournamentRepository.get(tournamentId)
                if (tournament.format == "round_robin") {
                    if (topScorers) {
                        StandingsUiState.LoadedRoundRobinTopScorers(standingsRepository.roundRobinTopScorers(tournamentId))
                    } else {
                        StandingsUiState.LoadedRoundRobinStandings(standingsRepository.roundRobinStandings(tournamentId))
                    }
                } else {
                    val standings = if (topScorers) standingsRepository.topScorers(tournamentId) else standingsRepository.standings(tournamentId)
                    StandingsUiState.LoadedSolo(standings, topScorers)
                }
            } catch (e: ApiException) {
                StandingsUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: `StandingsScreen.kt` — render the league table for round_robin**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.tournament.standings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
private fun TopScorersToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row {
        Text("Top scorers")
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun StandingsScreen(tournamentId: Int, viewModel: StandingsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is StandingsUiState.Loading -> CircularProgressIndicator()
        is StandingsUiState.Error -> Text(current.message)

        is StandingsUiState.LoadedSolo -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(current.showingTopScorers) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.standings) { standing ->
                    ListItem(
                        headlineContent = { Text("#${standing.rank} ${standing.name}") },
                        supportingContent = { Text("${standing.totalPoints} pts · ${standing.exactCount} exact") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        is StandingsUiState.LoadedRoundRobinStandings -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(checked = false) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.standings) { row ->
                    ListItem(
                        headlineContent = { Text("#${row.rank} ${row.name}") },
                        supportingContent = {
                            Text("${row.leaguePoints} pts · ${row.wins}W ${row.draws}D ${row.losses}L · GF ${row.goalsFor} GA ${row.goalsAgainst} (GD ${row.goalDifference})")
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        is StandingsUiState.LoadedRoundRobinTopScorers -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TopScorersToggle(checked = true) { viewModel.load(tournamentId, topScorers = it) }
            LazyColumn {
                items(current.topScorers) { row ->
                    ListItem(
                        headlineContent = { Text("#${row.rank} ${row.name}") },
                        supportingContent = { Text("${row.goalsFor} goals") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
```

---

### Task 4: Pairings — DTO, domain, repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/PairingDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Pairing.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/PairingsRepository.kt`

- [ ] **Step 1: `PairingDto.kt`**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingDto(val roundNumber: Int, val opponentUserId: Int?, val opponentName: String?, val isBotMatch: Boolean)
```

- [ ] **Step 2: `Pairing.kt`**

```kotlin
package com.balltown.predictrivals.domain.model

data class Pairing(val roundNumber: Int, val opponentUserId: Int?, val opponentName: String?, val isBotMatch: Boolean)
```

- [ ] **Step 3: `PairingsRepository.kt`**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.PairingDto
import com.balltown.predictrivals.domain.model.Pairing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PairingsRepository(private val client: HttpClient) {

    suspend fun mySchedule(tournamentId: Int): List<Pairing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/pairings").body<List<PairingDto>>().map { it.toDomain() }

    private fun PairingDto.toDomain() = Pairing(roundNumber, opponentUserId, opponentName, isBotMatch)
}
```

---

### Task 5: Calendar screen — opponent per round

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/calendar/CalendarViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/calendar/CalendarScreen.kt`

- [ ] **Step 1: `CalendarViewModel.kt` — fetch the schedule alongside the calendar for round_robin**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.tournament.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.PairingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Pairing
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Loaded(val rounds: List<Round>, val opponentsByRound: Map<Int, Pairing>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val tournamentRepository: TournamentRepository,
    private val pairingsRepository: PairingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                val rounds = calendarRepository.calendar(tournamentId)
                val tournament = tournamentRepository.get(tournamentId)
                val opponentsByRound = if (tournament.format == "round_robin") {
                    pairingsRepository.mySchedule(tournamentId).associateBy { it.roundNumber }
                } else {
                    emptyMap()
                }
                CalendarUiState.Loaded(rounds, opponentsByRound)
            } catch (e: ApiException) {
                CalendarUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: `CalendarScreen.kt` — show the opponent line above each round's matches**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.tournament.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(tournamentId: Int, viewModel: CalendarViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is CalendarUiState.Loading -> CircularProgressIndicator()
        is CalendarUiState.Error -> Text(current.message)
        is CalendarUiState.Loaded -> if (current.rounds.isEmpty()) {
            Text("No rounds yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                current.rounds.forEach { round ->
                    val opponent = current.opponentsByRound[round.roundNumber]
                    if (opponent != null) {
                        item {
                            Text(
                                text = "Round ${round.roundNumber}: vs " + if (opponent.isBotMatch) "BOT" else (opponent.opponentName ?: "Unknown"),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                    items(round.matches) { match ->
                        ListItem(
                            headlineContent = { Text("${match.homeTeam} vs ${match.awayTeam}") },
                            supportingContent = { Text("Round ${round.roundNumber} · ${match.status}") },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
```

---

### Task 6: Live payload — round_robin standings + round-in-progress goals

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/LiveDto.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/LiveSnapshot.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/LiveRepository.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/live/LiveScreen.kt`

- [ ] **Step 1: `LiveDto.kt` — replace with the format-flexible shape**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveStandingDto(
    val rank: Int,
    val userId: Int,
    val name: String,
    val totalPoints: Int? = null,
    val exactCount: Int? = null,
    val leaguePoints: Int? = null,
    val wins: Int? = null,
    val draws: Int? = null,
    val losses: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
)

@Serializable
data class LiveRoundScoreDto(val userId: Int, val name: String, val roundPoints: Int)

@Serializable
data class LiveSnapshotDto(
    val matches: List<MatchDto>,
    val standings: List<LiveStandingDto>,
    val roundScores: List<LiveRoundScoreDto> = emptyList(),
)
```

(This replaces the old `LiveSnapshotDto(matches, standings: List<StandingDto>)` shape — the
plain `StandingDto` used by the non-live `/standings` endpoint is untouched; this is a separate
type specifically for the live payload, matching the backend's `LiveStandingEntry`.)

- [ ] **Step 2: `LiveSnapshot.kt` — matching domain models**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.domain.model

data class LiveStanding(
    val rank: Int,
    val userId: Int,
    val name: String,
    val totalPoints: Int? = null,
    val exactCount: Int? = null,
    val leaguePoints: Int? = null,
    val wins: Int? = null,
    val draws: Int? = null,
    val losses: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
) {
    val isRoundRobin get() = leaguePoints != null
}

data class LiveRoundScore(val userId: Int, val name: String, val roundPoints: Int)

data class LiveSnapshot(val matches: List<Match>, val standings: List<LiveStanding>, val roundScores: List<LiveRoundScore> = emptyList())
```

- [ ] **Step 3: `LiveRepository.kt` — update the mapping**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.LiveRoundScoreDto
import com.balltown.predictrivals.data.dto.LiveSnapshotDto
import com.balltown.predictrivals.data.dto.LiveStandingDto
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.domain.model.LiveRoundScore
import com.balltown.predictrivals.domain.model.LiveSnapshot
import com.balltown.predictrivals.domain.model.LiveStanding
import com.balltown.predictrivals.domain.model.Match
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

private val liveJson = Json { ignoreUnknownKeys = true }

class LiveRepository(private val client: HttpClient) {

    suspend fun snapshot(tournamentId: Int): LiveSnapshot =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/live").body<LiveSnapshotDto>().toDomain()

    /**
     * Pushes a [LiveSnapshot] on every WebSocket frame. If the socket can't be established after
     * [maxReconnectAttempts] tries (exponential backoff), falls back to polling [snapshot] every
     * [pollIntervalMillis] instead of giving up.
     */
    fun observe(
        tournamentId: Int,
        accessToken: String,
        maxReconnectAttempts: Int = 3,
        pollIntervalMillis: Long = 10_000,
    ): Flow<LiveSnapshot> = flow {
        var attempt = 0
        var socketSucceededOnce = false
        while (attempt < maxReconnectAttempts && !socketSucceededOnce) {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = API_BASE_URL.removePrefix("https://").removePrefix("http://"),
                    path = "/ws/tournaments/$tournamentId/live",
                    request = { url.parameters.append("token", accessToken) },
                ) {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            socketSucceededOnce = true
                            emit(liveJson.decodeFromString<LiveSnapshotDto>(frame.readText()).toDomain())
                        }
                    }
                }
            } catch (e: Exception) {
                attempt++
                delay(backoffMillis(attempt))
            }
        }
        while (!socketSucceededOnce) {
            emit(snapshot(tournamentId))
            delay(pollIntervalMillis)
        }
    }.catch { emit(snapshot(tournamentId)) }

    private fun backoffMillis(attempt: Int): Long = 500L * (1 shl attempt)

    private fun LiveSnapshotDto.toDomain() = LiveSnapshot(
        matches.map { Match(it.id, it.homeTeam, it.awayTeam, it.kickoffAt, it.homeScore, it.awayScore, it.status) },
        standings.map { it.toDomain() },
        roundScores.map { it.toDomain() },
    )

    private fun LiveStandingDto.toDomain() =
        LiveStanding(rank, userId, name, totalPoints, exactCount, leaguePoints, wins, draws, losses, goalsFor, goalsAgainst)

    private fun LiveRoundScoreDto.toDomain() = LiveRoundScore(userId, name, roundPoints)
}
```

- [ ] **Step 4: `LiveScreen.kt` — render league-table fields when present**

Replace the whole file:

```kotlin
package com.balltown.predictrivals.ui.tournament.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveScreen(tournamentId: Int, viewModel: LiveViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.observe(tournamentId) }
    val snapshot by viewModel.snapshot.collectAsState()

    val current = snapshot
    if (current == null) {
        CircularProgressIndicator()
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            current.matches.forEach { match ->
                Text("${match.homeTeam} ${match.homeScore ?: "-"} : ${match.awayScore ?: "-"} ${match.awayTeam} (${match.status})")
            }
            Text("Standings")
            current.standings.forEach { standing ->
                if (standing.isRoundRobin) {
                    val goalDiff = (standing.goalsFor ?: 0) - (standing.goalsAgainst ?: 0)
                    Text("#${standing.rank} ${standing.name}: ${standing.leaguePoints} pts (${standing.wins}W ${standing.draws}D ${standing.losses}L, GD $goalDiff)")
                } else {
                    Text("#${standing.rank} ${standing.name}: ${standing.totalPoints}")
                }
            }
            if (current.roundScores.isNotEmpty()) {
                Text("This round")
                current.roundScores.forEach { entry -> Text("${entry.name}: ${entry.roundPoints}") }
            }
        }
    }
}
```

---

### Task 7: Wire DI — `AppModule.kt` and `ViewModelModule.kt`

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/AppModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/ViewModelModule.kt`

- [ ] **Step 1: `AppModule.kt` — register `PairingsRepository`**

Add the import `com.balltown.predictrivals.data.repository.PairingsRepository`, and add
`single { PairingsRepository(get()) }` alongside the other `single { ... }` repository
registrations.

- [ ] **Step 2: `ViewModelModule.kt` — update constructor arg counts**

Change:
```kotlin
    viewModel { StandingsViewModel(get()) }
    viewModel { CalendarViewModel(get()) }
```
to:
```kotlin
    viewModel { StandingsViewModel(get(), get()) }
    viewModel { CalendarViewModel(get(), get(), get()) }
```
(Koin resolves each `get()` positionally by the constructor's parameter types, which already
uniquely identify `StandingsRepository`/`TournamentRepository` and
`CalendarRepository`/`TournamentRepository`/`PairingsRepository` respectively — no named
qualifiers needed.)

---

## Self-review

**Spec coverage:** format picker at creation ✓ (Task 1), round_robin standings/top-scorers UI ✓
(Tasks 2–3), opponent indicator ✓ (Tasks 4–5, scoped to Calendar per the note above), live
league-table + round-in-progress goals ✓ (Task 6), DI ✓ (Task 7). The design doc's "new
schedule/fixtures screen (or a section of the existing calendar screen)" is resolved as the
latter — reusing Calendar rather than adding a new nav route.

**Placeholders:** none — every step has complete file contents. One flagged unverified detail
(Task 1's `menuAnchor()` signature) is explicitly called out with its fallback, not hidden.

**Type/name consistency:** `RoundRobinStanding`/`RoundRobinTopScorer`/`Pairing`/`LiveStanding`/
`LiveRoundScore` field names match their DTO counterparts exactly (checked against Task 2's and
Task 6's `toDomain()` mappings), and against the backend response shapes from
`2026-08-18-round-robin-backend.md` Tasks 9–11.
