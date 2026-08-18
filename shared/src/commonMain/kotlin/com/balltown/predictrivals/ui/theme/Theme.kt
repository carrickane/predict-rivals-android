package com.balltown.predictrivals.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

val Purple = Color(0xFF6A1B9A)

// ColorScheme colors must be opaque (they're used for contrast/elevation math elsewhere), so a
// literal 10%-alpha Color would misbehave. Instead pre-composite Purple at 10% over white to get
// the equivalent pale wash as a solid color — buttons/accents stay full-strength Purple.
private val PurpleBackground = Purple.copy(alpha = 0.10f).compositeOver(Color.White)
private val OnPurple = Color(0xFF2A0845)

private val PredictRivalsColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Purple,
    onSecondary = Color.White,
    background = PurpleBackground,
    onBackground = OnPurple,
    surface = PurpleBackground,
    onSurface = OnPurple,
    surfaceVariant = PurpleBackground,
    onSurfaceVariant = OnPurple,
)

@Composable
fun PredictRivalsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PredictRivalsColorScheme, content = content)
}
