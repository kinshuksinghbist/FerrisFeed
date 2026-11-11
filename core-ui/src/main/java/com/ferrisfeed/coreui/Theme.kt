package com.ferrisfeed.coreui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * FerrisFeed design system, v3 ("Workbench").
 *
 * One calm, typographic language for the whole app: flat surfaces with
 * hairline borders, no glass, no fake 3D, no competing "philosophies".
 * Colour is reserved for meaning:
 *   - track identity (Rust orange, System Design sky),
 *   - answer feedback (success green, danger red),
 *   - XP (gold).
 * Everything else is ink on paper (light) or ink on midnight (dark).
 */

/** Raw brand values. Screens should prefer [FerrisTheme.palette] (theme-aware). */
object FerrisColors {
    val Midnight = Color(0xFF0B0E14)
    val Paper = Color(0xFFFAF8F4)
    val FerrisOrange = Color(0xFFFF6B35)
    val Sky = Color(0xFF4CC9F0)
    val Gold = Color(0xFFFFC857)
}

/** Theme-aware semantic colours that Material's scheme does not cover. */
@Immutable
data class FerrisPalette(
    val rust: Color,
    val rustInk: Color,
    val rustSoft: Color,
    val sysDesign: Color,
    val sysDesignInk: Color,
    val sysDesignSoft: Color,
    val success: Color,
    val successInk: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerInk: Color,
    val dangerSoft: Color,
    val xp: Color,
    val hairline: Color,
    val raised: Color,
    val muted: Color,
    val isDark: Boolean,
)

private val DarkPalette = FerrisPalette(
    rust = Color(0xFFFF6B35),
    rustInk = Color(0xFFFFAA88),
    rustSoft = Color(0xFF2A1A13),
    sysDesign = Color(0xFF4CC9F0),
    sysDesignInk = Color(0xFF8FDCF7),
    sysDesignSoft = Color(0xFF0F2530),
    success = Color(0xFF34D399),
    successInk = Color(0xFF6EE7B7),
    successSoft = Color(0xFF0E2A22),
    danger = Color(0xFFFF5C72),
    dangerInk = Color(0xFFFF9AA8),
    dangerSoft = Color(0xFF2E1218),
    xp = Color(0xFFFFC857),
    hairline = Color(0xFF232A36),
    raised = Color(0xFF181D27),
    muted = Color(0xFF6B7385),
    isDark = true,
)

private val LightPalette = FerrisPalette(
    rust = Color(0xFFE8541C),
    rustInk = Color(0xFFAE3A0E),
    rustSoft = Color(0xFFFDEBE1),
    sysDesign = Color(0xFF0EA5E9),
    sysDesignInk = Color(0xFF036093),
    sysDesignSoft = Color(0xFFE2F3FC),
    success = Color(0xFF059669),
    successInk = Color(0xFF046C4E),
    successSoft = Color(0xFFDDF6EA),
    danger = Color(0xFFE11D48),
    dangerInk = Color(0xFFB0123A),
    dangerSoft = Color(0xFFFFE6EA),
    xp = Color(0xFFB7791F),
    hairline = Color(0xFFE6E0D5),
    raised = Color(0xFFF3F0EA),
    muted = Color(0xFF8A8375),
    isDark = false,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6B35),
    onPrimary = Color(0xFF1C0C04),
    primaryContainer = Color(0xFF3A1F14),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFF4CC9F0),
    onSecondary = Color(0xFF00202B),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF002417),
    error = Color(0xFFFF5C72),
    onError = Color(0xFF3A0009),
    errorContainer = Color(0xFF2E1218),
    onErrorContainer = Color(0xFFFFD9DF),
    background = FerrisColors.Midnight,
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF12161E),
    onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF181D27),
    onSurfaceVariant = Color(0xFF9AA3B2),
    surfaceContainer = Color(0xFF12161E),
    surfaceContainerHigh = Color(0xFF181D27),
    outline = Color(0xFF353D4C),
    outlineVariant = Color(0xFF232A36),
    surfaceTint = Color.Transparent,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDEBE1),
    onPrimaryContainer = Color(0xFF3A1A0C),
    secondary = Color(0xFF036093),
    onSecondary = Color.White,
    tertiary = Color(0xFF046C4E),
    onTertiary = Color.White,
    error = Color(0xFFB0123A),
    onError = Color.White,
    errorContainer = Color(0xFFFFE6EA),
    onErrorContainer = Color(0xFF410010),
    background = FerrisColors.Paper,
    onBackground = Color(0xFF17140F),
    surface = Color.White,
    onSurface = Color(0xFF17140F),
    surfaceVariant = Color(0xFFF3F0EA),
    onSurfaceVariant = Color(0xFF5E574A),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF3F0EA),
    outline = Color(0xFFCFC8BA),
    outlineVariant = Color(0xFFE6E0D5),
    surfaceTint = Color.Transparent,
)

