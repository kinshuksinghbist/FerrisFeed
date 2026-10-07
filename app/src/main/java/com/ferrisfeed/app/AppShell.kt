package com.ferrisfeed.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.AnimatedCounter
import com.ferrisfeed.coreui.DisplayFont
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisIconButton
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.NumberStyle
import com.ferrisfeed.coreui.StreakFlame
import com.ferrisfeed.coreui.glass
import com.ferrisfeed.coreui.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class NavItemData(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Floating glass navigation bar pill (P6 32a).
 * Centers horizontally and floats above content with a sliding orange indicator.
 */
@Composable
fun FerrisNavBar(
    selectedRoute: Route,
    onSelectRoute: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember {
        listOf(
            NavItemData(
                route = Route.Feed,
                label = "Feed",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
            ),
            NavItemData(
                route = Route.Path,
                label = "Path",
                selectedIcon = Icons.Filled.Timeline,
                unselectedIcon = Icons.Outlined.Timeline,
            ),
        )
    }

    val selectedIndex = if (selectedRoute is Route.Path) 1 else 0

    var item0Offset by remember { mutableStateOf(0.dp) }
    var item0Width by remember { mutableStateOf(0.dp) }
    var item1Offset by remember { mutableStateOf(0.dp) }
    var item1Width by remember { mutableStateOf(0.dp) }

    val density = LocalDensity.current

    val indicatorOffset by animateDpAsState(
        targetValue = if (selectedIndex == 0) item0Offset else item1Offset,
        animationSpec = FerrisMotion.Snappy,
        label = "nav-indicator-offset",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selectedIndex == 0) item0Width else item1Width,
        animationSpec = FerrisMotion.Snappy,
        label = "nav-indicator-width",
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .glass(CircleShape)
            .clip(CircleShape)
            .defaultMinSize(minWidth = 220.dp)
            .height(64.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Shared sliding orange indicator
        if (indicatorWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .width(indicatorWidth)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val haptic = LocalHapticFeedback.current

                val iconScale = remember { Animatable(1f) }
                LaunchedEffect(isSelected) {
                    if (isSelected) {
                        iconScale.animateTo(1.15f, FerrisMotion.Bouncy)
                        iconScale.animateTo(1f, FerrisMotion.Snappy)
                    } else {
                        iconScale.snapTo(1f)
                    }
                }

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInParent()
                            val xDp = with(density) { pos.x.toDp() }
                            val wDp = with(density) { coordinates.size.width.toDp() }
                            if (index == 0) {
                                item0Offset = xDp
                                item0Width = wDp
                            } else {
                                item1Offset = xDp
                                item1Width = wDp
                            }
                        }
                        .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .pressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectRoute(item.route)
                                }
                            },
                        )
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = item.label
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(iconScale.value),
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandHorizontally(animationSpec = FerrisMotion.Quick) + fadeIn(animationSpec = FerrisMotion.Quick),
                            exit = shrinkHorizontally(animationSpec = FerrisMotion.Quick) + fadeOut(animationSpec = FerrisMotion.Quick),
                        ) {
                            Row {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = DisplayFont,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top persistent stat bar (P6 32d).
 * Left = Streak flame, center = optional title, right = XP chip + rising XP toast.
 */
@Composable
fun StatBar(
    stats: UserStats,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    var previousXp by remember { mutableStateOf(stats.xp) }
    var xpDeltaText by remember { mutableStateOf<String?>(null) }
    val xpFloatProgress = remember { Animatable(0f) }
    val xpChipScale = remember { Animatable(1f) }

    var previousStreak by remember { mutableStateOf(stats.streakDays) }
    var celebrateStreak by remember { mutableStateOf(false) }

    LaunchedEffect(stats.streakDays) {
        if (stats.streakDays > previousStreak && previousStreak > 0) {
            celebrateStreak = true
            delay(1500)
            celebrateStreak = false
        }
        previousStreak = stats.streakDays
    }

    LaunchedEffect(stats.xp) {
        if (stats.xp > previousXp && previousXp > 0) {
            val delta = stats.xp - previousXp
            xpDeltaText = "+$delta XP"
            launch {
                xpChipScale.snapTo(1f)
                xpChipScale.animateTo(1.18f, FerrisMotion.Bouncy)
                xpChipScale.animateTo(1f, FerrisMotion.Snappy)
            }
            launch {
                xpFloatProgress.snapTo(0f)
                xpFloatProgress.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
                xpDeltaText = null
            }
        }
        previousXp = stats.xp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: StreakFlame
        StreakFlame(
            streakDays = stats.streakDays,
            celebrate = celebrateStreak,
        )

        // Center: optional title (e.g. "Path")
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Spacer(Modifier.weight(1f, fill = false))
        }

        // Right: XP chip with rising toast
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .scale(xpChipScale.value)
                    .glass(CircleShape)
                    .clip(CircleShape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = FerrisColors.GoldXp,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    AnimatedCounter(
                        value = stats.xp,
                        style = MaterialTheme.typography.titleMedium.merge(NumberStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (xpDeltaText != null) {
                Box(
                    modifier = Modifier
                        .offset(y = (-24).dp * xpFloatProgress.value - 18.dp)
                        .alpha(1f - xpFloatProgress.value)
                        .glass(CircleShape)
                        .clip(CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = xpDeltaText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.merge(NumberStyle),
                        color = FerrisColors.GoldXp,
                    )
                }
            }
        }
    }
}

/**
 * Top bar for TopicFeed and ReelDetail (P6 33d): back button + screen title chip.
 */
@Composable
fun BackChipBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FerrisIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            size = 44.dp,
            active = false,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(36.dp)
                .glass(CircleShape)
                .clip(CircleShape)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "Nav bar · light", showBackground = true, backgroundColor = 0xFFFFFBF2)
@Composable
private fun FerrisNavBarPreviewLight() {
    FerrisFeedTheme(darkTheme = false, dynamicColor = false) {
        FerrisNavBar(
            selectedRoute = Route.Feed,
            onSelectRoute = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Nav bar · dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun FerrisNavBarPreviewDark() {
    FerrisFeedTheme(darkTheme = true, dynamicColor = false) {
        FerrisNavBar(
            selectedRoute = Route.Path,
            onSelectRoute = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "StatBar · dark", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun StatBarPreviewDark() {
    FerrisFeedTheme(darkTheme = true, dynamicColor = false) {
        StatBar(
            stats = UserStats(xp = 420, streakDays = 7),
            title = "Path",
        )
    }
}
