package com.ferrisfeed.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ferrisfeed.coreui.FerrisFeedTheme
import com.ferrisfeed.coreui.FerrisMotion
import com.ferrisfeed.coreui.FerrisTheme

private data class Tab(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val Tabs = listOf(
    Tab(Route.Feed, "Learn", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Tab(Route.Path, "Path", Icons.Filled.Timeline, Icons.Outlined.Timeline),
    Tab(Route.Profile, "You", Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * Bottom navigation (v3): a plain, solid bar below the content. The old
 * floating glass pill sat on top of lessons and hid their last lines; this
 * bar never overlaps anything. Labels are always visible.
 */
@Composable
fun FerrisNavBar(
    selected: Route,
    onSelect: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(FerrisTheme.palette.hairline))
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Tabs.forEach { tab ->
                val isSelected = tab.route == selected
                val tint by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    FerrisMotion.QuickColor,
                    label = "tab-tint",
                )
                val pill by animateColorAsState(
                    if (isSelected) FerrisTheme.palette.raised else Color.Transparent,
                    FerrisMotion.QuickColor,
                    label = "tab-pill",
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(role = Role.Tab) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(tab.route)
                            }
                        }
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(pill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint)
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Preview(name = "Nav bar · dark", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360)
@Composable
private fun NavBarPreviewDark() {
    FerrisFeedTheme(darkTheme = true) {
        FerrisNavBar(selected = Route.Feed, onSelect = {})
    }
}

@Preview(name = "Nav bar · light", showBackground = true, backgroundColor = 0xFFFAF8F4, widthDp = 360)
@Composable
private fun NavBarPreviewLight() {
    FerrisFeedTheme(darkTheme = false) {
        FerrisNavBar(selected = Route.Path, onSelect = {})
    }
}
