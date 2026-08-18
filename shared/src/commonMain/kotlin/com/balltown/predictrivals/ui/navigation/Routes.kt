package com.balltown.predictrivals.ui.navigation

import com.balltown.predictrivals.res.Res
import com.balltown.predictrivals.res.tab_calendar
import com.balltown.predictrivals.res.tab_live
import com.balltown.predictrivals.res.tab_profile
import com.balltown.predictrivals.res.tab_standings
import com.balltown.predictrivals.res.tab_tournament
import org.jetbrains.compose.resources.StringResource

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Register : Routes("register")

    /** Bottom-nav host: Calendar / Live / Tournament / Standings / Profile. Reachable only once signed in. */
    data object Main : Routes("main")

    data object CreateTournament : Routes("tournament/create")
    data object JoinTournament : Routes("tournament/join")

    data object Predictions : Routes("tournament/{tournamentId}/predictions") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/predictions"
        const val ARG_TOURNAMENT_ID = "tournamentId"
    }
    data object Curate : Routes("tournament/{tournamentId}/curate") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/curate"
    }

    data object PrivacyPolicy : Routes("legal/privacy")
    data object Terms : Routes("legal/terms")
}

/** The 5 bottom-nav destinations, nested inside [Routes.Main]'s own NavHost. */
sealed class MainTab(val route: String, val labelRes: StringResource) {
    data object Calendar : MainTab("tab/calendar", Res.string.tab_calendar)
    data object Live : MainTab("tab/live", Res.string.tab_live)
    data object Tournament : MainTab("tab/tournament", Res.string.tab_tournament)
    data object Standings : MainTab("tab/standings", Res.string.tab_standings)
    data object Profile : MainTab("tab/profile", Res.string.tab_profile)

    companion object {
        // Deferred with `by lazy`: building this list eagerly in the companion's own <clinit>
        // races the JVM's initialization of the sibling `data object`s and can observe some of
        // them as null (a known Kotlin/JVM sealed-class-plus-companion gotcha).
        val all: List<MainTab> by lazy { listOf(Calendar, Live, Tournament, Standings, Profile) }
    }
}
