package com.ferrisfeed.coreui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Streak flame: shows current day streak. When [celebrate] flips true the flame
 * pops with a 300ms spring (matches feed motion spec) to reward continuation.
 */
@Composable
fun StreakFlame(
    streakDays: Int,
    modifier: Modifier = Modifier,
    celebrate: Boolean = false,
) {
    val scale by animateFloatAsState(
        targetValue = if (celebrate) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "flame-pop",
    )
    val flameColor = if (streakDays > 0) FerrisColors.FerrisOrange else Color.Gray

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak: $streakDays days",
                tint = flameColor,
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$streakDays",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "StreakFlame", showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
private fun StreakFlamePreview() {
    FerrisFeedTheme(darkTheme = true) {
        Row(Modifier.padding(16.dp)) {
            StreakFlame(0)
            Spacer(Modifier.width(8.dp))
            StreakFlame(7)
            Spacer(Modifier.width(8.dp))
            StreakFlame(21, celebrate = true)
        }
    }
}
