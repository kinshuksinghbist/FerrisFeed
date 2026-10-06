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
