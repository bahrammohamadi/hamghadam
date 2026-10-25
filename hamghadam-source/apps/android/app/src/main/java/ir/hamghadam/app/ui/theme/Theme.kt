package ir.hamghadam.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual identity: a warm, multi-coloured paper palette.
 *
 * One accent per job (terracotta = actions, olive = done/habits, violet =
 * challenge/focus, amber = events, sky = Google) keeps the screen calm while
 * still feeling colourful. Arabic/Persian glyphs fall back to the system font.
 */
object HamghadamPalette {
    val Cream = Color(0xFFFBF5EC)
    val Paper = Color(0xFFFFFDF9)
    val PaperSoft = Color(0xFFF6EDE1)
    val Ink = Color(0xFF2A211B)
    val InkMuted = Color(0xFF776A5F)
    val Line = Color(0xFFEDE0D0)

    val Terracotta = Color(0xFFDD6742)
    val TerracottaDeep = Color(0xFF9E3E1F)
    val Peach = Color(0xFFFBE4D7)

    val Olive = Color(0xFF5C7A4E)
    val OliveSoft = Color(0xFFE6EEDD)

    val Violet = Color(0xFF6B57D2)
    val VioletDeep = Color(0xFF443096)
    val VioletSoft = Color(0xFFEDE8FD)

    val Amber = Color(0xFFD99324)
    val AmberSoft = Color(0xFFFCF0D7)

    val Sky = Color(0xFF3F7AA8)
    val SkySoft = Color(0xFFE2EEF7)

    val Rose = Color(0xFFCE6079)
    val RoseSoft = Color(0xFFFBE3E9)

    val Night = Color(0xFF171310)
    val NightSurface = Color(0xFF221C18)
    val NightVariant = Color(0xFF2E2620)
}

object CardRadius {
    val card = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(50)
    val field = RoundedCornerShape(16.dp)
}

private val LightColors = lightColorScheme(
    primary = HamghadamPalette.Terracotta,
    onPrimary = Color.White,
    primaryContainer = HamghadamPalette.Peach,
    onPrimaryContainer = HamghadamPalette.TerracottaDeep,
    secondary = HamghadamPalette.Olive,
    onSecondary = Color.White,
    secondaryContainer = HamghadamPalette.OliveSoft,
    onSecondaryContainer = Color(0xFF243D1D),
    tertiary = HamghadamPalette.Violet,
    onTertiary = Color.White,
    tertiaryContainer = HamghadamPalette.VioletSoft,
    onTertiaryContainer = HamghadamPalette.VioletDeep,
    background = HamghadamPalette.Cream,
    onBackground = HamghadamPalette.Ink,
    surface = HamghadamPalette.Paper,
    onSurface = HamghadamPalette.Ink,
    surfaceVariant = HamghadamPalette.PaperSoft,
    onSurfaceVariant = HamghadamPalette.InkMuted,
    outline = HamghadamPalette.Line,
    outlineVariant = Color(0xFFF2E7D9),
    error = Color(0xFFB4432E),
    onError = Color.White,
    errorContainer = Color(0xFFFBE0D8),
    onErrorContainer = Color(0xFF6E2413),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA184),
    onPrimary = Color(0xFF4B1A08),
    primaryContainer = Color(0xFF6B3018),
    onPrimaryContainer = Color(0xFFFFDACB),
    secondary = Color(0xFFBFD3A9),
    onSecondary = Color(0xFF1E3616),
    secondaryContainer = Color(0xFF354A28),
    onSecondaryContainer = Color(0xFFDCEACB),
    tertiary = Color(0xFFC7B9FF),
    onTertiary = Color(0xFF2A1B66),
    tertiaryContainer = Color(0xFF42318A),
    onTertiaryContainer = Color(0xFFE7E0FF),
    background = HamghadamPalette.Night,
    onBackground = Color(0xFFF0E6DC),
    surface = HamghadamPalette.NightSurface,
    onSurface = Color(0xFFF0E6DC),
    surfaceVariant = HamghadamPalette.NightVariant,
    onSurfaceVariant = Color(0xFFC9BBAE),
    outline = Color(0xFF4A4038),
    outlineVariant = Color(0xFF352D26),
    error = Color(0xFFFFB4A1),
    onError = Color(0xFF5C1607),
    errorContainer = Color(0xFF7B2A16),
    onErrorContainer = Color(0xFFFFDAD1),
)

private val HamghadamShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val HamghadamTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.Default, fontSize = 15.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.Default, fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun HamghadamTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = HamghadamShapes,
        typography = HamghadamTypography,
        content = content,
    )
}
