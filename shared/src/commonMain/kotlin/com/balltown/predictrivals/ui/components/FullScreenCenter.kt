package com.balltown.predictrivals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Centers a loader/placeholder in the available screen space, staying clear of the status bar,
 * notch, and nav bar / home indicator on both Android and iOS. */
@Composable
fun FullScreenCenter(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        content()
    }
}
