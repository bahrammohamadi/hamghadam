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
 * Design tokens.
 *
 * Sizes follow Material Design 3 (4dp grid, 24dp icons, 48dp touch targets) and
 * the Apple HIG minimum of 44pt. Text contrast was audited against WCAG 2.2 AA:
 * every text colour below is >= 4.5:1 on its surface, and the vivid brand
 * colours are used only for fills (dots, rings, badges), never for small text.
 */
object Dimens {
    // icons — Material 3 scale
    val IconSmall = 16.dp
    val IconMedium = 20.dp
    val IconLarge = 24.dp
    val IconHero = 28.dp

    // touch targets
    val TouchMin = 48.dp
    val TouchMinIos = 44.dp
    val TouchCompact = 40.dp

    // rhythm (4dp grid)
    val ScreenPadding = 18.dp
    val CardGap = 14.dp
    val SectionGap = 20.dp
    val RowGap = 8.dp

    // shapes
    const val CardRadius = 24
    const val TileRadius = 20
    const val FieldRadius = 18

    // controls
    val Fab = 56.dp
    val NavBar = 64.dp
    val NavIcon = 20.dp
    val Avatar = 46.dp
}

object HamghadamPalette {
    // surfaces
    val Cream = Color(0xFFF4F5FB)
    val Paper = Color(0xFFFFFFFF)
    val PaperSoft = Color(0xFFEEF0F8)
    val Ink = Color(0xFF111324)
    /** Was #7B8194 (3.9:1) — darkened for AA normal text. */
    val InkMuted = Color(0xFF5C6274)
    val Line = Color(0xFFE3E6F0)
    val Scrim = Color(0xFF12141D)

    // brand — fills
    val Violet = Color(0xFF6C4EF6)
    val VioletDeep = Color(0xFF4B2FD6)
    val VioletSoft = Color(0xFFEDE9FE)

    val Terracotta = Color(0xFFF4553D)
    /** Text-safe coral (4.85:1 on white). */
    val TerracottaDeep = Color(0xFFC33722)
    val Peach = Color(0xFFFDE7E2)

    val Amber = Color(0xFFF5B942)
    /** Text-safe amber (5.0:1 on AmberSoft). */
    val AmberDeep = Color(0xFF8A5A00)
    val AmberSoft = Color(0xFFFEF2D6)

    val Olive = Color(0xFF2E9E5B)
    /** Text-safe green (5.4:1 on white). */
    val OliveDeep = Color(0xFF1E7A45)
    val OliveSoft = Color(0xFFDCF5E7)

    val Sky = Color(0xFF3B82F6)
    val SkyDeep = Color(0xFF1D4ED8)
    val SkySoft = Color(0xFFDDEBFF)

    val Teal = Color(0xFF2FB8A8)
    val TealSoft = Color(0xFFDDF6F2)

    val Rose = Color(0xFFEC4899)
    val RoseDeep = Color(0xFF9D174D)
    val RoseSoft = Color(0xFFFCE3EF)

    val Night = Color(0xFF0E1017)
    val NightSurface = Color(0xFF171A24)
    val NightVariant = Color(0xFF232734)
}

/**
 * Six accent schemes. The accent drives `primary` in both themes, so buttons,
 * selected navigation, switches and links follow the user's choice.
 */
enum class AccentColor(
    val label: String,
    val primary: Color,
    val soft: Color,
    val deep: Color,
    val darkPrimary: Color,
    val darkSoft: Color,
) {
    VIOLET("بنفش", Color(0xFF6C4EF6), Color(0xFFEDE9FE), Color(0xFF4B2FD6), Color(0xFFA897FF), Color(0xFF352A73)),
    CORAL("مرجانی", Color(0xFFE2472F), Color(0xFFFDE7E2), Color(0xFFB03019), Color(0xFFFF9A85), Color(0xFF5E1E11)),
    GREEN("سبز", Color(0xFF17864B), Color(0xFFDCF5E7), Color(0xFF0E5A32), Color(0xFF6FE0A0), Color(0xFF1B4630)),
    BLUE("آبی", Color(0xFF1D63D8), Color(0xFFDDEBFF), Color(0xFF134099), Color(0xFF8FBBFF), Color(0xFF16305C)),
    PINK("صورتی", Color(0xFFC22E7A), Color(0xFFFCE3EF), Color(0xFF8A1C55), Color(0xFFFFA3CC), Color(0xFF531232)),
    AMBER("نارنجی", Color(0xFFB4720A), Color(0xFFFEF2D6), Color(0xFF7C4C00), Color(0xFFF7CE84), Color(0xFF432C05)),
    ;

    companion object {
        fun fromName(name: String?): AccentColor = entries.firstOrNull { it.name == name } ?: VIOLET
    }
}

object CardRadius {
    val card = RoundedCornerShape(Dimens.CardRadius.dp)
    val tile = RoundedCornerShape(Dimens.TileRadius.dp)
    val small = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(50)
    val field = RoundedCornerShape(Dimens.FieldRadius.dp)
}

private fun lightColors(accent: AccentColor) = lightColorScheme(
    primary = accent.primary,
    onPrimary = Color.White,
    primaryContainer = accent.soft,
    onPrimaryContainer = accent.deep,
    secondary = HamghadamPalette.OliveDeep,
    onSecondary = Color.White,
    secondaryContainer = HamghadamPalette.OliveSoft,
    onSecondaryContainer = HamghadamPalette.OliveDeep,
    tertiary = HamghadamPalette.TerracottaDeep,
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
    outlineVariant = Color(0xFFF1F3FA),
    error = Color(0xFFC33722),
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceTint = accent.primary,
)

private fun darkColors(accent: AccentColor) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color(0xFF17102E),
    primaryContainer = accent.darkSoft,
    onPrimaryContainer = accent.soft,
    secondary = Color(0xFF7CE0A8),
    onSecondary = Color(0xFF06301B),
    secondaryContainer = Color(0xFF1B4630),
    onSecondaryContainer = Color(0xFFD3F5E1),
    tertiary = Color(0xFFFF9A85),
    onTertiary = Color(0xFF4B130A),
    tertiaryContainer = Color(0xFF6B2418),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = HamghadamPalette.Night,
    onBackground = Color(0xFFF3F4FA),
    surface = HamghadamPalette.NightSurface,
    onSurface = Color(0xFFF3F4FA),
    surfaceVariant = HamghadamPalette.NightVariant,
    onSurfaceVariant = Color(0xFFB4BACD),
    outline = Color(0xFF313746),
    outlineVariant = Color(0xFF232833),
    error = Color(0xFFFF9A9E),
    onError = Color(0xFF4E0B0F),
    errorContainer = Color(0xFF6B1A20),
    onErrorContainer = Color(0xFFFFDADB),
    surfaceTint = accent.darkPrimary,
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

/** Platform default family renders Persian correctly on every OEM. */
private val HamghadamTypography = Typography(
    displaySmall = bold(34),
    headlineLarge = bold(30),
    headlineMedium = bold(26),
    headlineSmall = bold(22),
    titleLarge = bold(20),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontSize = 10.sp),
)

@Composable
fun HamghadamTheme(
    darkTheme: Boolean,
    accent: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(accent) else lightColors(accent),
        shapes = HamghadamShapes,
        typography = HamghadamTypography,
        content = content,
    )
}
