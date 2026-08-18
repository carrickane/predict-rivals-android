package com.balltown.predictrivals.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberShareAction(): (String) -> Unit {
    return remember {
        { text -> copyToClipboard(text) }
    }
}

private fun copyToClipboard(text: String) {
    js("navigator.clipboard.writeText(text)")
}
