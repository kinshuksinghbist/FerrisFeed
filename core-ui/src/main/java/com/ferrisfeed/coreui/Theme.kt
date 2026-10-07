package com.ferrisfeed.coreui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape

/** FerrisFeed design tokens. Theme name: "Midnight Terminal + Warm Paper". */
object FerrisColors {
    // Brand
    val MidnightTerminal = Color(0xFF0B0E14)
    val MidnightSurface = Color(0xFF12161F)
    val MidnightSurfaceVariant = Color(0xFF1A2030)
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

/** Monospace stack for code. JetBrains Mono is bundled via font resource where available. */
val CodeFontFamily: FontFamily = FontFamily.Monospace

private val FerrisTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

private val FerrisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
    dynamicColor: Boolean = true,
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
