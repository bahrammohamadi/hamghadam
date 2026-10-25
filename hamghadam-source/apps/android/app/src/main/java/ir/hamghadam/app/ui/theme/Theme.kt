package ir.hamghadam.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Visual identity — soft "paper on lavender" surfaces with a pastel, multicolour
 * accent set.
 *
 * The reference screens use a cool near-white canvas, pure white cards with very
 * soft shadows and big bold headings. Colour is carried by whole tiles (lavender,
 * peach, sky, mint) instead of borders, so screens stay calm while still being
 * colourful. Purple is the single action colour; coral marks today's progress,
 * amber marks the selected calendar day and green marks completion.
 */
object HamghadamPalette {
    val Cream = Color(0xFFF4F5FB)
    val Paper = Color(0xFFFFFFFF)
    val PaperSoft = Color(0xFFEEF0F8)
    val Ink = Color(0xFF111324)
    val InkMuted = Color(0xFF7B8194)
    val Line = Color(0xFFE9EBF4)
    val Scrim = Color(0xFF12141D)

    val Violet = Color(0xFF6C4EF6)
    val VioletDeep = Color(0xFF4B2FD6)
    val VioletSoft = Color(0xFFEDE9FE)

    val Terracotta = Color(0xFFF4553D)
    val TerracottaDeep = Color(0xFFC33722)
    val Peach = Color(0xFFFDE7E2)

    val Amber = Color(0xFFF5B942)
    val AmberDeep = Color(0xFFB47B0B)
    val AmberSoft = Color(0xFFFEF2D6)

    val Olive = Color(0xFF2E9E5B)
    val OliveDeep = Color(0xFF166534)
    val OliveSoft = Color(0xFFDCF5E7)

    val Sky = Color(0xFF3B82F6)
    val SkyDeep = Color(0xFF1D4ED8)
    val SkySoft = Color(0xFFDDEBFF)

    val Teal = Color(0xFF2FB8A8)
    val TealSoft = Color(0xFFDDF6F2)

    val Rose = Color(0xFFEC4899)
    val RoseSoft = Color(0xFFFCE3EF)

    val Night = Color(0xFF0E1017)
    val NightSurface = Color(0xFF171A24)
    val NightVariant = Color(0xFF232734)
}

/** Soft radii: 24dp cards, 20dp tiles, pill chips and 18dp text fields. */
object CardRadius {
    val card = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(20.dp)
    val small = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(50)
    val field = RoundedCornerShape(18.dp)
}

private val LightColors = lightColorScheme(
    primary = HamghadamPalette.Violet,
    onPrimary = Color.White,
    primaryContainer = HamghadamPalette.VioletSoft,
    onPrimaryContainer = HamghadamPalette.VioletDeep,
    secondary = HamghadamPalette.Olive,
    onSecondary = Color.White,
    secondaryContainer = HamghadamPalette.OliveSoft,
    onSecondaryContainer = HamghadamPalette.OliveDeep,
    tertiary = HamghadamPalette.Terracotta,
    onTertiary = Color.White,
    tertiaryContainer = HamghadamPalette.Peach,
    onTertiaryContainer = HamghadamPalette.TerracottaDeep,
    background = HamghadamPalette.Cream,
    onBackground = HamghadamPalette.Ink,
    surface = HamghadamPalette.Paper,
    onSurface = HamghadamPalette.Ink,
    surfaceVariant = HamghadamPalette.PaperSoft,
    onSurfaceVariant = HamghadamPalette.InkMuted,
    outline = HamghadamPalette.Line,
    outlineVariant = Color(0xFFF2F4FB),
    error = Color(0xFFE5484D),
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceTint = HamghadamPalette.Violet,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA897FF),
    onPrimary = Color(0xFF211466),
    primaryContainer = Color(0xFF352A73),
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Color(0xFF6FE0A0),
    onSecondary = Color(0xFF06301B),
    secondaryContainer = Color(0xFF1B4630),
    onSecondaryContainer = Color(0xFFD3F5E1),
    tertiary = Color(0xFFFF8F7B),
    onTertiary = Color(0xFF4B130A),
    tertiaryContainer = Color(0xFF6B2418),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = HamghadamPalette.Night,
    onBackground = Color(0xFFF3F4FA),
    surface = HamghadamPalette.NightSurface,
    onSurface = Color(0xFFF3F4FA),
    surfaceVariant = HamghadamPalette.NightVariant,
    onSurfaceVariant = Color(0xFFA2A8BC),
    outline = Color(0xFF2A2F3D),
    outlineVariant = Color(0xFF1F232E),
    error = Color(0xFFFF9A9E),
    onError = Color(0xFF4E0B0F),
    errorContainer = Color(0xFF6B1A20),
    onErrorContainer = Color(0xFFFFDADB),
    surfaceTint = Color(0xFFA897FF),
)

private val HamghadamShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun bold(size: Int) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
)

private val HamghadamTypography = Typography(
    displaySmall = bold(34),
    headlineLarge = bold(30),
    headlineMedium = bold(26),
    headlineSmall = bold(22),
    titleLarge = bold(20),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 10.sp),
)

@Composable
fun HamghadamTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = HamghadamShapes,
        typography = HamghadamTypography,
        content = content,
    )
}
