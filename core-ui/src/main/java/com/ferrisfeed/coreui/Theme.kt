package com.ferrisfeed.coreui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** FerrisFeed design tokens. Theme name: "Midnight Terminal + Warm Paper". */
object FerrisColors {
    // Brand
    val MidnightTerminal = Color(0xFF0B0E14)
    val MidnightSurface = Color(0xFF10141C)
    val MidnightSurfaceVariant = Color(0xFF171C28)
    val MidnightOutline = Color(0xFF2A3348)

    val FerrisOrange = Color(0xFFFF6B35)
    val FerrisOrangeContainer = Color(0xFF3A1F14)
    val FerrisAmber = Color(0xFFFFB224)

    val MintCorrect = Color(0xFF00D9A6)
    val MintContainerDark = Color(0xFF0B332A)

    val LavenderSysDesign = Color(0xFFB8A6FF)
    // Dormant with the WASM track (Spec v2): kept so TrackColors stays stable.
    val WasmBlue = Color(0xFF4CC9F0)
    val RustOrange = Color(0xFFFF6B35)
    // Spec v2 card tint: orange Rust, sky-blue System Design.
    val SkyBlueSysDesign = Color(0xFF4CC9F0)

    val Error = Color(0xFFFF5470)
    val ErrorContainerDark = Color(0xFF3A1420)

    // Glass tokens (P6 30d)
    val GlassDark = Color(0xB3141926)
    val GlassStrokeDark = Color(0x1FFFFFFF)
    val GlassLight = Color(0xCCFFFFFF)
    val GlassStrokeLight = Color(0x14000000)
    val GoldXp = Color(0xFFFFC857)

    // Light ("Warm Paper") surfaces
    val PaperBackground = Color(0xFFFFFBF2)
    val PaperSurface = Color(0xFFFFFFFF)
    val PaperSurfaceVariant = Color(0xFFF3EDE0)
    val InkPrimary = Color(0xFF17130B)
    val InkSecondary = Color(0xFF5C5546)

    // Text on dark
    val TextPrimaryDark = Color(0xFFF2F4F8)
    val TextSecondaryDark = Color(0xFFA7B0C2)

    // Track text: contrast-safe ink for the takeaway closing line and other
    // small track-colored text. The raw track hues (orange #FF6B35,
    // sky-blue #4CC9F0) fail 4.5:1 on one theme each, so each track carries
    // a dark-theme and a light-theme ink. Pairs were chosen for roughly
    // equal luminance distance from their surfaces (see design-token audit
    // in docs/feed-ux.md). Shaped by `design-token` + `dark-mode-design`.
    val RustTextDark = Color(0xFFFFB59E)
    val RustTextLight = Color(0xFF9C3D12)
    val SysDesignTextDark = Color(0xFF8FDCF7)
    val SysDesignTextLight = Color(0xFF0A5A78)
}

@Composable
fun glassColor(): Color = if (isSystemInDarkTheme()) FerrisColors.GlassDark else FerrisColors.GlassLight

@Composable
fun glassStroke(): Color = if (isSystemInDarkTheme()) FerrisColors.GlassStrokeDark else FerrisColors.GlassStrokeLight

private val DarkColorScheme = darkColorScheme(
    primary = FerrisColors.FerrisOrange,
    onPrimary = Color(0xFF1A0D06),
    primaryContainer = FerrisColors.FerrisOrangeContainer,
    onPrimaryContainer = Color(0xFFFFD9C7),
    secondary = FerrisColors.LavenderSysDesign,
    onSecondary = Color(0xFF1A1533),
    secondaryContainer = Color(0xFF2A2547),
    onSecondaryContainer = Color(0xFFE2DAFF),
    tertiary = FerrisColors.MintCorrect,
    onTertiary = Color(0xFF00281E),
    tertiaryContainer = FerrisColors.MintContainerDark,
    onTertiaryContainer = Color(0xFFB9F5E3),
    error = FerrisColors.Error,
    onError = Color(0xFF380007),
    errorContainer = FerrisColors.ErrorContainerDark,
    onErrorContainer = Color(0xFFFFD9DF),
    background = FerrisColors.MidnightTerminal,
    onBackground = FerrisColors.TextPrimaryDark,
    surface = FerrisColors.MidnightSurface,
    onSurface = FerrisColors.TextPrimaryDark,
    surfaceVariant = FerrisColors.MidnightSurfaceVariant,
    onSurfaceVariant = FerrisColors.TextSecondaryDark,
    outline = FerrisColors.MidnightOutline,
    outlineVariant = Color(0xFF1E2534),
    surfaceTint = FerrisColors.FerrisOrange,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC24A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A1A0C),
    secondary = Color(0xFF5A4DB8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4DDFF),
    onSecondaryContainer = Color(0xFF1E1A3D),
    tertiary = Color(0xFF00775C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9BF2D6),
    onTertiaryContainer = Color(0xFF00281E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = FerrisColors.PaperBackground,
    onBackground = FerrisColors.InkPrimary,
    surface = FerrisColors.PaperSurface,
    onSurface = FerrisColors.InkPrimary,
    surfaceVariant = FerrisColors.PaperSurfaceVariant,
    onSurfaceVariant = FerrisColors.InkSecondary,
    outline = Color(0xFFD8D0BE),
    surfaceTint = Color(0xFFC24A1A),
)

