package com.balltown.predictrivals.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberShareAction(): (String) -> Unit {
    return remember {
        { text ->
            val activityController = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(activityController, animated = true, completion = null)
        }
    }
}
