package com.balltown.predictrivals.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.di.CurrentTournamentStore
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.ui.auth.LoginScreen
import com.balltown.predictrivals.ui.auth.RegisterScreen
import com.balltown.predictrivals.ui.home.CreateTournamentScreen
import com.balltown.predictrivals.ui.home.JoinTournamentScreen
import com.balltown.predictrivals.ui.legal.LegalScreen
import com.balltown.predictrivals.ui.legal.PRIVACY_POLICY_TEXT
import com.balltown.predictrivals.ui.legal.PRIVACY_POLICY_TITLE
import com.balltown.predictrivals.ui.legal.TERMS_TEXT
import com.balltown.predictrivals.ui.legal.TERMS_TITLE
import com.balltown.predictrivals.ui.tournament.curate.CurateScreen
import com.balltown.predictrivals.ui.tournament.predictions.PredictionsScreen
import org.koin.compose.koinInject

// NavBackStackEntry.arguments is a multiplatform SavedState (only a Bundle alias on Android),
// so reading it must go through SavedStateReader — a direct .getString() only resolves on Android.
private fun NavBackStackEntry.tournamentIdArg(): Int? =
    arguments?.read { getStringOrNull(Routes.Predictions.ARG_TOURNAMENT_ID) }?.toIntOrNull()

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authRepository = koinInject<AuthRepository>()
    val sessionStore = koinInject<SessionStore>()
    val currentTournamentStore = koinInject<CurrentTournamentStore>()

    // Tokens survive process death (TokenStore); SessionStore does not, so a cold start needs its
    // in-memory currentUserId restored before anything tournament-scoped can load.
    val startDestination = remember {
        val restoredUserId = authRepository.restoreSession()
        if (restoredUserId != null) {
            sessionStore.set(restoredUserId)
            Routes.Main.route
        } else {
            Routes.Login.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoggedIn = { navController.navigate(Routes.Main.route) { popUpTo(Routes.Login.route) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
            )
        }
        composable(Routes.Register.route) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.Main.route) { popUpTo(Routes.Login.route) { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.Main.route) {
            MainScaffold(
                onLoggedOut = { navController.navigate(Routes.Login.route) { popUpTo(0) } },
                onCreateTournament = { navController.navigate(Routes.CreateTournament.route) },
                onJoinTournament = { navController.navigate(Routes.JoinTournament.route) },
                onOpenPredictions = { navController.navigate(Routes.Predictions.of(it.id)) },
                onOpenCurate = { navController.navigate(Routes.Curate.of(it.id)) },
                onOpenPrivacyPolicy = { navController.navigate(Routes.PrivacyPolicy.route) },
                onOpenTerms = { navController.navigate(Routes.Terms.route) },
            )
        }
        composable(Routes.CreateTournament.route) {
            CreateTournamentScreen(onCreated = {
                currentTournamentStore.set(it.id)
                navController.popBackStack()
            })
        }
        composable(Routes.JoinTournament.route) {
            JoinTournamentScreen(onJoined = {
                currentTournamentStore.set(it.id)
                navController.popBackStack()
            })
        }
        composable(Routes.Predictions.route) { backStackEntry ->
            val tournamentId = backStackEntry.tournamentIdArg() ?: return@composable
            PredictionsScreen(tournamentId)
        }
        composable(Routes.Curate.route) { backStackEntry ->
            val tournamentId = backStackEntry.tournamentIdArg() ?: return@composable
            CurateScreen(tournamentId, from = "2026-08-17", to = "2026-09-17", onRoundCreated = { navController.popBackStack() })
        }
        composable(Routes.PrivacyPolicy.route) {
            LegalScreen(PRIVACY_POLICY_TITLE, PRIVACY_POLICY_TEXT, onBack = { navController.popBackStack() })
        }
        composable(Routes.Terms.route) {
            LegalScreen(TERMS_TITLE, TERMS_TEXT, onBack = { navController.popBackStack() })
        }
    }
}
