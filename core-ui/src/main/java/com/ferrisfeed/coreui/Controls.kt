package com.ferrisfeed.coreui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Visual weight of a [FerrisButton]. One Primary per screen. */
enum class FerrisButtonStyle { Primary, Secondary, Quiet }

private val ButtonShape = RoundedCornerShape(14.dp)

/** The app's only button. Flat, 52dp tall, press-scale feedback, ripple. */
@Composable
fun FerrisButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FerrisButtonStyle = FerrisButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val palette = FerrisTheme.palette
    val container = when (style) {
        FerrisButtonStyle.Primary -> scheme.primary
        FerrisButtonStyle.Secondary -> palette.raised
        FerrisButtonStyle.Quiet -> Color.Transparent
    }
    val content = when (style) {
        FerrisButtonStyle.Primary -> scheme.onPrimary
        FerrisButtonStyle.Secondary -> scheme.onSurface
        FerrisButtonStyle.Quiet -> scheme.primary
    }
    val border = if (style == FerrisButtonStyle.Secondary) palette.hairline else Color.Transparent
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
            .pressScale(interaction, pressed = 0.97f)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(ButtonShape)
            .background(container, ButtonShape)
            .border(1.dp, border, ButtonShape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

/** Filter / choice chip. Selected chips invert to solid ink so state never relies on hue alone. */
@Composable
fun FerrisChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDot: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = FerrisTheme.palette
    val bg = if (selected) scheme.onSurface else Color.Transparent
    val fg = if (selected) scheme.surface else scheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .border(1.dp, if (selected) bg else palette.hairline, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingDot != null) {
            Box(Modifier.size(7.dp).background(leadingDot, CircleShape))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** 48dp circular icon button with ripple. */
@Composable
fun FerrisIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Two-to-four way segmented switch (track picker, filters). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = FerrisTheme.palette
    val scheme = MaterialTheme.colorScheme
    val outer = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(outer)
            .background(palette.raised, outer)
            .border(1.dp, palette.hairline, outer)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(inner)
                    .background(if (isSelected) scheme.surface else Color.Transparent, inner)
                    .border(1.dp, if (isSelected) palette.hairline else Color.Transparent, inner)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Animated horizontal progress meter. */
@Composable
fun LinearMeter(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = FerrisTheme.palette.hairline,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = FerrisMotion.Smooth,
        label = "meter",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated.coerceAtLeast(0.02f))
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Three-bar difficulty glyph plus its word ("Beginner" etc). */
@Composable
fun LevelMeter(
    level: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showLabel: Boolean = true,
) {
    val filled = level.coerceIn(1, 3)
    val off = FerrisTheme.palette.hairline
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Difficulty: ${Tracks.levelLabel(level)}"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            listOf(6.dp, 9.dp, 12.dp).forEachIndexed { i, h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(h)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (i < filled) color else off),
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(Tracks.levelLabel(level), style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

/** "● Rust · Ownership & moves" meta line used on lessons, rows and results. */
@Composable
fun TrackTag(
    track: String,
    topicTitle: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(trackColor(track), CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (topicTitle.isNullOrBlank()) Tracks.label(track) else "${Tracks.label(track)} · $topicTitle",
            style = MaterialTheme.typography.labelMedium,
            color = trackTextColor(track),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Uppercase section label. */
@Composable
fun Overline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text = text.uppercase(), style = FerrisType.Overline, color = color, modifier = modifier)
}

/** Circular progress with a slot for centred content. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = FerrisTheme.palette.hairline,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = FerrisMotion.Smooth,
        label = "ring",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size).clearAndSetSemantics { }) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Placeholder block for loading states: a slow breathing tint, static under reduce-motion. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val reduceMotion = LocalReduceMotion.current
    val alpha by if (reduceMotion) {
        remember { mutableFloatStateOf(0.7f) }
    } else {
        rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0.45f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeleton-alpha",
        )
    }
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(FerrisTheme.palette.raised, shape),
    )
}

/** Icon + number capsule used in top bars (streak, XP). */
@Composable
fun StatPill(
    icon: ImageVector,
    value: Int,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(FerrisTheme.palette.raised, CircleShape)
            .padding(start = 8.dp, end = 12.dp)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        AnimatedCounter(
            value = value,
            style = MaterialTheme.typography.labelLarge.merge(NumberStyle),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun StreakPill(days: Int, modifier: Modifier = Modifier) {
    StatPill(
        icon = Icons.Filled.LocalFireDepartment,
        value = days,
        tint = FerrisTheme.palette.rust,
        contentDescription = "$days day streak",
        dimmed = days == 0,
        modifier = modifier,
    )
}

@Composable
fun XpPill(xp: Int, modifier: Modifier = Modifier) {
    StatPill(
        icon = Icons.Filled.Bolt,
        value = xp,
        tint = FerrisTheme.palette.xp,
        contentDescription = "$xp XP",
        modifier = modifier,
    )
}

/** A bordered flat surface: the one "card" primitive in v3. */
@Composable
fun FerrisSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = FerrisTheme.palette.hairline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color, shape)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        content()
    }
}

/** Row of label + trailing value used in stats blocks. */
@Composable
fun RowScope.StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier.weight(1f), horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.headlineSmall.merge(NumberStyle), color = valueColor, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Preview(name = "Controls · dark", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun ControlsPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FerrisButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
            FerrisButton("Browse lessons", onClick = {}, style = FerrisButtonStyle.Secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FerrisChip("Rust", selected = true, onClick = {})
                FerrisChip("System Design", selected = false, onClick = {}, leadingDot = FerrisColors.Sky)
            }
            SegmentedControl(listOf("Rust", "System Design"), 0, {}, Modifier.fillMaxWidth())
            LinearMeter(0.4f, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StreakPill(4)
                XpPill(320)
                LevelMeter(2)
            }
            TrackTag(Tracks.RUST, "Ownership & moves")
        }
    }
}

@Preview(name = "Controls · light", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360)
@Composable
private fun ControlsPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FerrisButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
            SegmentedControl(listOf("Rust", "System Design"), 1, {}, Modifier.fillMaxWidth())
            TrackTag(Tracks.SYSTEM_DESIGN, "Caching")
        }
    }
}
