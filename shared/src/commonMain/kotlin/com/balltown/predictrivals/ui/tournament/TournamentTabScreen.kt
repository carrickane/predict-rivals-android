package com.balltown.predictrivals.ui.tournament

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.domain.model.Tournament
import com.balltown.predictrivals.ui.home.HomeUiState
import com.balltown.predictrivals.ui.home.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Tournament tab: info about the current tournament (owner actions included), plus
 * create/join/search for the rest of "my tournaments". Tapping a tournament makes it current,
 * which is what drives the Calendar/Live/Standings tabs.
 */
@Composable
fun TournamentTabScreen(
    onCreateTournament: () -> Unit,
    onJoinTournament: () -> Unit,
    onOpenPredictions: (Tournament) -> Unit,
    onOpenCurate: (Tournament) -> Unit,
    homeViewModel: HomeViewModel = koinViewModel(),
    detailViewModel: TournamentDetailViewModel = koinViewModel(),
) {
    val currentTournamentStore = koinInject<CurrentTournamentStore>()
    val currentTournamentId by currentTournamentStore.currentTournamentId.collectAsState()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(currentTournamentId) {
        currentTournamentId?.let { detailViewModel.load(it) }
        homeViewModel.refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Tournament", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (currentTournamentId != null) {
            CurrentTournamentCard(
                state = detailViewModel.state.collectAsState().value,
                onStartEarly = { currentTournamentId?.let { detailViewModel.startEarly(it) } },
                onOpenPredictions = onOpenPredictions,
                onOpenCurate = onOpenCurate,
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreateTournament) { Text("Create") }
            Button(onClick = onJoinTournament) { Text("Join") }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search my tournaments") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        val state by homeViewModel.state.collectAsState()
        when (val current = state) {
            is HomeUiState.Loading -> CircularProgressIndicator()
            is HomeUiState.Error -> Text(current.message)
            is HomeUiState.Loaded -> {
                val filtered = current.tournaments.filter { it.name.contains(query, ignoreCase = true) }
                if (filtered.isEmpty()) {
                    Text("No tournaments yet — create or join one above.", textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered) { tournament ->
                            ListItem(
                                headlineContent = { Text(tournament.name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                supportingContent = {
                                    Text(
                                        "${tournament.playerCount}/${tournament.playerLimit} players · ${tournament.status}" +
                                            if (tournament.id == currentTournamentId) " · current" else "",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                },
                                modifier = Modifier.padding(vertical = 4.dp).clickable { currentTournamentStore.set(tournament.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTournamentCard(
    state: TournamentDetailUiState,
    onStartEarly: () -> Unit,
    onOpenPredictions: (Tournament) -> Unit,
    onOpenCurate: (Tournament) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is TournamentDetailUiState.Loading -> CircularProgressIndicator()
                is TournamentDetailUiState.Error -> Text(state.message)
                is TournamentDetailUiState.Loaded -> {
                    val tournament = state.tournament
                    val isOwner = tournament.isOwnedBy(state.currentUserId)
                    Text(tournament.name, style = MaterialTheme.typography.titleLarge)
                    Text("${tournament.playerCount}/${tournament.playerLimit} players · ${tournament.status}")
                    Text("Join code: ${tournament.joinCode}")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenPredictions(tournament) }) { Text("Predictions") }
                        if (isOwner && tournament.isOpen) {
                            Button(onClick = onStartEarly) { Text("Start early") }
                        }
                        if (isOwner && tournament.isActive) {
                            Button(onClick = { onOpenCurate(tournament) }) { Text("Curate matches") }
                        }
                    }
                }
            }
        }
    }
}