/** Display face: Space Grotesk (bundled, OFL). Headlines, labels, numbers. */
val DisplayFont: FontFamily = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Code face: JetBrains Mono (bundled, OFL). */
val CodeFontFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Tabular digits for XP, streaks and percentages so numbers never jitter. */
val NumberStyle = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

private val FerrisTypography = Typography(
    displayMedium = TextStyle(fontFamily = DisplayFont, fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-1.2).sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, letterSpacing = (-0.9).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = (-0.7).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = DisplayFont, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 23.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = DisplayFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 26.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, letterSpacing = 0.15.sp),
    labelLarge = TextStyle(fontFamily = DisplayFont, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = DisplayFont, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = DisplayFont, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.8.sp),
)

private val FerrisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Extra type roles the Material scale does not name. */
object FerrisType {
    /** The lesson hook: the biggest words on any screen. */
    val Hook = TextStyle(fontFamily = DisplayFont, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = (-0.6).sp)

    /** Uppercase overline used for section labels ("KEY IDEA", "UNIT 2"). */
    val Overline = TextStyle(fontFamily = DisplayFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp, letterSpacing = 1.4.sp)
}

val LocalFerrisPalette = staticCompositionLocalOf { DarkPalette }

/** Height the app shell reserves at the bottom (navigation bar + system inset). */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/** Accessor: `FerrisTheme.palette.success`. */
object FerrisTheme {
    val palette: FerrisPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalFerrisPalette.current
}

/** Identity colour of a track (dot, tag, progress fill). */
@Composable
@ReadOnlyComposable
fun trackColor(track: String): Color {
    val p = LocalFerrisPalette.current
    return when (track) {
        Tracks.RUST -> p.rust
        Tracks.SYSTEM_DESIGN -> p.sysDesign
        else -> MaterialTheme.colorScheme.primary
    }
}

/** Contrast-safe track colour for small text (>= 4.5:1 on background). */
@Composable
@ReadOnlyComposable
fun trackTextColor(track: String): Color {
    val p = LocalFerrisPalette.current
    return when (track) {
        Tracks.RUST -> p.rustInk
        Tracks.SYSTEM_DESIGN -> p.sysDesignInk
        else -> MaterialTheme.colorScheme.primary
    }
}

/** Very soft track tint for tag backgrounds and selected states. */
@Composable
@ReadOnlyComposable
fun trackSoftColor(track: String): Color {
    val p = LocalFerrisPalette.current
    return when (track) {
        Tracks.RUST -> p.rustSoft
        Tracks.SYSTEM_DESIGN -> p.sysDesignSoft
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

/**
 * App theme. Dark (Midnight) and light (Paper) brand schemes; dynamic colour
 * stays opt-in because wallpaper colours would erase the track identities.
 */
@Composable
fun FerrisFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(
        LocalFerrisPalette provides if (darkTheme) DarkPalette else LightPalette,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FerrisTypography,
            shapes = FerrisShapes,
            content = content,
        )
    }
}
