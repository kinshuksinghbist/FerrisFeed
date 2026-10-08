package com.ferrisfeed.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferrisfeed.coreui.AnimatedCounter
import com.ferrisfeed.coreui.DesignPhilosophy
import com.ferrisfeed.coreui.DisplayFont
import com.ferrisfeed.coreui.FerrisButton
import com.ferrisfeed.coreui.FerrisButtonStyle
import com.ferrisfeed.coreui.FerrisColors
import com.ferrisfeed.coreui.FerrisMark
import com.ferrisfeed.coreui.LocalBottomBarInset
import com.ferrisfeed.coreui.LocalDesignPhilosophy
import com.ferrisfeed.coreui.StreakFlame
import com.ferrisfeed.coreui.glass
import com.ferrisfeed.coreui.glassStroke
import com.ferrisfeed.coreui.pressScale
import com.ferrisfeed.coreui.tactileClickable
import com.ferrisfeed.coreui.tactileDepth
import com.ferrisfeed.coreui.trackColor
import com.ferrisfeed.feed.Reel

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSelectPhilosophy: (DesignPhilosophy) -> Unit,
    onOpenReel: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onRemoveSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentPhilosophy = LocalDesignPhilosophy.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 56.dp,
            end = 16.dp,
            bottom = LocalBottomBarInset.current + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // 1. Header Profile Banner
        item(key = "profile-header") {
            ProfileHeaderCard(
                xp = state.xp,
                streak = state.streakDays,
            )
        }

        // 2. Design Philosophy Switcher (Studio Glass vs Neo-Brutalist HUD)
        item(key = "philosophy-switcher") {
            DesignPhilosophySwitcher(
                current = state.designPhilosophy,
                onSelect = { philosophy ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSelectPhilosophy(philosophy)
                },
            )
        }

        // 3. Real Working Streak Card with 7-Day Week Indicator
        item(key = "streak-card") {
            WorkingStreakCard(
                streakDays = state.streakDays,
                activeDays = state.activeDaysThisWeek,
            )
        }

        // 4. User Stats Grid
        item(key = "stats-grid") {
            StatsGrid(
                xp = state.xp,
                streakDays = state.streakDays,
                masteredCount = state.completedReelsCount,
                savedCount = state.savedReels.size,
            )
        }

        // 5. Topic Mastery Breakdown: Topics I'm Good At
        item(key = "topics-good-at") {
            TopicMasterySection(
                title = "Topics I'm Good At",
                subtitle = "Mastery ≥ 70% · Strong foundation",
                items = state.topicsGoodAt,
                accentColor = FerrisColors.Mint400,
                icon = Icons.Filled.Star,
                emptyMessage = "Complete quizzes with high accuracy to build topic mastery!",
                onTopicClick = onTopicClick,
            )
        }

        // 6. Topic Mastery Breakdown: Topics to Practice
        item(key = "topics-to-practice") {
            TopicMasterySection(
                title = "Topics to Practice",
                subtitle = "Needs review · Tap to sharpen",
                items = state.topicsToPractice,
                accentColor = FerrisColors.Flame400,
                icon = Icons.Filled.Warning,
                emptyMessage = "No weak topics detected. Keep learning!",
                onTopicClick = onTopicClick,
            )
        }

        // 7. Saved Reels (Bookmarks)
        item(key = "saved-reels-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Saved Reels (${state.savedReels.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (state.savedReels.isEmpty()) {
            item(key = "empty-saved-reels") {
                EmptySavedCard()
            }
        } else {
            items(state.savedReels, key = { "saved-${it.id}" }) { reel ->
                SavedReelCard(
                    reel = reel,
                    onOpen = { onOpenReel(reel.id) },
                    onRemove = { onRemoveSaved(reel.id) },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    xp: Int,
    streak: Int,
) {
    val level = (xp / 100) + 1
    val rankTitle = when {
        level >= 10 -> "Rust Core Contributor"
        level >= 5 -> "Systems Architect"
        level >= 3 -> "Memory Wrangler"
        else -> "Ferris Explorer"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.large, 6.dp)
            .clip(MaterialTheme.shapes.large)
            .glass(MaterialTheme.shapes.large)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FerrisMark(size = 56.dp, animated = true)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = rankTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$xp Total Experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DesignPhilosophySwitcher(
    current: DesignPhilosophy,
    onSelect: (DesignPhilosophy) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.medium, 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .glass(MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "DESIGN PHILOSOPHY",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (current == DesignPhilosophy.StudioGlass) "Studio Glass" else "Neo-Brutalism",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DisplayFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhilosophyOptionChip(
                label = "Studio Glass",
                subtitle = "Specular Glow & Depth",
                isSelected = current == DesignPhilosophy.StudioGlass,
                onClick = { onSelect(DesignPhilosophy.StudioGlass) },
                modifier = Modifier.weight(1f),
            )
            PhilosophyOptionChip(
                label = "Neo-Brutalism",
                subtitle = "Solid 3D & Contrast",
                isSelected = current == DesignPhilosophy.NeoBrutalism,
                onClick = { onSelect(DesignPhilosophy.NeoBrutalism) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PhilosophyOptionChip(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val borderStroke = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .tactileClickable(shape = shape, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(if (isSelected) 1.5.dp else 0.5.dp, borderStroke, shape)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkingStreakCard(
    streakDays: Int,
    activeDays: List<Boolean>, // 7 booleans for Mon..Sun
) {
    val daysLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.large, 6.dp)
            .clip(MaterialTheme.shapes.large)
            .glass(MaterialTheme.shapes.large)
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DAILY STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = FerrisColors.Flame400,
                    )
                    Text(
                        text = if (streakDays > 0) "$streakDays Days Strong!" else "Start Your Streak!",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                StreakFlame(
                    streakDays = streakDays,
                    celebrate = streakDays >= 3,
                )
            }

            Text(
                text = "Keep your flame blazing by answering at least 1 quiz every calendar day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 7-day indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                daysLabels.forEachIndexed { index, dayLetter ->
                    val isActive = activeDays.getOrNull(index) ?: false
                    DayIndicatorPill(
                        day = dayLetter,
                        isActive = isActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayIndicatorPill(
    day: String,
    isActive: Boolean,
) {
    val shape = CircleShape
    val bg = if (isActive) FerrisColors.Flame400 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val textCol = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(shape)
                .background(bg)
                .border(
                    width = 1.dp,
                    color = if (isActive) FerrisColors.Flame400 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.Whatshot,
                    contentDescription = "Active day",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                )
            }
        }
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatsGrid(
    xp: Int,
    streakDays: Int,
    masteredCount: Int,
    savedCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatMetricCard(
                label = "Total XP",
                value = "$xp",
                icon = Icons.Filled.Bolt,
                tint = FerrisColors.GoldXp,
                modifier = Modifier.weight(1f),
            )
            StatMetricCard(
                label = "Active Streak",
                value = "$streakDays Days",
                icon = Icons.Filled.LocalFireDepartment,
                tint = FerrisColors.Flame400,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatMetricCard(
                label = "Mastered",
                value = "$masteredCount Reels",
                icon = Icons.Filled.Check,
                tint = FerrisColors.Mint400,
                modifier = Modifier.weight(1f),
            )
            StatMetricCard(
                label = "Saved",
                value = "$savedCount Reels",
                icon = Icons.Filled.Bookmark,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .tactileDepth(MaterialTheme.shapes.medium, 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .glass(MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TopicMasterySection(
    title: String,
    subtitle: String,
    items: List<TopicMasteryItem>,
    accentColor: Color,
    icon: ImageVector,
    emptyMessage: String,
    onTopicClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.large, 4.dp)
            .clip(MaterialTheme.shapes.large)
            .glass(MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (items.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(5).forEach { topicItem ->
                    TopicMasteryRow(
                        item = topicItem,
                        accentColor = accentColor,
                        onClick = { onTopicClick(topicItem.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicMasteryRow(
    item: TopicMasteryItem,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val pct = (item.score * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = item.score.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Practice topic",
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptySavedCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.medium, 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .glass(MaterialTheme.shapes.medium)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "No saved reels yet",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Double-tap any lesson card to bookmark it for quick access here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedReelCard(
    reel: Reel,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val trackCol = trackColor(reel.track.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .tactileDepth(MaterialTheme.shapes.medium, 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .glass(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(trackCol.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = reel.track.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            color = trackCol,
                        )
                    }
                    Text(
                        text = "Level ${reel.level}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reel.hook,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                        .padding(4.dp),
                )
            }
        }
    }
}
