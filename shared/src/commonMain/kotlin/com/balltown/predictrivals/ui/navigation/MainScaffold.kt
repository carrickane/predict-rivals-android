package com.balltown.predictrivals.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.balltown.predictrivals.domain.model.Tournament
import com.balltown.predictrivals.ui.profile.ProfileScreen
import com.balltown.predictrivals.ui.tournament.RequiresCurrentTournament
import com.balltown.predictrivals.ui.tournament.TournamentTabScreen
import com.balltown.predictrivals.ui.tournament.calendar.CalendarScreen
import com.balltown.predictrivals.ui.tournament.live.LiveScreen
import com.balltown.predictrivals.ui.tournament.standings.StandingsScreen

private val TAB_ICONS = mapOf(
    MainTab.Calendar to "📅",
    MainTab.Live to "🔴",
    MainTab.Tournament to "🏆",
    MainTab.Standings to "📊",
    MainTab.Profile to "👤",
)

// Material3's compact/medium window-size-class boundary. Below it: phone-style bottom
// NavigationBar (unchanged). At or above it (tablets landscape, desktop browser windows): a side
// NavigationRail, which is the idiomatic wide-viewport pattern instead of a stretched bottom bar.
private val WIDE_LAYOUT_BREAKPOINT = 600.dp

@Composable
fun MainScaffold(
    onLoggedOut: () -> Unit,
    onCreateTournament: () -> Unit,
    onJoinTournament: () -> Unit,
    onOpenPredictions: (Tournament) -> Unit,
    onOpenCurate: (Tournament) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    val tabNavController = rememberNavController()

    fun goToTournamentTab() {
        tabNavController.navigate(MainTab.Tournament.route) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val tabContent: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(navController = tabNavController, startDestination = MainTab.Tournament.route, modifier = modifier) {
            composable(MainTab.Calendar.route) {
                RequiresCurrentTournament(onGoToTournamentTab = ::goToTournamentTab) { tournamentId ->
                    CalendarScreen(tournamentId)
                }
            }
            composable(MainTab.Live.route) {
                RequiresCurrentTournament(onGoToTournamentTab = ::goToTournamentTab) { tournamentId ->
                    LiveScreen(tournamentId)
                }
            }
            composable(MainTab.Tournament.route) {
                TournamentTabScreen(
                    onCreateTournament = onCreateTournament,
                    onJoinTournament = onJoinTournament,
                    onOpenPredictions = onOpenPredictions,
                    onOpenCurate = onOpenCurate,
                )
            }
            composable(MainTab.Standings.route) {
                RequiresCurrentTournament(onGoToTournamentTab = ::goToTournamentTab) { tournamentId ->
                    StandingsScreen(tournamentId)
                }
            }
            composable(MainTab.Profile.route) {
                ProfileScreen(
                    onLoggedOut = onLoggedOut,
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onOpenTerms = onOpenTerms,
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_LAYOUT_BREAKPOINT) {
            WideLayout(tabNavController, tabContent)
        } else {
            NarrowLayout(tabNavController, tabContent)
        }
    }
}

/** Phone-width layout — bottom NavigationBar. This is the pre-existing mobile layout, untouched. */
@Composable
private fun NarrowLayout(tabNavController: NavHostController, tabContent: @Composable (Modifier) -> Unit) {
    Scaffold(
        bottomBar = {
            val backStackEntry by tabNavController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                MainTab.all.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navigateToTab(tabNavController, tab) },
                        icon = { Text(TAB_ICONS.getValue(tab)) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        tabContent(Modifier.padding(paddingValues))
    }
}

/** Tablet/desktop-width layout — side NavigationRail instead of a bottom bar stretched full-width. */
@Composable
private fun WideLayout(tabNavController: NavHostController, tabContent: @Composable (Modifier) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        val backStackEntry by tabNavController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        NavigationRail {
            MainTab.all.forEach { tab ->
                NavigationRailItem(
                    selected = currentRoute == tab.route,
                    onClick = { navigateToTab(tabNavController, tab) },
                    icon = { Text(TAB_ICONS.getValue(tab)) },
                    label = { Text(tab.label) },
                )
            }
        }
        tabContent(Modifier.weight(1f))
    }
}

private fun navigateToTab(tabNavController: NavHostController, tab: MainTab) {
    tabNavController.navigate(tab.route) {
        popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
