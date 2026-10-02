package io.github.shahalam22.walksafe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2A78D6),
    onPrimary = Color.White,
    background = Color(0xFFF9F9F7),
    surface = Color(0xFFFCFCFB),
    surfaceVariant = Color(0xFFF0EFEC),
    onSurface = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFF52514E),
    outline = Color(0xFFC3C2B7),
    outlineVariant = Color(0xFFE1E0D9),
    error = Color(0xFFC43333),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF3987E5),
    onPrimary = Color.White,
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A19),
    surfaceVariant = Color(0xFF262624),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC3C2B7),
    outline = Color(0xFF383835),
    outlineVariant = Color(0xFF2C2C2A),
    error = Color(0xFFE66767),
)

/** Status colours for risk levels; always paired with a label and an icon. */
object StatusColors {
    val good = Color(0xFF0CA30C)
    val warning = Color(0xFFFAB219)
    val serious = Color(0xFFEC835A)
    val critical = Color(0xFFD03B3B)
}

/** The guidance screen: black, with yellow for the current instruction. */
object GuideColors {
    val background = Color.Black
    val text = Color.White
    val command = Color(0xFFFFD400)
    val muted = Color(0xFFB5B5B5)
    val dim = Color(0xFF2A2A2A)
}

val SuccessGreen @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0CA30C) else Color(0xFF006300)

@Composable
fun WalkSafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
