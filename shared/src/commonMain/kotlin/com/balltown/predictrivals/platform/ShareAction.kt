package com.balltown.predictrivals.platform

import androidx.compose.runtime.Composable

/** Native share sheet on Android/iOS; copies to the clipboard on web (no native share sheet there). */
@Composable
expect fun rememberShareAction(): (String) -> Unit
