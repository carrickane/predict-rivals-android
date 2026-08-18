package com.balltown.predictrivals.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.balltown.predictrivals.domain.model.Tournament
import com.balltown.predictrivals.platform.isWebPlatform
import com.balltown.predictrivals.ui.components.WebHeader
import com.balltown.predictrivals.ui.profile.ProfileScreen
import com.balltown.predictrivals.ui.tournament.RequiresCurrentTournament
import com.balltown.predictrivals.ui.tournament.TournamentTabScreen
import com.balltown.predictrivals.ui.tournament.calendar.CalendarScreen
import com.balltown.predictrivals.ui.tournament.live.LiveScreen
import com.balltown.predictrivals.ui.tournament.standings.StandingsScreen
import org.jetbrains.compose.resources.stringResource

// Material3's compact/medium window-size-class boundary. Below it: phone-style bottom
// NavigationBar (unchanged). At or above it (tablets landscape, desktop browser windows): a side
// NavigationRail, which is the idiomatic wide-viewport pattern instead of a stretched bottom bar.
private val WIDE_LAYOUT_BREAKPOINT = 600.dp

private val ICON_SIZE = 24.dp
private val ICON_STROKE_WIDTH = 1.6.dp

/**
 * Hand-drawn outline tab icons, rather than a Material Icons Extended dependency — that artifact
 * doesn't follow the main Compose Multiplatform version train (see the design notes around this
 * change) and is explicitly discouraged for multiplatform binary size. Each reads
 * [LocalContentColor] so they automatically pick up whatever selected/unselected tint the
 * surrounding NavigationBarItem/NavigationRailItem applies. These are simplified approximations,
 * not polished vector assets — swap in real icon assets later if the look needs polishing.
 */
@Composable
private fun CalendarTabIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = ICON_STROKE_WIDTH.toPx()
        val bodyTop = size.height * 0.22f
        val corner = size.width * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.08f, bodyTop),
            size = Size(size.width * 0.84f, size.height * 0.7f),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.08f, bodyTop + size.height * 0.16f),
            end = Offset(size.width * 0.92f, bodyTop + size.height * 0.16f),
            strokeWidth = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, bodyTop - size.height * 0.08f),
            end = Offset(size.width * 0.3f, bodyTop + size.height * 0.06f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.7f, bodyTop - size.height * 0.08f),
            end = Offset(size.width * 0.7f, bodyTop + size.height * 0.06f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TrophyTabIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = ICON_STROKE_WIDTH.toPx()
        val w = size.width
        val h = size.height
        val bowlLeft = w * 0.22f
        val bowlRight = w * 0.78f
        val bowlTop = h * 0.12f
        val bowlBottomY = h * 0.5f

        val bowlPath = Path().apply {
            moveTo(bowlLeft, bowlTop)
            lineTo(bowlLeft, bowlTop + h * 0.08f)
            quadraticBezierTo(bowlLeft, bowlBottomY, w * 0.5f, bowlBottomY)
            quadraticBezierTo(bowlRight, bowlBottomY, bowlRight, bowlTop + h * 0.08f)
            lineTo(bowlRight, bowlTop)
        }
        drawPath(bowlPath, color = color, style = Stroke(width = stroke))
        drawLine(color = color, start = Offset(bowlLeft, bowlTop), end = Offset(bowlRight, bowlTop), strokeWidth = stroke)
        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(bowlLeft - w * 0.18f, bowlTop),
            size = Size(w * 0.18f, h * 0.22f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(bowlRight, bowlTop),
            size = Size(w * 0.18f, h * 0.22f),
            style = Stroke(width = stroke),
        )
        drawLine(color = color, start = Offset(w * 0.5f, bowlBottomY), end = Offset(w * 0.5f, h * 0.82f), strokeWidth = stroke)
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.86f),
            end = Offset(w * 0.66f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BarChartTabIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = ICON_STROKE_WIDTH.toPx()
        val baseline = size.height * 0.85f
        val barWidth = size.width * 0.18f
        val gap = size.width * 0.08f
        val heights = listOf(size.height * 0.35f, size.height * 0.55f, size.height * 0.75f)
        var x = size.width * 0.12f
        heights.forEach { barHeight ->
            drawRect(
                color = color,
                topLeft = Offset(x, baseline - barHeight),
                size = Size(barWidth, barHeight),
                style = Stroke(width = stroke),
            )
            x += barWidth + gap
        }
    }
}

@Composable
private fun PersonTabIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = ICON_STROKE_WIDTH.toPx()
        val headRadius = size.minDimension * 0.18f
        val headCenter = Offset(size.width / 2f, size.height * 0.32f)
        drawCircle(color = color, radius = headRadius, center = headCenter, style = Stroke(width = stroke))

        val bodyPath = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.85f)
            quadraticBezierTo(size.width * 0.2f, size.height * 0.55f, size.width * 0.5f, size.height * 0.55f)
            quadraticBezierTo(size.width * 0.8f, size.height * 0.55f, size.width * 0.8f, size.height * 0.85f)
        }
        drawPath(bodyPath, color = color, style = Stroke(width = stroke))
    }
}

/** Broadcast/signal icon — a center dot with two concentric arcs radiating outward on each side. */
@Composable
private fun LiveTabIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(ICON_SIZE)) {
        val stroke = ICON_STROKE_WIDTH.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dotRadius = size.minDimension * 0.12f

        drawCircle(color = color, radius = dotRadius, center = Offset(cx, cy))

        listOf(size.minDimension * 0.32f, size.minDimension * 0.48f).forEach { r ->
            // Left arc "(" and right arc ")", both centered on the dot.
            drawArc(
                color = color,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 270f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun TabIcon(tab: MainTab) {
    when (tab) {
        MainTab.Calendar -> CalendarTabIcon()
        MainTab.Live -> LiveTabIcon()
        MainTab.Tournament -> TrophyTabIcon()
        MainTab.Standings -> BarChartTabIcon()
        MainTab.Profile -> PersonTabIcon()
    }
}

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

    Column(modifier = Modifier.fillMaxSize()) {
        if (isWebPlatform()) {
            WebHeader(onLoggedOut = onLoggedOut)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (maxWidth >= WIDE_LAYOUT_BREAKPOINT) {
                WideLayout(tabNavController, tabContent)
            } else {
                NarrowLayout(tabNavController, tabContent)
            }
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
                        icon = { TabIcon(tab) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
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
                    icon = { TabIcon(tab) },
                    label = { Text(stringResource(tab.labelRes)) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
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
