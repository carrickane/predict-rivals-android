package com.balltown.predictrivals.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.platform.isPullToRefreshSupported

// Same breakpoint MainScaffold uses to switch between bottom-bar (phone-style) and side-rail
// (tablet/desktop-style) layouts — reused here since it's the same "touch-first vs mouse-first"
// signal pull-to-refresh needs, on the platforms where the gesture applies at all.
private val DESKTOP_BREAKPOINT = 600.dp

/**
 * Wraps [content] with Material3 pull-to-refresh, but only where it makes sense: never on web
 * (no touch-first gesture convention there — see [isPullToRefreshSupported]), and only in narrow
 * (phone-style) viewports elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableScreen(isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier) {
        if (isPullToRefreshSupported() && maxWidth < DESKTOP_BREAKPOINT) {
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                content()
            }
        } else {
            content()
        }
    }
}
