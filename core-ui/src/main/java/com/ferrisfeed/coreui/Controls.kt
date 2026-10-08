package com.ferrisfeed.coreui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Visual style for [FerrisButton] (P6 35a). */
enum class FerrisButtonStyle {
    Filled,
    Tonal,
    Ghost,
}

/**
 * Custom brand button with haptic-ready pressScale feedback (P6 35a).
 */
@Composable
fun FerrisButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FerrisButtonStyle = FerrisButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val darkPrimary = remember(primary) { lerp(primary, Color.Black, 0.12f) }

    val contentColor = when (style) {
        FerrisButtonStyle.Filled -> MaterialTheme.colorScheme.onPrimary
        FerrisButtonStyle.Tonal -> primary
        FerrisButtonStyle.Ghost -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val philosophy = LocalDesignPhilosophy.current
    val buttonShape = if (philosophy == DesignPhilosophy.NeoBrutalism) RoundedCornerShape(12.dp) else CircleShape

    val backgroundModifier = when (style) {
        FerrisButtonStyle.Filled -> when (philosophy) {
            DesignPhilosophy.StudioGlass -> Modifier
                .background(
                    brush = Brush.verticalGradient(listOf(primary, darkPrimary)),
                    shape = buttonShape,
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), buttonShape)
            DesignPhilosophy.NeoBrutalism -> Modifier
                .tactileDepth(buttonShape, depth = 3.dp, cornerRadius = 12.dp)
                .background(primary, buttonShape)
        }
        FerrisButtonStyle.Tonal -> Modifier
            .background(primary.copy(alpha = 0.16f), buttonShape)
            .then(if (philosophy == DesignPhilosophy.NeoBrutalism) Modifier.border(2.dp, primary, buttonShape) else Modifier)
        FerrisButtonStyle.Ghost -> Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 52.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .then(if (enabled) Modifier.tactileClickable(interactionSource) else Modifier)
            .clip(buttonShape)
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = contentColor,
            )
        }
    }
}

/**
 * Filter and topic chip with glass background and check indicator (P6 35b).
 */
@Composable
fun FerrisChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDot: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary

    val shape = CircleShape
    val containerModifier = if (selected) {
        Modifier
            .background(primary.copy(alpha = 0.20f), shape)
            .border(1.dp, primary, shape)
    } else {
        Modifier.glass(shape)
    }

    val textColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(36.dp)
            .pressScale(interactionSource)
            .clip(shape)
            .then(containerModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingDot != null && !selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(leadingDot, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
            }

            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}

/**
 * Section header with title and optional caption (P6 35c).
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Circular glass icon button with radial spark celebration on activation (P6 35d).
 */
@Composable
fun FerrisIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    inactiveTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val reduceMotion = LocalReduceMotion.current

    val sparkProgress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active && !reduceMotion) {
            sparkProgress.snapTo(0f)
            sparkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350),
            )
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = if (active && !reduceMotion) 1.22f else 1f,
        animationSpec = FerrisMotion.Bouncy,
        label = "icon-scale",
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .pressScale(interactionSource)
            .glass(CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (sparkProgress.value in 0.01f..0.99f && !reduceMotion) {
            Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics {}) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val t = sparkProgress.value
                val currentRadius = 14.dp.toPx() * t
                val alpha = (1f - t).coerceIn(0f, 1f)
                val dotRadius = 2.2.dp.toPx() * (1f - t * 0.5f)

                for (i in 0 until 6) {
                    val angle = (i * (Math.PI * 2 / 6)).toFloat()
                    val x = center.x + cos(angle) * currentRadius
                    val y = center.y + sin(angle) * currentRadius
                    drawCircle(
                        color = activeTint.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(x, y),
                    )
                }
            }
        }

        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) activeTint else inactiveTint,
            modifier = Modifier
                .size(size * 0.55f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Preview(name = "Controls Preview", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun ControlsPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(title = "Roadmap", subtitle = "Master Rust memory layout")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FerrisButton(text = "Start Lesson", onClick = {})
                FerrisButton(text = "Tonal", style = FerrisButtonStyle.Tonal, onClick = {})
                FerrisButton(text = "Ghost", style = FerrisButtonStyle.Ghost, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FerrisChip(label = "Rust", selected = true, onClick = {})
                FerrisChip(label = "System Design", selected = false, onClick = {})
                FerrisIconButton(
                    icon = Icons.Filled.Favorite,
                    contentDescription = "Like",
                    active = true,
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "Controls Preview Light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun ControlsPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FerrisButton(text = "Start Lesson", onClick = {})
            FerrisChip(label = "All tracks", selected = false, onClick = {})
        }
    }
}
