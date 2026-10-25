package ir.hamghadam.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Visual identity: calm warm-neutral surfaces with a single teal accent.
 * Colors live here (and nowhere else) so a future brand package can replace them.
 */
object HamghadamPalette {
    val Ink = Color(0xFF172C2A)
    val InkMuted = Color(0xFF5B6B67)
    val Teal = Color(0xFF237A69)
    val TealSoft = Color(0xFFCDE9E0)
    val Sand = Color(0xFFF6F7F4)
    val Clay = Color(0xFFB4763A)
    val Danger = Color(0xFF9B4545)
    val NightBackground = Color(0xFF0E1513)
    val NightSurface = Color(0xFF151D1B)
}

private val LightColors = lightColorScheme(
    primary = HamghadamPalette.Teal,
    onPrimary = Color.White,
    primaryContainer = HamghadamPalette.TealSoft,
    onPrimaryContainer = Color(0xFF0C3A30),
    secondary = HamghadamPalette.Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E3D1),
    onSecondaryContainer = Color(0xFF3E2A15),
    background = HamghadamPalette.Sand,
    onBackground = HamghadamPalette.Ink,
    surface = Color.White,
    onSurface = HamghadamPalette.Ink,
    surfaceVariant = Color(0xFFE8EDEA),
    onSurfaceVariant = HamghadamPalette.InkMuted,
    outline = Color(0xFFC5D1CC),
    error = HamghadamPalette.Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD3B8),
    onPrimary = Color(0xFF04352A),
    primaryContainer = Color(0xFF175447),
    onPrimaryContainer = Color(0xFFBFEFE1),
    secondary = Color(0xFFE0B487),
    onSecondary = Color(0xFF3E2A15),
    secondaryContainer = Color(0xFF4A3620),
    onSecondaryContainer = Color(0xFFF4E3D1),
    background = HamghadamPalette.NightBackground,
    onBackground = Color(0xFFE3EAE7),
    surface = HamghadamPalette.NightSurface,
    onSurface = Color(0xFFE3EAE7),
    surfaceVariant = Color(0xFF25302D),
    onSurfaceVariant = Color(0xFFAFBFBA),
    outline = Color(0xFF44534F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun HamghadamTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