/** Display font stack: Space Grotesk bundled resource. */
val DisplayFont: FontFamily = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Monospace stack for code: JetBrains Mono bundled resource. */
val CodeFontFamily: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/** Tabular-digit number style for XP, streaks, and percentages. */
val NumberStyle = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

private val FerrisTypography = androidx.compose.material3.Typography(
    displayMedium = TextStyle(fontFamily = DisplayFont, fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-1.0).sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = DisplayFont, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = DisplayFont, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = DisplayFont, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = DisplayFont, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = DisplayFont, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)

private val FerrisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

/** Extra track colors exposed via CompositionLocal so cards stay consistent. */
@Immutable
data class TrackColors(
    val rust: Color,
    val wasm: Color,
    val systemDesign: Color,
)

val LocalTrackColors = staticCompositionLocalOf {
    TrackColors(
        rust = FerrisColors.RustOrange,
        wasm = FerrisColors.WasmBlue,
        systemDesign = FerrisColors.SkyBlueSysDesign,
    )
}

/**
 * Track wash tokens (TODO 23, shaped by `design-token`).
 *
 * The card wash is always a 14% blend of the track hue into the current
 * surface, so identity reads without spending header chrome and text stays
 * on a near-surface color. Do NOT inline a different alpha or a raw track
 * color as a container elsewhere — call [trackWash] so the ratio cannot
 * drift. `dark-mode-design`: the same ratio works on Midnight OLED
 * (#0B0E14) and Warm Paper because both surfaces are near-neutral; the
 * blend moves with the surface instead of fighting it.
 */
object TrackWashTokens {
    const val WASH_RATIO = 0.14f
}

@Composable
fun trackWash(track: String): Color {
    val surface = MaterialTheme.colorScheme.surface
    return lerp(surface, trackColor(track), TrackWashTokens.WASH_RATIO)
}

/** Track gradient tokens (P6 30f). */
fun trackBrush(track: String, dark: Boolean): Brush {
    return when (track) {
        Tracks.RUST -> if (dark) {
            Brush.verticalGradient(listOf(Color(0xFF2A1710), Color(0xFF12161F)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFE3D4), Color(0xFFFFF6EF)))
        }
        Tracks.SYSTEM_DESIGN -> if (dark) {
            Brush.verticalGradient(listOf(Color(0xFF10222C), Color(0xFF12161F)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFD9F1FA), Color(0xFFF2FAFD)))
        }
        else -> if (dark) {
            Brush.verticalGradient(listOf(Color(0xFF1F1A2C), Color(0xFF12161F)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFEBE6F8), Color(0xFFFAF8FD)))
        }
    }
}

@Composable
fun trackBrush(track: String): Brush = trackBrush(track, isSystemInDarkTheme())

/**
 * Contrast-safe track ink for small text (takeaway line, captions).
 * Raw track hues are identity signals, not text colors: orange fails on
 * light paper and sky-blue fails on light paper, so this returns the
 * per-theme ink that holds >= 4.5:1 against the current surface
 * (TODO 23a `accessibility-audit` + `critique-color`). Body text itself
 * stays on onSurface/onSurfaceVariant and never uses this.
 */
@Composable
fun trackTextColor(track: String): Color {
    val dark = isSystemInDarkTheme()
    return when (track) {
        Tracks.RUST -> if (dark) FerrisColors.RustTextDark else FerrisColors.RustTextLight
        Tracks.SYSTEM_DESIGN -> if (dark) FerrisColors.SysDesignTextDark else FerrisColors.SysDesignTextLight
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * App theme. Supports dark (default, Midnight Terminal), light (Warm Paper),
 * and Material You dynamic color when [dynamicColor] is true (Android 12+).
 */
@Composable
fun FerrisFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(LocalTrackColors provides LocalTrackColors.current) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FerrisTypography,
            shapes = FerrisShapes,
            content = content,
        )
    }
}

