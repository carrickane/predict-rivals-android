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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.domain.model.Tournament
import com.balltown.predictrivals.platform.rememberShareAction
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.action_create
import com.balltown.predictrivals.res.action_curate_matches
import com.balltown.predictrivals.res.action_join
import com.balltown.predictrivals.res.action_predictions
import com.balltown.predictrivals.res.action_share
import com.balltown.predictrivals.res.action_start_early
import com.balltown.predictrivals.res.join_code_label
import com.balltown.predictrivals.res.no_tournaments_yet
import com.balltown.predictrivals.res.search_my_tournaments
import com.balltown.predictrivals.res.share_invite_message
import com.balltown.predictrivals.res.tournament_players_status
import com.balltown.predictrivals.res.tournament_tab_title
import com.balltown.predictrivals.ui.components.RefreshableScreen
import com.balltown.predictrivals.ui.home.HomeUiState
import com.balltown.predictrivals.ui.home.HomeViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Tournament tab: search + list of "my tournaments". Tapping an item makes it the current
 * tournament (which is what drives the Calendar/Live/Standings tabs) and expands that same row
 * in place to show its details and actions — no separate summary card above the list.
 */
@Composable
fun TournamentTabScreen(
    onCreateTournament: () -> Unit,
    onJoinTournament: () -> Unit,
    onOpenPredictions: (Tournament) -> Unit,
    onOpenCurate: (Tournament) -> Unit,
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    val currentTournamentStore = koinInject<CurrentTournamentStore>()
    val currentTournamentId by currentTournamentStore.currentTournamentId.collectAsState()
    val sessionStore = koinInject<SessionStore>()
    val currentUserId by sessionStore.currentUserId.collectAsState()
    val shareAction = rememberShareAction()
    var query by remember { mutableStateOf("") }

    val state by homeViewModel.state.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()

    RefreshableScreen(
        isRefreshing = isRefreshing,
        onRefresh = { homeViewModel.refresh() },
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.tournament_tab_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateTournament) { Text(stringResource(Res.string.action_create)) }
                Button(onClick = onJoinTournament) { Text(stringResource(Res.string.action_join)) }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.search_my_tournaments)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            when (val current = state) {
                is HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Error -> Text(current.message)
                is HomeUiState.Loaded -> {
                    val filtered = current.tournaments.filter { it.name.contains(query, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        Text(stringResource(Res.string.no_tournaments_yet), textAlign = TextAlign.Center)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(filtered) { tournament ->
                                TournamentListItem(
                                    tournament = tournament,
                                    isCurrent = tournament.id == currentTournamentId,
                                    isOwner = currentUserId != null && tournament.isOwnedBy(currentUserId!!),
                                    onClick = { currentTournamentStore.set(tournament.id) },
                                    onStartEarly = { homeViewModel.startEarly(tournament.id) },
                                    onOpenPredictions = { onOpenPredictions(tournament) },
                                    onOpenCurate = { onOpenCurate(tournament) },
                                    onShare = shareAction,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentListItem(
    tournament: Tournament,
    isCurrent: Boolean,
    isOwner: Boolean,
    onClick: () -> Unit,
    onStartEarly: () -> Unit,
    onOpenPredictions: () -> Unit,
    onOpenCurate: () -> Unit,
    onShare: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(tournament.name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            supportingContent = {
                Text(
                    stringResource(Res.string.tournament_players_status, tournament.playerCount, tournament.playerLimit, tournament.status),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            modifier = Modifier.padding(vertical = 4.dp),
        )
        if (isCurrent) {
            val shareMessage = stringResource(Res.string.share_invite_message, tournament.name, tournament.joinCode)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Text(stringResource(Res.string.join_code_label, tournament.joinCode))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenPredictions) { Text(stringResource(Res.string.action_predictions)) }
                    Button(onClick = { onShare(shareMessage) }) { Text(stringResource(Res.string.action_share)) }
                }
                if (isOwner && (tournament.isOpen || tournament.isActive)) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tournament.isOpen) {
                            Button(onClick = onStartEarly) { Text(stringResource(Res.string.action_start_early)) }
                        }
                        if (tournament.isActive) {
                            Button(onClick = onOpenCurate) { Text(stringResource(Res.string.action_curate_matches)) }
                        }
                    }
                }
            }
        }
    }
}
