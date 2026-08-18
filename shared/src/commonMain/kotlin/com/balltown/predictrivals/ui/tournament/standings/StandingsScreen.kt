package com.balltown.predictrivals.ui.tournament.standings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balltown.predictrivals.domain.model.RoundRobinStanding
import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.rank_name_headline
import com.balltown.predictrivals.res.standings_solo_supporting
import com.balltown.predictrivals.res.standings_topscorers_supporting
import com.balltown.predictrivals.res.table_header_draws
import com.balltown.predictrivals.res.table_header_games
import com.balltown.predictrivals.res.table_header_goal_diff
import com.balltown.predictrivals.res.table_header_goals_against
import com.balltown.predictrivals.res.table_header_goals_for
import com.balltown.predictrivals.res.table_header_losses
import com.balltown.predictrivals.res.table_header_player
import com.balltown.predictrivals.res.table_header_points
import com.balltown.predictrivals.res.table_header_wins
import com.balltown.predictrivals.res.top_scorers_label
import com.balltown.predictrivals.ui.components.FullScreenCenter
import com.balltown.predictrivals.ui.components.RefreshableScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PLACE_COL = 28.dp
private val PLAYER_COL = 120.dp
private val STAT_COL = 34.dp
private val POINTS_COL = 44.dp

@Composable
fun StandingsScreen(tournamentId: Int, viewModel: StandingsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    RefreshableScreen(isRefreshing = isRefreshing, onRefresh = { viewModel.refresh(tournamentId) }) {
        when (val current = state) {
            is StandingsUiState.Loading -> FullScreenCenter { CircularProgressIndicator() }
            is StandingsUiState.Error -> FullScreenCenter { Text(current.message) }

            is StandingsUiState.LoadedSolo -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(current.standings) { standing ->
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.rank_name_headline, standing.rank, standing.name)) },
                        supportingContent = {
                            Text(stringResource(Res.string.standings_solo_supporting, standing.totalPoints, standing.exactCount))
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            is StandingsUiState.LoadedRoundRobin -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            ) {
                StandingsTable(current.standings)
                Spacer(Modifier.height(24.dp))
                Text(stringResource(Res.string.top_scorers_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                current.topScorers.forEach { scorer ->
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.rank_name_headline, scorer.rank, scorer.name)) },
                        supportingContent = { Text(stringResource(Res.string.standings_topscorers_supporting, scorer.goalsFor)) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StandingsTable(rows: List<RoundRobinStanding>) {
    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        StandingsTableHeaderRow()
        HorizontalDivider()
        rows.forEach { row -> StandingsTableRow(row) }
    }
}

@Composable
private fun StandingsTableHeaderRow() {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        TableCell("#", PLACE_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_player), PLAYER_COL, FontWeight.Bold, TextAlign.Start)
        TableCell(stringResource(Res.string.table_header_games), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_wins), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_draws), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_losses), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_goals_for), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_goals_against), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_goal_diff), STAT_COL, FontWeight.Bold)
        TableCell(stringResource(Res.string.table_header_points), POINTS_COL, FontWeight.Bold)
    }
}

@Composable
private fun StandingsTableRow(row: RoundRobinStanding) {
    val games = row.wins + row.draws + row.losses
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        TableCell(row.rank.toString(), PLACE_COL)
        TableCell(row.name, PLAYER_COL, textAlign = TextAlign.Start)
        TableCell(games.toString(), STAT_COL)
        TableCell(row.wins.toString(), STAT_COL)
        TableCell(row.draws.toString(), STAT_COL)
        TableCell(row.losses.toString(), STAT_COL)
        TableCell(row.goalsFor.toString(), STAT_COL)
        TableCell(row.goalsAgainst.toString(), STAT_COL)
        TableCell(row.goalDifference.toString(), STAT_COL)
        TableCell(row.leaguePoints.toString(), POINTS_COL)
    }
}

@Composable
private fun TableCell(text: String, width: Dp, fontWeight: FontWeight = FontWeight.Normal, textAlign: TextAlign = TextAlign.Center) {
    Text(text, modifier = Modifier.padding(horizontal = 2.dp).width(width), textAlign = textAlign, fontWeight = fontWeight, fontSize = 13.sp)
}
